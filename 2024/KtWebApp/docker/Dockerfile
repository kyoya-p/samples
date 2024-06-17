FROM eclipse-temurin:17-jdk as builder
RUN apt update \
 && apt upgrade -y \
 && apt install -y git tomcat9 tomcat9-admin

WORKDIR /opt
RUN git clone --depth 1 https://github.com/kyoya-p/samples

WORKDIR /opt/samples/2024/KtWebApp
RUN sh gradlew war

FROM tomcat:jre17-temurin
COPY --from=builder  /opt/samples/2024/KtWebApp/KtServlet/build/libs/KtServlet*.war /var/lib/tomcat9/webapps/

CMD "/usr/local/tomcat/bin/catalina.sh start"
