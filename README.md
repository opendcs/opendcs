# OpenDCS 8.0

NOTE: This is currently a staging branch for the 8.0 release. Do not use this in production.

OpenDCS is a tool for doing the following:
 - retrieving data from the NOAA GOES Satellite system and processing it to a more usual form
 - retrieving data from arbitrary systems
 - near real-time timeseries data processing.

OpenDCS is currently used by:

- U.S. Army Corps of Engineers

If you're agency/company/etc uses OpenDCS and wishes that to be known, please submit either an issue indicating
you would like the name added, or a pull request that updates the list above. Company Logos are welcome in place
of plain text.

# Getting started

Documentation on how to use OpenDCS is available at https://opendcs-env.readthedocs.io/en/8.0/index.html

# Getting help

We have a mailing list at https://www.freelists.org/list/opendcs.

Additionally you can open a discussion at https://github.com/opendcs/opendcs/discussions

# Contributing

First of, if you feel a desire to contribute - which to us includes trying to use the software and reporting issues - thanks!

Checkout the CONTRIBUTING.md file, and don't be too scared by all the legalese; the project started at several government agencies who
have chosen to properly open source the project and we felt a little solid copy-pasting from other government contributing documents was
a good idea.

The short version is, add you're name to the CONTRIBUTORS.md file, and submit changes to the default branch, currently named master.

As for what to contribute: https://github.com/opendcs/opendcs/projects shows our current major projects, as well as a
project each for tracking bugs and features. If you want to contribute and aren't sure where
to start that should be a good place to see our plans and priorities; please do not hesitate to
ask questions on the mailing list, discussions, or within an issue itself.

We are currently in the process of some major overhauls to various subsystems and while that
can be scary we would like to encourage you to join us anyways. 

# Compiling

- Installing Ant and adding to your PATH
  - Follow the instructions listed [here](https://ant.apache.org/manual/install.html)
  - Ant version 1.10.12 or higher is required.
- OpenDCS 7.x targets Java 8. A JDK 8 is recommended for build though 11 and 17 have worked correctly.
- Our runtime target is primarily Java 8, however we expect to support 11 and are interested in issues with 17 and up.


To build the file opendcs.jar run the following

`ant jar`

If you want to build the installer run

`ant opendcs`

# General Development

To verify everything can work on your system run the following:

```
ant test
# NOTE: this will flash a few interfaces onto your display, let the task finish or the tests get stuck. 
# However, you can just run through the GUIs to finish the tests. Though be aware if you don't follow the 
# programmed script the task may return failure.
ant gui-test -Dno.docs=true
ant integration-test -Dno.docs=true -Dopendcs.test.engine=OpenDCS-XML
# and if you have docker
ant integration-test -Dno.docs=true -Dopendcs.test.engine=OpenDCS-Postgres
```

This will run all of the various tests and let you know you have everything setup such that you can start development.

For all test tasks you can add `-DdebugPort=<a port number>` and the JVMs started will wait for a debug connection.
Beware that gui-test and integration-test depend on test running, so you will have to attach the remote debugger twice.
This is a current limitation of the ant build.

See https://opendcs-env.readthedocs.io/en/latest/dev-docs.html for guidance on some of the newer components.

# IDE integration

While the OpenDCS maintainers don't have time to maintain files for other IDEs, we want to make
it as easy as possible for anyone to help and will welcome baseline configuration for others.
If you wish to help in this way please created additional targets and required files to generate them.

## Visual Studio Code or Eclipse

To create the appropriate project files for VS Code or Eclipse you can run the following task:

`ant eclipse-ide-files`

This will create the appropriate .project and .classpath files for intellisense to behave correctly.

## Intellij

See https://github.com/opendcs/opendcs/wiki/Coding-OpenDCS-with-IntelliJ for guidance in setting up intellij.
