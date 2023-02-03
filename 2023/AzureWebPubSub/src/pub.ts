import { WebSocket } from "ws"

function main() {
  const url=  process.env.WebPubSubUrl ?? ""
  const ws = new WebSocket(url)

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
