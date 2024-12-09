FROM eclipse-temurin:21-jdk AS builder

RUN apt-get update
RUN apt-get install -y git

WORKDIR /builder
RUN git clone --depth=1 https://github.com/kyoya-p/samples
ARG BULID=/builder/samples/2024/KotlinRPC/ContainerOp
WORKDIR $BULID
RUN sh gradlew server:shadowJar
RUN sh gradlew composeApp:wasmJsBrowserProductionWebpack


FROM eclipse-temurin:21-jre
WORKDIR /opt
ARG SERVER=$BUILD/server/build
ARG COMPOSEAPP=$BUILD/composeApp/build
COPY --from=builder $SERVER/libs/server-all.jar .
COPY --from=builder $COMPOSEAPP/kotlin-webpack/wasmJs/productionExecutable/* .

CMD "/bin/bash"
