serverMain()

async function serverMain() {
    const peerConnection = new RTCPeerConnection({ "iceServers": [] })
    peerConnection.onicecandidate = async ev => { await peerConnection.addIceCandidate(ev.candidate!) }
    const sendChannel = peerConnection.createDataChannel('sendDataChannel')
    console.log('Created send data channel');
    await connect(peerConnection)

    var count = 0
    setInterval(async () => { broadcast(sendChannel, `${count++}\n`) }, 100)
}
async function connect(peerConnection: RTCPeerConnection) {
    const sessionDescription = await peerConnection.createOffer()
    peerConnection.setLocalDescription(sessionDescription)
    const elmSdp = document.getElementById('sdp') as HTMLTextAreaElement | null
    elmSdp!["innerHTML"] = sessionDescription.sdp ?? "Error"
    console.log(sessionDescription.sdp)
}

async function broadcast(dataChannel: RTCDataChannel, data: string) {
    // const elmSend = document.getElementById('send') as HTMLTextAreaElement | null
    if (dataChannel.readyState === "open") {
        document.getElementById('send')!["innerHTML"] = data
        dataChannel.send(data)
    }else{
        document.getElementById('send')!["innerHTML"] = `${data}<<Data channel is not OPENED.>>`
    }
}


