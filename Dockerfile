FROM gradle:8.14-jdk21 AS builder

RUN --mount=type=cache,target=/home/gradle/.gradle
WORKDIR /builddir
COPY . /builddir/
RUN gradle build --info --no-daemon


FROM alpine:3.21.3 AS tomcat_base
RUN apk --no-cache upgrade && \
    apk --no-cache add \
        openjdk21-jre \
        curl \
        bash

RUN mkdir /download && \
    cd /download && \
    wget https://archive.apache.org/dist/tomcat/tomcat-9/v9.0.105/bin/apache-tomcat-9.0.105.tar.gz && \
    echo "904f10378ee2c7c68529edfefcba50c77eb677aa4586cfac0603e44703b0278f71f683b0295774f3cdcb027229d146490ef2c8868d8c2b5a631cf3db61ff9956 *apache-tomcat-9.0.105.tar.gz" > checksum.txt && \
    sha512sum -c checksum.txt && \
    tar xzf apache-tomcat-*tar.gz && \
    mv apache-tomcat-9.0.105 /usr/local/tomcat/ && \
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