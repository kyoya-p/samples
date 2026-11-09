"""
株価・時系列データのフーリエ変換（FFT）解析と可視化スクリプト
- 原信号（株価トレンド＋周期波）の生成
- 高速フーリエ変換（FFT）による周波数スペクトルの抽出
- 主要な周期成分（支配的周波数）の同定
- 逆フーリエ変換（IFFT）によるノイズ除去・平滑化フィルタリング
"""

import numpy as np
import matplotlib.pyplot as plt

def main():
    # 日本語フォント設定（文字化け防止用フォールバック）
    plt.rcParams['font.sans-serif'] = ['Meiryo', 'Yu Gothic', 'Hiragino Sans', 'DejaVu Sans']
    plt.rcParams['axes.unicode_minus'] = False

    # 1. サンプルデータの作成（例：株価に見立てた複数周期を持つ時系列データ）
    np.random.seed(42)
    n_days = 252  # 約1年間の営業日数
    t = np.arange(n_days)

    # トレンド + 複数の周期性（四半期周期、月次周期など） + ランダムノイズ
    trend = 500 + 0.15 * t
    cycle1 = 15 * np.sin(2 * np.pi * t / 60)   # 60日周期（四半期程度）
    cycle2 = 8 * np.sin(2 * np.pi * t / 20)    # 20日周期（1ヶ月程度）
    noise = np.random.normal(0, 3, n_days)     # 日々のランダムウォーク/ノイズ
    price = trend + cycle1 + cycle2 + noise

    # 2. フーリエ変換（FFT）の実行
    # トレンド（直流成分/低周波ドリフト）を線形回帰で除去して周波数を分析
    detrended_price = price - (np.polyval(np.polyfit(t, price, 1), t))

    fft_result = np.fft.rfft(detrended_price)
    frequencies = np.fft.rfftfreq(n_days, d=1.0)  # サンプリング周期 1日
    amplitudes = np.abs(fft_result) / (n_days / 2) # 振幅スペクトル

    # 3. 逆フーリエ変換（IFFT）による主要周期成分の抽出（ローパス・主要波フィルタ）
    # 振幅上位の成分のみを残して逆変換
    threshold = np.percentile(amplitudes[1:], 85) # 上位15%の周波数成分を抽出
    filtered_fft = fft_result.copy()
    filtered_fft[amplitudes < threshold] = 0
    reconstructed_cycle = np.fft.irfft(filtered_fft, n=n_days)
    filtered_price = reconstructed_cycle + np.polyval(np.polyfit(t, price, 1), t)

    # 4. 可視化グラフの作成
    fig, axes = plt.subplots(3, 1, figsize=(10, 8), constrained_layout=True)

    # グラフ1: 元の時系列データとフーリエ逆変換による平滑化
    axes[0].plot(t, price, label='Raw Price (Original)', color='#64748b', alpha=0.6, lw=1.2)
    axes[0].plot(t, filtered_price, label='Reconstructed / Filtered (IFFT)', color='#e11d48', lw=2.0)
    axes[0].set_title('1. Time Domain: Stock Price & Fourier Reconstruction', fontsize=12, fontweight='bold')
    axes[0].set_xlabel('Days')
    axes[0].set_ylabel('Price (Yen)')
    axes[0].grid(True, linestyle='--', alpha=0.5)
    axes[0].legend(loc='upper left')

    # グラフ2: トレンド除去後の波形
    axes[1].plot(t, detrended_price, label='Detrended Wave', color='#3b82f6', lw=1.2)
    axes[1].plot(t, reconstructed_cycle, label='Extracted Cycles', color='#e11d48', linestyle='--', lw=1.8)
    axes[1].set_title('2. Detrended Periodic Components', fontsize=12, fontweight='bold')
    axes[1].set_xlabel('Days')
    axes[1].set_ylabel('Variation')
    axes[1].grid(True, linestyle='--', alpha=0.5)
    axes[1].legend(loc='upper left')

    # グラフ3: 周波数スペクトル（周期・パワースペクトル）
    # 周期（日数 = 1 / 周波数）の表示
    valid_idx = frequencies > 0
    periods = 1 / frequencies[valid_idx]
    amp_valid = amplitudes[valid_idx]

    axes[2].stem(periods, amp_valid, linefmt='teal', markerfmt='o', basefmt=' ')
    axes[2].set_xlim(2, 100)
    axes[2].set_title('3. Frequency Domain: Amplitude Spectrum by Cycle Period (Days)', fontsize=12, fontweight='bold')
    axes[2].set_xlabel('Period Cycle (Days)')
    axes[2].set_ylabel('Amplitude')
    axes[2].grid(True, linestyle='--', alpha=0.5)

    # ピークの検出とアノテーション
    peaks = np.argsort(amp_valid)[-2:]
    for p in peaks:
        target_period = periods[p]
        target_amp = amp_valid[p]
        axes[2].annotate(f'Peak: ~{target_period:.1f} days',
                         xy=(target_period, target_amp),
                         xytext=(target_period + 3, target_amp * 0.9),
                         arrowprops=dict(facecolor='black', shrink=0.05, width=1, headwidth=5),
                         fontweight='bold', color='#0f172a')

    output_path = 'fourier_analysis_result.png'
    plt.savefig(output_path, dpi=150)
    print(f"Chart saved successfully to {output_path}")

if __name__ == '__main__':
    main()
