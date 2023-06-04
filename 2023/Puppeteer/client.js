function handleClick(event) {
  const x = event.clientX;
  const y = event.clientY;
  const url = `op/click?x=${x}&y=${y}`;
  console.log(`CALL: ${url}`);
  fetch(url)
    .then(response => response.json())
    // .then(reloadImage)
    .catch(error => console.error(error));
};

// function reloadImage() {
//   var img = document.getElementById("img");
//   img.src = img.src + "?t=" + new Date().getTime();
//   console.log(`Reloaded.`);
// }

var socket = io();

socket.on('connect', function () {
  console.log('Connected to server');
});

socket.on('server_message', function (message) {
  console.log(message);
  var img = document.getElementById("img");
  img.src = `${message}`
});