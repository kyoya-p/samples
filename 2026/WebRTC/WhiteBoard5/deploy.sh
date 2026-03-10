#!/bin/bash
set -e

RANDOM="1415"
RESOURCE_GROUP="rg-webrtc-poc"
LOCATION="japaneast"
ACS_NAME="acs-webrtc-poc-$RANDOM"
ACR_NAME="acrwebrtcpoc$RANDOM"

az group create --name $RESOURCE_GROUP --location $LOCATION

# ACS Resource
echo "Creating Azure Communication Service: $ACS_NAME..."
az communication create --name $ACS_NAME --location "Global" --data-location "Japan" --resource-group $RESOURCE_GROUP
CONNECTION_STRING=$(az communication list-key --name $ACS_NAME --resource-group $RESOURCE_GROUP --query primaryConnectionString -o tsv)

# ACR Resource
echo "Creating Azure Communication Service: $ACR_NAME..."
az acr create --resource-group $RESOURCE_GROUP --name $ACR_NAME --sku Basic --admin-enabled true
ACR_PASS=$(az acr credential show --name $ACR_NAME --query passwords[0].value -o tsv)

# --- TURN Server Deployment ---
TURN_ACI_NAME="webrtc-turn-server"
echo "Building Coturn image in ACR..."
az acr build --registry $ACR_NAME --image webrtc-turn:latest turn/

echo "Deploying Coturn to ACI (Initial)..."
TURN_DNS_LABEL="webrtc-turn-${RANDOM}"
TURN_PORTS="3478 49152 49153 49154"

az container create \
  --resource-group $RESOURCE_GROUP \
  --name $TURN_ACI_NAME \
  --image ${ACR_NAME}.azurecr.io/webrtc-turn:latest \
  --dns-name-label $TURN_DNS_LABEL \
  --ports $TURN_PORTS \
  --protocol UDP \
  --os-type linux \
  --cpu 1 \
  --memory 1.0 \
  --registry-login-server ${ACR_NAME}.azurecr.io \
  --registry-username $ACR_NAME \
  --registry-password "$ACR_PASS"

# ACI assignes a Public IP that is often different from the host's egress IP.
# We must fetch it and update Coturn's external-ip explicitly.
TURN_IP=$(az container show --resource-group $RESOURCE_GROUP --name $TURN_ACI_NAME --query ipAddress.ip -o tsv)
TURN_FQDN=$(az container show --resource-group $RESOURCE_GROUP --name $TURN_ACI_NAME --query ipAddress.fqdn -o tsv)

echo "Updating TURN server with its assigned Public IP: $TURN_IP..."
az container create \
  --resource-group $RESOURCE_GROUP \
  --name $TURN_ACI_NAME \
  --image ${ACR_NAME}.azurecr.io/webrtc-turn:latest \
  --dns-name-label $TURN_DNS_LABEL \
  --ports 3478 49152 49153 49154 \
  --protocol UDP \
  --os-type linux \
  --cpu 1 \
  --memory 1.0 \
  --registry-login-server ${ACR_NAME}.azurecr.io \
  --registry-username $ACR_NAME \
  --registry-password "$ACR_PASS" \
  --environment-variables EXTERNAL_IP="$TURN_IP"

TURN_ENV="TURN_URL=turn:$TURN_FQDN:3478 TURN_USER=user TURN_PASSWORD=password123"
echo "TURN server stabilized at: $TURN_FQDN ($TURN_IP)"

# --- App Server Build & Deploy ---
echo "Building App server image in ACR..."
az acr build --registry $ACR_NAME --image webrtc-server:latest server/

# Deploy to ACI
echo "Deploying App Server to Azure Container Instance..."
ACI_NAME="webrtc-full-stack"
DNS_LABEL="webrtc-full-stack"

# 強制再作成
echo "Removing existing App container if any..."
az container delete --resource-group $RESOURCE_GROUP --name $ACI_NAME --yes || true

az container create --resource-group $RESOURCE_GROUP --name $ACI_NAME \
  --image ${ACR_NAME}.azurecr.io/webrtc-server:latest \
  --dns-name-label $DNS_LABEL \
  --ports 3000 8081 8082 --os-type Linux --cpu 1 --memory 1.5 \
  --environment-variables COMMUNICATION_SERVICES_CONNECTION_STRING="$CONNECTION_STRING" $TURN_ENV \
  --registry-login-server ${ACR_NAME}.azurecr.io --registry-username $ACR_NAME --registry-password "$ACR_PASS"

FQDN=$(az container show --resource-group $RESOURCE_GROUP --name $ACI_NAME --query ipAddress.fqdn -o tsv)
URL="http://$FQDN:3000/index.html"
echo "Deployment complete! Validating access at: $URL"

# --- Wait for URL accessibility (Max 5 minutes) ---
echo "Waiting for service to become accessible..."
TIMEOUT=420
ELAPSED=0
while [ $ELAPSED -lt $TIMEOUT ]; do
    if curl -s --head --fail "$URL" > /dev/null; then
        echo "Service is up and running!"
        break
    fi
    echo "Still waiting... (${ELAPSED}s elapsed)"
    sleep 10
    ELAPSED=$((ELAPSED + 10))
done

if [ $ELAPSED -ge $TIMEOUT ]; then
    echo "Error: Timeout reached. Service is not accessible at $URL"
    exit 1
fi

echo "Successfully verified access at: $URL"

