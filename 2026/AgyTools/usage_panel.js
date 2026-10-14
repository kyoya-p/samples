// usage_samples.jsonl からデータを読み込み、agy-panel.html を生成する
const fs = require('fs'), path = require('path');

const samplesPath = path.join(__dirname, 'usage_samples.jsonl');
const samples = fs.existsSync(samplesPath)
  ? fs.readFileSync(samplesPath, 'utf8').trim().split('\n').filter(Boolean).map(l => JSON.parse(l))
  : [];

const lastSample = samples[samples.length - 1] || null;

const data = {
  generated: new Date().toISOString(),
  samples,
  lastSample
};

const html = `<!doctype html>
<html lang="ja">
<head>
  <meta charset="utf-8">
  <meta name="viewport" content="width=device-width,initial-scale=1">
  <title>Antigravity Usage Panel</title>
  <style>
    :root {
      --bg: #fafaf7;
      --card: #ffffff;
      --fg: #1d1d1b;
      --mut: #6b6b65;
      --bar: #1a73e8;
      --bar-claude: #d08c4a;
      --line: #e3e2dc;
      --green: #1e8e3e;
      --yellow: #f9ab00;
      --red: #d93025;
    }
    @media (prefers-color-scheme: dark) {
      :root {
        --bg: #171816;
        --card: #20211d;
        --fg: #ecebe6;
        --mut: #9a9a92;
        --bar: #8ab4f8;
        --bar-claude: #f09d5c;
        --line: #34352f;
      }
    }
    body {
      margin: 0;
      padding: 16px;
      background: var(--bg);
      color: var(--fg);
      font: 14px/1.5 system-ui, -apple-system, "Segoe UI", Roboto, sans-serif;
    }
    h1 { font-size: 20px; margin: 0 0 4px; display: flex; align-items: center; gap: 8px; }
    .mut { color: var(--mut); font-size: 12px; }
    .grid { display: grid; gap: 14px; grid-template-columns: repeat(auto-fit, minmax(320px, 1fr)); margin-top: 14px; }
    .card { background: var(--card); border: 1px solid var(--line); border-radius: 10px; padding: 16px; }
    .ch { display: flex; justify-content: space-between; align-items: center; margin: 0 0 12px; }
    .title { font-size: 14px; font-weight: 600; display: flex; align-items: center; gap: 6px; }
    .rt { display: flex; align-items: baseline; gap: 6px; }
    .now { font-size: 26px; font-weight: 700; font-variant-numeric: tabular-nums; }
    .remain-label { font-size: 11px; color: var(--mut); }
    .badge { padding: 2px 6px; border-radius: 4px; font-size: 11px; font-weight: 500; }
    .sw { width: 10px; height: 10px; border-radius: 2px; display: inline-block; }
    .track { background: var(--line); border-radius: 4px; height: 10px; overflow: hidden; margin-top: 6px; }
    .fill { height: 100%; border-radius: 4px; transition: width .3s; }
    .reset-info { font-size: 12px; color: var(--mut); margin-top: 8px; display: flex; justify-content: space-between; }
    .range-select { font: 12px system-ui; padding: 2px 6px; border: 1px solid var(--line); border-radius: 6px; background: var(--card); color: var(--fg); }
    svg text { font-family: system-ui, sans-serif; }
  </style>
</head>
<body>
  <h1><span>⚡ Antigravity Usage Panel</span></h1>
  <div class="mut">最終更新: <span id="gen-time"></span></div>

  <div class="grid">
    <!-- Gemini 5h -->
    <div class="card">
      <div class="ch">
        <span class="title"><i class="sw" style="background:var(--bar)"></i>Gemini Models (5時間枠)</span>
        <div class="rt">
          <span class="remain-label">残量:</span>
          <b class="now" id="val-gemini-5h" style="color:var(--bar)">--%</b>
        </div>
      </div>
      <div class="track"><div class="fill" id="bar-gemini-5h" style="background:var(--bar); width:0%"></div></div>
      <div class="reset-info"><span>リセット予定:</span><b id="rst-gemini-5h">--</b></div>
      <div style="margin-top:14px;" id="chart-gemini-5h"></div>
    </div>

    <!-- Gemini Weekly -->
    <div class="card">
      <div class="ch">
        <span class="title"><i class="sw" style="background:var(--bar)"></i>Gemini Models (週次)</span>
        <div class="rt">
          <span class="remain-label">残量:</span>
          <b class="now" id="val-gemini-week" style="color:var(--bar)">--%</b>
        </div>
      </div>
      <div class="track"><div class="fill" id="bar-gemini-week" style="background:var(--bar); width:0%"></div></div>
      <div class="reset-info"><span>リセット予定:</span><b id="rst-gemini-week">--</b></div>
      <div style="margin-top:14px;" id="chart-gemini-week"></div>
    </div>

    <!-- Claude & GPT 5h -->
    <div class="card">
      <div class="ch">
        <span class="title"><i class="sw" style="background:var(--bar-claude)"></i>Claude & GPT (5時間枠)</span>
        <div class="rt">
          <span class="remain-label">残量:</span>
          <b class="now" id="val-claude-5h" style="color:var(--bar-claude)">--%</b>
        </div>
      </div>
      <div class="track"><div class="fill" id="bar-claude-5h" style="background:var(--bar-claude); width:0%"></div></div>
      <div class="reset-info"><span>リセット予定:</span><b id="rst-claude-5h">--</b></div>
      <div style="margin-top:14px;" id="chart-claude-5h"></div>
    </div>

    <!-- Claude & GPT Weekly -->
    <div class="card">
      <div class="ch">
        <span class="title"><i class="sw" style="background:var(--bar-claude)"></i>Claude & GPT (週次)</span>
        <div class="rt">
          <span class="remain-label">残量:</span>
          <b class="now" id="val-claude-week" style="color:var(--bar-claude)">--%</b>
        </div>
      </div>
      <div class="track"><div class="fill" id="bar-claude-week" style="background:var(--bar-claude); width:0%"></div></div>
      <div class="reset-info"><span>リセット予定:</span><b id="rst-claude-week">--</b></div>
      <div style="margin-top:14px;" id="chart-claude-week"></div>
    </div>
  </div>

<script>
  const D = ${JSON.stringify(data)};
  const S = D.samples || [];
  const pad = n => String(n).padStart(2, '0');
  const fmtD = d => pad(d.getMonth()+1) + '/' + pad(d.getDate()) + ' ' + pad(d.getHours()) + ':' + pad(d.getMinutes());

  if (D.generated) {
    document.getElementById('gen-time').textContent = new Date(D.generated).toLocaleString('ja-JP');
  }

  const targets = [
    { key: 'gemini_5h', valId: 'val-gemini-5h', barId: 'bar-gemini-5h', rstId: 'rst-gemini-5h', chartId: 'chart-gemini-5h', col: 'var(--bar)' },
    { key: 'gemini_week', valId: 'val-gemini-week', barId: 'bar-gemini-week', rstId: 'rst-gemini-week', chartId: 'chart-gemini-week', col: 'var(--bar)' },
    { key: 'claude_gpt_5h', valId: 'val-claude-5h', barId: 'bar-claude-5h', rstId: 'rst-claude-5h', chartId: 'chart-claude-5h', col: 'var(--bar-claude)' },
    { key: 'claude_gpt_week', valId: 'val-claude-week', barId: 'bar-claude-week', rstId: 'rst-claude-week', chartId: 'chart-claude-week', col: 'var(--bar-claude)' },
  ];

  const last = D.lastSample?.limits || {};

  targets.forEach(t => {
    const item = last[t.key];
    if (!item) return;
    const pct = item.pct;
    document.getElementById(t.valId).textContent = pct + '%';
    document.getElementById(t.barId).style.width = pct + '%';
    if (item.reset) {
      document.getElementById(t.rstId).textContent = new Date(item.reset).toLocaleString('ja-JP');
    }
  });

  // 簡易折れ線グラフのレンダリング
  function renderMiniChart(t) {
    const el = document.getElementById(t.chartId);
    if (!el || S.length === 0) return;
    const W = 320, H = 100, P = 24;
    const pts = S.map(s => {
      return { t: Date.parse(s.ts), v: s.limits?.[t.key]?.pct ?? 100 };
    });

    const minT = Math.min(...pts.map(p => p.t));
    const maxT = Math.max(...pts.map(p => p.t), Date.now());
    const spanT = Math.max(1, maxT - minT);

    const x = ms => P + ((ms - minT) / spanT) * (W - 2 * P);
    const y = v => H - P - (v / 100) * (H - 2 * P);

    const polyPoints = pts.map(p => x(p.t).toFixed(1) + ',' + y(p.v).toFixed(1)).join(' ');
    const lineSvg = '<polyline fill="none" stroke="' + t.col + '" stroke-width="2" points="' + polyPoints + '"/>';
    const circlesSvg = pts.map(p => '<circle cx="' + x(p.t).toFixed(1) + '" cy="' + y(p.v).toFixed(1) + '" r="2" fill="' + t.col + '"/>').join('');

    const gridSvg = [0, 50, 100].map(v => 
      '<line x1="' + P + '" x2="' + (W - P) + '" y1="' + y(v) + '" y2="' + y(v) + '" stroke="var(--line)"/><text x="' + (P - 4) + '" y="' + (y(v) + 3) + '" font-size="9" text-anchor="end" fill="var(--mut)">' + v + '%</text>'
    ).join('');

    el.innerHTML = '<svg viewBox="0 0 ' + W + ' ' + H + '" width="100%">' + gridSvg + lineSvg + circlesSvg + '</svg>';
  }

  targets.forEach(renderMiniChart);
</script>
</body>
</html>`;

fs.writeFileSync(path.join(__dirname, 'agy-panel.html'), html, 'utf8');
console.log('written', path.join(__dirname, 'agy-panel.html'));
