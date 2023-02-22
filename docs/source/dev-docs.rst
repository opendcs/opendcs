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