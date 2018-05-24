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

# Schema Owner Account and password
export TSDB_ADM_SCHEMA=tsdb_adm
export TSDB_ADM_PASSWD=xxxxxxxx

# SID (a.k.a. TNS Name)
export DB_TNSNAME=opendcs_tsdb

# Oracle tablespace name and temporary tablespace name
export TBL_SPACE_DIR=/home/oracle/app/oradata/$DB_TNSNAME
export TBL_SPACE_DATA=opentsdb_data
export TBL_SPACE_TEMP=opentsdb_temp

# Number of numeric and string storage tables to create
export NUM_TABLES=50
export STRING_TABLES=20

