// 設定ページ: 辞書 (翻訳キャッシュ) の一覧・編集・削除・全件クリア
const { LANGS, SUPPORTED, DEFAULTS, MINIMUMS, limits, browserLang, cache, CODE_DEFAULTS, codeParams, codeRegex, codeScore } = NanoPrompt;

const $ = (id) => document.getElementById(id);
const rowsEl = $("rows");
const filterEl = $("filter");
const langEl = $("lang");
const MAX_ROWS = 200;

let all = []; // [{ k, s, t, l, ts }] 参照日時の新しい順 (ts = 最終参照日時)

for (const [code, name] of Object.entries(LANGS)) langEl.append(new Option(`翻訳先: ${name}`, code));

let msgTimer;
function say(text) {
  $("msg").textContent = text;
  clearTimeout(msgTimer);
  msgTimer = setTimeout(() => { $("msg").textContent = ""; }, 3000);
}

// 翻訳先言語: 「ブラウザ設定に従う」(storage の target を持たない) か、言語を明示
const targetEl = $("target");
targetEl.append(new Option(`ブラウザ設定に従う (現在: ${LANGS[browserLang()]})`, "auto"));
for (const [code, name] of Object.entries(LANGS)) targetEl.append(new Option(name, code));
async function loadTarget() {
  const t = (await chrome.storage.local.get("target")).target;
  targetEl.value = SUPPORTED.includes(t) ? t : "auto";
}
targetEl.addEventListener("change", async () => {
  if (targetEl.value === "auto") await chrome.storage.local.remove("target");
  else await chrome.storage.local.set({ target: targetEl.value });
  say("翻訳先を保存しました");
});
loadTarget();

// 数値設定 (キー名は prompt.js の DEFAULTS と同じ)。空欄・不正値は既定値に戻す
const numberInputs = { cacheMax: $("cache-max"), cacheMaxLen: $("cache-max-len"), maxUnit: $("max-unit") };
(async () => {
  const cur = await limits();
  for (const [k, el] of Object.entries(numberInputs)) { el.value = cur[k]; el.placeholder = DEFAULTS[k]; el.min = MINIMUMS[k]; }
})();
for (const [k, el] of Object.entries(numberInputs)) {
  el.addEventListener("change", async () => {
    const v = Math.floor(Number(el.value));
    if (el.value.trim() === "" || !Number.isFinite(v) || v < MINIMUMS[k]) {
      await chrome.storage.local.remove(k);
      el.value = DEFAULTS[k];
      say(`既定値 (${DEFAULTS[k]}) に戻しました`);
    } else {
      await chrome.storage.local.set({ [k]: v });
      say("保存しました");
    }
    if (k === "cacheMax") { await cache.prune(); await load(); }
  });
}

// チェックボックス設定 (既定 ON。キー未設定は ON 扱い): ソースコードは翻訳しない / 未翻訳・コードのハイライト
for (const [key, id, def = true] of [["skipCode", "skip-code"], ["markUntranslated", "mark-untranslated"], ["markCode", "mark-code", false], ["debugCode", "debug-code", false]]) {
  const el = $(id);
  (async () => { const v = (await chrome.storage.local.get(key))[key]; el.checked = v === undefined ? def : v; })();
  el.addEventListener("change", async () => {
    await chrome.storage.local.set({ [key]: el.checked });
    say("保存しました");
  });
}

// コード判定のパラメータ: data-key の入力欄。既定値と異なるものだけ codeParams に保存。空欄・不正値は既定値
const paramInputs = [...document.querySelectorAll("[data-key]")];
function readParams() {
  const p = { ...CODE_DEFAULTS };
  for (const el of paramInputs) {
    const k = el.dataset.key, def = CODE_DEFAULTS[k];
    const v = typeof def === "number" ? Number(el.value) : el.value.trim();
    if (el.value.trim() !== "" && (typeof def !== "number" || Number.isFinite(v))) p[k] = v;
  }
  return p;
}
async function loadParams() {
  const cur = await codeParams();
  for (const el of paramInputs) {
    const k = el.dataset.key;
    el.value = cur[k];
    el.placeholder = CODE_DEFAULTS[k];
    el.title = `既定値: ${CODE_DEFAULTS[k]}`;
    el.classList.toggle("changed", cur[k] !== CODE_DEFAULTS[k]);
  }
  tryScore();
}
async function saveParams() {
  const p = readParams();
  const o = Object.fromEntries(Object.entries(p).filter(([k, v]) => v !== CODE_DEFAULTS[k]));
  if (Object.keys(o).length) await chrome.storage.local.set({ codeParams: o });
  else await chrome.storage.local.remove("codeParams");
  await loadParams();
  say("保存しました");
}
for (const el of paramInputs) el.addEventListener("change", saveParams);
$("code-params-reset").addEventListener("click", async () => {
  await chrome.storage.local.remove("codeParams");
  await loadParams();
  say("既定値に戻しました");
});

