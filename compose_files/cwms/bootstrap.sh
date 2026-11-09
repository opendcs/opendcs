#!/bin/bash
set -euo pipefail

sqlplus -s / as sysdba <<'SQL'
whenever sqlerror exit failure rollback
whenever oserror exit failure rollback
alter session set container=FREEPDB1;
@/admin_user.sql
/
create tablespace CCP_DATA datafile 'ccp_data.dbf' size 100M autoextend on next 1M maxsize 2000M;
exit success
SQL
touch /tmp/opendcs-cwms-ready
