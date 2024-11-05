#!/bin/sh

# Set the database name.
DBNAME=lrgs
DBUSER=lrgs_adm

# TEST ONLY: drop the database
dropdb -U lrgs_adm lrgs

echo "Creating database ..."
createdb -U $DBUSER $DBNAME
echo "Defining tables ..."
psql -U $DBUSER $DBNAME -c '\i createLrgsDb.sql'
echo "Defining sequences..."
psql -U $DBUSER $DBNAME -c '\i createLrgsSequences.sql'
echo "Done. SQL database is now ready for additional imports or editing."
