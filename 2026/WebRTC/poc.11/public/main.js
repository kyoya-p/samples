let localStream;
let pc;
const config = { iceServers: [{ urls: 'stun:stun.l.google.com:19302' }] };
const sdpArea = document.getElementById('sdpArea');
const iceList = document.getElementById('iceList');
const statusText = document.getElementById('connStatus');
const localVideo = document.getElementById('localVideo');
const noHostCheck = document.getElementById('noHostCheck');

let qixContext = {
    active: false,
    lineSets: [],
    canvas: null,
    ctx: null
};

window.addEventListener('load', () => {
    initQixCanvas();
    
    // ページ読み込み時に最初のQixラインを自動開始
    qixContext.active = true;
    localStream = qixContext.canvas.captureStream(30);
    localVideo.srcObject = localStream;
    
    // 画面中央を起点に最初の1本を生成
    addNewLineSet(qixContext.canvas.width / 2, qixContext.canvas.height / 2);
    animateQix();

    noHostCheck.addEventListener('change', () => {
        document.body.classList.toggle('hide-host', noHostCheck.checked);
        if (pc && pc.localDescription) sdpArea.value = getProcessedSDP();
    });
});

localVideo.addEventListener('mousedown', (e) => {
    if (!qixContext.active) {
        qixContext.active = true;
        localStream = qixContext.canvas.captureStream(30);
        localVideo.srcObject = localStream;
        animateQix();
    }
    const rect = localVideo.getBoundingClientRect();
    const x = (e.clientX - rect.left) * (qixContext.canvas.width / rect.width);
    const y = (e.clientY - rect.top) * (qixContext.canvas.height / rect.height);
    addNewLineSet(x, y);
});

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
    if (qixContext.active) requestAnimationFrame(animateQix);
}

function getProcessedSDP() {
    if (!pc || !pc.localDescription) return "";
    let sdpObj = JSON.parse(JSON.stringify(pc.localDescription));
    if (noHostCheck.checked) {
        sdpObj.sdp = sdpObj.sdp.split('\n').filter(line => !line.includes('typ host')).join('\n');
    }
    return JSON.stringify(sdpObj);
}

document.getElementById('createOfferBtn').onclick = async () => {
    setupPC();
    const offer = await pc.createOffer();
    await pc.setLocalDescription(offer);
    pc.onicegatheringstatechange = () => {
        if (pc.iceGatheringState === 'complete') {
            sdpArea.value = getProcessedSDP();
            sdpArea.select();
        }
    };
    setTimeout(() => { if(pc.iceGatheringState !== 'complete') sdpArea.value = getProcessedSDP(); }, 3000);
};

document.getElementById('createAnswerBtn').onclick = async () => {
    if (!sdpArea.value) return alert("Please paste Offer SDP first");
    setupPC();
    try {
        const offer = JSON.parse(sdpArea.value);
        await pc.setRemoteDescription(new RTCSessionDescription(offer));
        const answer = await pc.createAnswer();
        await pc.setLocalDescription(answer);
        pc.onicegatheringstatechange = () => {
            if (pc.iceGatheringState === 'complete') {
                sdpArea.value = getProcessedSDP();
                sdpArea.select();
            }
        };
        setTimeout(() => { if(pc.iceGatheringState !== 'complete') sdpArea.value = getProcessedSDP(); }, 3000);
    } catch (e) { alert("Invalid Offer SDP"); }
};

document.getElementById('setAnswerBtn').onclick = async () => {
    try {
        const answer = JSON.parse(sdpArea.value);
        await pc.setRemoteDescription(new RTCSessionDescription(answer));
    } catch (e) { alert("Invalid Answer SDP"); }
};

function setupPC() {
    pc = new RTCPeerConnection(config);
    iceList.innerHTML = "";
    localStream.getTracks().forEach(track => pc.addTrack(track, localStream));
    pc.onicecandidate = (event) => {
        if (event.candidate) {
            const cand = event.candidate;
            const div = document.createElement('div');
            div.className = `ice-item ${cand.type}`;
            div.innerHTML = `<span>${cand.type}</span> ${cand.protocol}://${cand.address || cand.ip}:${cand.port}`;
            iceList.appendChild(div);
        }
    };
    pc.oniceconnectionstatechange = () => {
        const state = pc.iceConnectionState;
        statusText.innerHTML = `<span class="status-dot"></span> ICE Connection: ${state}`;
        const dot = statusText.querySelector('.status-dot');
        if (state === 'connected' || state === 'completed') dot.style.background = '#22c55e';
        else if (state === 'checking') dot.style.background = '#eab308';
        else dot.style.background = '#ef4444';
    };
    pc.ontrack = (event) => {
        document.getElementById('remoteVideo').srcObject = event.streams[0];
    };
}
