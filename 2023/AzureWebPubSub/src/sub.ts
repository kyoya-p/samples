import { WebSocket} from "ws"
import { WebPubSubServiceClient } from "@azure/web-pubsub"

async function main() {
  const hub = "hub1"
//  let service = new WebPubSubServiceClient(process.env.WebPubSubConnectionString, hub)
  let service = new WebPubSubServiceClient("Endpoint=https://k230116-pubsub.webpubsub.azure.com;AccessKey=uE9qsmpKl9nN2K9L4FseDvt4Jr4B7yxRelebBdfJDs8=;Version=1.0;", hub)
  let token = await service.getClientAccessToken()
  console.log(token.url)
  let ws = new WebSocket(token.url)
  ws.on('open', () => console.log('connected'))
  ws.on('message', data => console.log('Message received: %s', data))
}

main()
