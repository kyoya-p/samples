
Project手順
---
```
git clone https://github.com/webrtc/FirebaseRTC
cd FirebaseRTC/
npm i firebase-tools
npx firebase --version
npx firebase login:ci
[Remoteの場合] TODO

# 使用するプロジェクトを選択
npx firebase use <project名> 
[or] npx firebase use --add 

```

実行
---
```
npx firebase serve --only hosting
# http://localhost:5000 をブラウザで開く
```

参考
---
https://webrtc.org/getting-started/firebase-rtc-codelab?hl=ja
