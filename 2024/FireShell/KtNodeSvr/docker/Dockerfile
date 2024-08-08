FROM ubuntu:24.04 as b1

WORKDIR /opt
RUN apt update && apt upgrade -y \
 && apt install -y openjdk-17-jre-headless git

# && apt install -y npm wget && npm -g i n && n latest

ARG COMMIT="HEAD"
RUN git clone --depth 1 https://github.com/kyoya-p/samples
WORKDIR /opt/samples/2024/FireShell
RUN sh gradlew kotlinNpmInstall
RUN sh ./gradlew :KtNodeSvr:jsDevelopmentExecutableCompileSync
#RUN sh gradlew :KtNodeSvr:jsBrowserWebpack

#FROM gcr.io/distroless/nodejs22-debian12
FROM ubuntu:24.04
RUN apt update && apt upgrade -y \
 && apt install -y wget docker.io npm \
 && npm i -g n \
 && n latest

WORKDIR /opt
#COPY --from=busybox /bin /bin
COPY --from=b1 /opt/samples/2024/FireShell/build/js .
#COPY --from=b1 /opt/samples/2024/KtMPs/KtMpApp/KtNodeSvr/build/kotlin-webpack/js/productionExecutable/KtNodeSvr.js .

ENV APPKEY=""
ENV TARGETID=""
#ENTRYPOINT ["/nodejs/bin/node", "/opt/packages/KtMpApp-KtNodeSvr/kotlin/KtMpApp-KtNodeSvr.js"]
#ENTRYPOINT ["/nodejs/bin/node", "/opt/KtNodeSvr.js"]
#CMD ["node", "KtNodeSvr.js"]
CMD ["node", "packages/KtMpApp-KtNodeSvr/kotlin/KtMpApp-KtNodeSvr.js"]
