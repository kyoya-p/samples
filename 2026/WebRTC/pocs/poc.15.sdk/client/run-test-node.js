/**
 * Pure Node.js SDK Test Client
 */
const WebRTCChannel = require('../sdk/webrtc-channel');

const signalingUrl = process.env.SIGNALING_URL || 'http://host.docker.internal:49880';
const roomID = process.env.ROOM_ID || 'ROOM_'+Math.random().toString(36).substring(2,2);
const clientId =Math.random().toString(36).substring(2, 8);

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
    console.log(`[CID:${clientId}] Starting Pure Node.js SDK test in room: ${roomID}`);
    await sdk.join(roomID);

    let sentCount = 0;
    const maxMessages = 10;

    // 定期的にメッセージ送信を試行
    let interval = setInterval(() => {
        const timestamp = new Date().toLocaleTimeString();
        const randomStr = Math.random().toString(36).substring(2, 12);
        // クライアントIDをメッセージに含める
        const msg = `[RANDOM_MSG] CID:${clientId} MSG:${randomStr} (at ${timestamp})`;

        if (sdk.send(msg)) {
            console.log(`[SENT] ${msg}`);
        } else {
            console.log(`[TRY_SENT] ${msg}`); // 相手がいない場合は試行のみログ
        }
        sentCount++;

        if (sentCount >= maxMessages) {
            clearInterval(interval);
            console.log(`Finished ${maxMessages} send attempts. Test finished.`);
            setTimeout(() => process.exit(0), 2000);
        }
    }, 2000);

    // 安全のためのタイムアウト
    setTimeout(() => {
        clearInterval(interval);
        console.log("Test timed out.");
        process.exit(1);
    }, 45000);
})();
