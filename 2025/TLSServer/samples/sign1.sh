# make server key & csr
openssl req -new -newkey rsa:4096 -keyout server.key -out server.csr -nodes -subj "/CN=server1"

# sign by ca.crt
openssl x509 -req -in server.csr -CA ca.crt -CAkey ca.key -CAcreateserial -out server.crt -days 365 -sha256

# make keypair and import
keytool -import -trustcacerts -alias rootca -file ca.crt -keystore keystore.jks -storepass changeit -keypass changeit

#openssl pkcs12 -export -out server.p12 -inkey server.key.pem -in server.crt.pem -name tomcat -passout pass:changeit
#keytool -importkeystore -srckeystore server.p12 -srcstoretype pkcs12 -srcstorepass changeit -destkeystore keystore.jks -deststoretype jks -deststorepass changeit


keytool -list -keystore keystore.jks -storepass changeit
