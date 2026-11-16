# 🤖 AI生成: 任意銘柄の全期間日足を yfinance で採取し CSV 保存
# 使い方:
#   python fetch_stock.py 7203 6758 9984.T   # 引数で指定
#   python fetch_stock.py                    # 引数なし → tickers.txt を読む
# tickers.txt: 1行1銘柄 (例: 7203 / 6758.T)、'#' 以降はコメント
import sys
from pathlib import Path
import yfinance as yf
import pandas as pd

CONFIG = Path(__file__).with_name('tickers.txt')


def load_config() -> list[str]:
    if not CONFIG.exists():
        return []
    lines = (l.split('#')[0].strip() for l in CONFIG.read_text(encoding='utf-8').splitlines())
    return [l for l in lines if l]


def fetch(code: str) -> str | None:
    ticker = code if '.' in code else f'{code}.T'
    df = yf.download(ticker, period='max', progress=False)
    if isinstance(df.columns, pd.MultiIndex):
        df.columns = df.columns.get_level_values(0)
    df = df.dropna(subset=['Close'])
    if df.empty:
        print(f'{ticker}: データなし')
        return None
    out = f"{ticker.split('.')[0]}_daily_all_history.csv"
    df.to_csv(out)
    print(f'{ticker}: {df.index[0]:%Y-%m-%d} - {df.index[-1]:%Y-%m-%d} {len(df):,}件 -> {out}')
    return out


if __name__ == '__main__':
    codes = sys.argv[1:] or load_config()
    if not codes:
        sys.exit(f'usage: python fetch_stock.py <code> [code ...]  または {CONFIG.name} に記載')
    for c in codes:
        fetch(c)
