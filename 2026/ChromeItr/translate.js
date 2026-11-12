// 🤖 AI 生成: 文字起こし結果の翻訳。Chrome 組み込み Translator (取得済みの場合のみ) → ローカル Marian モデル (transformers.js)
import { loadTransformers } from './engines.js';

// ローカルモデル (すべて Hugging Face の Xenova/*。存在は HF API で確認済み)
// 品質: ja→en は opus-mt (実測良好)。en→ja は opus-mt-en-jap が意味不明な出力 (実測) のため NLLB-200 を使う
const OPUS = { 'ja>en': 'Xenova/opus-mt-ja-en', 'zh>en': 'Xenova/opus-mt-zh-en' };
const NLLB = 'Xenova/nllb-200-distilled-600M';
const FLORES = { ja: 'jpn_Jpan', en: 'eng_Latn', zh: 'zho_Hans', ko: 'kor_Hang', es: 'spa_Latn', fr: 'fra_Latn', de: 'deu_Latn' };

function pickLocal(src, dst) {
  if (OPUS[`${src}>${dst}`]) return { model: OPUS[`${src}>${dst}`] };
  if (FLORES[src] && FLORES[dst]) return { model: NLLB, opts: { src_lang: FLORES[src], tgt_lang: FLORES[dst] } };
  return null;
}

// 文字種による簡易判定 (LanguageDetector が使えない場合)。ラテン文字は英語とみなす
export function guessLanguage(text) {
  if (/[぀-ヿ]/.test(text)) return 'ja';
  if (/[가-힯]/.test(text)) return 'ko';
  if (/[一-鿿]/.test(text)) return 'ja'; // 漢字のみは日中を区別できない。日本語とみなす (音声の言語を固定すれば確実)
  if (/[A-Za-z]/.test(text)) return 'en';
  return null;
}

// モデルは重い (NLLB は約 600MB) ため、Translation の複数インスタンスで共有して重複ロードを防ぐ
const localPipes = new Map(); // model -> pipeline Promise

export class Translation {
  constructor(target, fixedSource) {
    this.target = target;            // 'none' | 'ja' | 'en' ...
    this.fixedSource = fixedSource;  // 認識言語が固定ならその言語。'auto' なら判定
    this.chrome = new Map();         // 'src>dst' -> Translator (取得済みの対のみ)
    this.detector = undefined;
  }

  async detect(text) {
    // 固定言語は文字種と矛盾しない場合のみ採用 (日本語固定でも Whisper が英語を出すことがあるため。2026-11-12 実機で発生)
    const script = guessLanguage(text);
    const fixed = this.fixedSource && this.fixedSource !== 'auto' ? this.fixedSource : null;
    const CJK = ['ja', 'zh', 'ko'];
    if (fixed && (fixed === script || (script === 'en' && !CJK.includes(fixed)) || (script === 'ja' && fixed === 'zh'))) return fixed;
    if (fixed && script) return script;
    try {
      if (this.detector === undefined) {
        this.detector = typeof LanguageDetector !== 'undefined' && (await LanguageDetector.availability()) === 'available'
          ? await LanguageDetector.create() : null;
      }
      if (this.detector && text.length >= 8) {
        const [top] = await this.detector.detect(text);
        if (top && top.confidence >= 0.6) return top.detectedLanguage.split('-')[0];
      }
    } catch { /* 簡易判定へ */ }
    return guessLanguage(text);
  }

  async viaChrome(text, src, dst) {
    if (typeof Translator === 'undefined') return null;
    const key = `${src}>${dst}`;
    if (!this.chrome.has(key)) {
      // 'downloadable' で create するとモデル取得が走る。テスト環境でタブがクラッシュしたため、取得済みの対だけ使う
      const a = await Translator.availability({ sourceLanguage: src, targetLanguage: dst });
      this.chrome.set(key, a === 'available' ? await Translator.create({ sourceLanguage: src, targetLanguage: dst }) : null);
    }
    const t = this.chrome.get(key);
    return t ? { text: await t.translate(text), via: 'Chrome Translator' } : null;
  }

  async viaLocal(text, src, dst, onStatus) {
    const pick = pickLocal(src, dst);
    if (!pick) return null;
    const { model, opts = {} } = pick;
    if (!localPipes.has(model)) {
      localPipes.set(model, (async () => {
        const { pipeline } = await loadTransformers();
        onStatus(`翻訳モデル取得中 (${model})`);
        const files = new Map();
        return pipeline('translation', model, {
          device: 'wasm',
          dtype: 'q8',
          progress_callback: (p) => {
            if (p.status === 'progress' && p.file) {
              files.set(p.file, p.progress);
              const avg = [...files.values()].reduce((a, b) => a + b, 0) / files.size;
              onStatus(`翻訳モデル取得中 ${Math.round(avg)}%`);
            }
          },
        });
      })());
      localPipes.get(model).catch(() => localPipes.delete(model));
    }
    const pipe = await localPipes.get(model);
    const r = await pipe(text, { max_new_tokens: 256, ...opts });
    return { text: (r[0]?.translation_text || '').trim(), via: 'ローカル ' + model.split('/')[1] };
  }

  // 戻り値: { src, text?, via?, skipped? }。翻訳しない場合は text なし
  async translate(text, onStatus) {
    if (!this.target || this.target === 'none') return null;
    const src = await this.detect(text);
    if (!src || src === this.target) return { src }; // 判定不能、または原文が既に翻訳先の言語
    try {
      const c = await this.viaChrome(text, src, this.target);
      if (c) return { src, ...c };
    } catch { /* ローカルへ */ }
    const l = await this.viaLocal(text, src, this.target, onStatus);
    return { src, ...(l || { skipped: `${src}→${this.target} は未対応 (Chrome の翻訳モデル未取得、ローカルモデルなし)` }) };
  }
}
