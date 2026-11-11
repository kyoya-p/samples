// 🤖 AI: getDisplayMedia(画面+システム音)を MediaRecorder で録画し、1 秒ごとにファイルへ逐次書き込む。
// 保存先は初回に選んだフォルダ。フォルダのハンドルを IndexedDB に記憶し、2 回目以降はダイアログなしで保存する。
// 逐次書き込み(File System Access API)により、途中でタブを閉じても、それまでの分は残る。
const $ = (id) => document.getElementById(id);
const logEl = $("log");
const log = (m) => { logEl.textContent += `[${new Date().toLocaleTimeString()}] ${m}\n`; logEl.scrollTop = logEl.scrollHeight; }; // 🤖 AI: 最新行へスクロール

let recorder, stream; // 🤖 AI: recorder は現在のファイルの MediaRecorder

// --- IndexedDB (ハンドルの記憶) ---
function idb(mode, fn) {
  return new Promise((res, rej) => {
    const o = indexedDB.open("chrome-recorder", 1);
    o.onupgradeneeded = () => o.result.createObjectStore("kv");
    o.onerror = () => rej(o.error);
    o.onsuccess = () => {
      const tx = o.result.transaction("kv", mode);
      const r = fn(tx.objectStore("kv"));
      tx.oncomplete = () => res(r?.result);
      tx.onerror = () => rej(tx.error);
    };
  });
}
const loadDir = () => idb("readonly", (s) => s.get("dir"));
const saveDir = (v) => idb("readwrite", (s) => s.put(v, "dir"));

async function showDir() {
  const v = await loadDir();
  $("dir").textContent = v ? v.dir.name : "未設定";
}

// 保存フォルダを選んで記憶する
async function pickDir() {
  const dir = await window.showDirectoryPicker({ id: "chrome-recorder", mode: "readwrite", startIn: "downloads" });
  await saveDir({ dir });
  await showDir();
  log(`保存フォルダ: ${dir.name}`);
  return dir;
}

// 記憶済みのフォルダを書き込み可能な状態で返す(未設定なら選ばせる)
async function getDir() {
  const v = await loadDir();
  if (!v) return pickDir();
  const opt = { mode: "readwrite" };
  if ((await v.dir.queryPermission(opt)) !== "granted" && (await v.dir.requestPermission(opt)) !== "granted") {
    throw new DOMException("保存フォルダへの書き込みが許可されませんでした", "NotAllowedError");
  }
  return v.dir;
}

function pickMime() {
  const c = ["video/webm;codecs=vp9,opus", "video/webm;codecs=vp8,opus", "video/webm"];
  return c.find((m) => MediaRecorder.isTypeSupported(m));
}

function fileName() {
  const d = new Date(), z = (n) => String(n).padStart(2, "0");
  return `rec_${d.getFullYear()}${z(d.getMonth() + 1)}${z(d.getDate())}_${z(d.getHours())}${z(d.getMinutes())}${z(d.getSeconds())}.webm`;
}

$("chdir").onclick = () => pickDir().catch((e) => log(`エラー: ${e.name}: ${e.message}`));

// --- 🤖 AI: 複数画面の同時録画 (追加した画面を canvas 横並び原寸 + 音声ミックスで 1 ストリームに合成) ---
const fps = () => Number($("fps").value) || 10;
const withAudio = () => true; // 🤖 AI: 音声は常に要求 (Audio チェックは不要との指示で削除)
const sources = []; // { name, stream }
let composite; // 録画中のみ { audioCtx }

function renderSources() {
  const ul = $("sources");
  ul.textContent = "";
  // 🤖 AI: 画面を追加せずに録画した (1 画面録画) 間は、録画中の画面を一覧に出す (停止後に同名で登録される)
  const items = tempSrc ? [...sources, tempSrc] : sources;
  for (const s of items) {
    // 🤖 AI: 名前 / 共有元 / 音声有無 / 削除 の 1 行表示
    const li = document.createElement("li");
    const v = s.stream.getVideoTracks()[0];
    const el = (tag, cls, text) => Object.assign(document.createElement(tag), { className: cls, textContent: text });
    const meter = el("span", "meter", ""), bar = el("i", "", ""); // 🤖 AI: 音量メーター
    meter.appendChild(bar); s.meterEl = s.stream.getAudioTracks().length ? bar : null; meter.hidden = !s.meterEl;
    li.append(el("span", "name", s.name), el("span", "meta", `${v?.getSettings().displaySurface ?? ""} · ${v?.label ?? ""}`), meter,
      el("span", "badge", s.stream.getAudioTracks().length ? "音声あり" : "音声なし"));
    const x = el("button", "ghost", "×");
    x.title = "削除 (共有を終了)";
    x.disabled = !!composite || !!s.temp;
    x.onclick = () => removeSource(s);
    li.appendChild(x);
    ul.appendChild(li);
  }
}

