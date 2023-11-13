import { WebSocket } from "ws"

 function main() {
  const url=  process.env.WebPubSubUrl ?? ""
  let ws = new WebSocket(url)
  ws.on('open', () => console.log('connected'))
  ws.on('message', data => console.log('Message received: %s', data))
}

main()
