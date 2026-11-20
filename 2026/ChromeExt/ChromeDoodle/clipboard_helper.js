const params = new URLSearchParams(location.search);
const taskId = params.get('id');
const statusEl = document.getElementById('status');
const copyBtn = document.getElementById('copy-btn');

let cachedBlob = null;
let resultSent = false;

async function reportResult(success, error) {
  if (resultSent || !taskId) return;
  resultSent = true;
  await chrome.runtime.sendMessage({
    type: 'CHROME_DOODLE_CLIPBOARD_RESULT',
    id: taskId,
    success,
    error
  }).catch(() => {});
}

async function waitForFocus(timeoutMs = 400) {
  if (document.hasFocus()) return;
  window.focus();
  await new Promise((resolve) => {
    let done = false;
    const finish = () => {
      if (done) return;
      done = true;
      window.removeEventListener('focus', finish);
      resolve();
    };
    window.addEventListener('focus', finish);
    setTimeout(finish, timeoutMs);
  });
}

async function writeBlobToClipboard(blob) {
  await navigator.clipboard.write([
    new ClipboardItem({ 'image/png': blob })
  ]);
}

async function init() {
  if (!taskId) {
    statusEl.className = 'error';
    statusEl.textContent = 'コピー対象のデータが見つかりません。';
    return;
  }

  try {
    const res = await chrome.runtime.sendMessage({
      type: 'CHROME_DOODLE_CLIPBOARD_GET_DATA',
      id: taskId
    });
    if (!res?.ok || !res.dataUrl) {
      throw new Error('画像データの取得に失敗しました');
    }

    const fetchRes = await fetch(res.dataUrl);
    cachedBlob = await fetchRes.blob();

    await waitForFocus();
    await writeBlobToClipboard(cachedBlob);
    await reportResult(true);
    window.close();
  } catch (err) {
    // Auto-write failed (e.g., focus or user-gesture policy); offer 1-click button
    statusEl.textContent = '下のボタンを押してクリップボードにコピーしてください。';
    copyBtn.style.display = 'inline-block';
  }
}

copyBtn.addEventListener('click', async () => {
  if (!cachedBlob) return;
  copyBtn.disabled = true;
  try {
    await writeBlobToClipboard(cachedBlob);
    await reportResult(true);
    window.close();
  } catch (err) {
    copyBtn.disabled = false;
    statusEl.className = 'error';
    statusEl.textContent = `コピーに失敗しました: ${err?.message || err}`;
  }
});

init();
