FROM coturn/coturn:latest

# 環境変数 EXTERNAL_IP を使用するように修正
# --min-port, --max-port を指定して ACI の開放ポートと合わせる（今回は最小限 3478 のみ疎通確認）
ENTRYPOINT ["turnserver", "--listening-port=3478", "--fingerprint", "--lt-cred-mech", "--user=user:password123", "--realm=webrtc.poc", "--log-file=stdout"]
