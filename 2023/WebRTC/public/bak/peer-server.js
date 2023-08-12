"use strict";
var __awaiter = (this && this.__awaiter) || function (thisArg, _arguments, P, generator) {
    function adopt(value) { return value instanceof P ? value : new P(function (resolve) { resolve(value); }); }
    return new (P || (P = Promise))(function (resolve, reject) {
        function fulfilled(value) { try { step(generator.next(value)); } catch (e) { reject(e); } }
        function rejected(value) { try { step(generator["throw"](value)); } catch (e) { reject(e); } }
        function step(result) { result.done ? resolve(result.value) : adopt(result.value).then(fulfilled, rejected); }
        step((generator = generator.apply(thisArg, _arguments || [])).next());
    });
};
serverMain();
function serverMain() {
    return __awaiter(this, void 0, void 0, function* () {
        const peerConnection = new RTCPeerConnection({ "iceServers": [] });
        peerConnection.onicecandidate = (ev) => __awaiter(this, void 0, void 0, function* () { yield peerConnection.addIceCandidate(ev.candidate); });
        const sendChannel = peerConnection.createDataChannel('sendDataChannel');
        console.log('Created send data channel');
        yield connect(peerConnection);
        var count = 0;
        setInterval(() => __awaiter(this, void 0, void 0, function* () { broadcast(sendChannel, `${count++}\n`); }), 100);
    });
}
function connect(peerConnection) {
    var _a;
    return __awaiter(this, void 0, void 0, function* () {
        const sessionDescription = yield peerConnection.createOffer();
        peerConnection.setLocalDescription(sessionDescription);
        const elmSdp = document.getElementById('sdp');
        elmSdp["innerHTML"] = (_a = sessionDescription.sdp) !== null && _a !== void 0 ? _a : "Error";
        console.log(sessionDescription.sdp);
    });
}
function broadcast(dataChannel, data) {
    return __awaiter(this, void 0, void 0, function* () {
        // const elmSend = document.getElementById('send') as HTMLTextAreaElement | null
        if (dataChannel.readyState === "open") {
            document.getElementById('send')["innerHTML"] = data;
            dataChannel.send(data);
        }
        else {
            document.getElementById('send')["innerHTML"] = `${data}<<Data channel is not OPENED.>>`;
        }
    });
}
