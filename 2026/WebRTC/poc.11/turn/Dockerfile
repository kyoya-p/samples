FROM coturn/coturn:latest

USER root
RUN apt-get update && apt-get install -y curl procps && rm -rf /var/lib/apt/lists/*

# リレー用ポートを制限（Dockerで公開しやすくするため）
ENTRYPOINT ["/bin/sh", "-c", "EXT_IP=${EXTERNAL_IP:-$(curl -s -4 ifconfig.me)}; echo \"Starting turnserver with external-ip: $EXT_IP\"; turnserver --listening-port=3478 --min-port=49152 --max-port=49154 --fingerprint --lt-cred-mech --user=user:password123 --realm=webrtc.poc --log-file=stdout --external-ip=$EXT_IP"]
