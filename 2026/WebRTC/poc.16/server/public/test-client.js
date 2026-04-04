/**
 * Test Client for WebRTCSDK
 */
const signalingUrl = window.location.origin;
const roomID = new URLSearchParams(window.location.search).get('room') || 'room1';

const sdk = new WebRTCSDK(signalingUrl, {
    iceServers: [
        { urls: 'stun:stun.l.google.com:19302' },
        {
            urls: [`turn:${location.hostname === 'server' ? 'turn' : location.hostname}:3478?transport=udp`],
            username: 'user',
            credential: 'password123'
        }
    ]
});

const iceStatusEl = document.getElementById('iceStatus');
const dcStatusEl = document.getElementById('dcStatus');
const peerCountEl = document.getElementById('peerCount');
const msgInput = document.getElementById('msgInput');
const sendBtn = document.getElementById('sendBtn');
const logArea = document.getElementById('logArea');

sdk.onMessage = (from, data) => {
    appendLog(`[MSG from ${from}] ${data}`);
};

sdk.onStatusChange = (status) => {
    iceStatusEl.innerText = status.iceConnected ? 'connected' : 'disconnected';
    dcStatusEl.innerText = status.dcOpen ? 'open' : 'closed';
    peerCountEl.innerText = status.peerCount;

    msgInput.disabled = !status.dcOpen;
    sendBtn.disabled = !status.dcOpen;
};

sdk.onLog = (msg) => {
    appendLog(`[SDK] ${msg}`);
};

function appendLog(msg) {
    const div = document.createElement('div');
    div.innerText = `${new Date().toLocaleTimeString()} ${msg}`;
    logArea.appendChild(div);
    logArea.scrollTop = logArea.scrollHeight;
}

window.onload = () => {
    appendLog(`Initializing SDK joining room: ${roomID}`);
    sdk.join(roomID);

    sendBtn.onclick = () => {
        const text = msgInput.value.trim();
        if (text && sdk.send(text)) {
            appendLog(`[SENT] ${text}`);
            msgInput.value = '';
        }
    };

    msgInput.onkeypress = (e) => {
        if (e.key === 'Enter') sendBtn.click();
    };
};
