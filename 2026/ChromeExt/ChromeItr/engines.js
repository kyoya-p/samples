// 🤖 AI 生成: 文字起こしエンジン (Gemini Nano / Whisper)。入力は 16kHz モノラルの Float32Array

const SAMPLE_RATE = 16000;

// transformers.js (同梱バンドル) の読込と環境設定。Whisper と翻訳で共有する
let tfPromise = null;
export function loadTransformers() {
  tfPromise ??= (async () => {
    let mod;
    try {
      mod = await import('./lib/transformers.bundle.js');
    } catch (e) {
      throw new Error('lib/ の読込失敗。未生成なら `mise run build` を実行 (' + (e.message || e) + ')');
    }
    const { env } = mod;
    // MV3 の CSP は blob: 読込を許可しないため wasm キャッシュ(blob 化)を無効にし、同梱ファイルを直接読む
    env.allowLocalModels = false;
    env.useWasmCache = false;
    const base = chrome.runtime.getURL('lib/');
    env.backends.onnx.wasm.wasmPaths = {
      mjs: base + 'ort-wasm-simd-threaded.asyncify.mjs',
      wasm: base + 'ort-wasm-simd-threaded.asyncify.wasm',
    };
    env.backends.onnx.wasm.numThreads = 1;
    return mod;
  })();
  tfPromise.catch(() => { tfPromise = null; });
  return tfPromise;
}

// 同じ語句が 4 回以上連続する反復 (Whisper の幻覚) を 1 回に圧縮する
export function collapseRepeats(text) {
  return text.replace(/(.{2,20}?)(?:[、。,.\s]*\1){3,}/gu, '$1');
}

export class NanoEngine {
  name = 'Gemini Nano';

  async init(onStatus) {
    if (typeof LanguageModel === 'undefined') throw new Error('LanguageModel API がない (Chrome 138 以上が必要)');
    this.opts = {
      expectedInputs: [{ type: 'text', languages: ['en'] }, { type: 'audio' }],
      expectedOutputs: [{ type: 'text', languages: ['en', 'ja'] }],
    };
    const availability = await LanguageModel.availability(this.opts);
    if (availability === 'unavailable') throw new Error('Gemini Nano の音声入力は利用不可 (availability=unavailable)');
    this.base = await LanguageModel.create({
      ...this.opts,
      initialPrompts: [{
        role: 'system',
        content: 'You are a speech transcription engine. Output only the verbatim transcript of the audio, in the language spoken. No commentary, no translation. If there is no intelligible speech, output nothing.',
      }],
      monitor(m) {
        m.addEventListener('downloadprogress', (e) => onStatus(`Gemini Nano モデル取得中 ${Math.round(e.loaded * 100)}%`));
      },
    });
  }

  async transcribe(samples) {
    const buf = new AudioBuffer({ length: samples.length, sampleRate: SAMPLE_RATE, numberOfChannels: 1 });
    buf.copyToChannel(samples, 0);
    const session = await this.base.clone();
    try {
      const out = await session.prompt([{
        role: 'user',
        content: [
          { type: 'text', value: 'Transcribe this audio.' },
          { type: 'audio', value: buf },
        ],
      }]);
      return (out || '').trim();
    } finally {
      session.destroy();
    }
  }
}

export class WhisperEngine {
  name = 'Whisper';

  constructor(model, language) {
    this.model = model;
    this.language = language; // 'ja' など。'auto' で自動判定
  }

  async init(onStatus) {
    const { pipeline } = await loadTransformers();

    let webgpu = false;
    try { webgpu = !!(navigator.gpu && (await navigator.gpu.requestAdapter())); } catch { /* CPU へ */ }
    this.device = webgpu ? 'webgpu' : 'wasm';
    onStatus(`Whisper モデル読込中 (${this.model}, ${this.device})`);
    const files = new Map();
    this.pipe = await pipeline('automatic-speech-recognition', this.model, {
      device: this.device,
      dtype: webgpu ? { encoder_model: 'fp32', decoder_model_merged: 'q4' } : 'q8',
      progress_callback: (p) => {
        if (p.status === 'progress' && p.file) {
          files.set(p.file, p.progress);
          const avg = [...files.values()].reduce((a, b) => a + b, 0) / files.size;
          onStatus(`Whisper モデル取得中 ${Math.round(avg)}%`);
        }
      },
    });
  }

  async transcribe(samples, language = this.language) {
    // 15 秒区間に対し十分な上限。無音・雑音での反復幻覚の暴走を防ぐ
    const opts = { task: 'transcribe', max_new_tokens: 200 };
    if (language && language !== 'auto') opts.language = language;
    const r = await this.pipe(samples, opts);
    return collapseRepeats((r.text || '').trim());
  }
}
