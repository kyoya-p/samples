import { WebSocket } from "ws"
import { WebPubSubServiceClient } from "@azure/web-pubsub"

async function main() {
  const hub = "hub1"
  // let service = new WebPubSubServiceClient(process.env.WebPubSubConnectionString, hub);
  let service = new WebPubSubServiceClient("Endpoint=https://k230116-pubsub.webpubsub.azure.com;AccessKey=uE9qsmpKl9nN2K9L4FseDvt4Jr4B7yxRelebBdfJDs8=;Version=1.0;", hub)

  // by default it uses `application/json`, specify contentType as `text/plain` if you want plain-text
  // service.sendToAll(process.argv[2], { contentType: "text/plain" });

  setInterval(
    () => service.sendToAll(Date(), { contentType: "text/plain" })
    , 1000 * 60 * 5
  )

  // let token = await service.getClientAccessToken()
  // console.log(token.url)
  // let ws = new WebSocket(token.url)
  // ws.on('open', () => {
  //   console.log('connected')
  //   ws.send(JSON.stringify({
  //     type: 'sendToGroup',
  //     event: 'message',
  //     dataType: 'json',
  //     data: ['Hello WS']
  //   }))
  // })

}

main()
