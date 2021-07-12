rem 
rem We need to set the INSTALL_PATH when defining tables and defining sequences.
rem 

rem
rem LRGSHOME must be set in the environment
rem
cd %LRGSHOME%

#
# Set the database name.
DBNAME=lrgs

# TEST ONLY: drop the database
# dropdb -U lrgs_adm lrgs

echo "Creating database ..."
createdb -U lrgs_adm $DBNAME
echo "Defining tables ..."
psql -U lrgs_adm $DBNAME -c '\i bin/createLrgsDb.sql'
echo "Defining sequences..."
psql -U lrgs_adm $DBNAME -c '\i bin/createLrgsSequences.sql'
echo "Done. SQL database is now ready for additional imports or editing."
