#!/bin/bash

DH=%INSTALL_PATH

#
# Source the defines file
#
if [ ! -f defines.sh ]
then
  echo "There is no 'defines.sh' in the current directory."
  echo "CD to the opendcs-oracle schema directory before running this script."
  exit
fi
. defines.sh

echo -n "Running createDb.sh at " >$LOG
date >> $LOG
echo >> $LOG

echo "====================" >>$LOG
# If you need a table space, define it here. then comment out the 
# non tablespace version of createdb below and uncomment the other one.
# TABLESPACE=pg_default

#cd %INSTALL_PATH%

echo -n "Enter db username of Open TSDB Administrator: "
read DBUSER
if [ -z "$DBUSER" ]
then
    echo "You must enter a database name for the TSDB administrator!"
	exit 1
fi
echo -n "Enter password for user $DBUSER:"
read -s PASSWD
echo
echo -n "Re-enter to verify:"
read -s PASSWD2
echo
if [ "$PASSWD" != "$PASSWD2" ]
then
	echo "Passwords do not match. Restart the script and try again."
	exit 1
fi

echo -n "Enter password for database super user $DBSUPER:"
read -s pw
echo
export PGPASSWORD="$pw"

echo -n "Enter number of numeric storage tables (default=10): "
read NUM_TABLES
if [ -z "$NUM_TABLES" ]
then
	NUM_TABLES=10
fi

echo -n "Enter number of string storage tables (default=5): "
read STRING_TABLES
if [ -z "$STRING_TABLES" ]
then
	STRING_TABLES=5
fi

echo "Will create $NUM_TABLES numeric tables and $STRING_TABLES string tables. (Press enter to continue)"
read x

echo "Defining Roles ..."
echo "Defining Roles ..." >>$LOG
psql -q -U $DBSUPER -h $DBHOST -f group_roles.sql >>$LOG

echo "Creating database user $DBUSER ..."
echo "Creating database user $DBUSER ..." >>$LOG
createuser -U $DBSUPER -S -E -d -r -l -i -h $DBHOST $DBUSER >>$LOG 2>&1
psql -q -U $DBSUPER -h $DBHOST -c "ALTER USER $DBUSER WITH PASSWORD '$PASSWD'" >>$LOG 2>&1
psql -q -U $DBSUPER -h $DBHOST -c "GRANT \"OTSDB_ADMIN\" TO $DBUSER" >>$LOG 2>&1

export PGPASSWORD="$PASSWD"
echo "Creating database as user $DBUSER (you will be prompted for password) ..."
# Non-tablespace:
createdb -U $DBUSER -h $DBHOST $DBNAME >>$LOG 2>&1

# For tablespace, uncomment the following line and comment the above one:
# createdb -U $DBUSER -h $DBHOST -D $TABLESPACE $DBNAME

echo "Creating combined schema file ..."
echo '\set VERBOSITY terse' > combined.sql
cat opendcs.sql >> combined.sql
cat dcp_trans_expanded.sql >>combined.sql
cat comp_trigger.sql >>combined.sql
./expandTs.sh $NUM_TABLES $STRING_TABLES
cat ts_tables_expanded.sql >>combined.sql
cat alarm.sql >>combined.sql
./makePerms.sh combined.sql
cat setPerms.sql >>combined.sql
cat sequences.sql >>combined.sql
echo "Setting Version Numbers ..."
echo >> combined.sql
echo "-- Set Version Numbers" >> combined.sql
echo 'delete from DecodesDatabaseVersion; ' >> combined.sql
echo "insert into DecodesDatabaseVersion values(17, '');" >> combined.sql
echo 'delete from tsdb_database_version; ' >> combined.sql
echo "insert into tsdb_database_version values(17, '');" >> combined.sql

for n in `seq 1 $NUM_TABLES`
do
	echo "insert into storage_table_list values($n, 'N', 0, 0);" >> combined.sql
done

for n in `seq 1 $STRING_TABLES`
do
	echo "insert into storage_table_list values($n, 'S', 0, 0);" >> combined.sql
done

echo "Creating schema as user $DBUSER (you will be prompted for password) ..."
psql -U $DBUSER -h $DBHOST -d $DBNAME -f combined.sql >>$LOG 2>&1

EDITDB=$DH/edit-db
if [ ! -d "$EDITDB" ]
then
	EDITDB=$DH/edit-db.init
fi

echo "Importing Enumerations from edit-db ..."
$DH/bin/dbimport -l $LOG -r $EDITDB/enum/*.xml >>$LOG 2>&1
echo "Importing Standard Engineering Units and Conversions from edit-db ..."
$DH/bin/dbimport -l $LOG -r $EDITDB/eu/EngineeringUnitList.xml >>$LOG 2>&1
echo "Importing Standard Data Types from edit-db ..."
$DH/bin/dbimport -l $LOG -r $EDITDB/datatype/DataTypeEquivalenceList.xml >>$LOG 2>&1
echo "Importing Presentation Groups ..."
$DH/bin/dbimport -l $LOG -r $EDITDB/presentation/*.xml >>$LOG 2>&1
echo "Importing standard computation apps and algorithms ..."
$DH/bin/compimport -l $LOG $EDITDB/comp-standard/*.xml >>$LOG 2>&1
echo "Importing DECODES loading apps ..."
$DH/bin/dbimport -l $LOG -r $EDITDB/loading-app/*.xml >>$LOG 2>&1
