clientMain()

async function clientMain() {
    const recvConnection = new RTCPeerConnection({ "iceServers": [] })
    ifNotNull(document.getElementById('submit-sdp'), (t) => { t.onclick = () => { onSdpSubmit(recvConnection) } })
    recvConnection.onicecandidate = async ev => { await recvConnection.addIceCandidate(ev.candidate!) }
    const recvChannel = recvConnection.createDataChannel('sendDataChannel')
    console.log('Created send data channel')
}

function ifNotNull<T, R>(t: T | null | undefined, x: (t: T) => R) {
    if (t == null || t == undefined) return null; else { return x(t) }
}

async function onSdpSubmit(recvConnection: RTCPeerConnection) {
    ifNotNull(document.getElementById('sdp') as HTMLTextAreaElement, async (elm) => {
        const sdp = elm.value
        console.log(`SDP=${sdp}`)
        let answer = new RTCSessionDescription({ type: 'answer', sdp: sdp, })
        // setAnswer(answer);
        await recvConnection.setRemoteDescription(answer)
    })
}

