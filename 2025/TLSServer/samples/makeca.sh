openssl genrsa -aes256 -out ca.key 4096
#openssl genrsa -out ca.key 2048
#openssl req -new -x509 -days 3650 -key ca.key -out ca.crt -subj "/C=JP/CN=Local Root CA"
openssl req -x509 -new -nodes -key ca.key -sha256 -days 3650 -out ca.crt -subj "/C=JP/CN=Local Root CA"