function removeSource(s) {
  detachMeter(s);
  s.stream.getTracks().forEach((t) => t.stop());
  const i = sources.indexOf(s);
  if (i >= 0) sources.splice(i, 1);
  renderSources();
  refreshPreview();
}

// 🤖 AI: 音声レベル表示 (2026-11-11)。画面ごとの横棒メーターと、全画面を合わせた直近 30 秒の推移グラフ。
// AnalyserNode は destination へ接続しない (再生もしない・録画にも影響しない)。タブ非表示でも止まらないよう setInterval。
const LEVEL_N = 600, LEVEL_MS = 50; // 30 秒分
// 🤖 AI: 音声有無の判定 (2026-11-11 たたき台): 合計音量が SOUND_DB 以上を ON_TICKS 連続 (1 秒) で「音あり」、SOUND_DB 未満を OFF_TICKS 連続 (30 秒) で「音なし」
const SOUND_DB = -45, ON_TICKS = 1000 / LEVEL_MS, OFF_TICKS = 30000 / LEVEL_MS;
const SOUND_LV = (SOUND_DB + 60) / 60;
let hasSound = false, runAbove = 0, runBelow = 0;
let mctx, master, masterBuf, history = new Array(LEVEL_N).fill(0), states = new Array(LEVEL_N).fill(false), meterTimer;
const toLevel = (buf) => { // RMS → dBFS (-60..0) → 0..1
  let sum = 0; for (const x of buf) sum += x * x;
  const db = 20 * Math.log10(Math.sqrt(sum / buf.length) || 1e-9);
  return Math.min(1, Math.max(0, (db + 60) / 60));
};
function attachMeter(s) {
  const tracks = s.stream.getAudioTracks();
  if (!tracks.length) return;
  mctx ??= new AudioContext();
  mctx.resume();
  if (!master) {
    master = mctx.createAnalyser(); master.fftSize = 1024; masterBuf = new Float32Array(master.fftSize);
    meterTimer = setInterval(tickMeter, LEVEL_MS);
  }
  const src = mctx.createMediaStreamSource(new MediaStream(tracks)), an = mctx.createAnalyser();
  an.fftSize = 1024;
  src.connect(an); src.connect(master);
  s.meter = { src, an, buf: new Float32Array(an.fftSize) };
  $("levelPanel").hidden = false;
}
function detachMeter(s) {
  if (!s.meter) return;
  s.meter.src.disconnect(); s.meter = null;
  if (!sources.some((x) => x !== s && x.meter) && !(tempSrc && tempSrc !== s && tempSrc.meter)) {
    clearInterval(meterTimer); master = null; history.fill(0); states.fill(false); hasSound = false; runAbove = runBelow = 0; $("levelPanel").hidden = true;
  }
}
function tickMeter() {
  for (const s of tempSrc ? [...sources, tempSrc] : sources) {
    if (!s.meter) continue;
    s.meter.an.getFloatTimeDomainData(s.meter.buf);
    if (s.meterEl) s.meterEl.style.width = `${toLevel(s.meter.buf) * 100}%`;
  }
  master.getFloatTimeDomainData(masterBuf);
  const lv = toLevel(masterBuf);
  if (lv >= SOUND_LV) { runAbove++; runBelow = 0; } else { runBelow++; runAbove = 0; }
  if (!hasSound && runAbove >= ON_TICKS) hasSound = true;
  else if (hasSound && runBelow >= OFF_TICKS) hasSound = false;
  history.push(lv); history.shift(); states.push(hasSound); states.shift();
  if (armed && !session && pre && hasSound) beginArmed(true);
  if (session) session.quiet = lv >= SOUND_LV ? 0 : (session.quiet || 0) + 1; // 録画開始 (またはファイル切替) 以降の連続無音
  if (session && $("autoStop").checked && !session.stopping && session.quiet >= OFF_TICKS) requestStop(true); // 無音 30 秒で保存 (自動開始が ON なら待機へ)
  drawLevel();
}
function drawLevel() {
  const c = $("level"), g = c.getContext("2d"), cs = getComputedStyle(document.documentElement);
  const w = c.width = c.clientWidth * devicePixelRatio, h = c.height = c.clientHeight * devicePixelRatio;
  g.clearRect(0, 0, w, h);
  g.strokeStyle = cs.getPropertyValue("--line"); g.lineWidth = 1;
  const bw = w / LEVEL_N, ty = h * (1 - SOUND_LV), th = 3 * devicePixelRatio; // ty: しきい値の y 座標
  history.forEach((v, i) => { // しきい値以上の棒は濃く、未満は薄く
    const bh = Math.max(1, v * h);
    g.fillStyle = cs.getPropertyValue(v >= SOUND_LV ? "--fg" : "--muted");
    g.fillRect(i * bw, h - bh, Math.max(1, bw - 0.5), bh);
  });
  g.fillStyle = cs.getPropertyValue("--rec"); // 「音あり」判定中は、しきい値の線に重ねて太線にする
  states.forEach((on, i) => { if (on) g.fillRect(i * bw, ty - th / 2, Math.ceil(bw), th); });
  g.strokeStyle = cs.getPropertyValue("--rec"); g.setLineDash([4, 4]); // しきい値
  g.beginPath(); g.moveTo(0, ty); g.lineTo(w, ty); g.stroke(); g.setLineDash([]);
}

