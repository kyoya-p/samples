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
clientMain();
function clientMain() {
    return __awaiter(this, void 0, void 0, function* () {
        const recvConnection = new RTCPeerConnection({ "iceServers": [] });
        ifNotNull(document.getElementById('submit-sdp'), (t) => { t.onclick = () => { onSdpSubmit(recvConnection); }; });
        recvConnection.onicecandidate = (ev) => __awaiter(this, void 0, void 0, function* () { yield recvConnection.addIceCandidate(ev.candidate); });
        const recvChannel = recvConnection.createDataChannel('sendDataChannel');
        console.log('Created send data channel');
    });
}
function ifNotNull(t, x) {
    if (t == null || t == undefined)
        return null;
    else {
        return x(t);
    }
}
function onSdpSubmit(recvConnection) {
    return __awaiter(this, void 0, void 0, function* () {
        ifNotNull(document.getElementById('sdp'), (elm) => __awaiter(this, void 0, void 0, function* () {
            const sdp = elm.value;
            console.log(`SDP=${sdp}`);
            let answer = new RTCSessionDescription({ type: 'answer', sdp: sdp, });
            // setAnswer(answer);
            yield recvConnection.setRemoteDescription(answer);
        }));
    });
}
