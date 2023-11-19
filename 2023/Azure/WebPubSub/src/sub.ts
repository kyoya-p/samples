import { WebPubSubServiceClient } from "@azure/web-pubsub";
import { WebSocket } from "ws"

main()

async function main() {
  if (process.argv.length !== 4) {
    console.log('Usage: node subscribe <connection-string> <hub-name>');
    return 1;
  }

  const connStr = process.argv[2]
  const hub = process.argv[3]

  console.log(`connection-string: ${connStr}`)
  console.log(`hub-name: ${hub}`)

  const serviceClient = new WebPubSubServiceClient(connStr, hub)
  const token = await serviceClient.getClientAccessToken()
  const ws = new WebSocket(token.url)
  ws.on('open', () => console.log('connected'))
  ws.on('message', data => console.log(data))
}

