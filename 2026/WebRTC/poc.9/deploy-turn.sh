#!/bin/bash
set -e

RESOURCE_GROUP="rg-webrtc-poc"
LOCATION="japaneast"
ACR_NAME_PREFIX="acrwebrtcpoc"
ACI_NAME="webrtc-turn-server"
DNS_LABEL="webrtc-turn-${RANDOM}"

echo "Ensuring Resource Group: $RESOURCE_GROUP..."
az group create --name $RESOURCE_GROUP --location $LOCATION || true

ACR_NAME=$(az acr list --resource-group $RESOURCE_GROUP --query "[0].name" -o tsv)
if [ -z "$ACR_NAME" ]; then
    ACR_NAME="${ACR_NAME_PREFIX}$RANDOM"
    echo "Creating ACR: $ACR_NAME..."
    az acr create --resource-group $RESOURCE_GROUP --name $ACR_NAME --sku Basic --admin-enabled true
fi

echo "Building Coturn image in ACR: $ACR_NAME..."
az acr build --registry $ACR_NAME --image webrtc-turn:latest turn/

echo "Deploying initial Coturn to ACI to get IP..."
ACR_PASS=$(az acr credential show --name $ACR_NAME --query passwords[0].value -o tsv)

# 引数の順序を整理
az container create \
  --resource-group $RESOURCE_GROUP \
  --name $ACI_NAME \
  --image ${ACR_NAME}.azurecr.io/webrtc-turn:latest \
  --dns-name-label $DNS_LABEL \
  --ports 3478 \
  --protocol UDP \
  --os-type linux \
  --cpu 1 \
  --memory 1.0 \
  --registry-login-server ${ACR_NAME}.azurecr.io \
  --registry-username $ACR_NAME \
  --registry-password "$ACR_PASS"

PUBLIC_IP=$(az container show --resource-group $RESOURCE_GROUP --name $ACI_NAME --query ipAddress.ip -o tsv)
FQDN=$(az container show --resource-group $RESOURCE_GROUP --name $ACI_NAME --query ipAddress.fqdn -o tsv)

echo "Updating Coturn with --external-ip: $PUBLIC_IP..."
az container create \
  --resource-group $RESOURCE_GROUP \
  --name $ACI_NAME \
  --image ${ACR_NAME}.azurecr.io/webrtc-turn:latest \
  --dns-name-label $DNS_LABEL \
  --ports 3478 \
  --protocol UDP \
  --os-type linux \
  --cpu 1 \
  --memory 1.0 \
  --registry-login-server ${ACR_NAME}.azurecr.io \
  --registry-username $ACR_NAME \
  --registry-password "$ACR_PASS" \
  --command-line "turnserver --listening-port=3478 --fingerprint --lt-cred-mech --user=user:password123 --realm=webrtc.poc --log-file=stdout --external-ip=$PUBLIC_IP"

echo "------------------------------------------------"
echo "Coturn Server: turn:$FQDN:3478"
echo "------------------------------------------------"
