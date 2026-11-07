// 設定ページ: 辞書 (翻訳キャッシュ) の一覧・編集・削除・全件クリア
const { LANGS, SUPPORTED, browserLang, cache } = NanoPrompt;

const $ = (id) => document.getElementById(id);
const rowsEl = $("rows");
const filterEl = $("filter");
const langEl = $("lang");
const MAX_ROWS = 200;

let all = []; // [{ k, s, t, l, ts }] 新しい順

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
