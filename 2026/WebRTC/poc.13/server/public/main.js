let pc;
let socket;
let roomName;
let dataChannel;

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

const iceList = document.getElementById('iceList');
const statusText = document.getElementById('connStatus');
const dcStatus = document.getElementById('dcStatus');
const noHostCheck = document.getElementById('noHostCheck');
const signalingStatus = document.getElementById('signalingStatus');
const signalingDot = document.getElementById('signalingDot');
const roomInput = document.getElementById('roomInput');
const connectBtn = document.getElementById('connectBtn');
const logArea = document.getElementById('logArea');
const signalingUrlDisplay = document.getElementById('signalingUrl');

const chatList = document.getElementById('chatList');
const chatInput = document.getElementById('chatInput');
const sendBtn = document.getElementById('sendBtn');

// --- Logger ---
function sigLog(message, isError = false) {
    if (!logArea) return;
    const div = document.createElement('div');
    const time = new Date().toLocaleTimeString([], { hour12: false, hour: '2-digit', minute:'2-digit', second:'2-digit', fractionalSecondDigits: 3 });
    div.innerText = `[${time}] ${message}`;
    if (isError) div.style.color = '#dc2626';
    logArea.appendChild(div);
    logArea.scrollTop = logArea.scrollHeight;
}

function appendMessage(text, side) {
    const div = document.createElement('div');
    div.className = `message ${side}`;
    const time = new Date().toLocaleTimeString([], { hour: '2-digit', minute:'2-digit' });
    div.innerHTML = `<div class="meta">${side === 'local' ? 'You' : 'Remote'} • ${time}</div>${text}`;
    chatList.appendChild(div);
    chatList.scrollTop = chatList.scrollHeight;
}

window.addEventListener('load', () => {
    if (signalingUrlDisplay) {
        signalingUrlDisplay.innerText = `Signaling: ${window.location.origin}`;
    }

    noHostCheck.addEventListener('change', () => {
        document.body.classList.toggle('hide-host', noHostCheck.checked);
    });

    if (connectBtn) {
        connectBtn.onclick = () => {
            if (socket) {
                socket.disconnect();
                return;
            }
            roomName = roomInput.value;
            if (!roomName) return alert("Enter room name");
            
            initSignaling();
            connectBtn.innerText = 'Connecting...';
            roomInput.disabled = true;
        };
    }

    sendBtn.onclick = sendMessage;
    chatInput.onkeypress = (e) => {
        if (e.key === 'Enter') sendMessage();
    };
});

function sendMessage() {
    const text = chatInput.value.trim();
    if (!text || !dataChannel || dataChannel.readyState !== 'open') return;
    
    dataChannel.send(text);
    appendMessage(text, 'local');
    chatInput.value = '';
}

function initSignaling() {
    socket = io();
    
    socket.on('connect', () => {
        sigLog('Connected to signaling server');
        signalingStatus.querySelector('span:last-child').innerText = `Connected: ${roomName}`;
        signalingDot.style.background = '#22c55e';
        connectBtn.innerText = 'Disconnect';
        connectBtn.style.backgroundColor = 'var(--danger)';
        sigLog(`Emitting join: ${roomName}`);
        socket.emit('join', roomName);
    });

    socket.on('disconnect', (reason) => {
        sigLog(`Disconnected from signaling server: ${reason}`, true);
        signalingStatus.querySelector('span:last-child').innerText = 'Offline';
        signalingDot.style.background = '#ef4444';
        roomInput.disabled = false;
        connectBtn.innerText = 'Connect';
        connectBtn.style.backgroundColor = 'var(--accent)';
        socket = null;
        if (pc) {
            pc.close();
            pc = null;
        }
        updateDCStatus('closed');
    });

    socket.on('user-joined', (id) => {
        sigLog(`New user joined room: ${id}`);
        initiateCall();
    });

    socket.on('offer', async ({ offer, from }) => {
        sigLog(`Received offer from: ${from}`);
        setupPC();
        await pc.setRemoteDescription(new RTCSessionDescription(offer));
        const answer = await pc.createAnswer();
        await pc.setLocalDescription(answer);
        
        const finalAnswer = {
            type: answer.type,
            sdp: filterSdp(answer.sdp)
        };
        sigLog(`Emitting answer to: ${from}`);
        socket.emit('answer', { answer: finalAnswer, roomName });
    });

    socket.on('answer', async ({ answer, from }) => {
        sigLog(`Received answer from: ${from}`);
        if (pc) {
            await pc.setRemoteDescription(new RTCSessionDescription(answer));
        }
    });

    socket.on('ice-candidate', async ({ candidate, from }) => {
        if (!candidate || !candidate.candidate) return;

        if (noHostCheck.checked && candidate.candidate.includes('typ host')) {
            return;
        }
        sigLog(`Received ice-candidate from: ${from} (${candidate.candidate.split(' ')[7] || ''})`);
        if (pc) {
            try {
                await pc.addIceCandidate(new RTCIceCandidate(candidate));
            } catch (e) {}
        }
    });
}

