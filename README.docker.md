# OpenDCS Docker files


# LRGS

To setup a permanent instance of an LRGS do the following, replace `--name lrgs` with a name
appropriate to your installation.

```
docker create volume lrgs_volume
docker run -d name lrgs -p 16003:16003 -v lrgs_volume:/lrgs_home -e LRGS_ADMIN_PASSWORD="the password you want" lrgs:latest

```

16003 is the DDS protocol Port that the gui `rtstat` application can use. At this time there is no API and this is required for later configuration.
There are additional input sources, and the ability to add additional custom input sources that may require you to 
expose additional ports.

## Variables

| Variable | Default | Description |
|----------|---------|-------------|
| LRGSHOME | /lrgs_home | location of primary files and output. |
| LRGS_ADMIN_PASSWORD| <not set> | Admin password to use. If not set will be randomly generated and printed to console |

# TsDbApps

The following containers support automated processing. The computation containers, that start with 'comp' require using one of the SQL databases. 

| Container | Purpose |
|-----------|---------|
| compdepends | Determines which data inputs trigger computations. |
| compproc | Handles computation |
| routingscheduler | Can pull data from an LRGS or other data source and push to the database or other locations |

The above containers are the same except that the default `APPLICATION_NAME` and CMD are to match the default purpose.

## Variables

The following environment variables are used to configure the container.
For example the DATABASE_URL contains a string such as:

```text
jdbc\:oracle\:thin\:@127.0.0.1\:1521/FREEPDB1?oracle.net.disableOob=true that tells your opendcs container where your SQL database is. 
```
which tells the container what database to connect to.

note: These variables (except username and password) are used when creating a configuration file in the container. 


| Variable | Default | Description |
| -------- | ------- | ----------- |
|DATABASE_TYPE|xml| Which type of database this container is for. Current optiosn are `XML`, `OTSDB`, `CWMS`, `HDB`|
|DATABASE_URL|`${DCSTOOL_USERDIR}/edit-db`|URL for the database. either a directory location or a jdbc URL|
|DATABASE_DRIVER|<not set>| If non XML database used the JDBC driver class. Only required for 7.0 images|
|DB_AUTH|env-auth-source:username=DATABASE_USERNAME,password=DATABASE_PASSWORD|How to retrieve database auth information. Defaults to environment. See opendcs properties documentation for how to get information from files.|
|DATABASE_USERNAME|<not set>|Username for the database connection|
|DATABASE_PASSWORD|<not set>|Password for the database connection|
|CWMS_OFFICE|<not set>|Only used for CWMS DATABASE_TYPE containers|
|DATATYPE_STANDARD|<not set>|Desired Datatype naming standard. See OpenDCS Documentation for options.|
|KEYGENERATOR|<not set>|Which sequence/key generated to use. Class name that depends on which DATABASE_TYPE used.|
|APPLICATION_NAME|<depends on image>|Named "process" this container is running as|

# Tags

7.5, 7.5, latest