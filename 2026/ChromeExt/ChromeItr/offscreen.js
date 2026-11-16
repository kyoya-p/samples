// 🤖 AI 生成: タブ音声の取得 → 無音区切り → 文字起こし → background 経由でページへ送る
import { NanoEngine, WhisperEngine } from './engines.js';
import { Translation } from './translate.js';

const RATE = 16000;
const BLOCK_MS = 100;
const SPEECH_RMS = 0.004;     // これ以上を「音あり」(約 -48 dBFS)
const END_SILENCE_MS = 700;   // 無音がこの長さ続いたら区切る
const MAX_SEGMENT_MS = 15000; // 強制区切り
const MIN_SPEECH_MS = 400;    // これ未満は捨てる
const PREROLL_BLOCKS = 3;
const MAX_QUEUE = 4;

const sessions = new Map(); // tabId -> { stream, ctxs, seg }

const send = (tabId, msg) => chrome.runtime.sendMessage({ target: 'background', tabId, ...msg });
const status = (tabId, text) => send(tabId, { type: 'status', text });

// ---- エンジン (全タブ共有・直列実行) ----
let enginePromise = null;
let engineKey = '';
let queue = Promise.resolve();
let pending = 0;

async function getEngine(settings, tabId) {
  const key = JSON.stringify([settings.engine, settings.whisperModel]);
  if (enginePromise && key === engineKey) return enginePromise;
  engineKey = key;
  enginePromise = (async () => {
    const report = (t) => status(tabId, t);
    if (settings.engine !== 'whisper') {
      try {
        const nano = new NanoEngine();
        report('Gemini Nano を初期化中');
        await nano.init(report);
        return nano;
      } catch (e) {
        if (settings.engine === 'nano') throw e;
        report('Gemini Nano 不可: ' + (e.message || e) + ' → Whisper へ切替');
      }
    }
    const w = new WhisperEngine(settings.whisperModel, settings.language);
    await w.init(report);
    return w;
  })();
  enginePromise.catch(() => { enginePromise = null; });
  return enginePromise;
}

let translation = null;
let translationKey = '';
function getTranslation(settings) {
  const key = settings.translateTo + '|' + settings.language;
  if (!translation || key !== translationKey) { translation = new Translation(settings.translateTo, settings.language); translationKey = key; }
  return translation;
}

// 翻訳は文字起こしとは別の直列キュー (翻訳モデルの取得中も文字起こしと原文表示を止めない)
let seq = 0;
let tQueue = Promise.resolve();
let tPending = 0;
function enqueueTranslation(tabId, id, text, settings) {
  const tl = getTranslation(settings);
  if (!tl.target || tl.target === 'none') return;
  if (tPending >= 8) { send(tabId, { type: 'translation', id, note: '翻訳が追いつかず省略' }); return; }
  tPending++;
  tQueue = tQueue.then(async () => {
    try {
      const tr = await tl.translate(text, (t) => status(tabId, t));
      send(tabId, { type: 'translation', id, translated: tr?.text, via: tr?.via, note: tr?.skipped, src: tr?.src });
    } catch (e) {
      send(tabId, { type: 'translation', id, note: '翻訳エラー: ' + (e.message || e) });
    } finally {
      tPending--;
    }
  });
}

function enqueue(tabId, samples, settings) {
  if (pending >= MAX_QUEUE) { status(tabId, '処理が追いつかず区間を破棄'); return; }
  pending++;
  queue = queue.then(async () => {
    try {
      let engine = await getEngine(settings, tabId);
      let text;
      try {
        text = await engine.transcribe(samples, settings.language);
      } catch (e) {
        if (engine instanceof NanoEngine && settings.engine === 'auto') {
          status(tabId, 'Gemini Nano 実行失敗: ' + (e.message || e) + ' → Whisper へ切替');
          settings = { ...settings, engine: 'whisper' };
          engine = await getEngine(settings, tabId);
          text = await engine.transcribe(samples, settings.language);
        } else throw e;
      }
      if (text && /[\p{L}\p{N}]/u.test(text)) {
        const id = ++seq;
        send(tabId, { type: 'text', id, text, engine: engine.name, info: `${engine.name}${engine.device ? '/' + engine.device : ''} 言語=${settings.language}`, translating: !!settings.translateTo && settings.translateTo !== 'none' }); // 原文は翻訳を待たず先に表示
        enqueueTranslation(tabId, id, text, settings);
      } else status(tabId, engine.name + ' で稼働中');
    } catch (e) {
      status(tabId, 'エラー: ' + (e.message || e));
    } finally {
      pending--;
    }
  });
}

