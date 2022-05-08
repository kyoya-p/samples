# syntax = docker/dockerfile:1.3-labs
FROM openjdk:19-slim
WORKDIR /opt
RUN apt update && apt -y upgrade && apt install -y curl zip bash
RUN apt install -y libopencv-dev
#SHELL ["/bin/bash", "-l", "-c"]
COPY . RTMake
WORKDIR /opt/RTMake
RUN sh ./gradlew build
RUN sh ./gradlew run -stacktrace -Djava.library.path=/usr/lib/jni

#ENTRYPOINT ["/bin/sh","./main.sh"]

# sudo docker build -t kyoyap/devenv:image-complement .
# sudo docker push kyoyap/devenv:image-complement
# sudo docker run --rm -v `pwd`:/host kyoyap/devenv:image-complement ${imagefile.png} ${maskfile.png}
