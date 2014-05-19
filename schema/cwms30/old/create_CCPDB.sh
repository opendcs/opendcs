#!/bin/sh

echo "This script installs CCP schema into an existing CWMS 3.0 database."
echo -n "Have you installed the CWMS V3.0 schema from HEC (y/n) ?"
read answer
if [ "$answer" != "y" ] && [ "$answer" != "Y" ]
then
  eche "Please install CWMS V3.0 from HEC before running this script."
  exit
fi

echo -n "Have you reviewed all settings in 'defines.sql' (y/n) ?"
read answer
if [ "$answer" != "y" ] && [ "$answer" != "Y" ]
then
  echo "Edit the file 'defines.sql' in this directory. Make correct settings for your environment."
  exit
fi

dba_name="SYS"
dba_pwd="xxxxxx"
db_tnsname="$ORACLE_SID"

echo -n "Enter DBA user name ($dba_name): "
read answer
if [ -n "$answer" ]
then
  dba_name=$answer
fi

echo -n "Enter DBA user password ($dba_pwd): "
read answer
if [ -n "$answer" ]
then
  dba_pwd=$answer
fi

echo -n "Enter Oracle SID name ($db_tnsname): "
read answer
if [ -n "$answer" ]
then
  db_tnsname=$answer
fi

if [ -z "$dba_name" ] || [ -z "$dba_pwd" ] || [ -z "$db_tnsname" ]
then
  echo "Check if DBA user name, DBA user password, or Oracle SID name is empty!"
  exit
fi

echo "Removing all previous *.out files"
rm *.out

# Create CCP tablespace, user, and roles, and grand permissions to CCP user.
# Modify the Create_CCPDB_Prelims.sql script as necessary.
echo "Creating CCP DB schema ..."
sqlplus /nolog @Create_CCPDB_Prelims.sql     $dba_name $dba_pwd $db_tnsname
sqlplus /nolog @Create_CCPDB_Objects.sql     $dba_name $dba_pwd $db_tnsname
sqlplus /nolog @Setup_CCPDB_Privs.sql        $dba_name $dba_pwd $db_tnsname
sqlplus /nolog @Create_CCPDB_Additionals.sql $dba_name $dba_pwd $db_tnsname
sqlplus /nolog @Create_CCPDB_VpdPolicy.sql   $dba_name $dba_pwd $db_tnsname
# sqlplus /nolog @Create_CCPDB_TestUsers.sql   $dba_name $dba_pwd $db_tnsname

echo
echo "The CCP DB schema and its objetcs have been created!"
