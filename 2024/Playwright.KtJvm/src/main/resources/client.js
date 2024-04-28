function f1() {
    var ws= new WebSocket("ws://localhost:8000/ws");
    ws.send("close");
    ws.close();
}