// LinkCertVerifier - Background Service Worker (MV3)
const MENU_ID = 'lcv-verify-link-address';
const NATIVE_HOST = 'com.kyoya.link_cert_verifier';
const INSTALL_COMMAND = 'node C:\\Users\\kyoya\\projs\\samples\\2026\\ChromeExt\\LinkCertVerifier\\host\\install.js';

function registerContextMenu() {
  try {
    chrome.contextMenus.removeAll(() => {
      void chrome.runtime.lastError;
      chrome.contextMenus.create({
        id: MENU_ID,
        title: 'リンクのアドレスを検証',
        contexts: ['link'],
      }, () => {
        void chrome.runtime.lastError;
      });
    });
  } catch (e) {
    console.warn('[LinkCertVerifier] contextMenus setup failed:', e);
  }
}

chrome.runtime.onInstalled.addListener(() => {
  registerContextMenu();
});

chrome.runtime.onStartup.addListener(() => {
  registerContextMenu();
});

// Fallback client-side URL analyzer when Native Messaging Host is not yet registered
function fallbackAnalyzeUrl(rawUrl, linkText, nativeError) {
  let parsed = null;
  try {
    parsed = new URL(rawUrl);
  } catch (e) {
    return {
      inspectedAt: new Date().toISOString(),
      originalUrl: rawUrl,
      nativeHostMissing: true,
      nativeError,
      installCommand: INSTALL_COMMAND,
      urlInfo: { validSyntax: false, rawUrl, error: e.message, warnings: [] },
      dnsInfo: null,
      tlsInfo: null,
      verdict: {
        level: 'error',
        title: 'URL 形式が不正です',
        summary: e.message,
        warnings: [],
        errors: [e.message],
      },
    };
  }

  const protocol = parsed.protocol.toLowerCase();
  const hostname = parsed.hostname || '';
  const hasUserinfo = Boolean(parsed.username || parsed.password);
  const warnings = [];

  if (hasUserinfo) {
    warnings.push({
      code: 'USERINFO_IN_URL',
      severity: 'danger',
      message: `URL に認証情報 (${parsed.username}@...) が含まれています。接続先ドメインを誤認させる偽装リンクの可能性があります。`,
    });
  }
  if (hostname.split('.').some(l => l.startsWith('xn--'))) {
    warnings.push({
      code: 'IDN_PUNYCODE',
      severity: 'warn',
      message: `Punycode ドメイン (${hostname}) です。ホモグラフ攻撃にご注意ください。`,
    });
  }

  return {
    inspectedAt: new Date().toISOString(),
    originalUrl: rawUrl,
    nativeHostMissing: true,
    nativeError,
    installCommand: INSTALL_COMMAND,
    urlInfo: {
      validSyntax: true,
      rawUrl,
      href: parsed.href,
      protocol,
      hostname,
      unicodeHostname: hostname,
      isPunycode: hostname.includes('xn--'),
      port: parsed.port ? parseInt(parsed.port, 10) : (protocol === 'https:' ? 443 : protocol === 'http:' ? 80 : null),
      pathname: parsed.pathname + parsed.search,
      hasUserinfo,
      linkText: String(linkText || '').trim(),
      warnings,
    },
    dnsInfo: null,
    tlsInfo: null,
    verdict: {
      level: 'warn',
      title: 'Native Messaging Host 未登録のため TLS 証明書を取得できませんでした',
      summary: 'TLS 証明書検証ホスト (com.kyoya.link_cert_verifier) を登録してください。',
      warnings,
      errors: nativeError ? [nativeError] : [],
    },
  };
}

function callNativeHost(payload) {
  return new Promise((resolve) => {
    try {
      chrome.runtime.sendNativeMessage(NATIVE_HOST, payload, (response) => {
        if (chrome.runtime.lastError) {
          resolve({ ok: false, error: chrome.runtime.lastError.message });
          return;
        }
        resolve(response || { ok: false, error: 'Empty response from native host' });
      });
    } catch (err) {
      resolve({ ok: false, error: err?.message || String(err) });
    }
  });
}

