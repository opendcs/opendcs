
# Developer Notes

This Gradle subproject is responsible for the web client interface for interacting with the OpenDCS system. It provides the frontend that communicates directly with the `opendcs-rest-api` via JavaScript.

## Building the Project

The project can be built using Gradle, and it produces a WAR distribution suitable for deployment on web containers like Tomcat or Jetty.

Run the following Gradle task to build the project:

```bash
./gradlew build
```

## Web Content

The web client uses static resources and dynamic templates to serve web pages:

- **WebContent**: This directory contains static assets (e.g., HTML, CSS, JavaScript) for the web client interface.

You can run the web client locally by deploying the WAR file in a Tomcat or Jetty instance.

## Security and Authentication

The web client supports two types of authentication mechanisms: **Basic Authentication** and **Single Sign-On (SSO)**. Each method uses a different way to manage sessions and user authentication, allowing for flexible integration with various environments.

### 1. Basic Authentication

In Basic Authentication, the user logs in with a **username** and **password**. Once authenticated, the server generates a session using the `JSESSIONID`, which is stored in the user's browser as a cookie. This session cookie allows the user to remain authenticated across subsequent requests without needing to re-enter credentials.

#### How Basic Authentication Works:

1. **Login Form**: The login page (`login.jsp`) presents a form that prompts the user for their **username** and **password**.
2. **Session Creation**: After the credentials are validated, the server generates a `JSESSIONID` cookie. This cookie is stored on the client-side and sent with every subsequent request to maintain the user's session.
3. **Session Management**: The server uses the `JSESSIONID` cookie to identify the user's session and ensure that the user remains authenticated. If the session expires (based on the server's timeout configuration), the user will need to log in again.

#### Configuration:
In the `web.xml` file, configure Basic Authentication as follows:

```xml
<context-param>
    <param-name>authentication_type</param-name>
    <param-value>basic</param-value>
</context-param>
```

This ensures that the login page uses the standard username and password fields and relies on the `JSESSIONID` cookie for session management.

#### Session Expiration:
You can configure the session timeout in the `web.xml` file to control how long the `JSESSIONID` remains valid:

```xml
<session-config>
    <session-timeout>30</session-timeout> <!-- Session expires after 30 minutes of inactivity -->
</session-config>
```

### 2. Single Sign-On (SSO)

Single Sign-On allows users to authenticate once and gain access to multiple applications without re-entering credentials. The web client supports SSO by redirecting the user to a trusted identity provider. Once authenticated, the user is redirected back to the web client.

#### How SSO Works:

1. **SSO Login Redirect**: When SSO is configured, the login page shows an "SSO Login" button instead of the username/password form. Clicking this button redirects the user to the SSO provider's login page.
2. **Return from SSO**: After successful authentication, the user is redirected back to the web client along with a session identifier.
3. **Session Handling**: The session is managed by the identity provider, and the web client uses the provided session cookie to authorize API requests.

#### Configuration:
To configure SSO, update the `web.xml` file with the following parameters:

```xml
<context-param>
    <param-name>authentication_type</param-name>
    <param-value>sso</param-value>
</context-param>

<context-param>
    <param-name>authentication_base_path</param-name>
    <param-value>/sso-auth</param-value>
</context-param>
```

- `authentication_type`: Set to `"sso"` to enable Single Sign-On.
- `authentication_base_path`: Specifies the path to the SSO provider's authentication endpoint.

The `login.jsp` file dynamically adapts based on the `authentication_type` configuration, showing either the SSO button or the username/password form.

### Session Management and Expiration

- **Basic Authentication**: Sessions are managed via the `JSESSIONID` cookie. The session timeout can be configured in the `web.xml` file, and the server will invalidate the session when it expires.
- **SSO**: Session management is handled by the SSO provider, and the user’s session is maintained as long as the SSO provider’s token is valid.