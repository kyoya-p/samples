// 🤖 AI: ChromeRecorder 拡張の動作確認 (Playwright)
// 使い方: node ext_test.cjs <ブラウザ exe> <拡張ディレクトリ> <出力 webm> <FAKE|MANUAL|共有元名>
// playwright-core は PW_CORE (パス) または通常の require で解決
const { chromium } = require(process.env.PW_CORE || "playwright-core");
const fs = require("fs"), path = require("path");
const [exe, src, out, sel] = process.argv.slice(2);
const prof = path.join(require("os").tmpdir(), "chrome-recorder-test-" + path.basename(exe, ".exe"));

(async () => {
  const ctx = await chromium.launchPersistentContext(prof, {
    executablePath: exe, headless: false,
    args: [
      `--disable-extensions-except=${src}`, `--load-extension=${src}`,
      ...(sel === "FAKE" ? ["--use-fake-ui-for-media-stream"] : sel === "MANUAL" ? [] : [`--auto-select-desktop-capture-source=${sel}`]),
      "--autoplay-policy=no-user-gesture-required",
    ],
  });
  try {
    let sw = ctx.serviceWorkers()[0];
    if (!sw) sw = await ctx.waitForEvent("serviceworker", { timeout: 10000 }).catch(() => null);
    if (!sw) { console.log("EXT_NOT_LOADED"); return; }
    const id = new URL(sw.url()).host;
    console.log("ext id", id);

    // 音源タブ: 440Hz
    const tone = await ctx.newPage();
    await tone.setContent("<p>tone</p>");
    await tone.evaluate(() => { const c = new AudioContext(); const o = c.createOscillator(); const g = c.createGain(); g.gain.value = 0.3; o.connect(g).connect(c.destination); o.start(); return c.state; }).then((s) => console.log("tone", s));

    const p = await ctx.newPage();
    p.on("console", (m) => console.log("page:", m.text()));
    await p.goto(`chrome-extension://${id}/recorder.html`);
    if (sel !== "MANUAL") console.log("probe:", await p.evaluate(async () => {
      const t = new Promise((r) => setTimeout(() => r("TIMEOUT(5s)"), 5000));
      const g = navigator.mediaDevices.getDisplayMedia({ video: true, audio: true, systemAudio: "include" })
        .then((s) => { const r = `ok v=${s.getVideoTracks().length} a=${s.getAudioTracks().length} label=${s.getVideoTracks()[0]?.label}`; s.getTracks().forEach((x) => x.stop()); return r; })
        .catch((e) => `${e.name}: ${e.message}`);
      return Promise.race([g, t]);
    }));
    // フォルダ選択ダイアログは自動操作できないため、OPFS のルートを親フォルダとして返す(呼び出し回数を数える)
    const rounds = Number(process.env.ROUNDS || 2);
    let picks = 0;
    await p.exposeFunction("__picked", () => ++picks);
    for (let r = 1; r <= rounds; r++) {
      await p.reload(); // 2 回目以降は IndexedDB に記憶したフォルダを使うことを確認する
      await p.evaluate(() => { window.showDirectoryPicker = async () => { await window.__picked(); return navigator.storage.getDirectory(); }; });
      console.log(`--- round ${r} dir表示: ${await p.textContent("#dir")}`);
      await p.click("#start");
      if (sel === "MANUAL") {
        await p.waitForFunction(() => /共有開始|エラー/.test(document.getElementById("log").textContent), null, { timeout: 90000 }).catch(() => {});
      }
      await p.waitForTimeout(6000);
      if (await p.isDisabled("#stop")) { console.log("LOG:\n" + await p.textContent("#log")); console.log("NOT_STARTED"); return; }
      await p.click("#stop");
      await p.waitForFunction(() => /保存完了|エラー/.test(document.getElementById("log").textContent), null, { timeout: 15000 }).catch(() => {});
      const logText = await p.textContent("#log");
      console.log("LOG:\n" + logText + `picker 呼び出し累計: ${picks}`);
      const name = (logText.match(/保存完了: (\S+)/) || [])[1];
      if (!name) continue;
      const b64 = await p.evaluate(async (n) => {
        const d = await navigator.storage.getDirectory();
        const u = new Uint8Array(await (await (await d.getFileHandle(n)).getFile()).arrayBuffer()); let s = "";
        for (let i = 0; i < u.length; i += 0x8000) s += String.fromCharCode(...u.subarray(i, i + 0x8000));
        return btoa(s);
      }, name);
      const o = rounds > 1 ? out.replace(/(\.webm)?$/, `_${r}.webm`) : out;
      fs.writeFileSync(o, Buffer.from(b64, "base64")); console.log("saved", o, fs.statSync(o).size);
    }
  } finally { await ctx.close(); }
})().catch((e) => { console.error("ERR", e.message); process.exit(1); });
