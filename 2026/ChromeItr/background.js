// 🤖 AI 生成: 拡張機能アイコンのクリックで対象タブの文字起こしを開始/停止する

// 文字起こし中の tabId。service worker は無通信が続くと停止しメモリが消えるため storage.session にも保持する
let active = null;
async function getActive() {
  if (!active) active = new Set((await chrome.storage.session.get({ active: [] })).active);
  return active;
}
async function saveActive() { await chrome.storage.session.set({ active: [...active] }); }

const DEFAULTS = { engine: 'auto', language: 'auto', translateTo: 'ja', whisperModel: 'onnx-community/whisper-base' };

async function ensureOffscreen() {
  if (await chrome.offscreen.hasDocument()) return;
  await chrome.offscreen.createDocument({
    url: 'offscreen.html',
    reasons: ['USER_MEDIA'],
    justification: 'タブ音声の取得と文字起こし',
  });
}

async function setBadge(tabId, on) {
  await chrome.action.setBadgeText({ tabId, text: on ? 'ON' : '' });
  if (on) await chrome.action.setBadgeBackgroundColor({ tabId, color: '#c00' });
}

async function toNotify(tabId, msg) {
  try { await chrome.tabs.sendMessage(tabId, { ...msg, target: 'content' }); } catch { /* タブ側が無い */ }
}

// ダイアログ (content script) が生きているか
async function alive(tabId) {
  try { return (await chrome.tabs.sendMessage(tabId, { target: 'content', type: 'ping' })) === true; } catch { return false; }
}

// ダイアログを (再) 表示。ページ遷移後は activeTab が失効するため、失敗したら false
async function showDialog(tabId, extra = {}) {
  try {
    await chrome.scripting.executeScript({ target: { tabId }, files: ['content.js'] });
  } catch {
    return false;
  }
  await toNotify(tabId, { type: 'show', settings: await chrome.storage.local.get(DEFAULTS), ...extra });
  return true;
}

async function start(tab) {
  const tabId = tab.id;
  await chrome.scripting.executeScript({ target: { tabId }, files: ['content.js'] });
  const streamId = await chrome.tabCapture.getMediaStreamId({ targetTabId: tabId });
  await ensureOffscreen();
  const settings = await chrome.storage.local.get(DEFAULTS);
  (await getActive()).add(tabId);
  await saveActive();
  await setBadge(tabId, true);
  await toNotify(tabId, { type: 'show', settings });
  chrome.runtime.sendMessage({ target: 'offscreen', type: 'start', tabId, streamId, settings });
}

async function stop(tabId, reason) {
  (await getActive()).delete(tabId);
  await saveActive();
  await setBadge(tabId, false).catch(() => {});
  chrome.runtime.sendMessage({ target: 'offscreen', type: 'stop', tabId }).catch(() => {});
  await toNotify(tabId, { type: 'stopped', reason });
  // offscreen は未処理の区間が終わるまで残す (offscreen から 'idle' が来たら閉じる)
}

async function closeIfIdle() {
  if ((await getActive()).size === 0 && (await chrome.offscreen.hasDocument())) await chrome.offscreen.closeDocument();
}

chrome.action.onClicked.addListener(async (tab) => {
  try {
    if ((await getActive()).has(tab.id)) {
      // 実行中にダイアログが消えている (ページ遷移後など) なら停止せず再表示する
      if (await alive(tab.id)) await stop(tab.id, 'アイコン');
      else await showDialog(tab.id, { status: '文字起こし継続中' });
    } else await start(tab);
  } catch (e) {
    (await getActive()).delete(tab.id);
    await saveActive();
    await setBadge(tab.id, false);
    await toNotify(tab.id, { type: 'status', text: 'エラー: ' + (e.message || e) });
  }
});

chrome.runtime.onMessage.addListener((msg, sender) => {
  if (msg.target === 'background') {
    // offscreen からの通知を対象タブへ中継
    if (msg.type === 'idle') { closeIfIdle(); return; }
    if (msg.type === 'ended') { stop(msg.tabId, msg.reason || '音声取得が終了'); return; }
    toNotify(msg.tabId, msg);
  } else if (msg.target === 'background-config' && sender.tab) {
    // ダイアログの言語選択: 保存して実行中の offscreen へ反映
    chrome.storage.local.set(msg.settings);
    getActive().then((a) => {
      if (a.has(sender.tab.id)) chrome.runtime.sendMessage({ target: 'offscreen', type: 'config', tabId: sender.tab.id, settings: msg.settings });
    });
  } else if (msg.target === 'background-stop' && sender.tab) {
    // ダイアログの「停止」ボタン
    stop(sender.tab.id, '停止ボタン');
  }
});

chrome.tabs.onRemoved.addListener(async (tabId) => { if ((await getActive()).has(tabId)) stop(tabId, 'タブ終了'); });

// ページ遷移では止めない (タブの音声取得は遷移後も続く)。読込完了後にダイアログが無ければ再表示を試みる。
// activeTab が失効して再表示できない場合は、アイコンを押すと再表示する
chrome.tabs.onUpdated.addListener(async (tabId, info) => {
  if (info.status !== 'complete' || !(await getActive()).has(tabId)) return;
  if (!(await alive(tabId))) await showDialog(tabId, { status: 'ページ遷移後も文字起こし継続中' });
});
