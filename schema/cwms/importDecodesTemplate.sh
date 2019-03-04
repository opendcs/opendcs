#!/bin/sh

#############################################################################
# This software was written by Cove Software, LLC ("COVE") under contract 
# to the United States Government. 
# No warranty is provided or implied other than specific contractual terms
# between COVE and the U.S. Government
# 
# Copyright 2014 U.S. Army Corps of Engineers, Hydrologic Engineering Center.
# All rights reserved.
#############################################################################

#
# This script assumes you have already installed OPENDCS
# and have setup the DECODES DB connection configurations.
# The script will import the look-up table data onto the CCP DB
# that user specifies.

if [ -z "$DCSTOOL_HOME" ]
then
	echo "Your DCSTOOL_HOME environment variable is empty."
	echo "Please make sure the toolkit is installed and "
	echo "set your DCSTOOL_HOME environment variable in "
	echo "your .bash_profile, and then rerun this script."
	exit 1
fi

cwd=`pwd`

cd $DCSTOOL_HOME
LOG=$cwd/util.log
rm $LOG

echo "Importing Enumerations from edit-db ..."
bin/dbimport -r -l $LOG -d3 $DCSTOOL_HOME/edit-db/enum/*.xml

echo "Importing Standard Engineering Units and Conversions from edit-db ..."
bin/dbimport -r -l $LOG -d3 $DCSTOOL_HOME/edit-db/eu/EngineeringUnitList.xml

echo "Importing Standard Data Types from edit-db ..."
bin/dbimport -r -l $LOG -d3 $DCSTOOL_HOME/edit-db/datatype/DataTypeEquivalenceList.xml

echo "Importing SHEF English and CWMS Presentation Groups ..."
bin/dbimport -r -l $LOG -d3 $DCSTOOL_HOME/edit-db/presentation/*.xml

echo "Importing CWMS-Specific Computations..."
bin/dbimport -r -l $LOG -d3 $cwd/cwms-import.xml

echo "Importing CWMS information..."
bin/compimport -l $LOG -d3 $cwd/cwms-comps.xml

echo "Review the file $cwd/util.log' to check for errors."

