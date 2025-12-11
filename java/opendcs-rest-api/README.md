# Developer Notes

This gradle subproject produces both a jar and a war distribution. 
The jar is used for the embedded Jetty distribution, while the war is 
for container deployments (Tomcat, Jetty, etc).

For ease of use, a tomcat gradle plugin was added in order to test the
container deployment configuration with default [context.xml](src/test/resources/context.xml)
available for manual configuration of the database. Run the task `./gradlew tomcatRun` followed by `./gradlew tomcatStop`.
Note this task can be run through the debugger (at least in IntelliJ) to debug
source code in this subproject.

## Authorization
PlantUML documentation for the following is available [./doc-source/authorization/authorization_sequence.png](./doc-source/authorization/authorization_sequence.png)

The `org.opendcs.odcsapi.sec.SecurityFilter` class implements `javax.ws.rs.container.ContainerRequestFilter` and intercepts all
endpoint requests to this REST API. The filter performs a check against the endpoint to determine if the `@javax.annotation.security.RolesAllowed`
annotation on the endpoint method has guest access or if the endpoint requires an authorized user. If an authorized user is required
checks will be performed against implementations of `org.opendcs.odcsapi.sec.AuthorizationCheck` configured through the `web.xml` init properties.
The user's roles will be added to the user Principal and the appropriate `javax.ws.rs.core.SecurityContext` will be set for the request.
Then the security context will be interrogated to ensure that the client is authorized against the roles specified in 
the `@javax.annotation.security.RolesAllowed` annotation on the endpoint. Authorization will expire
after a default of 15 minutes. Subsequent checks will ensure the user still has the appropriate roles granted.

The interface `org.opendcs.odcsapi.sec.AuthorizationCheck` provides capabilities for separate implementations to authorize
clients for appropriate endpoints. The current implementations are as follows:

### Basic Authorization - OpenTSDB Only
Once the user has been authentication via the `/credentials` endpoint, the server side session is populated
with the user Principal. That user principal is then used for authorization, using the Principal's name
to interrogate the database for appropriate roles.

Note - basic authentication will be removed in a future update.

### Container Single Sign-On - OpenTSDB and CWMS
If the user is authenticated via Single Sign-On for the container, the JSESSIONIDSSO cookie is populated
and the user principal is set on the security context (external from the REST API web app). 
When the client makes a request against an endpoint requiring authorization, this implementation 
checks the user principal for appropriate roles in the database.

### OpenID Connect - OpenTSDB and CWMS
The user must authenticate against a trusted service providing JWT authorization tokens. 
When the client makes a request against an endpoint requiring authorization, the token will be validated
against the JWK Set URL and expiration date. 

CWMS - if the JWT is valid, the EDIPI will be extracted from the token and will be used
to obtain roles from the database.

OpenTSDB - if the JWT is valid, the subject will be extracted from the token and will be used
to obtain roles from the database.

### HDB Implementation
Future work for HDB can implement both Container Single Sign-On and OpenID Connect by implementing the 
`org.opendcs.odcsapi.dao.ApiAuthorizationDAI` interface similar to the OpenTSDB and CWMS implementations.
