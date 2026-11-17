// 🤖[AI 生成・編集を含む (Antigravity)]
// Claude Usage Panel - MV3 Service Worker
const PROVIDER = 'claude';
const HOST_NAME = 'com.kyoya.usage_panel';
const ALARM_NAME = 'periodic-claude-usage';

const DEFAULT_SETTINGS = {
  intervalMinutes: 10, // 0 = OFF, 5, 10, 15, 30
  drawerWidth: 690,
  drawerTabTopPct: 42,
  showProj: false,
};

const DEFAULT_STATUS = {
  hostOk: null,
  lastError: null,
  lastSampleAt: null,
  lastReloadAt: null,
  sampling: false,
  lastResult: null,
};

let opQueue = Promise.resolve();
let alarmSyncQueue = Promise.resolve();
let lastCatchUpTryMs = 0;
let msgSeq = 0;
const drawerTabs = new Set();

function enqueue(fn) {
  const next = opQueue.then(fn, fn);
  opQueue = next.catch(() => {});
  return next;
}

async function getStorage(keys) {
  return chrome.storage.local.get(keys);
}

async function setStorage(obj) {
  return chrome.storage.local.set(obj);
}

async function getSettings() {
  const { settings } = await getStorage(['settings']);
  return { ...DEFAULT_SETTINGS, ...(settings || {}) };
}

async function getStatus() {
  const { status } = await getStorage(['status']);
  return { ...DEFAULT_STATUS, ...(status || {}) };
}

function broadcastState() {
  chrome.runtime.sendMessage({ type: 'state-updated' }).catch(() => {});
  for (const tabId of drawerTabs) {
    chrome.tabs.sendMessage(tabId, { type: 'USAGE_DRAWER_STATE_UPDATED' }).catch(() => {
      drawerTabs.delete(tabId);
    });
  }
}

function sendNative(payload) {
  return new Promise((resolve, reject) => {
    const req = { id: ++msgSeq, ...payload };
    chrome.runtime.sendNativeMessage(HOST_NAME, req, response => {
      if (chrome.runtime.lastError) {
        reject(new Error(chrome.runtime.lastError.message));
        return;
      }
      if (!response) {
        reject(new Error('Native Host から空の応答が返りました'));
        return;
      }
      if (!response.ok) {
        reject(new Error(response.error || 'Native Host エラー'));
        return;
      }
      resolve(response);
    });
  });
}

function getLatestSummary(data) {
  const S = data?.samples || [];
  const last = S[S.length - 1];
  if (!last) return null;
  const rem5h = typeof last.sessionPct === 'number' ? Math.max(0, 100 - last.sessionPct) : null;
  const rem7d = typeof last.weekPct === 'number' ? Math.max(0, 100 - last.weekPct) : null;
  const remAlt = typeof last.weekAltPct === 'number' ? Math.max(0, 100 - last.weekAltPct) : null;
  return { ts: last.ts, rem5h, rem7d, remAlt, altLabel: last.weekAltLabel || '' };
}

async function updateBadge() {
  const [{ data }, status] = await Promise.all([
    getStorage(['data']),
    getStatus(),
  ]);
  const sum = getLatestSummary(data);

  let badgeText = '';
  let badgeColor = '#6b6b65';
  if (status.sampling) {
    badgeText = '...';
    badgeColor = '#3c6fb4';
  } else if (status.hostOk === false && !sum) {
    badgeText = '!';
    badgeColor = '#d93025';
  } else {
    // 5h / 7d のうち残量が少ない方を表示。<=20% 黄、<=1% 赤
    const vals = [sum?.rem5h, sum?.rem7d, sum?.remAlt].filter(v => typeof v === 'number');
    if (vals.length) {
      const minRem = Math.min(...vals);
      badgeText = `${Math.round(minRem)}%`;
      badgeColor = minRem <= 1 ? '#d93025' : minRem <= 20 ? '#d9a100' : '#1baf7a';
    }
  }

  await chrome.action.setBadgeText({ text: badgeText });
  await chrome.action.setBadgeBackgroundColor({ color: badgeColor });
  await chrome.action.setBadgeTextColor({ color: '#ffffff' });

  const lines = ['Claude Usage Panel (クリックでドロワー開閉)'];
  if (sum) {
    lines.push(`5h残量: ${sum.rem5h ?? '-'}% / 7d残量: ${sum.rem7d ?? '-'}%` + (sum.remAlt !== null ? ` / 7d(${sum.altLabel}): ${sum.remAlt}%` : ''));
  }
  if (status.lastSampleAt) {
    lines.push(`最終/usage採取: ${new Date(status.lastSampleAt).toLocaleString('ja-JP')}`);
  }
  if (status.hostOk === false && status.lastError) {
    lines.push(`Hostエラー: ${status.lastError}`);
  }
  await chrome.action.setTitle({ title: lines.join('\n') });
}

