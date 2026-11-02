
## サーバロジック

### パス方式 (port 8180)
[1] queryに?url=URLがある場合、/url/$URLにリダイレクト
[2] Path /url/$URL がある場合、URLへのリバースプロキシ接続
[3] Error

### Cookie方式 (port 8380)
[1] queryに?url=URLがある場合、cookie(X-230701-Target-Url=URL) を加えて /にリダイレクト
[2] cookie(X-230701-Target-Url=URL) がある場合、URLへのリバースプロキシ接続
[3] Error

## 起動
引数: [パス方式port] [サンプルServer port] [Cookie方式port] (既定 8180 8181 8380)

## サンプルServer
http://127.0.0.1:8181/m/sample1
http://127.0.0.1:8181/m/sample1/

## Proxy Server (パス方式)
http://127.0.0.1:8180/url/http%3A%2F%2F127.0.0.1%3A8181/m/sample1
http://127.0.0.1:8180/url/http%3A%2F%2F127.0.0.1%3A8181/m/sample1/

## Proxy Server (Cookie方式)
http://127.0.0.1:8380/?url=http://127.0.0.1:8181/m/sample1
http://127.0.0.1:8380/?url=http://127.0.0.1:8181/m/sample1/

## 参考
https://github.com/ktorio/ktor-samples
https://github.com/ktorio/ktor-samples/blob/main/reverse-proxy
