main()

async function main() {
    const peerConnection = new RTCPeerConnection({ "iceServers": [] })
    connect(peerConnection)
    setInterval(async (peerConnection: RTCPeerConnection) => { broadcast(peerConnection) },1000)
}
async function connect(peerConnection: RTCPeerConnection) {
    const sessionDescription = await peerConnection.createOffer()
    peerConnection.setLocalDescription(sessionDescription)
    const sdp = document.getElementById('sdp') as HTMLTextAreaElement | null
    console.log(sessionDescription.sdp)
    sdp!["innerHTML"] = sessionDescription.sdp ?? "Error"
}

async function broadcast(peerConnection: RTCPeerConnection) {

}