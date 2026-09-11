# OpenDCS Docker files


# LRGS

To setup a permanent instance of an LRGS do the following, replace `--name lrgs` with a name
appropriate to your installation.

```
docker create volume lrgs_volume
docker run -d name lrgs -p 16003:16003 -v lrgs_volume:/lrgs_home -e LRGS_ADMIN_PASSWORD="the password you want" lrgs:latest

```

16003 is the legacy DDS protocol port that the GUI `rtstat` application can use.
The LRGS image can also enable DDS-over-HTTP on port 7000.
There are additional input sources, and the ability to add additional custom input sources that may require you to 
expose additional ports.

## Variables

| Variable | Default | Description |
|----------|---------|-------------|
| LRGSHOME | /lrgs_home | location of primary files and output. |
| LRGS_ADMIN_PASSWORD| <not set> | Admin password to use. If not set will be randomly generated and printed to console |
| LRGS_HTTP_ENABLED | false | Enable the embedded DDS-over-HTTP service. |
| LRGS_HTTP_PORT | 7000 | Internal port for DDS-over-HTTP. |
| DCPMON_FIXTURES_ENABLED | false | Load deterministic DCPMon demo data. Development use only. |

## Local DCPMon stack

`docker compose up --build` starts the full OpenDCS development stack plus an
LRGS and DCPMon. DCPMon is available at `http://localhost:7200`; direct LRGS
DDS-over-HTTP access is available at `http://localhost:7001/dds`. The Compose
LRGS uses ephemeral storage and deterministic SWT fixtures so every launch is
repeatable and does not require external DDS credentials.
The Compose PostgreSQL service is exposed on host port `5433` so it can run
alongside a local PostgreSQL service using the default `5432` port.

# TsDbApps

The following containers support automated processing. The computation containers, that start with 'comp' require using one of the SQL databases. 

| Container | Purpose |
|-----------|---------|
| compdepends | Determines which data inputs trigger computations. |
| compproc | Handles computation |
| routingscheduler | Can pull data from an LRGS or other data source and push to the database or other locations |

The above containers are the same except that the default `APPLICATION_NAME` and CMD are to match the default purpose.


# Migration

The migration container supports all of the below items as well as additional values to support the various flyway operations. DATABASE_USER and DATABASE_PASSWORD are replaced by the 4 entries described below.


| Variable | Default | Description |
| -------- | ------- | ----------- |
| MIGRATION_USER | <not set> | Database Schema owning or user permitted to perform Data Definition Language changes |
| MIGRATION_PASSWORD | <not set> | Password for the migration user |
| APP_USER | <not set> | User name for the initial dbimport and compimport as all as other applications that will connect |
| APP_PASSWORD | <not set> | Password for the app users |
| PLACEHOLDER_\* | <not set> | The SQL files of each implementation may contain Flyway Placeholder values. Any variable starting with PLACEHOLDER_ will have the PLACEHOLDER_ prefix striped and passed along to flyway for use. |

## Variables

The following environment variables are used to configure the container.
For example the DATABASE_URL contains a string such as:

```text
jdbc\:oracle\:thin\:@127.0.0.1\:1521/FREEPDB1?oracle.net.disableOob=true  
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
