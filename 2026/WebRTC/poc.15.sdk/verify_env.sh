#!/bin/bash

# Test0 ネットワーク疎通検証スクリプト (poc.14.agch 用)

SERVER_CONTAINER="poc14-server-1"
CLIENT_NAT1_CONTAINER="poc14agch-client-nat1-1"
PROXY1_CONTAINER="poc14agch-proxy1-1"

SERVER_IP=$(docker inspect -f '{{range .NetworkSettings.Networks}}{{.IPAddress}}{{end}}' $SERVER_CONTAINER 2>/dev/null)
CLIENT_NAT1_IP=$(docker inspect -f '{{range .NetworkSettings.Networks}}{{.IPAddress}}{{end}}' $CLIENT_NAT1_CONTAINER 2>/dev/null)

if [ -z "$SERVER_IP" ]; then
    echo "Error: $SERVER_CONTAINER is not running."
    exit 1
fi

echo "--- poc.14.agch Connectivity Verification ---"
echo "Server Internal IP: $SERVER_IP"

# 1. nat1 -> server (Expected: OK via Host Gateway)
echo "[NAT1 -> SERVER]"
echo -n "  TCP 49880 (HTTP): "
docker exec $CLIENT_NAT1_CONTAINER curl -s -o /dev/null --connect-timeout 2 -w "%{http_code}" http://host.docker.internal:49880/index.html | grep -q "200" && echo "OK" || echo "FAIL"

echo -n "  TCP 49881 (Echo): "
docker exec $CLIENT_NAT1_CONTAINER bash -c "echo 'test' | nc -w 1 host.docker.internal 49881" | grep -q "test" && echo "OK" || echo "FAIL"

echo -n "  UDP 49880 (Echo): "
docker exec $CLIENT_NAT1_CONTAINER bash -c "echo 'test' | nc -u -w 1 host.docker.internal 49880" | grep -q "test" && echo "OK" || echo "FAIL"

# 2. Inbound/Direct Checks (Expected: FAIL/Blocked)
echo "[ISOLATION CHECKS]"
echo -n "  nat1 -> server (Direct IP): "
docker exec $CLIENT_NAT1_CONTAINER curl -s -o /dev/null --connect-timeout 2 -w "%{http_code}" http://$SERVER_IP:49880/index.html && echo "SUCCESS (Unexpected)" || echo "FAIL (Blocked)"

echo -n "  nat1 <- server (Inbound):   "
docker exec $SERVER_CONTAINER bash -c "timeout 2 bash -c 'cat < /dev/null > /dev/tcp/$CLIENT_NAT1_IP/6080'" 2>/dev/null && echo "SUCCESS (Unexpected)" || echo "FAIL (Blocked)"

# 3. pxy1 -> server (Expected: OK via Proxy only)
echo "[PXY1 -> SERVER]"
echo -n "  Direct via Host (Direct):   "
docker exec $CLIENT_NAT1_CONTAINER curl --noproxy "*" -s -o /dev/null --connect-timeout 2 -w "%{http_code}" http://host.docker.internal:49880/index.html && echo "SUCCESS (Unexpected)" || echo "FAIL (Blocked)"

echo -n "  Via Proxy (HTTP):           "
docker exec poc14agch-client-pxy1-1 curl -s -o /dev/null --connect-timeout 2 -w "%{http_code}" -x http://proxy1:3128 http://server:49880/index.html | grep -q "200" && echo "OK" || echo "FAIL"

echo "--- Verification Finished ---"
