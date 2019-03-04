#!/bin/bash

#
# Modify the definitions below before running the scripts to
# install CCP components in a CWMS database.
#

# Hostname & port where the database is running
export DBHOST=Enter DB Hostname or IP Address Here
export DBPORT=1521

# Logfile for installation scripts.
export LOG=createdb.log

# SID (a.k.a. TNS Name)
export DB_TNSNAME=Enter DB SID Here

# Oracle tablespace name and temporary tablespace name
export TBL_SPACE_DIR=/export/home/oracle/app/oracle/oradata/CDB1/$DB_TNSNAME
export TBL_SPACE_DATA=CCP_DATA
export TBL_SPACE_TEMP=CCP_TEMP

# CCP Schema Owner Account and Password
export CCP_SCHEMA=CCP
export CCP_PASSWD=Enter DB User CCP Password Here

# The following will be used by VPD code.
export DEFAULT_OFFICE_ID=SWT
