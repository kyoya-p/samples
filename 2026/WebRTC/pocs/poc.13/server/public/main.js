let peers = {}; // id -> { pc, dc }
let socket;
let roomName;

const turnHost = location.hostname === 'server' ? 'turn' : location.hostname;
const config = {
    iceServers: [
        { urls: 'stun:stun.l.google.com:19302' },
        {
            urls: [
                `turn:${turnHost}:3478?transport=tcp`,
                `turn:${turnHost}:3478?transport=udp`
            ],
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
    if (!text) return;
    
    let sent = false;
    for (const id in peers) {
        const dc = peers[id].dc;
        if (dc && dc.readyState === 'open') {
            dc.send(text);
            sent = true;
        }
    }
    
    if (sent) {
        appendMessage(text, 'local');
        chatInput.value = '';
    }
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
        for (const id in peers) {
            peers[id].pc.close();
        }
        peers = {};
        updateOverallStatus();
    });

    socket.on('user-joined', (id) => {
        sigLog(`New user joined room: ${id}`);
        initiateCall(id);
    });

    socket.on('offer', async ({ offer, from }) => {
        sigLog(`Received offer from: ${from}`);
        const pc = setupPC(from);
        await pc.setRemoteDescription(new RTCSessionDescription(offer));
        const answer = await pc.createAnswer();
        await pc.setLocalDescription(answer);
        
        const finalAnswer = {
            type: answer.type,
            sdp: filterSdp(answer.sdp)
        };
        sigLog(`Emitting answer to: ${from}`);
        socket.emit('answer', { answer: finalAnswer, to: from });
    });

    socket.on('answer', async ({ answer, from }) => {
        sigLog(`Received answer from: ${from}`);
        const pc = peers[from]?.pc;
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
        const pc = peers[from]?.pc;
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

async function initiateCall(id) {
    const pc = setupPC(id);
    const dc = pc.createDataChannel("chat");
    peers[id].dc = dc;
    setupDataChannel(dc, id);
    
    const offer = await pc.createOffer();
    await pc.setLocalDescription(offer);
    
    const finalOffer = {
        type: offer.type,
        sdp: filterSdp(offer.sdp)
    };
    sigLog(`Emitting offer to: ${id}`);
    socket.emit('offer', { offer: finalOffer, to: id });
}

function setupPC(id) {
    if (peers[id]) return peers[id].pc;
    
    const pc = new RTCPeerConnection(config);
    peers[id] = { pc: pc, dc: null };
    
    pc.onicecandidate = (event) => {
        if (event.candidate) {
            const cand = event.candidate;
            const div = document.createElement('div');
            div.className = `ice-item ${cand.type}`;
            div.innerHTML = `<span>${cand.type}</span> ${cand.protocol}://${cand.address || cand.ip}:${cand.port}`;
            iceList.appendChild(div);
            
            if (socket) {
                if (noHostCheck.checked && cand.candidate.includes('typ host')) {
                } else {
                    sigLog(`Emitting local ice-candidate: ${cand.type} (${cand.address || cand.ip}) to ${id}`);
                    socket.emit('ice-candidate', { candidate: cand, to: id });
                }
            }
        }
    };

    pc.oniceconnectionstatechange = () => {
        updateOverallStatus();
    };

    pc.onconnectionstatechange = () => {
        updateOverallStatus();
    };

    pc.ondatachannel = (event) => {
        sigLog(`Received remote DataChannel from ${id}`);
        peers[id].dc = event.channel;
        setupDataChannel(event.channel, id);
    };

    return pc;
}

function setupDataChannel(dc, id) {
    dc.onopen = () => {
        sigLog(`DataChannel opened with ${id}`);
        updateOverallStatus();
    };
    dc.onclose = () => {
        sigLog(`DataChannel closed with ${id}`);
        updateOverallStatus();
    };
    dc.onmessage = (event) => {
        sigLog(`Message received via DataChannel from ${id}`);
        appendMessage(event.data, 'remote');
    };
}

function updateOverallStatus() {
    let anyOpen = false;
    let anyConnected = false;
    let anyChecking = false;

    for (const id in peers) {
        const pc = peers[id].pc;
        const dc = peers[id].dc;
        
        if (pc.iceConnectionState === 'connected' || pc.iceConnectionState === 'completed') anyConnected = true;
        if (pc.iceConnectionState === 'checking') anyChecking = true;
        
        if (dc && dc.readyState === 'open') anyOpen = true;
    }

    // ICE Status
    if (anyConnected) {
        statusText.innerHTML = `<span class="dot" style="background:#22c55e"></span> ICE: connected`;
    } else if (anyChecking) {
        statusText.innerHTML = `<span class="dot" style="background:#eab308"></span> ICE: checking`;
    } else {
        statusText.innerHTML = `<span class="dot" style="background:#ef4444"></span> ICE: disconnected`;
    }

    // DC Status
    if (anyOpen) {
        dcStatus.innerHTML = `<span class="dot" style="background:#22c55e"></span> DataChannel: open`;
        chatInput.disabled = false;
        sendBtn.disabled = false;
        chatInput.placeholder = "Type a message...";
    } else {
        dcStatus.innerHTML = `<span class="dot" style="background:#ef4444"></span> DataChannel: closed`;
        chatInput.disabled = true;
        sendBtn.disabled = true;
        chatInput.placeholder = "Awaiting connection...";
    }
}
