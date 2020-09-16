#
# Docker build --build-arg PASS={password}
#

FROM adoptopenjdk/openjdk11:aarch64-ubuntu-jdk-11.0.7_10-slim
RUN apt-get update \
    && apt-get install -y \
        git \
    	unzip

WORKDIR /opt

ARG T1
RUN git clone https://github.com/kyoya-p/samples
WORKDIR samples/2020firebase01

RUN sh ./gradlew jar

#RUN env GOOGLE_APPLICATION_CREDENTIALS=./road-to-iot-8efd3bfb2ccd.json bash ./gradlew jar

ARG PASS
RUN unzip -P $PASS road-to-iot-8efd3bfb2ccd.zip

#FROM jre

CMD java -jar build/libs/2020firebase.jar
