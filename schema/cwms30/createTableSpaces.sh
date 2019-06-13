#!/bin/bash

#############################################################################
# This software was written by Cove Software, LLC ("COVE") under contract 
# to the United States Government. 
# No warranty is provided or implied other than specific contractual terms
# between COVE and the U.S. Government
# 
# Copyright 2014 U.S. Army Corps of Engineers, Hydrologic Engineering Center.
# All rights reserved.
#############################################################################

DH=$DCSTOOL_HOME

if [ -z "$DH" ]
then
    echo "You must defined the environment variable DCSTOOL_HOME before running this script."
    exit 1
fi

if [ ! -f defines.sql ]
then
	echo "defines.sql does not exist in this directory!"
	echo "You must run createDefinesSql.sh before running this script."
	exit 1
fi

#
# Source the defines file
#
if [ ! -f defines.sh ]
then
  echo "There is no 'defines.sh' in the current directory."
  echo "CD to the CCP's cwms30 schema directory before running this script."
  exit
fi
. defines.sh

echo -n "Running createTableSpaces.sh at "
date
echo "===================="
echo "Creating tablespaces"
rm -f tablespace.log
echo sqlplus $DBSUPER/$DBSUPER_PASSWD@//$DBHOST:$DBPORT/$DB_TNSNAME as sysdba @tablespace.sql >>$LOG
sqlplus $DBSUPER/$DBSUPER_PASSWD@//$DBHOST:$DBPORT/$DB_TNSNAME as sysdba @tablespace.sql

echo "Find log output in 'tablespace.log' in the current directory."
