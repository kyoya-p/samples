let pc;
let ws;
let localStream;
let iceServers = [];

// UIの調整: ボード（ビデオコンテナ）を縦並びにし、ログを TextArea に置換
window.addEventListener('DOMContentLoaded', () => {
    const container = document.querySelector('.container');
    if (container) {
        container.style.flexDirection = 'column';
    }

    const oldLogs = document.getElementById('logs');
    if (oldLogs && oldLogs.tagName !== 'TEXTAREA') {
        const textArea = document.createElement('textarea');
        textArea.id = 'logs';
        textArea.style.width = '100%';
        textArea.style.height = '200px';
        textArea.style.marginTop = '10px';
        textArea.style.display = 'block';
        textArea.readOnly = true;
        oldLogs.parentNode.replaceChild(textArea, oldLogs);
    }
});

const log = (msg) => {
    const textArea = document.getElementById('logs');
    if (textArea) {
        const timestamp = new Date().toLocaleTimeString();
        textArea.value += `[${timestamp}] ${msg}\n`;
        textArea.scrollTop = textArea.scrollHeight;
    }
};

// サーバーから ICE サーバー設定を取得
async function fetchConfig() {
    try {
        const res = await fetch('/config');
        const config = await res.json();
        iceServers = config.iceServers;
        log('ICE configuration loaded from server');
    } catch (e) {
        log('Failed to load ICE configuration');
    }
}

const setLocalStream = (stream) => {
    localStream = stream;
    document.getElementById('localVideo').srcObject = localStream;
    
    if (pc) {
        const videoTrack = stream.getVideoTracks()[0];
        const senders = pc.getSenders();

        if (videoTrack) {
            const videoSender = senders.find(s => s.track && s.track.kind === 'video');
            if (videoSender) videoSender.replaceTrack(videoTrack);
            else pc.addTrack(videoTrack, stream);
        }
        log('Local video track updated');
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
    return canvas.captureStream(30);
};

document.getElementById('start').onclick = async () => {
    try {
        const stream = await navigator.mediaDevices.getUserMedia({ video: true, audio: false });
        setLocalStream(stream);
        log('Camera started (Video only)');
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
    const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
    ws = new WebSocket(`${protocol}//${window.location.host}`);
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
        iceServers: iceServers,
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
        log(`[Connection Established] ${isRelay ? '🔄 (Relay)' : '⚡ (P2P)'}`);
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

// 初期化
fetchConfig();
