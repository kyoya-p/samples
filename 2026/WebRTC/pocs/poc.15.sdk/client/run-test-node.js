/**
 * Pure Node.js SDK Test Client
 */
const WebRTCChannel = require('../sdk/webrtc-channel');

const signalingUrl = process.env.SIGNALING_URL || 'http://host.docker.internal:49880';
const roomID = process.env.ROOM_ID || 'room1';
const hostname = process.env.HOSTNAME || 'node-client';

const sdk = new WebRTCChannel(signalingUrl, {
    iceTransportPolicy: 'all',
    iceServers: [
        { urls: 'stun:stun.l.google.com:19302' },
        {
            urls: 'turn:turn:3478?transport=udp',
            username: 'user',
            credential: 'password123'
        }
    ]
});

sdk.onLog = (msg) => console.log(`[SDK] ${msg}`);
sdk.onMessage = (from, data) => console.log(`[MSG from ${from}] ${data}`);

let peerCount = 0;
sdk.onStatusChange = (status) => {
    console.log(`[STATUS] ${JSON.stringify(status)}`);
    peerCount = status.peerCount;
};

(async () => {
    console.log(`[${hostname}] Starting Pure Node.js SDK test in room: ${roomID}`);
    await sdk.join(roomID);

    // 定期的にメッセージ送信を試行
    let interval = setInterval(() => {
        if (peerCount === 0) {
            return;
        }
        const timestamp = new Date().toLocaleTimeString();
        const randomMsg = Math.random().toString(36).substring(2, 12) + Math.random().toString(36).substring(2, 12);
        const msg = `[RANDOM_MSG] ${randomMsg} (at ${timestamp})`;
        if (sdk.send(msg)) {
            console.log(`[SENT] ${msg}`);
        }
    }, 5000);

    // 60秒後に終了
    setTimeout(() => {
        clearInterval(interval);
        console.log("Test cycle finished.");
        process.exit(0);
    }, 60000);
})();
