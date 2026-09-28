/**
 * Redmine Ticket Graph Navigator (redmine-navi)
 *
 * Redmine上でチケットのリンク関係（親子、ブロック、先行後続、関連、重複等）を
 * 別ウィンドウでVis.jsを使ってグラフィカルに可視化・探索するBookmarklet。
 * 最近表示したチケットを200件までlocalStorageに保存し、起動時にメニュー表示。
 */
(function () {
  var origin = window.location.origin;
  var path = window.location.pathname;
  var match = path.match(/^(.*?)\/issues\/(\d+)/);
  var basePath = match ? match[1] : "";
  var initialIssueId = match ? match[2] : null;

  var baseUrl = origin + basePath;

  // ===================== サーバー管理 =====================
  var SERVERS_KEY = "redmine_navi_servers";
  var HIST_MAX = 200;

  function histKey(bUrl) {
    try {
      var u = new URL(bUrl);
      return "redmine_navi_history_" + u.origin + u.pathname.replace(/\/$/, "");
    } catch(e) { return "redmine_navi_history_" + bUrl; }
  }

  function loadServers() {
    try { return JSON.parse(window.localStorage.getItem(SERVERS_KEY) || "[]"); }
    catch(e) { return []; }
  }

  function saveServer(bUrl) {
    try {
      var servers = loadServers();
      servers = servers.filter(function(s) { return s.baseUrl !== bUrl; });
      servers.unshift({ baseUrl: bUrl, lastUsed: Date.now() });
      if (servers.length > 10) servers = servers.slice(0, 10);
      window.localStorage.setItem(SERVERS_KEY, JSON.stringify(servers));
    } catch(e) {}
  }

  function loadHistoryForServer(bUrl) {
    try {
      var raw = window.localStorage.getItem(histKey(bUrl));
      return raw ? JSON.parse(raw) : [];
    } catch(e) { return []; }
  }

  function loadAllHistory() {
    var servers = loadServers();
    // 現在のサーバーが未登録の場合も含める
    var knownUrls = servers.map(function(s) { return s.baseUrl; });
    if (baseUrl && knownUrls.indexOf(baseUrl) === -1) {
      servers = [{ baseUrl: baseUrl }].concat(servers);
    }
    var combined = [];
    servers.forEach(function(s) {
      var hist = loadHistoryForServer(s.baseUrl);
      hist.forEach(function(h) {
        h.baseUrl = s.baseUrl;
        combined.push(h);
      });
    });
    // 新しい順に並べ替え（重複IDは最新を優先）
    var seen = {};
    combined.sort(function(a, b) { return (b.ts || 0) - (a.ts || 0); });
    combined = combined.filter(function(h) {
      var key = h.baseUrl + ":" + h.id;
      if (seen[key]) return false;
      seen[key] = true;
      return true;
    });
    return combined.slice(0, HIST_MAX);
  }

  // チケットページから起動した場合はサーバーを登録
  if (initialIssueId) {
    saveServer(baseUrl);
  }

  var allHistory = loadAllHistory();

  // チケットページから起動した場合は即グラフ表示、そうでなければ履歴メニューウィンドウ
  var winName = "redmine_graph_win";
  var win = window.open("", winName, "width=1280,height=860,menubar=no,toolbar=no,location=no,status=no,resizable=yes,scrollbars=yes");
  if (!win) {
    alert("ポップアップウィンドウの表示がブロックされました。ブラウザのアドレスバー等でポップアップを許可してください。");
    return;
  }

  // ポップアップウィンドウ内で動作するアプリケーション本体
  function popupApp(initialId, baseUrlParam, histData, allHistory) {
    // baseUrl はサーバー切替のため変数として管理
    var baseUrl = baseUrlParam;

    if (initialId) {
      document.getElementById("origin-issue-link").href = baseUrl + "/issues/" + initialId;
    } else {
      document.getElementById("origin-issue-link").href = "#";
      document.getElementById("origin-issue-link").textContent = "---";
    }

    // ===================== 履歴管理（マルチサーバー対応） =====================
    var HIST_MAX = 200;
    var SERVERS_KEY = "redmine_navi_servers";

    function histKeyOf(bUrl) {
      try {
        var u = new URL(bUrl);
        return "redmine_navi_history_" + u.origin + u.pathname.replace(/\/$/, "");
      } catch(e) { return "redmine_navi_history_" + bUrl; }
    }

    function loadServers() {
      try { return JSON.parse(window.localStorage.getItem(SERVERS_KEY) || "[]"); }
      catch(e) { return []; }
    }

    function saveServerToRegistry(bUrl) {
      try {
        var servers = loadServers();
        servers = servers.filter(function(s) { return s.baseUrl !== bUrl; });
        servers.unshift({ baseUrl: bUrl, lastUsed: Date.now() });
        if (servers.length > 10) servers = servers.slice(0, 10);
        window.localStorage.setItem(SERVERS_KEY, JSON.stringify(servers));
      } catch(e) {}
    }

    function loadHistoryForServer(bUrl) {
      try {
        var raw = window.localStorage.getItem(histKeyOf(bUrl));
        return raw ? JSON.parse(raw) : [];
      } catch(e) { return []; }
    }

    function loadCombinedHistory() {
      // popupに注入されたallHistoryを基本とし、localStorageで最新化
      var servers = loadServers();
      var knownUrls = servers.map(function(s) { return s.baseUrl; });
      if (baseUrl && knownUrls.indexOf(baseUrl) === -1) {
        servers = [{ baseUrl: baseUrl }].concat(servers);
      }
      var combined = [];
      servers.forEach(function(s) {
        var hist = loadHistoryForServer(s.baseUrl);
        hist.forEach(function(h) {
          h.baseUrl = s.baseUrl;
          combined.push(h);
        });
      });
      // allHistoryにあってlocalStorageにないエントリも追加（フォールバック）
      if (allHistory && combined.length === 0) {
        combined = allHistory.slice();
      }
      var seen = {};
      combined.sort(function(a, b) { return (b.ts || 0) - (a.ts || 0); });
      combined = combined.filter(function(h) {
        var key = (h.baseUrl || "") + ":" + h.id;
        if (seen[key]) return false;
        seen[key] = true;
        return true;
      });
      return combined.slice(0, HIST_MAX);
    }

    function saveToHistory(issue) {
      if (!issue || !issue.id) return;
      try {
        var key = histKeyOf(baseUrl);
        var hist = loadHistoryForServer(baseUrl);
        hist = hist.filter(function(h) { return String(h.id) !== String(issue.id); });
        hist.unshift({
          id: String(issue.id),
          subject: issue.subject || "#" + issue.id,
          project: (issue.project ? issue.project.name : "") || "",
          url: baseUrl + "/issues/" + issue.id,
          baseUrl: baseUrl,
          ts: Date.now()
        });
        if (hist.length > HIST_MAX) hist = hist.slice(0, HIST_MAX);
        window.localStorage.setItem(key, JSON.stringify(hist));
        saveServerToRegistry(baseUrl);
        renderHistoryMenu();
      } catch(e) {}
    }

    function serverLabel(bUrl) {
      try {
        var u = new URL(bUrl);
        var label = u.hostname + (u.port ? ":" + u.port : "");
        var p = u.pathname.replace(/\/$/, "");
        if (p && p !== "/") label += p;
        return label;
      } catch(e) { return bUrl; }
    }

    function renderHistoryMenu() {
      var panel = document.getElementById("history-panel");
      if (!panel) return;
      var hist = loadCombinedHistory();
      var servers = loadServers();
      var multiServer = servers.length > 1 ||
        (hist.length > 0 && hist.some(function(h) { return h.baseUrl && h.baseUrl !== baseUrl; }));

      if (hist.length === 0) {
        panel.innerHTML = '<div style="color:#94a3b8;text-align:center;padding:20px;font-size:12px;">履歴がありません</div>';
        return;
      }
      var html = "";
      hist.forEach(function(h) {
        var d = h.ts ? new Date(h.ts) : null;
        var ds = d ? (d.getMonth()+1) + "/" + d.getDate() + " " + d.getHours() + ":" + String(d.getMinutes()).padStart(2,"0") : "";
        var subj = h.subject.length > 34 ? h.subject.substring(0, 32) + "…" : h.subject;
        var isSameServer = !h.baseUrl || h.baseUrl === baseUrl;
        var serverBadge = (multiServer && h.baseUrl)
          ? '<span class="hist-server" style="background:' + (isSameServer ? "#dbeafe" : "#fef3c7") + ';color:' + (isSameServer ? "#1d4ed8" : "#92400e") + ';">' + serverLabel(h.baseUrl) + '</span>'
          : '';
        var dataBaseUrl = h.baseUrl ? ' data-base-url="' + h.baseUrl + '"' : '';
        html += '<div class="hist-item" data-id="' + h.id + '"'  + dataBaseUrl + ' title="#' + h.id + ': ' + h.subject.replace(/"/g,"&quot;") + '">' +
          '<div style="display:flex;align-items:center;gap:4px;">' +
          '<span class="hist-id">#' + h.id + '</span>' +
          serverBadge +
          '</div>' +
          '<span class="hist-subj">' + subj + '</span>' +
          (h.project ? '<span class="hist-proj">' + h.project + '</span>' : '') +
          '</div>';
      });
      panel.innerHTML = html;
      panel.querySelectorAll(".hist-item").forEach(function(el) {
        el.onclick = function() {
          var id = this.getAttribute("data-id");
          var itemBaseUrl = this.getAttribute("data-base-url");
          loadGraphFromHistory(id, itemBaseUrl);
        };
      });
    }

    function loadGraphFromHistory(id, itemBaseUrl) {
      // サーバーが変わる場合はbaseUrlを切替
      if (itemBaseUrl && itemBaseUrl !== baseUrl) {
        baseUrl = itemBaseUrl;
        var titleEl = document.querySelector("title");
        if (titleEl) titleEl.textContent = "#" + id + " チケット関係図 - Redmine Graph Navigator";
      }
      // 既存グラフをクリアして新しい起点で再描画
      nodes.clear();
      edges.clear();
      document.getElementById("origin-issue-link").href = baseUrl + "/issues/" + id;
      document.getElementById("origin-issue-link").textContent = "#" + id;
      document.getElementById("history-sidebar").classList.add("collapsed");
      updateStatus("チケット #" + id + " を起点としてグラフを描画中...");
      showLoading("チケット #" + id + " を読み込み中...");
      loadAndGraphIssue(id, true).then(function() {
        hideLoading();
        network.fit();
        showSidebarDetails(id);
      }).catch(function(err) {
        hideLoading();
        updateStatus("エラー: " + err);
      });
    }

    var nodes = new vis.DataSet([]);
    var edges = new vis.DataSet([]);
    var container = document.getElementById("network");
    var data = { nodes: nodes, edges: edges };

    var currentLayout = "free";
    var physicsEnabled = true;

    var baseOptions = {
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
        arrows: {
          to: { enabled: true, scaleFactor: 0.7 }
        }
      },
      physics: {
        enabled: true,
        solver: "forceAtlas2Based",
        forceAtlas2Based: {
          gravitationalConstant: -60,
          centralGravity: 0.015,
          springLength: 130,
          springConstant: 0.08,
          damping: 0.4
        },
        stabilization: { iterations: 120 }
      },
      interaction: {
        hover: true,
        tooltipDelay: 150,
        navigationButtons: true,
        keyboard: true
      }
    };

    var network = new vis.Network(container, data, baseOptions);
    var issuesCache = {};
    var selectedIssueId = null;

    function updateStatus(msg) {
      if (msg) document.getElementById("status-left").textContent = msg;
      var totalEdges = edges.length;
      var visibleEdges = edges.get().filter(function(e) { return !e.hidden; }).length;
      document.getElementById("status-right").textContent = "ノード: " + nodes.length + " | エッジ: " + visibleEdges + (visibleEdges !== totalEdges ? " (" + totalEdges + "件中)" : "");
    }

    function showLoading(text) {
      document.getElementById("loading-text").textContent = text || "読み込み中...";
      document.getElementById("loading").style.display = "flex";
    }

    function hideLoading() {
      document.getElementById("loading").style.display = "none";
    }

    function getTrackerColor(trackerName, isClosed) {
      if (isClosed) {
        return { background: "#f1f5f9", border: "#94a3b8", text: "#64748b" };
      }
      var name = (trackerName || "").toLowerCase();
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
      var idStr = "<b>#" + issue.id + "</b>";
      var tracker = issue.tracker ? issue.tracker.name : "";
      var status = issue.status ? issue.status.name : "";
      var subject = issue.subject || "";
      if (subject.length > 28) {
        subject = subject.substring(0, 26) + "...";
      }
      var label = idStr + " " + (tracker ? "[" + tracker + "]" : "") + "\n" + subject;
      if (status) {
        label += "\n(" + status + (issue.assigned_to ? " / " + issue.assigned_to.name : "") + ")";
      }
      return label;
    }

    function addOrUpdateIssueNode(issue, isInitial) {
      var id = String(issue.id);
      var isClosed = issue.status ? (issue.status.is_closed || /終了|完了|closed|resolved|却下/i.test(issue.status.name)) : false;
      var colors = getTrackerColor(issue.tracker ? issue.tracker.name : "", isClosed);

      var borderWidth = isInitial ? 3.5 : 1.5;
      var borderColor = isInitial ? "#f59e0b" : colors.border;

      var tooltip = "<b>#" + issue.id + ": " + (issue.subject || "") + "</b><br>" +
                    "トラッカー: " + (issue.tracker ? issue.tracker.name : "-") + "<br>" +
                    "ステータス: " + (issue.status ? issue.status.name : "-") + "<br>" +
                    "優先度: " + (issue.priority ? issue.priority.name : "-") + "<br>" +
                    "担当者: " + (issue.assigned_to ? issue.assigned_to.name : "-") + "<br>" +
                    "進捗: " + (issue.done_ratio != null ? issue.done_ratio + "%" : "-");

      var nodeData = {
        id: id,
        label: formatNodeLabel(issue),
        title: tooltip,
        color: {
          background: colors.background,
          border: borderColor,
          highlight: { background: "#ffffff", border: "#2563eb" },
          hover: { background: "#ffffff", border: borderColor }
        },
        borderWidth: borderWidth,
        shapeProperties: { borderRadius: 6 }
      };

      if (nodes.get(id)) {
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
          id: id,
          label: "<b>#" + id + "</b>\n(読み込み中...)",
          color: { background: "#f8fafc", border: "#94a3b8" },
          borderWidth: 1,
          shapeProperties: { borderRadius: 4 }
        });
      }
    }

    var filterState = {
      parent: true,
      blocks: true,
      precedes: true,
      relates: true,
      duplicates: true,
      copied_to: true
    };

    function updateEdgeVisibility() {
      var allEdges = edges.get();
      var updates = [];
      allEdges.forEach(function(e) {
        var isVisible = filterState[e.relationType] !== false;
        if (e.hidden !== !isVisible) {
          updates.push({ id: e.id, hidden: !isVisible });
        }
      });
      if (updates.length > 0) {
        edges.update(updates);
      }
      updateStatus();
    }

    function addRelationEdge(fromId, toId, type, label, color, dashes, hasArrow) {
      var edgeId = fromId + "_" + toId + "_" + type;
      var isVisible = filterState[type] !== false;
      if (!edges.get(edgeId)) {
        edges.add({
          id: edgeId,
          from: String(fromId),
          to: String(toId),
          relationType: type,
          label: label,
          color: { color: color, highlight: color, hover: color },
          dashes: !!dashes,
          hidden: !isVisible,
          arrows: { to: { enabled: hasArrow !== false, scaleFactor: 0.7 } }
        });
      }
    }

    function parseFromOpenerDOM(doc, targetId) {
      try {
        var titleEl = doc.querySelector("div.subject h3") || doc.querySelector(".issue .subject");
        var trackerEl = doc.querySelector("div.subject h2") || doc.querySelector("h2.inline-flex");
        var subject = titleEl ? titleEl.textContent.trim() : "";
        var tracker = trackerEl ? trackerEl.textContent.trim() : "";

        var statusEl = doc.querySelector(".status.attribute .value") || doc.querySelector("td.status");
        var assignedEl = doc.querySelector(".assigned-to.attribute .value") || doc.querySelector("td.assigned-to");
        var priorityEl = doc.querySelector(".priority.attribute .value") || doc.querySelector("td.priority");

        var parentLink = doc.querySelector("div.subject h3 a[href*='/issues/']") || doc.querySelector(".parent a[href*='/issues/']");
        var parentId = null;
        if (parentLink) {
          var pm = parentLink.getAttribute("href").match(/\/issues\/(\d+)/);
          if (pm) parentId = pm[1];
        }

        var children = [];
        doc.querySelectorAll("#issue_tree table.list tr.issue").forEach(function(row) {
          var a = row.querySelector("td.subject a");
          if (a) {
            var cm = a.getAttribute("href").match(/\/issues\/(\d+)/);
            if (cm) children.push({ id: cm[1], subject: a.textContent.trim() });
          }
        });

        var relations = [];
        doc.querySelectorAll("#relations table.list tr.issue").forEach(function(row) {
          var a = row.querySelector("td.subject a");
          var relText = row.querySelector("td.relation-type") ? row.querySelector("td.relation-type").textContent.trim() : "";
          if (a) {
            var rm = a.getAttribute("href").match(/\/issues\/(\d+)/);
            if (rm) {
              var otherId = rm[1];
              var relType = "relates";
              if (/ブロック|block/i.test(relText)) relType = "blocks";
              else if (/先行|preced/i.test(relText)) relType = "precedes";
              else if (/後続|follow/i.test(relText)) relType = "follows";
              else if (/重複|duplicat/i.test(relText)) relType = "duplicates";
              relations.push({ issue_id: targetId, issue_to_id: otherId, relation_type: relType });
            }
          }
        });

        return {
          id: targetId,
          subject: subject || "チケット #" + targetId,
          tracker: tracker ? { name: tracker } : null,
          status: statusEl ? { name: statusEl.textContent.trim() } : null,
          assigned_to: assignedEl ? { name: assignedEl.textContent.trim() } : null,
          priority: priorityEl ? { name: priorityEl.textContent.trim() } : null,
          parent: parentId ? { id: parentId } : null,
          children: children,
          relations: relations
        };
      } catch (e) {
        console.error("DOM parse error:", e);
        return null;
      }
    }

    async function fetchIssueData(id) {
      id = String(id);
      try {
        var res = await fetch(baseUrl + "/issues/" + id + ".json?include=relations,children", {
          credentials: "include"
        });
        if (res.ok) {
          var json = await res.json();
          return json.issue;
        }
      } catch (err) {
        console.warn("API fetch error for #" + id, err);
      }

      if (window.opener && window.opener.document) {
        try {
          var opPath = window.opener.location.pathname;
          var opMatch = opPath.match(/\/issues\/(\d+)/);
          if (opMatch && opMatch[1] === id) {
            var domIssue = parseFromOpenerDOM(window.opener.document, id);
            if (domIssue) return domIssue;
          }
        } catch(e) {}
      }
      return null;
    }

    async function loadAndGraphIssue(issueId, isInitial) {
      var issue = await fetchIssueData(issueId);
      if (!issue) {
        issue = { id: issueId, subject: "チケット #" + issueId };
      }
      addOrUpdateIssueNode(issue, isInitial);

      var pendingIds = [];

      if (issue.parent && issue.parent.id) {
        var pid = String(issue.parent.id);
        addPlaceholderNode(pid);
        addRelationEdge(pid, issue.id, "parent", "子タスク", "#64748b", true, true);
        if (!issuesCache[pid]) pendingIds.push(pid);
      }

      if (issue.children && issue.children.length) {
        issue.children.forEach(function(child) {
          var cid = String(child.id);
          if (child.subject && !issuesCache[cid]) {
            issuesCache[cid] = child;
            addOrUpdateIssueNode(child, false);
          } else {
            addPlaceholderNode(cid);
          }
          addRelationEdge(issue.id, cid, "parent", "子タスク", "#64748b", true, true);
          if (!issuesCache[cid] || !issuesCache[cid].status) pendingIds.push(cid);
        });
      }

      if (issue.relations && issue.relations.length) {
        issue.relations.forEach(function(rel) {
          var from = String(rel.issue_id);
          var to = String(rel.issue_to_id);
          var otherId = (from === String(issue.id)) ? to : from;
          addPlaceholderNode(otherId);
          if (!issuesCache[otherId]) pendingIds.push(otherId);

          var type = rel.relation_type || "relates";
          if (type === "blocks") {
            addRelationEdge(from, to, type, "ブロック", "#ef4444", false, true);
          } else if (type === "blocked") {
            addRelationEdge(to, from, "blocks", "ブロック", "#ef4444", false, true);
          } else if (type === "precedes") {
            addRelationEdge(from, to, type, "先行", "#0284c7", false, true);
          } else if (type === "follows") {
            addRelationEdge(to, from, "precedes", "先行", "#0284c7", false, true);
          } else if (type === "duplicates") {
            addRelationEdge(from, to, type, "重複", "#f59e0b", false, true);
          } else if (type === "duplicated") {
            addRelationEdge(to, from, "duplicates", "重複", "#f59e0b", false, true);
          } else if (type === "copied_to") {
            addRelationEdge(from, to, type, "コピー", "#8b5cf6", true, true);
          } else if (type === "copied_from") {
            addRelationEdge(to, from, "copied_to", "コピー", "#8b5cf6", true, true);
          } else {
            addRelationEdge(from, to, "relates", "関連", "#10b981", false, false);
          }
        });
      }

      updateStatus("チケット #" + issueId + " を読み込みました");

      // 起点チケットを履歴に保存
      if (isInitial && issue && issue.subject) {
        saveToHistory(issue);
      }

      for (var i = 0; i < pendingIds.length; i++) {
        var pid = pendingIds[i];
        if (!issuesCache[pid] || !issuesCache[pid].status) {
          fetchIssueData(pid).then(function(detail) {
            if (detail) {
              addOrUpdateIssueNode(detail, false);
              updateStatus();
            }
          });
        }
      }
    }

    function showSidebarDetails(issueId) {
      selectedIssueId = issueId;
      var issue = issuesCache[issueId] || { id: issueId, subject: "チケット #" + issueId };
      var body = document.getElementById("sidebar-body");

      var isClosed = issue.status ? (issue.status.is_closed || /終了|完了|closed|resolved|却下/i.test(issue.status.name)) : false;
      var trackerBadge = issue.tracker ? '<span class="issue-badge" style="background:#e0f2fe;color:#0369a1;">' + issue.tracker.name + '</span>' : "";
      var statusBadge = issue.status ? '<span class="issue-badge" style="background:' + (isClosed ? "#f1f5f9" : "#dcfce7") + ';color:' + (isClosed ? "#64748b" : "#15803d") + ';">' + issue.status.name + '</span>' : "";

      var html = '<div style="margin-bottom:12px;">' +
        trackerBadge + statusBadge +
        '<div style="font-size:15px;font-weight:bold;margin-top:6px;color:#0f172a;">#' + issue.id + ': ' + (issue.subject || "") + '</div>' +
      '</div>' +
      '<div class="detail-row"><div class="detail-label">プロジェクト</div><div class="detail-value">' + (issue.project ? issue.project.name : "-") + '</div></div>' +
      '<div class="detail-row"><div class="detail-label">担当者</div><div class="detail-value">' + (issue.assigned_to ? issue.assigned_to.name : "-") + '</div></div>' +
      '<div class="detail-row"><div class="detail-label">優先度</div><div class="detail-value">' + (issue.priority ? issue.priority.name : "-") + '</div></div>' +
      '<div class="detail-row"><div class="detail-label">期日 / 開始日</div><div class="detail-value">' + (issue.due_date || "-") + ' (開始: ' + (issue.start_date || "-") + ')</div></div>' +
      '<div class="detail-row"><div class="detail-label">進捗率 (' + (issue.done_ratio != null ? issue.done_ratio : 0) + '%)</div>' +
        '<div class="progress-bar-bg"><div class="progress-bar-fill" style="width:' + (issue.done_ratio != null ? issue.done_ratio : 0) + '%;"></div></div>' +
      '</div>';

      if (issue.description) {
        var desc = issue.description.length > 350 ? issue.description.substring(0, 350) + "..." : issue.description;
        var safeDesc = desc.replace(/&/g, "&amp;").replace(/</g, "&lt;").replace(/>/g, "&gt;");
        html += '<div class="detail-row" style="margin-top:12px;"><div class="detail-label">説明 (抜粋)</div><div class="detail-value" style="background:#f8fafc;padding:8px;border-radius:4px;font-size:12px;white-space:pre-wrap;border:1px solid #e2e8f0;max-height:160px;overflow-y:auto;">' + safeDesc + '</div></div>';
      }

      body.innerHTML = html;
      document.getElementById("sidebar").classList.remove("collapsed");
      document.getElementById("sidebar-actions").style.display = "flex";
    }

    network.on("click", function(params) {
      if (params.nodes.length > 0) {
        showSidebarDetails(params.nodes[0]);
      }
    });

    network.on("doubleClick", function(params) {
      if (params.nodes.length > 0) {
        var id = params.nodes[0];
        updateStatus("チケット #" + id + " の関連を展開中...");
        loadAndGraphIssue(id, false);
      }
    });

    document.getElementById("btn-open-redmine").onclick = function() {
      if (selectedIssueId) {
        window.open(baseUrl + "/issues/" + selectedIssueId, "_blank");
      }
    };

    document.getElementById("btn-expand-this").onclick = function() {
      if (selectedIssueId) {
        updateStatus("チケット #" + selectedIssueId + " の関連を展開中...");
        loadAndGraphIssue(selectedIssueId, false);
      }
    };

    document.getElementById("btn-remove-this").onclick = function() {
      if (selectedIssueId) {
        var id = selectedIssueId;
        var connectedEdges = network.getConnectedEdges(id);
        edges.remove(connectedEdges);
        nodes.remove(id);
        document.getElementById("sidebar").classList.add("collapsed");
        selectedIssueId = null;
        updateStatus("チケット #" + id + " をグラフから除外しました");
      }
    };

    document.getElementById("btn-expand-selected").onclick = function() {
      var sel = network.getSelectedNodes();
      if (sel.length > 0) {
        sel.forEach(function(id) {
          loadAndGraphIssue(id, false);
        });
      } else {
        alert("展開したいノードをグラフ上でクリックして選択してください。");
      }
    };

    document.getElementById("btn-add-issue").onclick = function() {
      var input = document.getElementById("add-issue-id");
      var val = input.value.trim().replace(/^#/, "");
      if (val) {
        loadAndGraphIssue(val, false);
        input.value = "";
      }
    };

    document.getElementById("add-issue-id").onkeydown = function(e) {
      if (e.key === "Enter") {
        document.getElementById("btn-add-issue").click();
      }
    };

    document.getElementById("btn-fit").onclick = function() {
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
          layout: {
            hierarchical: {
              enabled: true,
              direction: "UD",
              sortMethod: "directed",
              nodeSpacing: 160,
              levelSeparation: 120
            }
          },
          physics: { enabled: false }
        });
      } else if (type === "hier-lr") {
        network.setOptions({
          layout: {
            hierarchical: {
              enabled: true,
              direction: "LR",
              sortMethod: "directed",
              nodeSpacing: 140,
              levelSeparation: 180
            }
          },
          physics: { enabled: false }
        });
      }
      setTimeout(function() { network.fit(); }, 200);
    }

    document.getElementById("btn-layout-free").onclick = function() { setLayout("free"); };
    document.getElementById("btn-layout-hier-ud").onclick = function() { setLayout("hier-ud"); };
    document.getElementById("btn-layout-hier-lr").onclick = function() { setLayout("hier-lr"); };

    document.getElementById("btn-physics").onclick = function() {
      physicsEnabled = !physicsEnabled;
      this.classList.toggle("active", physicsEnabled);
      if (currentLayout === "free") {
        network.setOptions({ physics: { enabled: physicsEnabled } });
      }
    };

    document.getElementById("btn-legend").onclick = function() {
      var modal = document.getElementById("legend-modal");
      modal.style.display = modal.style.display === "block" ? "none" : "block";
    };

    document.getElementById("btn-close-legend").onclick = function() {
      document.getElementById("legend-modal").style.display = "none";
    };

    document.getElementById("btn-close-sidebar").onclick = function() {
      document.getElementById("sidebar").classList.add("collapsed");
    };

    // リレーション・フィルターのイベント設定
    var relCheckboxes = document.querySelectorAll(".filter-bar input[type='checkbox']");
    relCheckboxes.forEach(function(chk) {
      chk.onchange = function() {
        var relType = this.getAttribute("data-rel");
        if (relType) {
          filterState[relType] = this.checked;
          updateEdgeVisibility();
        }
      };
    });

    document.getElementById("btn-filter-all").onclick = function() {
      relCheckboxes.forEach(function(chk) {
        chk.checked = true;
        var relType = chk.getAttribute("data-rel");
        if (relType) filterState[relType] = true;
      });
      updateEdgeVisibility();
    };

    document.getElementById("btn-filter-none").onclick = function() {
      relCheckboxes.forEach(function(chk) {
        chk.checked = false;
        var relType = chk.getAttribute("data-rel");
        if (relType) filterState[relType] = false;
      });
      updateEdgeVisibility();
    };

    document.getElementById("btn-history").onclick = function() {
      var hs = document.getElementById("history-sidebar");
      if (hs.classList.contains("collapsed")) {
        renderHistoryMenu();
        hs.classList.remove("collapsed");
      } else {
        hs.classList.add("collapsed");
      }
    };

    document.getElementById("btn-close-history").onclick = function() {
      document.getElementById("history-sidebar").classList.add("collapsed");
    };

    document.getElementById("btn-hist-clear").onclick = function() {
      var choice = confirm("全サーバーの履歴を削除しますか?\n\n[OK] 全サーバー削除\n[キャンセル] 現在のサーバーのみ削除");
      try {
        if (choice) {
          // 全サーバーの履歴を削除
          var servers = loadServers();
          servers.forEach(function(s) {
            window.localStorage.removeItem(histKeyOf(s.baseUrl));
          });
          window.localStorage.removeItem(SERVERS_KEY);
        } else {
          // 現在のサーバーのみ削除
          window.localStorage.removeItem(histKeyOf(baseUrl));
        }
      } catch(e) {}
      renderHistoryMenu();
    };

    // 初期化実行
    renderHistoryMenu();
    if (initialId) {
      showLoading("チケット #" + initialId + " と関連データを取得中...");
      loadAndGraphIssue(initialId, true).then(function() {
        hideLoading();
        network.fit();
        showSidebarDetails(initialId);
      }).catch(function(err) {
        hideLoading();
        alert("チケットデータの取得中にエラーが発生しました: " + err);
      });
    } else {
      // Redmineチケットページ以外から起動: 履歴パネルを開いてチケット選択を促す
      hideLoading();
      document.getElementById("history-sidebar").classList.remove("collapsed");
      updateStatus("履歴からチケットを選択してグラフを表示してください");
    }
  }

  // ポップアップウィンドウのHTMLを組み立て
  var html = '<!DOCTYPE html>\n' +
