[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=opendcs_rest_api&metric=alert_status)](https://sonarcloud.io/summary/new_code?id=opendcs_rest_api)
[![Security Rating](https://sonarcloud.io/api/project_badges/measure?project=opendcs_rest_api&metric=security_rating)](https://sonarcloud.io/summary/new_code?id=opendcs_rest_api)
[![Maintainability Rating](https://sonarcloud.io/api/project_badges/measure?project=opendcs_rest_api&metric=sqale_rating)](https://sonarcloud.io/summary/new_code?id=opendcs_rest_api)
[![Lines of Code](https://sonarcloud.io/api/project_badges/measure?project=opendcs_rest_api&metric=ncloc)](https://sonarcloud.io/summary/new_code?id=opendcs_rest_api)

# rest_api
OpenDCS Rest API is web application that provides access to the OpenDCS database using JSON (Java Script Object Notation).
OpenDCS Rest API is intended to run as a stand-alone Java program. It uses embedded JETTY to implement the web services.

# Structure
./opendcs-rest-api - contains source files for the OpenDCS REST API
./opendcs-rest-api-jetty - contains build scripts for generating a standalone Jetty installer for the OPENDCS REST API   
./opendcs-web-client - contains source files for the OpenDCS Web Application Client
./opendcs-web-client-jetty - contains build scripts for generating a standalone Jetty installer for the web client

# Installation and Configuration
There are two types of installations/configurations.  One is embedded Jetty bundles and the other is WAR files.

## OPENDCS API
The gradle task `./gradlew :opendcs-rest-api:war` will create a war file in the `build/libs` directory.

The SwaggerUI location can be found at the relative url path of /<context>/swaggerui.
Assuming the context is 'odcsapi', an example of the SwaggerUI location is http://localhost:8080/odcsapi/swaggerui.
These files are being served up from the resource file 'SwaggerResources.java' file located at 
'src/main/java/org/opendcs/odcsapi/res/SwaggerResources.java'.

## OPENDCS Web Client
The gradle task `./gradlew :opendcs-web-client:war` will create a war file in the `build/libs` directory.

##	OPENDCS API Jetty Standalone Installer

The Root URL for the service is specified using the command line arguments when the JETTY server is started. If the defaults are used, you can access the service from the local machine with:
http://localhost:8080/odcsapi/function-call

For example, to get a list of platform references:
http://localhost:8080/odcsapi/platformrefs

The gradle task `./gradlew :opendcs-rest-api-jetty:bundle` will create a tar ball in the `build/distributions` directory.  
This tar ball can be extracted and run with the `start.sh` script.  
The `start.sh` script can be configured with the following arguments:

-	Create a shell script, to run start.sh with some extra configurations.
     - Example shell script:
        ```
        export DCSTOOL_HOME=/home/opendcs/OPENDCS
        export DCSTOOL_USERDIR=/home/opendcs
        export JAVA_OPTS="-DDCSTOOL_HOME=$DCSTOOL_HOME -DDCSTOOL_USERDIR=$DCSTOOL_USERDIR"
        export JAVA_ARGS="-p 8081 -c odcsapi -cors /home/testuser/OPENDCS/opendcs_web_cors.cfg -s"
        ./start.sh
        ```
- The java args help configure the server
     - -cors
          -	A cors file that helps configure the cors settings of the server.  Below is an example of text in a cors file.
          ```
          Access-Control-Allow-Origin:*
          Access-Control-Allow-Headers:X-Requested-With,Content-Type,Accept,Origin,authorization
          Access-Control-Allow-Methods:GET,POST,HEAD,OPTIONS,DELETE
          ```
     - -c
       - The context (relative url path) of the API.
  - -p
      - HTTP port number
  - -sp
    - HTTPS port number.  Requires a key and a key password to work.
  - -key
    - Path to a key that can be accessed by the webserver.
  - -kp
    - Key Password (the password that was used to generate the key).
  - -P
    - Decodes Properties file path (by default it’s at $DCSTOOL_HOME/decodes.properties.
  - -s
    - Secure mode.  The authentication is done via the header, rather than as parameters passed through parameters.

Additionally, the build can run from a developer environment using the gradle task `./gradlew :opendcs-rest-api-jetty:startJetty`.
Properties read from gradle.properties will be used to configure the server.

##	OPENDCS Web Client Jetty Standalone Installer
The gradle task `./gradlew :opendcs-web-client-jetty:bundle` will create a tar ball in the `build/distributions` directory.  
This tar ball can be extracted and run with the `start.sh` script.  
The `start.sh` script can be configured with the following arguments:

Additionally, the build can run from a developer environment using the gradle task `./gradlew :opendcs-web-client-jetty:startJetty`.
Properties read from gradle.properties will be used to configure the server.

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
Quality Gates and new issues will automatically be added as notes to Pull Requests targeting the main branch.
The workflow [default.yml](./.github/workflows/default.yml) includes the analysis step for GitHub Actions.

## OWASP Zap API Scan
The workflow [owasp_zap.yml](./.github/workflows/owasp_zap.yml) runs the OWASP Zap API scan to generate an HTML report
detailing vulnerabilities in the OpenDCS REST API based on the OpenAPI documentation. OWASP Zap API scan 
[documentation](https://www.zaproxy.org/docs/docker/api-scan/) details the various options and rules for the scan.
The OWASP Zap HTML report is attached to the GitHub Workflow run along with a shortened summary rendered in Markdown.
At this time, the default configuration is used and the results are not used to gatekeep Pull Requests.

## Java Version Compatibility
OpenDCS REST API targets JDK 8 compatibility for distribution. 
In order to ensure compatibility with future versions of Java, the GitHub workflow [java_compatibility.yml](./.github/workflows/java_compatibility.yml)
will run a matrix build against the project to ensure compatibility with other JDK versions.

# Releases
Artifact releases from this repository are found in the GitHub Releases page for the repository. 

They can be created by using the GitHub interface. 
The GitHub workflow [publish.yml](./.github/workflows/publish.yml) will attach the WAR files and TAR files once the release is published.
