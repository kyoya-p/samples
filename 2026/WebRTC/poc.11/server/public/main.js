let localStream;
let pc;
let socket;
let roomName;

const turnHost = location.hostname === 'server' ? 'turn' : location.hostname;
const config = {
    iceServers: [
        { urls: 'stun:stun.l.google.com:19302' },
        {
            urls: `turn:${turnHost}:3478`,
            username: 'user',
            credential: 'password123'
        }
    ]
};

const sdpArea = document.getElementById('sdpArea');
const iceList = document.getElementById('iceList');
const statusText = document.getElementById('connStatus');
const localVideo = document.getElementById('localVideo');
const noHostCheck = document.getElementById('noHostCheck');
const signalingStatus = document.getElementById('signalingStatus');
const signalingDot = document.getElementById('signalingDot');
const roomInput = document.getElementById('roomInput');
const connectBtn = document.getElementById('connectBtn');
const logArea = document.getElementById('logArea');

// --- Custom Logger ---
const originalLog = console.log;
const originalError = console.error;

function appendLog(message, isError = false) {
    if (!logArea) return;
    const div = document.createElement('div');
    const time = new Date().toLocaleTimeString([], { hour12: false, hour: '2-digit', minute:'2-digit', second:'2-digit', fractionalSecondDigits: 3 });
    div.innerText = `[${time}] ${message}`;
    if (isError) div.style.color = '#dc2626'; // red
    logArea.appendChild(div);
    logArea.scrollTop = logArea.scrollHeight;
}

console.log = function(...args) {
    originalLog.apply(console, args);
    appendLog(args.map(a => typeof a === 'object' ? JSON.stringify(a) : a).join(' '));
};

console.error = function(...args) {
    originalError.apply(console, args);
    appendLog(args.map(a => typeof a === 'object' ? JSON.stringify(a) : a).join(' '), true);
};
// ---------------------

let qixContext = {
    active: false,
    lineSets: [],
    canvas: null,
    ctx: null
};

window.addEventListener('load', () => {
    initQixCanvas();
    qixContext.active = true;
    localStream = qixContext.canvas.captureStream(30);
    localVideo.srcObject = localStream;
    addNewLineSet(qixContext.canvas.width / 2, qixContext.canvas.height / 2);
    animateQix();

    noHostCheck.addEventListener('change', () => {
        document.body.classList.toggle('hide-host', noHostCheck.checked);
        if (pc && pc.localDescription) sdpArea.value = getProcessedSDP();
    });

    connectBtn.onclick = () => {
        if (socket) return;
        roomName = roomInput.value;
        if (!roomName) return alert("Enter room name");
        
        initSignaling();
        connectBtn.disabled = true;
        connectBtn.innerText = 'Connecting...';
        roomInput.disabled = true;
    };

    // --- Fix: Ensure listener is added after DOM load ---
    localVideo.addEventListener('mousedown', (e) => {
        console.log('Local video clicked: adding new effect at', e.offsetX, e.offsetY);
        const rect = localVideo.getBoundingClientRect();
        const x = (e.clientX - rect.left) * (qixContext.canvas.width / rect.width);
        const y = (e.clientY - rect.top) * (qixContext.canvas.height / rect.height);
        addNewLineSet(x, y);
    });
});

function initSignaling() {
    socket = io();
    
    socket.on('connect', () => {
        console.log('Connected to signaling server');
        signalingStatus.querySelector('span:last-child').innerText = `Connected: ${roomName}`;
        signalingDot.style.background = '#22c55e';
        connectBtn.innerText = 'Joined';
        socket.emit('join', roomName);
    });

    socket.on('disconnect', (reason) => {
        console.error('Disconnected from signaling server:', reason);
        signalingStatus.querySelector('span:last-child').innerText = 'Offline';
        signalingDot.style.background = '#ef4444';
        roomInput.disabled = false;
        connectBtn.disabled = false;
        connectBtn.innerText = 'Connect';
        socket = null;
    });

    socket.on('user-joined', (id) => {
        console.log('New user joined room:', id);
        // 新しいユーザーが来たらOfferを送る（自分が先にいた場合）
        initiateCall();
    });

    socket.on('offer', async ({ offer, from }) => {
        console.log('Received offer from:', from);
        setupPC();
        await pc.setRemoteDescription(new RTCSessionDescription(offer));
        const answer = await pc.createAnswer();
        await pc.setLocalDescription(answer);
        socket.emit('answer', { answer, roomName });
    });

    socket.on('answer', async ({ answer, from }) => {
        console.log('Received answer from:', from);
        if (pc) {
            await pc.setRemoteDescription(new RTCSessionDescription(answer));
        }
    });

    socket.on('ice-candidate', async ({ candidate, from }) => {
        console.log('Received ice-candidate from:', from);
        if (pc) {
            try {
                await pc.addIceCandidate(new RTCIceCandidate(candidate));
            } catch (e) {
                console.error('Error adding ice candidate:', e);
            }
        }
    });

    joinBtn.onclick = () => {
        // This is now integrated into connectBtn.onclick but keeping it for safety
        roomName = roomInput.value;
        if (!roomName) return alert("Enter room name");
        socket.emit('join', roomName);
    };
}

