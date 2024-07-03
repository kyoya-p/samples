import { WebPubSubServiceClient } from '@azure/web-pubsub';

main()

async function main() {
  const connStr = process.argv[2]
  const hub = process.argv[3] ?? "default"

  let serviceClient = new WebPubSubServiceClient(connStr, hub,);  

  for (let i = 0; i < 100; ++i) {
    for (let j = 0; j < 10; ++j) {
      let msg = `${i}-${j}`
      serviceClient.sendToAll(msg, { contentType: "text/plain" });
      console.log(msg)
    }
    await sleep(5000)
  }
}

async function sleep(t: number) { await new Promise(resolve => setTimeout(resolve, t)) }
