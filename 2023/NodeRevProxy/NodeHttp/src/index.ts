import http from 'http'
import httpProxy from 'http-proxy'

var proxy = httpProxy.createProxyServer({})

var server = http.createServer(function (req, res) {
  try{
  console.log(`Req: ${req.url}`)
  proxy.web(req, res, { target: 'http://jp.sharp' })
  }catch(ex){
  }
})

console.log("listening on port 8080")
server.listen(8080)

