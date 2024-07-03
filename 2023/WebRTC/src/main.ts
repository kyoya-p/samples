/*
 *  Copyright (c) 2015 The WebRTC project authors. All Rights Reserved.
 *
 *  Use of this source code is governed by a BSD-style license
 *  that can be found in the LICENSE file in the root of the source
 *  tree.
 */

"use strict";
const MAX_CHUNK_SIZE = 262144;

let localConnection: RTCPeerConnection | null;
let remoteConnection: RTCPeerConnection | null;
let sendChannel: RTCDataChannel | null;
let receiveChannel: RTCDataChannel | null;
let chunkSize = 0;
let lowWaterMark = 0;
let highWaterMark = 0;
let dataString = "";
let timeoutHandle: any | null = null;
const megsToSend = document.querySelector(
  "input#megsToSend"
) as HTMLInputElement;
const sendButton = document.querySelector(
  "button#sendTheData"
) as HTMLButtonElement;
const orderedCheckbox = document.querySelector(
  "input#ordered"
) as HTMLInputElement;
const sendProgress = document.querySelector(
  "progress#sendProgress"
) as HTMLProgressElement;
const receiveProgress = document.querySelector(
  "progress#receiveProgress"
) as HTMLProgressElement;
const errorMessage = document.querySelector("div#errorMsg") as HTMLDivElement;
const transferStatus = document.querySelector(
  "span#transferStatus"
) as HTMLSpanElement;

const btnCreateConnection = document.querySelector(
  "button#createConnection"
) as HTMLButtonElement;
const btnCreateChannel = document.querySelector(
  "button#createChannel"
) as HTMLButtonElement;

let bytesToSend = 0;
let totalTimeUsedInSend = 0;
let numberOfSendCalls = 0;
let maxTimeUsedInSend = 0;
let sendStartTime = 0;
let currentThroughput = 0;

sendButton.addEventListener("click", createConnection2);

// Prevent data sent to be set to 0.
megsToSend.addEventListener("change", function () {
  const number = parseInt(this.value);
  if (Number.isNaN(number)) {
    errorMessage!.innerHTML = `Invalid value for MB to send: ${number}`;
  } else if (number <= 0) {
    sendButton.disabled = true;
    errorMessage!.innerHTML = "<p>Please enter a number greater than zero.</p>";
  } else if (number > 64) {
    sendButton.disabled = true;
    errorMessage!.innerHTML =
      "<p>Please enter a number lower or equal than 64.</p>";
  } else {
    errorMessage!.innerHTML = "";
    sendButton.disabled = false;
  }
});

// TODO
async function createConnection2() {
  console.log(`Create Connection`);

  const peerConfig = { iceServers: [] };
  localConnection = new RTCPeerConnection(peerConfig);
  // sendChannel.addEventListener('open', onSendChannelOpen)
  // sendChannel.addEventListener('close', onSendChannelClosed)

  // localConnection.addEventListener('icecandidate', e => onIceCandidate(localConnection!, e));
  localConnection.onicecandidate = async function (ev) {
    if (ev.candidate) {
      console.log(ev.candidate);
    } else {
      console.log("empty ice event (no more candidate)");
    }
  };
  await createOffer();
}

async function createChannel() {
  console.log(`Create Channel`);
  sendChannel = localConnection!.createDataChannel("sendDataChannel");
}

async function createOffer() {
  await localConnection!.createOffer();
  const offer = await localConnection!.createOffer();
  console.log(`Offer:\n${offer.sdp}`);
  await localConnection!.setLocalDescription(offer);
  return offer;
}

async function createConnection() {
  sendButton.disabled = true;
  megsToSend.disabled = true;

  let servers: RTCConfiguration | undefined;

  const number = Number.parseInt(megsToSend.value);
  bytesToSend = number * 1024 * 1024;

  localConnection = new RTCPeerConnection(servers);

  // Let's make a data channel!
  const dataChannelParams = { ordered: false };
  if (orderedCheckbox!.checked) {
    dataChannelParams.ordered = true;
  }
  sendChannel = localConnection.createDataChannel(
    "sendDataChannel",
    dataChannelParams
  );
  sendChannel.addEventListener("open", onSendChannelOpen);
  sendChannel.addEventListener("close", onSendChannelClosed);
  console.log("Created send data channel: ", sendChannel);

  console.log(
    "Created local peer connection object localConnection: ",
    localConnection
  );

  localConnection.addEventListener("icecandidate", (e) =>
    onIceCandidate(localConnection!, e)
  );

  remoteConnection = new RTCPeerConnection(servers);
  remoteConnection.addEventListener("icecandidate", (e) =>
    onIceCandidate(remoteConnection!, e)
  );
  remoteConnection.addEventListener("datachannel", receiveChannelCallback);

  try {
    const localOffer = await localConnection.createOffer();
    await handleLocalDescription(localOffer);
  } catch (e) {
    console.error("Failed to create session description: ", e);
  }

  transferStatus.innerHTML = "Peer connection setup complete.";
}

