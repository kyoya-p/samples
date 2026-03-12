export class WebRTCController {
    constructor() {
        this.peerConnection = null;
        this.dataChannel = null;
        this.iceServers = null;
        this.room = "demo-room";
        this.init();
    }

    async init() {
        this.startListening();
        await this.fetchIceServers();
    }

    isForceTurn() {
        const el = document.getElementById("chkForceTurn");
        return el ? el.checked : false;
    }

    async fetchIceServers() {
        try {
            const response = await fetch("/ice-servers");
            this.iceServers = await response.json();
            console.log("ICE Servers fetched successfully.");
        } catch (e) {
            console.error("Failed to fetch ICE servers:", e.message);
        }
    }

    createPeerConnection() {
        if (!this.iceServers) {
            console.error("Error: ICE servers not loaded yet.");
            return;
        }

        const config = { iceServers: this.iceServers };
        if (this.isForceTurn()) {
            console.log("Policy: Force TURN (Relay only)");
            config.iceTransportPolicy = "relay";
        } else {
            console.log("Policy: All (Allow host/srflx)");
        }

        this.peerConnection = new RTCPeerConnection(config);

        this.peerConnection.onicecandidate = (event) => {
            if (event.candidate) {
                const cand = event.candidate;
                console.log(`Gathered ICE Candidate: type=${cand.type} protocol=${cand.protocol} address=${cand.address}:${cand.port}`);
                this.sendSignal({ type: 'candidate', candidate: cand.toJSON() });
            } else {
                console.log("ICE Gathering complete.");
            }
        };

        this.peerConnection.ondatachannel = (event) => {
            console.log("DataChannel received from remote");
            this.setDataChannel(event.channel);
        };

        this.peerConnection.onsignalingstatechange = () => {
            console.log(`WebRTC SignalingState: ${this.peerConnection.signalingState}`);
        };

        this.peerConnection.oniceconnectionstatechange = () => {
            console.log(`WebRTC ICEConnectionState: ${this.peerConnection.iceConnectionState}`);
            if (this.peerConnection.iceConnectionState === "connected") {
                window.setCanvasEnabled(true);
            }
        };

        this.peerConnection.onconnectionstatechange = () => {
            const state = this.peerConnection.connectionState;
            console.log(`WebRTC ConnectionState: ${state}`);
            
            if (state === "connected") {
                window.setCanvasEnabled(true);
                this.logSelectedPath();
            } else if (["failed", "closed", "disconnected"].includes(state)) {
                window.setCanvasEnabled(false);
            }
        };
    }

    async logSelectedPath() {
        const stats = await this.peerConnection.getStats();
        let selectedPairId = "";
        stats.forEach(report => {
            if (report.type === "transport" && report.selectedCandidatePairId) {
                selectedPairId = report.selectedCandidatePairId;
            }
        });
        if (selectedPairId) {
            const pair = stats.get(selectedPairId);
            const local = stats.get(pair.localCandidateId);
            const remote = stats.get(pair.remoteCandidateId);
            console.log(`Selected Path: Local[${local.candidateType}] <-> Remote[${remote.candidateType}]`);
        }
    }

    setDataChannel(channel) {
        this.dataChannel = channel;
        this.dataChannel.onopen = () => console.log("DataChannel opened");
        this.dataChannel.onmessage = (event) => {
            this.handleRemoteData(JSON.parse(event.data));
        };
    }

    async connectP2P() {
        if (!this.iceServers) {
            await this.fetchIceServers();
        }
        if (!this.iceServers) return;

        // 接続前に現在の部屋に明示的にジョイン
        window.socket.emit('join', this.room);

        this.createPeerConnection();
        this.dataChannel = this.peerConnection.createDataChannel("chaos-sync");
        this.setDataChannel(this.dataChannel);

        const offer = await this.peerConnection.createOffer();
        await this.peerConnection.setLocalDescription(offer);
        console.log("Local Offer set");
        this.sendSignal(offer);
    }

    sendSignal(signal) {
        signal.room = this.room;
        window.emitSignal(signal);
    }

    handleRemoteData(data) {
        if (data.type === "point") {
            window.drawRemotePoint(data.x, data.y);
        }
    }

    sendPoint(x, y) {
        const data = { type: 'point', x, y };
        if (this.dataChannel && this.dataChannel.readyState === "open") {
            this.dataChannel.send(JSON.stringify(data));
        }
    }

    startListening() {
        window.onRemoteSignal = async (data) => {
            switch (data.type) {
                case "offer":
                    // すでに接続処理中の場合は無視
                    if (this.peerConnection && this.peerConnection.signalingState !== "stable") return;
                    console.log("Received Remote Offer");
                    if (!this.peerConnection) this.createPeerConnection();
                    await this.peerConnection.setRemoteDescription(new RTCSessionDescription(data));
                    const answer = await this.peerConnection.createAnswer();
                    await this.peerConnection.setLocalDescription(answer);
                    console.log("Local Answer set");
                    this.sendSignal(answer);
                    break;
                case "answer":
                    if (!this.peerConnection || this.peerConnection.signalingState === "stable") return;
                    console.log("Received Remote Answer");
                    await this.peerConnection.setRemoteDescription(new RTCSessionDescription(data));
                    break;
                case "candidate":
                    if (this.peerConnection && data.candidate) {
                        console.log("Received Remote Candidate");
                        try {
                            await this.peerConnection.addIceCandidate(new RTCIceCandidate(data.candidate));
                        } catch (e) {
                            console.error("Error adding ice candidate", e.message);
                        }
                    }
                    break;
                case "point":
                    this.handleRemoteData(data);
                    break;
            }
        };
    }
}

// 自動初期化
window.webrtcController = new WebRTCController();
