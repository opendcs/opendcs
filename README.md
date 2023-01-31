# OpenDCS 

OpenDCS is a tool for doing the following:
 - retrieving data from the NOAA GOES Satellite system and processing it to a more usual form
 - retrieving data from arbitrary systems
 - near real-time timeseries data processing.

# Getting started

Documentation on how to use OpenDCS is available at https://opendcs-env.readthedocs.io/en/latest/index.html

# Getting help

We have a mailing list at https://www.freelists.org/list/opendcs.

Additionally you can open a discussion at https://github.com/opendcs/opendcs/discussions

# Contributing

Checkout the CONTRIBUTING.md file. Changes should be contributed to the default branch.

# Compiling

Installing Ant and adding to your PATH
- Follow the instructions listed [here](https://ant.apache.org/manual/install.html)

To build the file opendcs.jar run the following

`ant jar`

If you want to build the installer run

`ant opendcs`

# IDE integration

For the purposes of setting up the project VS Code or Eclipse you can run the following task:

`ant eclipse-ide-files`

This will create the appropriate .project and .classpath files for code completion and following.
