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

echo -n "Running update52to61.sh at " >$LOG
date >> $LOG
echo >> $LOG

echo "======================" >>$LOG
echo "Modifying roles & users" >>$LOG
echo "Modifying roles & users"
rm -f roles.log
echo sqlplus $DBSUPER/$DBSUPER_PASSWD@//$DBHOST:$DBPORT/$DB_TNSNAME as sysdba @update52to61grants.sql >>$LOG
sqlplus $DBSUPER/$DBSUPER_PASSWD@//$DBHOST:$DBPORT/$DB_TNSNAME as sysdba @update52to61grants.sql
cat roles.log >>$LOG
rm roles.log


echo "======================" >>$LOG
echo "Running initialization as CWMS_20" >>$LOG
echo "Running initialization as CWMS_20"
rm -f cwmsAdmin.log
echo sqlplus $CWMS_SCHEMA/$CWMS_PASSWD@//$DBHOST:$DBPORT/$DB_TNSNAME @cwmsAdmin.sql >>$LOG
sqlplus $CWMS_SCHEMA/$CWMS_PASSWD@//$DBHOST:$DBPORT/$DB_TNSNAME @cwmsAdmin.sql >>$LOG
cat cwmsAdmin.log >>$LOG
rm cwmsAdmin.log

echo "=================================" >>$LOG
echo "Updating the tables, indexes, and constraints ..." >>$LOG
echo "Updating the tables, indexes, and constraints ..."
rm -f combined.log
echo sqlplus $CCP_SCHEMA/$CCP_PASSWD@//$DBHOST:$DBPORT/$DB_TNSNAME @update52to61.sql >>$LOG
sqlplus $CCP_SCHEMA/$CCP_PASSWD@//$DBHOST:$DBPORT/$DB_TNSNAME @update52to61.sql
cat combined.log >>$LOG
rm combined.log

echo "======================" >>$LOG
echo "Setting permissions" >>$LOG
echo "Setting permissions"
echo sqlplus $CCP_SCHEMA/$CCP_PASSWD@//$DBHOST:$DBPORT/$DB_TNSNAME @setPerms.sql >>$LOG
rm -f setPerms.log
sqlplus $CCP_SCHEMA/$CCP_PASSWD@//$DBHOST:$DBPORT/$DB_TNSNAME @setPerms.sql
cat setPerms.log >>$LOG
rm -f setPerms.log
echo >>$LOG

echo "======================" >>$LOG
echo "Creating packages cwms_ccp and cwms_ccp_vpd" >>$LOG
echo "Creating packages cwms_ccp and cwms_ccp_vpd"
rm -f pkg_cwms_ccp.log
echo sqlplus $CCP_SCHEMA/$CCP_PASSWD@//$DBHOST:$DBPORT/$DB_TNSNAME @pkg_cwms_ccp.sql >>$LOG
sqlplus $CCP_SCHEMA/$CCP_PASSWD@//$DBHOST:$DBPORT/$DB_TNSNAME @pkg_cwms_ccp.sql
cat pkg_cwms_ccp.log >>$LOG
rm -f pkg_cwms_ccp.log
echo >>$LOG

echo "======================" >>$LOG
echo "Creating vpd policy" >>$LOG
echo "Creating vpd policy"
rm -f vpd_policy.log
echo sqlplus $CCP_SCHEMA/$CCP_PASSWD@//$DBHOST:$DBPORT/$DB_TNSNAME @vpd_policy.sql >>$LOG
sqlplus $CCP_SCHEMA/$CCP_PASSWD@//$DBHOST:$DBPORT/$DB_TNSNAME @vpd_policy.sql
cat vpd_policy.log >>$LOG
rm -f vpd_policy.log
echo >>$LOG

rm defines.sql
