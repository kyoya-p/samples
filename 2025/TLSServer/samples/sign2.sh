# 古い証明書削除(必要なら)
keytool -delete -alias tomcat -keystore .keystore -storepass changeit
keytool -delete -alias rootca -keystore .keystore -storepass changeit

# サーバ秘密鍵新規登録と署名要求生成
keytool -genkeypair -alias tomcat -keyalg RSA -keysize 2048 -keystore .keystore -storepass changeit -keypass changeit -validity 365 -dname "CN=shokkaa.wjg.jp, C=JP"
keytool -certreq -alias tomcat -keystore .keystore -storepass changeit -file server.csr

# 署名要求に従いCA証明書で署名
openssl x509 -req -days 365 -in server.csr -CA ca.crt -CAkey ca.key -CAcreateserial -out server.crt

# CA証明書、サーバ証明書をkeystoreに登録
keytool -importcert -trustcacerts -alias rootca -file ca.crt -keystore .keystore -storepass changeit -noprompt
keytool -importcert -alias tomcat -file server.crt -keystore .keystore -storepass changeit

keytool -list -keystore .keystore -storepass changeit