function sendData() {
  // Stop scheduled timer if any (part of the workaround introduced below)
  if (timeoutHandle !== null) {
    clearTimeout(timeoutHandle);
    timeoutHandle = null;
  }

  let bufferedAmount = sendChannel!.bufferedAmount;
  while (sendProgress!.value < sendProgress!.max) {
    transferStatus.innerText = "Sending data...";
    const timeBefore = performance.now();
    sendChannel!.send(dataString);
    const timeUsed = performance.now() - timeBefore;
    if (timeUsed > maxTimeUsedInSend) {
      maxTimeUsedInSend = timeUsed;
      totalTimeUsedInSend += timeUsed;
    }
    numberOfSendCalls += 1;
    bufferedAmount += chunkSize;
    sendProgress.value += chunkSize;

    // Pause sending if we reach the high water mark
    if (bufferedAmount >= highWaterMark) {
      // This is a workaround due to the bug that all browsers are incorrectly calculating the
      // amount of buffered data. Therefore, the 'bufferedamountlow' event would not fire.
      if (sendChannel!.bufferedAmount < lowWaterMark) {
        timeoutHandle = setTimeout(() => sendData(), 0);
      }
      console.log(
        `Paused sending, buffered amount: ${bufferedAmount} (announced: ${
          sendChannel!.bufferedAmount
        })`
      );
      break;
    }
  }

  if (sendProgress.value === sendProgress.max) {
    transferStatus.innerHTML = "Data transfer completed successfully!";
  }
}

function startSendingData() {
  transferStatus.innerHTML = "Start sending data.";
  sendProgress.max = bytesToSend;
  receiveProgress.max = sendProgress.max;
  sendProgress.value = 0;
  receiveProgress.value = 0;
  sendStartTime = performance.now();
  maxTimeUsedInSend = 0;
  totalTimeUsedInSend = 0;
  numberOfSendCalls = 0;
  sendData();
}

function maybeReset() {
  if (localConnection === null && remoteConnection === null) {
    sendButton.disabled = false;
    megsToSend.disabled = false;
  }
}

async function handleLocalDescription(desc: RTCSessionDescriptionInit) {
  localConnection!.setLocalDescription(desc);
  console.log("Offer from localConnection:\n", desc.sdp);
  remoteConnection!.setRemoteDescription(desc);
  try {
    const remoteAnswer = await remoteConnection!.createAnswer();
    handleRemoteAnswer(remoteAnswer);
  } catch (e) {
    console.error("Error when creating remote answer: ", e);
  }
}

function handleRemoteAnswer(desc: RTCSessionDescriptionInit) {
  remoteConnection!.setLocalDescription(desc);
  console.log("Answer from remoteConnection:\n", desc.sdp);
  localConnection!.setRemoteDescription(desc);
}

function getOtherPc(pc: RTCPeerConnection) {
  return pc === localConnection ? remoteConnection! : localConnection!;
}

async function onIceCandidate(
  pc: RTCPeerConnection,
  event: RTCPeerConnectionIceEvent
) {
  const candidate = event.candidate;
  if (candidate === null) {
    console.log(`onIceCandidate(): candidate === null`);
    return;
  } // Ignore null candidates
  try {
    await getOtherPc(pc).addIceCandidate(candidate);
    console.log("AddIceCandidate successful: ", candidate);
  } catch (e) {
    console.error("Failed to add Ice Candidate: ", e);
  }
}

function receiveChannelCallback(event: RTCDataChannelEvent) {
  console.log("Receive Channel Callback");
  receiveChannel = event.channel!;
  receiveChannel.binaryType = "arraybuffer";
  receiveChannel.addEventListener("close", onReceiveChannelClosed);
  receiveChannel.addEventListener("message", onReceiveMessageCallback);
}

function onReceiveMessageCallback(event: MessageEvent) {
  receiveProgress.value += event!.data.length;
  currentThroughput =
    receiveProgress.value / (performance.now() - sendStartTime);
  console.log("Current Throughput is:", currentThroughput, "bytes/sec");

  // Workaround for a bug in Chrome which prevents the closing event from being raised by the
  // remote side. Also a workaround for Firefox which does not send all pending data when closing
  // the channel.
  if (receiveProgress.value === receiveProgress.max) {
    sendChannel!.close();
    receiveChannel!.close();
  }
}

function onSendChannelOpen() {
  console.log("Send channel is open");

  chunkSize = Math.min(localConnection!.sctp!.maxMessageSize, MAX_CHUNK_SIZE);
  console.log("Determined chunk size: ", chunkSize);
  dataString = new Array(chunkSize).fill("X").join("");
  lowWaterMark = chunkSize; // A single chunk
  highWaterMark = Math.max(chunkSize * 8, 1048576); // 8 chunks or at least 1 MiB
  console.log("Send buffer low water threshold: ", lowWaterMark);
  console.log("Send buffer high water threshold: ", highWaterMark);
  sendChannel!.bufferedAmountLowThreshold = lowWaterMark;
  sendChannel!.addEventListener("bufferedamountlow", (e) => {
    console.log("BufferedAmountLow event:", e);
    sendData();
  });

  startSendingData();
}

function onSendChannelClosed() {
  console.log("Send channel is closed");
  localConnection!.close();
  localConnection = null;
  console.log("Closed local peer connection");
  maybeReset();
  console.log(
    "Average time spent in send() (ms): " +
      totalTimeUsedInSend / numberOfSendCalls
  );
  console.log("Max time spent in send() (ms): " + maxTimeUsedInSend);
  const spentTime = performance.now() - sendStartTime;
  console.log("Total time spent: " + spentTime);
  console.log("MBytes/Sec: " + bytesToSend / 1000 / spentTime);
}

function onReceiveChannelClosed() {
  console.log("Receive channel is closed");
  remoteConnection!.close();
  remoteConnection = null;
  console.log("Closed remote peer connection");
  maybeReset();
}