// 🤖 AI: 音声が付かないときの案内。ウィンドウ共有で音声が付くかは未確認 (2026-11-10 の実例は audio=0)。画面全体ならシステム音を取得できた (README「動作確認」)
function audioHint(surface) {
  if (surface === "window") return "ウィンドウの共有で音声が付きませんでした。音声が必要なら「画面全体」を選び「システム音声も共有」をオンにしてください";
  return "共有ダイアログで「システム音声も共有」をオンにしてください";
}

// getDisplayMedia/getUserMedia はクリック毎に 1 回しか呼べないため、ソースはボタンで 1 つずつ追加
// 🤖 AI: 共有中のストリームを一覧に登録 (画面の追加時、および 1 画面録画の停止後に共有を維持するとき)
let sourceSeq = 0;
let tempSrc; // 🤖 AI: 画面を追加せずに録画した 1 画面録画のストリーム。録画中だけ一覧・音声レベルの対象にする
function registerSource(stream) {
  const s = { name: `screen${++sourceSeq}`, stream };
  sources.push(s);
  attachMeter(s);
  stream.getTracks().forEach((t) => t.addEventListener("ended", () => {
    if (!composite && stream.getTracks().every((x) => x.readyState === "ended")) removeSource(s);
  }));
  renderSources();
  refreshPreview();
  return s;
}

const addSource = (kind, get) => async () => {
  try {
    const stream = await get();
    const s = registerSource(stream);
    const surface = stream.getVideoTracks()[0]?.getSettings().displaySurface; // monitor / window / browser
    log(`ソース追加: ${s.name} (${surface}) video=${stream.getVideoTracks().length} audio=${stream.getAudioTracks().length}`);
    // 🤖 AI: 音声が付かなかった場合は追加の時点で知らせる
    if (withAudio() && !stream.getAudioTracks().length) log(`警告: ${s.name} は音声なし。${audioHint(surface)}`);
  } catch (e) {
    log(`エラー: ${e.name}: ${e.message}`);
  }
};
$("addScreen").onclick = addSource("screen", () => navigator.mediaDevices.getDisplayMedia({
  video: { frameRate: fps() }, audio: withAudio(), systemAudio: withAudio() ? "include" : "exclude",
}));

// 🤖 AI: プレビュー表示 (映像がないときは枠ごと隠す)
function setPreview(stream) {
  $("preview").srcObject = stream;
  $("preview").hidden = !stream;
}

// 🤖 AI: 追加した画面を canvas に横並び原寸で描画し続ける (追加時点から #preview に表示、録画時はこの canvas を使う)
let comp; // { vids, canvas, timer, out }

