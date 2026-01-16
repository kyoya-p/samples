# バトスピデッキ構築補助ツール

バトルスピリッツカードの検索と管理の支援ツール。
**AIはこのファイルの変更禁止**

# 使用
配布パッケージ (`BS-CLI.zip`) を解凍し、中の `BS-CLI.exe` を使用。

```shell
.\BS-CLI.exe [options]... [Keyword...]
```

Keyword: 指定したキーワードで検出したカードの詳細情報をキャッシュファイル(~/.bscards/$cardNo.yaml)として保存。

Options:
-f, --force               キャッシュが存在する場合でも強制的に再取得して上書き
-d, --cache-dir=<value>   カードデータのキャッシュ先ディレクトリを指定 (default: C:\Users\kyoya\.bscards)
-c, --cost <range>        コスト範囲を指定（例: "3-5"、"7"）
-a, --color <attr>...     属性（色）を指定 (例: 赤, 紫)。複数指定可(OR検索)
-A, --color-and <attr>... 属性（色）を指定 (例: 赤, 紫)。複数指定可(AND検索)
-s, --system <family>...  系統を指定 (例: 星竜)。複数指定可(OR検索)
-S, --system-and <family>... 系統を指定 (例: 星竜)。複数指定可(AND検索)
-t, --type <category>...  カテゴリを指定 (例: スピリット)。複数指定可
-b, --block <icon>...     ブロックアイコンを指定 (例: 7)。複数指定可
※属性・系統のAND/ORは、引数リストの最後に指定されたオプション（-a/-A, -s/-S）で決定。

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
# 開発用 (Jar直接実行)
```shell  
java -jar tools/build/tasks/_jvm-cli_executableJarJvm/jvm-cli-jvm-executable.jar [options]... [Keyword...] 
```
# 配布用パッケージ作成 (Windows)
```powershell
./package.ps1
```
`tools/build/installer/BS-CLI.zip` を生成。

