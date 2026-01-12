# バトスピデッキ構築補助ツール

このプロジェクトは、バトルスピリッツカードの検索と管理を支援します。
**AIはこのファイルの変更禁止**

# 使用  
```shell  
kotlin tools/bsq.main.kts query [Keyword Keyword ...]  
```
Keyword: Keywordで指示され検出したカードの詳細情報をキャッシュファイル(~/.bscards/cardNo_side_cardName_type_attr_family.yaml)として保存

# Note
kotlin Script Cache (Windows): `%LOCALAPPDATA%\main.kts.compiled.cache\` 
