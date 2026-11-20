// Redmine Graph Navigator (rmnav) - Background Service Worker (MV3)

const LAUNCH_CONTEXT_KEY = "rmnav_launch_context";
const API_KEYS_STORAGE = "rmnav_api_keys";
const SETTINGS_STORAGE = "rmnav_settings";

let graphWindowId = null;
let graphTabId = null;

// アクティブタブの Redmine ページからコンテキストとチケットID一覧を抽出する（ページ内注入関数）
function extractRedminePageContext() {
  const origin = window.location.origin;
  const path = window.location.pathname;
  const href = window.location.href;

  // Redmine のベースURL推定（#top-menu a.home や /issues, /projects 等の前から判定）
  let basePath = "";
  const homeLink = document.querySelector("#top-menu a.home");
  if (homeLink) {
    try {
      const hu = new URL(homeLink.href, origin);
      if (hu.origin === origin) {
        basePath = hu.pathname.replace(/\/$/, "");
      }
    } catch (e) {}
  }
  if (!basePath) {
    const m = path.match(/^(.*?)(?:\/projects\/|\/issues(?:\/|$)|\/my\/|\/activity|\/versions\/|\/search)/);
    if (m) basePath = m[1].replace(/\/$/, "");
  }
  const baseUrl = origin + basePath;

  // 1. チケット詳細ページ (/issues/123) か判定
  const issueMatch = path.match(/\/issues\/(\d+)(?:\/|$)/);
  if (issueMatch) {
    const targetId = issueMatch[1];
    // DOMフォールバック用データの抽出
    let domIssue = null;
    try {
      const titleEl = document.querySelector("div.subject h3") || document.querySelector(".issue .subject");
      const trackerEl = document.querySelector("div.subject h2") || document.querySelector("h2.inline-flex");
      const subject = titleEl ? titleEl.textContent.trim() : "";
      const tracker = trackerEl ? trackerEl.textContent.trim() : "";
      const statusEl = document.querySelector(".status.attribute .value") || document.querySelector("td.status");
      const assignedEl = document.querySelector(".assigned-to.attribute .value") || document.querySelector("td.assigned-to");
      const priorityEl = document.querySelector(".priority.attribute .value") || document.querySelector("td.priority");

      const parentLink = document.querySelector("div.subject h3 a[href*='/issues/']") || document.querySelector(".parent a[href*='/issues/']");
      let parentId = null;
      if (parentLink) {
        const pm = (parentLink.getAttribute("href") || "").match(/\/issues\/(\d+)/);
        if (pm) parentId = pm[1];
      }

      const children = [];
      document.querySelectorAll("#issue_tree table.list tr.issue").forEach((row) => {
        const a = row.querySelector("td.subject a");
        if (a) {
          const cm = (a.getAttribute("href") || "").match(/\/issues\/(\d+)/);
          if (cm) children.push({ id: cm[1], subject: a.textContent.trim() });
        }
      });

      const relations = [];
      document.querySelectorAll("#relations table.list tr.issue").forEach((row) => {
        const a = row.querySelector("td.subject a");
        const relEl = row.querySelector("td.relation-type");
        const relText = relEl ? relEl.textContent.trim() : "";
        if (a) {
          const rm = (a.getAttribute("href") || "").match(/\/issues\/(\d+)/);
          if (rm) {
            let relType = "relates";
            if (/ブロック|block/i.test(relText)) relType = "blocks";
            else if (/先行|preced/i.test(relText)) relType = "precedes";
            else if (/後続|follow/i.test(relText)) relType = "follows";
            else if (/重複|duplicat/i.test(relText)) relType = "duplicates";
            relations.push({ issue_id: targetId, issue_to_id: rm[1], relation_type: relType });
          }
        }
      });

      const mentionRefs = [];
      const pushLinks = (root, where) => {
        if (!root) return;
        root.querySelectorAll("a[href*='/issues/']").forEach((a) => {
          const h = a.href || "";
          if (h.indexOf(baseUrl + "/issues/") !== 0) return;
          const lm = h.substring(baseUrl.length).match(/^\/issues\/(\d+)/);
          if (lm) mentionRefs.push({ id: lm[1], where });
        });
      };
      pushLinks(document.querySelector("div.issue div.description div.wiki"), "本文");
      document.querySelectorAll("#history div.journal").forEach((j) => {
        const na = j.querySelector("a[href*='#note-']");
        const nm = na ? (na.getAttribute("href") || "").match(/#note-(\d+)/) : null;
        pushLinks(j.querySelector("div.wiki"), nm ? "note-" + nm[1] : "コメント");
      });

      if (subject || statusEl) {
        domIssue = {
          id: targetId,
          subject: subject || "チケット #" + targetId,
          tracker: tracker ? { name: tracker } : null,
          status: statusEl ? { name: statusEl.textContent.trim() } : null,
          assigned_to: assignedEl ? { name: assignedEl.textContent.trim() } : null,
          priority: priorityEl ? { name: priorityEl.textContent.trim() } : null,
          parent: parentId ? { id: parentId } : null,
          children,
          relations,
          mentionRefs
        };
      }
    } catch (e) {}

    return {
      mode: "issue",
      baseUrl,
      pageUrl: href,
      pageTitle: document.title,
      issueIds: [targetId],
      domIssue
    };
  }

  // 2. ガントチャート・チケット一覧・ロードマップ等 (#content 内のチケットリンクを抽出)
  const seen = new Set();
  const issueIds = [];
  const addId = (id) => {
    if (id && !seen.has(id)) {
      seen.add(id);
      issueIds.push(id);
    }
  };

  const contentRoot = document.querySelector("#content") || document.body;
  if (contentRoot) {
    // 優先セレクタ: 一覧テーブル / ガント科目 / ロードマップ / カレンダー等
    const selectors = [
      "table.issues tr.issue td.id a[href*='/issues/']",
      "table.issues tr.issue td.subject a[href*='/issues/']",
      ".gantt_subjects_container .issue-subject a[href*='/issues/']",
      "div.issue-subject a[href*='/issues/']",
      "table.related-issues tr.issue a[href*='/issues/']",
      "a.issue[href*='/issues/']"
    ];
    for (const sel of selectors) {
      contentRoot.querySelectorAll(sel).forEach((a) => {
        const m = (a.getAttribute("href") || "").match(/\/issues\/(\d+)(?:$|[?#])/);
        if (m) addId(m[1]);
      });
    }
    // 上記で見つからなければ #content 内の全 /issues/123 リンクを走査（メニュー類は #content 外なので安全）
    if (issueIds.length === 0) {
      contentRoot.querySelectorAll("a[href*='/issues/']").forEach((a) => {
        const h = a.getAttribute("href") || "";
        const m = h.match(/\/issues\/(\d+)(?:$|[?#])/);
        if (m) addId(m[1]);
      });
    }
  }

  const isGantt = /\/issues\/gantt(?:\/|$)/.test(path) || !!document.querySelector(".gantt_subjects_container");
  return {
    mode: isGantt && issueIds.length > 0 ? "gantt" : (issueIds.length > 0 ? "list" : "empty"),
    baseUrl,
    pageUrl: href,
    pageTitle: document.title,
    issueIds: issueIds.slice(0, 200),
    domIssue: null
  };
}

async function openOrFocusGraphWindow() {
  const settingsData = await chrome.storage.local.get(SETTINGS_STORAGE);
  const settings = settingsData[SETTINGS_STORAGE] || {};
  const openMode = settings.openMode || "popup";
  const graphUrl = chrome.runtime.getURL("graph.html");

  if (openMode === "tab") {
    if (graphTabId !== null) {
      try {
        const tab = await chrome.tabs.get(graphTabId);
        if (tab) {
          await chrome.tabs.update(graphTabId, { active: true });
          await chrome.windows.update(tab.windowId, { focused: true });
          await chrome.tabs.sendMessage(graphTabId, { type: "RELOAD_LAUNCH_CONTEXT" }).catch(() => {});
          return;
        }
      } catch (e) {
        graphTabId = null;
      }
    }
    const created = await chrome.tabs.create({ url: graphUrl });
    graphTabId = created.id;
  } else {
    if (graphWindowId !== null) {
      try {
        const win = await chrome.windows.get(graphWindowId, { populate: true });
        if (win && win.tabs && win.tabs.length > 0) {
          await chrome.windows.update(graphWindowId, { focused: true });
          await chrome.tabs.sendMessage(win.tabs[0].id, { type: "RELOAD_LAUNCH_CONTEXT" }).catch(() => {});
          return;
        }
      } catch (e) {
        graphWindowId = null;
      }
    }
    const created = await chrome.windows.create({
      url: graphUrl,
      type: "popup",
      width: 1320,
      height: 880
    });
    graphWindowId = created.id;
  }
}

chrome.windows.onRemoved.addListener((winId) => {
  if (winId === graphWindowId) graphWindowId = null;
});
chrome.tabs.onRemoved.addListener((tabId) => {
  if (tabId === graphTabId) graphTabId = null;
});

chrome.action.onClicked.addListener(async (tab) => {
  let ctx = {
    mode: "empty",
    baseUrl: "",
    pageUrl: tab && tab.url ? tab.url : "",
    pageTitle: tab && tab.title ? tab.title : "",
    issueIds: [],
    domIssue: null,
    sourceTabId: tab ? tab.id : null,
    ts: Date.now()
  };

  if (tab && tab.id && tab.url && /^https?:\/\//i.test(tab.url)) {
    try {
      const results = await chrome.scripting.executeScript({
        target: { tabId: tab.id },
        func: extractRedminePageContext
      });
      if (results && results[0] && results[0].result) {
        ctx = {
          ...results[0].result,
          sourceTabId: tab.id,
          ts: Date.now()
        };
      }
    } catch (e) {
      console.warn("executeScript failed:", e);
    }
  }

  await chrome.storage.local.set({ [LAUNCH_CONTEXT_KEY]: ctx });
  await openOrFocusGraphWindow();
});

// 指定 baseUrl に一致する API キーを検索
async function findApiKey(baseUrl) {
  if (!baseUrl) return null;
  const data = await chrome.storage.local.get(API_KEYS_STORAGE);
  const map = data[API_KEYS_STORAGE] || {};
  const norm = baseUrl.replace(/\/$/, "").toLowerCase();
  for (const [k, v] of Object.entries(map)) {
    if (k.replace(/\/$/, "").toLowerCase() === norm && v) {
      return v;
    }
  }
  return null;
}

chrome.runtime.onMessage.addListener((msg, sender, sendResponse) => {
  if (!msg || !msg.type) return false;

  if (msg.type === "FETCH_REDMINE_JSON") {
    (async () => {
      const { url, baseUrl } = msg;
      const apiKey = await findApiKey(baseUrl);
      const ctrl = new AbortController();
      const tm = setTimeout(() => ctrl.abort(), 8000);
      const t0 = Date.now();
      try {
        const headers = { Accept: "application/json" };
        if (apiKey) {
          headers["X-Redmine-API-Key"] = apiKey;
        }
        const res = await fetch(url, {
          headers,
          credentials: apiKey ? "omit" : "include",
          signal: ctrl.signal
        });
        const elapsed = Date.now() - t0;
        if (!res.ok) {
          sendResponse({ ok: false, status: res.status, elapsed, hasApiKey: !!apiKey });
          return;
        }
        const json = await res.json();
        sendResponse({ ok: true, status: res.status, elapsed, data: json, hasApiKey: !!apiKey });
      } catch (err) {
        sendResponse({
          ok: false,
          status: 0,
          elapsed: Date.now() - t0,
          error: err && err.message ? err.message : String(err),
          hasApiKey: !!apiKey
        });
      } finally {
        clearTimeout(tm);
      }
    })();
    return true;
  }

  if (msg.type === "PUT_REDMINE_ISSUE") {
    (async () => {
      const { url, baseUrl, body } = msg;
      const apiKey = await findApiKey(baseUrl);
      const ctrl = new AbortController();
      const tm = setTimeout(() => ctrl.abort(), 10000);
      const t0 = Date.now();
      try {
        const headers = {
          "Content-Type": "application/json",
          Accept: "application/json"
        };
        if (apiKey) {
          headers["X-Redmine-API-Key"] = apiKey;
        }
        const res = await fetch(url, {
          method: "PUT",
          headers,
          body: JSON.stringify(body),
          credentials: apiKey ? "omit" : "include",
          signal: ctrl.signal
        });
        const elapsed = Date.now() - t0;
        if (!res.ok) {
          let errText = "";
          try { errText = await res.text(); } catch (e) {}
          sendResponse({ ok: false, status: res.status, elapsed, error: errText, hasApiKey: !!apiKey });
          return;
        }
        sendResponse({ ok: true, status: res.status, elapsed, hasApiKey: !!apiKey });
      } catch (err) {
        sendResponse({
          ok: false,
          status: 0,
          elapsed: Date.now() - t0,
          error: err && err.message ? err.message : String(err),
          hasApiKey: !!apiKey
        });
      } finally {
        clearTimeout(tm);
      }
    })();
    return true;
  }

  if (msg.type === "OPEN_OPTIONS") {
    chrome.runtime.openOptionsPage();
    sendResponse({ ok: true });
    return false;
  }

  return false;
});
