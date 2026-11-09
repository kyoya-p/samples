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
  <title>シャープ【6753】ローソク足時系列＆周波数空間（FFT）ダッシュボード</title>
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
      padding: 1.5rem;
      display: flex;
      flex-direction: column;
      gap: 1.5rem;
      min-height: 100vh;
    }}

    header {{
      display: flex;
      justify-content: space-between;
      align-items: center;
      flex-wrap: wrap;
      gap: 1rem;
      border-bottom: 1px solid var(--card-border);
      padding-bottom: 1rem;
    }}

    .title-area h1 {{
      font-size: 1.6rem;
      font-weight: 700;
      display: flex;
      align-items: center;
      gap: 0.75rem;
    }}

    .badge {{
      background: #0284c7;
      color: #fff;
      font-size: 0.8rem;
      padding: 0.2rem 0.6rem;
      border-radius: 9999px;
      font-weight: 600;
    }}

    .subtitle {{
      color: var(--text-muted);
      font-size: 0.875rem;
      margin-top: 0.25rem;
    }}

    .stat-cards {{
      display: grid;
      grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
      gap: 1rem;
    }}

    .card {{
      background-color: var(--card-bg);
      border: 1px solid var(--card-border);
      border-radius: 12px;
      padding: 1.2rem;
      display: flex;
      flex-direction: column;
      gap: 0.4rem;
    }}

    .card-title {{
      font-size: 0.8rem;
      color: var(--text-muted);
      text-transform: uppercase;
      letter-spacing: 0.05em;
    }}

    .card-value {{
      font-size: 1.6rem;
      font-weight: 700;
    }}

    .card-sub {{
      font-size: 0.8rem;
      color: var(--text-muted);
    }}

    .up {{ color: var(--accent-green); }}
    .down {{ color: var(--accent-red); }}

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
      height: 480px;
      width: 100%;
      border-radius: 8px;
      overflow: hidden;
      background: #0b1120;
    }}

    .freq-chart-wrapper {{
      position: relative;
      height: 310px;
      width: 100%;
    }}

    .peak-badges {{
      display: flex;
      align-items: center;
      gap: 0.5rem;
      flex-wrap: wrap;
    }}

    .peak-badge {{
      background: rgba(20, 184, 166, 0.15);
      border: 1px solid var(--accent-teal);
      color: var(--accent-teal);
      font-size: 0.8rem;
      padding: 0.25rem 0.6rem;
      border-radius: 6px;
      font-weight: 600;
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
  </style>
</head>
<body>

  <!-- ヘッダー -->
  <header>
    <div class="title-area">
      <h1>シャープ【6753】ローソク足時系列＆周波数空間（FFT）ダッシュボード <span class="badge">東証プライム</span></h1>
      <div class="subtitle">ローソク足四本値・移動平均・出来高・周波数空間スペクトル（リニア/対数切替）の統合分析</div>
    </div>
  </header>

  <!-- 統計カード -->
  <div class="stat-cards">
    <div class="card">
      <div class="card-title">最新株価（終値）</div>
      <div class="card-value" id="currentPrice">538.0 円</div>
      <div class="card-sub up" id="currentDiff">+9.0 円 (+1.70%)</div>
    </div>
    <div class="card">
      <div class="card-title">過去最高値（調整後）</div>
      <div class="card-value" style="color: var(--accent-amber)">20,524.6 円</div>
      <div class="card-sub">2007年4月9日（AQUOS全盛期）</div>
    </div>
    <div class="card">
      <div class="card-title">過去最安値（27年間）</div>
      <div class="card-value down">527.0 円</div>
      <div class="card-sub">2026年11月4日（直近安値）</div>
    </div>
    <div class="card">
      <div class="card-title">期間内 変動率</div>
      <div class="card-value down" id="rangeReturn">-97.4%</div>
      <div class="card-sub" id="rangeLabel">全期間 (2000年〜2026年)</div>
    </div>
  </div>

  <!-- 時間領域：ローソク足時系列チャート -->
  <div class="chart-container-card">
    <div class="card-header-row">
      <div class="section-title">
        <span style="color: var(--accent-blue);">●</span> 時間領域（Time Domain）：ローソク足四本値チャート
      </div>
      <div class="options-group">
        <div class="button-group" id="chartTypeButtons">
          <button class="btn active" data-type="candle">ローソク足</button>
          <button class="btn" data-type="line">ライン</button>
        </div>

        <label class="toggle-label">
          <input type="checkbox" id="toggleSMA25" checked> SMA25 (黄)
        </label>
        <label class="toggle-label">
          <input type="checkbox" id="toggleSMA75" checked> SMA75 (青)
        </label>
        <label class="toggle-label">
          <input type="checkbox" id="toggleSMA200" checked> SMA200 (紫)
        </label>
        <label class="toggle-label" style="color: var(--accent-red); font-weight: bold;">
          <input type="checkbox" id="toggleFourier"> フーリエ周期復元 (FFT)
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
      <span class="hint-tag">🔍 マウスホイールで時間軸の伸縮 / ドラッグでスクロール</span>
    </div>

    <!-- TradingView ローソク足チャート本体 -->
    <div id="tvChartWrapper"></div>
  </div>

  <!-- ★周波数空間グラフ（Frequency Domain）★ -->
  <div class="chart-container-card">
    <div class="card-header-row">
      <div class="section-title">
        <span style="color: var(--accent-teal);">●</span> 周波数空間（Frequency Domain）：振幅スペクトル [横軸：周波数 $f$ (1/営業日)]
      </div>
      <div class="options-group">
        <!-- 対数スケール切替オプション -->
        <label class="toggle-label" style="color: var(--accent-teal); font-weight: 600;">
          <input type="checkbox" id="toggleLogY"> 縦軸：対数 (Log Y / dB相当)
        </label>
        <label class="toggle-label" style="color: var(--accent-teal); font-weight: 600;">
          <input type="checkbox" id="toggleLogX"> 横軸：対数 (Log X)
        </label>

        <span class="hint-tag">🔍 ホイールで拡大縮小 / ドラッグでパン</span>
        <button class="btn" id="btnResetFreqZoom" style="background: #334155; padding: 0.3rem 0.6rem; font-size: 0.8rem;">
          ズームリセット
        </button>
        <button class="btn" id="btnLowFreqZoom" style="background: #0284c7; padding: 0.3rem 0.6rem; font-size: 0.8rem;">
          低周波域 (f≦0.1)
        </button>
      </div>
    </div>

    <div style="display: flex; justify-content: space-between; align-items: center; flex-wrap: wrap; gap: 0.5rem;">
      <div class="peak-badges" id="peakBadgesContainer"></div>
      <div style="font-size: 0.8rem; color: var(--text-muted);" id="scaleStatusText">
        スケール表示: [横軸: リニア] × [縦軸: リニア]
      </div>
    </div>

    <div class="freq-chart-wrapper">
      <canvas id="freqChart"></canvas>
    </div>
  </div>

  <!-- 重要歴史イベント -->
  <div class="chart-container-card">
    <div class="section-title" style="font-size: 0.95rem; color: var(--text-muted);">
      歴史的転換点（クリックで該当期間へズーム & 周波数解析）
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

    let tvChart = null;
    let candleSeries = null;
    let lineSeries = null;
    let volumeSeries = null;
    let sma25Series = null;
    let sma75Series = null;
    let sma200Series = null;
    let fourierSeries = null;

    let freqChart = null;
    let currentChartType = 'candle';
    let filteredData = [...RAW_DATA];

    function calcSMA_TV(data, windowSize) {{
      const result = [];
      for (let i = windowSize - 1; i < data.length; i++) {{
        let sum = 0;
        for (let j = 0; j < windowSize; j++) {{
          sum += data[i - j][4];
        }}
        result.push({{ time: data[i][0], value: sum / windowSize }});
      }}
      return result;
    }}

    function computeFFTSpectrum(data) {{
      const n = data.length;
      if (n < 8) return {{ spectrum: [], peaks: [] }};

      const closes = data.map(d => d[4]);
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

      const windowed = detrended.map((v, i) => v * (0.5 - 0.5 * Math.cos((2 * Math.PI * i) / (n - 1))));

      const numFreqs = Math.floor(n / 2);
      const spectrum = [];

      for (let k = 1; k < numFreqs; k++) {{
        let real = 0, imag = 0;
        for (let t = 0; t < n; t++) {{
          const angle = (2 * Math.PI * k * t) / n;
          real += windowed[t] * Math.cos(angle);
          imag -= windowed[t] * Math.sin(angle);
        }}
        const amp = (Math.sqrt(real * real + imag * imag) * 2) / (n / 2);
        const freq = k / n;
        const period = n / k;
        spectrum.push({{ x: freq, y: Math.max(amp, 0.001), period: period }});
      }}

      const sorted = [...spectrum].filter(s => s.period >= 3)
                                  .sort((a, b) => b.y - a.y);
      const peaks = sorted.slice(0, 3);

      return {{ spectrum, peaks, intercept, slope }};
    }}

    function calcFourierCycle_TV(data, fftResult) {{
      const n = data.length;
      if (n < 8 || !fftResult) return [];

      const topComponents = fftResult.peaks;
      const {{ intercept, slope }} = fftResult;

      const result = [];
      for (let t = 0; t < n; t++) {{
        let cycle = 0;
        for (const comp of topComponents) {{
          const k = Math.round(comp.x * n);
          const angle = (2 * Math.PI * k * t) / n;
          cycle += comp.y * Math.cos(angle);
        }}
        result.push({{ time: data[t][0], value: intercept + slope * t + cycle }});
      }}
      return result;
    }}

    function initTVChart() {{
      const container = document.getElementById('tvChartWrapper');

      tvChart = LightweightCharts.createChart(container, {{
        width: container.clientWidth,
        height: 480,
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
          timeVisible: true
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

      sma25Series = tvChart.addLineSeries({{ color: '#fbbf24', lineWidth: 1.5, title: 'SMA25' }});
      sma75Series = tvChart.addLineSeries({{ color: '#3b82f6', lineWidth: 1.5, title: 'SMA75' }});
      sma200Series = tvChart.addLineSeries({{ color: '#c084fc', lineWidth: 1.5, title: 'SMA200' }});

      fourierSeries = tvChart.addLineSeries({{
        color: '#f43f5e',
        lineWidth: 2,
        lineStyle: LightweightCharts.LineStyle.Dashed,
        title: 'FFT Cycle',
        visible: false
      }});

      applyDataToTVChart();

      tvChart.subscribeCrosshairMove(param => {{
        if (!param.time || !param.seriesData.get(candleSeries)) {{
          const last = RAW_DATA[RAW_DATA.length - 1];
          updateOHLCBar(last[0], last[1], last[2], last[3], last[4], last[5]);
          return;
        }}
        const candle = param.seriesData.get(candleSeries) || param.seriesData.get(lineSeries);
        const vol = param.seriesData.get(volumeSeries);
        const timeStr = typeof param.time === 'string' ? param.time : `${{param.time.year}}-${{String(param.time.month).padStart(2,'0')}}-${{String(param.time.day).padStart(2,'0')}}`;

        if (candle.open !== undefined) {{
          updateOHLCBar(timeStr, candle.open, candle.high, candle.low, candle.close, vol ? vol.value : 0);
        }} else {{
          updateOHLCBar(timeStr, candle.value, candle.value, candle.value, candle.value, vol ? vol.value : 0);
        }}
      }});

      window.addEventListener('resize', () => {{
        tvChart.applyOptions({{ width: container.clientWidth }});
      }});
    }}

    function updateOHLCBar(date, open, high, low, close, volume) {{
      document.getElementById('infoDate').textContent = date;
      document.getElementById('infoOpen').textContent = open.toFixed(1) + '円';
      document.getElementById('infoHigh').textContent = high.toFixed(1) + '円';
      document.getElementById('infoLow').textContent = low.toFixed(1) + '円';
      document.getElementById('infoClose').textContent = close.toFixed(1) + '円';
      document.getElementById('infoVolume').textContent = (volume || 0).toLocaleString() + '株';

      const diff = close - open;
      const pct = (diff / open) * 100;
      const diffElem = document.getElementById('infoDiff');
      diffElem.textContent = `${{diff >= 0 ? '+' : ''}}${{diff.toFixed(1)}}円 (${{pct >= 0 ? '+' : ''}}${{pct.toFixed(2)}}%)`;
      diffElem.className = 'ohlc-val ' + (diff >= 0 ? 'up' : 'down');
    }}

    function applyDataToTVChart() {{
      const candleData = filteredData.map(d => ({{
        time: d[0],
        open: d[1],
        high: d[2],
        low: d[3],
        close: d[4]
      }}));

      const lineData = filteredData.map(d => ({{ time: d[0], value: d[4] }}));

      const volumeData = filteredData.map(d => ({{
        time: d[0],
        value: d[5],
        color: d[4] >= d[1] ? 'rgba(34, 197, 94, 0.4)' : 'rgba(239, 68, 68, 0.4)'
      }}));

      candleSeries.setData(candleData);
      lineSeries.setData(lineData);
      volumeSeries.setData(volumeData);

      sma25Series.setData(calcSMA_TV(filteredData, 25));
      sma75Series.setData(calcSMA_TV(filteredData, 75));
      sma200Series.setData(calcSMA_TV(filteredData, 200));

      const fftResult = computeFFTSpectrum(filteredData);
      const fourierData = calcFourierCycle_TV(filteredData, fftResult);
      fourierSeries.setData(fourierData);
      updatePeakBadges(fftResult.peaks);

      tvChart.timeScale().fitContent();

      if (freqChart) {{
        freqChart.data.datasets[0].data = fftResult.spectrum || [];
        applyFreqChartScales();
        freqChart.resetZoom();
        freqChart.update();
      }}
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

    function filterByRange(rangeKey) {{
      const latestDate = new Date(RAW_DATA[RAW_DATA.length - 1][0]);
      let startDate = new Date(latestDate);

      if (rangeKey === '1M') startDate.setMonth(latestDate.getMonth() - 1);
      else if (rangeKey === '6M') startDate.setMonth(latestDate.getMonth() - 6);
      else if (rangeKey === '1Y') startDate.setFullYear(latestDate.getFullYear() - 1);
      else if (rangeKey === '5Y') startDate.setFullYear(latestDate.getFullYear() - 5);
      else if (rangeKey === '10Y') startDate.setFullYear(latestDate.getFullYear() - 10);
      else if (rangeKey === 'ALL') startDate = new Date(RAW_DATA[0][0]);

      const startStr = startDate.toISOString().split('T')[0];
      filteredData = RAW_DATA.filter(d => d[0] >= startStr);
      applyDataToTVChart();
      updateStatCards(rangeKey);
    }}

    function zoomTo(startStr, endStr) {{
      filteredData = RAW_DATA.filter(d => d[0] >= startStr && d[0] <= endStr);
      document.querySelectorAll('#rangeButtons button').forEach(b => b.classList.remove('active'));
      applyDataToTVChart();
      updateStatCards(`${{startStr}} ~ ${{endStr}}`);
    }}

    function updateStatCards(rangeLabel) {{
      if (filteredData.length === 0) return;
      const firstClose = filteredData[0][4];
      const lastClose = filteredData[filteredData.length - 1][4];
      const ret = ((lastClose - firstClose) / firstClose) * 100;

      const retElem = document.getElementById('rangeReturn');
      retElem.textContent = (ret >= 0 ? '+' : '') + ret.toFixed(1) + '%';
      retElem.className = 'card-value ' + (ret >= 0 ? 'up' : 'down');
      document.getElementById('rangeLabel').textContent = rangeLabel;
    }}

    function updatePeakBadges(peaks) {{
      const container = document.getElementById('peakBadgesContainer');
      container.innerHTML = '<span style="font-size: 0.85rem; color: var(--text-muted); font-weight:600;">支配的周波数ピーク（Dominant Frequencies）:</span>';
      if (!peaks || peaks.length === 0) return;

      peaks.forEach((p, idx) => {{
        const badge = document.createElement('div');
        badge.className = 'peak-badge';
        const fStr = p.x.toFixed(4);
        const pDays = p.period.toFixed(1);
        let extra = '';
        if (p.period >= 245) extra = ` (約${{(p.period / 245).toFixed(1)}}年)`;
        else if (p.period >= 40) extra = ` (約${{(p.period / 20).toFixed(1)}}ヶ月)`;
        badge.textContent = `第${{idx + 1}}位: f = ${{fStr}} [1/日]（周期: 約${{pDays}}日${{extra}}）振幅: ${{p.y.toFixed(1)}}円`;
        container.appendChild(badge);
      }});
    }}

    // 周波数空間 Chart.js 初期化
    function initFreqChart() {{
      const fftResult = computeFFTSpectrum(filteredData);
      const spectrum = fftResult.spectrum || [];

      const fCtx = document.getElementById('freqChart').getContext('2d');
      freqChart = new Chart(fCtx, {{
        type: 'line',
        data: {{
          datasets: [{{
            label: '振幅スペクトル |X(f)|',
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
                  const fVal = raw.x;
                  const pVal = raw.period > 0 ? raw.period.toFixed(1) : '∞';
                  return `周波数 f = ${{fVal.toFixed(4)}} [1/日] (周期 T ≈ ${{pVal}}日)`;
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
                x: {{ min: 0.0001, max: 0.505, minRange: 0.002 }}
              }}
            }}
          }},
          scales: {{
            x: {{
              type: 'linear',
              min: 0,
              max: 0.5,
              title: {{
                display: true,
                text: '周波数 f [1 / 営業日] (低周波・長周期 ← ────────────────── ホイールで拡大縮小 ────────────────── → 高周波・短周期)',
                color: '#94a3b8',
                font: {{ size: 11 }}
              }},
              ticks: {{
                color: '#64748b',
                callback: v => Number(v).toFixed(3)
              }},
              grid: {{ color: 'rgba(255, 255, 255, 0.05)' }}
            }},
            y: {{
              type: 'linear',
              position: 'right',
              title: {{ display: true, text: '振幅 |X(f)| [円]', color: '#94a3b8', font: {{ size: 11 }} }},
              ticks: {{ color: '#64748b' }},
              grid: {{ color: 'rgba(255, 255, 255, 0.05)' }}
            }}
          }}
        }}
      }});

      applyFreqChartScales();
    }}

    // 対数スケール切替の適用関数
    function applyFreqChartScales() {{
      if (!freqChart) return;
      const isLogY = document.getElementById('toggleLogY').checked;
      const isLogX = document.getElementById('toggleLogX').checked;

      // Y軸（振幅）
      if (isLogY) {{
        freqChart.options.scales.y.type = 'logarithmic';
        freqChart.options.scales.y.title.text = '振幅 |X(f)| [対数 Log scale / dB相当]';
        freqChart.options.scales.y.ticks.callback = v => Number(v) >= 0.1 ? Number(v).toLocaleString() + '円' : Number(v);
      }} else {{
        freqChart.options.scales.y.type = 'linear';
        freqChart.options.scales.y.title.text = '振幅 |X(f)| [円]';
        freqChart.options.scales.y.ticks.callback = v => Number(v).toLocaleString() + '円';
      }}

      // X軸（周波数）
      if (isLogX) {{
        freqChart.options.scales.x.type = 'logarithmic';
        freqChart.options.scales.x.min = 0.0005;
        freqChart.options.scales.x.max = 0.5;
        freqChart.options.scales.x.title.text = '周波数 f [1/営業日] (対数 Log scale: 低周波・長周期がワイド表示)';
        freqChart.options.scales.x.ticks.callback = v => Number(v).toString();
      }} else {{
        freqChart.options.scales.x.type = 'linear';
        freqChart.options.scales.x.min = 0;
        freqChart.options.scales.x.max = 0.5;
        freqChart.options.scales.x.title.text = '周波数 f [1 / 営業日] (低周波・長周期 ← ────────────────── ホイールで拡大縮小 ────────────────── → 高周波・短周期)';
        freqChart.options.scales.x.ticks.callback = v => Number(v).toFixed(3);
      }}

      // ステータステキスト更新
      const xMode = isLogX ? '対数 (Log)' : 'リニア';
      const yMode = isLogY ? '対数 (Log)' : 'リニア';
      document.getElementById('scaleStatusText').textContent = `スケール表示: [横軸: ${{xMode}}] × [縦軸: ${{yMode}}]`;
    }}

    // イベントリスナー設定
    document.querySelectorAll('#rangeButtons button').forEach(btn => {{
      btn.addEventListener('click', () => {{
        document.querySelectorAll('#rangeButtons button').forEach(b => b.classList.remove('active'));
        btn.classList.add('active');
        filterByRange(btn.dataset.range);
      }});
    }});

    document.querySelectorAll('#chartTypeButtons button').forEach(btn => {{
      btn.addEventListener('click', () => {{
        document.querySelectorAll('#chartTypeButtons button').forEach(b => b.classList.remove('active'));
        btn.classList.add('active');
        setChartType(btn.dataset.type);
      }});
    }});

    // 対数切替イベント
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
    document.getElementById('toggleFourier').addEventListener('change', (e) => {{
      fourierSeries.applyOptions({{ visible: e.target.checked }});
    }});

    document.getElementById('btnResetFreqZoom').addEventListener('click', () => {{
      if (freqChart) freqChart.resetZoom();
    }});

    document.getElementById('btnLowFreqZoom').addEventListener('click', () => {{
      if (freqChart) {{
        const isLogX = document.getElementById('toggleLogX').checked;
        freqChart.zoomScale('x', {{ min: isLogX ? 0.001 : 0, max: 0.1 }}, 'default');
      }}
    }});

    // 初期化実行
    initTVChart();
    initFreqChart();
    updateStatCards('全期間 (2000年〜2026年)');
  </script>
</body>
</html>
'''

with open('sharp_dashboard.html', 'w', encoding='utf-8') as f:
    f.write(html_template)

print("sharp_dashboard.html successfully upgraded with Log scale on Frequency Domain!")
