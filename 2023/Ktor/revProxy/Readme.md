
## サーバロジック

[1] queryに?url=URLがある場合、/url/$URLにリダイレクト
[2] Path /url/$URL がある場合、URLへのリバースプロキシ接続
[3] Error


## サンプルServer
http://127.0.0.1:8181/m/sample1
http://127.0.0.1:8181/m/sample1/

## Proxy Server
http://127.0.0.1:8180/url/http%3A%2F%2F127.0.0.1%3A8181/m/sample1
http://127.0.0.1:8180/url/http%3A%2F%2F127.0.0.1%3A8181/m/sample1/

## 参考
https://github.com/ktorio/ktor-samples
https://github.com/ktorio/ktor-samples/blob/main/reverse-proxy