/**
 * WebRTCSDK - P2P Communication Library
 * Compatible with both Browser and Node.js
 */
class WebRTCSDK {
    constructor(signalingUrl, iceConfig = {}, deps = {}) {
        this.signalingUrl = signalingUrl;
        this.io = deps.io || (typeof io !== 'undefined' ? io : null);
        this._RTCPeerConnection = deps.RTCPeerConnection || (typeof RTCPeerConnection !== 'undefined' ? RTCPeerConnection : null);
        this._RTCSessionDescription = deps.RTCSessionDescription || (typeof RTCSessionDescription !== 'undefined' ? RTCSessionDescription : null);
        this._RTCIceCandidate = deps.RTCIceCandidate || (typeof RTCIceCandidate !== 'undefined' ? RTCIceCandidate : null);

        this.config = {
            iceServers: iceConfig.iceServers || [{ urls: 'stun:stun.l.google.com:19302' }]
        };
        this.socket = null;
        this.peers = {}; 
        this.roomName = null;

        this.onMessage = (from, data) => {};
        this.onStatusChange = (status) => {};
        this.onLog = (msg) => {};
    }

    async join(roomName) {
        if (!this.io) throw new Error("Socket.io not found.");
        this.roomName = roomName;
        this.socket = this.io(this.signalingUrl);

        this.socket.on('connect', () => {
            this._log(`Connected to signaling server: ${this.signalingUrl}`);
            this.socket.emit('join', roomName);
        });

        this.socket.on('user-joined', (id) => {
            this._log(`New user joined: ${id}`);
            this._initiateCall(id);
        });

        this.socket.on('offer', async ({ offer, from }) => {
            this._log(`Received offer from: ${from}`);
            const pc = this._setupPC(from);
            await pc.setRemoteDescription(offer);
            const answer = await pc.createAnswer();
            await pc.setLocalDescription(answer);
            this.socket.emit('answer', { answer, to: from });
        });

        this.socket.on('answer', async ({ answer, from }) => {
            this._log(`Received answer from: ${from}`);
            const pc = this.peers[from]?.pc;
            if (pc) await pc.setRemoteDescription(answer);
        });

        this.socket.on('ice-candidate', async ({ candidate, from }) => {
            const pc = this.peers[from]?.pc;
            if (pc && candidate) {
                try {
                    // 受信したCandidateのタイプをログ
                    if (candidate.candidate) {
                        const type = candidate.candidate.split(' ')[7];
                        this._log(`Received ICE Candidate (${type}) from ${from}`);
                    }
                    await pc.addIceCandidate(candidate);
                } catch (e) {
                    this._log(`ICE Candidate error: ${e.message}`, true);
                }
            }
        });
    }

    send(message) {
        let sent = false;
        for (const id in this.peers) {
            const dc = this.peers[id].dc;
            if (dc && dc.readyState === 'open') {
                dc.send(message);
                sent = true;
            }
        }
        return sent;
    }

    _setupPC(id) {
        if (this.peers[id]) return this.peers[id].pc;
        const pc = new this._RTCPeerConnection(this.config);
        this.peers[id] = { pc, dc: null };

        pc.onicecandidate = (event) => {
            if (event.candidate) {
                // 生成したCandidateのタイプをログ
                const type = event.candidate.candidate.split(' ')[7];
                this._log(`Generated ICE Candidate (${type}) for ${id}`);
                if (this.socket) {
                    this.socket.emit('ice-candidate', { candidate: event.candidate, to: id });
                }
            }
        };

        pc.ondatachannel = (event) => {
            this._log(`Received remote DataChannel from ${id}`);
            this.peers[id].dc = event.channel;
            this._setupDataChannel(event.channel, id);
        };

        pc.oniceconnectionstatechange = () => {
            this._log(`ICE connection state change: ${pc.iceConnectionState}`);
            if (pc.iceConnectionState === 'connected' || pc.iceConnectionState === 'completed') {
                this._logIceStats(pc, id);
            }
            this._updateStatus();
        };
        return pc;
    }

    async _initiateCall(id) {
        const pc = this._setupPC(id);
        const dc = pc.createDataChannel("chat");
        this.peers[id].dc = dc;
        this._setupDataChannel(dc, id);

        const offer = await pc.createOffer();
        await pc.setLocalDescription(offer);
        this.socket.emit('offer', { offer, to: id });
    }

    _setupDataChannel(dc, id) {
        dc.onopen = () => {
            this._log(`DataChannel opened with ${id}`);
            this._logIceStats(this.peers[id].pc, id);
            this._updateStatus();
        };
        dc.onmessage = (event) => this.onMessage(id, event.data);
        dc.onclose = () => {
            this._log(`DataChannel closed with ${id}`);
            this._updateStatus();
        };
    }

    async _logIceStats(pc, id) {
        try {
            // Node.js (wrtc) では getStats() がコールバック形式の場合があるため、両対応
            const getStatsPromise = () => new Promise((resolve, reject) => {
                const res = pc.getStats(resolve, reject);
                if (res instanceof Promise) res.then(resolve, reject);
            });

            const stats = await getStatsPromise();
            let selectedType = "unknown";
            
            stats.forEach(report => {
                if (report.type === 'local-candidate' && (report.candidateType || report.type)) {
                    // 最後に成功したペアのタイプを推測 (succeededなpairを探すのが本来だが一旦全部出す)
                    if (report.candidateType) selectedType = report.candidateType;
                }
                if (report.type === 'candidate-pair' && report.state === 'succeeded') {
                    const local = stats.get(report.localCandidateId);
                    if (local) selectedType = local.candidateType || local.type;
                }
            });
            
            this._log(`ICE Connected using ${selectedType} with ${id}`);
        } catch (e) {
            // this._log(`Stats failed: ${e.message}`);
        }
    }

    _updateStatus() {
        let openDcCount = 0;
        for (const id in this.peers) {
            if (this.peers[id].dc?.readyState === 'open') openDcCount++;
        }
        this.onStatusChange({ dcOpen: openDcCount > 0, peerCount: Object.keys(this.peers).length });
    }

    _log(msg, isError = false) {
        this.onLog(msg);
        // コンテナの標準出力に確実に出す
        if (isError) console.error(`[ERR] ${msg}`);
        else console.log(`[SDK_RAW] ${msg}`);
    }
}

if (typeof module !== 'undefined' && module.exports) {
    module.exports = WebRTCSDK;
}
