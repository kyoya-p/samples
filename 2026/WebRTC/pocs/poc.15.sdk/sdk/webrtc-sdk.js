/**
 * WebRTCSDK - P2P Communication Library
 * Compatible with both Browser and Node.js
 */
class WebRTCSDK {
    constructor(signalingUrl, iceConfig = {}, deps = {}) {
        this.signalingUrl = signalingUrl;
        
        // Environment detection and dependency injection
        this.io = deps.io || (typeof io !== 'undefined' ? io : null);
        
        // Inject dependencies or use global ones
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
            if (event.candidate && this.socket) {
                this.socket.emit('ice-candidate', { candidate: event.candidate, to: id });
            }
        };

        pc.ondatachannel = (event) => {
            this._log(`Received remote DataChannel from ${id}`);
            this.peers[id].dc = event.channel;
            this._setupDataChannel(event.channel, id);
        };

        pc.oniceconnectionstatechange = () => this._updateStatus();
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
            this._updateStatus();
        };
        dc.onmessage = (event) => this.onMessage(id, event.data);
        dc.onclose = () => {
            this._log(`DataChannel closed with ${id}`);
            this._updateStatus();
        };
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
        if (isError) console.error(msg);
    }
}

if (typeof module !== 'undefined' && module.exports) {
    module.exports = WebRTCSDK;
}
