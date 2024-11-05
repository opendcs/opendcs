![current build](https://github.com/opendcs/opendcs/actions/workflows/build.yml/badge.svg)

# OpenDCS 

OpenDCS is a tool for doing the following:
 - retrieving data from the NOAA GOES Satellite system and processing it to a more usual form
 - retrieving data from arbitrary systems
 - near real-time timeseries data processing.

OpenDCS is currently used by:

- U.S. Army Corps of Engineers
- U.S. Bureau of Reclamation
- U.S. National Oceanic and Atmospheric Administration

And others.

If you're agency/company/etc uses OpenDCS and wishes that to be known, please submit either an issue indicating
you would like the name added, or a pull request that updates the list above. Company/Agency Logos are welcome in place
of plain text.

# Getting started

Documentation on how to use OpenDCS is available at https://opendcs-env.readthedocs.io/en/latest/index.html

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
project each for tracking bugs and features; Wiki (https://github.com/opendcs/opendcs) pages prefixed with "Project -" will have more
information about some projects.
If you want to contribute and aren't sure where to start that should be a good place to see our plans and priorities; please do not hesitate to
ask questions on the mailing list, discussions, or within an issue itself.

We are currently in the process of some major overhauls to various subsystems and while that
can be scary we would like to encourage you to join us anyways. 

# Compiling

- OpenDCS is compiled using the gradle wrapper 'gradlew'
- OpenDCS 7.x targets Java 8. A JDK 8 is recommended for build though 11 and 17 will work
- Our runtime target is Java 8, however we will support 11 and 17 at runtime

in the following examples replace `./gradlew` with `gradlew` if you are using windows.

To build the file opendcs.jar run the following command:

`./gradlew :opendcs:jar` 

output is here:

java/opendcs/build/libs/opendcs-main.99.main-SNAPSHOT.jar


If you want to build a distribution run:

`./gradlew distZip`

or

`./gradlew distTar`

output is here:

opendcs/install/build/distributions/opendcs-main.99.main-SNAPSHOT.zip

To get a simple baseline environment going:

`./gradlew runApp --info`

This will start the "launcher_start" application and do an initial setup of an XML database suitable for DECODES operations
going that you can use for manual and exploratory testing.

# General Development

To verify everything can work on your system run the following:

```
# General tests
ant test
# NOTE: this will flash a few interfaces onto your display, let the task finish or the tests get stuck. 
# However, you can just run through the GUIs to finish the tests. Though be aware if you don't follow the 
# programmed script the task may return failure.

# Test the GUI (NOTE: leave your hands off the keyboard and mouse or the runner gets confused.)
./gradlew test -Pno.docs=true

# Tests of a "live" system.
./gradlew testing:opendcs-tests:test -Pno.docs=true -Popendcs.test.engine=OpenDCS-XML
# and if you have docker
./gradlew testing:opendcs-tests:test -Pno.docs=true -Popendcs.test.engine=OpenDCS-Postgres

#To test the LRGS
gradlew testing:lrgs:test -Pno.docs=true
```

This will run all of the various tests and let you know you have everything setup such that you can start development.

For all test tasks you can add `-DdebugPort=<a port number>` and the JVMs started will wait for a debug connection.
Beware that gui-test and integration-test depend on test running, so you will have to attach the remote debugger twice.
This is a current limitation of the ant build.

To run a specific test only use:

```
./gradlew <test target> --tests # See [gradle documentation ](https://docs.gradle.org/current/userguide/java_testing.html#simple_name_pattern) for more detail
```

It is possible a file glob will work in the tests parameter above but we have not tested this.

See https://opendcs-env.readthedocs.io/en/latest/dev-docs.html for guidance on some of the newer components.

# IDE integration

While the OpenDCS maintainers don't have time to maintain files for other IDEs, we want to make
it as easy as possible for anyone to help and will welcome baseline configuration for others.
If you wish to help in this way please created additional targets and required files to generate them.

## Visual Studio Code or Eclipse

To create the appropriate project files for VS Code or Eclipse you can run the following task:

`./gradlew opendcs:eclipse`

Or you can use the VS Code gradle integration.

This will create the appropriate .project and .classpath files for intellisense to behave correctly.

## Intellij

Intellij detects the gradle project and has a Gradle tool window.

Some gradle tasks require a python environment.  Here is an example that launches inteliJ from a conda environment to have python enabled.
(base) C:\>conda activate karl
(karl) C:\project\opendcs>C:\Programs\ideaIC-2022.1.win\bin\idea64.exe
