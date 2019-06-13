#!/bin/bash

#
# Modify the definitions below before creating the database
#

#
# SYS_SCHEMA is a system administrator account for the oracle server.
# It is used to create users, roles, and tablespaces.
# Set SYS_PASSWD before executing and remove it afterward.
#
export DBSUPER=SYS
export DBSUPER_PASSWD=xxxxxxxx

# Hostname & port where the database is running
export DBHOST=localhost
export DBPORT=1521

# Logfile for installation scripts.
export LOG=createdb.log

# CWMS 3.0 Schema Owner Account and password
export CWMS_SCHEMA=CWMS_20
export CWMS_PASSWD=xxxxxxxx

# SID (a.k.a. TNS Name)
export DB_TNSNAME=cwms30

# Oracle tablespace name and temporary tablespace name
export TBL_SPACE_DIR=/opt/app/oracle/oradata/$DB_TNSNAME
export TBL_SPACE_DATA=CCP_DATA
export TBL_SPACE_TEMP=CCP_TEMP

# CCP Schema Owner Account and Password
export CCP_SCHEMA=CCP
export CCP_PASSWD=CCP

# A user account for testing. By default no test users are created.
export USER_SCHEMA=CCPUSER
export USER_PASSWD=xxxxxxx

export DEFAULT_OFFICE_ID=SPA