'<html lang="ja">\n' +
'<head>\n' +
'  <meta charset="UTF-8">\n' +
'  <title>' + (initialIssueId ? '#' + initialIssueId + ' チケット関係図' : 'Redmine Graph Navigator') + ' - Redmine Graph Navigator</title>\n' +
'  <style>\n' +
'    * { box-sizing: border-box; margin: 0; padding: 0; }\n' +
'    body {\n' +
'      font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "Helvetica Neue", Arial, sans-serif;\n' +
'      font-size: 13px;\n' +
'      color: #333;\n' +
'      background: #f8fafc;\n' +
'      height: 100vh;\n' +
'      display: flex;\n' +
'      flex-direction: column;\n' +
'      overflow: hidden;\n' +
'    }\n' +
'    header {\n' +
'      background: #1e293b;\n' +
'      color: #fff;\n' +
'      padding: 10px 16px;\n' +
'      display: flex;\n' +
'      align-items: center;\n' +
'      justify-content: space-between;\n' +
'      box-shadow: 0 2px 4px rgba(0,0,0,0.1);\n' +
'      z-index: 10;\n' +
'      flex-wrap: wrap;\n' +
'      gap: 8px;\n' +
'    }\n' +
'    .header-title {\n' +
'      font-size: 15px;\n' +
'      font-weight: bold;\n' +
'      display: flex;\n' +
'      align-items: center;\n' +
'      gap: 8px;\n' +
'    }\n' +
'    .header-title a {\n' +
'      color: #93c5fd;\n' +
'      text-decoration: none;\n' +
'    }\n' +
'    .header-title a:hover {\n' +
'      text-decoration: underline;\n' +
'    }\n' +
'    .toolbar {\n' +
'      display: flex;\n' +
'      align-items: center;\n' +
'      gap: 6px;\n' +
'      flex-wrap: wrap;\n' +
'    }\n' +
'    .btn {\n' +
'      padding: 5px 10px;\n' +
'      font-size: 12px;\n' +
'      border: 1px solid #475569;\n' +
'      background: #334155;\n' +
'      color: #f1f5f9;\n' +
'      border-radius: 4px;\n' +
'      cursor: pointer;\n' +
'      display: inline-flex;\n' +
'      align-items: center;\n' +
'      gap: 4px;\n' +
'      user-select: none;\n' +
'      transition: background 0.15s, border-color 0.15s;\n' +
'    }\n' +
'    .btn:hover {\n' +
'      background: #475569;\n' +
'    }\n' +
'    .btn.active {\n' +
'      background: #2563eb;\n' +
'      border-color: #3b82f6;\n' +
'      color: #ffffff;\n' +
'      font-weight: 500;\n' +
'    }\n' +
'    .input-box {\n' +
'      padding: 5px 8px;\n' +
'      font-size: 12px;\n' +
'      border: 1px solid #475569;\n' +
'      border-radius: 4px;\n' +
'      background: #0f172a;\n' +
'      color: #f8fafc;\n' +
'      width: 90px;\n' +
'    }\n' +
'    .input-box:focus {\n' +
'      outline: none;\n' +
'      border-color: #60a5fa;\n' +
'    }\n' +
'    .filter-bar {\n' +
'      background: #0f172a;\n' +
'      color: #cbd5e1;\n' +
'      padding: 6px 16px;\n' +
'      display: flex;\n' +
'      align-items: center;\n' +
'      gap: 14px;\n' +
'      border-bottom: 1px solid #334155;\n' +
'      font-size: 12px;\n' +
'      flex-wrap: wrap;\n' +
'      z-index: 9;\n' +
'    }\n' +
'    .filter-title {\n' +
'      font-weight: 600;\n' +
'      color: #94a3b8;\n' +
'      display: flex;\n' +
'      align-items: center;\n' +
'      gap: 4px;\n' +
'    }\n' +
'    .filter-label {\n' +
'      display: inline-flex;\n' +
'      align-items: center;\n' +
'      gap: 5px;\n' +
'      cursor: pointer;\n' +
'      user-select: none;\n' +
'      padding: 2px 6px;\n' +
'      border-radius: 4px;\n' +
'      transition: background 0.15s;\n' +
'    }\n' +
'    .filter-label:hover {\n' +
'      background: #1e293b;\n' +
'    }\n' +
'    .filter-label input[type="checkbox"] {\n' +
'      cursor: pointer;\n' +
'      accent-color: #3b82f6;\n' +
'    }\n' +
'    .filter-badge {\n' +
'      display: inline-block;\n' +
'      width: 10px;\n' +
'      height: 10px;\n' +
'      border-radius: 2px;\n' +
'    }\n' +
'    .filter-badge.dashed {\n' +
'      width: 12px;\n' +
'      height: 0;\n' +
'      border-bottom: 2px dashed #94a3b8;\n' +
'    }\n' +
'    #main-container {\n' +
'      flex: 1;\n' +
'      display: flex;\n' +
'      position: relative;\n' +
'      overflow: hidden;\n' +
'    }\n' +
'    #network-container {\n' +
'      flex: 1;\n' +
'      height: 100%;\n' +
'      position: relative;\n' +
'      background: #ffffff;\n' +
'    }\n' +
'    #network {\n' +
'      width: 100%;\n' +
'      height: 100%;\n' +
'    }\n' +
'    #sidebar {\n' +
'      width: 320px;\n' +
'      height: 100%;\n' +
'      background: #ffffff;\n' +
'      border-left: 1px solid #e2e8f0;\n' +
'      display: flex;\n' +
'      flex-direction: column;\n' +
'      box-shadow: -2px 0 6px rgba(0,0,0,0.03);\n' +
'      transition: width 0.2s;\n' +
'      z-index: 5;\n' +
'    }\n' +
'    #sidebar.collapsed {\n' +
'      width: 0;\n' +
'      display: none;\n' +
'    }\n' +
'    .sidebar-header {\n' +
'      padding: 12px 16px;\n' +
'      background: #f1f5f9;\n' +
'      border-bottom: 1px solid #e2e8f0;\n' +
'      display: flex;\n' +
'      align-items: center;\n' +
'      justify-content: space-between;\n' +
'      font-weight: bold;\n' +
'    }\n' +
'    .sidebar-content {\n' +
'      padding: 16px;\n' +
'      flex: 1;\n' +
'      overflow-y: auto;\n' +
'    }\n' +
'    .issue-badge {\n' +
'      display: inline-block;\n' +
'      padding: 2px 6px;\n' +
'      border-radius: 3px;\n' +
'      font-size: 11px;\n' +
'      font-weight: bold;\n' +
'      margin-right: 4px;\n' +
'    }\n' +
'    .detail-row {\n' +
'      margin-bottom: 10px;\n' +
'    }\n' +
'    .detail-label {\n' +
'      font-size: 11px;\n' +
'      color: #64748b;\n' +
'      margin-bottom: 2px;\n' +
'    }\n' +
'    .detail-value {\n' +
'      font-size: 13px;\n' +
'      word-break: break-all;\n' +
'    }\n' +
'    .progress-bar-bg {\n' +
'      background: #e2e8f0;\n' +
'      border-radius: 4px;\n' +
'      height: 8px;\n' +
'      overflow: hidden;\n' +
'      margin-top: 4px;\n' +
'    }\n' +
'    .progress-bar-fill {\n' +
'      background: #10b981;\n' +
'      height: 100%;\n' +
'    }\n' +
'    .sidebar-actions {\n' +
'      padding: 12px 16px;\n' +
'      border-top: 1px solid #e2e8f0;\n' +
'      display: flex;\n' +
'      flex-direction: column;\n' +
'      gap: 6px;\n' +
'      background: #f8fafc;\n' +
'    }\n' +
'    .sidebar-btn-row {\n' +
'      display: flex;\n' +
'      gap: 6px;\n' +
'    }\n' +
'    .sidebar-btn-row .btn {\n' +
'      flex: 1;\n' +
'      justify-content: center;\n' +
'    }\n' +
'    #statusbar {\n' +
'      background: #f1f5f9;\n' +
'      border-top: 1px solid #e2e8f0;\n' +
'      padding: 4px 16px;\n' +
'      font-size: 11px;\n' +
'      color: #64748b;\n' +
'      display: flex;\n' +
'      align-items: center;\n' +
'      justify-content: space-between;\n' +
'      height: 24px;\n' +
'    }\n' +
'    #legend-modal {\n' +
'      display: none;\n' +
'      position: absolute;\n' +
'      top: 10px;\n' +
'      right: 10px;\n' +
'      background: #ffffff;\n' +
'      border: 1px solid #cbd5e1;\n' +
'      border-radius: 6px;\n' +
'      padding: 12px 16px;\n' +
'      box-shadow: 0 4px 12px rgba(0,0,0,0.15);\n' +
'      z-index: 20;\n' +
'      font-size: 12px;\n' +
'      width: 250px;\n' +
'    }\n' +
'    .legend-item {\n' +
'      display: flex;\n' +
'      align-items: center;\n' +
'      gap: 8px;\n' +
'      margin-bottom: 6px;\n' +
'    }\n' +
'    .legend-line {\n' +
'      width: 28px;\n' +
'      height: 3px;\n' +
'    }\n' +
'    .loading-overlay {\n' +
'      position: absolute;\n' +
'      top: 0; left: 0; right: 0; bottom: 0;\n' +
'      background: rgba(255,255,255,0.75);\n' +
'      display: flex;\n' +
'      align-items: center;\n' +
'      justify-content: center;\n' +
'      font-size: 14px;\n' +
'      color: #1e293b;\n' +
'      z-index: 15;\n' +
'    }\n' +
'    .spinner {\n' +
'      border: 3px solid #e2e8f0;\n' +
'      border-top: 3px solid #2563eb;\n' +
'      border-radius: 50%;\n' +
'      width: 22px;\n' +
'      height: 22px;\n' +
'      animation: spin 0.8s linear infinite;\n' +
'      margin-right: 10px;\n' +
'    }\n' +
'    @keyframes spin {\n' +
'      0% { transform: rotate(0deg); }\n' +
'      100% { transform: rotate(360deg); }\n' +
'    }\n' +
'    #history-sidebar {\n' +
'      width: 280px;\n' +
'      height: 100%;\n' +
'      background: #ffffff;\n' +
'      border-right: 1px solid #e2e8f0;\n' +
'      display: flex;\n' +
'      flex-direction: column;\n' +
'      box-shadow: 2px 0 6px rgba(0,0,0,0.05);\n' +
'      z-index: 5;\n' +
'      flex-shrink: 0;\n' +
'    }\n' +
'    #history-sidebar.collapsed {\n' +
'      display: none;\n' +
'    }\n' +
'    .history-header {\n' +
'      padding: 10px 12px;\n' +
'      background: #1e293b;\n' +
'      border-bottom: 1px solid #334155;\n' +
'      display: flex;\n' +
'      align-items: center;\n' +
'      justify-content: space-between;\n' +
'      font-weight: bold;\n' +
'      color: #f1f5f9;\n' +
'      font-size: 13px;\n' +
'      flex-shrink: 0;\n' +
'    }\n' +
'    #history-panel {\n' +
'      flex: 1;\n' +
'      overflow-y: auto;\n' +
'      padding: 4px 0;\n' +
'    }\n' +
'    .hist-item {\n' +
'      display: flex;\n' +
'      flex-direction: column;\n' +
'      gap: 2px;\n' +
'      padding: 7px 12px;\n' +
'      cursor: pointer;\n' +
'      border-bottom: 1px solid #f1f5f9;\n' +
'      transition: background 0.12s;\n' +
'    }\n' +
'    .hist-item:hover { background: #f0f9ff; }\n' +
'    .hist-id {\n' +
'      font-size: 11px;\n' +
'      font-weight: bold;\n' +
'      color: #2563eb;\n' +
'      font-family: monospace;\n' +
'    }\n' +
'    .hist-subj {\n' +
'      font-size: 12px;\n' +
'      color: #0f172a;\n' +
'      overflow: hidden;\n' +
'      text-overflow: ellipsis;\n' +
'      white-space: nowrap;\n' +
'    }\n' +
'    .hist-proj {\n' +
'      font-size: 10px;\n' +
'      color: #64748b;\n' +
'    }\n' +
'    .hist-server {\n' +
'      font-size: 9px;\n' +
'      padding: 1px 5px;\n' +
'      border-radius: 3px;\n' +
'      font-weight: 600;\n' +
'      white-space: nowrap;\n' +
'      overflow: hidden;\n' +
'      text-overflow: ellipsis;\n' +
'      max-width: 160px;\n' +
'      display: inline-block;\n' +
'    }\n' +
'    .history-footer {\n' +
'      padding: 6px 12px;\n' +
'      border-top: 1px solid #e2e8f0;\n' +
'      background: #f8fafc;\n' +
'    }\n' +
'  </style>\n' +
'  <script src="https://cdnjs.cloudflare.com/ajax/libs/vis-network/9.1.9/standalone/umd/vis-network.min.js"></script>\n' +
'  <script>\n' +
'    if (typeof vis === "undefined") {\n' +
'      document.write(\'<script src="https://unpkg.com/vis-network/standalone/umd/vis-network.min.js"><\\/script>\');\n' +
'    }\n' +
'  </script>\n' +
'</head>\n' +
'<body>\n' +
'  <header>\n' +
'    <div class="header-title">\n' +
'      <span>🌐 Redmine Ticket Graph</span>\n' +
'      <span style="color:#64748b;">|</span>\n' +
'      <span>起点: <a id="origin-issue-link" href="#" target="_blank">#' + initialIssueId + '</a></span>\n' +
'    </div>\n' +
'    <div class="toolbar">\n' +
'      <input type="text" id="add-issue-id" class="input-box" placeholder="チケット#">\n' +
'      <button id="btn-add-issue" class="btn" title="チケットをグラフに追加">➕ 追加</button>\n' +
'      <button id="btn-expand-selected" class="btn" title="選択したチケットの関連を展開">🔄 関連展開</button>\n' +
'      <span style="color:#475569;">|</span>\n' +
'      <button id="btn-fit" class="btn" title="グラフ全体を表示">🎯 フィット</button>\n' +
'      <button id="btn-layout-free" class="btn active" title="自由配置 (力学モデル)">自由配置</button>\n' +
'      <button id="btn-layout-hier-ud" class="btn" title="階層ツリー (上から下)">ツリー(上下)</button>\n' +
'      <button id="btn-layout-hier-lr" class="btn" title="階層ツリー (左から右)">ツリー(左右)</button>\n' +
'      <button id="btn-physics" class="btn active" title="物理シミュレーションON/OFF">⚡ 物理演算</button>\n' +
'      <span style="color:#475569;">|</span>\n' +
'      <button id="btn-legend" class="btn" title="凡例を表示">ℹ️ 凡例</button>\n' +
'      <span style="color:#475569;">|</span>\n' +
'      <button id="btn-history" class="btn" title="最近表示したチケット履歴">🕐 履歴</button>\n' +
'    </div>\n' +
'  </header>\n' +
'\n' +
'  <div class="filter-bar">\n' +
'    <div class="filter-title">🔍 表示関連:</div>\n' +
'    <label class="filter-label"><input type="checkbox" id="chk-rel-parent" data-rel="parent" checked><span class="filter-badge dashed"></span>親子タスク</label>\n' +
'    <label class="filter-label"><input type="checkbox" id="chk-rel-blocks" data-rel="blocks" checked><span class="filter-badge" style="background:#ef4444;"></span>ブロック</label>\n' +
'    <label class="filter-label"><input type="checkbox" id="chk-rel-precedes" data-rel="precedes" checked><span class="filter-badge" style="background:#0284c7;"></span>先行・後続</label>\n' +
'    <label class="filter-label"><input type="checkbox" id="chk-rel-relates" data-rel="relates" checked><span class="filter-badge" style="background:#10b981;"></span>関連</label>\n' +
'    <label class="filter-label"><input type="checkbox" id="chk-rel-duplicates" data-rel="duplicates" checked><span class="filter-badge" style="background:#f59e0b;"></span>重複</label>\n' +
'    <label class="filter-label"><input type="checkbox" id="chk-rel-copied" data-rel="copied_to" checked><span class="filter-badge dashed" style="border-bottom-color:#8b5cf6;"></span>コピー</label>\n' +
'    <div style="margin-left:auto;display:flex;gap:4px;">\n' +
'      <button id="btn-filter-all" class="btn" style="padding:2px 8px;font-size:11px;">全選択</button>\n' +
'      <button id="btn-filter-none" class="btn" style="padding:2px 8px;font-size:11px;">全解除</button>\n' +
'    </div>\n' +
'  </div>\n' +
'\n' +
'  <div id="main-container">\n' +
'    <div id="history-sidebar" class="collapsed">\n' +
'      <div class="history-header">\n' +
'        <span>🕐 チケット履歴</span>\n' +
'        <button id="btn-close-history" class="btn" style="padding:2px 6px;">✕</button>\n' +
'      </div>\n' +
'      <div id="history-panel"></div>\n' +
'      <div class="history-footer">\n' +
'        <button id="btn-hist-clear" class="btn" style="width:100%;font-size:11px;padding:3px 8px;background:#dc2626;border-color:#b91c1c;">🗑️ 履歴をクリア</button>\n' +
'      </div>\n' +
'    </div>\n' +
'    <div id="network-container">\n' +
'      <div id="network"></div>\n' +
'      <div id="loading" class="loading-overlay">\n' +
'        <div class="spinner"></div>\n' +
'        <span id="loading-text">チケットデータを取得中...</span>\n' +
'      </div>\n' +
'      <div id="legend-modal">\n' +
'        <div style="display:flex;justify-content:space-between;align-items:center;margin-bottom:8px;">\n' +
'          <strong>凡例 (リレーション)</strong>\n' +
'          <span id="btn-close-legend" style="cursor:pointer;font-weight:bold;color:#64748b;font-size:15px;">✕</span>\n' +
'        </div>\n' +
'        <div class="legend-item"><span class="legend-line" style="border-bottom:2px dashed #64748b;"></span><span>親子タスク (点線 / 灰)</span></div>\n' +
'        <div class="legend-item"><span class="legend-line" style="background:#ef4444;"></span><span>ブロック (blocks / 赤)</span></div>\n' +
'        <div class="legend-item"><span class="legend-line" style="background:#0284c7;"></span><span>先行・後続 (precedes / 青)</span></div>\n' +
'        <div class="legend-item"><span class="legend-line" style="background:#10b981;"></span><span>関連 (relates / 緑)</span></div>\n' +
'        <div class="legend-item"><span class="legend-line" style="background:#f59e0b;"></span><span>重複 (duplicates / 橙)</span></div>\n' +
'        <div class="legend-item"><span class="legend-line" style="border-bottom:2px dashed #8b5cf6;"></span><span>コピー (copied / 紫)</span></div>\n' +
'        <div style="margin-top:8px;padding-top:6px;border-top:1px solid #e2e8f0;font-size:11px;color:#64748b;line-height:1.4;">\n' +
'          ※上部チェックボックスで各関連の表示/非表示を切替可能。<br>\n' +
'          ※金色の太枠が起点チケットです。<br>\n' +
'          ※ノードをダブルクリックで関連を展開できます。<br>\n' +
'          ※ノードをドラッグして位置を調整できます。\n' +
'        </div>\n' +
'      </div>\n' +
'    </div>\n' +
'\n' +
'    <div id="sidebar">\n' +
'      <div class="sidebar-header">\n' +
'        <span>チケット詳細</span>\n' +
'        <button id="btn-close-sidebar" class="btn" style="padding:2px 6px;">✕</button>\n' +
'      </div>\n' +
'      <div class="sidebar-content" id="sidebar-body">\n' +
'        <div style="color:#94a3b8;text-align:center;margin-top:40px;line-height:1.6;">\n' +
'          グラフ内のノードをクリックすると<br>詳細情報が表示されます\n' +
'        </div>\n' +
'      </div>\n' +
'      <div class="sidebar-actions" id="sidebar-actions" style="display:none;">\n' +
'        <button id="btn-open-redmine" class="btn" style="background:#2563eb;border-color:#1d4ed8;width:100%;">🔗 Redmineで開く (別タブ)</button>\n' +
'        <div class="sidebar-btn-row">\n' +
'          <button id="btn-expand-this" class="btn">🔄 関連展開</button>\n' +
'          <button id="btn-remove-this" class="btn" style="background:#dc2626;border-color:#b91c1c;">🗑️ 除外</button>\n' +
'        </div>\n' +
'      </div>\n' +
'    </div>\n' +
'  </div>\n' +
'\n' +
'  <div id="statusbar">\n' +
'    <div id="status-left">準備完了</div>\n' +
'    <div id="status-right">ノード: 0 | エッジ: 0</div>\n' +
'  </div>\n' +
'\n' +
'  <script>\n' +
'    (' + popupApp.toString() + ')(' + JSON.stringify(initialIssueId) + ', ' + JSON.stringify(baseUrl) + ', ' + JSON.stringify(loadHistoryForServer(baseUrl)) + ', ' + JSON.stringify(allHistory) + ');\n' +
'  </script>\n' +
'</body>\n' +
'</html>';

  win.document.open();
  win.document.write(html);
  win.document.close();
})();
