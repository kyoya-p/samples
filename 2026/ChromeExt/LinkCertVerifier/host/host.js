// Chrome / Edge Native Messaging Host for LinkCertVerifier
// 注意: process.stdout は 4-byte UInt32LE 長 + UTF-8 JSON の専用チャネル。通常ログは stdout に書かないこと。
const tls = require('tls');
const net = require('net');
const dns = require('dns');
const http = require('http');
const https = require('https');
const crypto = require('crypto');
const urlMod = require('url');

const nativeOut = process.stdout;
console.log = (...args) => console.error(...args);

function sendMessage(obj) {
  const json = JSON.stringify(obj);
  const body = Buffer.from(json, 'utf8');
  const header = Buffer.alloc(4);
  header.writeUInt32LE(body.length, 0);
  nativeOut.write(Buffer.concat([header, body]));
}

function getCombinedCAs() {
  if (typeof tls.getCACertificates === 'function') {
    try {
      const def = tls.getCACertificates('default') || [];
      const sys = tls.getCACertificates('system') || [];
      return [...new Set([...def, ...sys])];
    } catch { /* fallback */ }
  }
  return tls.rootCertificates;
}

function isPrivateOrLoopbackIP(ip) {
  if (!ip) return false;
  if (net.isIPv4(ip)) {
    const parts = ip.split('.').map(Number);
    if (parts[0] === 127) return true;
    if (parts[0] === 10) return true;
    if (parts[0] === 172 && parts[1] >= 16 && parts[1] <= 31) return true;
    if (parts[0] === 192 && parts[1] === 168) return true;
    if (parts[0] === 169 && parts[1] === 254) return true;
    if (parts[0] === 0) return true;
    return false;
  }
  const low = ip.toLowerCase();
  return low === '::1' || low.startsWith('fe80:') || low.startsWith('fc') || low.startsWith('fd');
}

function parseDNString(str) {
  const map = {};
  if (!str) return map;
  for (const line of String(str).split(/\r?\n/)) {
    const idx = line.indexOf('=');
    if (idx <= 0) continue;
    const k = line.slice(0, idx).trim();
    const v = line.slice(idx + 1).trim();
    map[k] = map[k] ? `${map[k]}, ${v}` : v;
  }
  return map;
}

function formatKeyInfo(pubKey) {
  if (!pubKey) return '不明';
  const type = pubKey.asymmetricKeyType || 'unknown';
  const d = pubKey.asymmetricKeyDetails || {};
  if (type === 'rsa' || type === 'rsa-pss') {
    return `${type.toUpperCase()} ${d.modulusLength || '?'} bit`;
  }
  if (type === 'ec') {
    return `ECDSA (${d.namedCurve || 'unknown'})`;
  }
  if (type === 'ed25519' || type === 'ed448') {
    return type.toUpperCase();
  }
  return type;
}

function parseAltNames(sanStr) {
  if (!sanStr) return [];
  return String(sanStr)
    .split(',')
    .map(s => s.trim())
    .filter(Boolean);
}

