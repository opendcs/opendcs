# OpenDCS 8.0

NOTE: This is currently a staging branch for the 8.0 release. Do not use this in production.

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
project each for tracking bugs and features; Wiki (https://github.com/opendcs/opendcs) pages prefixed with "Project -" will have more
information about some projects.
If you want to contribute and aren't sure where to start that should be a good place to see our plans and priorities; please do not hesitate to
ask questions on the mailing list, discussions, or within an issue itself.

We are currently in the process of some major overhauls to various subsystems and while that
can be scary we would like to encourage you to join us anyways. 

# Compiling

- OpenDCS 8.0 targets Java 11 during develop,ent. A JDK 11 is recommended for build though up to 17 will work, except for the generating the installer - which we are going to remove in favor of a simple zip distribution.
- Our runtime target is Java 11, however we will support 11 or higher.

To build the project and and verify things are working:

`gradlew test`

To get a simple baseline environment going:

`gradle run`

This will start the "launcher_start" application and do an initial setup of an XML database suitable for DECODES operations
going that you can use for manual and exploratory testing.

# General Development

To verify everything can work on your system run the following:

```
# General tests
gradlew test
# NOTE: this will flash a few interfaces onto your display, let the task finish or the tests get stuck. 
# However, you can just run through the GUIs to finish the tests. Though be aware if you don't follow the 
# programmed script the task may return failure.

# Tests of a "live" system.
gradlew :testing:integration -Popendcs.test.engine=OpenDCS-XML
# and if you have docker
gradlew :testing:integration -Popendcs.test.engine=OpenDCS-Postgres

#To test the LRGS (NOTE: not available yet.)
gradlew :testing:lrgs
```


See https://opendcs-env.readthedocs.io/en/8.0/dev-docs.html for guidance on some of the newer components.
