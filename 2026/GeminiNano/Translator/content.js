// ページ内テキストを Gemini Nano でその場 (in-place) 翻訳する。再実行で元に戻す。
// 翻訳単位: ブロック要素 1 つ (インライン要素は <1>..</1> / <2/> のプレースホルダで構造を保持して 1 回で翻訳)。検証に失敗したらテキストノード単位へフォールバック
// 翻訳順: 表示領域内を優先し、続けて画面外のロード済み部分 (表示領域に近い順)。スクロール・リサイズ時は表示領域内を再優先する。
(() => {
  if (globalThis.__nanoInPlace) { globalThis.__nanoInPlace.toggle(); return; }

  const { getTarget, wrap, wrapBatch, parseBatch, clean, createSession, cache, limits, codeParams } = NanoPrompt;
  const SKIP_TAGS = new Set(["SCRIPT", "STYLE", "NOSCRIPT", "TEXTAREA", "INPUT", "SVG", "CANVAS", "IFRAME", "SELECT", "OPTION"]);
  const BLOCK_SEL = "address,article,aside,blockquote,dd,details,dialog,div,dl,dt,fieldset,figcaption,figure,footer,form,h1,h2,h3,h4,h5,h6,header,hr,li,main,nav,ol,p,pre,section,summary,table,tbody,td,tfoot,th,thead,tr,ul,button,select,textarea,iframe,video,canvas";
  let maxUnit = 2000; // 1 回に AI で翻訳する最大文字数。複数の単位をこの文字数までまとめる。超えるブロックはノード単位に分割 (設定 maxUnit)
  // ソースコードと判定する要素 (タグ + 主要なシンタックスハイライタのクラス)。設定 skipCode (既定 ON) で翻訳対象から外す
  const CODE_SEL = 'pre,code,kbd,samp,tt,[class*="language-"],.hljs,.highlight,.prettyprint,.sourceCode,.codehilite,.shiki,.code-block,.CodeMirror,.cm-editor,.monaco-editor,.syntaxhighlighter';
  let skipCode = true;
  // 文脈からのコード判定 (タグ・クラスが無くても、等幅フォント・記号密度・キーワード・行末記号・インデントで推定)
  // 点数・しきい値・フォント名・キーワードは設定 codeParams (翻訳ループの開始時に読み込む)
  // 採点は prompt.js の codeScore (設定ページの試算と共用)
  let cp, re;
  let codeCache = new WeakMap();
  function setCodeParams(p) {
    cp = p;
    re = NanoPrompt.codeRegex(p);
    codeCache = new WeakMap();
  }
  setCodeParams(NanoPrompt.CODE_DEFAULTS);
  function codeLike(el) {
    if (codeCache.has(el)) return codeCache.get(el);
    const r = NanoPrompt.codeScore(el.textContent || "", re.mono.test(getComputedStyle(el).fontFamily), cp, re).code;
    codeCache.set(el, r);
    return r;
  }
  const blockOf = (el) => { while (el.parentElement && el !== document.body && getComputedStyle(el).display.startsWith("inline")) el = el.parentElement; return el; };
  const isCode = (el) => !!el.closest(CODE_SEL) || codeLike(blockOf(el));
  const isSkipped = (el) => SKIP_TAGS.has(el.tagName.toUpperCase()) || el.isContentEditable || (skipCode && isCode(el));
  const HAS_LETTER = /\p{L}{2,}/u;

  let state = "idle"; // idle (原文表示) | active (表示領域を翻訳。スクロール等で追加分も翻訳) | stopped (エラー等で自動翻訳を止めた。翻訳済み部分は残す)
  let act = null; // 翻訳モード 1 回分 { ctrl, target, base, relayReady, busy }
  const entries = []; // { show(), hide() } 訳文表示 / 原文表示の切り替え用
  const processed = new WeakSet(); // 処理済みのテキストノード (訳文に置換しても同じノードなので再翻訳しない)
  let viewTimer = 0;
  let badge, label, btnOrig, btnTrans, lbTrans;

  // バッジ: 本体をドラッグで移動、✕で閉じる、原文 ○─ 翻訳 の Material 3 風スイッチで表示を切り替え
  function createBadge() {
    const host = document.createElement("div");
    host.style.cssText = "all:initial;position:fixed;right:8px;bottom:8px;z-index:2147483647";
    const root = host.attachShadow({ mode: "open" });
    root.innerHTML = `
      <style>
        .b{font:12px/1.3 system-ui,sans-serif;background:#202124;color:#fff;border-radius:6px;padding:3px 6px;display:flex;gap:5px;align-items:center;box-shadow:0 1px 4px #0006;cursor:move;user-select:none;touch-action:none;white-space:nowrap}
        #l{max-width:260px;overflow:hidden;text-overflow:ellipsis}
        #l:empty{display:none}
        .g{border:0;padding:0;width:22px;height:22px;display:flex;align-items:center;justify-content:center;opacity:.8}
        .g:hover{opacity:1;background:#fff2}
        .g svg{width:14px;height:14px;fill:none;stroke:currentColor;stroke-width:2;stroke-linecap:round;stroke-linejoin:round;pointer-events:none}
        button{font:inherit;border:1px solid #fff6;background:transparent;color:#fff;border-radius:4px;padding:0 5px;cursor:pointer}
        .sw{display:flex;align-items:center;gap:6px}
        .lb{opacity:.55;cursor:pointer;transition:opacity .2s}
        .lb.on{opacity:1}
        .s{position:relative;box-sizing:border-box;width:36px;height:20px;padding:0;border:2px solid #9aa0a6;border-radius:10px;background:#3c4043;transition:background .2s,border-color .2s}
        .s::after{content:"";position:absolute;top:50%;left:3px;width:10px;height:10px;margin-top:-5px;border-radius:50%;background:#9aa0a6;transition:left .2s cubic-bezier(.2,0,0,1),width .2s,height .2s,margin-top .2s,background .2s}
        .s[aria-checked=true]{background:#8ab4f8;border-color:#8ab4f8}
        .s[aria-checked=true]::after{left:15px;width:14px;height:14px;margin-top:-7px;background:#202124}
        .s:active::after{left:2px;width:16px;height:16px;margin-top:-8px}
        .s[aria-checked=true]:active::after{left:13px}
        .s:focus-visible{outline:2px solid #8ab4f8;outline-offset:2px}
      </style>
      <div class="b"><span id="l"></span><span class="sw"><span class="lb" id="o">原文</span><button class="s" id="t" role="switch" aria-checked="false" aria-label="翻訳"></button><span class="lb" id="tl">翻訳</span></span><button id="g" class="g" title="設定 (辞書の編集・クリア)" aria-label="設定"><svg viewBox="0 0 24 24"><circle cx="12" cy="12" r="3"/><path d="M19.4 15a1.65 1.65 0 0 0 .33 1.82l.06.06a2 2 0 0 1 0 2.83 2 2 0 0 1-2.83 0l-.06-.06a1.65 1.65 0 0 0-1.82-.33 1.65 1.65 0 0 0-1 1.51V21a2 2 0 0 1-2 2 2 2 0 0 1-2-2v-.09A1.65 1.65 0 0 0 9 19.4a1.65 1.65 0 0 0-1.82.33l-.06.06a2 2 0 0 1-2.83 0 2 2 0 0 1 0-2.83l.06-.06a1.65 1.65 0 0 0 .33-1.82 1.65 1.65 0 0 0-1.51-1H3a2 2 0 0 1-2-2 2 2 0 0 1 2-2h.09A1.65 1.65 0 0 0 4.6 9a1.65 1.65 0 0 0-.33-1.82l-.06-.06a2 2 0 0 1 0-2.83 2 2 0 0 1 2.83 0l.06.06a1.65 1.65 0 0 0 1.82.33H9a1.65 1.65 0 0 0 1-1.51V3a2 2 0 0 1 2-2 2 2 0 0 1 2 2v.09a1.65 1.65 0 0 0 1 1.51 1.65 1.65 0 0 0 1.82-.33l.06-.06a2 2 0 0 1 2.83 0 2 2 0 0 1 0 2.83l-.06.06a1.65 1.65 0 0 0-.33 1.82V9a1.65 1.65 0 0 0 1.51 1H21a2 2 0 0 1 2 2 2 2 0 0 1-2 2h-.09a1.65 1.65 0 0 0-1.51 1z"/></svg></button><button id="x" class="g" title="閉じる" aria-label="閉じる"><svg viewBox="0 0 24 24"><path d="M6 6l12 12M18 6L6 18"/></svg></button></div>`;
    label = root.getElementById("l");
    btnOrig = root.getElementById("o");
    btnTrans = root.getElementById("t");
    lbTrans = root.getElementById("tl");
    const toOriginal = () => { if (state !== "idle") restore(); };
    const toTranslated = () => { if (state !== "active") { finish("idle"); start(); } }; // 原文表示中・エラー停止後のどちらからでも (再) 開始
    btnOrig.onclick = toOriginal;
    lbTrans.onclick = toTranslated;
    btnTrans.onclick = () => (state === "active" ? toOriginal() : toTranslated());
    root.getElementById("x").onclick = closeBadge;
    root.getElementById("g").onclick = () => { try { chrome.runtime.sendMessage({ type: "open-options" }); } catch {} };

    const box = root.querySelector(".b");
    box.addEventListener("pointerdown", (e) => {
      if (e.button !== 0 || e.target.closest("button")) return;
      const r = host.getBoundingClientRect();
      const dx = e.clientX - r.left;
      const dy = e.clientY - r.top;
      box.setPointerCapture(e.pointerId);
      const move = (ev) => {
        // 右端・下端を基準に固定する。進捗表示の増減で幅が変わっても右端は動かず、左端だけが伸び縮みする
        const w = host.offsetWidth, h = host.offsetHeight;
        const left = Math.max(0, Math.min(ev.clientX - dx, innerWidth - w));
        const top = Math.max(0, Math.min(ev.clientY - dy, innerHeight - h));
        host.style.right = innerWidth - left - w + "px";
        host.style.bottom = innerHeight - top - h + "px";
        host.style.left = host.style.top = "auto";
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
    return host;
  }

  // バッジを閉じる (表示だけ消す)。翻訳モードは続き、スクロールで見えた部分も翻訳する。再実行 (Alt+P) で元に戻す
  function closeBadge() {
    badge?.remove();
    badge = null;
  }

  function updateButtons() {
    if (!badge) return;
    const on = state !== "idle";
    btnTrans.setAttribute("aria-checked", on);
    btnOrig.classList.toggle("on", !on);
    lbTrans.classList.toggle("on", on);
  }

  // 古い翻訳モードの処理が表示を上書きしないよう、現在のモードのときだけ書く
  const setLabel = (a, t) => { if (act === a && badge && label) label.textContent = label.title = t; };

  // 次に翻訳する単位の一覧。表示領域内の未処理テキストノード (文書順) を優先し、無ければ表示領域外 (画面外・overflow で切り取られた部分) で
  // (旧) 表示領域に最も近い 1 ノードを返す。ロード済みの残りのページも順に翻訳するが、表示中の部分が常に先。units.pending = 未処理ノード総数
  // 薄いハイライト。CSS Custom Highlight API で描くので DOM を変えない (MutationObserver に掛からない)
  //   未翻訳 (黄): 設定 markUntranslated (既定 ON) / コードと判定して翻訳しない部分 (青): 設定 markCode (既定 OFF)
  let markUntranslated = true, markCode = false;
  const failed = new WeakSet(); // 訳文が得られなかったテキストノード (処理済みだが未翻訳として表示)
  const HL = "nano-untranslated", HL_CODE = "nano-code", HL_LIVE = "nano-live"; // HL_LIVE: デバッグ時の AI 翻訳中の部分 (緑)
  function hlStyle() {
    if (document.getElementById("nano-highlight-style")) return;
    const st = document.createElement("style");
    st.id = "nano-highlight-style";
    st.textContent = `::highlight(${HL}){background-color:rgba(255,193,7,.16)}::highlight(${HL_CODE}){background-color:rgba(66,133,244,.13)}::highlight(${HL_LIVE}){background-color:rgba(52,168,83,.28)}`;
    (document.head ?? document.documentElement).append(st);
  }
  function paint(name, nodes, on) {
    if (!globalThis.Highlight || !CSS.highlights) return;
    if (!on || !nodes.length) { CSS.highlights.delete(name); return; }
    hlStyle();
    const h = new Highlight();
    for (const n of nodes) { const r = new Range(); r.selectNodeContents(n); h.add(r); }
    CSS.highlights.set(name, h);
  }
  const unpaint = () => { paint(HL, [], false); paint(HL_CODE, [], false); };

  function collectVisible() {
    const nodes = [];
    const far = []; // 表示領域外のノード { n, dist }
    let pending = 0;
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
    const codes = []; // コードと判定したテキストノード
    const walker = document.createTreeWalker(document.body, NodeFilter.SHOW_TEXT, {
      acceptNode(n) {
        const p = n.parentElement;
        if (!p) return NodeFilter.FILTER_REJECT;
        if (skipCode && !SKIP_TAGS.has(p.tagName.toUpperCase()) && !p.isContentEditable && isCode(p)) {
          if ((markCode || debugCode) && n.nodeValue.trim()) codes.push(n);
          return NodeFilter.FILTER_REJECT;
        }
        if ((processed.has(n) && !failed.has(n)) || isSkipped(p)) return NodeFilter.FILTER_REJECT;
        if (!HAS_LETTER.test(n.nodeValue)) return NodeFilter.FILTER_REJECT;
        return NodeFilter.FILTER_ACCEPT;
      },
    });
    const marks = []; // 未翻訳 (未処理 + 失敗) のテキストノード
    for (let n = walker.nextNode(); n; n = walker.nextNode()) {
      marks.push(n);
      if (failed.has(n)) continue;
      range.selectNodeContents(n);
      const r = range.getBoundingClientRect();
      if (!r.width || !r.height) continue;
      const c = clipOf(n.parentElement);
      if (getComputedStyle(n.parentElement).visibility === "hidden") continue;
      pending++;
      const dy = r.bottom <= c.top ? c.top - r.bottom : r.top >= c.bottom ? r.top - c.bottom : 0;
      const dx = r.right <= c.left ? c.left - r.right : r.left >= c.right ? r.left - c.right : 0;
      if (!dx && !dy) nodes.push(n);
      else far.push({ n, dist: dx + dy });
    }
    // 表示中の後に、画面外を表示領域に近い順で足す (まとめ翻訳 1 回分程度まで)
    far.sort((x, y) => x.dist - y.dist);
    let len = nodes.reduce((s, n) => s + n.nodeValue.length, 0);
    for (const f of far) {
      if (nodes.length && len > maxUnit) break;
      nodes.push(f.n);
      len += f.n.nodeValue.length;
    }
    paint(HL, marks, markUntranslated);
    paint(HL_CODE, codes, markCode || debugCode);
    codeNodes = new Set(codes);
    // テキストノード → 翻訳単位 (インライン要素をたどって最寄りのブロック要素)。ブロックが入れ子の場合はノード単位
    const units = [];
    const seen = new Set();
    for (const n of nodes) {
      let el = n.parentElement;
      while (el && el !== document.body && getComputedStyle(el).display.startsWith("inline")) el = el.parentElement;
      const u = !el || el === document.body || el.querySelector(BLOCK_SEL) ? n : el;
      if (seen.has(u)) continue;
      seen.add(u);
      units.push(u);
    }
    units.pending = pending;
    return units;
  }

  // ブロック要素を プレースホルダ付きテキストにする。atomic: 翻訳しない要素 (<n/>)
  // アイコン等 (文字を持たない要素) は ghost として翻訳対象から外し、build() で元の位置に戻す。
  // プレースホルダにするとモデルが位置を動かし、矢印などが文中に移動してしまうため
  // atomic も位置を places に記録し、訳文で欠落していたら build() で補完する
  function serialize(root) {
    const map = new Map(); // id → { el, atomic }
    // { el, id?: atomic の id, parent: 親の id (0 = ブロック本体), pos: "start" | "end" | { after?: 直前のプレースホルダ id }, at / len: 親内の文字位置 (比率で補完) }
    const places = [];
    let id = 0;
    const isGhost = (c) => c.tagName !== "BR" && c.tagName !== "IMG" && !HAS_LETTER.test(c.textContent);
    const atomic = (c) => isSkipped(c) || (skipCode && codeLike(c)) || c.tagName === "BR" || c.tagName === "IMG" || !HAS_LETTER.test(c.textContent);
    const content = (x) => (x.nodeType === 3 ? x.nodeValue.trim() !== "" : x.nodeType === 1 && !isGhost(x));
    const walk = (parent, parentId) => {
      let out = "";
      let lastId = null; // 直前のプレースホルダ
      const mine = [];
      const kids = [...parent.childNodes];
      const place = (i, el, extra) => {
        const before = kids.slice(0, i).some(content);
        const after = kids.slice(i + 1).some(content);
        const p = { el, parent: parentId, pos: !before ? "start" : !after ? "end" : lastId === null ? {} : { after: lastId }, at: out.length, ...extra };
        places.push(p);
        mine.push(p);
      };
      for (let i = 0; i < kids.length; i++) {
        const c = kids[i];
        if (c.nodeType === 3) out += c.nodeValue;
        else if (c.nodeType === 1 && isGhost(c)) place(i, c);
        else if (c.nodeType === 1) {
          const n = ++id;
          if (atomic(c)) { map.set(n, { el: c, atomic: true }); place(i, c, { id: n }); out += `<${n}/>`; }
          else { map.set(n, { el: c, atomic: false }); out += `<${n}>${walk(c, n)}</${n}>`; }
          lastId = n;
        }
      }
      for (const p of mine) p.len = out.length;
      return out;
    };
    const raw = walk(root, 0);
    const text = root.closest("pre") ? raw.trim() : raw.replace(/\s+/g, " ").trim(); // pre 内は改行・インデントを保持
    return { text, map, places };
  }

  // 子ノード列 arr の文字数の比率 r の位置に el を入れる (テキストノードは近くの空白・句読点で分割)
  function insertAt(arr, r, el) {
    let rest = Math.round(arr.reduce((s, x) => s + x.textContent.length, 0) * r);
    for (let i = 0; i < arr.length; i++) {
      const x = arr[i];
      const len = x.textContent.length;
      if (rest > len) { rest -= len; continue; }
      if (x.nodeType !== 3 || rest <= 0 || rest >= len) { arr.splice(rest <= 0 ? i : i + 1, 0, el); return; }
      // 単語の途中で分けないよう、最寄りの空白・句読点の直後で分ける (無ければ末尾)
      const t = x.nodeValue;
      let k = t.length;
      for (const m of t.matchAll(/[\s、。，．,.;:!?！？]/g)) if (Math.abs(m.index + 1 - rest) < Math.abs(k - rest)) k = m.index + 1;
      const right = /\s/.test(t[k - 1] ?? "") ? " " + t.slice(k) : t.slice(k); // 空白で分けたら後ろにも空白
      arr.splice(i, 1, ...[document.createTextNode(t.slice(0, k)), el, document.createTextNode(right)].filter((n) => n.nodeType !== 3 || n.nodeValue));
      return;
    }
    arr.push(el);
  }

  // 訳文 (プレースホルダ付き) を検証しつつ子ノード列に組み立てる。構造が合わなければ null
  // 翻訳しない要素 (<n/>) は欠落・<n>…</n> 形でも許容 (欠落は元の位置へ補完、<n>…</n> の中身は捨てる)。翻訳する要素の欠落は不可
  function build(out, map, places) {
    const kids = new Map([[0, []]]); // id → 子ノード列 (0 = ブロック本体)
    const stack = [0];
    const used = new Set();
    const re = /<(\/?)(\d+)(\/?)>/g;
    let last = 0;
    const addText = (t) => { if (t) kids.get(stack.at(-1)).push(document.createTextNode(t)); };
    for (let m = re.exec(out); m; m = re.exec(out)) {
      addText(out.slice(last, m.index));
      last = re.lastIndex;
      const closing = m[1] === "/", self = m[3] === "/", n = Number(m[2]);
      if (closing) {
        if (self || stack.at(-1) !== n) return null;
        stack.pop();
      } else {
        const info = map.get(n);
        if (!info || used.has(n) || (self && !info.atomic)) return null;
        used.add(n);
        kids.get(stack.at(-1)).push(info.el);
        if (!self) { kids.set(n, []); stack.push(n); }
      }
    }
    addText(out.slice(last));
    if (stack.length !== 1) return null;
    for (const [n, info] of map) if (!info.atomic && !used.has(n)) return null;
    const starts = new Map(); // 親ごとの先頭挿入数 (先頭の要素の順序を保つ)
    for (const p of places) {
      if (p.id !== undefined && used.has(p.id)) continue;
      if (p.id !== undefined) console.debug("[Nano Translator] 欠落した要素を補完", p.el);
      const arr = kids.get(p.parent);
      if (p.pos === "start") { const s = starts.get(arr) ?? 0; arr.splice(s, 0, p.el); starts.set(arr, s + 1); continue; }
      if (p.pos === "end") { arr.push(p.el); continue; }
      const i = p.pos.after === undefined ? -1 : arr.indexOf(map.get(p.pos.after).el);
      if (i >= 0) arr.splice(i + 1, 0, p.el);
      else insertAt(arr, p.len ? p.at / p.len : 0, p.el);
    }
    return kids;
  }

  // デバッグ表示用: 翻訳単位ごとの記録 (ブロック要素 / テキストノード → 記録)
  //   ブロック: { mode: "block", input, marks: [{ id, tag, atomic, text }], ghosts, out, cached, result: "ok" | "long" | "mismatch", ms, nodes? }
  //   テキストノード: { mode: "node", input, out, cached, ms }
  const unitLog = new WeakMap();
  const markList = (map) => [...map].map(([id, i]) => ({ id, tag: describe(i.el), atomic: i.atomic, text: i.el.textContent.replace(/\s+/g, " ").trim().slice(0, 60) }));

  // ブロック要素 1 つを翻訳して置換する。成功なら true、失敗 (構造不一致・長すぎ) なら false
  async function translateBlock(a, root) {
    const { text, map, places } = serialize(root);
    if (!HAS_LETTER.test(text)) return true;
    const log = { mode: "block", input: text, marks: markList(map), ghosts: places.filter((p) => p.id === undefined).length, out: null, cached: false, result: null, ms: 0 };
    unitLog.set(root, log);
    if (text.length > maxUnit) { log.result = "long"; console.debug("[Nano Translator] 長すぎるためノード単位に切替", text.length); return false; }
    let out = await cache.get(a.target, text);
    if (out !== null && applyBlock(root, map, places, out)) { log.out = out; log.cached = true; log.result = "ok"; return true; }
    const t0 = performance.now();
    out = clean(await translateOne(a, text, root)).trim();
    log.ms = Math.round(performance.now() - t0);
    log.out = out;
    if (a.ctrl.signal.aborted) return true;
    if (!applyBlock(root, map, places, out)) { log.result = "mismatch"; console.debug("[Nano Translator] 構造不一致のためノード単位に切替", { text, out }); return false; }
    log.result = "ok";
    await cache.set(a.target, text, out);
    return true;
  }

  const unitEntry = new WeakMap(); // 翻訳単位 → 表示切替 { show, hide } (デバッグの辞書編集で差し替え)

  // 翻訳単位の訳文を out に差し替える (辞書編集用)。適用済みなら原文に戻してから適用。目印が合わなければ元に戻して false
  function reapply(unit, out) {
    const old = unitEntry.get(unit);
    if (old) { old.hide(); entries.splice(entries.indexOf(old), 1); unitEntry.delete(unit); }
    // テキストノード単位で翻訳済みだったブロックは、各ノードも原文に戻す
    for (const tn of unitLog.get(unit)?.nodes ?? []) {
      const e = unitEntry.get(tn);
      if (e) { e.hide(); entries.splice(entries.indexOf(e), 1); unitEntry.delete(tn); }
      processed.delete(tn);
      failed.delete(tn);
    }
    failed.delete(unit);
    if (unit.nodeType === 3) { applyNode(unit, unit.nodeValue, out); flush(); return true; }
    const s = serialize(unit);
    if (applyBlock(unit, s.map, s.places, out)) return true;
    if (old) { old.show(); entries.push(old); unitEntry.set(unit, old); flush(); }
    return false;
  }

  // 訳文 out (目印付き) を検証してブロックに適用する。構造が合わなければ何もせず false
  function applyBlock(root, map, places, out) {
    const kids = build(out, map, places);
    if (!kids) return false;
    const recs = [[root, [...root.childNodes], kids.get(0)]];
    for (const [n, info] of map) if (!info.atomic) recs.push([info.el, [...info.el.childNodes], kids.get(n)]);
    for (const [, , trans] of recs) for (const t of trans) if (t.nodeType === 3) processed.add(t);
    const entry = {
      show() { for (const [el, , trans] of recs) el.replaceChildren(...trans); },
      hide() { for (const [el, orig] of recs) el.replaceChildren(...orig); },
    };
    entry.show();
    flush();
    entries.push(entry);
    unitEntry.set(root, entry);
    return true;
  }

  // テキストノード 1 つを翻訳して置換する (ブロックが入れ子の場合・フォールバック)
  async function translateNode(a, node) {
    const raw = node.nodeValue;
    const text = raw.trim();
    const log = { mode: "node", input: text, out: null, cached: false, ms: 0 };
    unitLog.set(node, log);
    let out = await cache.get(a.target, text);
    log.cached = out !== null;
    if (out === null) {
      const t0 = performance.now();
      out = await translateOne(a, text, node);
      log.ms = Math.round(performance.now() - t0);
      await cache.set(a.target, text, out);
    }
    log.out = out;
    if (a.ctrl.signal.aborted) return; // 中断したノードは未処理のまま残し、再開時に翻訳し直す
    applyNode(node, raw, out);
  }

  // 訳文をテキストノードに適用 (前後の空白は原文のまま)。訳文が空なら失敗として記録
  function applyNode(node, raw, out) {
    processed.add(node);
    if (out && node.isConnected) {
      const value = raw.match(/^\s*/)[0] + out + raw.match(/\s*$/)[0];
      const entry = { show() { if (node.isConnected) node.nodeValue = value; }, hide() { if (node.isConnected) node.nodeValue = raw; } };
      entries.push(entry);
      unitEntry.set(node, entry);
      node.nodeValue = value;
      flush();
    } else failed.add(node);
  }

  // 複数の翻訳単位を JSON 文字列配列にまとめて 1 回で AI 翻訳する (原文の合計 maxUnit 文字まで)。処理した単位数を返す
  //   辞書にある単位はその場で適用。まとめ翻訳の応答が解釈できない・目印が合わない単位は、1 つずつ翻訳し直す
  async function translateBatch(a, units) {
    const items = [];
    let size = 0, done = 0;
    for (const u of units) {
      if (a.ctrl.signal.aborted) return done;
      let it;
      if (u.nodeType === 3) {
        const text = u.nodeValue.trim();
        it = { unit: u, text, raw: u.nodeValue, log: { mode: "node", input: text, out: null, cached: false, ms: 0 } };
      } else {
        const s = serialize(u);
        if (!HAS_LETTER.test(s.text) || s.text.length > maxUnit) { // 長すぎるブロックはまとめず単独で (ノード単位に分割)
          if (items.length) break;
          await translateUnit(a, u);
          return done + 1;
        }
        it = { unit: u, text: s.text, s, log: { mode: "block", input: s.text, marks: markList(s.map), ghosts: s.places.filter((p) => p.id === undefined).length, out: null, cached: false, result: null, ms: 0 } };
      }
      const hit = await cache.get(a.target, it.text);
      if (hit !== null && applyItem(it, hit)) {
        Object.assign(it.log, { out: hit, cached: true, result: it.s ? "ok" : undefined });
        unitLog.set(u, it.log);
        done++;
        continue;
      }
      if (items.length && size + it.text.length > maxUnit) break;
      items.push(it);
      size += it.text.length;
    }
    if (!items.length || a.ctrl.signal.aborted) return done;
    if (items.length === 1) { await translateUnit(a, items[0].unit); return done + 1; }

    const t0 = performance.now();
    const outs = await translateMany(a, items.map((it) => it.text), items.map((it) => it.unit));
    const ms = Math.round(performance.now() - t0);
    if (a.ctrl.signal.aborted) return done;
    for (const [i, it] of items.entries()) {
      const out = outs ? clean(outs[i]).trim() : null;
      if (out && applyItem(it, out)) {
        Object.assign(it.log, { out, ms, batch: items.length, result: it.s ? "ok" : undefined });
        unitLog.set(it.unit, it.log);
        await cache.set(a.target, it.text, out);
      } else {
        console.debug("[Nano Translator] まとめ翻訳の結果を使えないため単独で翻訳", { text: it.text, out });
        await translateUnit(a, it.unit);
        const l = unitLog.get(it.unit);
        if (l) l.batchFailed = { n: items.length, out };
      }
      done++;
    }
    return done;
  }
  const applyItem = (it, out) => (it.s ? applyBlock(it.unit, it.s.map, it.s.places, out) : (applyNode(it.unit, it.raw, out), true));

  // まとめ翻訳 (JSON 文字列配列)。Prompt API の responseConstraint で配列の形を指定。使えない応答・エラーは null
  async function translateMany(a, texts, units) {
    if (!("LanguageModel" in self)) return null; // offscreen への中継はまとめ翻訳に未対応 (単位ごとに翻訳)
    const input = wrapBatch(texts, a.target);
    live.begin(units, input);
    try {
      const raw = await promptRaw(a, input, { responseConstraint: { type: "array", items: { type: "string" }, minItems: texts.length, maxItems: texts.length } });
      live.end(raw);
      return parseBatch(raw, texts.length);
    } catch (e) {
      live.end(null, e);
      if (e.name === "AbortError") throw e;
      console.debug("[Nano Translator] まとめ翻訳に失敗", e);
      return null;
    }
  }

  async function translateUnit(a, u) {
    if (u.nodeType === 3) return translateNode(a, u);
    if (await translateBlock(a, u)) return;
    // フォールバック: ブロック内の未処理テキストノードを 1 つずつ翻訳
    const walker = document.createTreeWalker(u, NodeFilter.SHOW_TEXT);
    const nodes = [];
    for (let n = walker.nextNode(); n; n = walker.nextNode()) {
      const p = n.parentElement;
      if (processed.has(n) || isSkipped(p)) continue;
      if (HAS_LETTER.test(n.nodeValue)) nodes.push(n); else processed.add(n);
    }
    const log = unitLog.get(u);
    if (log) log.nodes = nodes;
    for (const n of nodes) { if (a.ctrl.signal.aborted) return; await translateNode(a, n); }
  }

  // unit: 翻訳中の単位 (ブロック要素 / テキストノード)。デバッグ時のリアルタイム表示用
  async function translateOne(a, text, unit) {
    live.begin(unit, text);
    try {
      const out = await translateRaw(a, text);
      live.end(out);
      return out;
    } catch (e) { live.end(null, e); throw e; }
  }
  async function translateRaw(a, text) {
    if ("LanguageModel" in self) return clean(await promptRaw(a, wrap(text, a.target))).trim();
    // HTTP ページ等 (非セキュアコンテキスト) では Prompt API が無いため、拡張の offscreen document に中継する
    if (!a.relayReady) {
      const r = await chrome.runtime.sendMessage({ type: "ensure-offscreen" });
      if (!r?.ok) throw new Error(`拡張側で翻訳を準備できません (${r?.error ?? "応答なし"})`);
      a.relayReady = true;
    }
    live.update(null, "offscreen document に中継中 (完了まで出力は表示されない)");
    const r = await chrome.runtime.sendMessage({ type: "nano-translate", target: a.target, text });
    if (!r?.ok) throw new Error(r?.error ?? "応答なし");
    return r.text;
  }

  // Prompt API に input を渡して応答 (生) を返す。opts: prompt() の追加オプション (responseConstraint 等)
  async function promptRaw(a, input, opts = {}) {
    const { signal } = a.ctrl;
    // セッションは辞書にない原文が出た時点で初めて作り、翻訳モード中は使い回す
    a.base ??= await createSession(a.target, {
      signal,
      monitor(m) { m.addEventListener("downloadprogress", (e) => setLabel(a, e.loaded < 1 ? `モデル取得中 ${Math.floor(e.loaded * 100)}%` : "準備中…")); },
    });
    const clone = await a.base.clone({ signal });
    try {
      if (!debugCode) return await clone.prompt(input, { ...opts, signal });
      // デバッグ時はストリーミングで受け取り、生成中の訳文を表示する (チャンクは差分。累積で返す版にも対応)
      let acc = "";
      for await (const chunk of clone.promptStreaming(input, { ...opts, signal })) {
        acc = chunk.startsWith(acc) && acc ? chunk : acc + chunk;
        live.update(acc);
      }
      return acc;
    } finally { clone.destroy(); }
  }

  // デバッグ: AI で翻訳中の単位をリアルタイム表示 (右下のパネル + 翻訳中の部分を薄い緑でハイライト)。設定 debugCode
  const live = (() => {
    let host = null, els = null, t0 = 0, timer = 0;
    function ensure() {
      if (host) return;
      host = document.createElement("div");
      host.style.cssText = "all:initial;position:fixed;right:8px;bottom:48px;z-index:2147483646";
      const root = host.attachShadow({ mode: "open" });
      root.innerHTML = `<style>
        .p{box-sizing:border-box;width:min(440px,calc(100vw - 16px));max-height:45vh;overflow:auto;padding:8px 10px;border-radius:8px;background:#202124ee;color:#e8eaed;font:12px/1.45 system-ui,"Yu Gothic UI",sans-serif;box-shadow:0 4px 16px #0008}
        .h{display:flex;justify-content:space-between;gap:8px;font-weight:bold;margin-bottom:4px}
        .s{font-weight:normal;color:#fdd663}.s.done{color:#81c995}.s.err{color:#f28b82}
        .l{color:#8ab4f8;margin:6px 0 2px}.m{color:#9aa0a6}
        pre{margin:0;padding:4px 6px;background:#303134;border-radius:4px;white-space:pre-wrap;word-break:break-word;font:11px/1.4 ui-monospace,Consolas,monospace;max-height:14em;overflow:auto}
      </style><div class="p"><div class="h"><span>AI 翻訳 (デバッグ)</span><span class="s"></span></div>
      <div class="m u"></div><div class="l">入力</div><pre class="i"></pre><div class="l">出力 (生成中)</div><pre class="o"></pre></div>`;
      els = { s: root.querySelector(".s"), u: root.querySelector(".u"), i: root.querySelector(".i"), o: root.querySelector(".o") };
      document.documentElement.append(host);
    }
    const elapsed = () => `${((performance.now() - t0) / 1000).toFixed(1)} 秒`;
    return {
      begin(unit, text) {
        if (!debugCode) return;
        ensure();
        t0 = performance.now();
        const one = (u) => (u.nodeType === 1 ? `ブロック要素 <${describe(u)}>` : `テキストノード (親 <${describe(u.parentElement)}>)`);
        const list = [].concat(unit ?? []);
        els.u.textContent = list.length > 1 ? `まとめ翻訳 ${list.length} 件: ${list.map(one).join(", ")}` : list.length ? one(list[0]) : "";
        els.i.textContent = text;
        els.o.textContent = "";
        els.s.className = "s";
        clearInterval(timer);
        timer = setInterval(() => { els.s.textContent = `翻訳中 ${elapsed()}`; }, 100);
        els.s.textContent = "翻訳中";
        if (list.length && globalThis.Highlight && CSS.highlights) {
          const h = new Highlight();
          for (const u of list) { const r = new Range(); r.selectNodeContents(u); h.add(r); }
          hlStyle();
          CSS.highlights.set(HL_LIVE, h);
        }
      },
      update(out, note) {
        if (!host || !debugCode) return;
        els.o.textContent = out ?? note ?? "";
        els.o.scrollTop = els.o.scrollHeight;
      },
      end(out, err) {
        if (!host) return;
        clearInterval(timer);
        els.s.textContent = err ? `エラー ${elapsed()}` : `完了 ${elapsed()}`;
        els.s.className = err ? "s err" : "s done";
        if (err) els.o.textContent = String(err?.message ?? err);
        else if (out != null) els.o.textContent = out;
        CSS.highlights?.delete(HL_LIVE);
      },
      hide() { clearInterval(timer); host?.remove(); host = null; CSS.highlights?.delete(HL_LIVE); },
    };
  })();

  // 表示領域の未処理ノードを、まとめ翻訳 1 回分ずつ翻訳。毎回見え方を取り直すので、途中でスクロールしても今見えている部分を優先する
  async function pump() {
    const a = act;
    if (state !== "active" || !a || a.busy) return;
    a.busy = true;
    updateButtons();
    const { signal } = a.ctrl;
    try {
      a.target ??= await getTarget();
      maxUnit = (await limits()).maxUnit;
      const o = await chrome.storage.local.get(["skipCode", "markUntranslated", "markCode", "debugCode"]);
      skipCode = o.skipCode !== false;
      markUntranslated = o.markUntranslated !== false;
      markCode = o.markCode === true;
      debugCode = o.debugCode === true;
      setCodeParams(await codeParams());
      let done = 0;
      for (let nodes = collectVisible(); nodes.length && !signal.aborted; nodes = collectVisible()) {
        setLabel(a, `翻訳中 ${done}/${done + nodes.pending}`);
        done += await translateBatch(a, nodes);
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

  // デバッグ: 翻訳モード中、テキストにマウスを合わせると、その翻訳ブロックの詳細をダイアログ表示
  // (翻訳単位・状態・AI への入力と出力・目印の対応・コード判定の根拠とパラメータ)。設定 debugCode (既定 OFF)
  // ダイアログは documentElement 直下の Shadow DOM (MutationObserver の対象外)
  let debugCode = false;
  let codeNodes = new Set(); // 直近の collectVisible でコードと判定したテキストノード
  let tip = null, tipFor = null, tipNode = null, tipTimer = 0; // tipFor: 表示中の翻訳ブロック、tipNode: マウス下のテキストノード
  function hideTip() { tip?.remove(); tip = null; tipFor = null; tipNode = null; }
  // テキストノードの翻訳単位 (collectVisible と同じ規則): 最寄りのブロック要素。入れ子のブロックを含む / body ならテキストノード
  function unitOf(n) {
    let el = n.parentElement;
    while (el && el !== document.body && getComputedStyle(el).display.startsWith("inline")) el = el.parentElement;
    return !el || el === document.body || el.querySelector(BLOCK_SEL) ? n : el;
  }
  const hoverable = (n) => n?.nodeType === 3 && n.nodeValue.trim() && n.parentElement && document.body.contains(n)
    && !SKIP_TAGS.has(n.parentElement.tagName.toUpperCase()) && !n.parentElement.isContentEditable;
  function onHover(e) {
    if (!debugCode || state === "idle") { if (tip) hideTip(); return; }
    if (tip && e.composedPath().includes(tip)) { clearTimeout(tipTimer); return; } // ダイアログ上は維持 (スクロール・コピー用)
    clearTimeout(tipTimer);
    const { clientX: x, clientY: y } = e;
    tipTimer = setTimeout(() => {
      const pos = document.caretPositionFromPoint?.(x, y);
      const n = pos ? pos.offsetNode : document.caretRangeFromPoint?.(x, y)?.startContainer;
      const r = document.createRange();
      if (n?.nodeType === 3) r.selectNodeContents(n);
      const over = hoverable(n) && [...r.getClientRects()].some((b) => x >= b.left && x <= b.right && y >= b.top && y <= b.bottom);
      if (!over) { hideTip(); return; }
      if (tipFor !== unitOf(n)) showTip(n, x, y);
    }, 150);
  }
  const describe = (el) => el.tagName.toLowerCase() + (el.id ? "#" + el.id : "") + [...el.classList].slice(0, 4).map((c) => "." + c).join("");
  function showTip(n, x, y, keep) {
    hideTip();
    const el = n.parentElement;
    const unit = unitOf(n);
    tipFor = unit;
    tipNode = n;
    const c = el.closest(CODE_SEL);
    const b = blockOf(el);
    const ff = getComputedStyle(b).fontFamily;
    const r = NanoPrompt.codeScore(b.textContent || "", re.mono.test(ff), cp, re);
    tip = document.createElement("div");
    tip.style.cssText = "all:initial;position:fixed;z-index:2147483647;left:0;top:0";
    const root = tip.attachShadow({ mode: "open" });
    root.innerHTML = `<style>
      .d{box-sizing:border-box;width:min(560px,calc(100vw - 16px));max-height:min(70vh,640px);overflow:auto;padding:10px 12px;border-radius:8px;background:#202124;color:#e8eaed;font:12px/1.45 system-ui,"Yu Gothic UI",sans-serif;box-shadow:0 4px 16px #0008}
      h1{font-size:13px;margin:0 0 6px;display:flex;justify-content:space-between;gap:8px}
      h2{font-size:12px;margin:10px 0 4px;color:#8ab4f8}
      .x{all:unset;cursor:pointer;padding:0 4px}
      table{border-collapse:collapse;width:100%}
      td,th{border-bottom:1px solid #3c4043;padding:2px 4px;text-align:left;vertical-align:top}
      th{color:#9aa0a6;font-weight:normal}
      tr.ok td{color:#fdd663}
      .v{font-weight:bold}.code{color:#8ab4f8}
      .m{color:#9aa0a6}
      code,pre{font:11px/1.4 ui-monospace,Consolas,monospace}
      pre{white-space:pre-wrap;word-break:break-all;margin:0;padding:4px 6px;background:#303134;border-radius:4px;max-height:8em;overflow:auto}
      input,textarea{box-sizing:border-box;font:12px ui-monospace,Consolas,monospace;color:#e8eaed;background:#303134;border:1px solid #5f6368;border-radius:4px;padding:1px 4px}
      input{width:80px}textarea{width:100%;resize:vertical}
      tr.chg td:first-child{color:#8ab4f8}tr.chg input,tr.chg textarea{border-color:#8ab4f8}
      .ok2{color:#81c995}.ng{color:#f28b82}.wait{color:#fdd663}
      .btn{margin-top:6px;font:inherit;color:#e8eaed;background:#303134;border:1px solid #5f6368;border-radius:4px;padding:2px 10px;cursor:pointer}
    </style><div class="d"></div>`;
    const d = root.querySelector(".d");
    const add = (tag, text, cls) => { const e = document.createElement(tag); if (text !== undefined) e.textContent = text; if (cls) e.className = cls; d.append(e); return e; };
    const h = add("h1");
    h.append("翻訳ブロック詳細 (デバッグ)");
    const close = document.createElement("button");
    close.className = "x";
    close.textContent = "✕";
    close.onclick = hideTip;
    h.append(close);

    // ---- 翻訳ブロック
    const isUnitBlock = unit.nodeType === 1;
    const blog = isUnitBlock ? unitLog.get(unit) : null;
    const nlog = unitLog.get(n);
    // マウス下のテキストと、その翻訳単位 (ブロック) は別々に判定する。
    // ブロックは翻訳対象でも、中の <code> 等はタグ・クラスでコード (点数に関係なく) → 目印 <n/> で渡し原文のまま
    const elCode = skipCode && isCode(el);
    const code = isUnitBlock ? skipCode && isCode(unit) : elCode; // 翻訳単位がコードで対象外か
    add("h2", "マウス下のテキスト");
    add("pre", n.nodeValue.trim().slice(0, 300));
    const how = c ? `タグ・クラス (${CODE_SEL.split(",").filter((s) => c.matches(s)).join(", ")}) に一致。点数に関係なくコード` : `文脈からの推定 (${r.score} 点 ≥ しきい値 ${cp.threshold})`;
    add("div", `要素 <${describe(el)}>: ${elCode ? "コード → " + how : "翻訳対象"}`, elCode ? "code" : "");
    if (elCode && !code) add("div", "ブロック内の翻訳しない要素: AI へは目印 <n/> で渡し、原文のまま残す (青のハイライトはこの要素だけ)", "m");

    add("h2", "翻訳単位");
    if (isUnitBlock) add("div", `ブロック要素 <${describe(unit)}> (中のテキストとインライン要素をまとめて 1 回で翻訳)`);
    else add("div", `テキストノード 1 つ (親 <${describe(el)}>。最寄りのブロックが入れ子のブロックを含む、または body のため)`);
    const st = add("div", "", "v");
    if (code) { st.textContent = "状態: 対象外 (コードと判定。下のコード判定を参照)"; st.className = "v code"; }
    else if (blog?.result === "ok") { st.textContent = `状態: 翻訳済み (ブロック単位、${blog.cached ? "辞書から" : `${blog.batch ? `まとめ翻訳 ${blog.batch} 件で ` : ""}AI ${blog.ms} ms`})`; st.className = "v ok2"; }
    else if (blog?.result === "long") { st.textContent = `状態: ブロック単位は最大文字数超 (${blog.input.length} > ${maxUnit} 文字) → テキストノード単位で翻訳`; st.className = "v wait"; }
    else if (blog?.result === "mismatch") { st.textContent = "状態: ブロック単位は AI 出力の目印が不一致 → テキストノード単位で翻訳"; st.className = "v wait"; }
    else if (failed.has(n)) { st.textContent = "状態: 失敗 (AI の出力が空)"; st.className = "v ng"; }
    else if (nlog?.out) { st.textContent = `状態: 翻訳済み (テキストノード単位、${nlog.cached ? "辞書から" : `${nlog.batch ? `まとめ翻訳 ${nlog.batch} 件で ` : ""}AI ${nlog.ms} ms`})`; st.className = "v ok2"; }
    else if (processed.has(n)) { st.textContent = "状態: 翻訳済み (訳文のテキスト)"; st.className = "v ok2"; }
    else { st.textContent = "状態: 未翻訳 (処理待ち。表示中の部分から順に翻訳)"; st.className = "v wait"; }

    // 原文と訳文 (目印を外したプレーンテキスト。翻訳しない要素 <n/> は原文のテキストに戻す)
    {
      const norm = (t) => (t || "").replace(/\s+/g, " ").trim();
      const plain = (t, marks) => norm(t.replace(/<(\d+)\/>/g, (_, id) => marks?.find((m) => m.id === Number(id))?.text ?? "").replace(/<\/?\d+>/g, ""));
      let src, dst;
      if (isUnitBlock) {
        src = blog ? plain(blog.input, blog.marks) : norm(unit.textContent);
        dst = code ? "(翻訳対象外)"
          : blog?.result === "ok" ? plain(blog.out, blog.marks)
          : blog?.nodes ? `${norm(unit.textContent)}  ※テキストノード単位の訳を含む現在の表示`
          : "(未翻訳)";
      } else {
        src = nlog?.input ?? norm(n.nodeValue);
        dst = elCode ? "(翻訳対象外)" : nlog?.out ? nlog.out : failed.has(n) ? "(失敗: AI の出力が空)" : "(未翻訳)";
      }
      add("h2", "原文");
      add("pre", src.slice(0, 1000));
      add("h2", "訳文");
      add("pre", dst.slice(0, 1000));
    }

    if (isUnitBlock && !code) {
      let input = blog?.input, marks = blog?.marks, ghosts = blog?.ghosts;
      if (!blog) { const s = serialize(unit); input = s.text; marks = markList(s.map); ghosts = s.places.filter((p) => p.id === undefined).length; }
      add("h2", `AI への入力 (目印付きテキスト、${input.length} 文字 / 最大 ${maxUnit})${blog ? "" : " ※未翻訳のため現在の DOM から生成"}`);
      if (blog?.batchFailed) add("div", `まとめ翻訳 (${blog.batchFailed.n} 件) の結果を使えず単独で翻訳し直した (まとめ翻訳での出力: ${blog.batchFailed.out ?? "応答を解釈できず"})`, "m");
      add("pre", input);
      if (marks.length) {
        add("h2", "目印の対応");
        const t = add("table");
        t.innerHTML = "<tr><th>目印</th><th>要素</th><th>扱い</th><th>原文</th></tr>";
        for (const m of marks) {
          const tr = document.createElement("tr");
          for (const v of [m.atomic ? `<${m.id}/>` : `<${m.id}>…</${m.id}>`, `<${m.tag}>`, m.atomic ? "翻訳しない" : "中身を翻訳", m.text]) {
            const td = document.createElement("td");
            td.textContent = v;
            tr.append(td);
          }
          t.append(tr);
        }
      }
      if (ghosts) add("div", `アイコン等 (文字を含まない要素) ${ghosts} 個: AI に渡さず、元の位置に戻す`, "m");
      if (blog?.out != null) {
        add("h2", `AI の出力${blog.cached ? " (辞書)" : ""}`);
        add("pre", blog.out || "(空)");
      }
      if (blog?.nodes?.length) {
        add("h2", `テキストノード単位の翻訳 (${blog.nodes.length} 個)`);
        const t = add("table");
        t.innerHTML = "<tr><th>原文</th><th>訳文</th></tr>";
        for (const tn of blog.nodes) {
          const l = unitLog.get(tn);
          const tr = document.createElement("tr");
          for (const v of [l?.input ?? tn.nodeValue.trim(), l ? (l.out || "(空: 失敗)") : "(未処理)"]) {
            const td = document.createElement("td");
            td.textContent = v;
            tr.append(td);
          }
          t.append(tr);
        }
      }
    } else if (!code) {
      add("h2", "AI への入力");
      add("pre", nlog?.input ?? n.nodeValue.trim());
      if (nlog?.out != null) { add("h2", `AI の出力${nlog.cached ? " (辞書)" : ""}`); add("pre", nlog.out || "(空)"); }
    }

    // ---- 辞書編集 (この翻訳単位の原文 = AI への入力 をキーにした訳文)
    if (!code) {
      const key = isUnitBlock ? (blog?.input ?? serialize(unit).text) : (nlog?.input ?? n.nodeValue.trim());
      add("h2", "辞書 (この翻訳単位の訳文を編集)");
      add("div", isUnitBlock ? "目印 <n>…</n> / <n/> は原文と同じ番号をすべて残す。保存するとページにも適用" : "保存するとページにも適用", "m");
      const ta = add("textarea");
      ta.rows = 4;
      ta.placeholder = "(辞書に未登録)";
      for (const t of ["keydown", "keyup", "keypress"]) ta.addEventListener(t, (e) => e.stopPropagation()); // ページのショートカットに渡さない
      const bar = add("div");
      const msg = document.createElement("span");
      msg.className = "m";
      const btn = (label, fn) => { const b = document.createElement("button"); b.className = "btn"; b.textContent = label; b.onclick = fn; bar.append(b, " "); return b; };
      const say = (t, cls = "m") => { msg.textContent = t; msg.className = cls; };
      let target = act?.target;
      (async () => {
        target ??= await getTarget();
        const v = await cache.get(target, key);
        ta.value = v ?? "";
        say(v === null ? "辞書に未登録" : "辞書の訳文");
      })();
      btn("保存して適用", async () => {
        const out = ta.value.trim();
        if (!out) { say("訳文が空", "ng"); return; }
        if (!reapply(unit, out)) { say("目印が原文と合わないため適用できない (保存もしない)", "ng"); return; }
        await cache.set(target, key, out);
        const l = unitLog.get(unit);
        if (l) Object.assign(l, { out, cached: true, result: l.mode === "block" ? "ok" : l.result });
        else unitLog.set(unit, { mode: isUnitBlock ? "block" : "node", input: key, out, cached: true, result: "ok", ms: 0, marks: [], ghosts: 0 });
        say("保存して適用しました", "ok2");
      });
      btn("辞書から削除", async () => {
        await cache.remove(target, key);
        ta.value = "";
        say("辞書から削除しました (ページの表示はそのまま。次回の翻訳で AI が翻訳し直す)");
      });
      bar.append(msg);
    }

    // ---- コード判定
    add("h2", `コード判定 (マウス下のテキスト): ${c || r.code ? "コード (翻訳しない)" : "文章 (翻訳する)"}`);
    add("div", "1. のタグ・クラスに一致すれば常にコード。一致しなければ 2. の点数で判定", "m");
    if (!skipCode) add("div", "「ソースコードは翻訳しない」が OFF のため、この判定は使われない (すべて翻訳)", "m");
    const tw = document.createTreeWalker(b, NodeFilter.SHOW_TEXT);
    for (let t = tw.nextNode(); t; t = tw.nextNode()) {
      if (processed.has(t) && !failed.has(t)) { add("div", "翻訳済みのブロック: 採点は現在の (訳文の) テキストによる", "m"); break; }
    }
    add("h2", `1. タグ・クラス (マウス下の要素 <${describe(el)}> と祖先)`);
    if (c) {
      add("div", `該当: <${describe(c)}>`);
      add("div", `一致したセレクタ: ${CODE_SEL.split(",").filter((s) => c.matches(s)).join(", ")}`);
      add("div", "タグ・クラスに該当するため、推定 (2.) の結果に関係なくコード", "m");
    } else add("div", "該当なし (2. の推定で判定)", "m");

    add("h2", `2. 文脈からの推定${c ? " (参考)" : ""}`);
    add("div", `対象ブロック: <${describe(b)}>`);
    add("div", `font-family: ${ff}`, "m");
    add("div", `合計 ${r.score} 点 / しきい値 ${cp.threshold} → ${r.code ? "コード" : "文章"}`, "v");
    if (r.stats) {
      const t = add("table");
      t.innerHTML = "<tr><th>条件</th><th>値 (基準)</th><th>該当</th><th>点</th></tr>";
      for (const k of r.checks) {
        const tr = document.createElement("tr");
        if (k.ok) tr.className = "ok";
        for (const v of [k.name, k.detail, k.ok ? "○" : "－", k.ok ? (k.score > 0 ? "+" : "") + k.score : `(${k.score})`]) {
          const td = document.createElement("td");
          td.textContent = v;
          tr.append(td);
        }
        t.append(tr);
      }
      const s = r.stats;
      add("div", `集計: ${s.lines} 行 / 空白を除き ${s.chars} 文字 / 記号 ${s.symbolList.length} 個 / キーワード ${s.kws.length} 個 / 行末が記号 ${s.ends} 行 / 字下げ ${s.indented} 行 / 文末記号+空白 ${s.prose} 個`, "m");
      if (s.kws.length) add("div", `キーワード: ${[...new Set(s.kws)].slice(0, 30).join(", ")}`, "m");
    } else add("div", "空白を除き 3 文字未満のため採点しない", "m");

    // パラメータはここで編集可。変更すると保存 (設定ページと共通の codeParams) して、この要素を再判定・ページを再走査
    add("h2", "3. パラメータ (編集すると保存して再判定)");
    const pt = add("table");
    pt.innerHTML = "<tr><th>パラメータ</th><th>値</th><th>既定</th></tr>";
    for (const [k, v] of Object.entries(cp)) {
      const def = NanoPrompt.CODE_DEFAULTS[k];
      const tr = document.createElement("tr");
      if (v !== def) tr.className = "chg";
      const a = document.createElement("td"), bb = document.createElement("td"), dd = document.createElement("td");
      a.textContent = PARAM_LABELS[k] ?? k;
      a.title = k;
      const input = document.createElement(typeof def === "number" ? "input" : "textarea");
      if (typeof def === "number") { input.type = "number"; input.step = Number.isInteger(def) ? "1" : "0.01"; } else input.rows = 2;
      input.value = v;
      input.addEventListener("change", () => applyParam(k, input.value));
      bb.append(input);
      dd.textContent = typeof def === "number" ? def : "";
      dd.className = "m";
      tr.append(a, bb, dd);
      pt.append(tr);
    }
    const reset = add("button", "すべて既定値に戻す", "btn");
    reset.onclick = () => applyParam(null);

    document.documentElement.append(tip);
    if (keep) {
      tip.style.left = keep.left;
      tip.style.top = keep.top;
      d.scrollTop = keep.scroll;
      return;
    }
    const w = d.offsetWidth, hh = d.offsetHeight;
    tip.style.left = Math.max(8, Math.min(x + 12, innerWidth - w - 8)) + "px";
    tip.style.top = Math.max(8, y + 16 + hh > innerHeight ? y - hh - 8 : y + 16) + "px";
  }
  const PARAM_LABELS = {
    threshold: "しきい値", monoScore: "等幅フォント: 点", monoLinesScore: "等幅 + 3 行以上: 点",
    monoSyntaxScore: "等幅 + 記号: 点", monoSymbolRatio: "等幅 + 記号: 記号率", symbolScore: "記号が多い: 点", symbolRatio: "記号が多い: 記号率",
    keywordScore: "キーワードが多い: 点", keywordCount: "キーワードが多い: 個数", lineEndScore: "行末が記号: 点", lineEndRatio: "行末が記号: 割合",
    indentScore: "字下げ: 点", indentRatio: "字下げ: 割合", proseScore: "文章らしい: 点", proseCount: "文章らしい: 個数",
    fonts: "等幅フォント名", keywords: "キーワード",
  };
  // k = null で全既定値。既定値と異なるものだけ保存。空欄・不正値は既定値
  async function applyParam(k, raw) {
    const D = NanoPrompt.CODE_DEFAULTS;
    const p = k === null ? { ...D } : { ...cp };
    if (k !== null) {
      const v = typeof D[k] === "number" ? Number(raw) : raw.trim();
      p[k] = raw.trim() === "" || (typeof D[k] === "number" && !Number.isFinite(v)) ? D[k] : v;
    }
    const o = Object.fromEntries(Object.entries(p).filter(([kk, vv]) => vv !== D[kk]));
    try {
      if (Object.keys(o).length) await chrome.storage.local.set({ codeParams: o });
      else await chrome.storage.local.remove("codeParams");
    } catch { /* 保存できなくてもこのページでは反映 */ }
    setCodeParams(p);
    if (tip && tipNode) {
      const d = tip.shadowRoot.querySelector(".d");
      showTip(tipNode, 0, 0, { left: tip.style.left, top: tip.style.top, scroll: d.scrollTop });
    }
    if (state === "active") { if (act?.busy) collectVisible(); else pump(); } // ハイライトを更新。非コードになった部分は翻訳
  }
  document.addEventListener("mousemove", onHover, { passive: true });

  // 画面が更新されたときに再度未翻訳の部分を探すための検知 (翻訳モード中のみ)。
  //  - DOM の追加・テキスト変更 (SPA の遷移、遅延ロード、メニューやダイアログの描画)
  //  - 表示状態の切り替え (class / style / hidden / open / aria-* の変更。メニューの開閉など、新規テキストが無くても見えるようになる場合)
  //  - transitionend / animationend (開く・スライドインするアニメーションの完了)、toggle (details・popover)、hashchange / popstate
  // 自分の書き込みは flush() で捨てて、自己トリガーのループを避ける
  const EVENTS = ["transitionend", "animationend", "toggle"];
  const WIN_EVENTS = ["hashchange", "popstate"];
  let observer = null;
  const flush = () => observer?.takeRecords();
  function watchChanges() {
    observer = new MutationObserver(() => { clearTimeout(viewTimer); viewTimer = setTimeout(pump, 500); });
    observer.observe(document.body, {
      childList: true, subtree: true, characterData: true,
      attributes: true, attributeFilter: ["class", "style", "hidden", "open", "aria-expanded", "aria-hidden", "aria-selected", "data-state"],
    });
    for (const t of EVENTS) document.addEventListener(t, onViewChange, true);
    for (const t of WIN_EVENTS) addEventListener(t, onViewChange);
  }
  function unwatchChanges() {
    observer?.disconnect();
    observer = null;
    for (const t of EVENTS) document.removeEventListener(t, onViewChange, true);
    for (const t of WIN_EVENTS) removeEventListener(t, onViewChange);
  }

  function start() {
    act = { ctrl: new AbortController(), target: null, base: null, relayReady: false, busy: false };
    state = "active";
    for (const e of entries) e.show(); // 翻訳済み部分は即座に訳文へ戻す
    badge ??= createBadge();
    label.textContent = "";
    updateButtons();
    document.addEventListener("scroll", onViewChange, { capture: true, passive: true }); // 内側のスクロール領域も拾う
    addEventListener("resize", onViewChange);
    watchChanges();
    pump();
  }

  // 翻訳モードを終える (自動翻訳の停止・セッション破棄)
  function finish(next) {
    document.removeEventListener("scroll", onViewChange, { capture: true });
    removeEventListener("resize", onViewChange);
    unwatchChanges();
    clearTimeout(viewTimer);
    if (act) {
      act.ctrl.abort();
      act.base?.destroy();
      act = null;
    }
    state = next;
  }

  // 原文表示に切り替える。訳文は保持し、[翻訳] で即座に戻せる
  function restore() {
    finish("idle");
    unpaint();
    live.hide();
    codeNodes = new Set();
    hideTip();
    for (const e of entries) e.hide();
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
