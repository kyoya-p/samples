
## サーバロジック

[1] queryに?url=URLがある場合、cookie(X-230701-Target-Url=URL) を加えて /にリダイレクト
[2] cookie(X-230701-Target-Url=URL) がある場合、URLへのリバースプロキシ接続
[3] Error


## サンプルServer
http://127.0.0.1:8381/m/sample1
http://127.0.0.1:8381/m/sample1/

## Proxy Server
http://127.0.0.1:8380/?url=http://127.0.0.1:8381/m/sample1
http://127.0.0.1:8380/?url=http://127.0.0.1:8381/m/sample1/

## 参考
https://github.com/ktorio/ktor-samples
https://github.com/ktorio/ktor-samples/blob/main/reverse-proxy