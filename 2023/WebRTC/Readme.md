
Project手順
---
```
git clone https://github.com/webrtc/FirebaseRTC
cd FirebaseRTC/
npm i firebase-tools
npx firebase --version
npx firebase login:ci
[Remoteの場合] TODO

firebase use --add
# 使用するプロジェクトを選択


```

実行
---
```
npx firebase serve --only hosting
# http://localhost:5000 をブラウザで不落
```

参考
---
https://webrtc.org/getting-started/firebase-rtc-codelab?hl=ja
