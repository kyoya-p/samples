import { WebSocket } from "ws"

function main() {
  const ws = new WebSocket('wss://k230116-pubsub.webpubsub.azure.com/client/hubs/hub1?access_token=eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJhdWQiOiJ3c3M6Ly9rMjMwMTE2LXB1YnN1Yi53ZWJwdWJzdWIuYXp1cmUuY29tL2NsaWVudC9odWJzL2h1YjEiLCJpYXQiOjE2NzUyNTE3NDUsImV4cCI6MTY3NTI1NTM0NSwicm9sZSI6WyJ3ZWJwdWJzdWIuc2VuZFRvR3JvdXAiLCJ3ZWJwdWJzdWIuam9pbkxlYXZlR3JvdXAiXX0.6-krUpUBsqzG8SfT6XWEnucLosEDn9J2WSnAEXgh1BA')

  ws.on('open', () => {
    let t = setInterval(() => {
      let m = `Hello: ${Date()}`
      ws.send(m)
      console.log(m)
    }
      , 1000
    )
  })

  ws.on('message', (data) => {
    console.log('received: %s', data)
  })
}

main()
