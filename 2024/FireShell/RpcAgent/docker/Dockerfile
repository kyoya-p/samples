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

FROM alpine:3.20.2
RUN apk update && apk upgrade\
 && apk add wget\
 && apk add docker\
 && apk add npm\
 && apk add nodejs
RUN apk add containerd-ctr

WORKDIR /opt
#COPY --from=busybox /bin /bin
COPY --from=b1 /opt/samples/2024/FireShell/RpcAgent/build/compileSync/js/main/productionExecutable/kotlin .
COPY --from=b1 /opt/samples/2024/FireShell/build/js/node_modules ./node_modules
WORKDIR /opt

ENV USERID=
ENV PASSWORD=
ENV http_proxy=
ENV https_proxy=
ENV no_proxy=
CMD ["node", "FireShell-RpcAgent.js"]
