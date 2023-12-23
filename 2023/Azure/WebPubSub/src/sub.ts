import { WebPubSubServiceClient } from "@azure/web-pubsub";
import { WebSocket } from "ws"

main()

async function main() {
  const connStr = process.argv[2]
  const hub = process.argv[3] ?? "default"

  const serviceClient = new WebPubSubServiceClient(connStr, hub)
  const token = await serviceClient.getClientAccessToken()
  console.log(`url:${token.url}`)
  const ws = new WebSocket(token.url)
  ws.on('open', () => console.log('connected'))
  ws.on('message', data => console.log(data.toString()))
}

