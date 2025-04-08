##################
Developer Guidance
##################

This Document is part of the OpenDCS Software Suite for environmental
data acquisition and processing. The project home is:
https://github.com/opendcs/opendcs

See INTENT.md at the project home for information on licensing.

.. contents. Table of Contents
   :depth: 3


Overview
========

The purpose of this document is to describe how different technologies are used for OpenDCS development.
Extra attention is given to testing and using OpenDCS within containers.

The Gradle Build
================

Basics
------

OpenDCS now uses gradle to perform builds, test, and deployment/distribution operations.

The simplest way to verify the build is working is to run the following 'task':

.. code-block:: bash

    ./gradlew test

Except for the integration tests in integrationtesting/opendcs-tests standard output directories are
use for various reports such as Junit or Jacoco. For the integration tests output reports are organized by
implementation.

The integration tests are only run if explicitly called out:

.. code-block:: bash

    ./gradlew :testing:opendcs-test:test --info -Popendcs.test.engine=OpenDCS-XML

.. WARNING::

    DO NOT RUN THESE TESTS AGAINST A PRODUCTION DATABASE.
    The test engine assumes it has complete control of the database it's pointed at and given may
    destroy anything it requires to verify operations.

The option opendcs.test.engine is required and the following values are supported

+------------------+---------------------------------------------------------+
| Value            | Notes                                                   |
+------------------+---------------------------------------------------------+
|OpenDCS-XML       |Test operations of the XML database                      |
+------------------+---------------------------------------------------------+
|OpenDCS-Postgres  |Test operations against the reference Postgres Database. |
|                  |Requires docker installed.                               |
+------------------+---------------------------------------------------------+
|CWMS-Oracle       |Requires an existing CWMS + CCP Schema setup             |
+------------------+---------------------------------------------------------+

Distribution tasks are used to prepare for release

+-------------------------+-------------------------------------------------------------------------+
|Task                     |Purpose                                                                  |
+=========================+=========================================================================+
|:install:distZip         |Creates the application distribution.                                    |
+-------------------------+-------------------------------------------------------------------------+
|:install:distTar         |Same as above but as tar instead of zip.                                 |
+-------------------------+-------------------------------------------------------------------------+
|:install:installDist     |Generates and signs release artifacts for upload                         |
|                         |requires `-Dgpg.key.id` command line option                              |
+-------------------------+-------------------------------------------------------------------------+

Building Documentation
-----------------------

.. code-block:: bash

    ./gradlew buildDocs


Debugging OpenDCS
-----------------

To debug any of the test tasks use the stand gradle option of `--debug-jvm` and attached to port 5005 as appropriate to your environment.

If you have an installation of OpenDCS already it can also be debugged in a similar way. NOTE: this is
known to work on linux/mac.

After you have an installation otherwise working start applications with the following:

.. code-block:: bash

    # For testing dbedit.
    DECJ_MAXHEAP="-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=<port>" dbedit

    # For testing dbedit, but you're trying to figured out an issue during startup.
    DECJ_MAXHEAP="-agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=<port>" dbedit


You can then have your IDE attach to the JVM and it will stop on break points appropriately.

The following workflow can be used:

.. code-block:: bash

    # <see issue and make tweaks to code>
    ./gradlew build
    cp java/opendcs/build/libs/opendcs-<version>.jar <your current $DCSTOOL_HOME>/bin
    DECJ_MAXHEAP="-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=<port>" <app>


And repeat as required. This works for the GUI and non gui applications.

Debugging OpenDCS from the build
--------------------------------

There is a `runApp` task that will allow you to run an OpenDCS application from the build environment.
the "stage" directory is used as DCSTOOL_HOME and DCSTOOL_USERDIR is the same default as an install.

.. WARNING::

    By using the default behavior you *MAY* be connecting to a live system. Consider that while
    manipulating any data. 

    If this is a major concern you should set the DCSTOOL_USERDIR for the session gradle runs in
    to point to a directory that only contains profiles that connect to test systems.

