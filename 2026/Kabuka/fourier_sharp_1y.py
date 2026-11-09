"""
シャープ（6753）過去1年間の実際の日足データに対するフーリエ変換（FFT）解析と可視化
"""

import pandas as pd
import numpy as np
import matplotlib.pyplot as plt

def main():
    plt.rcParams['font.sans-serif'] = ['Meiryo', 'Yu Gothic', 'Hiragino Sans', 'DejaVu Sans']
    plt.rcParams['axes.unicode_minus'] = False

    df = pd.read_csv('sharp_6753_daily_1y.csv', parse_dates=['Date'])
    df = df.dropna(subset=['Close']).reset_index(drop=True)

    dates = df['Date']
    close_prices = df['Close'].values
    n = len(close_prices)
    t = np.arange(n)

    # 1. 一次トレンド（直流・長期的傾き）の除去
    fit = np.polyfit(t, close_prices, 1)
    trend = np.polyval(fit, t)
    detrended = close_prices - trend

    # 2. 高速フーリエ変換 (FFT)
    fft_vals = np.fft.rfft(detrended)
    freqs = np.fft.rfftfreq(n, d=1.0) # 1営業日刻み
    amplitudes = np.abs(fft_vals) / (n / 2)

    # 3. 逆フーリエ変換 (IFFT) による支配的周期成分の抽出
    # 上位主要周波数（低周波の主要サイクル）を抽出
    cutoff_amp = np.percentile(amplitudes[1:], 80)
    filtered_fft = fft_vals.copy()
    filtered_fft[amplitudes < cutoff_amp] = 0
    reconstructed_cycle = np.fft.irfft(filtered_fft, n=n)
    reconstructed_price = trend + reconstructed_cycle

    # 4. 可視化
    fig, axes = plt.subplots(3, 1, figsize=(11, 9), constrained_layout=True)

    # (1) 株価終値と主要周期による復元線
    axes[0].plot(dates, close_prices, label='SHARP【6753】Actual Close', color='#475569', alpha=0.7, lw=1.2)
    axes[0].plot(dates, reconstructed_price, label='Fourier Cycle Reconstruction (IFFT)', color='#e11d48', lw=2.0)
    axes[0].plot(dates, trend, label='Linear Trend', color='#2563eb', linestyle=':', lw=1.5)
    axes[0].set_title('SHARP (6753) Past 1-Year Daily Close & Fourier Filtered Cycles', fontsize=12, fontweight='bold')
    axes[0].set_ylabel('Price (JPY)')
    axes[0].grid(True, linestyle='--', alpha=0.5)
    axes[0].legend()

    # (2) トレンド除去後の変動と抽出された周期波
    axes[1].plot(dates, detrended, label='Detrended Variation', color='#3b82f6', lw=1.0)
    axes[1].plot(dates, reconstructed_cycle, label='Extracted Dominant Waves', color='#e11d48', lw=1.8)
    axes[1].set_title('Detrended Fluctuation & Dominant Cyclical Components', fontsize=12, fontweight='bold')
    axes[1].set_ylabel('Diff from Trend (JPY)')
    axes[1].grid(True, linestyle='--', alpha=0.5)
    axes[1].legend()

    # (3) 周期スペクトル（営業日数ベース）
    valid = freqs > 0
    periods = 1 / freqs[valid] # 周期（営業日数）
    amp_valid = amplitudes[valid]

    axes[2].stem(periods, amp_valid, linefmt='#0284c7', markerfmt='o', basefmt=' ')
    axes[2].set_xlim(2, 120)
    axes[2].set_title('Fourier Amplitude Spectrum by Period (Business Days)', fontsize=12, fontweight='bold')
    axes[2].set_xlabel('Cycle Period (Trading Days)')
    axes[2].set_ylabel('Amplitude')
    axes[2].grid(True, linestyle='--', alpha=0.5)

    # ピークの検出
    peaks = np.argsort(amp_valid)[-3:]
    for p in peaks:
        target_p = periods[p]
        target_a = amp_valid[p]
        axes[2].annotate(f'{target_p:.1f} days',
                         xy=(target_p, target_a),
                         xytext=(target_p + 2, target_a * 0.9),
                         arrowprops=dict(facecolor='black', shrink=0.05, width=1, headwidth=4),
                         fontweight='bold', fontsize=9)

    out_file = 'sharp_6753_fourier_analysis.png'
    plt.savefig(out_file, dpi=150)
    print(f"Fourier chart saved to {out_file}")

if __name__ == '__main__':
    main()
