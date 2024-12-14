FROM eclipse-temurin:21-jdk AS builder

RUN apt-get update
RUN apt-get -y upgrade
RUN apt-get install -y --fix-missing git
#RUN apt-get install -y npm
#RUN npm i -g n
#RUN n latest

WORKDIR /builder
RUN git clone --depth=1 https://github.com/kyoya-p/samples
ARG PROJ=/builder/samples/2024/KotlinRPC/ContainerOp
WORKDIR $PROJ
RUN sh gradlew buildAppWeb

FROM eclipse-temurin:21-jre
RUN apt-get update
RUN apt-get install -y containerd

WORKDIR /opt
ARG PROJ=/builder/samples/2024/KotlinRPC/ContainerOp
COPY --from=builder $PROJ/build/appWeb/ ./

#CMD "/opt/java/openjdk/bin/java -jar server-all.jar"
CMD ["java","-jar","server-all.jar"]
