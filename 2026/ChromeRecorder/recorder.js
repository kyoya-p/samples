// 🤖 AI: getDisplayMedia(画面+システム音)を MediaRecorder で録画し、1 秒ごとにファイルへ逐次書き込む。
// 保存先は初回に選んだフォルダ。フォルダのハンドルを IndexedDB に記憶し、2 回目以降はダイアログなしで保存する。
// 逐次書き込み(File System Access API)により、途中でタブを閉じても、それまでの分は残る。
const $ = (id) => document.getElementById(id);
const logEl = $("log");
const log = (m) => { logEl.textContent += `[${new Date().toLocaleTimeString()}] ${m}\n`; };

let recorder, stream, writable, writeChain = Promise.resolve(), bytes = 0;

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

$("start").onclick = async () => {
  let file;
  try {
    const dir = await getDir();
    try {
      stream = await navigator.mediaDevices.getDisplayMedia({
        video: { frameRate: Number($("fps").value) || 10 },
        audio: true,
        systemAudio: "include",
      });
    } catch (e) {
      // フォルダ選択・許可のダイアログでクリックの効力が切れた場合
      if (e.name === "InvalidStateError") { log("保存フォルダの準備ができました。もう一度「録画開始」を押してください"); return; }
      throw e;
    }
    const name = fileName();
    file = { dir, name };
    writable = await (await dir.getFileHandle(name, { create: true })).createWritable();
    $("preview").srcObject = stream;

    const mime = pickMime();
    const v = stream.getVideoTracks().length, a = stream.getAudioTracks().length;
    log(`共有開始: video=${v} audio=${a} mime=${mime} -> ${dir.name}\\${name}`);
    if (!a) log("警告: 音声トラックがありません(共有ダイアログで「システム音声も共有」をオンにしてください)");

    recorder = new MediaRecorder(stream, { mimeType: mime });
    bytes = 0;
    recorder.ondataavailable = (e) => {
      if (!e.data.size) return;
      bytes += e.data.size;
      writeChain = writeChain.then(() => writable.write(e.data));
    };
    recorder.onstop = async () => {
      await writeChain;
      await writable.close();
      stream.getTracks().forEach((t) => t.stop());
      $("preview").srcObject = null;
      log(`保存完了: ${name} (${bytes} bytes)`);
      $("start").disabled = false; $("stop").disabled = true;
    };
    // 共有ダイアログの「共有を停止」でも終了させる
    stream.getVideoTracks()[0].addEventListener("ended", () => recorder.state !== "inactive" && recorder.stop());

    recorder.start(1000);
    $("start").disabled = true; $("stop").disabled = false;
  } catch (e) {
    log(`エラー: ${e.name}: ${e.message}`);
    stream?.getTracks().forEach((t) => t.stop());
    try { await writable?.abort(); } catch {}
    try { if (file) await file.dir.removeEntry(file.name); } catch {}
  }
};

$("stop").onclick = () => recorder && recorder.state !== "inactive" && recorder.stop();

showDir();
