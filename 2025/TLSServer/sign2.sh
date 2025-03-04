# サーバ秘密鍵登録と署名要求生成
keytool -genkeypair -alias tomcat -keyalg RSA -keysize 2048 -keystore keystore.jks -storepass changeit -keypass changeit -validity 365 -dname "CN=w2410, C=JP"
keytool -certreq -alias tomcat -keystore keystore.jks -storepass changeit -file server.csr

# 署名要求に従いCA証明書で署名
openssl x509 -req -days 365 -in server.csr -CA ca.crt -CAkey ca.key -CAcreateserial -out server.crt

# CA証明書、サーバ証明書をkeystoreに登録
keytool -importcert -trustcacerts -alias rootca -file ca.crt -keystore keystore.jks -storepass changeit
keytool -importcert -alias tomcat -file server.crt -keystore keystore.jks -storepass changeit

keytool -list -keystore keystore.jks -storepass changeit
