// ページ内テキストを Gemini Nano でその場 (in-place) 翻訳する。再実行で元に戻す。
// 翻訳するのは表示領域 (viewport) 内のテキストのみ。スクロール・リサイズで新たに見えた部分を追加で翻訳する。
(() => {
  if (globalThis.__nanoInPlace) { globalThis.__nanoInPlace.toggle(); return; }

  const { getTarget, wrap, clean, createSession, cache } = NanoPrompt;
  const SKIP_TAGS = new Set(["SCRIPT", "STYLE", "NOSCRIPT", "TEXTAREA", "INPUT", "CODE", "PRE", "SVG", "CANVAS", "IFRAME", "SELECT", "OPTION"]);
  const HAS_LETTER = /\p{L}{2,}/u;

  let state = "idle"; // idle (原文) | active (表示領域を翻訳。スクロール等で追加分も翻訳) | stopped (停止。翻訳済み部分は残す)
  let act = null; // 翻訳モード 1 回分 { ctrl, target, base, relayReady, busy }
  const originals = []; // [node, originalValue]
  let processed = new WeakSet(); // 処理済みのテキストノード (訳文に置換しても同じノードなので再翻訳しない)
  let viewTimer = 0;
  let badge, label, btnStop, btnToggle;

  // バッジ: 本体をドラッグで移動、バッジ外のクリックで閉じる、ボタンは 原文 ⇄ 翻訳 のトグル
  function createBadge() {
    const host = document.createElement("div");
    host.style.cssText = "all:initial;position:fixed;right:8px;bottom:8px;z-index:2147483647";
    const root = host.attachShadow({ mode: "open" });
    root.innerHTML = `
      <style>
        .b{font:11px/1.3 system-ui,sans-serif;background:#202124;color:#fff;border-radius:6px;padding:3px 6px;display:flex;gap:5px;align-items:center;box-shadow:0 1px 4px #0006;cursor:move;user-select:none;touch-action:none;white-space:nowrap}
        #l:empty{display:none}
        .g{border:0;padding:0 2px;font-size:13px;line-height:1}
        button{font:inherit;border:1px solid #fff6;background:transparent;color:#fff;border-radius:4px;padding:0 5px;cursor:pointer}
      </style>
      <div class="b"><span id="l"></span><button id="s">停止</button><button id="r"></button><button id="g" class="g" title="設定 (辞書の編集・クリア)">⚙️</button></div>`;
    label = root.getElementById("l");
    btnStop = root.getElementById("s");
    btnToggle = root.getElementById("r");
    btnStop.onclick = () => { finish("stopped"); label.textContent = "停止"; updateButtons(); };
    btnToggle.onclick = () => toggle();
    root.getElementById("g").onclick = () => { try { chrome.runtime.sendMessage({ type: "open-options" }); } catch {} };

    const box = root.querySelector(".b");
    box.addEventListener("pointerdown", (e) => {
      if (e.button !== 0 || e.target.closest("button")) return;
      const r = host.getBoundingClientRect();
      const dx = e.clientX - r.left;
      const dy = e.clientY - r.top;
      box.setPointerCapture(e.pointerId);
      const move = (ev) => {
        host.style.left = Math.max(0, Math.min(ev.clientX - dx, innerWidth - host.offsetWidth)) + "px";
        host.style.top = Math.max(0, Math.min(ev.clientY - dy, innerHeight - host.offsetHeight)) + "px";
        host.style.right = host.style.bottom = "auto";
      };
      const up = () => {
        box.removeEventListener("pointermove", move);
        box.removeEventListener("pointerup", up);
        box.removeEventListener("pointercancel", up);
      };
      box.addEventListener("pointermove", move);
      box.addEventListener("pointerup", up);
      box.addEventListener("pointercancel", up);
    });

    document.documentElement.append(host);
    document.addEventListener("pointerdown", onOutside, true);
    return host;
  }

  function onOutside(e) {
    if (badge && !e.composedPath().includes(badge)) closeBadge();
  }

  // バッジを閉じる (表示だけ消す)。翻訳モードは続き、スクロールで見えた部分も翻訳する。再実行で元に戻す
  function closeBadge() {
    document.removeEventListener("pointerdown", onOutside, true);
    badge?.remove();
    badge = null;
  }

  function updateButtons() {
    if (!badge) return;
    const busy = state === "active" && !!act?.busy;
    btnStop.hidden = !busy; // 翻訳処理中は「停止」のみ
    btnToggle.hidden = busy;
    btnToggle.textContent = state === "idle" ? "翻訳" : "原文";
  }

  // 古い翻訳モードの処理が表示を上書きしないよう、現在のモードのときだけ書く
  const setLabel = (a, t) => { if (act === a && badge && label) label.textContent = t; };

  // 表示領域内にある未処理のテキストノード (文書順)。overflow で切り取られた部分 (内側スクロール領域の外) は除く
  function collectVisible() {
    const nodes = [];
    const range = document.createRange();
    const clips = new Map();
    const clipOf = (el) => {
      if (!el || el === document.body || el === document.documentElement) return { top: 0, left: 0, bottom: innerHeight, right: innerWidth };
      let c = clips.get(el);
      if (c) return c;
      c = clipOf(el.parentElement);
      const cs = getComputedStyle(el);
      if (cs.overflowX !== "visible" || cs.overflowY !== "visible") {
        const b = el.getBoundingClientRect();
        c = { top: Math.max(c.top, b.top), left: Math.max(c.left, b.left), bottom: Math.min(c.bottom, b.bottom), right: Math.min(c.right, b.right) };
      }
      clips.set(el, c);
      return c;
    };
    const walker = document.createTreeWalker(document.body, NodeFilter.SHOW_TEXT, {
      acceptNode(n) {
        const p = n.parentElement;
        if (!p || processed.has(n) || SKIP_TAGS.has(p.tagName.toUpperCase()) || p.isContentEditable) return NodeFilter.FILTER_REJECT;
        if (!HAS_LETTER.test(n.nodeValue)) return NodeFilter.FILTER_REJECT;
        return NodeFilter.FILTER_ACCEPT;
      },
    });
    for (let n = walker.nextNode(); n; n = walker.nextNode()) {
      range.selectNodeContents(n);
      const r = range.getBoundingClientRect();
      if (!r.width || !r.height) continue;
      const c = clipOf(n.parentElement);
      if (r.bottom <= c.top || r.top >= c.bottom || r.right <= c.left || r.left >= c.right) continue;
      if (getComputedStyle(n.parentElement).visibility === "hidden") continue;
      nodes.push(n);
    }
    return nodes;
  }

  async function translateOne(a, text) {
    const { signal } = a.ctrl;
    if ("LanguageModel" in self) {
      // セッションは辞書にない原文が出た時点で初めて作り、翻訳モード中は使い回す
      a.base ??= await createSession(a.target, {
        signal,
        monitor(m) { m.addEventListener("downloadprogress", (e) => setLabel(a, e.loaded < 1 ? `モデル取得中 ${Math.floor(e.loaded * 100)}%` : "準備中…")); },
      });
      const clone = await a.base.clone({ signal });
      try { return clean(await clone.prompt(wrap(text, a.target), { signal })).trim(); } finally { clone.destroy(); }
    }
    // HTTP ページ等 (非セキュアコンテキスト) では Prompt API が無いため、拡張の offscreen document に中継する
    if (!a.relayReady) {
      const r = await chrome.runtime.sendMessage({ type: "ensure-offscreen" });
      if (!r?.ok) throw new Error(`拡張側で翻訳を準備できません (${r?.error ?? "応答なし"})`);
      a.relayReady = true;
    }
    const r = await chrome.runtime.sendMessage({ type: "nano-translate", target: a.target, text });
    if (!r?.ok) throw new Error(r?.error ?? "応答なし");
    return r.text;
  }

  // 表示領域の未処理ノードを 1 つずつ翻訳。毎回見え方を取り直すので、途中でスクロールしても今見えている部分を優先する
  async function pump() {
    const a = act;
    if (state !== "active" || !a || a.busy) return;
    a.busy = true;
    updateButtons();
    const { signal } = a.ctrl;
    try {
      a.target ??= await getTarget();
      let done = 0;
      for (let nodes = collectVisible(); nodes.length && !signal.aborted; nodes = collectVisible()) {
        const node = nodes[0];
        processed.add(node);
        setLabel(a, `翻訳中 ${done}/${done + nodes.length}`);
        const raw = node.nodeValue;
        const lead = raw.match(/^\s*/)[0];
        const trail = raw.match(/\s*$/)[0];
        const text = raw.trim();
        let out = await cache.get(a.target, text);
        if (out === null) {
          out = await translateOne(a, text);
          await cache.set(a.target, text, out);
        }
        if (signal.aborted) break;
        if (out && node.isConnected) {
          originals.push([node, raw]);
          node.nodeValue = lead + out + trail;
        }
        done++;
      }
      setLabel(a, "");
    } catch (e) {
      if (e.name !== "AbortError" && act === a) {
        setLabel(a, `エラー: ${e.message}`);
        finish("stopped");
      }
    }
    a.busy = false;
    updateButtons();
  }

  function onViewChange() {
    clearTimeout(viewTimer);
    viewTimer = setTimeout(pump, 300);
  }

  function start() {
    act = { ctrl: new AbortController(), target: null, base: null, relayReady: false, busy: false };
    state = "active";
    badge ??= createBadge();
    label.textContent = "";
    document.addEventListener("scroll", onViewChange, { capture: true, passive: true }); // 内側のスクロール領域も拾う
    addEventListener("resize", onViewChange);
    pump();
  }

  // 翻訳モードを終える (自動翻訳の停止・セッション破棄)
  function finish(next) {
    document.removeEventListener("scroll", onViewChange, { capture: true });
    removeEventListener("resize", onViewChange);
    clearTimeout(viewTimer);
    if (act) {
      act.ctrl.abort();
      act.base?.destroy();
      act = null;
    }
    state = next;
  }

  // 原文に戻す。バッジは残し、ボタンを「翻訳」に切り替える
  function restore() {
    finish("idle");
    for (const [node, raw] of originals) if (node.isConnected) node.nodeValue = raw;
    originals.length = 0;
    processed = new WeakSet();
    badge ??= createBadge();
    label.textContent = "";
    updateButtons();
  }

  function toggle() {
    if (state === "idle") start();
    else restore();
  }

  globalThis.__nanoInPlace = { toggle };
  start();
})();
