#!/bin/sh
( cd ..; sh gradlew build )
cp ../build/distributions/fsCustomAuthSvr.tar .
cp ../road-to-iot-8efd3bfb2ccd.json .
