// 拡張の offscreen document (セキュアコンテキスト) で翻訳する。HTTP ページの content script から中継される。
// 基底セッションは翻訳先ごとに保持し、ページをまたいで再利用する
const { wrap, clean, createSession } = NanoPrompt;
const bases = new Map(); // 翻訳先 -> Promise<基底セッション>

async function getBase(target) {
  if (!bases.has(target)) bases.set(target, createSession(target));
  try { return await bases.get(target); } catch (e) { bases.delete(target); throw e; }
}

async function translate(target, text) {
  const clone = await (await getBase(target)).clone();
  try { return clean(await clone.prompt(wrap(text, target))).trim(); } finally { clone.destroy(); }
}

chrome.runtime.onMessage.addListener((msg, _sender, sendResponse) => {
  if (msg?.type !== "nano-translate") return;
  if (!("LanguageModel" in self)) { sendResponse({ ok: false, error: "Prompt API を利用できません" }); return; }
  translate(msg.target, msg.text).then(
    (text) => sendResponse({ ok: true, text }),
    (e) => sendResponse({ ok: false, error: e.name === "NotAllowedError" ? "モデル未取得。ポップアップの「翻訳」で先に取得してください" : `${e.name}: ${e.message}` }),
  );
  return true;
});
