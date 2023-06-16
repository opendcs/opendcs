##################
Installation Guide
##################

This Document is part of the OpenDCS Software Suite for environmental
data acquisition and processing. The project home is:
https://github.com/opendcs/opendcs

See INTENT.md at the project home for information on licensing.

.. contents. Table of Contents
   :depth: 3


Overview
========

This is the initial start of the documentation and thus it's still quite empty
If you are working on OpenDCS and think of something that should be in this section
help expanding the docs are greatly appreciated.


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

    DECJ_MAXHEAP="-Dcwms.connection.pool.trace=true" routsched ...

With tracing on the WrappedConnectionMBean will show where a connection was created from. This useful for identifing 
what code to fix for connection pool leaks.

Authentication Sources
----------------------

Implementation
++++++++++++++

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
+++++

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


Code Analysis
-------------

Checkstyle, Spotbugs, and the PMD/CPD tools are available for anaylzing the code.

to run each do the following:


.. code-block: bash

    # SpotBugs
    ant spotbugs
    # output will be in build/reports/spotbugs/spotbugs.html

    # Checkstyle
    ant Checkstyle
    # output will output to the terminal

    # CPD
    ant cpd
    # output will be in build/reports/pmd/cpd/cpd.txt

Only CPD is fast. checkstyle and SpotBugs are rather slow.

Containers
==========

Theory of operation
-------------------

Each "application" will have it's own container, derived from a baseline image.
This allows organization while also minimizing downstream disk usage. The base image layer will be shared so each
application will only be a minor additional layer.

Some applications like LRGS, RoutingScheduler, CompProc will have a default CMD and parameters and be suitable for:

   docker run -d ...

To run as a service.

Other applications, like importts, complocklist, etc, will have an ENTRYPOINT and a user can call it like they normally would except prefixing with:
   
   docker run -v `pwd`/decodes.properties:/dcs_user/decodes.properties complocklist

NOTE: this is still a work in progress, we may switch or there will also be support for environment variables. However, the commandline apps will
likely not see common usage in docker directly.

The build
---------

The build is done in multiple stages. 

Stage 1 Build
^^^^^^^^^^^^^

The build uses the openjdk:8-jdk-bullseye image as it was easier to handle some of the basic dependencies. The documentation is not 
generated as it wouldn't be easily accessible anyways.

Stage 2 baseline
^^^^^^^^^^^^^^^^

This setups the basic "OpenDCS" install in /opt/opendcs. We use the openjdk:8-jre-alpine to save space for the final image.
We may experiment in the future with additional image reductions.

The baseline sets up the "DCSTOOL_HOME" directory in /opt/opendcs and alters the bin files with the appropriate full location.

The baseline "env.sh" script, our docker equivalent to opendcs.init, is added here.
The opendcs user, to avoid running as root, and group are added as well as the default entrypoint.

The build/stage directory is copied from the build stage

Stage 3+ lrgs
^^^^^^^^^^^^^

LRGSHOME  and LRGS_ADMIN_PASSWORD ENV variable is registered.
/lrgs_home volume is registered.
The default 16003 port is defined.

The runtime user is set to opendcs:opendcs

CMD is set to lrgs.sh

lrgs.sh handles first time setup, copy default config, initial admin user, and starting LRGS in the foreground.

The lrgs.lock file is currently ignored and docker just kills the process. Currently investigating better ways to 
handle shutdown. Will likely just add a flag to remove the lock file entirely.

Logging
=======

SLF4j is used as the logging interface for all components. No OpenDCS components should make assumptions about the logging
implementation used.

That said, OpenDCS as an application will use java.util.logging for the forseeable future and many components tee off of logged values.
Thus static main functions or classes dedicated solely to log bridging may reference the logging implementation. OpenDCS
components may only reference the java.util.logging implementation. However the project will pull dependencies on loggers we aren't actively using.

Any class or method beyond a static main that references the logging implementation directly will introduce a direct dependency on that particular implementation.
SLF4j is chosen to avoid this type of dependency so opendcs.jar can be used as a library.

Projects wishing to use OpenDCS as a library should review examples of how log teeing is setup if similar behavior is required. However,
the primary use-case is for internal components where that still makes sense or that have not yet been modernized.

Downstream users should consider a more direct separation of concerns.
