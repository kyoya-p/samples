let pc;
let ws;
let localStream;

const log = (msg) => {
    const div = document.createElement('div');
    div.textContent = `[${new Date().toLocaleTimeString()}] ${msg}`;
    document.getElementById('logs').appendChild(div);
    document.getElementById('logs').scrollTop = document.getElementById('logs').scrollHeight;
};

const setLocalStream = (stream) => {
    localStream = stream;
    document.getElementById('localVideo').srcObject = localStream;
    
    if (pc) {
        const videoTrack = stream.getVideoTracks()[0];
        const audioTrack = stream.getAudioTracks()[0];
        const senders = pc.getSenders();

        if (videoTrack) {
            const videoSender = senders.find(s => s.track && s.track.kind === 'video');
            if (videoSender) videoSender.replaceTrack(videoTrack);
            else pc.addTrack(videoTrack, stream);
        }
        if (audioTrack) {
            const audioSender = senders.find(s => s.track && s.track.kind === 'audio');
            if (audioSender) audioSender.replaceTrack(audioTrack);
            else pc.addTrack(audioTrack, stream);
        }
        log('Local tracks updated');
    }
};

const createDummyStream = () => {
    const canvas = document.createElement('canvas');
    canvas.width = 320; canvas.height = 240;
    const ctx = canvas.getContext('2d');
    const clientId = Math.random().toString(36).substring(7);
    const bgColor = `hsl(${Math.random() * 360}, 70%, 50%)`;
    let x = 0;
    setInterval(() => {
        ctx.fillStyle = bgColor; ctx.fillRect(0, 0, 320, 240);
        ctx.fillStyle = 'white'; x = (x + 5) % 320; ctx.fillRect(x, 100, 50, 50);
        ctx.fillStyle = 'black'; ctx.font = '20px Arial';
        ctx.fillText(`ID: ${clientId}`, 10, 30);
        ctx.fillText(new Date().toLocaleTimeString(), 10, 60);
    }, 50);
    const stream = canvas.captureStream(30);
    const ctxAudio = new AudioContext();
    const osc = ctxAudio.createOscillator();
    const dest = ctxAudio.createMediaStreamDestination();
    osc.connect(dest); osc.start();
    stream.addTrack(dest.stream.getAudioTracks()[0]);
    return stream;
};

document.getElementById('start').onclick = async () => {
    try {
        const stream = await navigator.mediaDevices.getUserMedia({ video: true, audio: true });
        setLocalStream(stream);
        log('Camera started');
    } catch (e) {
        log(`Camera error: ${e.message}. Using dummy stream.`);
        const stream = createDummyStream();
        setLocalStream(stream);
        log('Dummy stream started');
    }
};

document.getElementById('share').onclick = async () => {
    try {
        const stream = await navigator.mediaDevices.getDisplayMedia({ video: true });
        setLocalStream(stream);
        log('Window capture started');
        stream.getVideoTracks()[0].onended = () => log('Window capture ended');
    } catch (e) {
        log(`Capture error: ${e.message}`);
    }
};

document.getElementById('connect').onclick = () => {
    ws = new WebSocket('wss://' + window.location.host);
    ws.onopen = () => log('Connected to signaling');
    ws.onmessage = async (e) => {
        const data = JSON.parse(e.data);
        if (data.sdp) {
            log(`Received ${data.sdp.type}`);
            if (!pc) setupPeerConnection();
            await pc.setRemoteDescription(new RTCSessionDescription(data.sdp));
            if (data.sdp.type === 'offer') {
                const answer = await pc.createAnswer();
                await pc.setLocalDescription(answer);
                ws.send(JSON.stringify({ sdp: pc.localDescription }));
            }
        } else if (data.candidate) {
            if (pc) await pc.addIceCandidate(new RTCIceCandidate(data.candidate));
        }
    };
};

const setupPeerConnection = () => {
    const forceTurn = document.getElementById('forceTurn').checked;
    const config = {
        iceServers: [
            { urls: 'stun:stun.l.google.com:19302' },
            {
                urls: `turn:${window.location.hostname}:3478`,
                username: 'user',
                credential: 'password'
            }
        ],
        iceTransportPolicy: forceTurn ? 'relay' : 'all'
    };
    
    log(`Setup PC (Force TURN: ${forceTurn})`);
    pc = new RTCPeerConnection(config);

    pc.onicecandidate = (e) => {
        if (e.candidate) {
            log(`ICE Candidate: ${e.candidate.candidate.split(' ')[7] || 'unknown'} (${e.candidate.candidate.split(' ')[4]})`);
            ws.send(JSON.stringify({ candidate: e.candidate }));
        }
    };

    pc.ontrack = (e) => {
        log(`Received remote track: ${e.track.kind}`);
        const remoteVideo = document.getElementById('remoteVideo');
        if (e.streams && e.streams[0]) {
            remoteVideo.srcObject = e.streams[0];
        } else {
            if (!remoteVideo.srcObject) {
                remoteVideo.srcObject = new MediaStream();
            }
            remoteVideo.srcObject.addTrack(e.track);
        }
    };

    if (localStream) {
        localStream.getTracks().forEach(track => pc.addTrack(track, localStream));
        log(`Added ${localStream.getTracks().length} tracks to PC`);
    }

    pc.oniceconnectionstatechange = () => {
        log(`ICE State: ${pc.iceConnectionState}`);
        if (pc.iceConnectionState === 'connected' || pc.iceConnectionState === 'completed') {
            displayConnectionStats();
        }
    };
};

const displayConnectionStats = async () => {
    if (!pc) return;
    try {
        const stats = await pc.getStats();
        let activePair;

        for (const report of stats.values()) {
            if (report.type === 'transport' && report.selectedCandidatePairId) {
                activePair = stats.get(report.selectedCandidatePairId);
                break;
            }
            if (report.type === 'candidate-pair' && report.selected) {
                activePair = report;
                break;
            }
        }

        if (!activePair) return;

        const local = stats.get(activePair.localCandidateId);
        const remote = stats.get(activePair.remoteCandidateId);
        if (!local || !remote) return;

        const isRelay = local.candidateType === 'relay' || remote.candidateType === 'relay';
        log(`[Connection Established] ${isRelay ? '[Relay]' : '[P2P]'}`);
        log(`Path: ${local.candidateType} <-> ${remote.candidateType}`);
        log(`Local: ${local.ip || local.address}:${local.port} (${local.protocol})`);
        log(`Remote: ${remote.ip || remote.address}:${remote.port} (${remote.protocol})`);
    } catch (e) {
        log(`Stats error: ${e.message}`);
    }
};

document.getElementById('call').onclick = async () => {
    if (!pc) setupPeerConnection();
    const offer = await pc.createOffer();
    await pc.setLocalDescription(offer);
    ws.send(JSON.stringify({ sdp: pc.localDescription }));
    log('Offer sent');
};
