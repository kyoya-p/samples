#!/bin/bash
set -e

# Azure ACS + TURN Unified Deployment Script

RESOURCE_GROUP="rg-webrtc-poc"
LOCATION="japaneast"
ACS_NAME_PREFIX="acs-webrtc-poc"
ACR_NAME_PREFIX="acrwebrtcpoc"
TURN_ACI_NAME="webrtc-turn-server"
APP_ACI_NAME="webrtc-full-stack"

echo "Using Resource Group: $RESOURCE_GROUP in $LOCATION..."
az group create --name $RESOURCE_GROUP --location $LOCATION || true

# ACR Resource Check or Create
ACR_NAME=$(az acr list --resource-group $RESOURCE_GROUP --query "[0].name" -o tsv)
if [ -z "$ACR_NAME" ]; then
    ACR_NAME="${ACR_NAME_PREFIX}$RANDOM"
    echo "Creating ACR: $ACR_NAME..."
    az acr create --resource-group $RESOURCE_GROUP --name $ACR_NAME --sku Basic --admin-enabled true
fi
ACR_PASS=$(az acr credential show --name $ACR_NAME --query passwords[0].value -o tsv)

# --- 1. TURN Server Deployment ---
echo "Building Coturn image in ACR: $ACR_NAME..."
az acr build --registry $ACR_NAME --image webrtc-turn:latest turn/

echo "Removing existing TURN container if any..."
az container delete --resource-group $RESOURCE_GROUP --name $TURN_ACI_NAME --yes || true

echo "Deploying TURN server to ACI..."
TURN_DNS_LABEL="webrtc-turn-${RANDOM}"
az container create \
  --resource-group $RESOURCE_GROUP \
  --name $TURN_ACI_NAME \
  --image ${ACR_NAME}.azurecr.io/webrtc-turn:latest \
  --dns-name-label $TURN_DNS_LABEL \
  --ports 3478 \
  --protocol UDP \
  --os-type linux \
  --cpu 1 \
  --memory 1.0 \
  --registry-login-server ${ACR_NAME}.azurecr.io \
  --registry-username $ACR_NAME \
  --registry-password "$ACR_PASS"

TURN_FQDN=$(az container show --resource-group $RESOURCE_GROUP --name $TURN_ACI_NAME --query ipAddress.fqdn -o tsv)
echo "Found TURN server: $TURN_FQDN"

# --- 2. ACS Resource ---
ACS_NAME=$(az communication list --resource-group $RESOURCE_GROUP --query "[0].name" -o tsv)
if [ -z "$ACS_NAME" ]; then
    ACS_NAME="${ACS_NAME_PREFIX}-$RANDOM"
    echo "Creating Azure Communication Service: $ACS_NAME..."
    az communication create --name $ACS_NAME --location "Global" --data-location "Japan" --resource-group $RESOURCE_GROUP
fi
CONNECTION_STRING=$(az communication list-key --name $ACS_NAME --resource-group $RESOURCE_GROUP --query primaryConnectionString -o tsv)

# --- 3. App Server Deployment ---
echo "Building App server image in ACR..."
az acr build --registry $ACR_NAME --image webrtc-server:latest server/

echo "Removing existing App container if any..."
az container delete --resource-group $RESOURCE_GROUP --name $APP_ACI_NAME --yes || true

TURN_ENV="TURN_URL=turn:$TURN_FQDN:3478 TURN_USER=user TURN_PASSWORD=password123"
APP_DNS_LABEL="webrtc-full-stack-${ACS_NAME#*-*-*-}" # Semi-stable DNS

echo "Deploying App server to ACI..."
az container create --resource-group $RESOURCE_GROUP --name $APP_ACI_NAME \
  --image ${ACR_NAME}.azurecr.io/webrtc-server:latest \
  --dns-name-label $APP_DNS_LABEL \
  --ports 3000 --os-type Linux --cpu 1 --memory 1.5 \
  --environment-variables COMMUNICATION_SERVICES_CONNECTION_STRING="$CONNECTION_STRING" $TURN_ENV \
  --registry-login-server ${ACR_NAME}.azurecr.io --registry-username $ACR_NAME --registry-password "$ACR_PASS"

APP_FQDN=$(az container show --resource-group $RESOURCE_GROUP --name $APP_ACI_NAME --query ipAddress.fqdn -o tsv)
echo "Deployment complete! Access at: http://$APP_FQDN:3000/index.html"
