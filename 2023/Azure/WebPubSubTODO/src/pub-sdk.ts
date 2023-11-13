import { WebPubSubServiceClient } from '@azure/web-pubsub'

const hub = "hub1";
const connStr = process.env.WebPubSubConnectionString ?? ""
console.log(`ConnectionString: ${connStr}`)
let service = new WebPubSubServiceClient(connStr, hub);

const m = `${Date()}`
service.sendToAll(m, { contentType: "text/plain" });