function filterSdp(sdp) {
    if (!noHostCheck.checked) return sdp;
    return sdp.replace(/^a=candidate:.*typ host.*\r?\n/gm, '');
}

async function initiateCall() {
    setupPC();
    // 自分が発信側の場合はDataChannelを作成する
    setDataChannel(pc.createDataChannel("chat"));
    
    const offer = await pc.createOffer();
    await pc.setLocalDescription(offer);
    
    const finalOffer = {
        type: offer.type,
        sdp: filterSdp(offer.sdp)
    };
    sigLog(`Emitting offer for room: ${roomName}`);
    socket.emit('offer', { offer: finalOffer, roomName });
}

function setupPC() {
    if (pc) return;
    pc = new RTCPeerConnection(config);
    iceList.innerHTML = "";
    
    pc.onicecandidate = (event) => {
        if (event.candidate) {
            const cand = event.candidate;
            const div = document.createElement('div');
            div.className = `ice-item ${cand.type}`;
            div.innerHTML = `<span>${cand.type}</span> ${cand.protocol}://${cand.address || cand.ip}:${cand.port}`;
            iceList.appendChild(div);
            
            if (roomName && socket) {
                if (noHostCheck.checked && cand.candidate.includes('typ host')) {
                } else {
                    sigLog(`Emitting local ice-candidate: ${cand.type} (${cand.address || cand.ip})`);
                    socket.emit('ice-candidate', { candidate: cand, roomName });
                }
            }
        }
    };

    pc.oniceconnectionstatechange = () => {
        const state = pc.iceConnectionState;
        statusText.innerHTML = `<span class="dot"></span> ICE: ${state}`;
        const dot = statusText.querySelector('.dot');
        if (state === 'connected' || state === 'completed') {
            dot.style.background = '#22c55e';
            updateSelectedIce();
        }
        else if (state === 'checking') dot.style.background = '#eab308';
        else if (state === 'failed' || state === 'disconnected' || state === 'closed') dot.style.background = '#ef4444';
        else dot.style.background = '#94a3b8';
    };

    pc.onconnectionstatechange = () => {
        if (pc.connectionState === 'connected') {
            updateSelectedIce();
        }
    };

    // 相手側がDataChannelを受け取った時の処理
    pc.ondatachannel = (event) => {
        sigLog("Received remote DataChannel");
        setDataChannel(event.channel);
    };
}

function setDataChannel(dc) {
    dataChannel = dc;
    dataChannel.onopen = () => {
        sigLog("DataChannel opened");
        updateDCStatus('open');
    };
    dataChannel.onclose = () => {
        sigLog("DataChannel closed");
        updateDCStatus('closed');
    };
    dataChannel.onmessage = (event) => {
        sigLog("Message received via DataChannel");
        appendMessage(event.data, 'remote');
    };
}

function updateDCStatus(state) {
    dcStatus.innerHTML = `<span class="dot"></span> DataChannel: ${state}`;
    const dot = dcStatus.querySelector('.dot');
    if (state === 'open') {
        dot.style.background = '#22c55e';
        chatInput.disabled = false;
        sendBtn.disabled = false;
        chatInput.placeholder = "Type a message...";
    } else {
        dot.style.background = '#ef4444';
        chatInput.disabled = true;
        sendBtn.disabled = true;
        chatInput.placeholder = "Awaiting connection...";
    }
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
                    <span>LOCAL:</span> ${local.candidateType}<br>
                    <span>REMOTE:</span> ${remote.candidateType}
                </div>
            `;
        }
    } catch (e) {}
}