.. code-block:: bash

    # to just run the launcher
    ./gradlew runApp

    # to run a specific app
    ./gradlew runApp -Popendcs.app=compedit

    # to run a specific app with a profile
    ./gradlew runApp -Popendcs.app=dbedit -Popendcs.profile="full path to a profile or .properties file"

    # to run with the java remote debugger enabled
    ./gradlew runApp -Popendcs.debug=5006

    # to run with Java Flight Recorder
    ./gradlew runApp -Popendcs.jfr=true
    # recordings will be in the run directory of the build (default build/run)
    # with the name <opendcs.app>.recording.jfr where opendcs.app is the value of the property provided
    # or the default "launcher_start" app if the property is not set.

All of the options above can be in any combination.

The logs are set to the highest debug level and printed to stdout. You may need to add the gradle option `--info` to see the log information.

.. NOTE::

    On linux, ctrl-c of the run task will terminate the application. This does not appear to work correctly on Windows
    and you will likely need to close the application windows manually.

MBeans
======

We have started implementing JMX MBeans for components within OpenDCS. You can connect to the process
using the jconsole application provided with your JDK to view the information.

CWMS
====

MBeans
------

The cwms connection pool implements the ConnectionPool Mbean. This MBean provides a view into the connections 
outstanding and available. Additional each Connection returned implements a WrappedConnectionMBean that shows
the current lifetime and can show where the connection pool was opened from.

Connection pool
---------------

CwmsDb using a connection pool mechanism. Leaks are a concern, if you working against a CWMS
system you can turn pool tracing on for an application with the following java flags:

.. code-block:: bash

    DECJ_MAXHEAP="-Dopendcs.connection.pool.trace=true" routsched ...

With tracing on the WrappedConnectionMBean will show where a connection was created from. This useful for identifing 
what code to fix for connection pool leaks.

Authentication Sources
----------------------

Implementation
~~~~~~~~~~~~~~

If the simple file based, or environment variable based credential sources are insufficient it is possible to create and 
load a new source without additional configuration.

To do so implement the following interfaces:

   org.opendcs.spi.authentication.AuthSource

   org.opendcs.spi.authentication.AuthSourceProvider

AuthSource handles actually creating the credentials properties. All current implementations provide "username" and "password"
as that is the only need.


AuthSourceProvider gives the source implementation a name and takes the
configuration string from the user.properties or decodes.properties and instantiates the AuthSource instance.

You must also add a file:

    META-INF/services/org.opendcs.spi.authentication.AuthSourceProvider

that contains the fully qualified class name of your new AuthSource.

Usage
~~~~~

To acquire the configured credentials the following can be used:

.. code-block:: java

    ...
    String authFileName = DecodesSettings.instance().DbAuthFile;

    try
    {
        Properties credentials = null;
        credentials = AuthSourceService.getFromString(authFileName)
                                        .getCredentials();
        // ... work using the credentials
    }
    catch(AuthException ex)
    {
        String msg = "Cannot read username and password from '"
            + authFileName + "' (run setDecodesUser first): " + ex;
        System.err.println(msg);
        Logger.instance().log(Logger.E_FATAL, msg);
        throw new DatabaseConnectException(msg);
    }
    ...

PropertyProvider
----------------

The PropertyProvider system as added to support EnvExpander retrieving values from sources other than the java `System.properties`.
The mechanism uses the java ServiceProvider mechanism so downstream users can implement any custom sources they need.

To implement a custom property provider the following class `org.opendcs.spi.properties.PropertyValueProvider`.

.. code-block:: java
    :linenos:

    package org.opendcs.spi.properties;

    import java.io.IOException;
    import java.util.Map;
    import java.util.Properties;

    public interface PropertyValueProvider {
        /**
        * Determine if a given string can be processed by this provider
        * @param value
        * @return
        */
        public boolean canProcess(String value);

        /**
        * Retrieve property from the provided property or environment map.
        *
        * It is permissible for a given implemtation to completely ignore either the properties or
        * environment map. However, it should be made very clear where data is coming from
        *
        * @param value actual value to decipher.
        *
        * @param properties Properties to use for the given request.
        * @param environment Environment map to use for the given request.
        *
        * @return the real value, or null if not found.
        */
        public String processValue(String value, Properties properties, Map<String,String> env) throws IOException;
    }

