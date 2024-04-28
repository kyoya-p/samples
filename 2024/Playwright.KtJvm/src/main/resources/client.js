const ws= new WebSocket("ws://localhost:8000/ws");
ws.onmessage = (event) => {
    console.log(event.data);
}

