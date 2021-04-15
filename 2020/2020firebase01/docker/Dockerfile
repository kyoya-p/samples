#
# docker build --build-arg PASS={password}
# docker build --build-arg PASS={password} --build-arg T2="`date`"
#

FROM adoptopenjdk/openjdk11:jdk-11.0.7_10-slim AS builder
RUN apt-get update \
    && apt-get install -y \
        git \
    	unzip

WORKDIR /opt

ARG T1
RUN git clone https://github.com/kyoya-p/samples
WORKDIR /opt/samples/2020firebase01
RUN sh ./gradlew --no-daemon build

ARG T2
RUN git pull
RUN sh ./gradlew --no-daemon build
RUN sh ./gradlew --no-daemon jar

ARG PASS
RUN echo $PASS
RUN unzip -P $PASS road-to-iot-8efd3bfb2ccd.zip


#
#
#

FROM adoptopenjdk/openjdk11:jre-11.0.7_10
WORKDIR /opt
COPY --from=builder /opt/samples/2020firebase01/build/libs/2020firebase01.jar .

#CMD env GOOGLE_APPLICATION_CREDENTIALS=./road-to-iot-8efd3bfb2ccd.json sh ./gradlew --no-daemon run
CMD env GOOGLE_APPLICATION_CREDENTIALS=./road-to-iot-8efd3bfb2ccd.json java -jar 2020firebase01.jar



