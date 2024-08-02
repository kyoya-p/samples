FROM ubuntu:24.04 as b1

WORKDIR /opt
RUN apt update && apt upgrade -y \
 && apt install -y openjdk-17-jre-headless git

ARG COMMIT="HEAD"
RUN git clone --depth 1 https://github.com/kyoya-p/samples
WORKDIR /opt/samples/2024/FireShell

RUN sh gradlew :kotlinNpmInstall
RUN sh gradlew :RpcAgent:compileProductionExecutableKotlinJs
#RpcAgent:jsProductionExecutableCompileSync

FROM alpine
RUN apk update && apk upgrade
RUN apk add wget
RUN apk add docker
RUN apk add npm
RUN apk add nodejs

WORKDIR /opt
#COPY --from=busybox /bin /bin
#COPY --from=b1 /opt/samples/2024/App/build/js .
#COPY --from=b1 /opt/samples/2024/FireShell/RpcAgent/build/kotlin-webpack/js/productionExecutable/RpcAgent.js .

COPY --from=b1 /opt/samples/2024/FireShell/RpcAgent/build/compileSync/js/main/productionExecutable/kotlin .
COPY --from=b1 /opt/samples/2024/FireShell/build/js/node_modules ./kotlun
WORKDIR /opt/kotlin

ENV APPKEY=""
ENV TARGETID=""
CMD ["node", "Fireshell-RpcAgent.js"]
