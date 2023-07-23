serverMain()

async function serverMain() {
    function ifNotNull<T, R>(t: T | null | undefined, x: (t: T) => R) { if (t == null || t == undefined) return null; else { return x(t) } }

    const sendConnection = new RTCPeerConnection({ "iceServers": [] })
    sendConnection.onicecandidate = ev => {
        if (ev.candidate == null) {// nullはCandidate収集完了を示す、remoteに提示
            ifNotNull(document.getElementById('sdp'), elm => {
                elm.textContent = sendConnection.localDescription?.sdp ?? "ERROR"
            })
        }
    }
    const sendChannel = sendConnection.createDataChannel('sendDataChannel')
    console.log('Created send data channel');
    await connect(sendConnection)

    var count = 0
    setInterval(async () => { broadcast(sendChannel, `${count++}\n`) }, 100)
}
async function connect(sendConnection: RTCPeerConnection) {
    const sessionDescription = await sendConnection.createOffer()
    sendConnection.setLocalDescription(sessionDescription)
    console.log(sessionDescription.sdp)
}

async function broadcast(dataChannel: RTCDataChannel, data: string) {
    // const elmSend = document.getElementById('send') as HTMLTextAreaElement | null
    if (dataChannel.readyState === "open") {
        document.getElementById('send')!["innerHTML"] = data
        dataChannel.send(data)
    } else {
        document.getElementById('send')!["innerHTML"] = `${data}<<Data channel is not OPENED.>>`
    }
}

