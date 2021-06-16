const https = require('https');
const fs = require('fs');
const sslServerKey = 'server-key.pem';
const sslServerCrt = 'server-crt.pem';

const options = {
    key: fs.readFileSync(sslServerKey),
    cert: fs.readFileSync(sslServerCrt)
};

const server = https.createServer(options,
    (request, response) => {
        fs.readFile('./index2.html', 'UTF-8',
        (error, data) => {
            response.writeHead(200, {'Content-Type':'text/html'});
            response.write(data);
            response.end();
        });
    }
);
server.listen(3001);
console.log('server running...')
