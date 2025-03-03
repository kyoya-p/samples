# CA証明書作成 - 

keytool:
```sh
keytool -genkeypair -alias ca -keyalg RSA -keysize 2048 -keystore ca.jks -storepass capassword -keypass capassword -dname "CN=CA, OU=Shokkaa, O=Shokkaa, L=City, ST=State, C=JP" -ext KeyUsage:critical=keyCertSign,cRLSign -ext BasicConstraints:critical=ca:true
```

openssl:
```sh
openssl ecparam -out contoso.key -name prime256v1 -genkey
openssl req -new -sha256 -key contoso.key -out contoso.csr
openssl x509 -req -sha256 -days 365 -in contoso.csr -signkey contoso.key -out contoso.crt
```

# `sa-basyoumeisyo 
`
```shell
openssl ecparam -out fabrikam.key -name prime256v1 -genkey

```

# 参照

- https://learn.microsoft.com/ja-jp/azure/application-gateway/self-signed-certificates