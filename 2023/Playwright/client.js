reloadImage()
setInterval(async () => { reloadImage() }, 2000)

function reloadImage() {
  fetch(`/hash`).then(res => res.json()).then((data) => {
    const bg = document.getElementById("img");
    bg.style.backgroundImage = `url(/result/screenshot.png?${data.hash})`;
    // bg.style.width = "100%";
    // bg.style.height = "100%";
  })
}

function handleClick(ev) {
  fetch(`/click?x=${ev.clientX}&y=${ev.clientY}`).then(res => res.json())
    .then((data) => { reloadImage() })
    .catch(err => console.log(err));
};