async function initiateCall() {
    console.log('Initiating call...');
    setupPC();
    const offer = await pc.createOffer();
    await pc.setLocalDescription(offer);
    socket.emit('offer', { offer, roomName });
    updateSDPArea();
}

function updateSDPArea() {
    if (pc && pc.localDescription) {
        sdpArea.value = getProcessedSDP();
    }
}

function getProcessedSDP() {
    if (!pc || !pc.localDescription) return "";
    let sdpObj = JSON.parse(JSON.stringify(pc.localDescription));
    if (noHostCheck.checked) {
        sdpObj.sdp = sdpObj.sdp.split('\n').filter(line => !line.includes('typ host')).join('\n');
    }
    return JSON.stringify(sdpObj);
}

// Manual buttons still work for debugging
document.getElementById('createOfferBtn').onclick = initiateCall;

document.getElementById('createAnswerBtn').onclick = async () => {
    if (!sdpArea.value) return alert("Please paste Offer SDP first");
    console.log('Manual Answer creation...');
    setupPC();
    try {
        const offer = JSON.parse(sdpArea.value);
        await pc.setRemoteDescription(new RTCSessionDescription(offer));
        const answer = await pc.createAnswer();
        await pc.setLocalDescription(answer);
        updateSDPArea();
    } catch (e) { alert("Invalid Offer SDP"); }
};

document.getElementById('setAnswerBtn').onclick = async () => {
    console.log('Manual Answer setting...');
    try {
        const answer = JSON.parse(sdpArea.value);
        await pc.setRemoteDescription(new RTCSessionDescription(answer));
    } catch (e) { alert("Invalid Answer SDP"); }
};

function setupPC() {
    if (pc) {
        console.log('RTCPeerConnection already exists. Reusing...');
        return;
    }
    console.log('Creating new RTCPeerConnection...');
    pc = new RTCPeerConnection(config);
    iceList.innerHTML = "";
    localStream.getTracks().forEach(track => {
        console.log('Adding local track:', track.kind);
        pc.addTrack(track, localStream);
    });
    
    pc.onicecandidate = (event) => {
        if (event.candidate) {
            const cand = event.candidate;
            console.log('Local ICE candidate found:', cand.type, cand.address || cand.ip);
            const div = document.createElement('div');
            div.className = `ice-item ${cand.type}`;
            div.innerHTML = `<span>${cand.type}</span> ${cand.protocol}://${cand.address || cand.ip}:${cand.port}`;
            iceList.appendChild(div);
            
            // 自動シグナリング時はCandidateを送信
            if (roomName && socket) {
                socket.emit('ice-candidate', { candidate: cand, roomName });
            }
        } else {
            console.log('Local ICE candidate gathering complete.');
        }
    };

    pc.oniceconnectionstatechange = () => {
        const state = pc.iceConnectionState;
        console.log('ICE Connection State Change:', state);
        statusText.innerHTML = `<span class="status-dot"></span> ICE Connection: ${state}`;
        const dot = statusText.querySelector('.status-dot');
        if (state === 'connected' || state === 'completed') {
            dot.style.background = '#22c55e';
            updateSelectedIce(); // 接続完了時にアクティブなICEを確認
        }
        else if (state === 'checking') dot.style.background = '#eab308';
        else if (state === 'failed' || state === 'disconnected' || state === 'closed') dot.style.background = '#ef4444';
        else dot.style.background = '#94a3b8';
    };

    pc.onconnectionstatechange = () => {
        console.log('PeerConnection State Change:', pc.connectionState);
        if (pc.connectionState === 'connected') {
            updateSelectedIce();
        }
    };

    pc.onsignalingstatechange = () => {
        console.log('Signaling State Change:', pc.signalingState);
    };

    pc.ontrack = (event) => {
        console.log('Remote track received:', event.track.kind);
        const remoteVideo = document.getElementById('remoteVideo');
        if (remoteVideo.srcObject !== event.streams[0]) {
            remoteVideo.srcObject = event.streams[0];
            console.log('Remote stream attached to video element');
        }
    };
}

