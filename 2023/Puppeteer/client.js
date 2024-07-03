function handleMouseEvent(eventName, event) {
  const x = event.clientX;
  const y = event.clientY;
  const url = `op/${eventName}?x=${x}&y=${y}`;
  console.log(`CALL: ${url}`);
  fetch(url)
    .then(response => response.json())
    .then((data) => {
      // reloadImage(data.img)
    })
    .catch(error => console.error(error));
};


function handleClick(event) {
  const x = event.clientX;
  const y = event.clientY;
  const url = `op/mdown?x=${x}&y=${y}`;
  console.log(`CALL: ${url}`);
  fetch(url)
    .then(response => response.json())
    .then((data) => {
      // reloadImage(data.img)
    })
    .catch(error => console.error(error));
};

function handleMouseup(ev) { return handleMouseEvent("mouseup", ev) }
function handleMousedown(ev) { return handleMouseEvent("mousedown", ev) }
function handleMousemove(ev) {return handleMouseEvent("mousemove", ev) }


function reloadImage(imagePath) {
  const bg = document.getElementById("img");
  // img.src = imagePath
  // img.background = imagePath
  bg.style.backgroundImage = `url(${imagePath})`;
  bg.style.width=800;
  bg.style.height=800;
  console.log(`Reloaded ${imagePath}`);
}

var socket = io();

socket.on('connect', function () {
  console.log('Connected to server');
});

socket.on('server_message', function (message) {
  console.log(message);
  var img = document.getElementById("img");
  img.src = `${message}`
});