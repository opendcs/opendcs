## 7.1 branch

This branch will track the master branch code wise. The purpose of this branch is to improve the 
local build system and automated pipeline for tests. No restructuring of code (except moving existing tests into junit test)
will be done.

# OpenDCS 

OpenDCS is a tool for doing the following:
 - retrieving data from the NOAA GOES Satellite system and processing it to a more usual form
 - retrieving data from arbitrary systems
 - near real-time timeseries data processing.

# Getting started

To better learn what the system does and how it works see the PDFs in the doc directory.

# Contributing

Checkout the CONTRIBUTING.md file. Contribution to this branch are welcome, however new features and general bugfixes
should be submitted to the master branch.

# Compiling

To build the file opendcs.jar run the following

`ant jar`

If you want to build the installer run

`ant opendcs`