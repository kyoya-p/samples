import { WebPubSubServiceClient } from '@azure/web-pubsub';

main()

async function main() {
  const connStr = process.argv[2]
  const hub = process.argv[3]

  let serviceClient = new WebPubSubServiceClient(connStr, hub);

  for (let i = 0; i < 1000; ++i) {
    serviceClient.sendToAll(`${i}`, { contentType: "text/plain" });
    console.log(i)
    await sleep1s()
  }
}

async function sleep1s() { await new Promise(resolve => setTimeout(resolve, 1000)) }
