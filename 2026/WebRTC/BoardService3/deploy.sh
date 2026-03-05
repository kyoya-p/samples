#!/bin/bash
set -e

# Azure ACS deployment script (Unified: Resource + Build + ACI)

RESOURCE_GROUP="rg-webrtc-poc"
LOCATION="japaneast"
ACS_NAME_PREFIX="acs-webrtc-poc"
ACR_NAME_PREFIX="acrwebrtcpoc"

echo "Using Resource Group: $RESOURCE_GROUP in $LOCATION..."
az group create --name $RESOURCE_GROUP --location $LOCATION || true

# ACS Resource
ACS_NAME=$(az communication list --resource-group $RESOURCE_GROUP --query "[0].name" -o tsv)
if [ -z "$ACS_NAME" ]; then
    ACS_NAME="${ACS_NAME_PREFIX}-$RANDOM"
    echo "Creating Azure Communication Service: $ACS_NAME..."
    az communication create --name $ACS_NAME --location "Global" --data-location "Japan" --resource-group $RESOURCE_GROUP
fi

CONNECTION_STRING=$(az communication list-key --name $ACS_NAME --resource-group $RESOURCE_GROUP --query primaryConnectionString -o tsv)

# ACR Resource
ACR_NAME=$(az acr list --resource-group $RESOURCE_GROUP --query "[0].name" -o tsv)
if [ -z "$ACR_NAME" ]; then
    ACR_NAME="${ACR_NAME_PREFIX}$RANDOM"
    echo "Creating Azure Container Registry: $ACR_NAME..."
    az acr create --resource-group $RESOURCE_GROUP --name $ACR_NAME --sku Basic --admin-enabled true
fi

# Build and Push
echo "Building container image in ACR..."
az acr build --registry $ACR_NAME --image webrtc-server:latest server/

# Deploy to ACI
echo "Deploying to Azure Container Instance..."
ACR_PASS=$(az acr credential show --name $ACR_NAME --query passwords[0].value -o tsv)
ACI_NAME="webrtc-full-stack"
DNS_LABEL="webrtc-full-stack-${ACS_NAME#*-*-*-}" # Semi-stable DNS

# 強制再作成
echo "Removing existing container if any..."
az container delete --resource-group $RESOURCE_GROUP --name $ACI_NAME --yes || true

az container create --resource-group $RESOURCE_GROUP --name $ACI_NAME \
  --image ${ACR_NAME}.azurecr.io/webrtc-server:latest \
  --dns-name-label $DNS_LABEL \
  --ports 3000 --os-type Linux --cpu 1 --memory 1.5 \
  --environment-variables COMMUNICATION_SERVICES_CONNECTION_STRING="$CONNECTION_STRING" \
  --registry-login-server ${ACR_NAME}.azurecr.io --registry-username $ACR_NAME --registry-password "$ACR_PASS"

FQDN=$(az container show --resource-group $RESOURCE_GROUP --name $ACI_NAME --query ipAddress.fqdn -o tsv)
echo "Deployment complete! Access at: http://$FQDN:3000/index.html"
