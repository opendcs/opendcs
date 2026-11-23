# syntax=docker/dockerfile:1
# check=skip=SecretsUsedInArgOrEnv
# The values are either not secrets or are placeholders


# Depends on having buildx available for the --mount feature
# use buildplatform for the builder, since we're building a java application
# it's already cross platform.
FROM --platform=$BUILDPLATFORM eclipse-temurin:25-jdk-jammy AS builder

RUN --mount=type=cache,target=/var/cache/apt \
    apt-get update && apt-get -y upgrade && \
    apt-get install -y python3-venv python3-pip git
WORKDIR /app

COPY . .

RUN --mount=type=cache,target=/root \
    ./gradlew --no-daemon installDist war -Dno.docs=true --info
# end initial build

FROM --platform=$BUILDPLATFORM scratch AS export
COPY --from=builder --parents /app .

FROM eclipse-temurin:21-jre-alpine AS opendcs_base

RUN apk upgrade --no-cache
RUN apk add --no-cache \
    bash
RUN addgroup opendcs && \
    adduser -D opendcs -G opendcs
WORKDIR /opt/opendcs
COPY --from=builder /app/install/build/install/opendcs/ /opt/opendcs
COPY docker_scripts/env.sh /opt/opendcs/
COPY docker_scripts/logback.xml /opt/opendcs/
WORKDIR /opt/opendcs/bin
RUN rm *.bat && \
    chmod +x /opt/opendcs/bin/*

ENV DCSTOOL_HOME=/opt/opendcs
ENV DECODES_INSTALL_DIR=${DCSTOOL_HOME}
ENTRYPOINT ["/opt/opendcs/env.sh"]
LABEL org.opencontainers.image.source=https://github.com/opendcs/opendcs
LABEL org.opencontainers.image.documentation=https://github.com/opendcs/opendcs/README.docker.md
# end baseline setup

FROM opendcs_base AS lrgs
COPY docker_scripts/lrgs.sh /
USER opendcs:opendcs
WORKDIR /lrgs_home
ENV LRGSHOME=/lrgs_home
# DDS Port
EXPOSE 16003
CMD ["/lrgs.sh"]


FROM opendcs_base AS tsdbapp
COPY docker_scripts/tsdb_config.sh /opt/opendcs
COPY docker_scripts/decodes.properties /opt/opendcs/decodes.properties.template
USER opendcs:opendcs
WORKDIR /dcs_user_dir
ENV DCSTOOL_USERDIR=/dcs_user_dir
ENV DATABASE_TYPE=xml
ENV DATABASE_URL="${DCSTOOL_USERDIR}/edit-db"
# Ignore Secrets In Env warning here, this just explains how to reference the secrets.
ENV DB_AUTH="env-auth-source:username=DATABASE_USERNAME,password=DATABASE_PASSWORD"
ENV DATABASE_DRIVER=""
ENV CWMS_OFFICE=""
ENV DATATYPE_STANDARD=""
ENV KEYGENERATOR=""
ENV APPLICATION_NAME="RoutingScheduler"

FROM tsdbapp AS routingscheduler
COPY docker_scripts/routingscheduler.sh /
RUN mkdir routstat

CMD ["/routingscheduler.sh"]

FROM tsdbapp AS compproc
COPY docker_scripts/compproc.sh /
ENV APPLICATION_NAME="compproc"

CMD ["/compproc.sh"]

FROM tsdbapp AS compdepends
COPY docker_scripts/compdepends.sh /
ENV APPLICATION_NAME="compdepends"

CMD ["/compdepends.sh"]

FROM alpine:3.24.4 AS tomcat_base
RUN apk --no-cache upgrade && \
    apk --no-cache add \
        openjdk21-jre \
        curl \
        bash
RUN addgroup tomcat && adduser -D tomcat -G tomcat
RUN mkdir /download && \
    cd /download && \
    wget https://dlcdn.apache.org/tomcat/tomcat-11/v11.0.29/bin/apache-tomcat-11.0.29.tar.gz && \
    echo "be277ce58876a584653f980fd695c82e6708029f05fc7ad9fcfbdcede3836ae6ffa4f858eac26de6ff938f130f31158be8a54b5f546393ea1ad5da0cd202873c *apache-tomcat-11.0.29.tar.gz" > checksum.txt && \
    sha512sum -c checksum.txt && \
    tar xzf apache-tomcat-*tar.gz && \
    mv apache-tomcat-11.0.29 /usr/local/tomcat/ && \
    cd / && \
    rm -rf /download && \
    rm -rf /usr/local/tomcat/webapps/* && \
    chown -R root:tomcat /usr/local/tomcat
RUN mkdir -p /var/tomcat/webapps && mkdir -p /var/tomcat/conf && \
    cp /usr/local/tomcat/conf/web.xml /var/tomcat/conf/web.xml && \
    chown -R tomcat:tomcat /var/tomcat

ENV CATALINA_HOME=/usr/local/tomcat
ENV CATALINA_BASE=/var/tomcat
USER tomcat:tomcat
CMD ["/usr/local/tomcat/bin/catalina.sh","run"]

FROM tomcat_base AS web-api

COPY --from=builder /app/java/opendcs-rest-api/build/libs/*.war /var/tomcat/webapps/odcsapi.war
COPY --from=builder /app/javascript/opendcs-web-ui-react/build/libs/*.war /var/tomcat/webapps/ROOT.war
COPY /docker_files/tomcat/conf/context.xml /var/tomcat/conf/Catalina/localhost/odcsapi.xml
COPY /docker_files/tomcat/conf/tomcat-server.xml /var/tomcat/conf/server.xml
COPY /docker_files/tomcat/conf/setenv.sh /var/tomcat/bin/setenv.sh
ENV DCSTOOL_HOME="/opt/opendcs"

FROM tsdbapp AS migration
COPY docker_scripts/migration.sh /
CMD ["/migration.sh"]
ENV APPLICATION_NAME="migration"
# Current valid options are OpenDCS-Postgres (default), OpenDCS-Oracle, and CWMS-Oracle
ENV DATABASE_IMPLEMENTATION=OpenDCS-Postgres