function extractCertDetails(peerCert) {
  const chain = [];
  const seen = new Set();
  let cur = peerCert;
  let depth = 0;

  while (cur && (cur.raw || cur.fingerprint256) && depth < 12) {
    const fpKey = cur.fingerprint256 || `${depth}_${cur.serialNumber}`;
    if (seen.has(fpKey)) break;
    seen.add(fpKey);

    let x = null;
    try {
      if (cur.raw && typeof crypto.X509Certificate === 'function') {
        x = new crypto.X509Certificate(cur.raw);
      }
    } catch { /* fallback to legacy object */ }

    const subjectRaw = x ? x.subject : '';
    const issuerRaw = x ? x.issuer : '';
    const subjectObj = x ? parseDNString(subjectRaw) : (cur.subject || {});
    const issuerObj = x ? parseDNString(issuerRaw) : (cur.issuer || {});

    const validFrom = x ? x.validFrom : cur.valid_from;
    const validTo = x ? x.validTo : cur.valid_to;
    const validFromMs = Date.parse(validFrom) || null;
    const validToMs = Date.parse(validTo) || null;
    const now = Date.now();
    const daysRemaining = validToMs ? Math.floor((validToMs - now) / 86400000) : null;
    const isExpired = validToMs ? now > validToMs : false;
    const isNotYetValid = validFromMs ? now < validFromMs : false;

    let selfSigned = false;
    if (x) {
      try {
        selfSigned = x.subject === x.issuer && x.verify(x.publicKey);
      } catch {
        selfSigned = x.subject === x.issuer;
      }
    } else if (cur.issuerCertificate === cur) {
      selfSigned = true;
    }

    const sanList = parseAltNames(x ? x.subjectAltName : cur.subjectaltname);

    chain.push({
      role: depth === 0 ? 'server' : (selfSigned ? 'root' : 'intermediate'),
      subject: subjectObj,
      subjectRaw,
      subjectCN: subjectObj.CN || subjectObj.O || '(CNなし)',
      issuer: issuerObj,
      issuerRaw,
      issuerCN: issuerObj.CN || issuerObj.O || '(不明)',
      validFrom,
      validTo,
      validFromISO: validFromMs ? new Date(validFromMs).toISOString() : null,
      validToISO: validToMs ? new Date(validToMs).toISOString() : null,
      daysRemaining,
      isExpired,
      isNotYetValid,
      san: sanList,
      serialNumber: (x ? x.serialNumber : cur.serialNumber) || '',
      fingerprint: cur.fingerprint || '',
      fingerprint256: (x ? x.fingerprint256 : cur.fingerprint256) || '',
      fingerprint512: (x ? x.fingerprint512 : cur.fingerprint512) || '',
      keyInfo: x ? formatKeyInfo(x.publicKey) : (cur.bits ? `${cur.bits} bit` : '不明'),
      ca: x ? Boolean(x.ca) : depth > 0,
      keyUsage: x && Array.isArray(x.keyUsage) ? x.keyUsage : [],
      selfSigned,
    });

    const next = cur.issuerCertificate;
    if (!next || next === cur) break;
    cur = next;
    depth++;
  }

  return chain;
}

