import json

print("Reading stock data...")
with open('sharp_data.json', 'r', encoding='utf-8') as f:
    raw_data = json.load(f)

# raw_data: [[date, open, high, low, close, volume], ...]
html_template = f'''<!DOCTYPE html>
<html lang="ja">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>シャープ【6753】カレンダー日等間隔ローソク足＆周期逆算重畳ダッシュボード</title>
  <!-- TradingView Lightweight Charts (Candlestick) -->
  <script src="https://unpkg.com/lightweight-charts@4.1.3/dist/lightweight-charts.standalone.production.js"></script>
  <!-- Chart.js & Zoom for Frequency Domain -->
  <script src="https://cdn.jsdelivr.net/npm/chart.js@4.4.1/dist/chart.umd.min.js"></script>
  <script src="https://cdn.jsdelivr.net/npm/hammerjs@2.0.8/hammer.min.js"></script>
  <script src="https://cdn.jsdelivr.net/npm/chartjs-plugin-zoom@2.0.1/dist/chartjs-plugin-zoom.min.js"></script>
  <style>
    :root {{
      --bg-color: #0b1120;
      --card-bg: #1e293b;
      --card-border: #334155;
      --text-main: #f8fafc;
      --text-muted: #94a3b8;
      --accent-blue: #38bdf8;
      --accent-teal: #14b8a6;
      --accent-green: #22c55e;
      --accent-red: #ef4444;
      --accent-purple: #c084fc;
      --accent-amber: #fbbf24;
      --accent-rose: #f43f5e;
    }}

    * {{
      box-sizing: border-box;
      margin: 0;
      padding: 0;
      font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "Helvetica Neue", Arial, sans-serif;
    }}

    body {{
      background-color: var(--bg-color);
      color: var(--text-main);
      padding: 1rem;
      display: flex;
      flex-direction: column;
      gap: 1rem;
      min-height: 100vh;
    }}

    .chart-container-card {{
      background-color: var(--card-bg);
      border: 1px solid var(--card-border);
      border-radius: 12px;
      padding: 1.25rem;
      display: flex;
      flex-direction: column;
      gap: 1rem;
    }}

    .card-header-row {{
      display: flex;
      justify-content: space-between;
      align-items: center;
      flex-wrap: wrap;
      gap: 0.75rem;
      border-bottom: 1px solid rgba(255,255,255,0.06);
      padding-bottom: 0.75rem;
    }}

    .section-title {{
      font-size: 1.1rem;
      font-weight: 700;
      display: flex;
      align-items: center;
      gap: 0.5rem;
    }}

    .toolbar {{
      display: flex;
      justify-content: space-between;
      align-items: center;
      flex-wrap: wrap;
      gap: 0.75rem;
    }}

    .button-group {{
      display: flex;
      gap: 0.35rem;
      background: #0f172a;
      padding: 0.25rem;
      border-radius: 8px;
      border: 1px solid var(--card-border);
    }}

    button.btn {{
      background: transparent;
      border: none;
      color: var(--text-muted);
      padding: 0.4rem 0.8rem;
      font-size: 0.85rem;
      font-weight: 600;
      border-radius: 6px;
      cursor: pointer;
      transition: all 0.2s;
    }}

    button.btn:hover {{
      color: var(--text-main);
      background: rgba(255, 255, 255, 0.08);
    }}

    button.btn.active {{
      background: #2563eb;
      color: #fff;
    }}

    .options-group {{
      display: flex;
      align-items: center;
      gap: 0.8rem;
      flex-wrap: wrap;
    }}

    .toggle-label {{
      display: flex;
      align-items: center;
      gap: 0.4rem;
      font-size: 0.85rem;
      color: var(--text-muted);
      cursor: pointer;
    }}

    .toggle-label input {{
      cursor: pointer;
      accent-color: var(--accent-blue);
    }}

    .ohlc-bar {{
      display: flex;
      align-items: center;
      gap: 1.2rem;
      font-size: 0.85rem;
      background: #0f172a;
      padding: 0.5rem 1rem;
      border-radius: 8px;
      border: 1px solid var(--card-border);
      flex-wrap: wrap;
    }}

    .ohlc-item {{
      display: flex;
      gap: 0.4rem;
    }}

    .ohlc-label {{
      color: var(--text-muted);
    }}

    .ohlc-val {{
      font-weight: 600;
    }}

    #tvChartWrapper {{
      position: relative;
      height: 520px;
      width: 100%;
      border-radius: 8px;
      overflow: hidden;
      background: #0b1120;
    }}

    .freq-chart-wrapper {{
      position: relative;
      height: 290px;
      width: 100%;
    }}

    .events-grid {{
      display: grid;
      grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
      gap: 0.8rem;
    }}

    .event-card {{
      background: #0f172a;
      border: 1px solid var(--card-border);
      border-radius: 8px;
      padding: 0.75rem;
      font-size: 0.8rem;
      cursor: pointer;
      transition: border-color 0.2s;
    }}

    .event-card:hover {{
      border-color: var(--accent-blue);
    }}

    .event-date {{
      font-weight: bold;
      color: var(--accent-blue);
      margin-bottom: 0.2rem;
    }}

    .event-desc {{
      color: var(--text-muted);
      line-height: 1.35;
    }}

    .hint-tag {{
      background: rgba(56, 189, 248, 0.1);
      border: 1px solid rgba(56, 189, 248, 0.3);
      color: var(--accent-blue);
      font-size: 0.75rem;
      padding: 0.2rem 0.5rem;
      border-radius: 4px;
      display: inline-flex;
      align-items: center;
      gap: 0.3rem;
    }}

    .sync-tag {{
      background: rgba(20, 184, 166, 0.12);
      border: 1px solid rgba(20, 184, 166, 0.4);
      color: #2dd4bf;
      font-size: 0.8rem;
      padding: 0.25rem 0.6rem;
      border-radius: 6px;
      font-weight: 600;
    }}

    .overlay-controls {{
      display: flex;
      align-items: center;
      gap: 0.6rem;
      background: rgba(244, 63, 94, 0.12);
      border: 1px solid rgba(244, 63, 94, 0.4);
      padding: 0.3rem 0.7rem;
      border-radius: 6px;
    }}
  </style>
</head>
<body>

  <!-- 時間領域：ローソク足チャート（逆算波重畳） -->
  <div class="chart-container-card">
    <div class="card-header-row">
      <div class="section-title">
        <span style="color: var(--accent-blue);">●</span> 時間領域（Time Domain）：ローソク足 ＆ 周期逆算（IFFT）重畳表示
      </div>

      <div class="options-group">
        <!-- ★ 周期スペクトル逆算波の重畳コントロール ★ -->
        <div class="overlay-controls">
          <label class="toggle-label" style="color: var(--accent-rose); font-weight: 700;">
            <input type="checkbox" id="toggleFourier" checked> 周期逆算波 (IFFT) を重ねる
          </label>
          <select id="selectHarmonicsCount" style="background:#0f172a; color:#f8fafc; border:1px solid rgba(244,63,94,0.5); border-radius:4px; padding:0.15rem 0.4rem; font-size:0.8rem; cursor:pointer;">
            <option value="3">上位3成分 (大局サイクル)</option>
            <option value="5" selected>上位5成分 (滑らかな近似)</option>
            <option value="10">上位10成分 (詳細近似)</option>
            <option value="20">上位20成分 (高周波追従)</option>
          </select>
          <label class="toggle-label" style="color: var(--accent-blue); font-size:0.8rem; margin-left:0.3rem;">
            <input type="checkbox" id="toggleTop1"> 第1主周期波も表示
          </label>
        </div>

        <div class="button-group" id="chartTypeButtons">
          <button class="btn active" data-type="candle">ローソク足</button>
          <button class="btn" data-type="line">ライン</button>
        </div>

        <label class="toggle-label">
          <input type="checkbox" id="toggleSMA25" checked> SMA25
        </label>
        <label class="toggle-label">
          <input type="checkbox" id="toggleSMA75" checked> SMA75
        </label>
        <label class="toggle-label">
          <input type="checkbox" id="toggleSMA200" checked> SMA200
        </label>
      </div>
    </div>

    <!-- OHLCV リアルタイム情報バー -->
    <div class="ohlc-bar" id="ohlcBar">
      <div class="ohlc-item"><span class="ohlc-label">日付:</span> <span class="ohlc-val" id="infoDate">2026-11-09</span></div>
      <div class="ohlc-item"><span class="ohlc-label">始値(O):</span> <span class="ohlc-val" id="infoOpen">541.0円</span></div>
      <div class="ohlc-item"><span class="ohlc-label">高値(H):</span> <span class="ohlc-val up" id="infoHigh">546.0円</span></div>
      <div class="ohlc-item"><span class="ohlc-label">安値(L):</span> <span class="ohlc-val down" id="infoLow">532.0円</span></div>
      <div class="ohlc-item"><span class="ohlc-label">終値(C):</span> <span class="ohlc-val" id="infoClose">538.0円</span></div>
      <div class="ohlc-item"><span class="ohlc-label">出来高(V):</span> <span class="ohlc-val" id="infoVolume">3,269,900株</span></div>
      <div class="ohlc-item"><span class="ohlc-label">前日比:</span> <span class="ohlc-val up" id="infoDiff">+9.0円 (+1.70%)</span></div>
      <div class="ohlc-item" id="infoIFFTItem" style="margin-left:auto; color: var(--accent-rose); font-weight: bold;">
        <span class="ohlc-label" style="color: var(--accent-rose);">IFFT逆算値:</span> <span id="infoIFFTVal">-- 円</span>
      </div>
    </div>

    <div class="toolbar">
      <div class="button-group" id="rangeButtons">
        <button class="btn" data-range="1M">1ヶ月</button>
        <button class="btn" data-range="6M">6ヶ月</button>
        <button class="btn" data-range="1Y">1年</button>
        <button class="btn" data-range="5Y">5年</button>
        <button class="btn" data-range="10Y">10年</button>
        <button class="btn active" data-range="ALL">全期間 (27y)</button>
      </div>
      <span class="hint-tag">🔍 マウスホイールで時間軸の伸縮 / ドラッグでスクロール（周期逆算波もリアルタイムに自動再計算・追従）</span>
    </div>

    <!-- TradingView ローソク足チャート本体 -->
    <div id="tvChartWrapper"></div>
  </div>

  <!-- ★周期スペクトルグラフ★ -->
  <div class="chart-container-card">
    <div class="card-header-row">
      <div class="section-title">
        <span style="color: var(--accent-teal);">●</span> 周期スペクトル（Period Spectrum）：振幅 [横軸：周期 $T$ (カレンダー日)]
      </div>
      <div class="options-group">
        <label class="toggle-label" style="color: var(--accent-teal); font-weight: 600;">
          <input type="checkbox" id="toggleLogX" checked> 横軸：対数 (Log X)
        </label>
        <label class="toggle-label" style="color: var(--accent-teal); font-weight: 600;">
          <input type="checkbox" id="toggleLogY"> 縦軸：対数 (Log Y / dB相当)
        </label>

        <button class="btn" id="btnResetFreqZoom" style="background: #334155; padding: 0.3rem 0.6rem; font-size: 0.8rem;">
          ズームリセット
        </button>
        <button class="btn" id="btnShortPeriodZoom" style="background: #0284c7; padding: 0.3rem 0.6rem; font-size: 0.8rem;">
          短期〜中期 (2〜120日)
        </button>
      </div>
    </div>

    <div style="display: flex; justify-content: space-between; align-items: center; flex-wrap: wrap; gap: 0.5rem;">
      <div class="sync-tag" id="visibleRangeStatus">
        ⚡ FFT解析対象：上のチャートの表示区間を自動追従中
      </div>
      <div style="font-size: 0.8rem; color: var(--text-muted);" id="scaleStatusText">
        横軸：周期 T (カレンダー日) ── 左端: 短期 (2日〜) ──→ 右端: 長期 (表示区間最大日数)
      </div>
    </div>

    <div class="freq-chart-wrapper">
      <canvas id="freqChart"></canvas>
    </div>
  </div>

  <!-- 重要歴史イベント -->
  <div class="chart-container-card">
    <div class="section-title" style="font-size: 0.95rem; color: var(--text-muted);">
      歴史的転換点（クリックで該当期間へズーム & 周期解析）
    </div>
    <div class="events-grid">
      <div class="event-card" onclick="zoomTo('2006-01-01', '2008-12-31')">
        <div class="event-date">2007年4月</div>
        <div class="event-desc"><strong>最高値 20,525円</strong>（AQUOS・亀山工場ブームの絶頂期）</div>
      </div>
      <div class="event-card" onclick="zoomTo('2011-01-01', '2013-12-31')">
        <div class="event-date">2012年10月</div>
        <div class="event-desc"><strong>液晶危機</strong>（円高・パネル供給過剰による経営悪化）</div>
      </div>
      <div class="event-card" onclick="zoomTo('2015-06-01', '2017-06-30')">
        <div class="event-date">2016年8月</div>
        <div class="event-desc"><strong>鴻海（Foxconn）買収</strong>（外資傘下入りによる再建開始）</div>
      </div>
      <div class="event-card" onclick="zoomTo('2017-01-01', '2018-06-30')">
        <div class="event-date">2017年9月</div>
        <div class="event-desc"><strong>10株→1株の株式併合</strong> & 東証一部復帰</div>
      </div>
      <div class="event-card" onclick="zoomTo('2025-01-01', '2026-11-09')">
        <div class="event-date">2026年11月</div>
        <div class="event-desc"><strong>最安値 527円</strong>（直近の安値水準更新）</div>
      </div>
    </div>
  </div>

  <script>
    const RAW_DATA = {json.dumps(raw_data)};

    function buildFullCalendarData(rawData) {{
      const map = new Map();
      rawData.forEach(d => map.set(d[0], d));

      const startParts = rawData[0][0].split('-').map(Number);
      const endParts = rawData[rawData.length - 1][0].split('-').map(Number);

      const cur = new Date(Date.UTC(startParts[0], startParts[1] - 1, startParts[2]));
      const end = new Date(Date.UTC(endParts[0], endParts[1] - 1, endParts[2]));

      const calData = [];
      let lastClose = rawData[0][4];

      while (cur <= end) {{
        const y = cur.getUTCFullYear();
        const m = String(cur.getUTCMonth() + 1).padStart(2, '0');
        const d = String(cur.getUTCDate()).padStart(2, '0');
        const dateStr = `${{y}}-${{m}}-${{d}}`;

        if (map.has(dateStr)) {{
          const row = map.get(dateStr);
          lastClose = row[4];
          calData.push({{
            date: dateStr,
            open: row[1],
            high: row[2],
            low: row[3],
            close: row[4],
            volume: row[5],
            isHoliday: false
          }});
        }} else {{
          calData.push({{
            date: dateStr,
            open: lastClose,
            high: lastClose,
            low: lastClose,
            close: lastClose,
            volume: 0,
            isHoliday: true
          }});
        }}
        cur.setUTCDate(cur.getUTCDate() + 1);
      }}

      return calData;
    }}

    const CALENDAR_DATA = buildFullCalendarData(RAW_DATA);

    let tvChart = null;
    let candleSeries = null;
    let lineSeries = null;
    let volumeSeries = null;
    let sma25Series = null;
    let sma75Series = null;
    let sma200Series = null;
    let fourierSeries = null; // ★ 周期逆算（IFFT）合成復元波
    let top1Series = null;    // ★ 第1主周期成分波（単体）

    let freqChart = null;
    let currentChartType = 'candle';
    let visibleRangeTimer = null;
    let currentFFTResult = null;
    let currentVisibleCalData = null;

    function calcSMA_Calendar(calData, windowDays) {{
      const result = [];
      for (let i = windowDays - 1; i < calData.length; i++) {{
        let sum = 0;
        for (let j = 0; j < windowDays; j++) {{
          sum += calData[i - j].close;
        }}
        result.push({{ time: calData[i].date, value: sum / windowDays }});
      }}
      return result;
    }}

    // 表示区間限定の周期スペクトルおよびIFFT逆算情報計算
    function computePeriodSpectrumAndIFFT(calSubset) {{
      const n = calSubset.length;
      if (n < 8) return {{ spectrum: [], sortedComponents: [], maxPeriod: 10 }};

      const closes = calSubset.map(d => d.close);

      // 1次線形トレンド除去
      const sumX = (n * (n - 1)) / 2;
      const sumX2 = (n * (n - 1) * (2 * n - 1)) / 6;
      let sumY = 0, sumXY = 0;
      for (let i = 0; i < n; i++) {{
        sumY += closes[i];
        sumXY += i * closes[i];
      }}
      const slope = (n * sumXY - sumX * sumY) / (n * sumX2 - sumX * sumX);
      const intercept = (sumY - slope * sumX) / n;
      const detrended = closes.map((y, x) => y - (intercept + slope * x));

      // 窓関数
      const windowed = detrended.map((v, i) => v * (0.5 - 0.5 * Math.cos((2 * Math.PI * i) / (n - 1))));

      const numFreqs = Math.floor(n / 2);
      const allComponents = [];

      for (let k = 1; k < numFreqs; k++) {{
        let real = 0, imag = 0;
        for (let t = 0; t < n; t++) {{
          const angle = (2 * Math.PI * k * t) / n;
          real += windowed[t] * Math.cos(angle);
          imag -= windowed[t] * Math.sin(angle);
        }}
        const amp = (Math.sqrt(real * real + imag * imag) * 2) / (n / 2);
        const phase = Math.atan2(imag, real);
        const freq = k / n;
        const period = n / k;
        allComponents.push({{
          k: k,
          period: period,
          freq: freq,
          amp: amp,
          phase: phase,
          x: period,
          y: Math.max(amp, 0.001)
        }});
      }}

      // 振幅順ソート
      const sortedByAmp = [...allComponents].filter(s => s.period >= 2)
                                            .sort((a, b) => b.amp - a.amp);

      // 周期（横軸）昇順ソート
      const spectrum = [...allComponents].sort((a, b) => a.x - b.x);

      return {{
        spectrum: spectrum,
        sortedComponents: sortedByAmp,
        intercept: intercept,
        slope: slope,
        n: n,
        maxPeriod: n
      }};
    }}

    // ★ 上のチャートに重ねるための逆算（IFFT）時系列データを生成 ★
    function generateOverlayIFFTData(calSubset, fftResult, numHarmonics) {{
      const {{ n, intercept, slope, sortedComponents }} = fftResult;
      const topComponents = sortedComponents.slice(0, numHarmonics);

      const fourierSeriesData = [];
      const top1SeriesData = [];

      for (let t = 0; t < n; t++) {{
        const baseTrend = intercept + slope * t;
        let cycleSum = 0;
        for (const comp of topComponents) {{
          const angle = (2 * Math.PI * comp.k * t) / n + comp.phase;
          cycleSum += comp.amp * Math.cos(angle);
        }}
        const reconVal = baseTrend + cycleSum;
        fourierSeriesData.push({{ time: calSubset[t].date, value: reconVal }});

        if (topComponents.length > 0) {{
          const c1 = topComponents[0];
          const a1 = (2 * Math.PI * c1.k * t) / n + c1.phase;
          top1SeriesData.push({{ time: calSubset[t].date, value: baseTrend + c1.amp * Math.cos(a1) }});
        }}
      }}

      return {{
        reconstructed: fourierSeriesData,
        top1: top1SeriesData,
        topPeriod: topComponents.length > 0 ? topComponents[0].period : 0
      }};
    }}

    function parseTimeObj(t) {{
      if (!t) return '';
      if (typeof t === 'string') return t;
      if (t.year) {{
        const y = t.year;
        const m = String(t.month).padStart(2, '0');
        const d = String(t.day).padStart(2, '0');
        return `${{y}}-${{m}}-${{d}}`;
      }}
      return '';
    }}

    // 表示区間変更時のリアルタイム更新リスナー
    function onVisibleTimeRangeChanged(range) {{
      if (!range || !range.from || !range.to) return;

      clearTimeout(visibleRangeTimer);
      visibleRangeTimer = setTimeout(() => {{
        const fromStr = parseTimeObj(range.from);
        const toStr = parseTimeObj(range.to);
        if (!fromStr || !toStr) return;

        const visibleCalData = CALENDAR_DATA.filter(d => d.date >= fromStr && d.date <= toStr);
        if (visibleCalData.length < 8) return;

        currentVisibleCalData = visibleCalData;
        currentFFTResult = computePeriodSpectrumAndIFFT(visibleCalData);

        // 下の周期スペクトルチャートを更新
        if (freqChart) {{
          freqChart.data.datasets[0].data = currentFFTResult.spectrum || [];
          applyFreqChartScales(currentFFTResult.maxPeriod);
          freqChart.update();
        }}

        // ★ 上のローソク足チャートに重ねる逆算（IFFT）波を更新 ★
        updateOverlayIFFTLines();

        const busDays = visibleCalData.filter(d => !d.isHoliday).length;
        document.getElementById('visibleRangeStatus').textContent =
          `⚡ FFT解析対象：${{fromStr}} 〜 ${{toStr}}（表示区間：${{visibleCalData.length.toLocaleString()}}カレンダー日 / 内営業日:${{busDays.toLocaleString()}}日）`;
      }}, 50);
    }}

    // ★ 上のチャート上の逆算（IFFT）ラインを更新 ★
    function updateOverlayIFFTLines() {{
      if (!currentVisibleCalData || !currentFFTResult) return;

      const isShowFourier = document.getElementById('toggleFourier').checked;
      const isShowTop1 = document.getElementById('toggleTop1').checked;
      const numH = parseInt(document.getElementById('selectHarmonicsCount').value);

      if (isShowFourier || isShowTop1) {{
        const overlayData = generateOverlayIFFTData(currentVisibleCalData, currentFFTResult, numH);

        fourierSeries.setData(overlayData.reconstructed);
        fourierSeries.applyOptions({{
          visible: isShowFourier,
          title: `IFFT逆算波(${{numH}}成分)`
        }});

        top1Series.setData(overlayData.top1);
        top1Series.applyOptions({{
          visible: isShowTop1,
          title: `第1主周期(T≈${{overlayData.topPeriod.toFixed(1)}}日)`
        }});
      }} else {{
        fourierSeries.applyOptions({{ visible: false }});
        top1Series.applyOptions({{ visible: false }});
      }}
    }}

    function initTVChart() {{
      const container = document.getElementById('tvChartWrapper');

      tvChart = LightweightCharts.createChart(container, {{
        width: container.clientWidth,
        height: 520,
        layout: {{
          background: {{ color: '#090d16' }},
          textColor: '#94a3b8'
        }},
        grid: {{
          vertLines: {{ color: 'rgba(255, 255, 255, 0.05)' }},
          horzLines: {{ color: 'rgba(255, 255, 255, 0.05)' }}
        }},
        crosshair: {{ mode: LightweightCharts.CrosshairMode.Normal }},
        rightPriceScale: {{
          borderColor: 'rgba(255, 255, 255, 0.1)',
          scaleMargins: {{ top: 0.08, bottom: 0.22 }}
        }},
        timeScale: {{
          borderColor: 'rgba(255, 255, 255, 0.1)',
          timeVisible: true,
          minBarSpacing: 0.0001
        }}
      }});

      candleSeries = tvChart.addCandlestickSeries({{
        upColor: '#22c55e',
        downColor: '#ef4444',
        borderVisible: false,
        wickUpColor: '#22c55e',
        wickDownColor: '#ef4444'
      }});

      lineSeries = tvChart.addLineSeries({{
        color: '#38bdf8',
        lineWidth: 2,
        visible: false
      }});

      volumeSeries = tvChart.addHistogramSeries({{
        color: '#64748b',
        priceFormat: {{ type: 'volume' }},
        priceScaleId: '',
        scaleMargins: {{ top: 0.8, bottom: 0 }}
      }});

      sma25Series = tvChart.addLineSeries({{ color: '#fbbf24', lineWidth: 1.2, title: 'SMA25' }});
      sma75Series = tvChart.addLineSeries({{ color: '#3b82f6', lineWidth: 1.2, title: 'SMA75' }});
      sma200Series = tvChart.addLineSeries({{ color: '#c084fc', lineWidth: 1.2, title: 'SMA200' }});

      // ★ 上の時系列チャートに直接重ねる「周期逆算（IFFT）合成波」★
      fourierSeries = tvChart.addLineSeries({{
        color: '#f43f5e',
        lineWidth: 2.5,
        title: 'IFFT逆算波',
        visible: true
      }});

      // ★ 上の時系列チャートに重ねる「第1主周期波（単体）」★
      top1Series = tvChart.addLineSeries({{
        color: '#38bdf8',
        lineWidth: 1.5,
        lineStyle: LightweightCharts.LineStyle.Dashed,
        title: '第1主周期波',
        visible: false
      }});

      loadInitialCalendarData();

      tvChart.timeScale().subscribeVisibleTimeRangeChange(onVisibleTimeRangeChanged);

      tvChart.subscribeCrosshairMove(param => {{
        if (!param.time || !param.seriesData.get(candleSeries)) {{
          const last = CALENDAR_DATA[CALENDAR_DATA.length - 1];
          updateOHLCBar(last.date, last.open, last.high, last.low, last.close, last.volume, last.isHoliday, null);
          return;
        }}
        const candle = param.seriesData.get(candleSeries) || param.seriesData.get(lineSeries);
        const vol = param.seriesData.get(volumeSeries);
        const ifftPoint = param.seriesData.get(fourierSeries);
        const timeStr = parseTimeObj(param.time);

        const item = CALENDAR_DATA.find(d => d.date === timeStr);
        const isHol = item ? item.isHoliday : false;
        const ifftVal = ifftPoint ? ifftPoint.value : null;

        if (candle.open !== undefined) {{
          updateOHLCBar(timeStr, candle.open, candle.high, candle.low, candle.close, vol ? vol.value : 0, isHol, ifftVal);
        }} else {{
          updateOHLCBar(timeStr, candle.value, candle.value, candle.value, candle.value, vol ? vol.value : 0, isHol, ifftVal);
        }}
      }});

      window.addEventListener('resize', () => {{
        tvChart.applyOptions({{ width: container.clientWidth }});
      }});
    }}

    function updateOHLCBar(date, open, high, low, close, volume, isHoliday, ifftVal) {{
      document.getElementById('infoDate').textContent = isHoliday ? `${{date}} (休場日)` : date;
      document.getElementById('infoOpen').textContent = open.toFixed(1) + '円';
      document.getElementById('infoHigh').textContent = high.toFixed(1) + '円';
      document.getElementById('infoLow').textContent = low.toFixed(1) + '円';
      document.getElementById('infoClose').textContent = close.toFixed(1) + '円';
      document.getElementById('infoVolume').textContent = (volume || 0).toLocaleString() + '株';

      const diff = close - open;
      const pct = open > 0 ? (diff / open) * 100 : 0;
      const diffElem = document.getElementById('infoDiff');
      if (isHoliday) {{
        diffElem.textContent = '休場日 (前日終値維持)';
        diffElem.className = 'ohlc-val';
      }} else {{
        diffElem.textContent = `${{diff >= 0 ? '+' : ''}}${{diff.toFixed(1)}}円 (${{pct >= 0 ? '+' : ''}}${{pct.toFixed(2)}}%)`;
        diffElem.className = 'ohlc-val ' + (diff >= 0 ? 'up' : 'down');
      }}

      // IFFT逆算値の表示
      const ifftValElem = document.getElementById('infoIFFTVal');
      if (ifftVal !== null && ifftVal !== undefined) {{
        ifftValElem.textContent = `${{ifftVal.toFixed(1)}} 円`;
      }} else {{
        ifftValElem.textContent = '-- 円';
      }}
    }}

    function loadInitialCalendarData() {{
      const candleData = CALENDAR_DATA.map(d => {{
        if (d.isHoliday) {{
          return {{
            time: d.date,
            open: d.open,
            high: d.high,
            low: d.low,
            close: d.close,
            color: 'rgba(148, 163, 184, 0.2)',
            borderColor: 'rgba(148, 163, 184, 0.2)',
            wickColor: 'rgba(148, 163, 184, 0.2)'
          }};
        }} else {{
          return {{
            time: d.date,
            open: d.open,
            high: d.high,
            low: d.low,
            close: d.close
          }};
        }}
      }});

      const lineData = CALENDAR_DATA.map(d => ({{ time: d.date, value: d.close }}));

      const volumeData = CALENDAR_DATA.map(d => ({{
        time: d.date,
        value: d.volume,
        color: d.isHoliday ? 'transparent' : (d.close >= d.open ? 'rgba(34, 197, 94, 0.4)' : 'rgba(239, 68, 68, 0.4)')
      }}));

      candleSeries.setData(candleData);
      lineSeries.setData(lineData);
      volumeSeries.setData(volumeData);

      sma25Series.setData(calcSMA_Calendar(CALENDAR_DATA, 25));
      sma75Series.setData(calcSMA_Calendar(CALENDAR_DATA, 75));
      sma200Series.setData(calcSMA_Calendar(CALENDAR_DATA, 200));

      setVisibleRangeByButtons('ALL');
    }}

    function setChartType(type) {{
      currentChartType = type;
      if (type === 'candle') {{
        candleSeries.applyOptions({{ visible: true }});
        lineSeries.applyOptions({{ visible: false }});
      }} else {{
        candleSeries.applyOptions({{ visible: false }});
        lineSeries.applyOptions({{ visible: true }});
      }}
    }}

    function setVisibleRangeByButtons(rangeKey) {{
      const totalLen = CALENDAR_DATA.length;
      const latestDateStr = CALENDAR_DATA[totalLen - 1].date;
      const latestDate = new Date(latestDateStr);

      if (rangeKey === 'ALL') {{
        const startStr = CALENDAR_DATA[0].date;
        tvChart.timeScale().setVisibleRange({{ from: startStr, to: latestDateStr }});
        tvChart.timeScale().fitContent();
        return;
      }}

      let startDate = new Date(latestDate);
      if (rangeKey === '1M') startDate.setMonth(latestDate.getMonth() - 1);
      else if (rangeKey === '6M') startDate.setMonth(latestDate.getMonth() - 6);
      else if (rangeKey === '1Y') startDate.setFullYear(latestDate.getFullYear() - 1);
      else if (rangeKey === '5Y') startDate.setFullYear(latestDate.getFullYear() - 5);
      else if (rangeKey === '10Y') startDate.setFullYear(latestDate.getFullYear() - 10);

      const startStr = startDate.toISOString().split('T')[0];
      tvChart.timeScale().setVisibleRange({{ from: startStr, to: latestDateStr }});
    }}

    function zoomTo(startStr, endStr) {{
      document.querySelectorAll('#rangeButtons button').forEach(b => b.classList.remove('active'));
      tvChart.timeScale().setVisibleRange({{ from: startStr, to: endStr }});
    }}

    function initFreqChart() {{
      currentVisibleCalData = CALENDAR_DATA;
      currentFFTResult = computePeriodSpectrumAndIFFT(CALENDAR_DATA);
      const spectrum = currentFFTResult.spectrum || [];

      const fCtx = document.getElementById('freqChart').getContext('2d');
      freqChart = new Chart(fCtx, {{
        type: 'line',
        data: {{
          datasets: [{{
            label: '振幅スペクトル |X(T)|',
            data: spectrum,
            borderColor: '#14b8a6',
            backgroundColor: 'rgba(20, 184, 166, 0.15)',
            borderWidth: 1.5,
            pointRadius: 0,
            fill: true,
            tension: 0.05
          }}]
        }},
        options: {{
          responsive: true,
          maintainAspectRatio: false,
          animation: false,
          interaction: {{ mode: 'nearest', axis: 'x', intersect: false }},
          plugins: {{
            legend: {{ display: false }},
            tooltip: {{
              backgroundColor: 'rgba(15, 23, 42, 0.95)',
              titleColor: '#14b8a6',
              callbacks: {{
                title: (items) => {{
                  const raw = items[0].raw;
                  const pVal = raw.x.toFixed(1);
                  let unitDesc = '';
                  if (raw.x >= 365) unitDesc = ` (約${{(raw.x / 365.25).toFixed(2)}}年)`;
                  else if (raw.x >= 60) unitDesc = ` (約${{(raw.x / 30.4).toFixed(1)}}ヶ月)`;
                  else if (raw.x >= 14) unitDesc = ` (約${{(raw.x / 7).toFixed(1)}}週間)`;
                  return `周期 T = ${{pVal}} 日${{unitDesc}}`;
                }},
                label: (item) => `振幅スペクトル: ${{item.raw.y.toFixed(2)}} 円`
              }}
            }},
            zoom: {{
              pan: {{ enabled: true, mode: 'x' }},
              zoom: {{
                wheel: {{ enabled: true, speed: 0.1 }},
                pinch: {{ enabled: true }},
                mode: 'x'
              }},
              limits: {{
                x: {{ min: 2, max: 15000, minRange: 1 }}
              }}
            }}
          }},
          scales: {{
            x: {{
              type: 'logarithmic',
              min: 2,
              max: currentFFTResult.maxPeriod || 365,
              title: {{
                display: true,
                text: '周期 T [カレンダー日] (短期: 2日・7日・30日 ← ────────────── ホイールで拡大縮小 ────────────── → 長期: 表示区間最大日数)',
                color: '#94a3b8',
                font: {{ size: 11 }}
              }},
              ticks: {{
                color: '#64748b',
                callback: v => {{
                  const val = Number(v);
                  if (val === 2) return '2日';
                  if (val === 7) return '7日(1週)';
                  if (val === 30) return '30日(1ヶ月)';
                  if (val === 90) return '90日(3ヶ月)';
                  if (val === 180) return '180日(半年)';
                  if (val === 365) return '365日(1年)';
                  if (val === 730) return '2年';
                  if (val === 1095) return '3年';
                  if (val === 1825) return '5年';
                  if (val === 3650) return '10年';
                  return val >= 10 ? val.toString() : '';
                }}
              }},
              grid: {{ color: 'rgba(255, 255, 255, 0.05)' }}
            }},
            y: {{
              type: 'linear',
              position: 'right',
              title: {{ display: true, text: '振幅 |X(T)| [円]', color: '#94a3b8', font: {{ size: 11 }} }},
              ticks: {{ color: '#64748b' }},
              grid: {{ color: 'rgba(255, 255, 255, 0.05)' }}
            }}
          }}
        }}
      }});

      applyFreqChartScales(currentFFTResult.maxPeriod || 365);
    }}

    function applyFreqChartScales(maxP) {{
      if (!freqChart) return;
      const isLogY = document.getElementById('toggleLogY').checked;
      const isLogX = document.getElementById('toggleLogX').checked;

      if (isLogY) {{
        freqChart.options.scales.y.type = 'logarithmic';
        freqChart.options.scales.y.title.text = '振幅 |X(T)| [対数 Log scale / dB相当]';
        freqChart.options.scales.y.ticks.callback = v => Number(v) >= 0.1 ? Number(v).toLocaleString() + '円' : Number(v);
      }} else {{
        freqChart.options.scales.y.type = 'linear';
        freqChart.options.scales.y.title.text = '振幅 |X(T)| [円]';
        freqChart.options.scales.y.ticks.callback = v => Number(v).toLocaleString() + '円';
      }}

      if (isLogX) {{
        freqChart.options.scales.x.type = 'logarithmic';
        freqChart.options.scales.x.min = 2;
        freqChart.options.scales.x.max = Math.max(maxP || 365, 5);
        freqChart.options.scales.x.title.text = '周期 T [カレンダー日] (対数 Log scale: 7日, 30日, 90日, 365日, 数年)';
        freqChart.options.scales.x.ticks.callback = v => {{
          const val = Number(v);
          if (val === 2) return '2日';
          if (val === 7) return '7日(1週)';
          if (val === 30) return '30日(1ヶ月)';
          if (val === 90) return '90日(3ヶ月)';
          if (val === 180) return '180日(半年)';
          if (val === 365) return '365日(1年)';
          if (val === 730) return '2年';
          if (val === 1095) return '3年';
          if (val === 1825) return '5年';
          if (val === 3650) return '10年';
          return val >= 10 ? val.toString() : '';
        }};
      }} else {{
        freqChart.options.scales.x.type = 'linear';
        freqChart.options.scales.x.min = 2;
        freqChart.options.scales.x.max = Math.max(maxP || 365, 5);
        freqChart.options.scales.x.title.text = '周期 T [カレンダー日] (リニア表示: 2日 ────── ホイールで拡大縮小 ──────→ 最大日数)';
        freqChart.options.scales.x.ticks.callback = v => `${{Math.round(v)}}日`;
      }}

      const xMode = isLogX ? '対数 (Log)' : 'リニア';
      const yMode = isLogY ? '対数 (Log)' : 'リニア';
      document.getElementById('scaleStatusText').textContent = `横軸単位: 周期 T [カレンダー日] | スケール: [横軸: ${{xMode}}] × [縦軸: ${{yMode}}]`;
    }}

    document.querySelectorAll('#rangeButtons button').forEach(btn => {{
      btn.addEventListener('click', () => {{
        document.querySelectorAll('#rangeButtons button').forEach(b => b.classList.remove('active'));
        btn.classList.add('active');
        setVisibleRangeByButtons(btn.dataset.range);
      }});
    }});

    document.querySelectorAll('#chartTypeButtons button').forEach(btn => {{
      btn.addEventListener('click', () => {{
        document.querySelectorAll('#chartTypeButtons button').forEach(b => b.classList.remove('active'));
        btn.classList.add('active');
        setChartType(btn.dataset.type);
      }});
    }});

    // 逆算波のコントロール
    document.getElementById('toggleFourier').addEventListener('change', () => updateOverlayIFFTLines());
    document.getElementById('toggleTop1').addEventListener('change', () => updateOverlayIFFTLines());
    document.getElementById('selectHarmonicsCount').addEventListener('change', () => updateOverlayIFFTLines());

    document.getElementById('toggleLogY').addEventListener('change', () => {{
      applyFreqChartScales();
      freqChart.update();
    }});

    document.getElementById('toggleLogX').addEventListener('change', () => {{
      applyFreqChartScales();
      freqChart.resetZoom();
      freqChart.update();
    }});

    document.getElementById('toggleSMA25').addEventListener('change', (e) => {{
      sma25Series.applyOptions({{ visible: e.target.checked }});
    }});
    document.getElementById('toggleSMA75').addEventListener('change', (e) => {{
      sma75Series.applyOptions({{ visible: e.target.checked }});
    }});
    document.getElementById('toggleSMA200').addEventListener('change', (e) => {{
      sma200Series.applyOptions({{ visible: e.target.checked }});
    }});

    document.getElementById('btnResetFreqZoom').addEventListener('click', () => {{
      if (freqChart) freqChart.resetZoom();
    }});

    document.getElementById('btnShortPeriodZoom').addEventListener('click', () => {{
      if (freqChart) {{
        freqChart.zoomScale('x', {{ min: 2, max: 120 }}, 'default');
      }}
    }});

    initTVChart();
    initFreqChart();
  </script>
</body>
</html>
'''

with open('sharp_dashboard.html', 'w', encoding='utf-8') as f:
    f.write(html_template)

print("sharp_dashboard.html successfully updated: IFFT reconstructed line is now directly overlaid onto the upper Candlestick chart!")
