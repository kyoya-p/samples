#!/bin/bash
set -e

# Azure ACS deployment script

RESOURCE_GROUP="rg-webrtc-poc"
# Generate a random name for globally unique ACS
ACS_NAME="acs-webrtc-poc-$RANDOM"
LOCATION="Global"
DATA_LOCATION="Japan" # The data residency location

echo "Creating Resource Group: $RESOURCE_GROUP in japaneast..."
az group create --name $RESOURCE_GROUP --location japaneast

echo "Creating Azure Communication Service: $ACS_NAME..."
az communication create --name $ACS_NAME --location $LOCATION --data-location $DATA_LOCATION --resource-group $RESOURCE_GROUP

echo "Retrieving Connection String..."
CONNECTION_STRING=$(az communication list-key --name $ACS_NAME --resource-group $RESOURCE_GROUP --query primaryConnectionString -o tsv)

if [ -z "$CONNECTION_STRING" ]; then
    echo "Failed to retrieve connection string. Exiting."
    exit 1
fi

echo "Setting up Server..."
export COMMUNICATION_SERVICES_CONNECTION_STRING=$CONNECTION_STRING
cd server
npm install

echo "Starting Server..."
node index.js
