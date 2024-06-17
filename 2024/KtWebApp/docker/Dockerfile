FROM tomcat:jre17-temurin
RUN apt update \
 && apt upgrade -y \
 && apt install -y git

WORKDIR /opt
RUN git clone --depth 1 https://github.com/kyoya-p/samples

WORKDIR /opt/2024/KtWebApp
RUN sh gradlew war
