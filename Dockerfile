# Depends on having buildx available for the --mount feature
FROM openjdk:8-jdk-bullseye as builder

RUN --mount=type=cache,target=/var/cache/apt \ 
    apt-get update && apt-get -y upgrade && \
    apt-get install -y ant
WORKDIR /app

COPY . .

RUN --mount=type=cache,target=/root \
    ant stage -Dno.docs=true
# end initial build

FROM openjdk:8-jre-alpine as opendcs_base

RUN apk update && apk add bash
RUN addgroup opendcs && \
    adduser -D opendcs -G opendcs
WORKDIR /opt/opendcs
COPY --from=builder /app/stage /opt/opendcs
COPY docker_scripts/env.sh /opt/opendcs/
WORKDIR /opt/opendcs/bin
RUN rm *.bat && \
    chmod +x /opt/opendcs/bin/*

ENV DCSTOOL_HOME=/opt/opendcs DECODES_INSTALL_DIR=${DCSTOOL_HOME}
ENTRYPOINT ["/opt/opendcs/env.sh"]
# end baseline setup

FROM opendcs_base as lrgs
COPY docker_scripts/lrgs.sh /
USER opendcs:opendcs
VOLUME lrgs_home
WORKDIR /lrgs_home
ENV LRGSHOME=/lrgs_home
ENV LRGS_ADMIN_PASSWORD=""
# DDS Port
EXPOSE 16003 
CMD ["/lrgs.sh"]


FROM opendcs_base as tsdbapp
COPY docker_scripts/tsdb_config.sh /opt/opendcs
COPY docker_scripts/decodes.properties /opt/opendcs/decodes.properties.template
USER opendcs:opendcs
VOLUME /dcs_user_dir
WORKDIR /dcs_user_dir
ENV DCSTOOL_USERDIR=/dcs_user_dir
ENV DATABASE_TYPE=xml
ENV DATABASE_URL="${DCSTOOL_USERDIR}/edit-db"
ENV DB_AUTH="env-auth-source:username=DATABASE_USERNAME,password=DATABASE_PASSWORD"
ENV DATABASE_USERNAME=""
ENV DATABASE_PASSWORD=""
ENV DATABASE_DRIVER=""
ENV CWMS_OFFICE=""
ENV DATATYPE_STANDARD=""
ENV KEYGENERATOR=""
ENV APPLICATION_NAME="RoutingScheduler"

FROM tsdbapp as routingscheduler
COPY docker_scripts/routingscheduler.sh /
RUN mkdir routstat

CMD ["/routingscheduler.sh"]

FROM tsdbapp as compproc
COPY docker_scripts/compproc.sh /
ENV APPLICATION_NAME="compproc"

CMD ["/compproc.sh"]

FROM tsdbapp as compdepends
COPY docker_scripts/compdepends.sh /
ENV APPLICATION_NAME="compdepends"

CMD ["/compdepends.sh"]