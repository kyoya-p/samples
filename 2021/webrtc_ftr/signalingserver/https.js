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

const server = http.createServer(options,(request, response) => {
    const url = request.url;
    console.log('file:'+url);
    fs.readFile("."+url, 'UTF-8',
		(error, data) => {
		    console.log(error);
		    if(error!=null) {
			console.log('no file:'+url);
			response.writeHead(404);
			response.end();
		    }else{
			response.writeHead(200, {'Content-Type':'text/html'});
			response.write(data);
			response.end();
		    }
		});
});
server.listen(port);
console.log('server running port:'+port);

