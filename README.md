# rest_api
OpenDCS Rest API is web application that provides access to the OpenDCS database using JSON (Java Script Object Notation).
OpenDCS Rest API is intended to run as a stand-alone Java program. It uses embedded JETTY to implement the web services.

# Structure
./rest_api - contains source files for the OpenDCS REST API
./web_client - contains source files for the OpenDCS Web Application Client

# Installation and Configuration
There are two types of installations/configurations.  One is Jetty and the other is a WAR file.

The Root URL for the service is specified using the command line arguments when the JETTY server is started. If the defaults are used, you can access the service from the local machine with:
http://localhost:8080/odcsapi/function-call

For example, to get a list of platform references:
http://localhost:8080/odcsapi/platformrefs

##	Jetty
-	Change directory to the base directory of the project.
-	Run the following command: `mvn install`
-	In the ‘target’ directory, there will be a .tar.gz file.  This is the newly created jetty tar ball.
-	Move the tar ball to the desired location and extract it.
-	Then change directory to the ‘bin’ directory.  There will be a ‘start.sh’ file.
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

## WAR file
Once mvn install has been run to create a jetty instance, a war file can then be created.
- Run the following command after the jetty embedded server tar ball is created
- ant -f build-war.xml war


