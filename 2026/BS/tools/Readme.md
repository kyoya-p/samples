# バトスピデッキ構築補助ツール

このプロジェクトは、バトルスピリッツカードの検索と管理を支援します。
**AIはこのファイルの変更禁止**

# 使用  
```shell  
java -jar Main.jar [options]... [Keyword...] 
```
Keyword: Keywordで指示され検出したカードの詳細情報をキャッシュファイル(~/.bscards/$cardNo.yaml)として保存

Options:
-f, --force               キャッシュが存在する場合でも強制的に再取得して上書きします
-d, --cache-dir=<value>   カードデータのキャッシュ先ディレクトリを指定します (default: C:\Users\kyoya\.bscards)
-c, --cost <range>        コスト範囲を指定します（例: "3-5"、"7"）
-a, --color <attr>        属性（色）を指定します (例: 赤, 紫)。複数指定可
-m, --attr-mode <AND/OR>  属性検索モード (デフォルト: OR)
-s, --system <family>     系統 (例: 星竜)。複数指定可
-n, --system-mode <AND/OR> 系統検索モード (デフォルト: OR)
-t, --type <category>     カテゴリ (例: スピリット)。複数指定可
-b, --block <icon>        ブロックアイコン (例: 7)。複数指定可


# Note
kotlin Script Cache (Windows): `%LOCALAPPDATA%\main.kts.compiled.cache\` 

# テスト実行
```shell
./amper run -m jvm-cli      # Jvm
./amper run -m linux-cli    # Linux
./amper run -m windows-cli  # Windows
```

# ビルド
```shell
./amper task :jvm-cli:executableJarJvm
./amper task :linux-cli:linkLinuxX64Release
./amper task :windows-cli:linkMingwX64Release
```

# TODO
- windows版動作しない