// 試算: 入力テキストを今のパラメータ (未保存の入力を含む) で採点し、内訳を表示
function tryScore() {
  const out = $("try-result");
  const text = $("try-text").value;
  if (!text.trim()) { out.textContent = "テキストを入力すると採点結果を表示"; return; }
  const p = readParams();
  const r = codeScore(text, $("try-mono").checked, p, codeRegex(p));
  const el = (tag, cls, t) => { const e = document.createElement(tag); if (cls) e.className = cls; if (t !== undefined) e.textContent = t; return e; };
  const verdict = el("div", "verdict" + (r.code ? " code" : ""), `合計 ${r.score} 点 (しきい値 ${p.threshold}) → ${r.code ? "コード (翻訳しない)" : "文章 (翻訳する)"}`);
  const ul = el("ul");
  if (!r.hits.length) ul.append(el("li", "", "該当する条件なし"));
  for (const [name, v] of r.hits) ul.append(el("li", "", `${name}: ${v > 0 ? "+" : ""}${v}`));
  const s = r.stats;
  const stats = s ? el("div", "muted",
    `集計: ${s.lines} 行 / 空白を除き ${s.chars} 文字 / 記号 ${s.symbolList.length} 個 (割合 ${s.symbols.toFixed(2)}) / キーワード ${s.kws.length} 個${s.kws.length ? " (" + s.kws.slice(0, 10).join(", ") + ")" : ""} / 行末が記号 ${s.ends} 行 / 字下げ ${s.indented} 行 / 文末記号+空白 ${s.prose} 個`)
    : el("div", "muted", "空白を除き 3 文字未満のため採点しない");
  out.replaceChildren(verdict, ul, stats);
}
for (const b of document.querySelectorAll(".ex")) {
  b.addEventListener("click", () => {
    $("try-text").value = b.dataset.text;
    $("try-mono").checked = !!b.dataset.mono;
    tryScore();
  });
}
$("try-text").addEventListener("input", tryScore);
$("try-mono").addEventListener("change", tryScore);
for (const el of paramInputs) el.addEventListener("input", tryScore);
loadParams();

async function load() {
  all = (await cache.entries()).map(([k, e]) => ({ k, ...e })).sort((a, b) => b.ts - a.ts);
  render();
}

function render() {
  const q = filterEl.value.trim().toLowerCase();
  const lang = langEl.value;
  const hit = all.filter((e) =>
    (!lang || e.l === lang) && (!q || e.s.toLowerCase().includes(q) || e.t.toLowerCase().includes(q)));
  const shown = hit.slice(0, MAX_ROWS);

  $("count").textContent = `${hit.length} / ${all.length} 件`;
  $("empty").hidden = hit.length > 0;
  $("more").hidden = hit.length <= MAX_ROWS;
  $("more").textContent = `先頭 ${MAX_ROWS} 件のみ表示。絞り込みで残りを表示`;
  $("clear-all").disabled = all.length === 0;
  rowsEl.replaceChildren(...shown.map(row));
}

function row(e) {
  const tr = document.createElement("tr");
  const cell = (cls, content) => {
    const td = document.createElement("td");
    if (cls) td.className = cls;
    if (content !== undefined) td.textContent = content;
    tr.append(td);
    return td;
  };

  cell("", LANGS[e.l] ?? e.l);
  cell("src", e.s);

  const area = document.createElement("textarea");
  area.value = e.t;
  cell("", "").append(area);

  cell("ts", new Date(e.ts).toLocaleString("ja-JP"));

  const ops = cell("ops");
  const save = document.createElement("button");
  save.type = "button";
  save.textContent = "保存";
  save.className = "primary";
  save.disabled = true;
  const del = document.createElement("button");
  del.type = "button";
  del.textContent = "削除";
  del.className = "danger";
  ops.append(save, del);

  area.addEventListener("input", () => {
    const dirty = area.value !== e.t;
    tr.classList.toggle("dirty", dirty);
    save.disabled = !dirty || !area.value.trim();
  });
  save.addEventListener("click", async () => {
    const t = area.value.trim();
    if (!t) return;
    e.t = t;
    e.ts = Date.now();
    await chrome.storage.local.set({ [e.k]: { s: e.s, t: e.t, l: e.l, ts: e.ts } });
    all.sort((a, b) => b.ts - a.ts);
    say("保存しました");
    render();
  });
  del.addEventListener("click", async () => {
    await chrome.storage.local.remove(e.k);
    all = all.filter((x) => x !== e);
    say("削除しました");
    render();
  });
  return tr;
}

$("clear-all").addEventListener("click", async () => {
  if (!confirm(`辞書 ${all.length} 件をすべて削除します。よろしいですか?`)) return;
  await cache.clear();
  all = [];
  say("全件クリアしました");
  render();
});
filterEl.addEventListener("input", render);
langEl.addEventListener("change", render);

load();
