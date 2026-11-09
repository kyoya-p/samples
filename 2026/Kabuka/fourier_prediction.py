"""
シャープ【6753】のフーリエ変換（周期成分外挿）による今後の株価推移シミュレーション
"""

import pandas as pd
import numpy as np

df = pd.read_csv('sharp_6753_daily_1y.csv', parse_dates=['Date']).sort_values('Date').reset_index(drop=True)
closes = df['Close'].values
n = len(closes)
t = np.arange(n)

# 1. 線形トレンド
fit = np.polyfit(t, closes, 1)
trend = np.polyval(fit, t)
detrended = closes - trend

# 2. FFT
fft_vals = np.fft.rfft(detrended)
freqs = np.fft.rfftfreq(n, d=1.0)
amps = np.abs(fft_vals) / (n / 2)

# 上位主要3成分（支配的サイクル）を抽出
top_indices = np.argsort(amps[1:])[-3:] + 1 # 0は直流成分なので除外
cycles_info = []
for idx in top_indices:
    period = 1 / freqs[idx]
    amp = amps[idx]
    phase = np.angle(fft_vals[idx])
    cycles_info.append({'period': period, 'amp': amp, 'phase': phase, 'k': idx})

# 3. 将来60営業日（約3ヶ月先、2027年2月頃まで）の外挿予測
future_days = 60
t_future = np.arange(n, n + future_days)
trend_future = np.polyval(fit, t_future)

cycle_future = np.zeros(future_days)
for c in cycles_info:
    angle = (2 * np.pi * c['k'] * t_future) / n + c['phase']
    cycle_future += c['amp'] * np.cos(angle)

pred_price = trend_future + cycle_future

print("=== 主要周期成分 ===")
for i, c in enumerate(cycles_info):
    print(f"サイクル{i+1}: 周期 {c['period']:.1f}日, 振幅 {c['amp']:.1f}円")

print(f"\n現在株価 (2026-11-09): {closes[-1]:.1f}円")
print(f"直近安値 (2026-11-04): 527.0円")
print(f"予測 +10営業日 (約11月下旬): {pred_price[9]:.1f}円")
print(f"予測 +20営業日 (約12月上旬): {pred_price[19]:.1f}円")
print(f"予測 +40営業日 (約1月上旬): {pred_price[39]:.1f}円")
print(f"予測 +60営業日 (約2月上旬): {pred_price[59]:.1f}円")
print(f"予測期間内の最高値: {np.max(pred_price):.1f}円 (day {np.argmax(pred_price)+1})")
print(f"予測期間内の最安値: {np.min(pred_price):.1f}円 (day {np.argmin(pred_price)+1})")