function getLatestSampleMs(data, status) {
  const samples = data?.samples;
  const lastSampleTs = Array.isArray(samples) && samples.length ? Date.parse(samples[samples.length - 1].ts) : 0;
  const statusTs = status?.lastSampleAt ? Date.parse(status.lastSampleAt) : 0;
  return Math.max(Number.isFinite(lastSampleTs) ? lastSampleTs : 0, Number.isFinite(statusTs) ? statusTs : 0);
}

function syncAlarm(opts = {}) {
  const forceReset = typeof opts === 'boolean' ? opts : Boolean(opts.forceReset);
  const allowCatchUp = typeof opts === 'object' ? Boolean(opts.allowCatchUp) : false;
  const run = async () => {
    const [settings, status, store] = await Promise.all([
      getSettings(),
      getStatus(),
      getStorage(['data']),
    ]);
    const minutes = Number(settings.intervalMinutes) || 0;
    if (minutes <= 0) {
      await chrome.alarms.clear(ALARM_NAME);
      return null;
    }
    const periodMs = minutes * 60000;
    const now = Date.now();
    const lastMs = getLatestSampleMs(store.data, status);
    const remainMs = lastMs > 0 ? (lastMs + periodMs - now) : periodMs;
    const overdue = lastMs > 0 && remainMs <= 0;
    const lastErrTs = status.lastResult?.error && status.lastResult?.ts ? Date.parse(status.lastResult.ts) : 0;
    const recentError = lastErrTs > 0 && (now - lastErrTs < periodMs);

    if (allowCatchUp && overdue && !status.sampling && !recentError && (now - lastCatchUpTryMs >= 60000)) {
      lastCatchUpTryMs = now;
      await chrome.alarms.clear(ALARM_NAME);
      await chrome.alarms.create(ALARM_NAME, {
        delayInMinutes: minutes,
        periodInMinutes: minutes,
      });
      void runSample(true, 'catchup');
      const created = await chrome.alarms.get(ALARM_NAME);
      return created?.scheduledTime || null;
    }

    const existing = await chrome.alarms.get(ALARM_NAME);
    const existingValid = existing &&
      existing.periodInMinutes === minutes &&
      existing.scheduledTime > now - 30000 &&
      existing.scheduledTime <= now + periodMs + 60000;
    if (!forceReset && existingValid) {
      return existing.scheduledTime;
    }

    const delayMin = lastMs > 0 && !overdue
      ? Math.max(0.5, remainMs / 60000)
      : minutes;
    await chrome.alarms.clear(ALARM_NAME);
    await chrome.alarms.create(ALARM_NAME, {
      delayInMinutes: delayMin,
      periodInMinutes: minutes,
    });
    const created = await chrome.alarms.get(ALARM_NAME);
    return created?.scheduledTime || null;
  };
  const next = alarmSyncQueue.then(run, run);
  alarmSyncQueue = next.catch(() => {});
  return next;
}

async function reloadData(checkAlarm = true) {
  const res = await enqueue(async () => {
    const status = await getStatus();
    try {
      const nativeRes = await sendNative({ action: 'getData', provider: PROVIDER });
      status.hostOk = true;
      status.lastError = null;
      status.lastReloadAt = new Date().toISOString();
      await setStorage({ data: nativeRes.data, status });
      await updateBadge();
      broadcastState();
      return { ok: true };
    } catch (err) {
      status.hostOk = false;
      status.lastError = err.message;
      status.lastReloadAt = new Date().toISOString();
      await setStorage({ status });
      await updateBadge();
      broadcastState();
      return { ok: false, error: err.message };
    }
  });
  if (checkAlarm && res?.ok) {
    void syncAlarm({ allowCatchUp: true });
  }
  return res;
}