function analyzeLinkUrl(rawUrl, linkText) {
  let parsed;
  try {
    parsed = new URL(rawUrl);
  } catch (e) {
    return { validSyntax: false, error: `無効な URL 形式: ${e.message}` };
  }

  const protocol = parsed.protocol.toLowerCase();
  const asciiHost = parsed.hostname || '';
  const unicodeHost = asciiHost ? urlMod.domainToUnicode(asciiHost) : '';
  const isPunycode = asciiHost !== unicodeHost || asciiHost.split('.').some(l => l.startsWith('xn--'));
  const hasUserinfo = Boolean(parsed.username || parsed.password);
  const isIPLiteral = Boolean(asciiHost && net.isIP(asciiHost));
  const defaultPort = protocol === 'https:' ? 443 : protocol === 'http:' ? 80 : null;
  const port = parsed.port ? parseInt(parsed.port, 10) : defaultPort;
  const isNonStandardPort = Boolean(parsed.port && defaultPort && port !== defaultPort);

  const warnings = [];
  if (hasUserinfo) {
    warnings.push({
      code: 'USERINFO_IN_URL',
      severity: 'danger',
      message: `URL に認証情報 (${parsed.username}@...) が含まれています。接続先ドメインを誤認させる偽装リンクの可能性があります。`,
    });
  }
  if (isPunycode) {
    warnings.push({
      code: 'IDN_PUNYCODE',
      severity: 'warn',
      message: `国際化ドメイン (Punycode: ${asciiHost} / Unicode: ${unicodeHost}) です。見た目と異なるドメインへの誘導（ホモグラフ攻撃）にご注意ください。`,
    });
  }
  if (isIPLiteral) {
    warnings.push({
      code: 'IP_LITERAL',
      severity: 'warn',
      message: `ドメイン名ではなく IP アドレス (${asciiHost}) への直接リンクです。`,
    });
  }
  if (isNonStandardPort) {
    warnings.push({
      code: 'NON_STANDARD_PORT',
      severity: 'info',
      message: `標準外のポート番号 (${port}) が指定されています。`,
    });
  }

  // Check if visible linkText itself looks like a URL or domain pointing to a different host
  let linkTextMismatch = null;
  const trimmedText = String(linkText || '').trim();
  if (trimmedText && (protocol === 'http:' || protocol === 'https:')) {
    const urlLike = trimmedText.match(/^(?:https?:\/\/)?([a-z0-9.-]+\.[a-z]{2,})(?::\d+)?(?:[/?#]|$)/i);
    if (urlLike) {
      const shownHost = urlLike[1].toLowerCase().replace(/^www\./, '');
      const actualHost = asciiHost.toLowerCase().replace(/^www\./, '');
      if (shownHost && actualHost && shownHost !== actualHost && !actualHost.endsWith('.' + shownHost)) {
        linkTextMismatch = { shownHost, actualHost };
        warnings.push({
          code: 'LINK_TEXT_MISMATCH',
          severity: 'danger',
          message: `リンク表示文字列 (${shownHost}) と実際の接続先ドメイン (${asciiHost}) が一致しません！フィッシングリンクの疑いがあります。`,
        });
      }
    }
  }

  return {
    validSyntax: true,
    rawUrl,
    href: parsed.href,
    protocol,
    hostname: asciiHost,
    unicodeHostname: unicodeHost,
    isPunycode,
    port,
    pathname: parsed.pathname + parsed.search,
    hasUserinfo,
    userinfo: hasUserinfo ? `${parsed.username}${parsed.password ? ':****' : ''}` : null,
    isIPLiteral,
    isNonStandardPort,
    linkText: trimmedText,
    linkTextMismatch,
    warnings,
  };
}

async function resolveDNS(hostname) {
  if (!hostname) return { addresses: [], dnsMs: 0 };
  if (net.isIP(hostname)) {
    return {
      addresses: [{ address: hostname, family: net.isIPv6(hostname) ? 6 : 4, isPrivate: isPrivateOrLoopbackIP(hostname) }],
      dnsMs: 0,
    };
  }
  const t0 = Date.now();
  try {
    const res = await dns.promises.lookup(hostname, { all: true });
    return {
      addresses: res.map(r => ({
        address: r.address,
        family: r.family,
        isPrivate: isPrivateOrLoopbackIP(r.address),
      })),
      dnsMs: Date.now() - t0,
    };
  } catch (err) {
    return {
      addresses: [],
      dnsMs: Date.now() - t0,
      error: err.code ? `${err.code}: ${err.message}` : err.message,
    };
  }
}

function inspectTLS(hostname, port, connectAddress) {
  return new Promise((resolve) => {
    const t0 = Date.now();
    const ca = getCombinedCAs();
    let settled = false;

    const finish = (payload) => {
      if (settled) return;
      settled = true;
      resolve(payload);
    };

    const isIP = Boolean(net.isIP(hostname));
    const opts = {
      host: connectAddress || hostname,
      port: port || 443,
      servername: isIP ? undefined : hostname,
      rejectUnauthorized: false,
      ca,
      ALPNProtocols: ['h2', 'http/1.1'],
    };

    const socket = tls.connect(opts, () => {
      const handshakeMs = Date.now() - t0;
      try {
        const peer = socket.getPeerCertificate(true);
        const chain = peer && (peer.raw || peer.fingerprint256) ? extractCertDetails(peer) : [];
        const authorized = Boolean(socket.authorized);
        const authorizationError = socket.authorizationError || null;
        let hostnameCheckError = null;
        if (peer && (peer.raw || peer.subjectaltname)) {
          const idErr = tls.checkServerIdentity(hostname, peer);
          if (idErr) hostnameCheckError = idErr.message;
        }
        const cipher = socket.getCipher ? socket.getCipher() : null;
        const protocol = socket.getProtocol ? socket.getProtocol() : null;
        const alpnProtocol = socket.alpnProtocol || null;

        socket.end();
        finish({
          ok: true,
          handshakeMs,
          authorized,
          authorizationError,
          hostnameCheckError,
          protocol,
          cipher: cipher ? { name: cipher.standardName || cipher.name, version: cipher.version } : null,
          alpnProtocol,
          remoteAddress: socket.remoteAddress,
          remotePort: socket.remotePort,
          chain,
        });
      } catch (e) {
        try { socket.destroy(); } catch {}
        finish({
          ok: false,
          handshakeMs,
          error: e.message || String(e),
        });
      }
    });

    socket.setTimeout(8000, () => {
      try { socket.destroy(); } catch {}
      finish({
        ok: false,
        handshakeMs: Date.now() - t0,
        error: 'ETIMEDOUT: TLS ハンドシェイクがタイムアウトしました (8秒)',
      });
    });

    socket.on('error', (err) => {
      try { socket.destroy(); } catch {}
      finish({
        ok: false,
        handshakeMs: Date.now() - t0,
        errorCode: err.code || null,
        error: err.code ? `${err.code}: ${err.message}` : err.message,
      });
    });
  });
}

function singleHeadRequest(targetUrl) {
  return new Promise((resolve) => {
    let parsed;
    try { parsed = new URL(targetUrl); } catch (e) {
      return resolve({ ok: false, error: e.message });
    }
    const mod = parsed.protocol === 'https:' ? https : http;
    const req = mod.request(parsed, { method: 'HEAD', timeout: 6000, rejectUnauthorized: false }, (res) => {
      res.resume();
      resolve({
        ok: true,
        statusCode: res.statusCode,
        location: res.headers.location || null,
      });
    });
    req.on('timeout', () => {
      req.destroy();
      resolve({ ok: false, error: 'ETIMEDOUT (HEAD)' });
    });
    req.on('error', (err) => {
      resolve({ ok: false, error: err.code ? `${err.code}: ${err.message}` : err.message });
    });
    req.end();
  });
}

async function traceRedirects(startUrl, maxHops = 5) {
  const hops = [];
  let current = startUrl;
  for (let i = 0; i < maxHops; i++) {
    const r = await singleHeadRequest(current);
    hops.push({ url: current, ...r });
    if (!r.ok || !r.location || r.statusCode < 300 || r.statusCode >= 400) break;
    try {
      current = new URL(r.location, current).href;
    } catch {
      break;
    }
  }
  return { hops, finalUrl: current };
}

function explainAuthError(code, detail) {
  const map = {
    ERR_TLS_CERT_ALTNAME_INVALID: '証明書の対象ドメイン (SAN/CN) がリンク先ホスト名と一致しません。',
    CERT_HAS_EXPIRED: '証明書の有効期限が切れています。',
    CERT_NOT_YET_VALID: '証明書の有効期間開始前です。',
    DEPTH_ZERO_SELF_SIGNED_CERT: '自己署名証明書（オレオレ証明書）であり、信頼された認証局で署名されていません。',
    SELF_SIGNED_CERT_IN_CHAIN: '証明書チェーン内に信頼されない自己署名ルート証明書が含まれています。',
    UNABLE_TO_VERIFY_LEAF_SIGNATURE: '中間証明書の不足または未知の認証局のためチェーンを検証できません。',
    UNABLE_TO_GET_ISSUER_CERT_LOCALLY: 'ローカルの信頼ストアで発行者証明書が見つかりません。',
    CERT_REVOKED: '証明書が失効しています。',
  };
  const base = map[code] || `証明書検証エラー (${code || 'unknown'})`;
  return detail ? `${base} (${detail})` : base;
}

function buildVerdict(urlInfo, dnsInfo, tlsInfo) {
  const warnings = [...(urlInfo.warnings || [])];
  const errors = [];

  if (!urlInfo.validSyntax) {
    return { level: 'error', title: 'URL 形式が不正です', summary: urlInfo.error, warnings, errors: [urlInfo.error] };
  }

  if (urlInfo.protocol === 'javascript:' || urlInfo.protocol === 'data:') {
    return {
      level: 'danger',
      title: `危険なスキーム (${urlInfo.protocol})`,
      summary: 'スクリプト実行またはインラインデータを直接開くリンクです。TLS 証明書はありません。',
      warnings,
      errors: [],
    };
  }

  if (urlInfo.protocol !== 'http:' && urlInfo.protocol !== 'https:') {
    return {
      level: 'info',
      title: `非 HTTP スキーム (${urlInfo.protocol})`,
      summary: 'Web 通信ではないため TLS 証明書検証の対象外です。',
      warnings,
      errors: [],
    };
  }

  if (dnsInfo?.addresses?.some(a => a.isPrivate)) {
    warnings.push({
      code: 'PRIVATE_IP',
      severity: 'warn',
      message: '名前解決先がプライベート IP / ループバックアドレスです。',
    });
  }

  if (urlInfo.protocol === 'http:' && !tlsInfo) {
    return {
      level: 'warn',
      title: 'HTTP（暗号化なし・TLS 証明書なし）',
      summary: '通信が暗号化されず、サーバー身元を証明する TLS 証明書は存在しません。',
      warnings,
      errors,
    };
  }

  if (dnsInfo?.error && (!tlsInfo || !tlsInfo.ok)) {
    errors.push(`DNS 名前解決に失敗しました: ${dnsInfo.error}`);
    return {
      level: 'error',
      title: 'DNS 名前解決に失敗',
      summary: `${urlInfo.hostname} の IP アドレスを取得できませんでした。`,
      warnings,
      errors,
    };
  }

  if (!tlsInfo || !tlsInfo.ok) {
    const errText = tlsInfo?.error || 'TLS 接続に失敗しました';
    errors.push(errText);
    return {
      level: 'error',
      title: 'TLS 接続エラー',
      summary: errText,
      warnings,
      errors,
    };
  }

  const leaf = tlsInfo.chain?.[0];
  if (leaf) {
    if (leaf.isExpired) {
      errors.push(`有効期限切れ (${leaf.validToISO || leaf.validTo})`);
    } else if (leaf.isNotYetValid) {
      errors.push(`有効期間開始前 (${leaf.validFromISO || leaf.validFrom})`);
    } else if (typeof leaf.daysRemaining === 'number' && leaf.daysRemaining <= 30) {
      warnings.push({
        code: 'EXPIRING_SOON',
        severity: 'warn',
        message: `証明書の有効期限まで残り ${leaf.daysRemaining} 日です。`,
      });
    }
  }

  if (tlsInfo.protocol && (tlsInfo.protocol === 'TLSv1' || tlsInfo.protocol === 'TLSv1.1')) {
    warnings.push({
      code: 'LEGACY_TLS',
      severity: 'warn',
      message: `非推奨の古いプロトコル (${tlsInfo.protocol}) が使用されています。`,
    });
  }

  if (!tlsInfo.authorized) {
    const authCode = tlsInfo.authorizationError || (leaf?.isExpired ? 'CERT_HAS_EXPIRED' : 'VERIFY_FAILED');
    const detail = authCode === 'ERR_TLS_CERT_ALTNAME_INVALID' ? tlsInfo.hostnameCheckError : '';
    errors.push(explainAuthError(authCode, detail));
  }

  if (errors.length > 0) {
    return {
      level: 'danger',
      title: 'TLS 証明書の検証に失敗しました',
      summary: errors[0],
      warnings,
      errors,
    };
  }

  const hasDangerWarn = warnings.some(w => w.severity === 'danger');
  if (hasDangerWarn) {
    return {
      level: 'danger',
      title: '証明書は有効ですが URL に重大な警告があります',
      summary: warnings.find(w => w.severity === 'danger').message,
      warnings,
      errors,
    };
  }

  if (warnings.length > 0) {
    return {
      level: 'warn',
      title: 'TLS 証明書は有効（注意事項あり）',
      summary: warnings[0].message,
      warnings,
      errors,
    };
  }

  return {
    level: 'valid',
    title: '有効な TLS 証明書を確認',
    summary: `${leaf?.subjectCN || urlInfo.hostname} — ${leaf?.issuerCN || '信頼された CA'} による署名・ホスト名一致・有効期限内`,
    warnings,
    errors,
  };
}

async function verifyLink(msg) {
  const rawUrl = String(msg.url || '').trim();
  const linkText = String(msg.linkText || '');
  const probeHttps = Boolean(msg.probeHttps);
  const followRedirects = Boolean(msg.followRedirects);

  let targetUrl = rawUrl;
  let redirectTrace = null;
  if (followRedirects && /^https?:\/\//i.test(rawUrl)) {
    redirectTrace = await traceRedirects(rawUrl, 5);
    if (redirectTrace.finalUrl) {
      targetUrl = redirectTrace.finalUrl;
    }
  }

  const urlInfo = analyzeLinkUrl(targetUrl, linkText);
  if (!urlInfo.validSyntax) {
    return {
      inspectedAt: new Date().toISOString(),
      originalUrl: rawUrl,
      redirectTrace,
      urlInfo,
      dnsInfo: null,
      tlsInfo: null,
      verdict: buildVerdict(urlInfo, null, null),
    };
  }

  if (urlInfo.protocol !== 'http:' && urlInfo.protocol !== 'https:') {
    return {
      inspectedAt: new Date().toISOString(),
      originalUrl: rawUrl,
      redirectTrace,
      urlInfo,
      dnsInfo: null,
      tlsInfo: null,
      verdict: buildVerdict(urlInfo, null, null),
    };
  }

  const dnsInfo = await resolveDNS(urlInfo.hostname);
  let tlsInfo = null;

  if (urlInfo.protocol === 'https:' || probeHttps) {
    const tlsPort = urlInfo.protocol === 'https:' ? urlInfo.port : 443;
    const connectIp = dnsInfo.addresses?.[0]?.address || null;
    tlsInfo = await inspectTLS(urlInfo.hostname, tlsPort, connectIp);
  }

  const verdict = buildVerdict(urlInfo, dnsInfo, tlsInfo);
  return {
    inspectedAt: new Date().toISOString(),
    originalUrl: rawUrl,
    probedHttpsFromHttp: urlInfo.protocol === 'http:' && probeHttps,
    redirectTrace,
    urlInfo,
    dnsInfo,
    tlsInfo,
    verdict,
  };
}

async function handleMessage(msg) {
  const id = msg?.id;
  const action = msg?.action;
  try {
    if (action === 'ping') {
      return { id, ok: true, action, nodeVersion: process.version, hostPid: process.pid };
    }
    if (action === 'verifyLink') {
      const result = await verifyLink(msg);
      return { id, ok: true, action, result };
    }
    throw new Error(`Unknown action: ${action}`);
  } catch (err) {
    return { id, ok: false, action, error: err?.message || String(err) };
  }
}

if (require.main === module) {
  let inBuf = Buffer.alloc(0);
  let pending = 0;
  let stdinEnded = false;

  const maybeExit = () => {
    if (stdinEnded && pending === 0) process.exit(0);
  };

  process.stdin.on('data', (chunk) => {
    inBuf = Buffer.concat([inBuf, chunk]);
    while (inBuf.length >= 4) {
      const msgLen = inBuf.readUInt32LE(0);
      if (msgLen > 10 * 1024 * 1024) {
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
}

module.exports = { verifyLink, analyzeLinkUrl, inspectTLS, handleMessage };
