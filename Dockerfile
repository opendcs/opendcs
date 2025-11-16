FROM gradle:9.2-jdk21 AS builder

RUN --mount=type=cache,target=/home/gradle/.gradle
WORKDIR /builddir
COPY . /builddir/
RUN gradle build --info --no-daemon


FROM alpine:3.21.5 AS tomcat_base
RUN apk --no-cache upgrade && \
    apk --no-cache add \
        openjdk21-jre \
        curl \
        bash

RUN mkdir /download && \
    cd /download && \
    wget https://archive.apache.org/dist/tomcat/tomcat-9/v9.0.112/bin/apache-tomcat-9.0.112.tar.gz && \
    echo "fc55589f28bf6659928167461c741649b6005b64285dd81df05bb5ee40f4c6de59b8ee3af84ff756ae1513fc47f5f73070e29313b555e27f096f25881c69841d *apache-tomcat-9.0.112.tar.gz" > checksum.txt && \    
    sha512sum -c checksum.txt && \
    tar xzf apache-tomcat-*tar.gz && \
    mv apache-tomcat-9.0.112 /usr/local/tomcat/ && \
    cd / && \
    rm -rf /download && \
    rm -rf /usr/local/tomcat/webapps/*
CMD ["/usr/local/tomcat/bin/catalina.sh","run"]

FROM tomcat_base AS api

COPY --from=builder /builddir/opendcs-rest-api/build/libs/*.war /usr/local/tomcat/webapps/odcsapi.war
COPY --from=builder /builddir/opendcs-web-client/build/libs/*.war /usr/local/tomcat/webapps/ROOT.war
COPY /docker_files/tomcat/conf/context.xml /usr/local/tomcat/conf/Catalina/localhost/odcsapi.xml
COPY /docker_files/tomcat/conf/tomcat-server.xml /usr/local/tomcat/conf/server.xml
COPY /docker_files/tomcat/conf/setenv.sh /usr/local/tomcat/bin
ENV DCSTOOL_HOME="/opt/opendcs"
EXPOSE 7000