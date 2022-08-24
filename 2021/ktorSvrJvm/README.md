Configuration
----
build.gradle.kts の下記に実行クラス名を記述
```
application.mainClassName 
```

Build
----
sh ./gradlew build

Run
----
java -cp build/classes/kotlin/

Note:
----
- Linuxではport=80や443は一般権限では開けない。1024以上

### Proxy - HTTP CONNECT
```
export http_proxy=...
curl http://api.github.com/zen
```

```
CONNECT api.github.com:443 HTTP/1.1
Host: api.github.com:443
User-Agent: curl/7.58.0
Proxy-Connection: Keep-Alive

HTTP/1.1 200 Connection established

<SSLセッション確立>
<HTTPリクエスト-レスポンス>
```

```
// Open with Chrome https://api.github.com/zen
```
```
CONNECT api.github.com:443 HTTP/1.1
Host: api.github.com:443
Proxy-Connection: keep-alive
User-Agent: Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/89.0.4389.82 Safari/537.36

HTTP/1.1 200 Connection established
```