FROM eclipse-temurin:17-jdk
RUN apt update \
 && apt upgrade -y \
 && apt install -y git 

RUN apt install -y tomcat9 tomcat9-admin \
 && systemctl enable tomcat9

WORKDIR /opt
RUN git clone --depth 1 https://github.com/kyoya-p/samples

WORKDIR /opt/samples/2024/KtWebApp
RUN sh gradlew war

RUN copy /var/lib/Tomcat8/webapps