# OpenDCS Integration Test Subproject

# Automated Integration Testing
This will be the future home of automated integration testing 
that is currently only setup in the opendcs-rest-api gradle subproject.

Both Rest Assured and Selinium tests should be configured to run in this gradle subproject.

# Manual Integration Testing
The `./gradlew :opendcs-integration-test:run` task will start up a docker database
based on the `opendcs.test.integration.db` gradle project property. Default is `OpenDCS-Postgres`
and can be changed to `CWMS-Oracle` to test against CWMS/CCP.

The property `opendcs.tomcat.port` can also be configured to define which port to run
the tomcat server on. Default is 7000.

## Configuration
Tomcat config files can be found at [src/test/resources/tomcat/conf](src/test/resources/tomcat/conf) and uses
environment variables for the database connection information and CWMS office id.

REST API config files can be found at [src/test/resources/rest-api/conf](src/test/resources/rest-api/conf)
which includes separate configurations for CWMS and OpenTSDB.

Web Client config files can be found at [src/test/resources/web-client/conf](src/test/resources/web-client/conf)
which includes separate configurations for CWMS and OpenTSDB.
