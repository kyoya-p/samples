"use strict";

//const https = require('https');
const http = require('http');
const fs = require('fs');
let WebSocketServer = require('ws').Server;
let port = 3010;

//var server = https.createServer({
//    key: fs.readFileSync('server-key.pem'),
//    cert: fs.readFileSync('server-crt.pem')
//});
var server = http.createServer({});

let wssServer = new WebSocketServer({server});
server.listen(port);
console.log('websocket server start. port=' + port);

wssServer.on('connection', function(ws) {
    console.log('-- websocket connected --');
    ws.on('message', function(message) {
        wssServer.clients.forEach(function each(client) {
            if (isSame(ws, client)) {
                console.log('- skip sender -');
            } else {
                client.send(message);
            }
        });
    });
});

function isSame(ws1, ws2) {
    return (ws1 === ws2);
}