// ---- 無音区切り ----
class Segmenter {
  constructor(onSegment) {
    this.onSegment = onSegment;
    this.pre = [];
    this.blocks = [];
    this.speaking = false;
    this.silence = 0;
    this.speech = 0;
  }
  push(block) {
    let sum = 0;
    for (let i = 0; i < block.length; i++) sum += block[i] * block[i];
    const loud = Math.sqrt(sum / block.length) >= SPEECH_RMS;
    if (!this.speaking) {
      this.pre.push(block);
      if (this.pre.length > PREROLL_BLOCKS) this.pre.shift();
      if (loud) { this.speaking = true; this.blocks = [...this.pre]; this.pre = []; this.silence = 0; this.speech = BLOCK_MS; }
      return;
    }
    this.blocks.push(block);
    if (loud) { this.silence = 0; this.speech += BLOCK_MS; } else this.silence += BLOCK_MS;
    if (this.silence >= END_SILENCE_MS || this.blocks.length * BLOCK_MS >= MAX_SEGMENT_MS) this.flush();
  }
  flush() {
    if (this.speaking && this.speech >= MIN_SPEECH_MS) {
      const out = new Float32Array(this.blocks.reduce((n, b) => n + b.length, 0));
      let o = 0;
      for (const b of this.blocks) { out.set(b, o); o += b.length; }
      this.onSegment(out);
    }
    this.speaking = false; this.blocks = []; this.silence = 0; this.speech = 0;
  }
}

// ---- キャプチャ ----
async function start({ tabId, streamId, settings }) {
  if (sessions.has(tabId)) return;
  try {
    const stream = await navigator.mediaDevices.getUserMedia({
      audio: { mandatory: { chromeMediaSource: 'tab', chromeMediaSourceId: streamId } },
    });
    // キャプチャ中はタブの音が止まるため、そのまま再生へ戻す
    const playCtx = new AudioContext();
    playCtx.createMediaStreamSource(stream).connect(playCtx.destination);
    // 文字起こし用は 16kHz の別コンテキスト
    const capCtx = new AudioContext({ sampleRate: RATE });
    await capCtx.audioWorklet.addModule('worklet.js');
    const node = new AudioWorkletNode(capCtx, 'pcm-tap');
    const mute = capCtx.createGain();
    mute.gain.value = 0;
    capCtx.createMediaStreamSource(stream).connect(node);
    node.connect(mute).connect(capCtx.destination);

    const seg = new Segmenter((samples) => enqueue(tabId, samples, sessions.get(tabId)?.settings || settings));
    node.port.onmessage = (e) => seg.push(e.data);
    stream.getAudioTracks().forEach((t) => t.addEventListener('ended', () => send(tabId, { type: 'ended' })));
    sessions.set(tabId, { stream, ctxs: [playCtx, capCtx], seg, settings });
    status(tabId, '音声待機中');
    getEngine(settings, tabId)
      .then((e) => status(tabId, e.name + ' 準備完了 / 音声待機中'))
      .catch((e) => status(tabId, 'エラー: ' + (e.message || e)));
  } catch (e) {
    status(tabId, 'キャプチャ失敗: ' + (e.message || e));
    send(tabId, { type: 'ended' });
  }
}

function stop(tabId) {
  const s = sessions.get(tabId);
  if (!s) { checkIdle(); return; }
  s.seg.flush();
  s.stream.getTracks().forEach((t) => t.stop());
  s.ctxs.forEach((c) => c.close());
  sessions.delete(tabId);
  checkIdle();
}

// 未処理の区間・翻訳が残っている間は閉じない。全て終わったら background へ知らせる
function checkIdle() {
  Promise.all([queue, tQueue]).then(() => {
    if (sessions.size !== 0) return;
    if (pending === 0 && tPending === 0) chrome.runtime.sendMessage({ target: 'background', type: 'idle' });
    else checkIdle();
  });
}

chrome.runtime.onMessage.addListener((msg) => {
  if (msg.target !== 'offscreen') return;
  if (msg.type === 'start') start(msg);
  else if (msg.type === 'stop') stop(msg.tabId);
  else if (msg.type === 'config') {
    // ダイアログでの言語変更を実行中のセッションへ即反映 (次の区間から)
    const s = sessions.get(msg.tabId);
    if (s) s.settings = { ...s.settings, ...msg.settings };
  }
});
