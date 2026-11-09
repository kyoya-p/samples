import yfinance as yf
import pandas as pd

ticker = '6753.T'
print("Fetching maximum historical data for 6753.T...")
df = yf.download(ticker, period='max')

if isinstance(df.columns, pd.MultiIndex):
    df.columns = df.columns.get_level_values(0)

# 空データ除去
df = df.dropna(subset=['Close'])

print("=== 期間情報 ===")
start_date = df.index[0].strftime('%Y-%m-%d')
end_date = df.index[-1].strftime('%Y-%m-%d')
print(f"最古日付: {start_date}")
print(f"最新日付: {end_date}")
print(f"総営業日数 (レコード数): {len(df):,} 日")

print("\n=== 最古5日分 ===")
print(df[['Open', 'High', 'Low', 'Close', 'Volume']].head())

print("\n=== 直近5日分 ===")
print(df[['Open', 'High', 'Low', 'Close', 'Volume']].tail())

max_close_date = df['Close'].idxmax()
min_close_date = df['Close'].idxmin()
max_close_val = df.loc[max_close_date, 'Close']
min_close_val = df.loc[min_close_date, 'Close']

print("\n=== 過去最高値・最安値 (終値ベース / 調整後) ===")
print(f"過去最高値: {max_close_val:,.1f} 円 ({max_close_date.strftime('%Y-%m-%d')})")
print(f"過去最安値: {min_close_val:,.1f} 円 ({min_close_date.strftime('%Y-%m-%d')})")

# 年ごとの始値・終値・高値・安値・年間騰落率
yearly = df['Close'].resample('YE').agg(['first', 'max', 'min', 'last'])
yearly['return_pct'] = ((yearly['last'] - yearly['first']) / yearly['first']) * 100
print("\n=== 年代別サマリー（直近10年） ===")
print(yearly.tail(10))

# 株式分割情報
t = yf.Ticker(ticker)
splits = t.splits
print("\n=== 株式分割・併合の履歴 ===")
if not splits.empty:
    print(splits)
else:
    print("履歴なし")

# 全期間データをCSVに保存
out_csv = 'sharp_6753_daily_all_history.csv'
df.to_csv(out_csv)
print(f"\n全期間データを {out_csv} に保存しました。")