async function runVerification(url, linkText, options = {}) {
  const res = await callNativeHost({
    id: `req_${Date.now()}_${Math.random().toString(36).slice(2, 7)}`,
    action: 'verifyLink',
    url,
    linkText: linkText || '',
    probeHttps: Boolean(options.probeHttps),
    followRedirects: Boolean(options.followRedirects),
  });

  if (res && res.ok && res.result) {
    return res.result;
  }

  const errMsg = res?.error || 'Native Messaging Host との通信に失敗しました';
  const isMissingHost = /not found|forbidden|specified native messaging host/i.test(errMsg);
  if (isMissingHost) {
    return fallbackAnalyzeUrl(url, linkText, errMsg);
  }

  const fallback = fallbackAnalyzeUrl(url, linkText, errMsg);
  fallback.nativeHostMissing = false;
  fallback.verdict = {
    level: 'error',
    title: '検証処理エラー',
    summary: errMsg,
    warnings: fallback.urlInfo?.warnings || [],
    errors: [errMsg],
  };
  return fallback;
}

async function saveSessionResult(requestId, state) {
  try {
    await chrome.storage.session.set({ [`lcv:${requestId}`]: state });
  } catch {
    try {
      await chrome.storage.local.set({ [`lcv:${requestId}`]: state });
    } catch { /* ignore */ }
  }
}

async function openFallbackWindow(requestId) {
  const url = chrome.runtime.getURL(`result.html?requestId=${encodeURIComponent(requestId)}`);
  await chrome.windows.create({
    url,
    type: 'popup',
    width: 720,
    height: 780,
  });
}

async function sendToTab(tabId, message) {
  return new Promise((resolve) => {
    chrome.tabs.sendMessage(tabId, message, { frameId: 0 }, (resp) => {
      if (chrome.runtime.lastError) {
        resolve({ ok: false, error: chrome.runtime.lastError.message });
        return;
      }
      resolve(resp || { ok: true });
    });
  });
}

async function ensureInjected(tabId) {
  const ping = await sendToTab(tabId, { type: 'LCV_PING' });
  if (ping && ping.ok && ping.pong) return true;
  await chrome.scripting.executeScript({
    target: { tabId },
    files: ['viewer.js', 'content.js'],
  });
  return true;
}

chrome.contextMenus.onClicked.addListener(async (info, tab) => {
  if (info.menuItemId !== MENU_ID || !info.linkUrl) return;

  const requestId = `lcv_${Date.now()}_${Math.random().toString(36).slice(2, 8)}`;
  const url = info.linkUrl;
  const linkText = info.linkText || '';

  const initialState = {
    requestId,
    status: 'loading',
    url,
    linkText,
    result: null,
  };
  await saveSessionResult(requestId, initialState);

  let useTabOverlay = false;
  if (tab && typeof tab.id === 'number' && tab.id >= 0) {
    try {
      await ensureInjected(tab.id);
      const shown = await sendToTab(tab.id, {
        type: 'LCV_SHOW_LOADING',
        requestId,
        url,
        linkText,
      });
      if (shown && shown.ok) {
        useTabOverlay = true;
      }
    } catch {
      useTabOverlay = false;
    }
  }

  if (!useTabOverlay) {
    await openFallbackWindow(requestId);
  }

  const result = await runVerification(url, linkText, {});
  const doneState = {
    requestId,
    status: 'done',
    url,
    linkText,
    result,
  };
  await saveSessionResult(requestId, doneState);

  if (useTabOverlay && tab?.id >= 0) {
    await sendToTab(tab.id, {
      type: 'LCV_SHOW_RESULT',
      requestId,
      result,
    });
  } else {
    chrome.runtime.sendMessage({
      type: 'LCV_WINDOW_RESULT',
      requestId,
      state: doneState,
    }, () => { void chrome.runtime.lastError; });
  }
});

chrome.runtime.onMessage.addListener((message, sender, sendResponse) => {
  if (message?.type === 'LCV_VERIFY_REQUEST') {
    runVerification(message.url, message.linkText || '', message.options || {})
      .then((result) => {
        sendResponse({ ok: true, result });
      })
      .catch((err) => {
        sendResponse({ ok: false, error: err?.message || String(err) });
      });
    return true;
  }
});
