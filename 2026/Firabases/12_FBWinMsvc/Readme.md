# Firebase C++ SDK Sample
**[AI編集禁止]**


 **Firebase C++ SDK** を使用した、Firestore上のデータをリアルタイムに参照・操作する関数。
Linux/Ubuntuで使用可能なC++ライブラリとする。

機能:
- アドレス帳一覧取得: Firestoreの `addressbook` コレクションからデータを取得して表示。
- アドレス帳追加: Name(id) / Email /現在日時を指定
- アドレス帳削除: アドレスを削除。

# 環境

- OS: Windows および Linux

### ツール/ライブラリ追加
- msvc / g++

 
- ```powershell
winget install --id Microsoft.VisualStudio.2022.BuildTools --override "--passive --config https://aka.ms --add Microsoft.VisualStudio.Workload.VCTools --add Microsoft.VisualStudio.Component.Windows11SDK.22621 --includeRecommended"
```

# ビルド
```bash
cmake -S . -B build        # [CMakeLixt.txt修正後]Makefile作成
cmake --build build -j 4   # 実行ファイル作成
```

# テストコード実行

```bash
export API_KEY="your-api-key"
./build/FBTest
```

