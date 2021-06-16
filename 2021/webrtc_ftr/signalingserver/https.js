//const https = require('https');
const http = require('http');
const fs = require('fs');
//const sslServerKey = 'server-key.pem';
//const sslServerCrt = 'server-crt.pem';
const port = 3080;

const options = {
//    key: fs.readFileSync(sslServerKey),
//    cert: fs.readFileSync(sslServerCrt)
};

const server = http.createServer(options,
    (request, response) => {
        fs.readFile('./index.html', 'UTF-8',
        (error, data) => {
            response.writeHead(200, {'Content-Type':'text/html'});
            response.write(data);
            response.end();
        });
    }
);
server.listen(port);
console.log('server running port:'+port)
