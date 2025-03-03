# CA証明書作成 - keytool

```sh
keytool -genkeypair -alias ca -keyalg RSA -keysize 2048 -keystore ca.jks -storepass capassword -keypass capassword -dname "CN=CA, OU=Shokkaa, O=Shokkaa, L=City, ST=State, C=JP" -ext KeyUsage:critical=keyCertSign,cRLSign -ext BasicConstraints:critical=ca:true
```


