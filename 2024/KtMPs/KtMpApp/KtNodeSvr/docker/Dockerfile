FROM ubuntu:24.04 as b1

WORKDIR /opt
RUN apt update && apt upgrade -y \
 && apt install -y openjdk-17-jre-headless git \
 && apt install -y npm wget && npm -g i n && n latest

RUN git clone --depth 1 https://github.com/kyoya-p/samples
WORKDIR /opt/samples/2024/KtMPs/KtMpApp
RUN sh gradlew kotlinNpmInstall
#RUN sh ./gradlew :KtNodeSvr:jsDevelopmentExecutableCompileSync
RUN sh gradlew :KtNodeSvr:jsBrowserWebpack

FROM gcr.io/distroless/nodejs22-debian12
WORKDIR /opt
COPY --from=busybox /bin /bin
#COPY --from=b1 /opt/samples/2024/KtMPs/KtMpApp/build/js /opt/
COPY --from=b1 /opt/samples/2024/KtMPs/KtMpApp/KtNodeSvr/build/kotlin-webpack/js/productionExecutable/KtNodeSvr.js /opt/

ENV APPKEY=""
ENV TARGETID=""
#ENTRYPOINT ["/nodejs/bin/node", "/opt/packages/KtMpApp-KtNodeSvr/kotlin/KtMpApp-KtNodeSvr.js"]
ENTRYPOINT ["/nodejs/bin/node", "/opt/KtNodeSvr.js"]
