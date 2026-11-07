const { LANGS, SUPPORTED, getTarget, wrap, clean, languageOptions, createSession, cache } = NanoPrompt;

const $ = (id) => document.getElementById(id);
const sourceEl = $("source");
const targetEl = $("target");
const resultEl = $("result");
const statusEl = $("status");
const noticeEl = $("notice");
const detectedEl = $("detected");
const translateBtn = $("translate");
const stopBtn = $("stop");

const params = new URLSearchParams(location.search);
if (params.get("window")) document.body.classList.add("window");

const bases = new Map(); // 翻訳先 -> 基底セッション
let controller = null;

const setStatus = (t) => { statusEl.textContent = t; };
const showNotice = (t) => { noticeEl.textContent = t; noticeEl.hidden = !t; };
const showError = (t) => { resultEl.textContent = t; resultEl.classList.add("error"); };

async function getBaseSession(target) {
  if (bases.has(target)) return bases.get(target);
  const session = await createSession(target, {
    monitor(m) {
      m.addEventListener("downloadprogress", (e) => setStatus(e.loaded < 1 ? `モデル取得中 ${Math.floor(e.loaded * 100)}%` : "準備中…"));
    },
  });
  bases.set(target, session);
  return session;
}

async function checkAvailability() {
  if (!("LanguageModel" in self)) {
    showNotice("このブラウザは Prompt API (LanguageModel) に対応していません。Chrome 138 以降が必要です。");
    return "unavailable";
  }
  const a = await LanguageModel.availability(languageOptions(targetEl.value));
  if (a === "unavailable") {
    showNotice("Gemini Nano を利用できません (空き容量 22GB 以上・GPU 4GB 超 または RAM 16GB 以上が必要)。chrome://on-device-internals を確認してください。");
  } else if (a === "available") {
    showNotice("");
  }
  setStatus(a === "available" ? "" : a === "downloadable" ? "初回はモデル取得あり" : a === "downloading" ? "モデル取得中" : "利用不可");
  return a;
}

async function detect(text) {
  if (!("LanguageDetector" in self)) return null;
  try {
    const d = await LanguageDetector.create();
    const [top] = await d.detect(text);
    return top && top.confidence > 0.5 ? top.detectedLanguage : null;
  } catch {
    return null;
  }
}

async function translate() {
  const text = sourceEl.value.trim();
  if (!text) return;
  const target = targetEl.value;
  resultEl.classList.remove("error");
  resultEl.textContent = "";
  controller?.abort();
  controller = new AbortController();
  const { signal } = controller;
  translateBtn.disabled = true;
  stopBtn.hidden = false;

  detect(text).then((lang) => {
    if (!lang) { detectedEl.textContent = "言語を自動検出"; return; }
    const name = new Intl.DisplayNames(["ja"], { type: "language" }).of(lang);
    detectedEl.textContent = `検出: ${name}`;
    showNotice(SUPPORTED.includes(lang) ? "" : `入力言語 (${name}) は公式サポート外のため、精度が下がる可能性があります。`);
  });

  let clone = null;
  try {
    const cached = await cache.get(target, text);
    if (cached !== null) {
      resultEl.textContent = cached;
      setStatus("");
      return;
    }
    setStatus("翻訳中…");
    const base = await getBaseSession(target);
    clone = await base.clone({ signal });
    let out = "";
    let prev = "";
    for await (const chunk of clone.promptStreaming(wrap(text, target), { signal })) {
      // 累積/差分どちらのチャンクにも対応
      out += chunk.startsWith(prev) ? chunk.slice(prev.length) : chunk;
      prev = chunk;
      resultEl.textContent = clean(out);
    }
    await cache.set(target, text, clean(out).trim());
    setStatus("");
  } catch (e) {
    if (e.name === "AbortError") setStatus("停止しました");
    else {
      showError(e.name === "NotAllowedError"
        ? "モデルの取得にはユーザー操作が必要です。「翻訳」ボタンを押してください。"
        : `${e.name}: ${e.message}`);
      setStatus("エラー");
    }
  } finally {
    clone?.destroy();
    translateBtn.disabled = false;
    stopBtn.hidden = true;
  }
}

// manifest の options_ui が未反映 (拡張の再読み込み前) でも開けるよう、失敗時は直接タブで開く
$("settings").addEventListener("click", async () => {
  try { await chrome.runtime.openOptionsPage(); }
  catch { chrome.tabs.create({ url: chrome.runtime.getURL("options.html") }); }
});
translateBtn.addEventListener("click", translate);
stopBtn.addEventListener("click", () => controller?.abort());
sourceEl.addEventListener("keydown", (e) => {
  if (e.key === "Enter" && (e.ctrlKey || e.metaKey)) { e.preventDefault(); translate(); }
});
$("clear").addEventListener("click", () => {
  sourceEl.value = ""; resultEl.textContent = ""; detectedEl.textContent = "言語を自動検出"; sourceEl.focus();
});
$("copy").addEventListener("click", () => navigator.clipboard.writeText(resultEl.textContent));
targetEl.addEventListener("change", () => {
  chrome.storage.local.set({ target: targetEl.value });
  checkAvailability();
});

async function init() {
  targetEl.value = await getTarget();
  await checkAvailability();

  let text = params.get("text") || "";
  // ポップアップ時は表示中タブの選択テキストを初期値にする
  if (!text && chrome?.scripting && chrome?.tabs) {
    try {
      const [tab] = await chrome.tabs.query({ active: true, currentWindow: true });
      const [r] = await chrome.scripting.executeScript({ target: { tabId: tab.id }, func: () => getSelection().toString() });
      text = r?.result ?? "";
    } catch { /* chrome:// 等は取得不可 */ }
  }
  if (text) {
    sourceEl.value = text;
    // 右クリックメニュー (?text=) からは常に即翻訳を開始。モデル未取得なら create() がユーザー操作を要求して失敗するので、その旨を表示
    if (params.get("text")) translate();
    // ポップアップで選択テキストを拾った場合は available のときのみ自動翻訳
    else if ((await LanguageModel.availability(languageOptions(targetEl.value))) === "available") translate();
  }
}
init();
