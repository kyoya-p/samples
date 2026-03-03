FROM coturn/coturn:latest

USER root
RUN apt-get update && apt-get install -y curl procps && rm -rf /var/lib/apt/lists/*

# 起動コマンドのシェル展開を確実にする
# ifconfig.me で取得した IP を変数に格納し、turnserver を起動
ENTRYPOINT ["/bin/sh", "-c", "EXT_IP=${EXTERNAL_IP:-$(curl -s -4 ifconfig.me)}; echo \"Starting turnserver with external-ip: $EXT_IP\"; turnserver --listening-port=3478 --fingerprint --lt-cred-mech --user=user:password123 --realm=webrtc.poc --log-file=stdout --external-ip=$EXT_IP"]
