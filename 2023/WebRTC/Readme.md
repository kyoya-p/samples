
## サンプルの確認
```
git clone https://github.com/webrtc/samples
cd samples
npm i 
npm start
```

## RTCPeerConnection
```:Build and Run
npm i
npx tsc
<<<<<<< HEAD
mkcert localhost 192.168.x.x 127.0.0.1
npx http-server -d -S -C localhost+1.pem -K localhost+1-key.pem
=======
npx http-server -c-1 -d 
npx http-server -c-1 -d -S -C <cert-file> -K <key-file> #https
>>>>>>> 767473694b17263abf57b7d2e0c683fa70424e6c
# http://localhost:8080/public/peer-server.html
# http://localhost:8080/public/peer-client.html
```


## mkcert
```
wget -O mkcert https://github.com/FiloSottile/mkcert/releases/download/v1.4.3/mkcert-v1.4.3-linux-amd64
chmod +x mkcert
mkcert -install
```
参考
---
https://webrtc.org/getting-started/firebase-rtc-codelab?hl=ja
https://webrtc.github.io/samples/src/content/datachannel/datatransfer/
