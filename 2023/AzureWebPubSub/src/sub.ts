import { WebSocket } from "ws"
// import { WebPubSubServiceClient } from "@azure/web-pubsub"

 function main() {
  const hub = "hub1"
  //  let service = new WebPubSubServiceClient(process.env.WebPubSubConnectionString, hub)
  // // let service = new WebPubSubServiceClient("Endpoint=https://k230116-pubsub.webpubsub.azure.com;AccessKey=uE9qsmpKl9nN2K9L4FseDvt4Jr4B7yxRelebBdfJDs8=;Version=1.0;", hub)
  // let token = await service.getClientAccessToken()
  // console.log(token.url)
  // let ws = new WebSocket(token.url)
  let ws = new WebSocket('wss://k230116-pubsub.webpubsub.azure.com/client/hubs/hub1?access_token=eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJhdWQiOiJ3c3M6Ly9rMjMwMTE2LXB1YnN1Yi53ZWJwdWJzdWIuYXp1cmUuY29tL2NsaWVudC9odWJzL2h1YjEiLCJpYXQiOjE2NzUyNTE3NDUsImV4cCI6MTY3NTI1NTM0NSwicm9sZSI6WyJ3ZWJwdWJzdWIuc2VuZFRvR3JvdXAiLCJ3ZWJwdWJzdWIuam9pbkxlYXZlR3JvdXAiXX0.6-krUpUBsqzG8SfT6XWEnucLosEDn9J2WSnAEXgh1BA')
  ws.on('open', () => console.log('connected'))
  ws.on('message', data => console.log('Message received: %s', data))
}

main()
