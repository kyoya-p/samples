clientMain()

const sdpField = document.getElementById('sdp') as HTMLTextAreaElement
const answerField = document.getElementById('sdp-answer') as HTMLTextAreaElement

async function clientMain() {

    const recvConnection = new RTCPeerConnection({ "iceServers": [] })
    ifNotNull(document.getElementById('submit-sdp'), (t) => { t.onclick = () => { onSdpSubmit(recvConnection) } })
    // recvConnection.onicecandidate = ev => { recvConnection.addIceCandidate(ev.candidate!) }
    console.log('Created send data channel')
}

function ifNotNull<T, R>(t: T | null | undefined, x: (t: T) => R) {
    if (t == null || t == undefined) return null; else { return x(t) }
}

async function onSdpSubmit(recvConnection: RTCPeerConnection) {
    const sdp = sdpField.value
    console.log(`SDP=${sdp}`)
    const offer = new RTCSessionDescription({ type: 'offer', sdp: sdp, })
    await recvConnection.setRemoteDescription(offer)
    const answer = await recvConnection.createAnswer()
    answerField.value = answer.sdp ?? "Error"
}

function getSdpWithCandidates(peer: RTCPeerConnection) {
    return new Promise<string>(resolve =>
        peer.onicecandidate = ev => {
            if (ev.candidate == null) {
                resolve(peer.localDescription?.sdp ?? "")
            }
        }
    )
}