async function runSample(force = true, source = 'manual') {
  const res = await enqueue(async () => {
    const status = await getStatus();
    status.sampling = true;
    status.lastError = null;
    await setStorage({ status });
    await updateBadge();
    broadcastState();

    try {
      const nativeRes = await sendNative({ action: 'sample', provider: PROVIDER, force });
      const nextStatus = await getStatus();
      nextStatus.sampling = false;
      nextStatus.hostOk = true;
      nextStatus.lastError = null;
      nextStatus.lastSampleAt = nativeRes.sampledAt || new Date().toISOString();
      nextStatus.lastResult = {
        ts: nextStatus.lastSampleAt,
        skipped: Boolean(nativeRes.skipped),
        reason: nativeRes.reason || null,
        appended: Boolean(nativeRes.appended),
      };
      await setStorage({ data: nativeRes.data, status: nextStatus });
      await updateBadge();
      broadcastState();
      return { ok: true };
    } catch (err) {
      const nextStatus = await getStatus();
      nextStatus.sampling = false;
      if (/Native host/i.test(err.message) || /specified native messaging host not found/i.test(err.message)) {
        nextStatus.hostOk = false;
      }
      nextStatus.lastError = err.message;
      nextStatus.lastResult = { ts: new Date().toISOString(), error: err.message };
      await setStorage({ status: nextStatus });
      await updateBadge();
      broadcastState();
      return { ok: false, error: err.message };
    }
  });
  if (res?.ok && source !== 'alarm') {
    await syncAlarm({ forceReset: true, allowCatchUp: false });
  }
  return res;
}

async function openOrFocusPanelTab() {
  const url = chrome.runtime.getURL('panel.html');
  const tabs = await chrome.tabs.query({ url });
  if (tabs.length > 0) {
    const tab = tabs[0];
    await chrome.tabs.update(tab.id, { active: true });
    if (tab.windowId !== undefined) {
      await chrome.windows.update(tab.windowId, { focused: true });
    }
    return;
  }
  await chrome.tabs.create({ url });
}

function isRestrictedUrl(url = '') {
  return (
    !url ||
    url.startsWith('chrome://') ||
    url.startsWith('edge://') ||
    url.startsWith('about:') ||
    url.startsWith('devtools://') ||
    url.startsWith('chrome-extension://') ||
    url.startsWith('https://chrome.google.com/webstore') ||
    url.startsWith('https://chromewebstore.google.com')
  );
}

chrome.action.onClicked.addListener(async tab => {
  if (!tab?.id || isRestrictedUrl(tab.url)) {
    await openOrFocusPanelTab();
    return;
  }
  drawerTabs.add(tab.id);
  try {
    await chrome.tabs.sendMessage(tab.id, { type: 'USAGE_DRAWER_TOGGLE' });
  } catch {
    try {
      await chrome.scripting.executeScript({
        target: { tabId: tab.id },
        files: ['styles.js', 'panel_core.js', 'drawer.js'],
      });
      await chrome.tabs.sendMessage(tab.id, { type: 'USAGE_DRAWER_TOGGLE' });
    } catch {
      drawerTabs.delete(tab.id);
      await openOrFocusPanelTab();
    }
  }
});

async function getTabDrawerState(tabId) {
  if (!tabId) return null;
  try {
    const { drawerTabStates } = await chrome.storage.session.get(['drawerTabStates']);
    return drawerTabStates?.[String(tabId)] || null;
  } catch {
    return null;
  }
}

async function setTabDrawerState(tabId, state) {
  if (!tabId) return;
  try {
    const { drawerTabStates } = await chrome.storage.session.get(['drawerTabStates']);
    const next = { ...(drawerTabStates || {}) };
    if (state && (state.open || state.dismissed)) {
      next[String(tabId)] = { open: Boolean(state.open), dismissed: Boolean(state.dismissed) };
    } else {
      delete next[String(tabId)];
    }
    await chrome.storage.session.set({ drawerTabStates: next });
  } catch { /* ignore */ }
}

