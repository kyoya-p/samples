# 実装手順

1. オリジナルレンダラサイト https://plantuml.com/ja/sequence-diagram を参照し、サンプルコードを採取しsamplesに格納。
上記を参照し同等のsvgファイルを生成するindex.htmlコードを生成する
dead code, test codeは常に削除

2. 検証
samples以下のサンプルコードをそれぞれに付いて:
  originalレンダラでsvg画像を生成
  localレンダラでsvg画像を生成
  それぞれの画像を比較し、
    異なればlocalレンダラを修正し、画像生成から再試行
すべてのサンプルについてlocalレンダラを修正せずに施行完了したら検証終了
一つ以上のサンプルについてlocalレンダラを修正した場合、すべてのサンプルについて検証を再試行

検証のための一時生成画像は./build以下に格納

