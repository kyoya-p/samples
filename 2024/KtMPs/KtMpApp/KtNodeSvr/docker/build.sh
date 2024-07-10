#!/bin/sh

cp -r ../../build/js/packages/KtMpApp-KtNodeSvr/kotlin/ .
sudo docker build .
