FROM eclipse-temurin:21-jre
RUN apt-get update
RUN apt-get install -y containerd

WORKDIR /opt
ARG PROJ=/builder/samples/2024/KotlinRPC/ContainerOp
COPY build/appWeb/ ./

CMD ["java","-jar","server-all.jar"]
