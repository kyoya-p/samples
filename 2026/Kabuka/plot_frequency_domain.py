"""
シャープ【6753】株価の周波数空間（Frequency Domain）解析グラフ
- 時間領域から周波数領域（周波数空間）へのFFT変換
- 振幅スペクトル、パワースペクトル密度（PSD: dB）
- 周期（営業日数・月・年）ごとの強さ（支配的周期のピーク抽出）
"""

import pandas as pd
import numpy as np
import matplotlib.pyplot as plt

def main():
    # 日本語フォント設定
    plt.rcParams['font.sans-serif'] = ['Meiryo', 'Yu Gothic', 'Hiragino Sans', 'DejaVu Sans']
    plt.rcParams['axes.unicode_minus'] = False

    # 1. データ読み込み
    df = pd.read_csv('sharp_6753_daily_all_history.csv', parse_dates=['Date'])
    df = df.dropna(subset=['Close']).sort_values('Date').reset_index(drop=True)

    # 2. 全期間（27年）の解析
    closes_all = df['Close'].values
    n_all = len(closes_all)
    t_all = np.arange(n_all)

    # トレンド除去（対数リターンまたは線形トレンド除去）
    # 株価の比率変動を捉えるため対数株価でトレンド除去
    log_closes = np.log(closes_all)
    fit_all = np.polyfit(t_all, log_closes, 1)
    detrended_all = log_closes - np.polyval(fit_all, t_all)

    # 窓関数（ハミング窓）を適用してスペクトル漏れを抑制
    window_all = np.hamming(n_all)
    fft_all = np.fft.rfft(detrended_all * window_all)
    freqs_all = np.fft.rfftfreq(n_all, d=1.0) # 単位: 1/営業日
    amps_all = np.abs(fft_all) * 2 / np.sum(window_all)
    psd_all = 20 * np.log10(amps_all + 1e-12) # パワースペクトル (dB)

    # 3. 直近1年間（242営業日）の解析
    df_1y = df.tail(242).reset_index(drop=True)
    closes_1y = df_1y['Close'].values
    n_1y = len(closes_1y)
    t_1y = np.arange(n_1y)

    fit_1y = np.polyfit(t_1y, closes_1y, 1)
    detrended_1y = closes_1y - np.polyval(fit_1y, t_1y)

    window_1y = np.hamming(n_1y)
    fft_1y = np.fft.rfft(detrended_1y * window_1y)
    freqs_1y = np.fft.rfftfreq(n_1y, d=1.0)
    amps_1y = np.abs(fft_1y) * 2 / np.sum(window_1y)
    psd_1y = 20 * np.log10(amps_1y + 1e-12)

    # 4. グラフ描画（周波数空間のマルチパネル）
    fig = plt.figure(figsize=(14, 10), constrained_layout=True)
    gs = fig.add_gridspec(2, 2)

    # --- 左上: 全期間 周波数スペクトル (vs 周波数 f [1/日]) ---
    ax1 = fig.add_subplot(gs[0, 0])
    valid_all = freqs_all > 0
    ax1.plot(freqs_all[valid_all], amps_all[valid_all], color='#0284c7', lw=1.2)
    ax1.set_title('(A) 周波数空間: 全期間(27年) 振幅スペクトル [周波数 f 表示]', fontsize=11, fontweight='bold')
    ax1.set_xlabel('周波数 f (1 / 営業日)')
    ax1.set_ylabel('振幅 (Amplitude)')
    ax1.set_xlim(0, 0.1) # 10日以上の周期領域を中心に表示
    ax1.grid(True, linestyle='--', alpha=0.5)

    # --- 右上: 全期間 周期空間 (vs 周期 T = 1/f [年・月]) ---
    ax2 = fig.add_subplot(gs[0, 1])
    periods_all_years = (1 / freqs_all[valid_all]) / 245 # 1年約245営業日換算
    ax2.plot(periods_all_years, amps_all[valid_all], color='#2563eb', lw=1.5)
    ax2.set_xscale('log')
    ax2.set_title('(B) 周期空間: 全期間(27年) 周期スペクトル [年単位・対数軸]', fontsize=11, fontweight='bold')
    ax2.set_xlabel('周期 (年 / Log scale)')
    ax2.set_ylabel('振幅 (Amplitude)')
    ax2.grid(True, which="both", linestyle='--', alpha=0.5)

    # ピーク注記（全期間）
    peak_idx_all = np.argsort(amps_all[valid_all])[-3:]
    for idx in peak_idx_all:
        py = periods_all_years[idx]
        ay = amps_all[valid_all][idx]
        if py >= 0.5: # 半年以上のサイクル
            ax2.annotate(f'約{py:.1f}年周期',
                         xy=(py, ay),
                         xytext=(py * 1.2, ay * 1.05),
                         arrowprops=dict(facecolor='#b91c1c', shrink=0.05, width=1.2, headwidth=5),
                         fontweight='bold', fontsize=9, color='#b91c1c')

    # --- 左下: 直近1年 周波数スペクトル (vs 周期 T [営業日数]) ---
    ax3 = fig.add_subplot(gs[1, 0])
    valid_1y = freqs_1y > 0
    periods_1y_days = 1 / freqs_1y[valid_1y]
    ax3.stem(periods_1y_days, amps_1y[valid_1y], linefmt='#0d9488', markerfmt='o', basefmt=' ')
    ax3.set_xlim(2, 120)
    ax3.set_title('(C) 周期空間: 直近1年 主要サイクル [営業日数表示]', fontsize=11, fontweight='bold')
    ax3.set_xlabel('周期 (営業日数)')
    ax3.set_ylabel('振幅 (円)')
    ax3.grid(True, linestyle='--', alpha=0.5)

    # 直近1年のピーク注記
    peak_idx_1y = np.argsort(amps_1y[valid_1y])[-3:]
    for idx in peak_idx_1y:
        pd_val = periods_1y_days[idx]
        ad_val = amps_1y[valid_1y][idx]
        ax3.annotate(f'{pd_val:.1f}日周期\n({ad_val:.1f}円)',
                     xy=(pd_val, ad_val),
                     xytext=(pd_val + 2, ad_val * 0.95),
                     arrowprops=dict(facecolor='#0f766e', shrink=0.05, width=1, headwidth=4),
                     fontweight='bold', fontsize=8.5)

    # --- 右下: パワースペクトル密度 (PSD: Power Spectral Density) ---
    ax4 = fig.add_subplot(gs[1, 1])
    ax4.plot(freqs_all[valid_all], psd_all[valid_all], color='#7c3aed', lw=1.2, label='全期間 (27y)')
    ax4.plot(freqs_1y[valid_1y], psd_1y[valid_1y], color='#f43f5e', lw=1.2, alpha=0.7, label='直近1年')
    ax4.set_title('(D) パワースペクトル密度 (PSD: dB) [1/f ノイズ特性・エネルギー分布]', fontsize=11, fontweight='bold')
    ax4.set_xlabel('周波数 f (1 / 営業日)')
    ax4.set_ylabel('Power (dB)')
    ax4.set_xlim(0, 0.2)
    ax4.grid(True, linestyle='--', alpha=0.5)
    ax4.legend(loc='upper right')

    output_path = 'sharp_frequency_domain_analysis.png'
    plt.savefig(output_path, dpi=160)
    print(f"Frequency domain chart saved to {output_path}")

if __name__ == '__main__':
    main()
