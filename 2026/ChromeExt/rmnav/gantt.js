// Redmine Graph Navigator (rmnav) - Gantt Editor Module (MV3)
(function () {
  const ROW_HEIGHT = 32;
  const ZOOM_PX = { day: 24, week: 10, month: 4, quarter: 1.5, year: 0.4 };

  let zoom = "week";
  let pxPerDay = ZOOM_PX.week;
  let timelineStartMs = 0;
  let timelineEndMs = 0;
  let totalDays = 0;
  let orderedRows = [];
  let selectedId = null;

  const pendingEdits = {}; // id -> { start_date, due_date, done_ratio }
  let hostCtx = {
    getIssuesCache: () => ({}),
    getPageIssueIds: () => null,
    getBaseUrl: () => "",
    onSelectIssue: () => {},
    onStatus: () => {},
    onRefreshIssues: async () => {},
    putIssue: async () => ({ ok: false })
  };

  function parseDateMs(str) {
    if (!str) return null;
    const m = /^(\d{4})-(\d{2})-(\d{2})$/.exec(str);
    if (!m) return null;
    return Date.UTC(Number(m[1]), Number(m[2]) - 1, Number(m[3]));
  }

  function formatMsDate(ms) {
    const d = new Date(ms);
    const pad = (n) => String(n).padStart(2, "0");
    return d.getUTCFullYear() + "-" + pad(d.getUTCMonth() + 1) + "-" + pad(d.getUTCDate());
  }

  function todayMs() {
    const now = new Date();
    return Date.UTC(now.getFullYear(), now.getMonth(), now.getDate());
  }

  function addDaysMs(ms, days) {
    return ms + days * 86400000;
  }

  function isParentIssue(issue) {
    return Array.isArray(issue.children) && issue.children.length > 0;
  }

  function isClosedIssue(issue) {
    return issue.status ? (issue.status.is_closed || /終了|完了|closed|resolved|却下/i.test(issue.status.name)) : false;
  }

  function getEffectiveValues(issue) {
    const edit = pendingEdits[String(issue.id)];
    return {
      start_date: edit && edit.start_date !== undefined ? edit.start_date : (issue.start_date || ""),
      due_date: edit && edit.due_date !== undefined ? edit.due_date : (issue.due_date || ""),
      done_ratio: edit && edit.done_ratio !== undefined ? edit.done_ratio : (issue.done_ratio != null ? issue.done_ratio : 0)
    };
  }

  function setEdit(issue, patch) {
    const id = String(issue.id);
    if (isParentIssue(issue)) return;
    const cur = pendingEdits[id] || {
      start_date: issue.start_date || "",
      due_date: issue.due_date || "",
      done_ratio: issue.done_ratio != null ? issue.done_ratio : 0
    };
    const next = { ...cur, ...patch };
    const origStart = issue.start_date || "";
    const origDue = issue.due_date || "";
    const origPct = issue.done_ratio != null ? issue.done_ratio : 0;
    if (next.start_date === origStart && next.due_date === origDue && Number(next.done_ratio) === Number(origPct)) {
      delete pendingEdits[id];
    } else {
      pendingEdits[id] = next;
    }
    updateDirtyUI();
  }

  function updateDirtyUI() {
    const count = Object.keys(pendingEdits).length;
    const badge = document.getElementById("gantt-dirty-badge");
    const btnDiscard = document.getElementById("btn-gantt-discard");
    const btnSave = document.getElementById("btn-gantt-save");
    if (!badge) return;
    if (count > 0) {
      badge.textContent = "未保存: " + count + "件";
      badge.style.display = "inline-block";
      btnDiscard.style.display = "inline-flex";
      btnSave.style.display = "inline-flex";
    } else {
      badge.style.display = "none";
      btnDiscard.style.display = "none";
      btnSave.style.display = "none";
    }
  }

  function buildHierarchyRows(cache) {
    const pageOnly = document.getElementById("chk-gantt-page-only")?.checked;
    const pageIds = pageOnly ? hostCtx.getPageIssueIds() : null;
    const pageSet = pageIds && pageIds.length > 0 ? new Set(pageIds.map(String)) : null;
    const openOnly = document.getElementById("chk-gantt-open-only")?.checked;
    const q = (document.getElementById("gantt-search")?.value || "").trim().toLowerCase();

    const all = Object.values(cache).filter((i) => i && i.id && (!pageSet || pageSet.has(String(i.id))));
    const byId = {};
    all.forEach((i) => { byId[String(i.id)] = i; });

    const matches = (i) => {
      if (openOnly && isClosedIssue(i)) return false;
      if (q) {
        const hay = ("#" + i.id + " " + (i.subject || "") + " " + (i.assigned_to ? i.assigned_to.name : "")).toLowerCase();
        if (!hay.includes(q)) return false;
      }
      return true;
    };

    const childrenMap = {};
    const roots = [];
    all.forEach((i) => {
      const pid = i.parent && i.parent.id ? String(i.parent.id) : null;
      if (pid && byId[pid]) {
        (childrenMap[pid] = childrenMap[pid] || []).push(i);
      } else {
        roots.push(i);
      }
    });

    const sortFn = (a, b) => {
      const sa = parseDateMs(a.start_date) || parseDateMs(a.due_date) || Number.MAX_SAFE_INTEGER;
      const sb = parseDateMs(b.start_date) || parseDateMs(b.due_date) || Number.MAX_SAFE_INTEGER;
      if (sa !== sb) return sa - sb;
      return Number(a.id) - Number(b.id);
    };
    roots.sort(sortFn);
    Object.values(childrenMap).forEach((arr) => arr.sort(sortFn));

    const result = [];
    const visited = new Set();
    function walk(issue, depth) {
      const id = String(issue.id);
      if (visited.has(id)) return;
      visited.add(id);
      if (matches(issue)) {
        result.push({ issue, depth });
      }
      (childrenMap[id] || []).forEach((ch) => walk(ch, depth + 1));
    }
    roots.forEach((r) => walk(r, 0));
    return result;
  }

  function computeTimelineRange(rows) {
    let minMs = todayMs();
    let maxMs = todayMs();
    let hasAny = false;
    rows.forEach(({ issue }) => {
      const eff = getEffectiveValues(issue);
      const s = parseDateMs(eff.start_date);
      const d = parseDateMs(eff.due_date);
      [s, d].forEach((ms) => {
        if (ms !== null) {
          if (!hasAny || ms < minMs) minMs = ms;
          if (!hasAny || ms > maxMs) maxMs = ms;
          hasAny = true;
        }
      });
    });
    timelineStartMs = addDaysMs(minMs, -14);
    timelineEndMs = addDaysMs(maxMs, 30);
    totalDays = Math.max(30, Math.ceil((timelineEndMs - timelineStartMs) / 86400000) + 1);
  }

  function renderTimelineHeaderAndGrid(totalHeight) {
    const hdr = document.getElementById("gantt-timeline-header");
    const grid = document.getElementById("gantt-grid");
    const canvas = document.getElementById("gantt-timeline-canvas");
    const width = totalDays * pxPerDay;

    hdr.style.width = width + "px";
    canvas.style.width = width + "px";
    canvas.style.height = totalHeight + "px";
    hdr.innerHTML = "";
    grid.innerHTML = "";

    for (let r = 0; r < orderedRows.length; r++) {
      const rowEl = document.createElement("div");
      rowEl.className = "gantt-grid-row";
      rowEl.style.top = (r * ROW_HEIGHT) + "px";
      grid.appendChild(rowEl);
    }

    let curMonth = -1;
    let monthStartX = 0;
    let monthLabel = "";

    const flushMonth = (endX) => {
      if (curMonth === -1) return;
      const mEl = document.createElement("div");
      mEl.className = "gantt-hdr-month";
      mEl.style.left = monthStartX + "px";
      mEl.style.width = Math.max(20, endX - monthStartX) + "px";
      mEl.textContent = monthLabel;
      hdr.appendChild(mEl);
    };

    for (let i = 0; i < totalDays; i++) {
      const ms = addDaysMs(timelineStartMs, i);
      const d = new Date(ms);
      const x = i * pxPerDay;
      const ym = zoom === "year"
        ? d.getUTCFullYear()
        : zoom === "quarter"
          ? d.getUTCFullYear() * 4 + Math.floor(d.getUTCMonth() / 3)
          : d.getUTCFullYear() * 12 + d.getUTCMonth();
      if (ym !== curMonth) {
        flushMonth(x);
        curMonth = ym;
        monthStartX = x;
        monthLabel = zoom === "year"
          ? d.getUTCFullYear() + "年"
          : zoom === "quarter"
            ? d.getUTCFullYear() + " Q" + (Math.floor(d.getUTCMonth() / 3) + 1)
            : d.getUTCFullYear() + "/" + String(d.getUTCMonth() + 1).padStart(2, "0");
      }

      const dow = d.getUTCDay();
      const dom = d.getUTCDate();
      const showTick =
        zoom === "day" ||
        (zoom === "week" && (dow === 1 || dom === 1)) ||
        ((zoom === "month" || zoom === "quarter") && dom === 1) ||
        (zoom === "year" && dom === 1 && d.getUTCMonth() % 3 === 0);

      if (showTick) {
        const tEl = document.createElement("div");
        tEl.className = "gantt-hdr-tick";
        tEl.style.left = x + "px";
        tEl.style.width = (zoom === "day" ? pxPerDay : pxPerDay * (zoom === "week" ? 7 : zoom === "year" ? 90 : 30)) + "px";
        tEl.textContent = zoom === "year"
          ? "Q" + (Math.floor(d.getUTCMonth() / 3) + 1)
          : zoom === "quarter"
            ? (d.getUTCMonth() + 1) + "月"
            : (zoom === "month" ? "" : String(dom));
        hdr.appendChild(tEl);

        const colEl = document.createElement("div");
        colEl.className = "gantt-grid-col" + ((dow === 0 || dow === 6) && zoom === "day" ? " weekend" : "");
        colEl.style.left = x + "px";
        if (zoom === "day") colEl.style.width = pxPerDay + "px";
        grid.appendChild(colEl);
      } else if (zoom === "day" && (dow === 0 || dow === 6)) {
        const colEl = document.createElement("div");
        colEl.className = "gantt-grid-col weekend";
        colEl.style.left = x + "px";
        colEl.style.width = pxPerDay + "px";
        grid.appendChild(colEl);
      }
    }
    flushMonth(width);

    const tMs = todayMs();
    const todayLine = document.getElementById("gantt-today-line");
    if (tMs >= timelineStartMs && tMs <= timelineEndMs) {
      todayLine.style.display = "block";
      todayLine.style.left = (((tMs - timelineStartMs) / 86400000) * pxPerDay) + "px";
    } else {
      todayLine.style.display = "none";
    }
  }

  function renderArrows(rowIndexById, totalHeight) {
    const svg = document.getElementById("gantt-arrows");
    svg.setAttribute("width", totalDays * pxPerDay);
    svg.setAttribute("height", totalHeight);
    svg.innerHTML = '<defs><marker id="gantt-arr" markerWidth="6" markerHeight="6" refX="5" refY="3" orient="auto"><path d="M0,0 L6,3 L0,6 Z" fill="#0284c7"/></marker></defs>';

    const cache = hostCtx.getIssuesCache();
    const drawn = new Set();
    orderedRows.forEach(({ issue }, toIdx) => {
      (issue.relations || []).forEach((rel) => {
        if (rel.relation_type !== "precedes" && rel.relation_type !== "blocks") return;
        const fromId = String(rel.issue_id);
        const toId = String(rel.issue_to_id);
        const fromIdx = rowIndexById[fromId];
        const targetIdx = rowIndexById[toId];
        if (fromIdx === undefined || targetIdx === undefined) return;
        const key = fromId + "_" + toId + "_" + rel.relation_type;
        if (drawn.has(key)) return;
        drawn.add(key);

        const fIssue = cache[fromId];
        const tIssue = cache[toId];
        if (!fIssue || !tIssue) return;
        const fEff = getEffectiveValues(fIssue);
        const tEff = getEffectiveValues(tIssue);
        const fEnd = parseDateMs(fEff.due_date || fEff.start_date);
        const tStart = parseDateMs(tEff.start_date || tEff.due_date);
        if (fEnd === null || tStart === null) return;

        const x1 = (((fEnd - timelineStartMs) / 86400000) + 1) * pxPerDay;
        const y1 = fromIdx * ROW_HEIGHT + ROW_HEIGHT / 2;
        const x2 = ((tStart - timelineStartMs) / 86400000) * pxPerDay;
        const y2 = targetIdx * ROW_HEIGHT + ROW_HEIGHT / 2;
        const midX = x1 + 8;

        const path = document.createElementNS("http://www.w3.org/2000/svg", "path");
        const color = rel.relation_type === "blocks" ? "#ef4444" : "#0284c7";
        path.setAttribute("d", `M ${x1} ${y1} L ${midX} ${y1} L ${midX} ${y2} L ${x2} ${y2}`);
        path.setAttribute("fill", "none");
        path.setAttribute("stroke", color);
        path.setAttribute("stroke-width", "1.4");
        path.setAttribute("stroke-dasharray", rel.relation_type === "blocks" ? "3,2" : "");
        path.setAttribute("marker-end", "url(#gantt-arr)");
        svg.appendChild(path);
      });
    });
  }

  function attachBarDrag(barEl, issue, mode) {
    barEl.addEventListener("mousedown", (e) => {
      if (isParentIssue(issue)) return;
      e.stopPropagation();
      e.preventDefault();

      const startX = e.clientX;
      const eff = getEffectiveValues(issue);
      let sMs = parseDateMs(eff.start_date || eff.due_date);
      let dMs = parseDateMs(eff.due_date || eff.start_date);
      if (sMs === null || dMs === null) return;
      if (dMs < sMs) dMs = sMs;

      const onMove = (ev) => {
        const deltaDays = Math.round((ev.clientX - startX) / pxPerDay);
        let ns = sMs;
        let nd = dMs;
        if (mode === "move") {
          ns = addDaysMs(sMs, deltaDays);
          nd = addDaysMs(dMs, deltaDays);
        } else if (mode === "left") {
          ns = Math.min(dMs, addDaysMs(sMs, deltaDays));
        } else if (mode === "right") {
          nd = Math.max(sMs, addDaysMs(dMs, deltaDays));
        }
        const leftPx = ((ns - timelineStartMs) / 86400000) * pxPerDay;
        const widthPx = Math.max(pxPerDay, (((nd - ns) / 86400000) + 1) * pxPerDay);
        barEl.style.left = leftPx + "px";
        barEl.style.width = widthPx + "px";
        barEl.dataset.dragStart = formatMsDate(ns);
        barEl.dataset.dragDue = formatMsDate(nd);
      };

      const onUp = () => {
        document.removeEventListener("mousemove", onMove);
        document.removeEventListener("mouseup", onUp);
        if (barEl.dataset.dragStart && barEl.dataset.dragDue) {
          setEdit(issue, { start_date: barEl.dataset.dragStart, due_date: barEl.dataset.dragDue });
          render();
        }
      };

      document.addEventListener("mousemove", onMove);
      document.addEventListener("mouseup", onUp);
    });
  }

  function render() {
    const view = document.getElementById("gantt-view");
    if (!view || view.style.display === "none") return;

    const cache = hostCtx.getIssuesCache();
    orderedRows = buildHierarchyRows(cache);
    computeTimelineRange(orderedRows);

    const totalHeight = Math.max(200, orderedRows.length * ROW_HEIGHT);
    renderTimelineHeaderAndGrid(totalHeight);

    const leftRows = document.getElementById("gantt-left-rows");
    const barsEl = document.getElementById("gantt-bars");
    leftRows.innerHTML = "";
    barsEl.innerHTML = "";

    const rowIndexById = {};
    orderedRows.forEach(({ issue, depth }, idx) => {
      const id = String(issue.id);
      rowIndexById[id] = idx;
      const isParent = isParentIssue(issue);
      const isClosed = isClosedIssue(issue);
      const eff = getEffectiveValues(issue);
      const isDirty = !!pendingEdits[id];

      // Left row
      const lRow = document.createElement("div");
      lRow.className = "gantt-left-row" + (selectedId === id ? " selected" : "") + (isDirty ? " dirty" : "");
      lRow.onclick = () => {
        selectedId = id;
        hostCtx.onSelectIssue(id);
        render();
      };

      const idCol = document.createElement("div");
      idCol.className = "gl-col-id";
      idCol.textContent = "#" + id;

      const subjCol = document.createElement("div");
      subjCol.className = "gl-col-subj";
      subjCol.style.paddingLeft = (depth * 14) + "px";
      subjCol.title = (isParent ? "[親: 子チケットから自動算出] " : "") + (issue.subject || "");
      subjCol.textContent = (isParent ? "🔒 " : "") + (issue.subject || "");

      const startCol = document.createElement("div");
      startCol.className = "gl-col-date";
      const startIn = document.createElement("input");
      startIn.type = "date";
      startIn.className = "gantt-date-input" + (isDirty ? " dirty" : "");
      startIn.value = eff.start_date;
      startIn.disabled = isParent;
      startIn.onclick = (e) => e.stopPropagation();
      startIn.onchange = () => {
        setEdit(issue, { start_date: startIn.value });
        render();
      };
      startCol.appendChild(startIn);

      const dueCol = document.createElement("div");
      dueCol.className = "gl-col-date";
      const dueIn = document.createElement("input");
      dueIn.type = "date";
      dueIn.className = "gantt-date-input" + (isDirty ? " dirty" : "");
      dueIn.value = eff.due_date;
      dueIn.disabled = isParent;
      dueIn.onclick = (e) => e.stopPropagation();
      dueIn.onchange = () => {
        setEdit(issue, { due_date: dueIn.value });
        render();
      };
      dueCol.appendChild(dueIn);

      const pctCol = document.createElement("div");
      pctCol.className = "gl-col-pct";
      const pctIn = document.createElement("input");
      pctIn.type = "number";
      pctIn.min = "0";
      pctIn.max = "100";
      pctIn.step = "10";
      pctIn.className = "gantt-pct-input";
      pctIn.value = eff.done_ratio;
      pctIn.disabled = isParent;
      pctIn.onclick = (e) => e.stopPropagation();
      pctIn.onchange = () => {
        const v = Math.max(0, Math.min(100, Number(pctIn.value) || 0));
        setEdit(issue, { done_ratio: v });
        render();
      };
      pctCol.appendChild(pctIn);

      lRow.append(idCol, subjCol, startCol, dueCol, pctCol);
      leftRows.appendChild(lRow);

      // Timeline Bar
      const sMs = parseDateMs(eff.start_date || eff.due_date);
      const dMs = parseDateMs(eff.due_date || eff.start_date);
      if (sMs !== null && dMs !== null) {
        const startPlot = Math.min(sMs, dMs);
        const endPlot = Math.max(sMs, dMs);
        const leftPx = ((startPlot - timelineStartMs) / 86400000) * pxPerDay;
        const widthPx = Math.max(pxPerDay, (((endPlot - startPlot) / 86400000) + 1) * pxPerDay);

        const bar = document.createElement("div");
        bar.className = "gantt-bar" +
          (isParent ? " parent" : "") +
          (isClosed ? " closed" : "") +
          (isDirty ? " dirty" : "") +
          (selectedId === id ? " selected" : "");
        bar.style.left = leftPx + "px";
        bar.style.width = widthPx + "px";
        bar.style.top = (idx * ROW_HEIGHT + (isParent ? 9 : 6)) + "px";
        bar.title = `#${id} ${issue.subject || ""}\n${eff.start_date || "-"} .. ${eff.due_date || "-"} (${eff.done_ratio}%)` +
          (isParent ? "\n※親タスクは子チケットから自動算出されるため編集ロック中" : "\n※ドラッグで移動、左右端で期間変更");

        const prog = document.createElement("div");
        prog.className = "gantt-bar-progress";
        prog.style.width = (eff.done_ratio || 0) + "%";
        bar.appendChild(prog);

        const lbl = document.createElement("div");
        lbl.className = "gantt-bar-label";
        lbl.textContent = `#${id} ${issue.subject || ""}`;
        bar.appendChild(lbl);

        bar.onclick = (e) => {
          e.stopPropagation();
          selectedId = id;
          hostCtx.onSelectIssue(id);
          render();
        };

        if (!isParent) {
          const hLeft = document.createElement("div");
          hLeft.className = "gantt-handle left";
          attachBarDrag(hLeft, issue, "left");
          const hRight = document.createElement("div");
          hRight.className = "gantt-handle right";
          attachBarDrag(hRight, issue, "right");
          bar.append(hLeft, hRight);
          attachBarDrag(bar, issue, "move");
        }
        barsEl.appendChild(bar);
      }
    });

    renderArrows(rowIndexById, totalHeight);
    updateDirtyUI();
  }

  function scrollToToday() {
    const right = document.getElementById("gantt-right");
    if (!right) return;
    const x = ((todayMs() - timelineStartMs) / 86400000) * pxPerDay;
    right.scrollLeft = Math.max(0, x - right.clientWidth / 3);
  }

  async function saveAllEdits() {
    const ids = Object.keys(pendingEdits);
    if (ids.length === 0) return;
    const cache = hostCtx.getIssuesCache();
    const baseUrl = hostCtx.getBaseUrl();
    if (!baseUrl) {
      alert("Redmine のベースURLが不明です。");
      return;
    }

    const lines = ids.map((id) => {
      const e = pendingEdits[id];
      const subj = cache[id] ? cache[id].subject : "";
      return `#${id} (${subj.slice(0, 28)}): ${e.start_date || "-"} .. ${e.due_date || "-"} (${e.done_ratio}%)`;
    });
    if (!confirm(`以下の ${ids.length} 件を Redmine に保存しますか？\n\n` + lines.join("\n"))) {
      return;
    }

    const refreshSet = new Set();
    let okCount = 0;
    const errors = [];

    for (const id of ids) {
      const e = pendingEdits[id];
      const payload = {
        issue: {
          start_date: e.start_date || null,
          due_date: e.due_date || null,
          done_ratio: Number(e.done_ratio) || 0
        }
      };
      const res = await hostCtx.putIssue(baseUrl + "/issues/" + id + ".json", baseUrl, payload);
      if (res && res.ok) {
        okCount++;
        delete pendingEdits[id];
        refreshSet.add(id);
        const orig = cache[id];
        if (orig && orig.parent && orig.parent.id) {
          refreshSet.add(String(orig.parent.id));
        }
      } else {
        const msg = res && res.status === 401
          ? "認証エラー(401): ⚙️ 設定から API キーを登録してください"
          : `HTTP ${res ? res.status : 0} ${res && res.error ? res.error : ""}`;
        errors.push(`#${id}: ${msg}`);
      }
    }

    if (refreshSet.size > 0) {
      await hostCtx.onRefreshIssues(Array.from(refreshSet));
    }
    render();

    if (errors.length > 0) {
      alert(`保存完了: ${okCount}件 / 失敗: ${errors.length}件\n\n` + errors.join("\n"));
    } else {
      hostCtx.onStatus(`Redmine に ${okCount} 件の日程・進捗を保存しました`);
    }
  }

  function bindEvents() {
    const zoomSel = document.getElementById("gantt-zoom-select");
    if (zoomSel) {
      zoomSel.onchange = () => {
        zoom = zoomSel.value || "week";
        pxPerDay = ZOOM_PX[zoom] || 10;
        render();
      };
    }

    document.getElementById("btn-gantt-today").onclick = scrollToToday;
    document.getElementById("btn-gantt-fit").onclick = () => {
      const right = document.getElementById("gantt-right");
      if (right) right.scrollLeft = 0;
    };
    document.getElementById("chk-gantt-page-only").onchange = render;
    document.getElementById("chk-gantt-open-only").onchange = render;
    document.getElementById("gantt-search").oninput = render;
    document.getElementById("btn-gantt-discard").onclick = () => {
      if (confirm("未保存の編集内容をすべて破棄しますか？")) {
        Object.keys(pendingEdits).forEach((k) => delete pendingEdits[k]);
        render();
      }
    };
    document.getElementById("btn-gantt-save").onclick = saveAllEdits;

    const right = document.getElementById("gantt-right");
    const leftRows = document.getElementById("gantt-left-rows");
    right.addEventListener("scroll", () => {
      leftRows.scrollTop = right.scrollTop;
    });
    leftRows.addEventListener("scroll", () => {
      right.scrollTop = leftRows.scrollTop;
    });
  }

  window.RmnavGantt = {
    init(ctx) {
      hostCtx = ctx;
      bindEvents();
    },
    render,
    scrollToToday,
    clearEdits() {
      Object.keys(pendingEdits).forEach((k) => delete pendingEdits[k]);
      updateDirtyUI();
    }
  };
})();
