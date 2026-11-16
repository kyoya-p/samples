const params = new URLSearchParams(location.search);
const targetTabId = Number(params.get('tabId')) || null;
const btn = document.getElementById('capture-btn');
const statusEl = document.getElementById('status');

async function startCapture() {
  btn.disabled = true;
  statusEl.className = 'status';
  statusEl.textContent = '共有する画面またはウィンドウを選択してください...';

  let stream = null;
  try {
    stream = await navigator.mediaDevices.getDisplayMedia({
      video: { cursor: 'always' },
      audio: false
    });

    statusEl.textContent = 'フレームを取得中...';
    const video = document.createElement('video');
    video.muted = true;
    video.playsInline = true;
    video.srcObject = stream;
    await video.play();

    await new Promise((resolve) => {
      const check = () => {
        if (video.readyState >= 2 && video.videoWidth > 0 && video.videoHeight > 0) {
          resolve();
        } else {
          requestAnimationFrame(check);
        }
      };
      check();
    });
    await new Promise(r => setTimeout(r, 120));

    const canvas = document.createElement('canvas');
    canvas.width = video.videoWidth;
    canvas.height = video.videoHeight;
    const ctx = canvas.getContext('2d');
    ctx.drawImage(video, 0, 0, canvas.width, canvas.height);
    const dataUrl = canvas.toDataURL('image/png');

    stream.getTracks().forEach(t => t.stop());
    stream = null;

    await chrome.runtime.sendMessage({
      type: 'CHROME_DOODLE_DESKTOP_RESULT',
      targetTabId,
      success: true,
      dataUrl
    });

    window.close();
  } catch (err) {
    if (stream) {
      stream.getTracks().forEach(t => t.stop());
    }
    btn.disabled = false;
    statusEl.className = 'status error';
    if (err.name === 'AbortError' || err.name === 'NotAllowedError') {
      statusEl.textContent = 'キャンセルされました。もう一度試すには上のボタンを押してください。';
      await chrome.runtime.sendMessage({
        type: 'CHROME_DOODLE_DESKTOP_RESULT',
        targetTabId,
        success: false,
        cancelled: true
      }).catch(() => {});
    } else {
      statusEl.textContent = `エラー: ${err.message || err}`;
    }
  }
}

btn.addEventListener('click', startCapture);
