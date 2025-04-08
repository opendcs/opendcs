#  
# decodes.properties for testing with localhost/pd-db (most likely docker)

#
EditDatabaseType=OPENTSDB
EditDatabaseLocation=jdbc\:postgresql\://127.0.0.1/opendcs
# Settings for the dbedit GUI:
EditPresentationGroup=SHEF-English
routingStatusDir=$DCSTOOL_USERDIR/routstat

# Various agency-specific preferences:
SiteNameTypePreference=CWMS
EditTimeZone=UTC
EditOutputFormat=Human-Readable

jdbcDriverClass=org.postgresql.Driver

SqlKeyGenerator=decodes.sql.SequenceKeyGenerator
sqlDateFormat=yyyy-MM-dd HH\:mm\:ss
sqlTimeZone=UTC
dbOfficeId=SPK
CwmsOfficeId=SPK
transportMediumTypePreference=goes

#defaultDataSource=
dataTypeStdPreference=CWMS
#decwizTimeZone=
#decwizOutputFormat=
#decwizDebugLevel=
#decwizDecodedDataDir=
#decwizSummaryLog=

dbAuthFile=env-auth-source:username=DATABASE_USERNAME,password=DATABASE_PASSWORD
#env-auth-source:username=DATABASE_USERNAME,password=DATABASE_PASSWORD
