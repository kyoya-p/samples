/**
 * WebRTCSDK - P2P Communication Library
 * Handles signaling, peer connection management, and data channels.
 */
class WebRTCSDK {
    constructor(signalingUrl, iceConfig = {}) {
        this.signalingUrl = signalingUrl;
        this.config = {
            iceServers: iceConfig.iceServers || [{ urls: 'stun:stun.l.google.com:19302' }]
        };
        this.socket = null;
        this.peers = {}; // id -> { pc, dc }
        this.roomName = null;

        // Callbacks
        this.onMessage = (from, data) => {};
        this.onStatusChange = (status) => {};
        this.onLog = (msg) => {};
    }

    async join(roomName) {
        this.roomName = roomName;
        // socket.io-client is assumed to be loaded globally via <script src="/socket.io/socket.io.js"></script>
        this.socket = io(this.signalingUrl);

        this.socket.on('connect', () => {
            this._log(`Connected to signaling server: ${this.signalingUrl}`);
            this.socket.emit('join', roomName);
        });

        this.socket.on('user-joined', (id) => {
            this._log(`New user joined room: ${id}`);
            this._initiateCall(id);
        });

        this.socket.on('offer', async ({ offer, from }) => {
            this._log(`Received offer from: ${from}`);
            const pc = this._setupPC(from);
            await pc.setRemoteDescription(new RTCSessionDescription(offer));
            const answer = await pc.createAnswer();
            await pc.setLocalDescription(answer);
            this.socket.emit('answer', { answer, to: from });
        });

        this.socket.on('answer', async ({ answer, from }) => {
            this._log(`Received answer from: ${from}`);
            const pc = this.peers[from]?.pc;
            if (pc) {
                await pc.setRemoteDescription(new RTCSessionDescription(answer));
            }
        });

        this.socket.on('ice-candidate', async ({ candidate, from }) => {
            if (!candidate) return;
            const pc = this.peers[from]?.pc;
            if (pc) {
                try {
                    await pc.addIceCandidate(new RTCIceCandidate(candidate));
                } catch (e) {
                    this._log(`Error adding ICE candidate: ${e.message}`, true);
                }
            }
        });

        this.socket.on('disconnect', (reason) => {
            this._log(`Disconnected from signaling server: ${reason}`);
            this._cleanup();
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
        const pc = new RTCPeerConnection(this.config);
        this.peers[id] = { pc: pc, dc: null };

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

        pc.oniceconnectionstatechange = () => {
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
            this._updateStatus();
        };
        dc.onmessage = (event) => {
            this.onMessage(id, event.data);
        };
        dc.onclose = () => {
            this._log(`DataChannel closed with ${id}`);
            this._updateStatus();
        };
    }

    _updateStatus() {
        let connectedCount = 0;
        let openDcCount = 0;
        
        for (const id in this.peers) {
            const pc = this.peers[id].pc;
            const dc = this.peers[id].dc;
            if (['connected', 'completed'].includes(pc.iceConnectionState)) {
                connectedCount++;
            }
            if (dc && dc.readyState === 'open') {
                openDcCount++;
            }
        }

        this.onStatusChange({
            iceConnected: connectedCount > 0,
            dcOpen: openDcCount > 0,
            connectedCount,
            openDcCount,
            peerCount: Object.keys(this.peers).length
        });
    }

    _cleanup() {
        for (const id in this.peers) {
            this.peers[id].pc.close();
        }
        this.peers = {};
        this._updateStatus();
    }

    _log(msg, isError = false) {
        this.onLog(msg);
        if (isError) console.error(msg);
        else console.log(msg);
    }
}
