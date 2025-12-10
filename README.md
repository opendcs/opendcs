[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=opendcs_rest_api&metric=alert_status)](https://sonarcloud.io/summary/new_code?id=opendcs_rest_api)
[![Security Rating](https://sonarcloud.io/api/project_badges/measure?project=opendcs_rest_api&metric=security_rating)](https://sonarcloud.io/summary/new_code?id=opendcs_rest_api)
[![Maintainability Rating](https://sonarcloud.io/api/project_badges/measure?project=opendcs_rest_api&metric=sqale_rating)](https://sonarcloud.io/summary/new_code?id=opendcs_rest_api)
[![Lines of Code](https://sonarcloud.io/api/project_badges/measure?project=opendcs_rest_api&metric=ncloc)](https://sonarcloud.io/summary/new_code?id=opendcs_rest_api)
[![Coverage](https://sonarcloud.io/api/project_badges/measure?project=opendcs_rest_api&metric=coverage)](https://sonarcloud.io/summary/new_code?id=opendcs_rest_api)

# rest_api
OpenDCS Rest API is web application that provides access to the OpenDCS database using JSON (Java Script Object Notation).
OpenDCS Rest API is intended to run as a stand-alone Java program. It uses embedded JETTY to implement the web services.

# Structure
./opendcs-rest-api - contains source files for the OpenDCS REST API
./opendcs-web-client - contains source files for the OpenDCS Web Application Client
./opendcs-integration-test - contains scripts for running embedded tomcat to deploy the REST API and Web Client wars for testing.


# Building

JDK 21 and Node 22 or higher are required to build the project.

## OPENDCS API
The gradle task `./gradlew :opendcs-rest-api:war` will create a war file in the `build/libs` directory.

The SwaggerUI location can be found at the relative url path of /<context>/swaggerui.
Assuming the context is 'odcsapi', an example of the SwaggerUI location is http://localhost:8080/odcsapi/swaggerui.
These files are being served up from the resource file 'SwaggerResources.java' file located at 
'src/main/java/org/opendcs/odcsapi/res/SwaggerResources.java'.

### web.xml configurations
The bundled [web.xml](opendcs-rest-api/src/main/webapp/WEB-INF/web.xml) contains the following
properties that should be configured for your system.
- `opendcs.rest.api.authorization.type` - supports a comma separated list of authorization types. These can include basic,sso,openid. See section on authorization for details.
- `opendcs.rest.api.authorization.expiration.duration` - denotes the duration that an authorization attempt is valid for. Defaults to 15 minutes.
- `opendcs.rest.api.cwms.office` - office id specific to CWMS systems. This is the office the authorizing user will check privileges for.
- `opendcs.rest.api.authorization.jwt.jwkset.url` - for openid authorization this is the JWK Set URL
- `opendcs.rest.api.authorization.jwt.issuer.url`  - for openid authorization this is the Issuer URL

## OPENDCS Web Client
The gradle task `./gradlew :opendcs-web-client:war` will create a war file in the `build/libs` directory.

# CI/CD
The GitHub Action workflow [default.yml](./.github/workflows/default.yml) contains the primary CI/CD pipeline for the project.
This workflow is responsible for compiling, testing, analyzing, and packaging the project.

WAR and TAR files are generated as part of the CI/CD pipeline and are uploaded as artifacts in the GitHub Action.

## Static Code Analysis
During the analysis step of the CI/CD pipeline, the project is analyzed using SonarLint.

To view the analysis results, navigate to the project on the SonarCloud website. 
[OpenDCS REST API SonarCloud](https://sonarcloud.io/project/overview?id=opendcs_rest_api)

The gradle task `./gradlew sonar` will run a SonarLint analysis on the project.  
The SonarLint analysis will be run against the SonarCloud server with an analysis uploaded to the OpenDCS REST API project.
**The SonarCloud Quality Gate status for the Pull Request is reported in the Pull Request checks section for all PR's targeting the main branch.**
The workflow [default.yml](./.github/workflows/default.yml) includes the analysis step for GitHub Actions.

Jacoco code coverage scans are uploaded to SonarCloud. In order to reduce reporting redundancy, the Jacoco HTML reports are 
not available through the GitHub interface, but they can be generated locally through the gradle plugin.

## OWASP Zap API Scan
The workflow [owasp_zap.yml](./.github/workflows/owasp_zap.yml) runs the OWASP Zap API scan to generate an HTML report
detailing vulnerabilities in the OpenDCS REST API based on the OpenAPI documentation. OWASP Zap API scan 
[documentation](https://www.zaproxy.org/docs/docker/api-scan/) details the various options and rules for the scan.
The OWASP Zap HTML report is attached to the GitHub Workflow run along with a shortened summary rendered in Markdown.
At this time, the default configuration is used and the results are not used to gatekeep Pull Requests.

## Java Version Compatibility
OpenDCS REST API targets JDK 21 compatibility for distribution. 
In order to ensure compatibility with future versions of Java, the GitHub workflow [java_compatibility.yml](./.github/workflows/java_compatibility.yml)
will run a matrix build against the project to ensure compatibility with other JDK versions.

# Releases
Artifact releases from this repository are found in the GitHub Releases page for the repository. 

They can be created by using the GitHub interface. 
The GitHub workflow [publish.yml](./.github/workflows/publish.yml) will attach the WAR files and TAR files once the release is published.

# Codespaces
A basic [GitHub Codespaces](https://docs.github.com/en/codespaces/overview) configuration setup with a dev container with
Java, Gradle, and SonarCloud integrations. Additionally, GitHub Copilot is available for users with appropriate licensing.
The Codespaces are intended for the easy and consistent onboarding of developers who may not have access or experience with
heavy-weight development IDE's.

Configuration for the dev container is found in [devcontainer.json](./.devcontainer/devcontainer.json)

# Authorization
The OpenDCS REST API supports three authorization mechanisms Basic Authentication, Container Single Sign-On, and OpenID Connect.

When the client attempts to access endpoints that are not marked with the Guest role the authorization mechanisms are checked to determine
which roles are currently granted to the client. See [./opendcs-rest-api/README.md](opendcs-rest-api/README.md) for more info.


# Running the docker image

Please note that we are still sorting out how we are handling authentication and credentials in different contexts

## OpenDCS-Postgres

This assumes the database has already been setup and you have created an "application" user named dcs_app

```bash
# network is optional and only needs to be used if you're database in hosted in docker, set the name appropriately.
docker run  --network database_net \
            -p 7000:7000 \
            --rm \
            -e DB_URL=jdbc:postgres:db:5324/dcs \
            -e DB_USERNAME=dcs_app \
            -e DB_PASSWORD=dcs_app_password \
            -e DB_VALIDATION_QUERY="select 1" \
            -e DB_MAX_CONNECTIONS=10 \
            -e DB_MAX_IDLE=5 \
            -e DB_MIN_IDLE=1 \
            -e DB_DRIVER_CLASS="org.postgresql.Driver" \
            ghcr.io/opendcs/web-api:latest
```

## CWMS-Oracle

This assumes the CWMS Database and OpenDCS Schema (CCP) have already been setup.

```bash
# network is optional and only needs to be used if you're database in hosted in docker, set the name appropriately.
docker run  --network database_net \
            -p 7000:7000 \
            --rm \
            -e DB_URL=jdbc:oracle:thin:@//cwmsdb:1521/CWMSTEST \
            -e DB_USERNAME=ccp_app \
            -e DB_PASSWORD=ccp \
            -e DB_VALIDATION_QUERY="select 1 from dual" \
            -e DB_MAX_CONNECTIONS=10 \
            -e DB_MAX_IDLE=5 \
            -e DB_MIN_IDLE=1 \
            -e DB_DRIVER_CLASS="oracle.jdbc.driver.OracleDriver" \
            ghcr.io/opendcs/web-api:latest
```


# Using the Docker compose

## Service Connections

| Service   | Host / IP      | Port (Host → Container) | Connection String Example                            |
|-----------|----------------|--------------------------|------------------------------------------------------|
| **db** (Postgres) | `localhost` or container name `db` | `5432 -> 5432` | `jdbc:postgresql://localhost:5432/dcs` <br> (user: `dcs_owner`, pass: `dcs_password`) |
| **migration** | Runs one-time init job, no exposed port | – | Uses `DATABASE_URL=jdbc:postgresql://db:5432/dcs` internally |
| **api** | `localhost` | `7000 -> 7000` | `http://localhost:7000` |
| **pgadmin** | `localhost` | `5050 -> 80` | `http://localhost:5050` <br> (login: `user@exaple.com` / `admin123`) |

---

## Quick Start

1. Clone Repo: `git clone https://github/opendcs/rest_api`
2. Run docker compose: `docker compose up`
3. Navigate to: [http://localhost:7000](http://localhost:7000)
4. Enter username: `app`
5. Enter password: `app_pass` (from `docker-compose.yaml`)
6. BONUS: Manipulate database via web interface [http://localhost:5050](http://localhost:5050) or via exposed port
