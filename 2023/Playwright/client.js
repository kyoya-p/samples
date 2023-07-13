setInterval(reloadImage, 2000)

function reloadImage() {
  fetch(`/hash`).then(res => res.json()).then((data) => { flushImage(data.hash); })
}

function flushImage(hash) {
  document.getElementById("img").style.backgroundImage = `url(/result/screenshot.png?${hash})`;
}

function handleClick(ev) {
  fetch(`/click?x=${ev.offsetX}&y=${ev.offsetY}`).then(res => res.json()).then((data) => {
    flushImage(data.hash);
  });
};
