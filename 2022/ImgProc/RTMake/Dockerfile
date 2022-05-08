# syntax = docker/dockerfile:1.3-labs
FROM openjdk:19-slim
WORKDIR /opt
RUN apt update && apt -y upgrade && apt install -y curl zip bash
RUN apt install -y libopencv-dev=4.5.1+dfsg-5
COPY . RTMake
WORKDIR /opt/RTMake
RUN sh ./gradlew build
#RUN sh ./gradlew run -stacktrace -Djava.library.path=/usr/lib/jni

WORKDIR /host
ENTRYPOINT ["/bin/sh","/opt/RTMake/gradlew","run","-Djava.library.path=/usr/lib/jni"]

# sudo docker build -t kyoyap/tools:image-adj1 .
# sudo docker push kyoyap/tools:image-adj1

# sudo docker run --rm -v `pwd`:/host kyoyap/tools:image-adj1 ${imagefile.png} ${maskfile.png}
