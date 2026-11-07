const SELECTION_ID = "nano-translate-selection";
const PAGE_ID = "nano-translate-page";

importScripts("prompt.js");

chrome.runtime.onInstalled.addListener(() => {
  NanoPrompt.cache.migrate();
  chrome.contextMenus.create({ id: SELECTION_ID, title: "Gemini Nano で翻訳: “%s”", contexts: ["selection"] });
  chrome.contextMenus.create({ id: PAGE_ID, title: "ページを Gemini Nano で翻訳 / 元に戻す", contexts: ["page"] });
});

// ページ全体を in-place 翻訳 (content.js を注入。再実行で元に戻す)
function translatePage(tabId) {
  return chrome.scripting.executeScript({ target: { tabId }, files: ["prompt.js", "content.js"] });
}

chrome.contextMenus.onClicked.addListener((info, tab) => {
  if (info.menuItemId === PAGE_ID && tab?.id) translatePage(tab.id);
  // Prompt API は Service Worker で使えないため、選択範囲は翻訳ページを小ウィンドウで開く
  if (info.menuItemId === SELECTION_ID && info.selectionText) {
    const url = chrome.runtime.getURL("translate.html") + "?window=1&text=" + encodeURIComponent(info.selectionText);
    chrome.windows.create({ url, type: "popup", width: 480, height: 640 });
  }
});

// content script は openOptionsPage を呼べないため、バッジの ⚙ からの依頼を受けて開く
// HTTP ページ (非セキュアコンテキスト) の翻訳は offscreen document で実行する (offscreen.js)
async function ensureOffscreen() {
  if (await chrome.offscreen.hasDocument()) return;
  await chrome.offscreen.createDocument({
    url: "offscreen.html",
    reasons: ["DOM_PARSER"],
    justification: "Prompt API (Gemini Nano) を拡張のセキュアコンテキストで実行するため",
  }).catch((e) => { if (!/single offscreen/i.test(e.message)) throw e; });
}

chrome.runtime.onMessage.addListener((msg, sender, sendResponse) => {
  if (sender.id !== chrome.runtime.id) return;
  if (msg?.type === "open-options") {
    chrome.runtime.openOptionsPage().catch(() => chrome.tabs.create({ url: chrome.runtime.getURL("options.html") }));
  } else if (msg?.type === "ensure-offscreen") {
    ensureOffscreen().then(() => sendResponse({ ok: true }), (e) => sendResponse({ ok: false, error: e.message }));
    return true;
  }
});

chrome.commands.onCommand.addListener(async (command, tab) => {
  if (command !== "translate-page") return;
  const t = tab ?? (await chrome.tabs.query({ active: true, currentWindow: true }))[0];
  if (t?.id) translatePage(t.id);
});
