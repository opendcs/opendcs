#!/bin/bash

#############################################################################
# This software was written by Cove Software, LLC ("COVE") under contract 
# to the United States Government. 
# No warranty is provided or implied other than specific contractual terms
# between COVE and the U.S. Government
# 
# Copyright 2016 U.S. Army Corps of Engineers, Hydrologic Engineering Center.
# All rights reserved.
#############################################################################

DH=$DCSTOOL_HOME
LOG=update.log

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

echo -n "Running update61to62.sh at " >$LOG
date >> $LOG
echo >> $LOG

echo "======================" >>$LOG

echo "Updating the tables, indexes, and constraints ..." >>$LOG
echo "Updating the tables, indexes, and constraints ..."
rm -f combined.log
echo sqlplus $CCP_SCHEMA/$CCP_PASSWD@//$DBHOST:$DBPORT/$DB_TNSNAME @update61to62.sql >>$LOG
sqlplus $CCP_SCHEMA/$CCP_PASSWD@//$DBHOST:$DBPORT/$DB_TNSNAME @update61to62.sql
cat combined.log >>$LOG
rm combined.log

