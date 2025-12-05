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
    wget https://archive.apache.org/dist/tomcat/tomcat-11/v11.0.14/bin/apache-tomcat-11.0.14.tar.gz && \
    echo "832a02e6b8979192b428f41f2586181d14134877d7703a13bec9760ac4722b14e604a914239437657d552dc01e3f9422e2f69b8ab94ad3d85dc03dff2eb8df8c *apache-tomcat-11.0.14.tar.gz" > checksum.txt && \    
    sha512sum -c checksum.txt && \
    tar xzf apache-tomcat-*tar.gz && \
    mv apache-tomcat-11.0.14 /usr/local/tomcat/ && \
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