function refreshPreview() {
  if (comp) { clearInterval(comp.timer); comp.out.getTracks().forEach((t) => t.stop()); comp = null; }
  const list = sources.filter((s) => s.stream.getVideoTracks().some((t) => t.readyState === "live"));
  if (!list.length) { if (!recorder || recorder.state === "inactive") setPreview(null); return; }
  const vids = list.map((s) => {
    const v = document.createElement("video");
    v.srcObject = new MediaStream(s.stream.getVideoTracks());
    v.muted = true;
    v.play();
    return v;
  });
  const c = document.createElement("canvas"), g = c.getContext("2d");
  c.width = 1; c.height = 1;
  // setInterval: requestAnimationFrame は録画ページのタブが非表示だと止まるため
  const timer = setInterval(() => {
    const w = vids.reduce((n, v) => n + v.videoWidth, 0), h = Math.max(...vids.map((v) => v.videoHeight));
    if (w && (c.width !== w || c.height !== h)) { c.width = w; c.height = h; } // 横並び原寸
    g.fillStyle = "#000";
    g.fillRect(0, 0, c.width, c.height);
    let x = 0;
    for (const v of vids) { if (v.videoWidth) g.drawImage(v, x, 0); x += v.videoWidth; }
  }, 1000 / fps());
  comp = { vids, canvas: c, timer, out: c.captureStream(fps()) };
  setPreview(comp.out);
}

async function buildComposite() {
  if (!comp) refreshPreview();
  if (!comp) throw new DOMException("共有中の画面がありません", "NotFoundError");
  // 映像サイズ確定を待つ (未確定のまま録画を始めると webm の解像度が仮サイズになるため)
  await Promise.all(comp.vids.map((v) => v.videoWidth || new Promise((r) => v.addEventListener("loadedmetadata", r, { once: true }))));
  const c = comp.canvas;
  const w = comp.vids.reduce((n, v) => n + v.videoWidth, 0), h = Math.max(...comp.vids.map((v) => v.videoHeight));
  if (c.width !== w || c.height !== h) { c.width = w; c.height = h; } // 同値の代入でも canvas がクリアされるため比較
  const out = new MediaStream(comp.out.getVideoTracks());
  let audioCtx;
  const list = sources.filter((s) => s.stream.getTracks().some((t) => t.readyState === "live"));
  const audios = withAudio() ? list.flatMap((s) => s.stream.getAudioTracks()) : [];
  if (audios.length) {
    audioCtx = new AudioContext();
    const dest = audioCtx.createMediaStreamDestination();
    audios.forEach((t) => audioCtx.createMediaStreamSource(new MediaStream([t])).connect(dest));
    out.addTrack(dest.stream.getAudioTracks()[0]);
  }
  composite = { audioCtx };
  log(`合成: ${list.map((s) => s.name).join(", ")} (${c.width}x${c.height})`);
  return out;
}

function stopComposite() {
  if (!composite) return;
  composite.audioCtx?.close();
  composite = null;
  refreshPreview(); // 🤖 AI: 停止後も画面の共有は維持 (ソースは解放せず、プレビューを再開)
  renderSources();
}

// 🤖 AI: 録画 1 ファイル分 (セグメント)。録画中に ⏺ を押すと、ここまでを保存して新しいファイルで続ける (2026-11-11 ユーザー指示)
async function uniqueName(dir) {
  const base = fileName().replace(/\.webm$/, "");
  for (let i = 1; ; i++) { // 同じ秒に分割した場合は _2, _3… を付ける
    const name = i === 1 ? `${base}.webm` : `${base}_${i}.webm`;
    try { await dir.getFileHandle(name); } catch (e) { if (e.name === "NotFoundError") return name; throw e; }
  }
}

// 🤖 AI: MediaRecorder の出力をファイルへ逐次書き込む。initial は書き込み前にためていたチャンク (待機中の先頭バッファ)
function bindWriter(r, w, dir, name, initial = []) {
  let chain = Promise.resolve(), n = 0;
  const put = (d) => { n += d.size; chain = chain.then(() => w.write(d)); };
  initial.forEach((d) => d.size && put(d));
  r.ondataavailable = (e) => e.data.size && put(e.data);
  r.saved = new Promise((res) => {
    r.onstop = async () => {
      try {
        await chain; await w.close();
        if (n) log(`保存完了: ${name} (${n} bytes)`);
        else { await dir.removeEntry(name); log(`空のため削除: ${name}`); } // 🤖 AI: 分割直後に再度押した場合など、データ 0 の場合
      }
      catch (e) { log(`エラー: 保存 ${name}: ${e.name}: ${e.message}`); }
      res();
    };
  });
  r.fileName = name;
}

