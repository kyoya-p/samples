import pandas as pd
import matplotlib.pyplot as plt

plt.rcParams['font.sans-serif'] = ['Meiryo', 'Yu Gothic', 'Hiragino Sans', 'DejaVu Sans']
plt.rcParams['axes.unicode_minus'] = False

df = pd.read_csv('sharp_6753_daily_all_history.csv', parse_dates=['Date'], index_col='Date')

fig, (ax1, ax2) = plt.subplots(2, 1, figsize=(12, 8), gridspec_kw={'height_ratios': [3, 1]}, sharex=True)

# 終値推移
ax1.plot(df.index, df['Close'], color='#2563eb', lw=1.2, label='Sharp Close (Split-adjusted)')
ax1.set_title('SHARP【6753】Long-term Stock Price History (2000 - 2026)', fontsize=14, fontweight='bold')
ax1.set_ylabel('Stock Price (JPY)', fontsize=11)
ax1.grid(True, linestyle='--', alpha=0.5)

# 歴史的なイベントのアノテーション
# 2007年 最高値（液晶・AQUOS好調期）
ax1.annotate('All-time High: 20,525 JPY (Adj)\n(April 2007 - AQUOS Boom)',
             xy=(pd.Timestamp('2007-04-09'), 20524.6),
             xytext=(pd.Timestamp('2008-01-01'), 22000),
             arrowprops=dict(facecolor='red', shrink=0.05, width=1.5, headwidth=6),
             fontweight='bold', fontsize=9, color='#b91c1c')

# 2012年 経営危機・液晶不況期
ax1.annotate('LCD Crisis (2012-2016)\nFoxconn Acquisition (2016)',
             xy=(pd.Timestamp('2012-10-01'), 1500),
             xytext=(pd.Timestamp('2011-01-01'), 7000),
             arrowprops=dict(facecolor='black', shrink=0.05, width=1, headwidth=5),
             fontsize=9)

# 2017年 株式併合（10株→1株）
ax1.axvline(pd.Timestamp('2017-09-27'), color='purple', linestyle=':', alpha=0.7, label='10-to-1 Reverse Split (Sep 2017)')

# 2026年 直近最安値
ax1.annotate('Recent Low: 527 JPY\n(Nov 2026)',
             xy=(pd.Timestamp('2026-11-04'), 527.0),
             xytext=(pd.Timestamp('2023-01-01'), 3500),
             arrowprops=dict(facecolor='darkblue', shrink=0.05, width=1.5, headwidth=6),
             fontweight='bold', fontsize=9, color='#1e3a8a')

ax1.legend(loc='upper right')

# 出来高推移
ax2.bar(df.index, df['Volume'] / 1e6, color='#64748b', alpha=0.7, width=2)
ax2.set_title('Trading Volume', fontsize=11)
ax2.set_xlabel('Year', fontsize=11)
ax2.set_ylabel('Volume (Million Shares)', fontsize=10)
ax2.grid(True, linestyle='--', alpha=0.5)

plt.tight_layout()
out_img = 'sharp_6753_longterm_chart.png'
plt.savefig(out_img, dpi=150)
print(f"Chart saved to {out_img}")
