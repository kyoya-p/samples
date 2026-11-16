# 🤖 AI生成: ダッシュボード配信 + 銘柄追加API
# 起動: mise exec -- python server.py [port]  →  http://localhost:8765/sharp_dashboard.html
# POST /api/add?code=7203 : yfinanceで全期間採取 → tickers.txt追記 → build_dashboard.py再実行
import json
import re
import subprocess
import sys
from http.server import SimpleHTTPRequestHandler, ThreadingHTTPServer
from pathlib import Path
from urllib.parse import urlparse, parse_qs

import yfinance as yf

from fetch_stock import fetch, CONFIG

ROOT = Path(__file__).parent
PORT = int(sys.argv[1]) if len(sys.argv) > 1 else 8765


def add_to_config(code: str, name: str) -> None:
    text = CONFIG.read_text(encoding='utf-8') if CONFIG.exists() else ''
    if re.search(rf'^\s*{code}(\.T)?\b', text, re.M):
        return
    with CONFIG.open('a', encoding='utf-8') as f:
        f.write(('' if text.endswith('\n') or not text else '\n') + f'{code}  # {name}\n')


def add_stock(raw: str) -> dict:
    code = raw.strip().upper().removesuffix('.T')
    if not re.fullmatch(r'\d{4}', code):
        return {'ok': False, 'error': '銘柄コードは4桁の数字で入力してください'}
    if fetch(code) is None:
        return {'ok': False, 'error': f'{code}: データを取得できませんでした'}
    try:
        info = yf.Ticker(f'{code}.T').info
        name = info.get('shortName') or info.get('longName') or ''
    except Exception:
        name = ''
    add_to_config(code, name)
    r = subprocess.run([sys.executable, 'build_dashboard.py'], cwd=ROOT, capture_output=True, text=True)
    if r.returncode != 0:
        return {'ok': False, 'error': 'ダッシュボード再生成に失敗: ' + r.stderr[-300:]}
    return {'ok': True, 'code': code, 'name': name}


class Handler(SimpleHTTPRequestHandler):
    def __init__(self, *a, **kw):
        super().__init__(*a, directory=str(ROOT), **kw)

    def do_POST(self):
        u = urlparse(self.path)
        if u.path != '/api/add':
            self.send_error(404)
            return
        res = add_stock(parse_qs(u.query).get('code', [''])[0])
        body = json.dumps(res, ensure_ascii=False).encode('utf-8')
        self.send_response(200 if res['ok'] else 400)
        self.send_header('Content-Type', 'application/json; charset=utf-8')
        self.send_header('Content-Length', str(len(body)))
        self.end_headers()
        self.wfile.write(body)


if __name__ == '__main__':
    print(f'http://localhost:{PORT}/sharp_dashboard.html')
    ThreadingHTTPServer(('127.0.0.1', PORT), Handler).serve_forever()
