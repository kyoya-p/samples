# syntax = docker/dockerfile:1.3-labs
FROM adoptopenjdk/openjdk11
WORKDIR /opt
RUN apt update && apt -y upgrade && apt install -y curl zip bash
RUN apt install -y libopencv-dev=4.5.4+dfsg-9+b5
#COPY . RTMake
WORKDIR /opt/RTMake
#RUN sh ./gradlew shadowJar
COPY build/libs RTMake/build/libs
ENTRYPOINT ["/bin/bash","/usr/local/openjdk-19/bin/java","-Djava.library.path=/usr/lib/jni","-jar","/opt/RTMake/build/libs/RTMake-1.0-SNAPSHOT-all.jar", "/host"]

# sudo docker build -t kyoyap/tools:image-adj1 .
# sudo docker push kyoyap/tools:image-adj1

# sudo docker run --rm -v `pwd`:/host kyoyap/tools:image-adj1 ${imagefile.png} ${maskfile.png}
