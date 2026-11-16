// 🤖[AI 生成・編集を含む (Antigravity)]
// Chrome / Edge Native Messaging Host for AI Usage Panel
// 注意: process.stdout は 4-byte UInt32LE 長 + UTF-8 JSON の専用チャネル。通常ログは stdout に書かないこと。
const fs = require('fs');
const path = require('path');
const os = require('os');
const { execFile } = require('child_process');

// Guard stdout so accidental console.log cannot corrupt Native Messaging framing
const nativeOut = process.stdout;
console.log = (...args) => console.error(...args);

const extraPaths = [
  path.join(os.homedir(), 'AppData', 'Local', 'mise', 'installs', 'claude-code', 'latest'),
  path.join(os.homedir(), 'AppData', 'Local', 'mise', 'installs', 'antigravity-cli', 'latest'),
  path.join(os.homedir(), 'AppData', 'Local', 'Microsoft', 'WinGet', 'Links'),
  path.join(os.homedir(), 'AppData', 'Local', 'mise', 'shims'),
].filter(p => fs.existsSync(p) && !(process.env.PATH || '').includes(p));
if (extraPaths.length) {
  process.env.PATH = extraPaths.join(path.delimiter) + path.delimiter + (process.env.PATH || '');
}

const REPO_ROOT = path.resolve(__dirname, '..', '..');
const PROVIDERS = {
  claude: {
    sampleScript: path.join(REPO_ROOT, 'ClaudeCodeTools', 'usage_sample.js'),
    panelScript: path.join(REPO_ROOT, 'ClaudeCodeTools', 'usage_panel.js'),
    dataDir: path.join(os.homedir(), '.claude', 'usage'),
    samplesPath: path.join(os.homedir(), '.claude', 'usage', 'usage_samples.jsonl'),
    lockPath: path.join(os.homedir(), '.claude', 'usage', '.usage_hook.lock'),
  },
  agy: {
    sampleScript: path.join(REPO_ROOT, 'AgyTools', 'usage_sample.js'),
    panelScript: path.join(REPO_ROOT, 'AgyTools', 'usage_panel.js'),
    dataDir: path.join(os.homedir(), '.gemini', 'usage'),
    samplesPath: path.join(os.homedir(), '.gemini', 'usage', 'usage_samples.jsonl'),
    lockPath: path.join(os.homedir(), '.gemini', 'usage', '.usage_hook.lock'),
  },
};

function logError(msg) {
  try {
    const dir = path.join(os.homedir(), '.claude', 'usage');
    fs.mkdirSync(dir, { recursive: true });
    fs.appendFileSync(path.join(dir, 'chrome_host.log'), `${new Date().toISOString()} ${msg}\n`, 'utf8');
  } catch { /* ignore */ }
}

function sendMessage(obj) {
  const json = JSON.stringify(obj);
  const body = Buffer.from(json, 'utf8');
  const header = Buffer.alloc(4);
  header.writeUInt32LE(body.length, 0);
  nativeOut.write(Buffer.concat([header, body]));
}

function compactData(fullData) {
  // Drop redundant modelSamples (reconstructed on extension side from samples) to save ~50% wire size
  const data = { ...fullData };
  delete data.modelSamples;
  // Guard against Chrome's 1MB Native Messaging single-message limit
  const MAX_BYTES = 880 * 1024;
  if (Buffer.byteLength(JSON.stringify(data), 'utf8') > MAX_BYTES && Array.isArray(data.samples)) {
    const trimmed = [...data.samples];
    while (trimmed.length > 10 && Buffer.byteLength(JSON.stringify({ ...data, samples: trimmed }), 'utf8') > MAX_BYTES) {
      trimmed.splice(0, Math.max(1, Math.floor(trimmed.length * 0.1)));
    }
    data.samples = trimmed;
    data.truncatedOldSamples = true;
  }
  return data;
}

function execFileAsync(cmd, args, opts) {
  return new Promise((resolve, reject) => {
    execFile(cmd, args, opts, (err, stdout, stderr) => {
      if (err) {
        err.stdout = stdout;
        err.stderr = stderr;
        reject(err);
      } else {
        resolve({ stdout: stdout || '', stderr: stderr || '' });
      }
    });
  });
}

function fileSize(p) {
  try { return fs.statSync(p).size; } catch { return 0; }
}

function loadProviderData(providerKey) {
  const cfg = PROVIDERS[providerKey];
  if (!cfg) throw new Error(`Unknown provider: ${providerKey}`);
  const panelMod = require(cfg.panelScript);
  return { panelMod, fullData: panelMod.buildData() };
}

