#! /bin/bash
RG=g3
LOCATION=westus
FUNCAPP=${RG}a221001
DBACC=${FUNCAPP}db
DB=${DBACC}1

az group create -n $RG --location $LOCATION
az storage account create -n s$FUNCAPP  -l $LOCATION -g $RG 
az functionapp create -n $FUNCAPP -c $LOCATION -g $RG --storage-account s$FUNCAPP --os-type Linux --functions-version 4 --runtime node --runtime-version 16
az cosmosdb create -n $DBACC -g $RG
az cosmosdb sql database create -a $DBACC -g $RG -n $DB


