const { RTCPeerConnection } = require('werift');
const { io } = require('socket.io-client');

/**
 * WebRTCChannel SDK
 * Node.js専用 WebRTC DataChannel ライブラリ
 */
class WebRTCChannel {
    constructor(signalingUrl, iceConfig = {}) {
        this.signalingUrl = signalingUrl;
        this.config = {
            iceServers: iceConfig.iceServers || [{ urls: 'stun:stun.l.google.com:19302' }],
            iceTransportPolicy: iceConfig.iceTransportPolicy || 'all'
        };
        this.socket = null;
        this.peers = {}; // id -> { pc, dc, iceBuffer }
        
        // Event Callbacks
        this.onMessage = (from, data) => {};
        this.onStatusChange = (status) => {};
        this.onLog = (msg) => {};
    }

    async join(roomName) {
        this.socket = io(this.signalingUrl);

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
            this._processBufferedCandidates(from);
            const answer = await pc.createAnswer();
            await pc.setLocalDescription(answer);
            this.socket.emit('answer', { answer, to: from });
        });

        this.socket.on('answer', async ({ answer, from }) => {
            this._log(`Received answer from: ${from}`);
            const pc = this.peers[from]?.pc;
            if (pc) {
                await pc.setRemoteDescription(answer);
                this._processBufferedCandidates(from);
            }
        });

        this.socket.on('ice-candidate', async ({ candidate, from }) => {
            const peer = this.peers[from];
            if (peer && peer.pc && peer.pc.remoteDescription && candidate) {
                try {
                    await peer.pc.addIceCandidate(candidate);
                } catch (e) {
                    this._log(`ICE Candidate error: ${e.message}`, true);
                }
            } else if (candidate) {
                if (!this.peers[from]) {
                    this.peers[from] = { pc: null, dc: null, iceBuffer: [] };
                }
                this.peers[from].iceBuffer.push(candidate);
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
        if (this.peers[id] && this.peers[id].pc) return this.peers[id].pc;
        
        const pc = new RTCPeerConnection(this.config);
        if (!this.peers[id]) {
            this.peers[id] = { pc, dc: null, iceBuffer: [] };
        } else {
            this.peers[id].pc = pc;
        }

        pc.onIceCandidate.subscribe((candidate) => {
            if (candidate && this.socket) {
                this.socket.emit('ice-candidate', { candidate: candidate.toJSON(), to: id });
            }
        });

        pc.onDataChannel.subscribe((channel) => {
            this._log(`Received remote DataChannel from ${id}`);
            this.peers[id].dc = channel;
            this._setupDataChannel(channel, id);
        });

        pc.iceConnectionStateChange.subscribe((state) => {
            this._log(`ICE connection state change: ${state}`);
            this._updateStatus();
        });

        return pc;
    }

    _processBufferedCandidates(id) {
        const peer = this.peers[id];
        if (peer && peer.pc && peer.iceBuffer && peer.iceBuffer.length > 0) {
            peer.iceBuffer.forEach(async (candidate) => {
                try {
                    await peer.pc.addIceCandidate(candidate);
                } catch (e) {
                    this._log(`Buffered ICE Candidate error: ${e.message}`, true);
                }
            });
            peer.iceBuffer = [];
        }
    }

    async _initiateCall(id) {
        const pc = this._setupPC(id);
        const dc = pc.createDataChannel("chat");
        this.peers[id].dc = dc;
        this._setupDataChannel(dc, id);

        this._log(`Creating offer for ${id}`);
        const offer = await pc.createOffer();
        await pc.setLocalDescription(offer);
        this.socket.emit('offer', { offer, to: id });
    }

    _setupDataChannel(dc, id) {
        dc.stateChanged.subscribe((state) => {
            if (state === 'open') {
                this._log(`DataChannel opened with ${id}`);
            }
            this._updateStatus();
        });
        dc.onMessage.subscribe((data) => {
            this.onMessage(id, data.toString());
        });
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
        if (isError) console.error(`[WebRTCChannel] ERROR: ${msg}`);
    }
}

module.exports = WebRTCChannel;
