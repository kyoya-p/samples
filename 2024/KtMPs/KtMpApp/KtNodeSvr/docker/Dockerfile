FROM ubuntu:24.04 as b1

WORKDIR /opt
RUN apt update && apt upgrade -y \
 && apt install -y openjdk-17-jre-headless git \
 && apt install -y npm wget && npm -g i n && n latest \
 && git clone --depth 1 https://github.com/kyoya-p/samples
WORKDIR /opt/samples/2024/KtMPs/KtMpApp
RUN sh gradlew kotlinNpmInstall
RUN sh ./gradlew :KtNodeSvr:jsDevelopmentExecutableCompileSync

FROM gcr.io/distroless/nodejs22-debian12
WORKDIR /opt
COPY --from=busybox /bin /bin
COPY --from=b1 /opt/samples/2024/KtMPs/KtMpApp/build/js /opt/

ENV APPKEY=""
ENV TARGETID=""
ENTRYPOINT ["/nodejs/bin/node", "/opt/packages/KtMpApp-KtNodeSvr/kotlin/KtMpApp-KtNodeSvr.js"]
