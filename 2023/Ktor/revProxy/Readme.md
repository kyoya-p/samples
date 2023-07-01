
## サーバロジック

[1] queryに?url=URLがある場合、cookie(X-230701-Target-Url=URL) を加えて /にリダイレクト
[2] cookie(X-230701-Target-Url=URL) がある場合、URLへのリバースプロキシ接続
[3] Pathがある場合、Pathの一段目をURLとし、URLへのリバースプロキシ接続
[4] Error


## サンプルURL
http://127.0.0.1:8180/http%3A%2F%2F127.0.0.1%3A8181/m/sample1
http://127.0.0.1:8180/http%3A%2F%2F127.0.0.1%3A8181/m/sample1/

## 参考
https://github.com/ktorio/ktor-samples
https://github.com/ktorio/ktor-samples/blob/main/reverse-proxy