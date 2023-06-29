setInterval(async () => { window.location.reload() }, 2000)

function handleClick(ev) {
  fetch(`click?x=${ev.clientX}&y=${ev.clientY}`).then(response => response.json())
    .then((data) => { window.location.reload()})
    .catch(error => console.log(error));
};
