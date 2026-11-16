// 🤖[AI 生成・編集を含む (Antigravity)]
// Slide-out right-side drawer with draggable edge tab injected via Shadow DOM
(() => {
  const GLOBAL_KEY = '__claudeUsageDrawer';
  if (globalThis[GLOBAL_KEY]?.teardown) {
    try { globalThis[GLOBAL_KEY].teardown(); } catch { /* ignore */ }
  }

  document.querySelectorAll('[data-claude-usage-drawer]').forEach(el => el.remove());

  const DEFAULT_WIDTH = 690;
  const DEFAULT_TAB_TOP_PCT = 42;

  const isContextValid = () => {
    try { return Boolean(chrome?.runtime?.id); } catch { return false; }
  };

  function safeStorageGet(keys) {
    if (!isContextValid()) return Promise.resolve({});
    try {
      return Promise.resolve(chrome.storage.local.get(keys)).catch(() => ({}));
    } catch {
      return Promise.resolve({});
    }
  }

  function safeStorageSet(obj) {
    if (!isContextValid()) return;
    try {
      chrome.storage.local.set(obj)?.catch?.(() => {});
    } catch { /* ignore */ }
  }

  function safeSend(msg) {
    if (!isContextValid()) return Promise.resolve(null);
    try {
      return Promise.resolve(chrome.runtime.sendMessage(msg)).catch(() => null);
    } catch {
      return Promise.resolve(null);
    }
  }

  const host = document.createElement('div');
  host.dataset.claudeUsageDrawer = '';
  host.style.cssText = 'all:initial;position:fixed;top:0;right:0;width:0;height:0;z-index:2147483647;pointer-events:none;';
  const root = host.attachShadow({ mode: 'open' });

  const styleEl = document.createElement('style');
  styleEl.textContent = globalThis.__USAGE_PANEL_CSS || '';
  root.appendChild(styleEl);

  const drawer = document.createElement('aside');
  drawer.className = 'usage-scope drawer';
  drawer.setAttribute('role', 'dialog');
  drawer.setAttribute('aria-label', 'Claude Usage Panel');

  const tab = document.createElement('button');
  tab.type = 'button';
  tab.className = 'drawer-tab';
  tab.setAttribute('aria-label', 'Claude Usage Panel を開く/閉じる');
  tab.setAttribute('aria-expanded', 'false');
  tab.title = 'クリック: 開閉 / 横ドラッグ: 幅変更 / 縦ドラッグ: タブ位置移動 (Alt+Shift+C)';
  tab.innerHTML =
    '<span class="drawer-tab-dot" style="background:#d97757"></span>' +
    '<span class="drawer-tab-grip" aria-hidden="true"><i></i><i></i><i></i></span>' +
    '<span class="drawer-tab-tag">C</span>';

  const resizer = document.createElement('div');
  resizer.className = 'drawer-resizer';
  resizer.title = 'ドラッグして幅を変更';

  const scrollEl = document.createElement('div');
  scrollEl.className = 'drawer-scroll';

  drawer.appendChild(tab);
  drawer.appendChild(resizer);
  drawer.appendChild(scrollEl);
  root.appendChild(drawer);

  drawer.addEventListener('keydown', e => {
    if (e.key === 'Escape') {
      e.stopPropagation();
      setOpen(false);
    } else {
      e.stopPropagation();
    }
  });

  const SESSION_KEY = '__claudeUsageDrawerState';
  let isOpen = false;
  let isDismissed = false;
  let ctrl = null;
  let currentWidth = DEFAULT_WIDTH;
  let currentTabTopPct = DEFAULT_TAB_TOP_PCT;
  let pendingScrollTop = 0;
  let userToggled = false;

  function readSessionState() {
    try {
      const raw = sessionStorage.getItem(SESSION_KEY);
      return raw ? JSON.parse(raw) : null;
    } catch {
      return null;
    }
  }

  function writeSessionState(patch) {
    try {
      const prev = readSessionState() || {};
      sessionStorage.setItem(SESSION_KEY, JSON.stringify({ ...prev, ...patch }));
    } catch { /* ignore */ }
  }

  function applyWidth(w) {
    const maxW = Math.max(380, Math.round(window.innerWidth * 0.96));
    const clamped = Math.max(380, Math.min(maxW, Number(w) || DEFAULT_WIDTH));
    currentWidth = clamped;
    drawer.style.width = clamped + 'px';
    return clamped;
  }

  function clampTabTopPx(px) {
    const vh = Math.max(120, window.innerHeight || 720);
    const margin = 45;
    return Math.max(margin, Math.min(vh - margin, px));
  }

  function updateTabVisualTop(px) {
    let topPx = clampTabTopPx(px);
    if (isOpen && host.isConnected) {
      const dh = drawer.getBoundingClientRect().height;
      if (dh >= 90) {
        topPx = Math.min(topPx, Math.max(45, dh - 42));
      }
    }
    tab.style.top = Math.round(topPx) + 'px';
  }

  function applyTabTopPct(pct) {
    const vh = Math.max(120, window.innerHeight || 720);
    const rawPct = Number.isFinite(Number(pct)) ? Number(pct) : DEFAULT_TAB_TOP_PCT;
    const clampedPx = clampTabTopPx((rawPct / 100) * vh);
    const nextPct = Math.round((clampedPx / vh) * 1000) / 10;
    currentTabTopPct = nextPct;
    updateTabVisualTop(clampedPx);
    return nextPct;
  }

  function applyTabTopPx(px) {
    const vh = Math.max(120, window.innerHeight || 720);
    const clampedPx = clampTabTopPx(px);
    const nextPct = Math.round((clampedPx / vh) * 1000) / 10;
    currentTabTopPct = nextPct;
    updateTabVisualTop(clampedPx);
    return nextPct;
  }

  const savedSession = readSessionState();
  applyWidth(savedSession?.width ?? DEFAULT_WIDTH);
  applyTabTopPct(savedSession?.tabTopPct ?? DEFAULT_TAB_TOP_PCT);

  safeStorageGet(['drawerWidth', 'drawerTabTopPct', 'settings']).then(res => {
    const w = res.drawerWidth ?? res.settings?.drawerWidth;
    const topPct = res.drawerTabTopPct ?? res.settings?.drawerTabTopPct;
    if (w) {
      const appliedW = applyWidth(w);
      writeSessionState({ width: appliedW });
    }
    if (topPct !== undefined) {
      const appliedPct = applyTabTopPct(topPct);
      writeSessionState({ tabTopPct: appliedPct });
    }
  });

  const onStorageChanged = (changes, area) => {
    if (!isContextValid() || area !== 'local' || resizeDrag || tabDrag) return;
    if (changes.drawerWidth?.newValue) {
      const appliedW = applyWidth(changes.drawerWidth.newValue);
      writeSessionState({ width: appliedW });
    } else if (
      changes.settings?.oldValue &&
      changes.settings?.newValue?.drawerWidth &&
      changes.settings.newValue.drawerWidth !== changes.settings.oldValue.drawerWidth
    ) {
      const appliedW = applyWidth(changes.settings.newValue.drawerWidth);
      writeSessionState({ width: appliedW });
    }
    if (changes.drawerTabTopPct?.newValue !== undefined) {
      const appliedPct = applyTabTopPct(changes.drawerTabTopPct.newValue);
      writeSessionState({ tabTopPct: appliedPct });
    } else if (
      changes.settings?.oldValue &&
      changes.settings?.newValue?.drawerTabTopPct !== undefined &&
      changes.settings.newValue.drawerTabTopPct !== changes.settings.oldValue.drawerTabTopPct
    ) {
      const appliedPct = applyTabTopPct(changes.settings.newValue.drawerTabTopPct);
      writeSessionState({ tabTopPct: appliedPct });
    }
  };
  try {
    if (isContextValid()) {
      chrome.storage.onChanged.addListener(onStorageChanged);
    }
  } catch { /* ignore */ }

  const onResize = () => {
    if (!isContextValid() || !host.isConnected) return;
    applyWidth(currentWidth);
    applyTabTopPct(currentTabTopPct);
  };
  window.addEventListener('resize', onResize);

  function ensureMounted() {
    if (ctrl) return;
    ctrl = globalThis.__UsagePanelMount({
      root,
      container: scrollEl,
      scrollEl,
      isDrawer: true,
      isActive: () => isOpen && !isDismissed,
      onClose: () => dismiss(),
      onRendered: () => {
        if (pendingScrollTop > 0 && scrollEl.scrollHeight > scrollEl.clientHeight) {
          scrollEl.scrollTop = pendingScrollTop;
          pendingScrollTop = 0;
        }
        applyTabTopPct(currentTabTopPct);
      },
    });
  }

  function dismiss(opts) {
    isDismissed = true;
    isOpen = false;
    pendingScrollTop = 0;
    if (!opts?.fromRestore) userToggled = true;
    drawer.classList.remove('open');
    tab.setAttribute('aria-expanded', 'false');
    if (ctrl) {
      ctrl.destroy();
      ctrl = null;
    }
    scrollEl.innerHTML = '';
    host.remove();
    if (!opts?.skipSave && isContextValid()) {
      writeSessionState({ open: false, dismissed: true, scrollTop: 0 });
      safeSend({ type: 'setTabDrawerState', state: { open: false, dismissed: true } });
    }
  }

  function setOpen(next, opts) {
    if (!isContextValid()) {
      dismiss({ skipSave: true, fromRestore: opts?.fromRestore });
      return;
    }
    isOpen = Boolean(next);
    if (isOpen) isDismissed = false;
    if (!opts?.fromRestore) userToggled = true;
    tab.setAttribute('aria-expanded', isOpen ? 'true' : 'false');
    if (isOpen) {
      if (!host.isConnected && document.documentElement) document.documentElement.appendChild(host);
      ensureMounted();
      if (opts?.immediate) {
        drawer.classList.add('dragging');
        drawer.classList.add('open');
        void drawer.offsetWidth;
        drawer.classList.remove('dragging');
      } else {
        requestAnimationFrame(() => drawer.classList.add('open'));
      }
      ctrl?.refresh();
      applyTabTopPct(currentTabTopPct);
    } else {
      if (!isDismissed && !host.isConnected && document.documentElement) {
        document.documentElement.appendChild(host);
      }
      drawer.classList.remove('open');
      pendingScrollTop = 0;
      applyTabTopPct(currentTabTopPct);
    }
    if (!opts?.skipSave) {
      writeSessionState({ open: isOpen, dismissed: isDismissed, scrollTop: isOpen ? scrollEl.scrollTop : 0 });
      safeSend({ type: 'setTabDrawerState', state: { open: isOpen, dismissed: isDismissed } });
    }
  }

  let scrollTimer = null;
  scrollEl.addEventListener('scroll', () => {
    if (!isOpen || !isContextValid() || !host.isConnected) return;
    clearTimeout(scrollTimer);
    scrollTimer = setTimeout(() => {
      if (!isContextValid() || !host.isConnected) return;
      writeSessionState({ open: isOpen, scrollTop: scrollEl.scrollTop });
    }, 80);
  }, { passive: true });
  const onPageHide = () => {
    if (!isContextValid() || !host.isConnected) return;
    if (isOpen) writeSessionState({ open: true, scrollTop: scrollEl.scrollTop });
  };
  window.addEventListener('pagehide', onPageHide);

  function saveDrawerWidth(w) {
    writeSessionState({ width: w });
    safeStorageSet({ drawerWidth: w });
  }

  function saveTabTopPct(pct) {
    writeSessionState({ tabTopPct: pct });
    safeStorageSet({ drawerTabTopPct: pct });
  }

  // Toggle tab: click to open/close, horizontal drag to resize drawer width, vertical drag to move tab up/down
  let tabDrag = null;
  tab.addEventListener('pointerdown', e => {
    if (e.button !== 0) return;
    e.preventDefault();
    e.stopPropagation();
    const vh = Math.max(120, window.innerHeight || 720);
    tabDrag = {
      pointerId: e.pointerId,
      startX: e.clientX,
      startY: e.clientY,
      startW: drawer.getBoundingClientRect().width,
      startTopPx: parseFloat(tab.style.top) || (currentTabTopPct / 100) * vh,
      moved: false,
      axis: null,
      openedByDrag: false,
      resized: false,
    };
    tab.setPointerCapture(e.pointerId);
    tab.classList.add('active');
  });

  tab.addEventListener('pointermove', e => {
    if (!tabDrag || e.pointerId !== tabDrag.pointerId) return;
    const dx = e.clientX - tabDrag.startX;
    const dy = e.clientY - tabDrag.startY;
    if (!tabDrag.moved) {
      if (Math.hypot(dx, dy) < 5) return;
      tabDrag.moved = true;
      tabDrag.axis = Math.abs(dx) >= Math.abs(dy) ? 'x' : 'y';
      if (tabDrag.axis === 'x' && isOpen) {
        drawer.classList.add('dragging');
      }
    }
    if (tabDrag.axis === 'x') {
      if (!isOpen) {
        if (dx < -8 && !tabDrag.openedByDrag) {
          tabDrag.openedByDrag = true;
          setOpen(true);
        }
      } else if (!tabDrag.openedByDrag) {
        tabDrag.resized = true;
        applyWidth(tabDrag.startW - dx);
      }
    } else if (tabDrag.axis === 'y') {
      applyTabTopPx(tabDrag.startTopPx + dy);
    }
  });

  const endTabDrag = e => {
    if (!tabDrag || (e.pointerId !== undefined && e.pointerId !== tabDrag.pointerId)) return;
    const d = tabDrag;
    tabDrag = null;
    tab.classList.remove('active');
    drawer.classList.remove('dragging');
    if (d.pointerId !== undefined && tab.hasPointerCapture(d.pointerId)) {
      try { tab.releasePointerCapture(d.pointerId); } catch { /* ignore */ }
    }
    if (!d.moved) {
      setOpen(!isOpen);
      return;
    }
    if (d.axis === 'x' && d.resized) {
      saveDrawerWidth(Math.round(drawer.getBoundingClientRect().width));
    } else if (d.axis === 'y') {
      saveTabTopPct(currentTabTopPct);
    }
  };
  tab.addEventListener('pointerup', endTabDrag);
  tab.addEventListener('pointercancel', endTabDrag);
  tab.addEventListener('lostpointercapture', endTabDrag);

  // Also allow resizing along the rest of the left drawer border when open
  let resizeDrag = null;
  resizer.addEventListener('pointerdown', e => {
    if (!isOpen || e.button !== 0) return;
    e.preventDefault();
    e.stopPropagation();
    resizeDrag = { startX: e.clientX, startW: drawer.getBoundingClientRect().width };
    resizer.setPointerCapture(e.pointerId);
    resizer.classList.add('active');
    drawer.classList.add('dragging');
  });
  resizer.addEventListener('pointermove', e => {
    if (!resizeDrag) return;
    applyWidth(resizeDrag.startW + (resizeDrag.startX - e.clientX));
  });
  const endResize = () => {
    if (!resizeDrag) return;
    const finalW = Math.round(drawer.getBoundingClientRect().width);
    resizeDrag = null;
    resizer.classList.remove('active');
    drawer.classList.remove('dragging');
    saveDrawerWidth(finalW);
  };
  resizer.addEventListener('pointerup', endResize);
  resizer.addEventListener('pointercancel', endResize);

  const onRuntimeMessage = (msg, _sender, sendResponse) => {
    if (!isContextValid() || !host.isConnected) return false;
    if (msg?.type === 'USAGE_DRAWER_TOGGLE') {
      setOpen(isDismissed ? true : !isOpen);
      sendResponse({ ok: true, open: isOpen, dismissed: isDismissed });
      return false;
    }
    if (msg?.type === 'USAGE_DRAWER_STATE_UPDATED') {
      ctrl?.sync();
      return false;
    }
    return false;
  };
  try {
    if (isContextValid()) {
      chrome.runtime.onMessage.addListener(onRuntimeMessage);
    }
  } catch { /* ignore */ }

  function teardown() {
    clearTimeout(scrollTimer);
    if (isOpen) {
      writeSessionState({ open: true, dismissed: false, scrollTop: scrollEl.scrollTop });
    }
    window.removeEventListener('resize', onResize);
    window.removeEventListener('pagehide', onPageHide);
    try { chrome.storage.onChanged.removeListener(onStorageChanged); } catch { /* ignore */ }
    try { chrome.runtime.onMessage.removeListener(onRuntimeMessage); } catch { /* ignore */ }
    if (ctrl) {
      ctrl.destroy();
      ctrl = null;
    }
    host.remove();
    if (globalThis[GLOBAL_KEY]?.teardown === teardown) {
      delete globalThis[GLOBAL_KEY];
    }
  }

  if (savedSession?.dismissed) {
    dismiss({ skipSave: true, fromRestore: true });
  } else {
    if (document.documentElement) {
      document.documentElement.appendChild(host);
    }
    if (savedSession?.open) {
      pendingScrollTop = Math.max(0, Number(savedSession.scrollTop) || 0);
      setOpen(true, { immediate: true, skipSave: true, fromRestore: true });
    } else if (savedSession === null) {
      safeSend({ type: 'getTabDrawerState' }).then(res => {
        if (userToggled || !isContextValid()) return;
        const st = res?.state;
        if (st?.dismissed) {
          dismiss({ skipSave: true, fromRestore: true });
          writeSessionState({ open: false, dismissed: true, scrollTop: 0 });
        } else if (st?.open && !isOpen) {
          setOpen(true, { immediate: true, skipSave: true, fromRestore: true });
          writeSessionState({ open: true, dismissed: false, scrollTop: 0 });
        }
      });
    }
  }

  globalThis[GLOBAL_KEY] = { setOpen, toggle: () => setOpen(isDismissed ? true : !isOpen), dismiss, teardown };
})();