async function startSegment(dir) {
  const name = await uniqueName(dir);
  const w = await (await dir.getFileHandle(name, { create: true })).createWritable();
  try {
    const r = new MediaRecorder(stream, { mimeType: pickMime() });
    bindWriter(r, w, dir, name);
    r.start(1000);
    return r;
  } catch (e) {
    try { await w.abort(); } catch {}
    try { await dir.removeEntry(name); } catch {}
    throw e;
  }
}

// --- 🤖 AI: 音声で自動録画 (2026-11-11 ユーザー指示 c5: 先頭の欠け対策つき) ---
// 待機中は合成ストリームを MediaRecorder でメモリに録り続け (PRE_MS ごとに作り直して古い分を捨てる)、
// 「音あり」判定の時点でそのバッファをファイルの先頭にして続きを書き込む。音あり判定は 1 秒かかるが、その 1 秒も先頭に含まれる。
// 音の立ち上がり候補 (runAbove > 0) の間は作り直さないので、立ち上がりがバッファの前に切れることはない。
const PRE_MS = 10000;
let armed = false, pre, preTimer;
function startPre() {
  const r = new MediaRecorder(stream, { mimeType: pickMime() }), chunks = [];
  r.ondataavailable = (e) => e.data.size && chunks.push(e.data);
  r.start(1000);
  pre = { r, chunks, t0: Date.now() };
}
function rotatePre() {
  if (!pre || session || hasSound || runAbove > 0 || Date.now() - pre.t0 < PRE_MS) return;
  const old = pre; startPre(); old.r.stop(); // 古いバッファは破棄
}
async function arm() {
  if (armed || session) return;
  if (!sources.some((s) => s.meter)) { log("自動録画: 音声ありの画面を追加してください"); $("autoStart").checked = false; return; }
  armed = true; $("addScreen").disabled = true;
  try {
    stream = await buildComposite();
  } catch (e) {
    log(`エラー: ${e.name}: ${e.message}`); armed = false; $("addScreen").disabled = false; $("autoStart").checked = false; return;
  }
  renderSources();
  startPre(); preTimer = setInterval(rotatePre, 1000);
  const ends = sources.flatMap((s) => s.stream.getTracks());
  ends.forEach((t) => t.addEventListener("ended", () => { if (armed && !session && ends.every((x) => x.readyState === "ended")) { $("autoStart").checked = false; disarm(); } }));
  $("status").textContent = "待機中"; setTabRec("待機中", true);
  log("自動録画: 待機中 (音が出たら録画を開始)");
}
function disarm() {
  if (session) { armed = false; return; } // 録画中なら停止まで続ける (停止後は待機に戻らない)
  if (!armed) return;
  armed = false; clearInterval(preTimer);
  if (pre) { try { pre.r.stop(); } catch {} pre = null; }
  if (composite) { stream.getAudioTracks().forEach((t) => t.stop()); stopComposite(); }
  $("addScreen").disabled = false; $("status").textContent = ""; setTabRec("");
  log("自動録画: 解除");
}
// 待機バッファを引き継いで録画開始 (auto=true: 音声検知 / false: ⏺ の手動押下)
async function beginArmed(auto) {
  const p = pre; if (!p) return;
  pre = null; clearInterval(preTimer); // 以後の作り直し・二重開始を防ぐ
  $("start").disabled = true;
  try {
    const dir = await getDir();
    const name = await uniqueName(dir);
    const w = await (await dir.getFileHandle(name, { create: true })).createWritable();
    recorder = p.r;
    bindWriter(recorder, w, dir, name, p.chunks); // 同期で以降のチャンクの書き込み先を切り替える
    session = { dir, auto };
    log(`${auto ? "音声検知" : "手動"}: 先頭 ${Math.round((Date.now() - p.t0) / 1000)} 秒分のバッファから録画`);
    afterStart(dir);
  } catch (e) {
    log(`エラー: ${e.name}: ${e.message}`);
    try { p.r.stop(); } catch {}
    $("autoStart").checked = false; armed = false;
    if (composite) { stream.getAudioTracks().forEach((t) => t.stop()); stopComposite(); }
    $("addScreen").disabled = false; $("status").textContent = ""; setTabRec(""); $("start").disabled = false;
  }
}
$("autoStart").onchange = () => ($("autoStart").checked ? arm() : disarm());

let session; // 録画中のみ { dir, busy, stopping }

