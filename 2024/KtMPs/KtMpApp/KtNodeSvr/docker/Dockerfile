FROM ubuntu:24.04 as b1

WORKDIR /opt
RUN apt update && apt upgrade -y
RUN apt install -y openjdk-17-jre-headless git
RUN git clone --depth 1 https://github.com/kyoya-p/samples
WORKDIR /opt/samples/2024/KtMPs/KtMpApp
RUN sh gradlew kotlinNpmInstall
#RUN sh gradlew :KtNodeSvr:jsDevelopmentExecutableCompileSync
#RUN ls /opt/samples/2024/KtMPs/KtMpApp/build/js/packages/KtMpApp-KtNodeSvr/kotlin
#
#FROM gcr.io/distroless/nodejs22-debian12
#WORKDIR /opt
#COPY --from=busybox /bin /bin
#COPY --from=b1 /opt/samples/2024/KtMPs/KtMpApp/build/js/packages/KtMpApp-KtNodeSvr/ /opt/
#ENV APPKEY=""
#ENV TARGETID=""
#ENTRYPOINT ["/nodejs/bin/node", "/opt/kotlin/KtMpApp-KtNodeSvr.js"]