async function updateSelectedIce() {
    if (!pc) return;
    try {
        const stats = await pc.getStats();
        let activeCandidatePair = null;
        
        stats.forEach(report => {
            if (report.type === 'transport') {
                const pairId = report.selectedCandidatePairId;
                if (pairId) activeCandidatePair = stats.get(pairId);
            }
        });

        if (activeCandidatePair && activeCandidatePair.state === 'succeeded') {
            const local = stats.get(activeCandidatePair.localCandidateId);
            const remote = stats.get(activeCandidatePair.remoteCandidateId);
            
            const selectedInfo = document.getElementById('selectedIce');
            selectedInfo.innerHTML = `
                <div style="color: var(--success); font-weight: bold; margin-bottom: 4px;">✔ Selected ICE Pair</div>
                <div class="ice-item active" style="border: 1px solid var(--success); background: #f0fdf4;">
                    <span>LOCAL:</span> ${local.candidateType} (${local.protocol}://${local.ip}:${local.port})<br>
                    <span>REMOTE:</span> ${remote.candidateType} (${remote.protocol}://${remote.ip}:${remote.port})
                </div>
            `;
            console.log('Selected ICE Pair:', local.candidateType, '->', remote.candidateType);
        }
    } catch (e) {
        console.error('Error fetching ICE stats:', e);
    }
}

// Qix Logic
function initQixCanvas() {
    const canvas = document.createElement('canvas');
    canvas.width = 640; canvas.height = 480;
    qixContext.canvas = canvas;
    qixContext.ctx = canvas.getContext('2d');
    const ctx = qixContext.ctx;
    ctx.fillStyle = '#0f172a';
    ctx.fillRect(0, 0, canvas.width, canvas.height);
}

function addNewLineSet(startX, startY) {
    const now = Date.now();
    const newLine = {
        points: [
            { x: startX, y: startY, vx: (Math.random() - 0.5) * 15, vy: (Math.random() - 0.5) * 15 },
            { x: startX, y: startY, vx: (Math.random() - 0.5) * 15, vy: (Math.random() - 0.5) * 15 }
        ],
        history: [],
        color: `hsl(${Math.random() * 360}, 90%, 65%)`,
        expires: now + 60000
    };
    qixContext.lineSets.push(newLine);
}

function animateQix() {
    const { canvas, ctx, lineSets } = qixContext;
    const now = Date.now();
    ctx.fillStyle = 'rgba(10, 15, 30, 0.2)';
    ctx.fillRect(0, 0, canvas.width, canvas.height);
    qixContext.lineSets = lineSets.filter(set => set.expires > now);
    qixContext.lineSets.forEach(set => {
        set.points.forEach(p => {
            p.x += p.vx; p.y += p.vy;
            if (p.x < 0 || p.x > canvas.width) p.vx *= -1;
            if (p.y < 0 || p.y > canvas.height) p.vy *= -1;
        });
        set.history.push({ p1: { ...set.points[0] }, p2: { ...set.points[1] } });
        if (set.history.length > 25) set.history.shift();
        set.history.forEach((h, i) => {
            ctx.beginPath();
            ctx.strokeStyle = set.color;
            ctx.globalAlpha = i / set.history.length;
            ctx.lineWidth = 1.5;
            ctx.moveTo(h.p1.x, h.p1.y);
            ctx.lineTo(h.p2.x, h.p2.y);
            ctx.stroke();
        });
    });
    ctx.globalAlpha = 1.0;
    
    // Use setTimeout instead of requestAnimationFrame so the stream doesn't freeze in background tabs
    if (qixContext.active) {
        setTimeout(animateQix, 1000 / 30); // ~30fps
    }
}