function setRecordingUI(on) {
  $("fps").disabled = $("rotate").disabled = on;
  $("stop").disabled = !on;
  $("start").disabled = false;
  $("start").title = on ? "ここまでを保存し、新しいファイルで録画を続ける" : "録画開始";
  // 🤖 AI: 録画中は「分割」(控えめな見た目) に変え、停止を赤の強調表示にして、開始ボタンと区別する
  $("start").classList.toggle("primary", !on); $("start").classList.toggle("split", on);
  $("start").innerHTML = on ? '<span class="rec-mark">&#x23FA;&#xFE0E;</span> 分割' : '<span class="rec-mark">&#x23FA;&#xFE0E;</span> 録画';
  $("start").setAttribute("aria-label", $("start").title);
}

// 🤖 AI: 録画開始後の共通処理 (ログ・共有停止の監視・UI・自動分割)
function afterStart(dir) {
  const v = stream.getVideoTracks().length, a = stream.getAudioTracks().length;
  log(`共有開始: video=${v} audio=${a} mime=${recorder.mimeType} -> ${dir.name}\\${recorder.fileName}`);
  if (!a && withAudio()) log(`警告: 音声なしで録画します。${audioHint((composite ? sources.map((x) => x.stream) : [stream]).some((x) => x.getVideoTracks()[0]?.getSettings().displaySurface === "window") ? "window" : "")}`);

  // 共有ダイアログの「共有を停止」でも終了させる
  // 🤖 AI: まとめて録画では全ソースの共有停止で終了
  const ends = composite ? sources.flatMap((s) => s.stream.getTracks()) : stream.getVideoTracks();
  ends.forEach((t) => t.addEventListener("ended", () => {
    if (ends.every((x) => x.readyState === "ended")) requestStop();
  }));

  setRecordingUI(true);
  setStatus(true);
  renderSources();
  if (rotateMs()) log(`自動分割: ${$("rotate").value} 分ごと`);
  scheduleRotate();
}

$("start").onclick = async () => {
  if (session) return splitSegment();
  if (armed) return beginArmed(false);
  $("start").disabled = true; // 開始処理中の二重押し防止
  try {
    const dir = await getDir();
    if (sources.length) {
      stream = await buildComposite();
      renderSources();
    } else try {
      stream = await navigator.mediaDevices.getDisplayMedia({
        video: { frameRate: fps() },
        audio: withAudio(),
        systemAudio: withAudio() ? "include" : "exclude",
      });
    } catch (e) {
      // フォルダ選択・許可のダイアログでクリックの効力が切れた場合
      if (e.name === "InvalidStateError") { log("保存フォルダの準備ができました。もう一度 ⏺ (録画開始) を押してください"); $("start").disabled = false; return; }
      throw e;
    }
    setPreview(stream);
    tempSrc = { name: `screen${sourceSeq + 1}`, stream, temp: true }; attachMeter(tempSrc); // 音声レベルの対象にする
    recorder = await startSegment(dir);
    session = { dir };

    afterStart(dir);
  } catch (e) {
    log(`エラー: ${e.name}: ${e.message}`);
    if (tempSrc) { detachMeter(tempSrc); tempSrc = null; }
    stream?.getTracks().forEach((t) => t.stop());
    stopComposite(); // 🤖 AI
    $("start").disabled = false;
  }
};

// 🤖 AI: 分割。新しいファイルの録画を先に始めてから、前のファイルを止めて保存する (つなぎ目の欠落を減らすため)
async function splitSegment() {
  const s = session;
  if (s.stopping || s.busy) return;
  $("start").disabled = true;
  s.busy = (async () => {
    const old = recorder;
    try { recorder = await startSegment(s.dir); }
    catch (e) { log(`エラー: 分割できませんでした (録画は継続): ${e.name}: ${e.message}`); return; }
    log(`分割: ${old.fileName} を保存し、${recorder.fileName} へ録画`);
    old.stop();
    setStatus(true); // 経過時間はファイルごと
  })();
  await s.busy;
  s.busy = null;
  if (!s.stopping) $("start").disabled = false;
  scheduleRotate(); // 手動で分割した場合も、そこから数え直す
}

