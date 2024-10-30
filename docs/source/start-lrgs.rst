###########################
LRGS Installation and Setup
###########################

This Document is part of the OpenDCS Software Suite for environmental
data acquisition and processing. The project home is:
https://github.com/opendcs/opendcs

See INTENT.md at the project home for information on licensing.

.. contents. Table of Contents
   :depth: 3



What is the LRGS?
#################

The letters LRGS stand for Local Readout Ground Station. The primary purpose of this component is to get data from
satellite sources, a DRGS (Direct Readout Ground Station) or an HRIT (High Rate Information Transfer).

See The legacy lrgs user guide for additional information <./legacy-lrgs-userguide.rst>

While this is still a reasonable description the LRGS can take data from Satellites (HRIT, DRGS, NOAAport), Irridium,
, HRIT files, or another LRGS (DDS Protocol), and any network device that implements the DAMS-NT protocol as 
built-in sources.

Users can also provide custom sources to the LRGS.

What each LRGS data source has in common is that it acquires a unit of data, for example a GOES transmission, and saves it 
to an archive as a `DcpMsg`. DECODES can then search for and further process these individual messages.

The remainder of this guide will focus on setting up the LRGS component of OpenDCS for Linux.  However,  LRGS also works on a windows computer.


Installation - Linux
====================


This will guide a user through manual setup on a linux system using `systemd` for service control
For a guide on hardware requirements the LRGS is known to work for at least a single HRIT source on
the old Raspberry Pi 3B hardware and was used to create this documentation and verify examples. 

This example will use commands appropriate to Rocky Linux 9 on a Raspberry Pi. It is assumed the reader has
gotten all networking setup as needed. The instructions would be appropriate to Rocky Linux 9, RHEL 8/9, or
any other RHEL based distribution given appropriate changes in usernames.

These instructions will assume a fresh system without any OpenDCS components installed.

Install Java
------------

The minimum Java is 8. However, we recommend a Java 11 Runtime to take advantage of performance
improvements to java.

.. code-block:: bash
    
    sudo dnf install java-11-openjdk-headless

Download and install OpenDCS
----------------------------

.. code-block:: bash

    curl -O -L https://github.com/opendcs/opendcs/releases/download/7.0.12/opendcs-installer-7.0.12.jar
    sudo java -jar opendcs-installer-7.0.12.jar
    # /opt/opendcs/<version> is the recommend installation directory

Example:

.. code-block:: bash

    [rocky@localhost ~]$ sudo java -jar opendcs-installer-7.0.12.jar
    Welcome to the installation of OPENDCS Open Data Collection System 7.0.12!
    - OpenDCS Team <https://github.com/opendcs/opendcs>
    - Cove Software, LLC <info@covesw.com>
    - U.S. Army Corps of Engineers <Webmaster-HEC@usace.army.mil>
    - U.S. Bureau of Reclamation <hdbsupport@precisionwre.com>
    The homepage is at: https://github.com/opendcs/opendcs
    press 1 to continue, 2 to quit, 3 to redisplay
    1
    Select target path [/home/rocky]
    /opt/opendcs/7.0.12
    press 1 to continue, 2 to quit, 3 to redisplay
    1

    Select the packs you want to install:

    [<required>] OpenDCS Base (OPENDCS Java Archive (jar) and scripts necessary for all installations. This will not modify your existing database or configuration files.
    IMPORTANT: For a new installation, you should also select the Template Database.)
    [x] XML Database Template (Initial Empty Database This is required for a new install. This will not overwrite any existing files.)
    input 1 to select, 0 to deselect:

    [x] Docs (PDF and HTML Documentation to go in the 'doc' subdirectory.)
    input 1 to select, 0 to deselect:

    [x] TSDB Computation Components (Time Series and Computation Database Components)
    input 1 to select, 0 to deselect:

    [x] Open Time Series Database Schema and Components (Scripts and DDL for building Open TSDB Database)
    input 1 to select, 0 to deselect:

    [ ] Corps Water Management System (CWMS) Components (Schema, Scripts, and Jars for CWMS)
    input 1 to select, 0 to deselect:

    [ ] Bureau of Reclamation Hydrologic Database (HDB) Components (Schema, Scripts, and Jars for HDB)
    input 1 to select, 0 to deselect:

    [x] LRGS (Open LRGS (Local Readout Ground Station) supplies raw data acquisition functions.)
    input 1 to select, 0 to deselect:


    ...pack selection done.
    press 1 to continue, 2 to quit, 3 to redisplay
    1
    [ Starting to unpack ]
    [ Processing package: OpenDCS Base (1/6) ]
    [ Processing package: XML Database Template (2/6) ]
    [ Processing package: Docs (3/6) ]
    [ Processing package: TSDB Computation Components (4/6) ]
    [ Processing package: Open Time Series Database Schema and Components (5/6) ]
    [ Processing package: LRGS (6/6) ]
    [ Unpacking finished ]
    Install was successful
    application installed on /opt/opendcs/7.0.12
    [ Console installation done ]