chrome.tabs.onRemoved.addListener(tabId => {
  drawerTabs.delete(tabId);
  setTabDrawerState(tabId, null);
});

async function reinjectDrawerTabs() {
  try {
    const tabs = await chrome.tabs.query({ url: ['http://*/*', 'https://*/*'] });
    for (const tab of tabs) {
      if (!tab?.id || isRestrictedUrl(tab.url)) continue;
      chrome.scripting.executeScript({
        target: { tabId: tab.id },
        files: ['styles.js', 'panel_core.js', 'drawer.js'],
      }).catch(() => {});
    }
  } catch { /* ignore */ }
}

chrome.runtime.onInstalled.addListener(async () => {
  const settings = await getSettings();
  await setStorage({ settings });
  await reinjectDrawerTabs();
  await reloadData(false);
  await syncAlarm({ forceReset: false, allowCatchUp: true });
});

chrome.runtime.onStartup.addListener(async () => {
  await reloadData(false);
  await syncAlarm({ forceReset: false, allowCatchUp: true });
});

chrome.alarms.onAlarm.addListener(async alarm => {
  if (alarm.name !== ALARM_NAME) return;
  const settings = await getSettings();
  if ((Number(settings.intervalMinutes) || 0) <= 0) return;
  await runSample(true, 'alarm');
});

chrome.runtime.onMessage.addListener((msg, sender, sendResponse) => {
  if (sender?.tab?.id) drawerTabs.add(sender.tab.id);
  (async () => {
    if (msg?.type === 'getState') {
      const [store, settings, status, alarm] = await Promise.all([
        getStorage(['data', 'prefs']),
        getSettings(),
        getStatus(),
        chrome.alarms.get(ALARM_NAME),
      ]);
      const nextAlarmTime = alarm?.scheduledTime || ((Number(settings.intervalMinutes) || 0) > 0 ? await syncAlarm({ forceReset: false, allowCatchUp: true }) : null);
      return {
        settings,
        status,
        data: store.data || null,
        prefs: store.prefs || {},
        nextAlarmTime,
      };
    }
    if (msg?.type === 'getTabDrawerState') {
      return { ok: true, state: await getTabDrawerState(sender?.tab?.id) };
    }
    if (msg?.type === 'setTabDrawerState') {
      await setTabDrawerState(sender?.tab?.id, msg.state || null);
      return { ok: true };
    }
    if (msg?.type === 'reloadData') {
      return await reloadData();
    }
    if (msg?.type === 'runSample') {
      return await runSample(msg.force !== false, 'manual');
    }
    if (msg?.type === 'updateSettings') {
      const prev = await getSettings();
      const next = { ...prev, ...(msg.settings || {}) };
      await setStorage({ settings: next });
      const forceReset = prev.intervalMinutes !== next.intervalMinutes;
      const nextAlarmTime = await syncAlarm({
        forceReset,
        allowCatchUp: forceReset && (Number(next.intervalMinutes) || 0) > 0,
      });
      await updateBadge();
      broadcastState();
      return { ok: true, settings: next, nextAlarmTime };
    }
    if (msg?.type === 'pingHost') {
      try {
        const res = await sendNative({ action: 'ping' });
        const status = await getStatus();
        status.hostOk = true;
        status.lastError = null;
        await setStorage({ status });
        await updateBadge();
        broadcastState();
        return { ok: true, ping: res };
      } catch (err) {
        const status = await getStatus();
        status.hostOk = false;
        status.lastError = err.message;
        await setStorage({ status });
        await updateBadge();
        broadcastState();
        return { ok: false, error: err.message };
      }
    }
    if (msg?.type === 'openTab') {
      await openOrFocusPanelTab();
      return { ok: true };
    }
    return { ok: false, error: 'Unknown message type' };
  })()
    .then(sendResponse)
    .catch(err => sendResponse({ ok: false, error: err.message }));
  return true;
});
