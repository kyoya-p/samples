// LinkCertVerifier - In-page Shadow DOM Modal
(function () {
  if (window.__LCV_CONTENT_INIT__) return;
  window.__LCV_CONTENT_INIT__ = true;

  const HOST_ID = 'link-cert-verifier-host';
  let currentState = null;
  let hostEl = null;
  let shadowRoot = null;
  let bodyEl = null;
  let dialogEl = null;
  let closeBtnEl = null;

  function ensureModal() {
    if (hostEl && document.documentElement.contains(hostEl)) {
      return;
    }

    hostEl = document.createElement('div');
    hostEl.id = HOST_ID;
    hostEl.style.cssText = 'all: initial; position: fixed; inset: 0; z-index: 2147483647; pointer-events: auto;';

    shadowRoot = hostEl.attachShadow({ mode: 'open' });
    const viewer = window.__LCV_VIEWER__;
    const style = document.createElement('style');
    style.textContent = `
      ${viewer.getViewerStyles()}
      .lcv-backdrop {
        position: fixed;
        inset: 0;
        background: rgba(15, 23, 42, 0.42);
        backdrop-filter: blur(3px);
        display: flex;
        align-items: center;
        justify-content: center;
        padding: 16px;
      }
      .lcv-dialog {
        width: min(680px, calc(100vw - 32px));
        max-height: min(84vh, 820px);
        display: flex;
        flex-direction: column;
        background: #ffffff;
        border: 1px solid #d4d4d8;
        border-radius: 14px;
        box-shadow: 0 24px 60px rgba(0, 0, 0, 0.28);
        overflow: hidden;
        position: relative;
      }
      .lcv-header {
        display: flex;
        align-items: center;
        justify-content: space-between;
        gap: 12px;
        padding: 12px 16px;
        background: #f8fafc;
        border-bottom: 1px solid #e4e4e7;
        cursor: move;
        user-select: none;
      }
      .lcv-header-title {
        display: flex;
        align-items: center;
        gap: 9px;
        font-size: 14px;
        font-weight: 700;
        color: #18181b;
      }
      .lcv-shield-badge {
        width: 22px;
        height: 22px;
        border-radius: 6px;
        background: linear-gradient(135deg, #2563eb, #10b981);
        display: inline-flex;
        align-items: center;
        justify-content: center;
        font-size: 13px;
        font-weight: 800;
        color: #fff;
      }
      .lcv-close {
        appearance: none;
        background: #ffffff;
        color: #52525b;
        border: 1px solid #d4d4d8;
        border-radius: 7px;
        width: 28px;
        height: 28px;
        font-size: 16px;
        line-height: 1;
        cursor: pointer;
        display: inline-flex;
        align-items: center;
        justify-content: center;
      }
      .lcv-close:hover {
        background: #fee2e2;
        border-color: #fca5a5;
        color: #b91c1c;
      }
      .lcv-scroll {
        padding: 16px;
        background: #ffffff;
        overflow-y: auto;
        overscroll-behavior: contain;
      }
    `;

    const backdrop = document.createElement('div');
    backdrop.className = 'lcv-backdrop lcv-root';

    dialogEl = document.createElement('div');
    dialogEl.className = 'lcv-dialog';
    dialogEl.setAttribute('role', 'dialog');
    dialogEl.setAttribute('aria-modal', 'true');
    dialogEl.setAttribute('aria-label', 'リンクアドレス・TLS証明書検証');

    const header = document.createElement('div');
    header.className = 'lcv-header';
    header.innerHTML = `
      <div class="lcv-header-title">
        <span class="lcv-shield-badge">✓</span>
        <span>リンクアドレス・TLS 証明書検証</span>
      </div>
      <button type="button" class="lcv-close" title="閉じる (Esc)" aria-label="閉じる">✕</button>
    `;

    bodyEl = document.createElement('div');
    bodyEl.className = 'lcv-scroll';

    dialogEl.appendChild(header);
    dialogEl.appendChild(bodyEl);
    backdrop.appendChild(dialogEl);
    shadowRoot.appendChild(style);
    shadowRoot.appendChild(backdrop);
    document.documentElement.appendChild(hostEl);

    closeBtnEl = header.querySelector('.lcv-close');
    closeBtnEl.addEventListener('click', closeModal);
    backdrop.addEventListener('mousedown', (e) => {
      if (e.target === backdrop) closeModal();
    });

    setupDrag(header, dialogEl);
  }

  function setupDrag(handle, target) {
    let dragging = false;
    let startX = 0;
    let startY = 0;
    let baseLeft = 0;
    let baseTop = 0;

    handle.addEventListener('mousedown', (e) => {
      if (e.button !== 0 || e.target.closest('button')) return;
      dragging = true;
      const rect = target.getBoundingClientRect();
      if (target.style.position !== 'fixed') {
        target.style.position = 'fixed';
        target.style.margin = '0';
        target.style.left = `${rect.left}px`;
        target.style.top = `${rect.top}px`;
      }
      startX = e.clientX;
      startY = e.clientY;
      baseLeft = parseFloat(target.style.left) || rect.left;
      baseTop = parseFloat(target.style.top) || rect.top;
      e.preventDefault();
    });

    window.addEventListener('mousemove', (e) => {
      if (!dragging) return;
      const dx = e.clientX - startX;
      const dy = e.clientY - startY;
      const rect = target.getBoundingClientRect();
      const maxLeft = Math.max(8, window.innerWidth - rect.width - 8);
      const maxTop = Math.max(8, window.innerHeight - rect.height - 8);
      const nextLeft = Math.min(maxLeft, Math.max(8, baseLeft + dx));
      const nextTop = Math.min(maxTop, Math.max(8, baseTop + dy));
      target.style.left = `${nextLeft}px`;
      target.style.top = `${nextTop}px`;
    });

    window.addEventListener('mouseup', () => {
      dragging = false;
    });
  }

  function onKeyDown(e) {
    if (e.key === 'Escape' && hostEl) {
      closeModal();
    }
  }
  window.addEventListener('keydown', onKeyDown, true);

  function closeModal() {
    if (hostEl) {
      hostEl.remove();
      hostEl = null;
      shadowRoot = null;
      bodyEl = null;
      dialogEl = null;
    }
  }

  function render() {
    ensureModal();
    const viewer = window.__LCV_VIEWER__;
    bodyEl.innerHTML = viewer.renderVerificationBody(currentState);
    viewer.bindViewerEvents(bodyEl, currentState, {
      onReverify: (options) => {
        if (!currentState) return;
        const targetUrl = currentState.url;
        const linkText = currentState.linkText || '';
        currentState = { ...currentState, status: 'loading', result: null };
        render();
        chrome.runtime.sendMessage({
          type: 'LCV_VERIFY_REQUEST',
          url: targetUrl,
          linkText,
          options,
        }, (resp) => {
          if (chrome.runtime.lastError) {
            currentState = {
              ...currentState,
              status: 'done',
              result: {
                inspectedAt: new Date().toISOString(),
                originalUrl: targetUrl,
                verdict: { level: 'error', title: '通信エラー', summary: chrome.runtime.lastError.message, warnings: [], errors: [chrome.runtime.lastError.message] },
              },
            };
          } else if (resp && resp.ok) {
            currentState = { ...currentState, status: 'done', result: resp.result };
          } else {
            const err = resp?.error || '再検証に失敗しました';
            currentState = {
              ...currentState,
              status: 'done',
              result: {
                inspectedAt: new Date().toISOString(),
                originalUrl: targetUrl,
                verdict: { level: 'error', title: '検証エラー', summary: err, warnings: [], errors: [err] },
              },
            };
          }
          render();
        });
      },
    });
  }

  chrome.runtime.onMessage.addListener((message, sender, sendResponse) => {
    if (message?.type === 'LCV_PING') {
      sendResponse({ ok: true, pong: true });
      return;
    }
    if (message?.type === 'LCV_SHOW_LOADING') {
      currentState = {
        requestId: message.requestId,
        status: 'loading',
        url: message.url,
        linkText: message.linkText || '',
        result: null,
      };
      render();
      sendResponse({ ok: true });
      return;
    }
    if (message?.type === 'LCV_SHOW_RESULT') {
      if (currentState && currentState.requestId && message.requestId && currentState.requestId !== message.requestId) {
        sendResponse({ ok: true, ignored: true });
        return;
      }
      currentState = {
        requestId: message.requestId,
        status: 'done',
        url: message.result?.originalUrl || currentState?.url || '',
        linkText: currentState?.linkText || '',
        result: message.result,
      };
      render();
      sendResponse({ ok: true });
      return;
    }
  });
})();