async function runSample(providerKey, force) {
  const cfg = PROVIDERS[providerKey];
  if (!cfg) throw new Error(`Unknown provider: ${providerKey}`);
  fs.mkdirSync(cfg.dataDir, { recursive: true });

  // Respect existing hook lock (ignore stale locks older than 5 min)
  if (fs.existsSync(cfg.lockPath)) {
    try {
      if (Date.now() - fs.statSync(cfg.lockPath).mtimeMs < 5 * 60 * 1000) {
        const { fullData } = loadProviderData(providerKey);
        return {
          provider: providerKey,
          skipped: true,
          reason: 'already_running',
          sampledAt: new Date().toISOString(),
          data: compactData(fullData),
        };
      }
    } catch { /* ignore */ }
  }

  fs.writeFileSync(cfg.lockPath, String(process.pid), 'utf8');
  try {
    const env = {
      ...process.env,
      USAGE_SAMPLING: '1',
      AGY_USAGE_SAMPLING: '1',
    };
    if (force) {
      delete env.USAGE_GATE;
    } else {
      env.USAGE_GATE = '1';
    }

    const before = fileSize(cfg.samplesPath);
    const { stdout, stderr } = await execFileAsync(process.execPath, [cfg.sampleScript], {
      env,
      timeout: 150000,
      windowsHide: true,
      maxBuffer: 4 * 1024 * 1024,
    });
    const after = fileSize(cfg.samplesPath);
    const trimmedOut = stdout.trim();
    const skipped = trimmedOut === 'skipped' && after === before;

    const { panelMod, fullData } = loadProviderData(providerKey);
    if (!skipped) {
      try { panelMod.writePanel(fullData); } catch (e) { logError(`writePanel(${providerKey}): ${e.message}`); }
    }

    return {
      provider: providerKey,
      skipped,
      appended: after !== before,
      sampleOutput: trimmedOut,
      sampleStderr: stderr.trim() || undefined,
      sampledAt: new Date().toISOString(),
      data: compactData(fullData),
    };
  } finally {
    try { fs.unlinkSync(cfg.lockPath); } catch { /* ignore */ }
  }
}

async function handleMessage(msg) {
  const id = msg?.id;
  const action = msg?.action;
  try {
    if (action === 'ping') {
      return {
        id,
        ok: true,
        action,
        nodeVersion: process.version,
        hostPid: process.pid,
        repoRoot: REPO_ROOT,
      };
    }
    if (action === 'getData') {
      const provider = msg?.provider;
      const { fullData } = loadProviderData(provider);
      return {
        id,
        ok: true,
        action,
        provider,
        data: compactData(fullData),
      };
    }
    if (action === 'sample') {
      const provider = msg?.provider;
      const force = Boolean(msg?.force);
      const res = await runSample(provider, force);
      return {
        id,
        ok: true,
        action,
        ...res,
      };
    }
    throw new Error(`Unknown action: ${action}`);
  } catch (err) {
    const detail = [err.message, err.stderr && String(err.stderr).trim()].filter(Boolean).join(' | ');
    logError(`${action || 'unknown'}(${msg?.provider || '-'}): ${detail}`);
    return {
      id,
      ok: false,
      action,
      provider: msg?.provider,
      error: detail,
    };
  }
}

let inBuf = Buffer.alloc(0);
let pending = 0;
let stdinEnded = false;

function maybeExit() {
  if (stdinEnded && pending === 0) process.exit(0);
}

process.stdin.on('data', chunk => {
  inBuf = Buffer.concat([inBuf, chunk]);
  while (inBuf.length >= 4) {
    const msgLen = inBuf.readUInt32LE(0);
    if (msgLen > 10 * 1024 * 1024) {
      logError(`Invalid message length: ${msgLen}`);
      process.exit(1);
    }
    if (inBuf.length < 4 + msgLen) break;
    const raw = inBuf.subarray(4, 4 + msgLen).toString('utf8');
    inBuf = inBuf.subarray(4 + msgLen);

    let msg;
    try {
      msg = JSON.parse(raw);
    } catch (e) {
      sendMessage({ ok: false, error: `Invalid JSON: ${e.message}` });
      continue;
    }

    pending++;
    handleMessage(msg)
      .then(res => sendMessage(res))
      .catch(err => sendMessage({ id: msg?.id, ok: false, error: err.message }))
      .finally(() => {
        pending--;
        maybeExit();
      });
  }
});

process.stdin.on('end', () => {
  stdinEnded = true;
  maybeExit();
});
