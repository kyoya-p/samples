// Chrome / Edge Native Messaging Host (com.kyoya.link_cert_verifier) を HKCU レジストリに登録/解除する
const fs = require('fs');
const path = require('path');
const crypto = require('crypto');
const { execFileSync } = require('child_process');

const HOST_NAME = 'com.kyoya.link_cert_verifier';
const HOST_DIR = __dirname;
const MANIFEST_JSON = path.join(HOST_DIR, `${HOST_NAME}.json`);
const HOST_CMD = path.join(HOST_DIR, 'host.cmd');
const EXT_MANIFEST = path.resolve(HOST_DIR, '..', 'manifest.json');

const REG_KEYS = [
  `HKCU\\Software\\Google\\Chrome\\NativeMessagingHosts\\${HOST_NAME}`,
  `HKCU\\Software\\Microsoft\\Edge\\NativeMessagingHosts\\${HOST_NAME}`,
];

function computeExtensionIdFromKey(keyB64) {
  const der = Buffer.from(keyB64, 'base64');
  const hash = crypto.createHash('sha256').update(der).digest('hex').slice(0, 32);
  return [...hash].map(c => String.fromCharCode(97 + parseInt(c, 16))).join('');
}

function main() {
  const args = process.argv.slice(2);
  if (args.includes('--uninstall')) {
    for (const regKey of REG_KEYS) {
      try {
        execFileSync('reg', ['delete', regKey, '/f'], { stdio: 'pipe' });
        console.log('Removed:', regKey);
      } catch {
        console.log('Not present:', regKey);
      }
    }
    return;
  }

  const extManifest = JSON.parse(fs.readFileSync(EXT_MANIFEST, 'utf8'));
  if (!extManifest.key) {
    throw new Error(`"key" field not found in ${EXT_MANIFEST}`);
  }
  const extId = computeExtensionIdFromKey(extManifest.key);
  const origins = [`chrome-extension://${extId}/`];
  console.log('Extension ID:', extId);

  for (let i = 0; i < args.length; i++) {
    if (args[i] === '--extra-id' && args[i + 1]) {
      origins.push(`chrome-extension://${args[i + 1]}/`);
      i++;
    }
  }

  const hostManifest = {
    name: HOST_NAME,
    description: 'LinkCertVerifier Native Messaging Host (TLS Certificate & Link Inspector)',
    path: HOST_CMD,
    type: 'stdio',
    allowed_origins: [...new Set(origins)],
  };

  fs.writeFileSync(MANIFEST_JSON, JSON.stringify(hostManifest, null, 2) + '\n', 'utf8');
  console.log('Written host manifest:', MANIFEST_JSON);

  for (const regKey of REG_KEYS) {
    execFileSync('reg', ['add', regKey, '/ve', '/t', 'REG_SZ', '/d', MANIFEST_JSON, '/f'], { stdio: 'inherit' });
    console.log('Registered:', regKey);
  }
}

if (require.main === module) {
  main();
}

module.exports = { computeExtensionIdFromKey, HOST_NAME, MANIFEST_JSON, HOST_CMD };
