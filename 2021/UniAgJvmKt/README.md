
Ktor Web Server SSL対応
----
Refer: https://ktor.io/docs/advanced-http2.html#ssl-certificate

- 証明書作成
  
> keytool -keystore test.jks -genkeypair -alias testkey -keyalg RSA -keysize 4096 -validity 5000 -dname "CN=localhost,OU=ktor,O=ktor,L=Unspecified,ST=Unspecified,C=US"
 
- 証明書 test.jskを src/main/resource にコピー

- application.conf に ktor.security 設定追加


