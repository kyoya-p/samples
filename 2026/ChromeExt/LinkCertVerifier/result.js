// LinkCertVerifier - Fallback Window & Toolbar Popup Script
(function () {
  const viewer = window.__LCV_VIEWER__;
  const mount = document.getElementById('viewerMount');
  const urlForm = document.getElementById('urlForm');
  const urlInput = document.getElementById('urlInput');

  const styleEl = document.createElement('style');
  styleEl.textContent = viewer.getViewerStyles();
  document.head.appendChild(styleEl);

  const params = new URLSearchParams(location.search);
  const mode = params.get('mode');
  const requestId = params.get('requestId');

  let state = null;

  function render() {
    mount.innerHTML = viewer.renderVerificationBody(state);
    viewer.bindViewerEvents(mount, state, {
      onReverify: (options) => {
        if (!state?.url) return;
        verifyUrl(state.url, state.linkText || '', options);
      },
    });
  }

  function verifyUrl(url, linkText = '', options = {}) {
    state = { requestId: requestId || 'manual', status: 'loading', url, linkText, result: null };
    render();
    chrome.runtime.sendMessage({
      type: 'LCV_VERIFY_REQUEST',
      url,
      linkText,
      options,
    }, (resp) => {
      if (chrome.runtime.lastError) {
        state = {
          ...state,
          status: 'done',
          result: {
            inspectedAt: new Date().toISOString(),
            originalUrl: url,
            verdict: {
              level: 'error',
              title: '通信エラー',
              summary: chrome.runtime.lastError.message,
              warnings: [],
              errors: [chrome.runtime.lastError.message],
            },
          },
        };
      } else if (resp && resp.ok) {
        state = { ...state, status: 'done', result: resp.result };
      } else {
        const err = resp?.error || '検証に失敗しました';
        state = {
          ...state,
          status: 'done',
          result: {
            inspectedAt: new Date().toISOString(),
            originalUrl: url,
            verdict: { level: 'error', title: '検証エラー', summary: err, warnings: [], errors: [err] },
          },
        };
      }
      render();
    });
  }

  async function loadSessionState(reqId) {
    const key = `lcv:${reqId}`;
    try {
      const data = await chrome.storage.session.get(key);
      if (data && data[key]) return data[key];
    } catch { /* fallback */ }
    try {
      const data = await chrome.storage.local.get(key);
      if (data && data[key]) return data[key];
    } catch { /* ignore */ }
    return null;
  }

  if (mode === 'popup') {
    document.body.classList.add('mode-popup');
    urlForm.style.display = 'flex';
    urlForm.addEventListener('submit', (e) => {
      e.preventDefault();
      const target = urlInput.value.trim();
      if (target) verifyUrl(target);
    });

    chrome.tabs.query({ active: true, currentWindow: true }, (tabs) => {
      const activeUrl = tabs?.[0]?.url || '';
      if (activeUrl && /^https?:\/\//i.test(activeUrl)) {
        urlInput.value = activeUrl;
        verifyUrl(activeUrl);
      } else {
        urlInput.value = activeUrl;
        mount.innerHTML = `<div style="color:#52525b; font-size:12.5px; padding:8px 2px;">検証したい URL を入力するか、Web ページ上のリンクを右クリックして「リンクのアドレスを検証」を選択してください。</div>`;
      }
    });
  } else if (requestId) {
    let hasDoneState = false;
    chrome.runtime.onMessage.addListener((message) => {
      if (message?.type === 'LCV_WINDOW_RESULT' && message.requestId === requestId && message.state) {
        if (message.state.status === 'done') hasDoneState = true;
        state = message.state;
        render();
      }
    });

    loadSessionState(requestId).then((loaded) => {
      if (loaded) {
        if (hasDoneState && loaded.status !== 'done') return;
        if (loaded.status === 'done') hasDoneState = true;
        state = loaded;
        render();
      } else if (!hasDoneState) {
        state = { status: 'loading', url: '', result: null };
        render();
      }
    });
  }
})();
