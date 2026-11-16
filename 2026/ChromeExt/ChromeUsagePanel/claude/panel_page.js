// 🤖[AI 生成・編集を含む (Antigravity)]
// Standalone tab entry point for panel.html
(() => {
  const styleEl = document.createElement('style');
  styleEl.textContent = globalThis.__USAGE_PANEL_CSS || '';
  document.head.appendChild(styleEl);

  const ctrl = globalThis.__UsagePanelMount({
    root: document,
    container: document.getElementById('app'),
    scrollEl: null,
    isDrawer: false,
  });

  chrome.runtime.onMessage.addListener(msg => {
    if (msg?.type === 'state-updated') {
      ctrl?.sync();
    }
  });
})();
