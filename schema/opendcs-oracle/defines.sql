undefine LOG;
define LOG = createdb.log;

-- Directory where table space files are created
undefine TBL_SPACE_DIR;
define TBL_SPACE_DIR = /home/oracle/app/oradata/opendcs_tsdb;

-- Name of the DATA table space
undefine TBL_SPACE_DATA;
define TBL_SPACE_DATA = opentsdb_data;

-- Name of the TEMP table space
undefine TBL_SPACE_TEMP;
define TBL_SPACE_TEMP = opentsdb_temp;

-- Name of the new Oracle User that will own the DECODES schema
undefine TSDB_ADM_SCHEMA;
define TSDB_ADM_SCHEMA = tsdb_adm;

-- and the password thereof
undefine TSDB_ADM_PASSWD;
define TSDB_ADM_PASSWD = xxxxxxxx;

-- Used in create table statements. Should match the definition for TBL_SPACE_DATA above.
undefine TBL_SPACE_SPEC;
define TBL_SPACE_SPEC = 'tablespace opentsdb_data'
