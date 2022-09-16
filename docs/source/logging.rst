###########
Logging
###########

Default mechanism
=================

By default most applications write to a log in the current directory with the name `<application>.log`

Otherwise the log file will be "util.log"

All applications take a `-l` parameter by default. This will cause the applications
to write to a specific file.


Additional configuration
========================

There are multiple loggers witin OpenDCS however they are used in specific context.
However any of them can be forced to be the logger with some extra environment varialbes.

For example, if you are running in docker and want to see everything on stdout.

.. code-block:: bash

    $ export DECJ_JAVA_OPTS="-Dopendcs.logger=ilex.util.StdOutLogger"
    $ compproc

Now all the log entries will print in the terminal.