// 🤖 AI: 自動ローテーション (2026-11-11 ユーザー指示「時間か分で設定、0=自動ローテなし」)。値と単位は localStorage に記憶
const rotateMs = () => Math.max(0, Number($("rotate").value) || 0) * 60 * 1000; // 単位は分固定 (ユーザー指示)
let rotateTimer;
function scheduleRotate() {
  clearTimeout(rotateTimer);
  const ms = rotateMs();
  if (ms && session && !session.stopping) rotateTimer = setTimeout(splitSegment, ms);
}
for (const id of ["rotate"]) {
  try { const v = localStorage.getItem(`chrome-recorder.${id}`); if (v !== null) $(id).value = v; } catch {}
  $(id).addEventListener("change", () => { try { localStorage.setItem(`chrome-recorder.${id}`, $(id).value); } catch {} });
}

// 🤖 AI: 録画中の経過時間表示
let statusTimer;
// 🤖 AI: 録画中はタブのタイトルに「● REC mm:ss」を出す (2026-11-11 ユーザー要望)
const BASE_TITLE = document.title;
function setTabRec(text, wait = false) { // wait=true: 待機中 (自動開始)。ファビコンの赤丸は録画中のみ (2026-11-11 ユーザー指示)
  document.title = text ? `${text} | ${BASE_TITLE}` : BASE_TITLE; // タイトル中の丸印は不要 (ユーザー指示)
  const rec = !!text && !wait && !text.startsWith("保存中");
  let link = document.querySelector("link[rel=icon]");
  if (rec === !!link?.dataset.rec) return;
  link?.remove(); // 差し替え時は作り直す (href の書き換えだけだとブラウザが前のアイコンを保持することがある)
  link = document.createElement("link"); link.rel = "icon";
  if (rec) {
    const c = document.createElement("canvas"); c.width = c.height = 32;
    const g = c.getContext("2d"); g.fillStyle = "#e00"; g.beginPath(); g.arc(16, 16, 13, 0, Math.PI * 2); g.fill();
    link.href = c.toDataURL("image/png"); link.dataset.rec = "1";
  } else link.href = "data:,"; // 録画中以外は既定のアイコンに戻す (空の icon 指定)
  document.head.appendChild(link);
}
function setStatus(on) {
  clearInterval(statusTimer);
  const st = $("status");
  st.classList.toggle("on", on);
  if (!on) { st.textContent = ""; setTabRec(""); return; }
  const t0 = Date.now(), z = (n) => String(n).padStart(2, "0");
  const tick = () => { const s = Math.floor((Date.now() - t0) / 1000); st.textContent = `REC ${z(Math.floor(s / 60))}:${z(s % 60)}`; setTabRec(st.textContent); };
  tick();
  statusTimer = setInterval(tick, 1000);
}

// 🤖 AI: 停止は押した時点で反映 (ボタン無効化・「保存中…」表示)。保存完了後に共有の後始末
async function requestStop(rearm = false) {
  const s = session;
  if (!s || s.stopping) return;
  s.stopping = true;
  clearTimeout(rotateTimer);
  $("stop").disabled = $("start").disabled = true;
  clearInterval(statusTimer);
  const st = $("status"); st.classList.remove("on"); st.textContent = "保存中…"; setTabRec("保存中…");
  log("停止: 保存中…");
  await s.busy; // 分割の途中なら終わるのを待つ
  const r = recorder;
  if (r.state !== "inactive") r.stop();
  await r.saved;
  if (tempSrc) { detachMeter(tempSrc); tempSrc = null; } // 一覧へ移す場合は registerSource が改めてつなぐ
  // 🤖 AI: 停止しても画面の共有は止めない
  if (composite) {
    stream.getAudioTracks().forEach((t) => t.stop()); // 合成用の音声ミックスのみ停止
    stopComposite();
  } else if (stream.getVideoTracks().some((t) => t.readyState === "live")) {
    const x = registerSource(stream); // 1 画面録画の共有は一覧へ移して維持
    log(`共有を維持: ${x.name} (不要なら ×)`);
  } else {
    stream.getTracks().forEach((t) => t.stop());
    setPreview(null);
  }
  renderSources();
  session = null;
  renderSources(); // 録画中だけ出していた仮の行を整理
  armed = false; $("addScreen").disabled = false;
  setRecordingUI(false);
  setStatus(false);
  if (rearm && $("autoStart").checked) arm(); // 無音で自動終了した場合は待機へ戻る
  else if ($("autoStart").checked) { $("autoStart").checked = false; log("自動録画: 解除 (手動で停止したため)"); }
}
$("stop").onclick = () => requestStop(); // 引数 (MouseEvent) を rearm と取り違えないよう明示

showDir();
