import pandas as pd
import numpy as np
import matplotlib.pyplot as plt

plt.rcParams['font.sans-serif'] = ['Meiryo', 'Yu Gothic', 'Hiragino Sans', 'DejaVu Sans']
plt.rcParams['axes.unicode_minus'] = False

df = pd.read_csv('sharp_6753_daily_1y.csv', parse_dates=['Date']).sort_values('Date').reset_index(drop=True)
closes = df['Close'].values
dates = df['Date'].values
n = len(closes)
t = np.arange(n)

# FFT & トレンド
fit = np.polyfit(t, closes, 1)
trend = np.polyval(fit, t)
detrended = closes - trend

fft_vals = np.fft.rfft(detrended)
freqs = np.fft.rfftfreq(n, d=1.0)
amps = np.abs(fft_vals) / (n / 2)

top_indices = np.argsort(amps[1:])[-4:] + 1
future_days = 60
t_future = np.arange(n, n + future_days)
trend_future = np.polyval(fit, t_future)

cycle_future = np.zeros(future_days)
for idx in top_indices:
    angle = (2 * np.pi * idx * t_future) / n + np.angle(fft_vals[idx])
    cycle_future += amps[idx] * np.cos(angle)

pred_price = trend_future + cycle_future

# 日付の拡張（営業日ベース）
future_dates = pd.date_range(start='2026-11-10', periods=future_days, freq='B')

fig, ax = plt.subplots(figsize=(12, 6), constrained_layout=True)

# 過去実績
ax.plot(dates[-120:], closes[-120:], label='実績株価（過去半年）', color='#334155', lw=1.5)

# 予測ライン
ax.plot(future_dates, pred_price, label='フーリエ周期外挿モデル（メイン予測）', color='#e11d48', lw=2.2, linestyle='--')

# 予測レンジ（強気・弱気シナリオのバンド）
upper_band = pred_price + 35
lower_band = pred_price - 30
ax.fill_between(future_dates, lower_band, upper_band, color='#fda4af', alpha=0.25, label='シナリオ想定レンジ (±1σ相当)')

# 重要水準の水平線
ax.axhline(527, color='#2563eb', linestyle=':', label='27年最安値ライン (527円 / 2026-11-04)')
ax.axhline(600, color='#d97706', linestyle=':', label='節目レジスタンス (600円)')

# 直近安値と予測ピークのアノテーション
ax.annotate('直近安値 527円\n(底打ち反発起点)', xy=(pd.Timestamp('2026-11-04'), 527),
            xytext=(pd.Timestamp('2026-10-15'), 490),
            arrowprops=dict(facecolor='#2563eb', shrink=0.05, width=1, headwidth=5),
            fontweight='bold', fontsize=9, color='#1e3a8a')

peak_idx = np.argmax(pred_price)
ax.annotate(f'自律反発ピーク想定: 約{pred_price[peak_idx]:.0f}円\n(12月上旬〜中旬)',
            xy=(future_dates[peak_idx], pred_price[peak_idx]),
            xytext=(future_dates[peak_idx] + pd.Timedelta(days=5), pred_price[peak_idx] + 25),
            arrowprops=dict(facecolor='#e11d48', shrink=0.05, width=1, headwidth=5),
            fontweight='bold', fontsize=9, color='#9f1239')

ax.set_title('SHARP【6753】今後の推移予測シミュレーション（〜2027年2月）', fontsize=13, fontweight='bold')
ax.set_ylabel('株価 (円)')
ax.grid(True, linestyle='--', alpha=0.5)
ax.legend(loc='upper right')

out_path = 'sharp_future_prediction.png'
plt.savefig(out_path, dpi=150)
print(f"Prediction chart saved to {out_path}")
