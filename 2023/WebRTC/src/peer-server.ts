// import adapter from 'webrtc-adapter'

const peerConnection = new RTCPeerConnection({ "iceServers": [] })
connect()

async function connect() {
    const sessionDescription = await peerConnection.createOffer()
    peerConnection.setLocalDescription(sessionDescription)
    const sdp = document.getElementById('sdp') as HTMLTextAreaElement | null 
    console.log(sessionDescription.sdp)
    sdp!.innerHTML = sessionDescription.sdp ?? "Error"
}