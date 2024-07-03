import { MessageEvent, WebSocket } from 'ws';

const url=``
const ws = new WebSocket(url)

ws.onopen = async () => {
  async function sleep(t: number) { await new Promise(resolve => setTimeout(resolve, t)) }
  console.log('Connected to server');
  for (let i = 0; i < 99; ++i) {
    for (let j = 0; j < 9; ++j) {
      ws.send(`${i}-${j}`)
    }
    ws.send(``)
    await sleep(5000)
  }
};

ws.onmessage = async (event: MessageEvent) => { console.log(`Received: ${event.data}`); };

ws.onclose = async () => { console.log('Connection closed'); };
