# syntax = docker/dockerfile:1.3-labs
FROM openjdk:19-slim
WORKDIR /opt
RUN apt update && apt -y upgrade && apt install -y curl zip bash
SHELL ["/bin/bash", "-l", "-c"]
COPY tools tools
WORKDIR /opt/tools
RUN sh ./gradlew build
RUN apt-get install -y libopencv-dev
RUN sh ./gradlew run -stacktrace

#ENTRYPOINT ["/bin/sh","./main.sh"]

# sudo docker build -t kyoyap/devenv:image-complement .
# sudo docker push kyoyap/devenv:image-complement
# sudo docker run --rm -v `pwd`:/host kyoyap/devenv:image-complement ${imagefile.png} ${maskfile.png}
