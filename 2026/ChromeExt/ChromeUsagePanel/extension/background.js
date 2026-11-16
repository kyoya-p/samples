// 🤖[AI 生成・編集を含む (Antigravity)]
// AI Usage Panel - MV3 Service Worker
const HOST_NAME = 'com.kyoya.usage_panel';
const ALARM_NAME = 'periodic-usage-sample';

const DEFAULT_SETTINGS = {
  intervalMinutes: 10, // 0 = OFF, 5, 10, 15, 30
  enableClaude: true,
  enableAgy: true,
  activeProvider: 'both', // 'both' | 'claude' | 'agy'
};

const DEFAULT_STATUS = {
  hostOk: null,
  lastError: null,
  lastSampleAt: null,
  lastReloadAt: null,
  sampling: { claude: false, agy: false },
  lastResult: {},
};

let opQueue = Promise.resolve();
let msgSeq = 0;

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
  return {
    ...DEFAULT_STATUS,
    ...(status || {}),
    sampling: { ...DEFAULT_STATUS.sampling, ...(status?.sampling || {}) },
    lastResult: { ...(status?.lastResult || {}) },
  };
}

function broadcastState() {
  chrome.runtime.sendMessage({ type: 'state-updated' }).catch(() => {});
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

function getLatestClaudeSummary(data) {
  const S = data?.samples || [];
  const last = S[S.length - 1];
  if (!last) return null;
  const rem5h = typeof last.sessionPct === 'number' ? Math.max(0, 100 - last.sessionPct) : null;
  const rem7d = typeof last.weekPct === 'number' ? Math.max(0, 100 - last.weekPct) : null;
  return { ts: last.ts, rem5h, rem7d };
}

function getLatestAgySummary(data) {
  const S = data?.samples || [];
  const last = S[S.length - 1];
  if (!last?.limits) return null;
  const g5 = last.limits.gemini_5h;
  const gw = last.limits.gemini_week;
  const ga = last.limits.gemini_argon_week;
  const rem = lim => lim ? (typeof lim.pct === 'number' ? lim.pct : (100 - (lim.usedPct ?? 100))) : null;
  return {
    ts: last.ts,
    rem5h: rem(g5),
    rem7d: rem(gw),
    remArgon7d: rem(ga),
  };
}

async function updateBadge() {
  const [{ data_claude, data_agy }, settings, status] = await Promise.all([
    getStorage(['data_claude', 'data_agy']),
    getSettings(),
    getStatus(),
  ]);

  const cSum = getLatestClaudeSummary(data_claude);
  const aSum = getLatestAgySummary(data_agy);

  const candidates = [];
  if ((settings.activeProvider === 'claude' || settings.activeProvider === 'both') && cSum?.rem5h !== null && cSum?.rem5h !== undefined) {
    candidates.push(cSum.rem5h);
  }
  if ((settings.activeProvider === 'agy' || settings.activeProvider === 'both') && aSum?.rem5h !== null && aSum?.rem5h !== undefined) {
    candidates.push(aSum.rem5h);
  }

  let badgeText = '';
  let badgeColor = '#6b6b65';
  if (status.sampling.claude || status.sampling.agy) {
    badgeText = '...';
    badgeColor = '#3c6fb4';
  } else if (status.hostOk === false && candidates.length === 0) {
    badgeText = '!';
    badgeColor = '#d93025';
  } else if (candidates.length > 0) {
    const minRem = Math.min(...candidates);
    badgeText = `${Math.round(minRem)}%`;
    badgeColor = minRem > 40 ? '#1baf7a' : minRem > 15 ? '#d08c4a' : '#d93025';
  }

  await chrome.action.setBadgeText({ text: badgeText });
  await chrome.action.setBadgeBackgroundColor({ color: badgeColor });

  const lines = ['AI Usage Panel'];
  if (cSum) {
    lines.push(`Claude Code: 5h残 ${cSum.rem5h ?? '-'}% / 7d残 ${cSum.rem7d ?? '-'}%`);
  }
  if (aSum) {
    lines.push(`Antigravity: Gemini 5h残 ${aSum.rem5h ?? '-'}% / 7d残 ${aSum.rem7d ?? '-'}%` + (aSum.remArgon7d !== null ? ` / Argon 7d残 ${aSum.remArgon7d}%` : ''));
  }
  if (status.lastSampleAt) {
    lines.push(`最終/usage採取: ${new Date(status.lastSampleAt).toLocaleString('ja-JP')}`);
  }
  if (status.hostOk === false && status.lastError) {
    lines.push(`Hostエラー: ${status.lastError}`);
  }
  await chrome.action.setTitle({ title: lines.join('\n') });
}

async function syncAlarm(forceReset = false) {
  const settings = await getSettings();
  const minutes = Number(settings.intervalMinutes) || 0;
  if (minutes <= 0) {
    await chrome.alarms.clear(ALARM_NAME);
    return null;
  }
  const existing = await chrome.alarms.get(ALARM_NAME);
  if (!forceReset && existing && existing.periodInMinutes === minutes) {
    return existing.scheduledTime;
  }
  await chrome.alarms.clear(ALARM_NAME);
  await chrome.alarms.create(ALARM_NAME, {
    delayInMinutes: minutes,
    periodInMinutes: minutes,
  });
  const created = await chrome.alarms.get(ALARM_NAME);
  return created?.scheduledTime || null;
}

async function reloadProviders(providers = ['claude', 'agy']) {
  return enqueue(async () => {
    const status = await getStatus();
    const updates = {};
    let anyError = null;
    let anyOk = false;

    for (const p of providers) {
      try {
        const res = await sendNative({ action: 'getData', provider: p });
        updates[`data_${p}`] = res.data;
        anyOk = true;
      } catch (err) {
        anyError = `${p}: ${err.message}`;
      }
    }

    status.hostOk = anyOk ? true : (anyError ? false : status.hostOk);
    status.lastError = anyError;
    status.lastReloadAt = new Date().toISOString();
    await setStorage({ ...updates, status });
    await updateBadge();
    broadcastState();
    return { ok: !anyError, error: anyError };
  });
}

async function sampleProviders(providers = ['claude', 'agy'], force = true) {
  return enqueue(async () => {
    let anyError = null;
    for (const p of providers) {
      const status = await getStatus();
      status.sampling[p] = true;
      status.lastError = null;
      await setStorage({ status });
      await updateBadge();
      broadcastState();

      try {
        const res = await sendNative({ action: 'sample', provider: p, force });
        const nextStatus = await getStatus();
        nextStatus.sampling[p] = false;
        nextStatus.hostOk = true;
        nextStatus.lastError = null;
        nextStatus.lastSampleAt = res.sampledAt || new Date().toISOString();
        nextStatus.lastResult[p] = {
          ts: nextStatus.lastSampleAt,
          skipped: Boolean(res.skipped),
          reason: res.reason || null,
          appended: Boolean(res.appended),
        };
        await setStorage({
          [`data_${p}`]: res.data,
          status: nextStatus,
        });
      } catch (err) {
        anyError = `${p}: ${err.message}`;
        const nextStatus = await getStatus();
        nextStatus.sampling[p] = false;
        if (/Native host/i.test(err.message) || /specified native messaging host not found/i.test(err.message)) {
          nextStatus.hostOk = false;
        }
        nextStatus.lastError = anyError;
        nextStatus.lastResult[p] = {
          ts: new Date().toISOString(),
          error: err.message,
        };
        await setStorage({ status: nextStatus });
      }
      await updateBadge();
      broadcastState();
    }
    return { ok: !anyError, error: anyError };
  });
}

async function openOrFocusPanel() {
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

chrome.action.onClicked.addListener(() => {
  openOrFocusPanel();
});

chrome.runtime.onInstalled.addListener(async () => {
  const settings = await getSettings();
  await setStorage({ settings });
  await syncAlarm(false);
  await reloadProviders(['claude', 'agy']);
});

chrome.runtime.onStartup.addListener(async () => {
  await syncAlarm(false);
  await reloadProviders(['claude', 'agy']);
});

chrome.alarms.onAlarm.addListener(async alarm => {
  if (alarm.name !== ALARM_NAME) return;
  const settings = await getSettings();
  if ((Number(settings.intervalMinutes) || 0) <= 0) return;
  const targets = [];
  if (settings.enableClaude) targets.push('claude');
  if (settings.enableAgy) targets.push('agy');
  if (targets.length === 0) return;
  await sampleProviders(targets, true);
});

chrome.runtime.onMessage.addListener((msg, _sender, sendResponse) => {
  (async () => {
    if (msg?.type === 'getState') {
      const [store, settings, status, alarm] = await Promise.all([
        getStorage(['data_claude', 'data_agy']),
        getSettings(),
        getStatus(),
        chrome.alarms.get(ALARM_NAME),
      ]);
      // Reset stale sampling flags if no queue work or > 5m old
      return {
        settings,
        status,
        data_claude: store.data_claude || null,
        data_agy: store.data_agy || null,
        nextAlarmTime: alarm?.scheduledTime || null,
      };
    }
    if (msg?.type === 'reloadData') {
      const providers = Array.isArray(msg.providers) ? msg.providers : ['claude', 'agy'];
      return await reloadProviders(providers);
    }
    if (msg?.type === 'runSample') {
      const providers = Array.isArray(msg.providers) ? msg.providers : ['claude', 'agy'];
      return await sampleProviders(providers, msg.force !== false);
    }
    if (msg?.type === 'updateSettings') {
      const prev = await getSettings();
      const next = { ...prev, ...(msg.settings || {}) };
      await setStorage({ settings: next });
      const forceReset = prev.intervalMinutes !== next.intervalMinutes;
      const nextAlarmTime = await syncAlarm(forceReset);
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
    return { ok: false, error: 'Unknown message type' };
  })()
    .then(sendResponse)
    .catch(err => sendResponse({ ok: false, error: err.message }));
  return true;
});