Here is the `EnvironmentPropertyValueProvider` for an example:

.. code-block:: java
    :linenos:

    package org.opendcs.utils.properties;

    import java.util.Map;
    import java.util.Properties;

    import org.opendcs.spi.properties.PropertyValueProvider;

    /**
    * Get the real value of a property from the environment.
    */
    public class EnvironmentPropertyValueProvider implements PropertyValueProvider
    {
        private static final String prefix = "env.";

        @Override
        public boolean canProcess(String value)
        {
            return value.toLowerCase().startsWith(prefix);
        }

        /**
        * Retrieve property from the provided envrionment map
        * @param value actual value to decipher.
        *
        * @param properties ignored in this implementation.
        * @param environment Environment to use for the given request.
        *
        * @return the real value, or null if not found.
        */
        @Override
        public String processValue(String value, Properties props, Map<String,String> environment)
        {
            String envVar = value.substring(prefix.length());
            return environment.get(envVar);
        }

    }


The following prefixes are reserved:

+----------+--------------------------------------+
|<nothing> |no prefix is used for default behavoir|
+----------+--------------------------------------+
|env       |Values from `System.getenv`           |
+----------+--------------------------------------+
|java      |Values from `System.getProperty`      |
+----------+--------------------------------------+
|file      |Values from files on the file system. |
+----------+--------------------------------------+

Custom Decodes Functions
========================

To create a custom function, implement the following interface `org.opendcs.spi.decodes.DecodesFunctionProvider`, and derive
your actual function from `decodes.decoder.DecodesFunction`.

Additionally make sure your full class name is in the appropriate
`META-INF/services/org.opendcs.spi.decodes.DecodesFunctionProvider` file.

.. code-block:: java
    :linenos:

    package org.opendcs.spi.decodes;

    import decodes.decoder.DecodesFunction;

    public interface DecodesFunctionProvider
    {
        /**
        * Name of the decodes function that will be used in a DecodesScript.
        * The name is case sensitive. If you function is provided outside of the
        * OpenDCS distribution, please prefix the name with some sort of organizational identifier.
        * @return
        */
        String getName();

        /**
        * Create an actual instance of your custom decodes function.
        * @return Valid and immediately usable instance of a DecodesFunction.
        */
        DecodesFunction createInstance();
    }

Decodes Function Operations
---------------------------

We will expand this section later. For the moment please review the existing DecodesFunction implementations to
determine the most appropriate implementation details for your function.


Additional Logging
==================

Similar to the connection pool tracing above, if you are having difficulty with a provider
you can log missed results with the following feature flag.

.. code-block:: bash

    DECJ_MAXHEAP="-Dopendcs.property.providers.trace=true" routsched ...

This will cause excessive logging and drastically slow execution. We do not recommend
leaving this setting on for any length of time beyond a debugging session.

Code Analysis
-------------

Checkstyle, Spotbugs, and the PMD/CPD tools are available for anaylzing the code.

to run each do the following:

.. code-block:: bash

    # SpotBugs
    ./gradlew spotbugsMain
    # output will be in build/reports/spotbugs/spotbugs.html

    # Checkstyle
    ./gradlew checkstyleMain
    # output will output to the terminal

    # CPD
    ./gradlew cpd
    # output will be in build/reports/cpd.xml

Only CPD is fast. checkstyle and SpotBugs are rather slow.

Additionally SonarCloud will be used as part of the CI/CD pipeline on Github, results will be automatically linked
through a comment in pull requests.

.. _integration_test_infra:

Integration Test infrastructure
===============================

OpenDCS now contains a framework for running integration tests. See the folder `src/test-integration` for the code.
The intent is to be a simple to use "Compatibility Toolkit" where a given implementation is only responsible for identifying
the OpenDCS concepts (DECODES, Timeseries, computations, etc) that it supports and handling instantiation of external resources
and setting up the configuration.

Framework
---------

