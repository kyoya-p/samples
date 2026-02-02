# 概要
Firebasae API (REST) および Official Firebase C++ SDK を使用した Kotlin Multiplatform (Amper + Mosaic) デモ。

# 構成
- **shared**: 共通インターフェース (`FirestoreClient`) と REST 実装 (`RestFirestoreClient`)。
- **jvmApp/windowsApp**: REST API を使用して動作します。
- **linuxApp**: デフォルトでは REST ですが、C++ SDK (Native) 版への切り替えが可能です。
- **native-interop**: Firebase C++ SDK を Kotlin Native から呼び出すための C Shim (ラッパー)。

# C++ SDK (Native) の利用について
C++ SDK は C++ クラスベースであるため、Kotlin Native から直接呼び出すことが困難です。
本プロジェクトでは `native-interop` に C 言語のインターフェースを持つ Shim を作成し、これを介して SDK を利用する構成をとっています。

### 1. Shim のビルド (Linux)
`native-interop` ディレクトリで SDK をダウンロードし、Shim をビルドします。
```shell
cd native-interop
./build_shim.sh
```
※ 1.1GB 程度の SDK のダウンロードと解凍が行われます。

### 2. Native 版の有効化
`linuxApp` で C++ SDK 版を使用するには以下の手順が必要です。
1. `linuxApp/src/NativeFirestoreClient.kt.disabled` を `.kt` にリネーム。
2. `linuxApp/src/main.kt` で `NativeFirestoreClient` をインスタンス化。
3. `linuxApp/module.yaml` で `cinterop` を有効化 (環境に合わせたパス設定が必要な場合があります)。

# 実行方法 (JVM)
```shell
./amper run -m jvmApp
```

# 参照
- https://firebase.google.com/docs/firestore/query-data/listen?hl=ja#c++
- https://github.com/google/firebase-cpp-sdk
