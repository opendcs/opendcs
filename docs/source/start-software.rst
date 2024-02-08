################################
Software Set-Up
################################

OpenDCS Overview
================

OpenDCS is a software built for the following purposes:

* Retrieving data from NOAA GOES Satellite system
* Processing data from NOAA GOES Satellite system to a more useful 
* Retrieving data from arbitrary systems
* Processing of near real-time time series data

OpenDCS is currently used by:

* U.S. Army Corps of Engineers
* U.S. Bureau of Reclamation
* U.S. National Oceanic and Atmospheric Administration
* And Others

OpenDCS current and past contributors include:

* U.S. Army Corps of Engineers
* U.S. Bureau of Reclamation
* U.S. Geological Survey
* ILEX Engineering Incorporated
* Sutron Corporation
* Cove Software, LLC
* Precision Water Resources Engineering
* Xcellious Consulting
* Resource Management Associates


What is OpenDCS?
----------------

OpenDCS is a software for retrieving and processing data from NOAA 
GOES Satellite system.  OpenDCS is also used for retrieving data
from arbitrary systems (ie ), and processing of time series data.
The software is built to be compatible with the U.S. Army Corps 
of Engineers Corps Water Management System (CWMS) and with the U.S.
Bureau of Reclamation Hydrologic Database (HDB).

OpenDCS includes a subset of tools for users, including the
following applications:

* DECODES
* LRGS
* Computation Processor
* Screening

Once set-up, users can experience automated data and retrieval and
processing from LRGS satellite data.  Automatically triggered 
calculations can be set-up if data is being stored in CWMS or HDB 
systems.  


Where can I find more information?
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Information about OpenDCS can be found on the github repository.

Github: https://github.com/opendcs/opendcs 

Where can I find this information in PDF format?
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Previous versions of OpenDCS (before 7.0.0), included a doc folder
in the install directory (ie OPENDCS/doc), with PDFs, which were
previously developer focused.  Versions 7.0.0 and after include 
html code in the OPENDCS/docs folder.  The content of the older
PDFs is now contained in the html files.

How do I install OpenDCS?
=========================

Installing OpenDCS requires a few steps outlined below.

#. Download the software package (opendcs-installer-#.#.#.jar).
#. Unzip the contents.
#. Configure OpenDCS.

What do I need installed before getting started with OpenDCS?
-------------------------------------------------------------

Prior to installing OpenDCS, java 1.8 or a later version must be
installed.  

For windows users, check if java is installed and check if java is 
in the user environment path (or system environment path).

Run the following commands in a command prompt.

.. code-block:: batch

   > where java
   C:\Program Files (x86)\Common Files\Oracle\Java\javapath\java.exe
   
In the code block above java is installed in the path returned.  The 
location may be different on your PC - this is OK.  If nothing is 
returned (or a message like ""INFO: Could not find files for the given
pattern(s).), check if java is installed somewhere on your PC. If it
is installed, and if it is at least version 1.8, then add the location
to the system or user environment path.

If a path is returned, run the following command to see what version
is installed.
   
.. code-block:: batch

   > java -version
   java version "1.8.0_391"

If a java version (at least 1.8.##) is returned, then java is installed.
Proceed with installing OpenDCS.
 
If nothing is returned or the version is older then 1.8, then install
the latest version from https://adoptium.net/temurin/releases/ . 

Where can I find releases of OpenDCS
------------------------------------

The latest version releases of OpenDCS can be found online: https://github.com/opendcs/opendcs .  

More content coming soon ...

To find the latest releases can be found on the top of the page.  

More content coming soon ...

How do I launch the software?
-----------------------------

More content coming soon ...

OpenDCS Main Menu Components - Overview
=======================================

More content coming soon ...

DECODES Components
-----------------------------------

More content coming soon ...

LRGS Status
~~~~~~~~~~~

DCP Message Browser
~~~~~~~~~~~~~~~~~~~

DECODES Database Editor
~~~~~~~~~~~~~~~~~~~~~~~

Platform Monitor
~~~~~~~~~~~~~~~~

Routing Monitor
~~~~~~~~~~~~~~~

Setup
~~~~~

Time Series Database Components
-------------------------------

More content coming soon ...

Time Series
~~~~~~~~~~~

Time Series Groups
~~~~~~~~~~~~~~~~~~

Computations
~~~~~~~~~~~~

Test Computations
~~~~~~~~~~~~~~~~~

Processes
~~~~~~~~~

Algorithms
~~~~~~~~~~