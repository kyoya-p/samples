FROM ubuntu:24.04 as b1

WORKDIR /opt
RUN apt update && apt upgrade -y \
 && apt install -y openjdk-17-jre-headless git

# && apt install -y npm wget && npm -g i n && n latest

ARG COMMIT="HEAD"
RUN git clone --depth 1 https://github.com/kyoya-p/samples
WORKDIR /opt/samples/2024/FireShell
RUN sh gradlew KtNodeSvr:kotlinNpmInstall
RUN sh gradlew KtNodeSvr:jsBrowserProductionWebpack

#FROM gcr.io/distroless/nodejs22-debian12
FROM ubuntu:24.04
RUN apt update && apt upgrade -y \
 && apt install -y wget docker.io npm \
 && npm i -g n \
 && n latest

WORKDIR /opt
COPY --from=busybox /bin /bin
#COPY --from=b1 /opt/samples/2024/App/build/js .
COPY --from=b1 /opt/samples/2024/FireShell/KtNodeSvr/build/kotlin-webpack/js/productionExecutable/KtNodeSvr.js .

ENV APPKEY=""
ENV TARGETID=""
CMD ["node", "KtNodeSvr.js"]
