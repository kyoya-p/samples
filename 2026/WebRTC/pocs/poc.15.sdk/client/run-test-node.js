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

    let sentCount = 0;
    const maxMessages = 10;

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
            sentCount++;
        }

        if (sentCount >= maxMessages) {
            clearInterval(interval);
            console.log(`Successfully sent ${maxMessages} messages. Test finished.`);
            setTimeout(() => process.exit(0), 2000); // 残りの受信を少し待ってから終了
        }
    }, 2000); // 2秒間隔に短縮

    // 安全のためのタイムアウト (相手が現れない場合)
    setTimeout(() => {
        clearInterval(interval);
        console.log("Test timed out.");
        process.exit(1);
    }, 45000);
})();
