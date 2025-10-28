#######
LOGGING
#######

.. contents. Table of Contents
   :depth: 3


Overview
========

SLF4J (https://www.slf4j.org) for all of our logging statements. This allows for the use of different loggers
depending on the exact usage of OpenDCS.

The packaged applications (released zip and docker containers) will use logback (https://logback.qos.ch/).

Configuration
=============

A default logback.xml configuration is provided in `$DCSTOOL_HOME/logback.xml`. If `$DCSTOOL_USERDIR/logback.xml`
is present then it is used as-is.

The default logback configuration files outputs OpenDCS logging messages at the set log level to a file and
 standard error. Logging statements from dependencies will be at info. Additionally logs will be rotated
 daily and kept for 7 days, with a maximum total size of 3 GB allowed.


The command parameters of -l and -d will continue to work, at least on unix style systems, as before and will be
translated to the forms that logback will pick up correctly. If may be necessary or desired to manually set
the required parameters. A table follows of java (-D) properties that can be set to determine behavior.

+----------------------+---------------------------------+---------------------------------------------+
| Name                 | Default                         | Description                                 |
+======================+=================================+=============================================+
| logback.configura\   | `$DCSTOOL_HOME/logback.xml`     | Total control of the log back configuration |
| tionFile             | `$DCSTOOL_USERDIR/logback.xml`  | The values below assume the processing      |
|                      | if present                      | present in the default config and may not   |
|                      |                                 | have any affect if using your own           |
+----------------------+---------------------------------+---------------------------------------------+
| LOG_DIR              | `$DCSTOOL_USERDIR/logs`         | Directory to which logs are stored if -l or |
|                      |                                 | `$LOG_FILE` are not used                    |
+----------------------+---------------------------------+---------------------------------------------+
| APP_NAME             | java class or value of -a       | Base name of the log file and value for     |
|                      | command line option             | logback's `ContextName`                     |
+----------------------+---------------------------------+---------------------------------------------+
| LOG_FILE             | `${LOG_DIR/${APP_NAME}.log`     | The file name of the log. Will be used      |
|                      |                                 | as-is if set.                               |
+----------------------+---------------------------------+---------------------------------------------+
| LOG_LEVEL            | INFO                            | Logging level to use. One of:               |
|                      |                                 | (TRACE, DEBUG, INFO, WARN, ERROR, OFF)      |
+----------------------+---------------------------------+---------------------------------------------+

.. WARNING::

    OpenDCS previously had 3 levels of debug. With the migration to slf4j there are only 2 levels, debug and trace.
    When using the numeric -dN command line option 2 and 3 are mapped to trace.


decj
====

The script decj (and decj.bat) are used to setup the environment and paramters before starting java.
If `LOGBACK_OVERRIDE` is set any calculated values are set aside and *only* the parameters provided are used.

NOTE: at the time of writing, decj.bat may not have been updated to process any of the logback options.
In that case manually set the above values to what is required.

Examples
========


.. code-block::bash

    launcher_start -d3
    # Will start the launcher at the TRACE level with the default log file of 
    #  $DCSTOOL_USERDIR/logs/decodes.launcher.LaucherFrame

    launcher_start -d1 -l my-log.log
    # Will start the launcher at the INFO level with a file named my-log.log in the current directory.

    compproc -a 1day-comps -d3
    # Will start a compproc instance at the TRACE level with the default log filename of $DCSTOOL_USERDIR/logs/1day-comps.log

    export LOG_DIR=/tmp/logs-on-fast-storage
    compproc -a 1hour-comps -d3
    # Will start a compproc instance at the TRACE level with a logfile name of /tmp/logs-on-fast-storage/hour-comps.log

    export LOGBACK_OVERRIDE=" -Dlogback.configurationFile=path-to-my-config.xml"
    compproc -a 1hour-comps -d3
    # Will start a compproc instance doing whatever the provided logback xml configuration says.

Customization
=============

If you want the overall behavior of the default logging, but want to change something like the maximum days or size
of logs we recommend copying the logback.xml file from `$DCSTOOL_HOME` to `$DCSTOOL_USERDIR` and making the adjustments.

Docker
======

THe Docker containers use a default logback.xml that outputs JSONL event data.
Control log level and context name with `APP_NAME` and `LOG_LEVEL` directly.

Choosing a different logger
===========================

If logback is not suitable for your usage, there are several  logging providers that work with
slf4j. While actual configuration of those providers is beyond the scope of this projects we are
making use of the MappedDiagnosticContext and will likely make use of Markers. To get the most out of logging,
your chosen logging provider should understand and be able to use these features.