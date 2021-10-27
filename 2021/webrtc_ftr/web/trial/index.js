"user strict";

const mediaStreamConstraints = {
  video: true
};

const localVideo = document.querySelector("video");

function gotLocalMediaStream(mediaStream) {
  const localStream = mediaStream;
  localVideo.srcObject = mediaStream;
}

function handleLocalMediaStreamError(error) {
  console.log("navigator.getUserMedia error: ", error);
}

navigator.mediaDevices
  .getDisplayMedia(mediaStreamConstraints)
  .then(gotLocalMediaStream)
  .catch(handleLocalMediaStreamError);
