# riot agitator


# ビルド/開発環境設定
## SDK導入

1. flutter SDKの設置
  https://flutter.dev/docs/get-started/install  
  DartSDKも含まれるようだ  
  コマンドから利用するならpathを設定
  
2. flutterのターゲットにwebを追加し Web(beta)を有効に
```
flutter channel beta
flutter upgrade  
flutter config --enable-web (*1)
flutter devices
```                   
*1: この設定は ~/.flutter_settings に格納される


## Intelli-J設定(使うなら)
1. Flutter plugin導入: Settings > Plugin > flutterで検索
2. Flutter SDK設定: Settings > lang&Framework > Flutter > Path入力
3. プロジェクト設定: Project Structure > Modules > Dependancies > [+] > Libraries > Flutter plugin, Dart SDK を追加

# Build
> flutter build web

# テスト実行
- CLI
> flutter run -d chrome

ホスト外からのアクセス許可とポート指定:
 
> flutter run -d chrome --web-hostname=<自分のアドレス>> --web-port=50080

- [intelliJ]デバッグ実行  
Deviceを選択し、(Chromeまたはその他)
実行ボタンクリック

# 開発履歴
## Flutterプロジェクトのテンプレ準備
> flutter create --project-name=riotagitator .

Flutterのプロジェクトファイルは `pubspec.yaml`

## Cloud Firestore
(参考)https://pub.dev/packages/cloud_firestore
- pubspec.yaml
  - 依存関係をdependancesに追記
- index.html
  - ライブラリのロード追加
  - Firestore API-Key情報追加

## Cloud Firestore Realtime
サンプルアプリ作成 - main_test1.dart

## ユーザ認証
 - サンプルコード: https://github.com/firebase/firebaseui-web
 - ガイド: https://www.flutter-study.dev/firebase/authentication/
   - pubspec.yaml: dependanciesに
     - firebase_auth: 0.18.0+1 追加
   - indxe.html:　追加
     - <script src="https://www.gstatic.com/firebasejs/7.15.5/firebase-auth.js"></script>

 
## Cloud firestore 備考
- アクセスルール初期設定では、期限が制限30日に制限されている(すぐ開発できるよう)
  - 久しぶりに使う場合はルールを見直すこと

- API Keyは公開しても構わない。
  - ただし、デフォルトのアクセス管理はすべて許可になっている
  - なので、公開前にはかならずユーザ認証で制限すること
  
## Firestoreでページネーション
- https://medium.com/@ntaoo/firestore-%E3%83%AA%E3%82%A2%E3%83%AB%E3%82%BF%E3%82%A4%E3%83%A0%E3%82%A2%E3%83%83%E3%83%97%E3%83%87%E3%83%BC%E3%83%88%E6%A9%9F%E8%83%BD%E3%82%92%E7%B5%A1%E3%82%81%E3%81%9F%E3%83%9A%E3%83%BC%E3%82%B8%E3%83%8D%E3%83%BC%E3%82%B7%E3%83%A7%E3%83%B3%E3%81%AE%E8%A8%AD%E8%A8%88%E3%81%AB%E9%96%A2%E3%81%99%E3%82%8B%E8%A7%A3%E8%AA%AC%E3%81%A8%E6%A4%9C%E8%A8%BC-f4306d5466b5

## [Firestore] オフライン/ローカルデータ
- https://firebase.google.com/docs/firestore/manage-data/enable-offline?hl=ja
- https://qiita.com/Bosch_san/items/e3e06acdb3c7b53553f2
