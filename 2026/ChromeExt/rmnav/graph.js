// Redmine Graph Navigator (rmnav) - Graph View Script (MV3)
(function () {
  const LAUNCH_CONTEXT_KEY = "rmnav_launch_context";
  const SERVERS_KEY = "rmnav_servers";
  const HIST_PREFIX = "rmnav_history_";
  const STATE_KEY = "rmnav_last_state";
  const HIST_MAX = 200;

  let baseUrl = "";
  let currentLaunchMode = "empty";
  let pageIssueIds = null;
  let selectedIssueId = null;
  let currentLayout = "free";
  let physicsEnabled = true;
  const TOOLTIP_OFF = 2147483000;
  let tooltipEnabled = false;
  let restoring = false;
  let saveTimer = null;

  const issuesCache = {};
  const filterState = {
    parent: true,
    blocks: true,
    precedes: true,
    relates: true,
    duplicates: true,
    copied_to: true,
    mentions: true
  };

  function escapeHtml(str) {
    return String(str || "")
      .replace(/&/g, "&amp;")
      .replace(/</g, "&lt;")
      .replace(/>/g, "&gt;")
      .replace(/"/g, "&quot;");
  }

  function showFatal(msg) {
    const el = document.getElementById("loading");
    if (el) {
      el.innerHTML =
        '<div style="max-width:520px;padding:18px 22px;background:#fef2f2;border:1px solid #fca5a5;border-radius:8px;color:#991b1b;font-size:13px;line-height:1.6;">' +
        '<div style="font-weight:bold;margin-bottom:6px;">Redmine Graph Navigator 起動エラー</div>' +
        '<div style="white-space:pre-wrap;">' + escapeHtml(msg) + "</div></div>";
      el.style.display = "flex";
    }
  }

  if (typeof vis === "undefined" || !vis.DataSet || !vis.Network) {
    showFatal("Vis.js (vendor/vis-network.min.js) を読み込めませんでした。");
    return;
  }

  // ===================== 履歴管理 (chrome.storage.local) =====================
  function histKeyOf(bUrl) {
    try {
      const u = new URL(bUrl);
      return HIST_PREFIX + u.origin + u.pathname.replace(/\/$/, "");
    } catch (e) {
      return HIST_PREFIX + bUrl;
    }
  }

  function serverLabel(bUrl) {
    try {
      const u = new URL(bUrl);
      const p = u.pathname.replace(/\/$/, "");
      return u.host + (p && p !== "/" ? p : "");
    } catch (e) {
      return bUrl || "";
    }
  }

  async function loadServers() {
    const data = await chrome.storage.local.get(SERVERS_KEY);
    return Array.isArray(data[SERVERS_KEY]) ? data[SERVERS_KEY] : [];
  }

  async function saveServer(bUrl) {
    if (!bUrl) return;
    let servers = await loadServers();
    servers = servers.filter((s) => s.baseUrl !== bUrl);
    servers.unshift({ baseUrl: bUrl, lastUsed: Date.now() });
    if (servers.length > 10) servers = servers.slice(0, 10);
    await chrome.storage.local.set({ [SERVERS_KEY]: servers });
  }

  async function loadHistoryForServer(bUrl) {
    if (!bUrl) return [];
    const key = histKeyOf(bUrl);
    const data = await chrome.storage.local.get(key);
    return Array.isArray(data[key]) ? data[key] : [];
  }

  async function loadAllHistory() {
    let servers = await loadServers();
    if (baseUrl && !servers.some((s) => s.baseUrl === baseUrl)) {
      servers = [{ baseUrl }, ...servers];
    }
    const keys = servers.map((s) => histKeyOf(s.baseUrl));
    if (keys.length === 0) return [];
    const data = await chrome.storage.local.get(keys);
    const combined = [];
    servers.forEach((s) => {
      const list = Array.isArray(data[histKeyOf(s.baseUrl)]) ? data[histKeyOf(s.baseUrl)] : [];
      list.forEach((h) => combined.push({ ...h, baseUrl: s.baseUrl }));
    });
    combined.sort((a, b) => (b.ts || 0) - (a.ts || 0));
    const seen = new Set();
    const deduped = [];
    for (const h of combined) {
      const k = h.baseUrl + ":" + h.id;
      if (!seen.has(k)) {
        seen.add(k);
        deduped.push(h);
      }
    }
    return deduped.slice(0, HIST_MAX);
  }

  async function saveToHistory(issue) {
    if (!baseUrl || !issue || !issue.id) return;
    await saveServer(baseUrl);
    const key = histKeyOf(baseUrl);
    let hist = await loadHistoryForServer(baseUrl);
    hist = hist.filter((h) => String(h.id) !== String(issue.id));
    hist.unshift({
      id: String(issue.id),
      subject: issue.subject || "チケット #" + issue.id,
      tracker: issue.tracker ? issue.tracker.name : "",
      status: issue.status ? issue.status.name : "",
      project: issue.project ? issue.project.name : "",
      baseUrl: baseUrl,
      ts: Date.now()
    });
    if (hist.length > HIST_MAX) hist = hist.slice(0, HIST_MAX);
    await chrome.storage.local.set({ [key]: hist });
    renderHistoryMenu();
  }

  async function savePageToHistory(ctx) {
    if (!baseUrl || !ctx || !ctx.issueIds || ctx.issueIds.length === 0) return;
    await saveServer(baseUrl);
    const key = histKeyOf(baseUrl);
    let hist = await loadHistoryForServer(baseUrl);
    const pageKey = "page:" + (ctx.pageUrl || "");
    hist = hist.filter((h) => String(h.id) !== pageKey);
    hist.unshift({
      kind: "page",
      id: pageKey,
      pageUrl: ctx.pageUrl || "",
      issueIds: ctx.issueIds.slice(),
      subject: (ctx.pageTitle || "一覧/ガント") + " (" + ctx.issueIds.length + "件)",
      project: serverLabel(baseUrl),
      baseUrl: baseUrl,
      ts: Date.now()
    });
    if (hist.length > HIST_MAX) hist = hist.slice(0, HIST_MAX);
    await chrome.storage.local.set({ [key]: hist });
    renderHistoryMenu();
  }

  function formatHistTime(ts) {
    if (!ts) return "";
    const d = new Date(ts);
    const pad = (n) => String(n).padStart(2, "0");
    return (d.getMonth() + 1) + "/" + d.getDate() + " " + pad(d.getHours()) + ":" + pad(d.getMinutes());
  }

  async function renderHistoryMenu() {
    const panel = document.getElementById("history-panel");
    if (!panel) return;

    const hist = await loadAllHistory();
    if (hist.length === 0) {
      panel.innerHTML = '<div style="padding:20px;text-align:center;color:#94a3b8;font-size:12px;">履歴はまだありません</div>';
      return;
    }
    const multiServer = new Set(hist.map((h) => h.baseUrl || "")).size > 1;
    let html = "";
    hist.forEach((h, idx) => {
      const subj = escapeHtml(h.subject || "");
      const isSame = !h.baseUrl || h.baseUrl === baseUrl;
      const serverBadge = multiServer && h.baseUrl
        ? '<span class="hist-server" style="background:' + (isSame ? "#dbeafe" : "#fef3c7") + ";color:" + (isSame ? "#1d4ed8" : "#92400e") + ';">' + escapeHtml(serverLabel(h.baseUrl)) + "</span>"
        : "";
      const isPage = h.kind === "page";
      const idLabel = isPage ? "📋 一覧" : "#" + escapeHtml(h.id);
      html += '<div class="hist-item" data-idx="' + idx + '" title="' + (isPage ? subj : "#" + escapeHtml(h.id) + ": " + subj) + '">' +
        '<div style="display:flex;align-items:center;gap:4px;">' +
        '<span class="hist-id"' + (isPage ? ' style="color:#0f766e;"' : "") + ">" + idLabel + "</span>" +
        serverBadge +
        '<span class="hist-time">' + escapeHtml(formatHistTime(h.ts)) + "</span>" +
        "</div>" +
        '<span class="hist-subj">' + subj + "</span>" +
        (h.project && !isPage ? '<span class="hist-proj">' + escapeHtml(h.project) + "</span>" : "") +
        "</div>";
    });
    panel.innerHTML = html;
    panel.querySelectorAll(".hist-item").forEach((el) => {
      el.onclick = () => {
        const item = hist[Number(el.getAttribute("data-idx"))];
        if (!item) return;
        if (item.kind === "page") {
          loadPageFromHistory(item);
        } else {
          loadGraphFromHistory(item.id, item.baseUrl);
        }
      };
    });
  }

  // ===================== Vis.js 初期化 =====================
  const nodes = new vis.DataSet([]);
  const edges = new vis.DataSet([]);
  const container = document.getElementById("network");
  const network = new vis.Network(container, { nodes, edges }, {
    nodes: {
      shape: "box",
      margin: { top: 8, bottom: 8, left: 12, right: 12 },
      borderWidth: 1.5,
      shadow: { enabled: true, color: "rgba(0,0,0,0.1)", size: 4, x: 1, y: 1 },
      font: {
        face: '-apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif',
        size: 13,
        color: "#1e293b",
        multi: "html"
      }
    },
    edges: {
      width: 1.8,
      smooth: { type: "cubicBezier", forceDirection: "none", roundness: 0.3 },
      font: {
        face: "sans-serif",
        size: 10,
        color: "#475569",
        align: "middle",
        background: "#ffffff"
      },
      arrows: { to: { enabled: true, scaleFactor: 0.7 } }
    },
    physics: {
      enabled: false,
      solver: "forceAtlas2Based",
      forceAtlas2Based: {
        gravitationalConstant: -65,
        centralGravity: 0.015,
        springLength: 140,
        springConstant: 0.06,
        damping: 0.85,
        avoidOverlap: 0.35
      },
      maxVelocity: 25,
      minVelocity: 0.75,
      stabilization: { enabled: true, iterations: 200, updateInterval: 25, fit: true }
    },
    interaction: {
      hover: true,
      tooltipDelay: TOOLTIP_OFF,
      navigationButtons: true,
      keyboard: true
    }
  });

  function updateStatus(msg) {
    if (msg) document.getElementById("status-left").textContent = msg;
    const totalEdges = edges.length;
    const visibleEdges = edges.get().filter((e) => !e.hidden).length;
    document.getElementById("status-right").textContent =
      "ノード: " + nodes.length + " | エッジ: " + visibleEdges + (visibleEdges !== totalEdges ? " (" + totalEdges + "件中)" : "");
  }

  function showLoading(text) {
    document.getElementById("loading-text").textContent = text || "読み込み中...";
    document.getElementById("loading").style.display = "flex";
  }

  function hideLoading() {
    document.getElementById("loading").style.display = "none";
    if (window.RmnavGantt) window.RmnavGantt.render();
  }

  function getTrackerColor(trackerName, isClosed) {
    if (isClosed) {
      return { background: "#f1f5f9", border: "#94a3b8", text: "#64748b" };
    }
    const name = (trackerName || "").toLowerCase();
    if (name.includes("バグ") || name.includes("bug") || name.includes("defect") || name.includes("障害")) {
      return { background: "#fee2e2", border: "#ef4444", text: "#991b1b" };
    }
    if (name.includes("機能") || name.includes("feature")) {
      return { background: "#dbeafe", border: "#3b82f6", text: "#1e40af" };
    }
    if (name.includes("サポート") || name.includes("support")) {
      return { background: "#fef3c7", border: "#f59e0b", text: "#92400e" };
    }
    return { background: "#ecfdf5", border: "#10b981", text: "#065f46" };
  }

  function formatNodeLabel(issue) {
    const idStr = "<b>#" + issue.id + "</b>";
    const tracker = issue.tracker ? issue.tracker.name : "";
    const status = issue.status ? issue.status.name : "";
    let subject = issue.subject || "";
    if (subject.length > 28) subject = subject.substring(0, 26) + "...";
    let label = idStr + " " + (tracker ? "[" + tracker + "]" : "") + "\n" + subject;
    if (status) {
      label += "\n(" + status + (issue.assigned_to ? " / " + issue.assigned_to.name : "") + ")";
    }
    return label;
  }

  function addOrUpdateIssueNode(issue, isInitial) {
    const id = String(issue.id);
    const isClosed = issue.status ? (issue.status.is_closed || /終了|完了|closed|resolved|却下/i.test(issue.status.name)) : false;
    const colors = getTrackerColor(issue.tracker ? issue.tracker.name : "", isClosed);

    const existing = nodes.get(id);
    const keepInitial = isInitial || (existing && existing.isInitial);
    const borderWidth = keepInitial ? 3.5 : 1.5;
    const borderColor = keepInitial ? "#f59e0b" : colors.border;

    const tooltip =
      "<b>#" + issue.id + ": " + escapeHtml(issue.subject || "") + "</b><br>" +
      "トラッカー: " + escapeHtml(issue.tracker ? issue.tracker.name : "-") + "<br>" +
      "ステータス: " + escapeHtml(issue.status ? issue.status.name : "-") + "<br>" +
      "優先度: " + escapeHtml(issue.priority ? issue.priority.name : "-") + "<br>" +
      "担当者: " + escapeHtml(issue.assigned_to ? issue.assigned_to.name : "-") + "<br>" +
      "進捗: " + (issue.done_ratio != null ? issue.done_ratio + "%" : "-");

    const nodeData = {
      id,
      isInitial: keepInitial,
      label: formatNodeLabel(issue),
      title: tooltip,
      color: {
        background: colors.background,
        border: borderColor,
        highlight: { background: "#ffffff", border: "#2563eb" },
        hover: { background: "#ffffff", border: borderColor }
      },
      borderWidth,
      shapeProperties: { borderRadius: 6 }
    };

    if (existing) {
      nodes.update(nodeData);
    } else {
      nodes.add(nodeData);
    }
    issuesCache[id] = issue;
  }

  function addPlaceholderNode(id) {
    id = String(id);
    if (!nodes.get(id)) {
      nodes.add({
        id,
        label: "<b>#" + id + "</b>\n(読み込み中...)",
        color: { background: "#f8fafc", border: "#94a3b8" },
        borderWidth: 1,
        shapeProperties: { borderRadius: 4 }
      });
    }
  }

  function updateEdgeVisibility() {
    const updates = [];
    edges.get().forEach((e) => {
      const isVisible = filterState[e.relationType] !== false;
      if (e.hidden !== !isVisible) {
        updates.push({ id: e.id, hidden: !isVisible });
      }
    });
    if (updates.length > 0) edges.update(updates);
    updateStatus();
  }

  function addRelationEdge(fromId, toId, type, label, color, dashes, hasArrow) {
    const edgeId = fromId + "_" + toId + "_" + type;
    const isVisible = filterState[type] !== false;
    if (!edges.get(edgeId)) {
      edges.add({
        id: edgeId,
        from: String(fromId),
        to: String(toId),
        relationType: type,
        label,
        color: { color, highlight: color, hover: color },
        dashes: Array.isArray(dashes) ? dashes : !!dashes,
        hidden: !isVisible,
        arrows: { to: { enabled: hasArrow !== false, scaleFactor: 0.7 } }
      });
    }
  }

  // ===================== 記載リンク抽出 (mentions) =====================
  function findIssueRefs(text) {
    const ids = [];
    if (!text) return ids;
    text = text
      .replace(/\[([^\]]*)\]\(([^)\s]+)\)/g, (_, t, u) => " " + u + " ")
      .replace(/"([^"\r\n]*)":(\S+)/g, (_, t, u) => " " + u + " ");
    let m;
    const reUrl = /(https?:\/\/[^\s"'<>()\[\]]+?)\/issues\/(\d+)/g;
    while ((m = reUrl.exec(text)) !== null) {
      if (baseUrl && m[1].replace(/\/$/, "").toLowerCase() === baseUrl.replace(/\/$/, "").toLowerCase()) {
        ids.push(m[2]);
      }
    }
    const reHash = /(^|[\s(\[,.、。:：「『（])#(\d{3,7})(?![\w])/g;
    while ((m = reHash.exec(text)) !== null) ids.push(m[2]);
    return ids;
  }

  function collectMentions(issue) {
    const map = {};
    function add(ids, where) {
      ids.forEach((id) => {
        if (id === String(issue.id)) return;
        if (!map[id]) map[id] = [];
        if (map[id].indexOf(where) < 0) map[id].push(where);
      });
    }
    add(findIssueRefs(issue.description), "本文");
    (issue.journals || []).forEach((j, idx) => add(findIssueRefs(j.notes), "note-" + (idx + 1)));
    (issue.mentionRefs || []).forEach((r) => add([r.id], r.where));
    return map;
  }

  // ===================== Redmine API Fetch (via background.js) =====================
  async function fetchIssueData(id, fallbackDomIssue) {
    id = String(id);
    if (!baseUrl) return fallbackDomIssue || null;
    const url = baseUrl + "/issues/" + id + ".json?include=relations,children,journals";
    try {
      const resp = await chrome.runtime.sendMessage({
        type: "FETCH_REDMINE_JSON",
        url,
        baseUrl
      });
      if (resp && resp.ok && resp.data && resp.data.issue) {
        updateStatus("API応答 #" + id + ": HTTP " + resp.status + " (" + resp.elapsed + "ms)");
        return resp.data.issue;
      }
      if (resp) {
        const hint = resp.status === 401 && !resp.hasApiKey ? " (⚙️設定でAPIキー登録可)" : "";
        updateStatus("API応答 #" + id + ": HTTP " + resp.status + hint);
      }
    } catch (err) {
      updateStatus("API取得エラー #" + id + ": " + (err && err.message ? err.message : err));
    }
    return fallbackDomIssue || null;
  }

  function connectIssueRelations(issue, pendingSet) {
    if (issue.parent && issue.parent.id) {
      const pid = String(issue.parent.id);
      addPlaceholderNode(pid);
      addRelationEdge(pid, issue.id, "parent", "子タスク", "#64748b", true, true);
      if (!issuesCache[pid] && pendingSet) pendingSet.add(pid);
    }

    if (issue.children && issue.children.length) {
      issue.children.forEach((child) => {
        const cid = String(child.id);
        if (child.subject && !issuesCache[cid]) {
          issuesCache[cid] = child;
          addOrUpdateIssueNode(child, false);
        } else {
          addPlaceholderNode(cid);
        }
        addRelationEdge(issue.id, cid, "parent", "子タスク", "#64748b", true, true);
        if ((!issuesCache[cid] || !issuesCache[cid].status) && pendingSet) pendingSet.add(cid);
      });
    }

    if (issue.relations && issue.relations.length) {
      issue.relations.forEach((rel) => {
        const from = String(rel.issue_id);
        const to = String(rel.issue_to_id);
        const otherId = from === String(issue.id) ? to : from;
        addPlaceholderNode(otherId);
        if (!issuesCache[otherId] && pendingSet) pendingSet.add(otherId);

        const type = rel.relation_type || "relates";
        if (type === "blocks") {
          addRelationEdge(from, to, "blocks", "ブロック", "#ef4444", false, true);
        } else if (type === "blocked") {
          addRelationEdge(to, from, "blocks", "ブロック", "#ef4444", false, true);
        } else if (type === "precedes") {
          addRelationEdge(from, to, "precedes", "先行", "#0284c7", false, true);
        } else if (type === "follows") {
          addRelationEdge(to, from, "precedes", "先行", "#0284c7", false, true);
        } else if (type === "duplicates") {
          addRelationEdge(from, to, "duplicates", "重複", "#f59e0b", false, true);
        } else if (type === "duplicated") {
          addRelationEdge(to, from, "duplicates", "重複", "#f59e0b", false, true);
        } else if (type === "copied_to") {
          addRelationEdge(from, to, "copied_to", "コピー", "#8b5cf6", true, true);
        } else if (type === "copied_from") {
          addRelationEdge(to, from, "copied_to", "コピー", "#8b5cf6", true, true);
        } else {
          addRelationEdge(from, to, "relates", "関連", "#10b981", false, false);
        }
      });
    }

    const mentions = collectMentions(issue);
    Object.keys(mentions).forEach((mid) => {
      const where = mentions[mid];
      const label = "記載 " + (where.length > 3 ? where.slice(0, 3).join(",") + "…" : where.join(","));
      addPlaceholderNode(mid);
      if (!issuesCache[mid] && pendingSet) pendingSet.add(mid);
      addRelationEdge(issue.id, mid, "mentions", label, "#d946ef", [2, 4], true);
    });
  }

  async function runPool(items, concurrency, worker) {
    let idx = 0;
    const runners = Array.from({ length: Math.min(concurrency, items.length) }, async () => {
      while (idx < items.length) {
        const cur = items[idx++];
        await worker(cur);
      }
    });
    await Promise.all(runners);
  }

  function setPhysicsState(enabled) {
    physicsEnabled = !!enabled;
    const btn = document.getElementById("btn-physics");
    if (btn) btn.classList.toggle("active", physicsEnabled);
    if (currentLayout === "free") {
      network.setOptions({ physics: { enabled: physicsEnabled } });
    }
  }

  function stabilizeAndFreeze(iterations) {
    if (currentLayout !== "free") return;
    setPhysicsState(true);
    network.once("stabilizationIterationsDone", () => {
      setPhysicsState(false);
      network.fit({ animation: { duration: 300, easingFunction: "easeInOutQuad" } });
    });
    network.stabilize(iterations || 200);
  }

  async function loadAndGraphIssue(issueId, isInitial, fallbackDomIssue) {
    network.setOptions({ physics: { enabled: false } });
    let issue = await fetchIssueData(issueId, fallbackDomIssue);
    if (!issue) {
      issue = { id: issueId, subject: "チケット #" + issueId };
    }
    addOrUpdateIssueNode(issue, isInitial);

    const pendingSet = new Set();
    connectIssueRelations(issue, pendingSet);

    if (isInitial && issue && issue.subject) {
      saveToHistory(issue);
    }

    const pendingIds = Array.from(pendingSet).filter((pid) => !issuesCache[pid] || !issuesCache[pid].status);
    if (pendingIds.length > 0) {
      await runPool(pendingIds, 6, async (pid) => {
        const detail = await fetchIssueData(pid);
        if (detail) {
          addOrUpdateIssueNode(detail, false);
          updateStatus();
        }
      });
    }
    stabilizeAndFreeze(180);
    updateStatus("チケット #" + issueId + " を読み込みました");
  }

  // 現在開いている一覧・ガント画面等から複数チケットを一括取得し、関連をグラフ化
  async function loadMultipleIssues(issueIds) {
    network.setOptions({ physics: { enabled: false } });
    const uniqueIds = Array.from(new Set(issueIds.map(String)));
    const pendingSet = new Set();
    let done = 0;

    uniqueIds.forEach((id) => addPlaceholderNode(id));

    await runPool(uniqueIds, 6, async (id) => {
      let issue = await fetchIssueData(id);
      if (!issue) {
        issue = { id, subject: "チケット #" + id };
      }
      addOrUpdateIssueNode(issue, true);
      connectIssueRelations(issue, pendingSet);
      done++;
      showLoading("ページ内のチケットを取得中... (" + done + " / " + uniqueIds.length + ")");
      updateStatus();
    });

    // ページ外の関連チケット（プレースホルダ）も詳細を補完
    const externalIds = Array.from(pendingSet).filter((pid) => !issuesCache[pid] || !issuesCache[pid].status);
    if (externalIds.length > 0) {
      let extDone = 0;
      const limitIds = externalIds.slice(0, 80);
      await runPool(limitIds, 6, async (pid) => {
        const detail = await fetchIssueData(pid);
        if (detail) {
          addOrUpdateIssueNode(detail, false);
        }
        extDone++;
        updateStatus("関連チケット詳細を補完中... (" + extDone + " / " + limitIds.length + ")");
      });
    }

    stabilizeAndFreeze(220);
    updateStatus("ページ内の " + uniqueIds.length + " 件と関連リンクを描画しました");
  }

  // ===================== 詳細サイドバー =====================
  function showSidebarDetails(issueId) {
    selectedIssueId = issueId;
    const issue = issuesCache[issueId] || { id: issueId, subject: "チケット #" + issueId };
    const body = document.getElementById("sidebar-body");

    const isClosed = issue.status ? (issue.status.is_closed || /終了|完了|closed|resolved|却下/i.test(issue.status.name)) : false;
    const trackerBadge = issue.tracker ? '<span class="issue-badge" style="background:#e0f2fe;color:#0369a1;">' + escapeHtml(issue.tracker.name) + "</span>" : "";
    const statusBadge = issue.status ? '<span class="issue-badge" style="background:' + (isClosed ? "#f1f5f9" : "#dcfce7") + ";color:" + (isClosed ? "#64748b" : "#15803d") + ';">' + escapeHtml(issue.status.name) + "</span>" : "";

    let html =
      '<div style="margin-bottom:12px;">' +
      trackerBadge + statusBadge +
      '<div style="font-size:15px;font-weight:bold;margin-top:6px;color:#0f172a;">#' + escapeHtml(issue.id) + ": " + escapeHtml(issue.subject || "") + "</div>" +
      "</div>" +
      '<div class="detail-row"><div class="detail-label">プロジェクト</div><div class="detail-value">' + escapeHtml(issue.project ? issue.project.name : "-") + "</div></div>" +
      '<div class="detail-row"><div class="detail-label">担当者</div><div class="detail-value">' + escapeHtml(issue.assigned_to ? issue.assigned_to.name : "-") + "</div></div>" +
      '<div class="detail-row"><div class="detail-label">優先度</div><div class="detail-value">' + escapeHtml(issue.priority ? issue.priority.name : "-") + "</div></div>" +
      '<div class="detail-row"><div class="detail-label">期日 / 開始日</div><div class="detail-value">' + escapeHtml(issue.due_date || "-") + " (開始: " + escapeHtml(issue.start_date || "-") + ")</div></div>" +
      '<div class="detail-row"><div class="detail-label">進捗率 (' + (issue.done_ratio != null ? issue.done_ratio : 0) + '%)</div>' +
      '<div class="progress-bar-bg"><div class="progress-bar-fill" style="width:' + (issue.done_ratio != null ? issue.done_ratio : 0) + '%;"></div></div>' +
      "</div>";

    if (issue.description) {
      const desc = issue.description.length > 350 ? issue.description.substring(0, 350) + "..." : issue.description;
      html += '<div class="detail-row" style="margin-top:12px;"><div class="detail-label">説明 (抜粋)</div><div class="detail-value" style="background:#f8fafc;padding:8px;border-radius:4px;font-size:12px;white-space:pre-wrap;border:1px solid #e2e8f0;max-height:160px;overflow-y:auto;">' + escapeHtml(desc) + "</div></div>";
    }

    body.innerHTML = html;
    document.getElementById("sidebar").classList.remove("collapsed");
    document.getElementById("sidebar-actions").style.display = "flex";
    if (!restoring && issue && issue.subject && issue.subject !== "チケット #" + issue.id) {
      saveToHistory(issue);
    }
  }

  async function loadPageFromHistory(item) {
    if (item.baseUrl && item.baseUrl !== baseUrl) {
      baseUrl = item.baseUrl;
    }
    document.title = (item.subject || "一覧") + " - Redmine Graph Navigator";
    nodes.clear();
    edges.clear();
    Object.keys(issuesCache).forEach((k) => delete issuesCache[k]);
    if (window.RmnavGantt) window.RmnavGantt.clearEdits();
    const link = document.getElementById("origin-issue-link");
    link.href = item.pageUrl || "#";
    link.textContent = item.subject || "一覧";
    showLoading("履歴ページ (" + (item.issueIds ? item.issueIds.length : 0) + "件) を読み込み中...");
    try {
      await loadMultipleIssues(item.issueIds || []);
      hideLoading();
      network.fit();
    } catch (err) {
      hideLoading();
      updateStatus("エラー: " + err);
    }
  }

  async function loadGraphFromHistory(id, itemBaseUrl) {
    if (itemBaseUrl && itemBaseUrl !== baseUrl) {
      baseUrl = itemBaseUrl;
    }
    document.title = "#" + id + " チケット関係図 - Redmine Graph Navigator";
    nodes.clear();
    edges.clear();
    const link = document.getElementById("origin-issue-link");
    link.href = baseUrl + "/issues/" + id;
    link.textContent = "#" + id;
    updateStatus("チケット #" + id + " を起点としてグラフを描画中...");
    showLoading("チケット #" + id + " を読み込み中...");
    try {
      await loadAndGraphIssue(id, true);
      hideLoading();
      network.fit();
      showSidebarDetails(id);
    } catch (err) {
      hideLoading();
      updateStatus("エラー: " + err);
    }
  }

  // ===================== UI イベント =====================
  network.on("click", (params) => {
    if (params.nodes.length > 0) showSidebarDetails(params.nodes[0]);
  });

  network.on("doubleClick", (params) => {
    if (params.nodes.length > 0) {
      const id = params.nodes[0];
      updateStatus("チケット #" + id + " の関連を展開中...");
      loadAndGraphIssue(id, false);
    }
  });

  document.getElementById("btn-open-redmine").onclick = () => {
    if (selectedIssueId && baseUrl) {
      window.open(baseUrl + "/issues/" + selectedIssueId, "_blank");
    }
  };

  document.getElementById("btn-expand-this").onclick = () => {
    if (selectedIssueId) {
      updateStatus("チケット #" + selectedIssueId + " の関連を展開中...");
      loadAndGraphIssue(selectedIssueId, false);
    }
  };

  document.getElementById("btn-remove-this").onclick = () => {
    if (selectedIssueId) {
      const id = selectedIssueId;
      edges.remove(network.getConnectedEdges(id));
      nodes.remove(id);
      document.getElementById("sidebar").classList.add("collapsed");
      selectedIssueId = null;
      updateStatus("チケット #" + id + " をグラフから除外しました");
    }
  };

  document.getElementById("btn-expand-selected").onclick = () => {
    const sel = network.getSelectedNodes();
    if (sel.length > 0) {
      sel.forEach((id) => loadAndGraphIssue(id, false));
    } else {
      alert("展開したいノードをグラフ上でクリックして選択してください。");
    }
  };

  document.getElementById("btn-add-issue").onclick = () => {
    const input = document.getElementById("add-issue-id");
    const val = input.value.trim().replace(/^#/, "");
    if (val) {
      loadAndGraphIssue(val, false);
      input.value = "";
    }
  };

  document.getElementById("add-issue-id").onkeydown = (e) => {
    if (e.key === "Enter") document.getElementById("btn-add-issue").click();
  };

  document.getElementById("btn-fit").onclick = () => {
    network.fit({ animation: { duration: 500, easingFunction: "easeInOutQuad" } });
  };

  function setLayout(type) {
    currentLayout = type;
    document.getElementById("btn-layout-free").classList.toggle("active", type === "free");
    document.getElementById("btn-layout-hier-ud").classList.toggle("active", type === "hier-ud");
    document.getElementById("btn-layout-hier-lr").classList.toggle("active", type === "hier-lr");

    if (type === "free") {
      network.setOptions({
        layout: { hierarchical: { enabled: false } },
        physics: { enabled: physicsEnabled }
      });
    } else if (type === "hier-ud") {
      network.setOptions({
        layout: { hierarchical: { enabled: true, direction: "UD", sortMethod: "directed", nodeSpacing: 160, levelSeparation: 120 } },
        physics: { enabled: false }
      });
    } else if (type === "hier-lr") {
      network.setOptions({
        layout: { hierarchical: { enabled: true, direction: "LR", sortMethod: "directed", nodeSpacing: 140, levelSeparation: 180 } },
        physics: { enabled: false }
      });
    }
    setTimeout(() => network.fit(), 200);
  }

  document.getElementById("btn-layout-free").onclick = () => setLayout("free");
  document.getElementById("btn-layout-hier-ud").onclick = () => setLayout("hier-ud");
  document.getElementById("btn-layout-hier-lr").onclick = () => setLayout("hier-lr");

  document.getElementById("btn-physics").onclick = function () {
    setPhysicsState(!physicsEnabled);
  };

  document.getElementById("btn-tooltip").onclick = function () {
    tooltipEnabled = !tooltipEnabled;
    this.classList.toggle("active", tooltipEnabled);
    network.setOptions({ interaction: { tooltipDelay: tooltipEnabled ? 300 : TOOLTIP_OFF } });
  };

  document.getElementById("btn-legend").onclick = () => {
    const modal = document.getElementById("legend-modal");
    modal.style.display = modal.style.display === "block" ? "none" : "block";
  };
  document.getElementById("btn-close-legend").onclick = () => {
    document.getElementById("legend-modal").style.display = "none";
  };
  document.getElementById("btn-close-sidebar").onclick = () => {
    document.getElementById("sidebar").classList.add("collapsed");
  };

  const relCheckboxes = document.querySelectorAll(".filter-bar input[type='checkbox']");
  relCheckboxes.forEach((chk) => {
    chk.onchange = function () {
      const relType = this.getAttribute("data-rel");
      if (relType) {
        filterState[relType] = this.checked;
        updateEdgeVisibility();
      }
    };
  });

  document.getElementById("btn-filter-all").onclick = () => {
    relCheckboxes.forEach((chk) => {
      chk.checked = true;
      const relType = chk.getAttribute("data-rel");
      if (relType) filterState[relType] = true;
    });
    updateEdgeVisibility();
  };
  document.getElementById("btn-filter-none").onclick = () => {
    relCheckboxes.forEach((chk) => {
      chk.checked = false;
      const relType = chk.getAttribute("data-rel");
      if (relType) filterState[relType] = false;
    });
    updateEdgeVisibility();
  };

  document.getElementById("btn-history").onclick = () => {
    const hs = document.getElementById("history-sidebar");
    if (hs.classList.contains("collapsed")) {
      renderHistoryMenu();
      hs.classList.remove("collapsed");
    } else {
      hs.classList.add("collapsed");
    }
  };
  document.getElementById("btn-close-history").onclick = () => {
    document.getElementById("history-sidebar").classList.add("collapsed");
  };

  document.getElementById("btn-hist-clear").onclick = async () => {
    const choice = confirm("全サーバーの履歴を削除しますか?\n\n[OK] 全サーバー削除\n[キャンセル] 現在のサーバーのみ削除");
    if (choice) {
      const servers = await loadServers();
      const keys = servers.map((s) => histKeyOf(s.baseUrl)).concat([SERVERS_KEY]);
      await chrome.storage.local.remove(keys);
    } else if (baseUrl) {
      await chrome.storage.local.remove(histKeyOf(baseUrl));
    }
    renderHistoryMenu();
  };

  document.getElementById("btn-options").onclick = () => {
    chrome.runtime.sendMessage({ type: "OPEN_OPTIONS" });
  };
  document.getElementById("btn-help").onclick = () => {
    window.open(chrome.runtime.getURL("help.html"), "redmine_navi_help");
  };

  // ===================== ビュー切替 (グラフ / ガント) =====================
  function setActiveView(mode) {
    const isGantt = mode === "gantt";
    document.getElementById("btn-view-graph").classList.toggle("active", !isGantt);
    document.getElementById("btn-view-gantt").classList.toggle("active", isGantt);
    document.getElementById("network").style.display = isGantt ? "none" : "block";
    document.getElementById("gantt-view").style.display = isGantt ? "flex" : "none";
    if (isGantt && window.RmnavGantt) {
      window.RmnavGantt.render();
      window.RmnavGantt.scrollToToday();
    }
  }
  document.getElementById("btn-view-graph").onclick = () => setActiveView("graph");
  document.getElementById("btn-view-gantt").onclick = () => setActiveView("gantt");

  if (window.RmnavGantt) {
    window.RmnavGantt.init({
      getIssuesCache: () => issuesCache,
      getPageIssueIds: () => pageIssueIds,
      getBaseUrl: () => baseUrl,
      onSelectIssue: (id) => showSidebarDetails(id),
      onStatus: (msg) => updateStatus(msg),
      putIssue: (url, bUrl, body) =>
        new Promise((resolve) => {
          chrome.runtime.sendMessage({ type: "PUT_REDMINE_ISSUE", url, baseUrl: bUrl, body }, (res) => {
            resolve(res || { ok: false, status: 0 });
          });
        }),
      onRefreshIssues: async (ids) => {
        for (const id of ids) {
          const detail = await fetchIssueData(id);
          if (detail) addOrUpdateIssueNode(detail, false);
        }
        if (selectedIssueId && issuesCache[selectedIssueId]) {
          showSidebarDetails(selectedIssueId);
        }
        updateStatus();
      }
    });
  }

  // ===================== 最終状態の保存・復元 =====================
  async function saveState() {
    if (restoring || nodes.length === 0) return;
    try {
      const pos = network.getPositions();
      const ns = nodes.get().map((n) => {
        const p = pos[n.id];
        if (p) { n.x = p.x; n.y = p.y; }
        return n;
      });
      const link = document.getElementById("origin-issue-link");
      await chrome.storage.local.set({
        [STATE_KEY]: {
          v: 2,
          ts: Date.now(),
          baseUrl,
          originHref: link.getAttribute("href"),
          originText: link.textContent,
          nodes: ns,
          edges: edges.get(),
          cache: issuesCache,
          filter: filterState,
          layout: currentLayout,
          physics: physicsEnabled,
          selected: selectedIssueId,
          view: { position: network.getViewPosition(), scale: network.getScale() }
        }
      });
    } catch (e) {}
  }

  function scheduleSave() {
    if (saveTimer) clearTimeout(saveTimer);
    saveTimer = setTimeout(saveState, 500);
  }

  async function restoreState() {
    const data = await chrome.storage.local.get(STATE_KEY);
    const st = data[STATE_KEY];
    if (!st || !st.nodes || !st.nodes.length) return false;
    restoring = true;
    baseUrl = st.baseUrl || baseUrl;
    Object.assign(issuesCache, st.cache || {});
    if (st.filter) {
      Object.keys(st.filter).forEach((k) => { filterState[k] = st.filter[k]; });
      relCheckboxes.forEach((chk) => {
        const t = chk.getAttribute("data-rel");
        if (t && filterState[t] === false) chk.checked = false;
      });
    }
    const link = document.getElementById("origin-issue-link");
    link.href = st.originHref || "#";
    link.textContent = st.originText || "---";
    physicsEnabled = st.physics !== false;
    document.getElementById("btn-physics").classList.toggle("active", physicsEnabled);
    network.setOptions({ physics: { enabled: false } });
    nodes.add(st.nodes);
    edges.add(st.edges || []);
    if (st.layout && st.layout !== "free") {
      setLayout(st.layout);
    } else {
      currentLayout = "free";
      if (st.view && st.view.position) {
        network.moveTo({ position: st.view.position, scale: st.view.scale, animation: false });
      } else {
        network.fit();
      }
    }
    restoring = false;
    if (currentLayout === "free" && physicsEnabled) {
      setTimeout(() => network.setOptions({ physics: { enabled: true } }), 500);
    }
    if (st.selected && nodes.get(st.selected)) showSidebarDetails(st.selected);
    nodes.get().forEach((n) => {
      if (!issuesCache[n.id] || !issuesCache[n.id].status) {
        fetchIssueData(n.id).then((detail) => {
          if (detail) { addOrUpdateIssueNode(detail, false); updateStatus(); }
        });
      }
    });
    updateStatus("前回終了時のグラフを復元しました");
    return true;
  }

  nodes.on("*", scheduleSave);
  edges.on("*", scheduleSave);
  network.on("dragEnd", scheduleSave);
  network.on("zoom", scheduleSave);
  network.on("click", scheduleSave);
  window.addEventListener("pagehide", saveState);
  window.addEventListener("beforeunload", saveState);

  // ===================== 起動・コンテキスト読み込み =====================
  async function initFromLaunchContext() {
    const data = await chrome.storage.local.get(LAUNCH_CONTEXT_KEY);
    const ctx = data[LAUNCH_CONTEXT_KEY];
    await renderHistoryMenu();

    if (!ctx || !ctx.issueIds || ctx.issueIds.length === 0) {
      hideLoading();
      if (!(await restoreState())) {
        document.getElementById("history-sidebar").classList.remove("collapsed");
        updateStatus("履歴からチケットを選択するか、Redmineページを開いて起動してください");
      }
      return;
    }

    baseUrl = ctx.baseUrl || "";
    currentLaunchMode = ctx.mode || "issue";
    pageIssueIds = (currentLaunchMode === "gantt" || currentLaunchMode === "list") && Array.isArray(ctx.issueIds)
      ? ctx.issueIds.map(String)
      : null;
    const chkPageOnly = document.getElementById("chk-gantt-page-only");
    if (chkPageOnly) {
      chkPageOnly.checked = currentLaunchMode === "gantt";
    }
    setActiveView(currentLaunchMode === "gantt" ? "gantt" : "graph");
    nodes.clear();
    edges.clear();
    Object.keys(issuesCache).forEach((k) => delete issuesCache[k]);
    if (window.RmnavGantt) window.RmnavGantt.clearEdits();
    selectedIssueId = null;
    document.getElementById("sidebar").classList.add("collapsed");

    const link = document.getElementById("origin-issue-link");
    if (currentLaunchMode === "issue") {
      const targetId = ctx.issueIds[0];
      document.title = "#" + targetId + " チケット関係図 - Redmine Graph Navigator";
      link.href = baseUrl + "/issues/" + targetId;
      link.textContent = "#" + targetId;
      showLoading("チケット #" + targetId + " と関連データを取得中...");
      try {
        await loadAndGraphIssue(targetId, true, ctx.domIssue);
        hideLoading();
        network.fit();
        showSidebarDetails(targetId);
      } catch (err) {
        hideLoading();
        updateStatus("取得エラー: " + err);
      }
    } else {
      document.title = (ctx.pageTitle || "一覧") + " (" + ctx.issueIds.length + "件) - Redmine Graph Navigator";
      link.href = ctx.pageUrl || "#";
      link.textContent = "現在ページ (" + ctx.issueIds.length + "件)";
      savePageToHistory(ctx);
      showLoading("現在ページの " + ctx.issueIds.length + " 件を取得中...");
      try {
        await loadMultipleIssues(ctx.issueIds);
        hideLoading();
        network.fit();
        if (currentLaunchMode === "gantt" && window.RmnavGantt) {
          window.RmnavGantt.scrollToToday();
        }
      } catch (err) {
        hideLoading();
        updateStatus("取得エラー: " + err);
      }
    }
  }

  chrome.runtime.onMessage.addListener((msg) => {
    if (msg && msg.type === "RELOAD_LAUNCH_CONTEXT") {
      initFromLaunchContext();
    }
  });

  initFromLaunchContext();
})();
