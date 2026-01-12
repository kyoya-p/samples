# バトスピデッキ構築補助ツール

このプロジェクトは、バトルスピリッツカードの検索と管理を支援します。
**AIはこのファイルの変更禁止**

# 使用  
```shell  
java -jar Main.jar [options]... [Keyword...] 
```
Keyword: Keywordで指示され検出したカードの詳細情報をキャッシュファイル(~/.bscards/$cardNo.yaml)として保存

Options:
-f, --force              Force rewrite cache
-c, --cache-dir=<value>  Cache Directory (default: C:\Users\kyoya\.bscards)
--cost                   "3-5"ならコスト3以上5以下のカード。 "7"ならコスト7以上7以下のカード


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
