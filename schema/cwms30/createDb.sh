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

echo -n "Running createDb.sh at " >$LOG
date >> $LOG
echo >> $LOG

echo "======================" >>$LOG
echo "Creating roles & users" >>$LOG
echo "Creating roles & users"
rm -f roles.log
echo sqlplus $DBSUPER/$DBSUPER_PASSWD@//$DBHOST:$DBPORT/$DB_TNSNAME as sysdba @roles.sql >>$LOG
sqlplus $DBSUPER/$DBSUPER_PASSWD@//$DBHOST:$DBPORT/$DB_TNSNAME as sysdba @roles.sql
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
echo "Creating combined schema file ..." >>$LOG
echo "Creating combined schema file ..."
cp combined_hdr.sql combined.sql
cat opendcs.sql >> combined.sql
cat sequences.sql >>combined.sql
echo >> combined.sql
echo "-- Set Version Numbers" >> combined.sql
now=`date`
echo 'delete from DecodesDatabaseVersion; ' >> combined.sql
echo "insert into DecodesDatabaseVersion values(13, 'Installed $now');" >> combined.sql
echo 'delete from tsdb_database_version; ' >> combined.sql
echo "insert into tsdb_database_version values(13, 'Installed $now');" >> combined.sql
echo "spool off" >>combined.sql
echo "exit;" >> combined.sql

echo "======================" >>$LOG
echo "creating tables, indexes, and sequences" >>$LOG
echo "creating tables, indexes, and sequences"
rm -f combined.log
echo sqlplus $CCP_SCHEMA/$CCP_PASSWD@//$DBHOST:$DBPORT/$DB_TNSNAME @combined.sql >>$LOG
sqlplus $CCP_SCHEMA/$CCP_PASSWD@//$DBHOST:$DBPORT/$DB_TNSNAME @combined.sql
cat combined.log >>$LOG
rm combined.log
echo >>$LOG

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

echo "Left for you to do ..."
echo "  For each office that will use CCP on this database ..."
echo "     Create a CCP user with manager privilege in that office."
echo "     In the toolkit Setup menu, set URL, DB User and Password for that user."
echo "     In the Setup menu, set DbOfficeId appropriately."
echo "     CD to the directory containing this script and ..."
echo "     If you have a snapshot of that office's db, run dbimport -r filename"
echo "     Otherwise run the importDecodesTemplate.sh script in this directory."

rm defines.sql
