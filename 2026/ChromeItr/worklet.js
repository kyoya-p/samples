// 🤖 AI 生成: 入力を 100ms (16kHz = 1600 サンプル) ごとにまとめてメインへ渡す
class PcmTap extends AudioWorkletProcessor {
  constructor() { super(); this.buf = new Float32Array(1600); this.n = 0; }
  process(inputs) {
    const ch = inputs[0] && inputs[0][0];
    if (ch) {
      for (let i = 0; i < ch.length; i++) {
        this.buf[this.n++] = ch[i];
        if (this.n === this.buf.length) {
          this.port.postMessage(this.buf, [this.buf.buffer]);
          this.buf = new Float32Array(1600);
          this.n = 0;
        }
      }
    }
    return true;
  }
}
registerProcessor('pcm-tap', PcmTap);