Initial Setup
-------------

Newer versions of OpenDCS (7.0.13 or higher) will automatically create a $HOME/.opendcs directory for you the first
time any of applications are started. However, as this guide is for a specific server setup we will manually create the
appropriate directories.

.. code-block:: bash

    cd /home/rocky
    mkdir -p .opendcs/lrgs
    # Now copy the initial configuration
    cd .opendcs/lrgs
    cp /opt/opendcs/7.0.12/lrgs.conf .
    cp /opt/opendcs/7.0.12/ddsrecv.conf .
    cp /opt/opendcs/7.0.12/drgsconf.xml .
    cp -r /opt/opendcs/7.0.12/netlist .
    cp -r /opt/opendcs/7.0.12/users .
    # The Rocky Linux 9 Raspberry Pi image has a firewall on by default.
    # OpenDCS does not recommend turning the firewall off. Allow Port 16003
    # to be used.
    sudo firewall-cmd --zone=public --add-port=16003/tcp --permanent

    
You will need to set your environment. Add the following to .bashrc, if using bash. Otherwise adjust to your choosen shell.

.. code-block:: bash

    export PATH=$PATH:/opt/opendcs/7.0.12/bin
    export DCSTOOL_USERDIR=$HOME/.opendcs
    export LRGSHOME=$DCSTOOL_USERDIR/lrgs

.. code-block:: bash

    # For the current shell. If you add the above to .bashrc the commands
    # will be available by default.
    source ~/.bashrc
    

Now set the LRGS Admin Password::

.. code-block:: bash

    #For random Generation:
    if [ "$LRGS_ADMIN_PASSWORD" == "" ]; then
        LRGS_ADMIN_PASSWORD=`tr -cd '[:alnum:]' < /dev/urandom | fold -w30 | head -n1`
        echo "Admin Password is $LRGS_ADMIN_PASSWORD"
        echo "This will not be printed on subsequent runs"
    fi
    cat `<<EOF | editPasswd
        adduser lrgsadmin
        $LRGS_ADMIN_PASSWORD
        $LRGS_ADMIN_PASSWORD
        addrole lrgsadmin dds
        addrole lrgsadmin admin
        write
        quit
    EOF


.. code-block:: bash
    
    # To set manually
    editPasswd
    adduser lrgsadmin
    # provide desired password
    addrole lrgsadmin dds
    addrole lrgsadmin admin
    write
    quit

Run LRGS
--------

# To run in the background using the normal start process
.. code-block:: bash
    
    startLRGS
    cd ~/.opendcs/lrgs
    # Use
    tail -f lrgslog
    # to see if there are any errors in the initial setup

If you would like to run the LRGS in the foreground use the following:

.. code-block:: bash    

    decj -DLRGSHOME=$LRGSHOME lrgs.lrgsmain.LrgsMain -d3 -l /dev/stdout -F -k -


Run LRGS as a service
---------------------



Installation - docker
#####################

.. code-block:: bash

    docker pull ghcr.io/opendcs/opendcs/lrgs:7.0.13-rc05
    
    docker volume create lrgs_home
    # A default password will be generated and in the logs
    docker run -d --name lrgs -p 16003:16003 -v lrgs_home:/lrgs_home ghcr.io/opendcs/opendcs/lrgs:7.0.13-rc05
    # or if you wish to manually set the password
    docker run -d --name lrgs -p 16003:16003 -v lrgs_home:/lrgs_home -e LRGS_ADMIN_PASSWORD="<password>" ghcr.io/opendcs/opendcs/lrgs:7.0.13-rc05

Connecting
##########

Now that you have an initial LRGS you can use the RtStat program (LRGS Status in the launcher) to connect to your LRGS at the host and port 16003.