There is set of code under :code:`org.opendcs.fixtures` that allows configuration and setup to take place and determine if a given 
test should be enabled or not and other per test tasks.

All new integration test classes should derive from :code:`org.opendcs.fixtures.ApptestBase`. This class is marked with the :code:`OpenDCSTestConfigExtension` 
and handles determining which OpenDCS implementation to run, and performing any required "installation and setup steps" needed.

Implementations should derive from :code:`org.opendcs.fixtures.spi.configuration.Configuration` and :code:`org.opendcs.spi.configuration.ConfigurationProvider`
and implement any required setup. All `Configurations` are given a temporary directory to create the `DCSTOOL_USERDIR` contents.
Application logs are all written into this directory.

The Currently Implemented engines are demonstrated below.  OpenDCS-Postgres, CWMS-Oracle, and OpenDCS-Oracle use the (Testcontainers)[https://java.testcontainers.org] library which requires docker. OpenDCS-XML only depends on the file system.

To run use the following commands:

.. code-block:: bash

    ./gradlew :testing:opendcs-test:test -Popendcs.test.engine=OpenDCS-XML
    # or 
    ./gradlew :testing:opendcs-test:test -Popendcs.test.engine=OpenDCS-Postgres
    # or 
    ./gradlew :testing:opendcs-test:test -Popendcs.test.engine=OpenDCS-Oracle
    # or 
    ./gradlew :testing:opendcs-test:test -Popendcs.test.engine=CWMS-Oracle

Algorithm tests
---------------

Algorithm tests are a suite of regression tests designed to ensure that all algorithms are functioning correctly across builds and updates. These tests validate the correctness and stability of algorithmic computations by comparing the actual outputs against expected results.

To run the algorithm tests, execute the following command:

.. code-block:: bash

    ./gradlew :testing:opendcs-tests:test --tests org.opendcs.regression_tests.AlgorithmTestsIT.test_algorithm_operations

This will run the full suite of algorithm tests.

If you want to run a specific test, you can use the following command with the `-P` argument to filter the test by name:

.. code-block:: bash

    ./gradlew :testing:opendcs-tests:test --tests org.opendcs.regression_tests.AlgorithmTestsIT.test_algorithm_operations -P"opendcs.test.algorithm.filter=ResEvapTest1"

Replace `ResEvapTest1` with the name of the specific test you want to run. This allows for targeted testing of individual algorithms, which is useful during development or debugging.

Note: some tests may require -P"opendcs.test.engine=CWMS-Oracle"

Adding tests
------------

New classes, or methods to existing classes, should go under :code:`org.opendcs.regression_tests`

Integration tests inherit from :code:AppTestBase. This simplifies access to resources, environment, properties, and methods as described below.

+--------------------------------------------+--------------------------------+
|Member Variable                             |Description                     |
+============================================+================================+
|@SystemStub\                                |variables from \                |
|protected final EnvironmentVariables \      |System.getenv \                 |
|environment = new EnvironmentVariables();   |that applications will see.     |
+--------------------------------------------+--------------------------------+
|@SystemStub\                                |variables from \                |
|protected final SystemProperties \          |System.getProperty \            |
|properties = new SystemProperties();        |that applications will see.     |
+--------------------------------------------+--------------------------------+
|@SystemStub\                                |Used to trap System.exit        |
|protected final SystemExit \                |calls to allow testing          |
|exit = new SystemExit();                    |without aborting the test run   |
+--------------------------------------------+--------------------------------+
|@ConfiguredField                            |Instance of the                 |
|protected Configuration configuration;      |:code:`Configuration` that was  |
|                                            |create for this run. Contains   |
|                                            |reference to user.properties and|
|                                            |other specific information.     |
|                                            |This is provided by default as  |
|                                            |almost all interactions will    |
|                                            |require access to the           |
|                                            |user.properties file            |
+--------------------------------------------+--------------------------------+
 

At the Class and method level the following annotations are available.

+--------------------------------------------+--------------------------------+
|Annotation                                  |Description                     |
+============================================+================================+
|DecodesConfigurationRequired                |List of database import files   |
|                                            |needed for tests to succeed.    |
|                                            |Can be set at the Class level,  |
|                                            |Method level, or both in which  |
|                                            |case the sets will be merged    |
+--------------------------------------------+--------------------------------+

Adding Subtests
---------------

This module provides functionality for decoding data and implementing algorithms.

Decodes Functions
~~~~~~~~~~~~~~~~~
- **Purpose**: Functions in this section are responsible for decoding input data into a usable format.
- **Usage**: These functions are typically used to parse encoded data or transform it into a structured representation.

Adding Tests for Decodes Functions
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
To add new tests for the Decoding language and functions, developers need to create four files in the `./opendcs/java/opendcs/src/test/resources/decodes/db` directory:

1. **`.assertions` file**:  
    - Purpose: Defines the expected output or results for the test.  
    - Usage: Specify the expected values for the decoded data to validate the function's correctness.  
    - Example:  
      .. code-block:: csv

         #sensor number, time (ISO8601), expected value (double or string), precision, message
         1,2014-03-01T12:00:00Z,23.95,  0.0, Expected value not parsed (sensor 1)
         1,2014-03-01T13:00:00Z,23.96,  0.0, Expected value not parsed (sensor 1)
         1,2014-03-01T14:00:00Z,23.97,  0.0, Expected value not parsed (sensor 1)
         2,2014-03-01T12:00:00Z,17.2,  0.0, Expected value not parsed (sensor 2)
         2,2014-03-01T13:00:00Z,16.9,  0.0, Expected value not parsed (sensor 2)
         2,2014-03-01T14:00:00Z,15.2,  0.0, Expected value not parsed (sensor 2)
         3,2014-03-01T12:00:00Z,98.1,  0.0, Expected value not parsed (sensor 3)
         3,2014-03-01T13:00:00Z,98.1,  0.0, Expected value not parsed (sensor 3)
         3,2014-03-01T14:00:00Z,98.2,  0.0, Expected value not parsed (sensor 3)
         4,2014-03-01T12:00:00Z,8252,  0.0, Expected value not parsed (sensor 4)
         4,2014-03-01T13:00:00Z,8252,  0.0, Expected value not parsed (sensor 4)
         4,2014-03-01T14:00:00Z,8252,  0.0, Expected value not parsed (sensor 4)
         5,2014-03-01T12:00:00Z,0.0,  0.0, Expected value not parsed (sensor 5)
         5,2014-03-01T13:00:00Z,0.0,  0.0, Expected value not parsed (sensor 5)
         5,2014-03-01T14:00:00Z,0.0,  0.0, Expected value not parsed (sensor 5)
         6,2014-03-01T12:00:00Z,0.0,  0.0, Expected value not parsed (sensor 6)
         6,2014-03-01T13:00:00Z,0.0,  0.0, Expected value not parsed (sensor 6)
         6,2014-03-01T14:00:00Z,0.0,  0.0, Expected value not parsed (sensor 6)

2. **`.decodescript` file**:  
    - Purpose: Contains the Decodes script that defines how the input data should be processed.  
    - Usage: Write the script to test the specific functionality of the Decodes Function.  
    - Example:  
      .. code-block:: text

         csv: 3(/, F(D,A,10,4), x, F(T,A,8), csv(1, 2, 4, 5, 6, 3))

3. **`.input` file**:  
    - Purpose: Provides the raw input data to be decoded.  
    - Usage: Include the encoded data that the Decodes Function will process.  
    - Example:  
      .. code-block:: text

         # Ignored header line
         03/01/2014 12:00:00 23.95, 17.2, 8252, 0, 0, 98.1
         03/01/2014 13:00:00 23.96, 16.9, 8252, 0, 0, 98.1
         03/01/2014 14:00:00 23.97, 15.2, 8252, 0, 0, 98.2

4. **`.sensors` file**:  
    - Purpose: Describes the sensors and their configurations used in the decoding process.  
    - Usage: Define the sensor metadata required for the test.  
    - Example:  
      .. code-block:: csv

         #sensor number, sensor name, units, description
         1, Stage, ft, none
         2, Humidity, %, none
         3, Temp, degF, none
         4, Storage, acft, none
         5, Precip, in, none
         6, Zero, raw, none

By adding these files, developers can create comprehensive tests to ensure the correctness and reliability of new or modified Decodes Functions.

Algorithms
~~~~~~~~~~
- **Purpose**: This section contains algorithmic implementations for solving specific problems or performing computations.
- **Usage**: These algorithms are designed to be reusable and efficient for various applications.

Adding Tests for Algorithms
~~~~~~~~~~~~~~~~~~~~~~~~~~~~
To add new tests for algorithms, developers need to create a new directory within `./opendcs/integrationtesting/opendcs-tests/src/test/resources/data/Comps`. This folder must be titled with the name of the algorithm you wish to test.

Within this directory, you can create subdirectories named `Test1`, `Test2`, etc., for each test case. Each test directory can contain the following resources required to run the test:

1. **Rating Tables**:  
    - Location: `rating` folder within the test directory.  
    - Purpose: Contains rating tables required for the computation.  
    - Format: Stored as `.xml` files.
    - Note: You can create the `rating.xml` file using the `exportRating` command.

2. **Time Series Data**:  
    - Location: `timeseries` folder within the test directory.  
    - Structure:  
        - `input` directory: Contains `.tsimport` files defining the input time series for the computation.  
          Example `.tsimport` file:  
          .. code-block:: text

             TSID:TESTSITE1.Speed-Wind.Inst.1Hour.0.Rev-AWC
             SET:TZ=UTC
             SET:UNITS=kph
             2024/10/04-24:00:00,20.5200000000,0
             2024/10/05-01:00:00,20.5200000000,0
             2024/10/05-02:00:00,22.3199999999,0
             2024/10/05-03:00:00,24.1200000001,0
             2024/10/05-04:00:00,25.9200000000,0
             2024/10/05-05:00:00,63.0000000001,0
             2024/10/05-06:00:00,59.4000000000,0
             2024/10/05-07:00:00,50.0400000001,0
             2024/10/05-08:00:00,40.6800000000,0
             2024/10/05-09:00:00,59.4000000000,0
             2024/10/05-10:00:00,38.8800000000,0
             2024/10/05-11:00:00,20.5200000000,0
             2024/10/05-12:00:00,25.9200000000,0
             2024/10/05-13:00:00,18.3600000000,0
             2024/10/05-14:00:00,16.5600000001,0

          You can create this file using the `outputts` command.

        - `output` directory: Contains `.tsimport` files defining the output time series generated by the computation.  
        - `expectedOutputs` directory: Contains `.tsimport` files defining the expected output time series for validation.

3. **Computation Configuration**:  
    - File: `comp.xml`  
    - Purpose: This file defines the setup for the computation and the tests that need to be run. It specifies the algorithm configuration and links the input, output, and expected output time series.  
    - Note: You can create the `comp.xml` file using the `compexport` command.

By organizing the test resources in this structure, developers can ensure that the algorithm tests are comprehensive and easy to manage.


Extension and other Junit information
-------------------------------------

The :code:`OpenDCSTestConfigExtension`, if it knows about a given type, will inject 
an instance of any field annotated with :code:`@ConfiguredField` as seen in the table 
above for the configuration.

The only other injected field is a :code:`TimeSeriesDb` which is Provided by the Configuration
and will already be valid and can be used directly for things like testing DaoObjects or null which 
indicates the implementation under test doesn't use the any of the timeseries database components.
A test may or may not require access to the :code:`TimeSeriesDb` and so is not provided by default.

The sample :code:LoadingAppDaoTestIT uses the :code:@EnableIfSql annotation that extends from Junit's :code:ExecutionCondition
to determine if the test should be run or not.

Additional ExecutionConditions and parameter injection will be added in the future as needed and as
we better identify concepts to map to vs implementation details.


Caveats
-------

OpenDCS supports several implementations, the XML database, the baseline Postgres and Oracle database, two additional Oracle Databases:
USBR's HDB and USACE's CWMS.

Each share the same fundamental concepts. However, portions of the implementation, like Site names
and Data type parameter names (e.g. are we measuring Stage, Elevation, Precipitation, etc) are handle differently.

These tests are intended to be independent of these concerns; however, the current tests getting merged in are for the baseline implementation
which was Derived from CWMS and directly shares naming and data labelling styles. Given a new implementation it is quite 
likely that work will be required to handle this situation. We will address this situation when it happens and you should
not be afraid to reach out in discussions if you are having difficulties.

Containers
==========

Theory of operation
-------------------

Each "application" will have it's own container, derived from a baseline image.
This allows organization while also minimizing downstream disk usage. The base image layer will be shared so each
application will only be a minor additional layer.

Some applications like LRGS, RoutingScheduler, CompProc will have a default CMD and parameters and be suitable for:

.. code-block:: bash

   docker run -d ...

To run as a service.

Other applications, like importts, complocklist, etc, will have an ENTRYPOINT and a user can call it like they normally would except prefixing with:

.. code-block:: bash

   docker run -v `pwd`/decodes.properties:/dcs_user/decodes.properties complocklist

NOTE: this is still a work in progress, we may switch or there will also be support for environment variables. However, the commandline apps will
likely not see common usage in docker directly.

The build
---------

The build is done in multiple stages. 

Stage 1 Build
~~~~~~~~~~~~~

The build uses the openjdk:8-jdk-bullseye image as it was easier to handle some of the basic dependencies. The documentation is not 
generated as it wouldn't be easily accessible anyways.

Stage 2 baseline
~~~~~~~~~~~~~~~~

This setups the basic "OpenDCS" install in /opt/opendcs. We use the openjdk:8-jre-alpine to save space for the final image.
We may experiment in the future with additional image reductions.

The baseline sets up the "DCSTOOL_HOME" directory in /opt/opendcs and alters the bin files with the appropriate full location.

The baseline "env.sh" script, our docker equivalent to opendcs.init, is added here.
The opendcs user, to avoid running as root, and group are added as well as the default entrypoint.

The build/stage directory is copied from the build stage

Stage 3+ lrgs
~~~~~~~~~~~~~

LRGSHOME  and LRGS_ADMIN_PASSWORD ENV variable is registered.
/lrgs_home volume is registered.
The default 16003 port is defined.

The runtime user is set to opendcs:opendcs

CMD is set to lrgs.sh

lrgs.sh handles first time setup, copy default config, initial admin user, and starting LRGS in the foreground.

The lrgs.lock file is currently ignored and docker just kills the process. Currently investigating better ways to 
handle shutdown. Will likely just add a flag to remove the lock file entirely.

The docker environment now uses the special sequence `lrgsStart -F -k -` to run in the foreground (-F) and use the NoOpServerLock file (-k -)  which causes
the applications using that Lock to assume it's always valid for them.

Database Scripts
================

OpenDCS is transitioning to using Flyway to manage database schema installation and upgrades.
See https://flywaydb.org for detail on the specifics. The following assumes you have read 
at least some of the documentation.

The following guidance *MUST* be observed:

- DO NOT ALTER a released versioned migration file. For example `src/main/resource/db/opendcs-pg/schema/V6.8__opendcs.sql` is final
- For each implementation the structure should be as follows:
  
  - `src/main/resource/db/<implementation>/callbacks` for the before/after migration handlers
  - `src/main/resource/db/<implementation>/schema` for the actual versioned migrations
  - `src/main/resource/db/<implementation>/triggers` for any triggers
  - and so on. A given implementation may also provide baseline/bootstrap data
  - Java Migrations, if any, should followed the same structure but within the `src/main/java` folder.
- Each new change should be add to a new migration file that includes the next version number (listed in `rcnum.txt`).
  
  - At the time of writing that would mean V7.0.12, the next would be V7.0.13

- If we end up with a large number of migration and only looking at changes becomes confusing we can create a baseline migration
  that gathers up all previous changes.

While the actual versioned migrations *MUST* stay the same, the other organization is not final; please open a pull-request
if you think you have a superior organization for these data.
