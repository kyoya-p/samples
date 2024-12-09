FROM eclipse-temurin:21-jdk AS builder

RUN apt-get update
RUN apt-get upgrade -y
RUN apt-get install -y --fix-missing git
RUN apt-get install -y npm
RUN npm i -g n
RUN n latest

WORKDIR /builder
RUN git clone --depth=1 https://github.com/kyoya-p/samples
ARG PROJ=/builder/samples/2024/KotlinRPC/ContainerOp
WORKDIR $PROJ
RUN sh gradlew server:shadowJar
RUN #sh gradlew composeApp:wasmJsBrowserDistribution
RUN sh gradlew kotlinNpmInstall wasmJsBrowserDistribution


FROM eclipse-temurin:21-jre
WORKDIR /opt
ARG PROJ=/builder/samples/2024/KotlinRPC/ContainerOp
COPY --from=builder $PROJ/server/build/libs/server-all.jar .
COPY --from=builder $PROJ/composeApp/build/dist/wasmJs/productionExecutable/* ./

#CMD "/bin/bash"
CMD "/opt/java/openjdk/bin/java -jar server-all.jar"