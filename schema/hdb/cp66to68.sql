--------------------------------------------------------------------------
-- This script updates CP tables from an USBR HDB 6.6 CCP Schema to 
-- OpenDCS 6.8 Schema.
--
-- Important!!! This script should be executed as schema user that owns the CP tables.
-- Also: Edit the file defines.sql before executing this script and make 
-- sure CP_OWNER is set correctly.
-- Execute this script in the same directory that contains defines.sql
--------------------------------------------------------------------------

		if (theDb.getDecodesDatabaseVersion() < DecodesDatabaseVersion.DECODES_DB_67)
-----------------------------------------------------------------------------
-- This software was written by Cove Software, LLC ("COVE") under contract 
-- to the United States Government. 
-- No warranty is provided or implied other than specific contractual terms
-- between COVE and the U.S. Government
-- 
-- Copyright 2019 U.S. Government.
-----------------------------------------------------------------------------
set echo on
spool combined.log
    
whenever sqlerror continue

ALTER TABLE ALARM_SCREENING ADD LOADING_APPLICATION_ID INT;
ALTER TABLE ALARM_SCREENING DROP CONSTRAINT AS_SDI_START_UNIQUE;
ALTER TABLE ALARM_SCREENING ADD CONSTRAINT AS_SDI_START_UNIQUE
	UNIQUE(SITE_ID, DATATYPE_ID, START_DATE_TIME, LOADING_APPLICATION_ID);
ALTER TABLE ALARM_SCREENING ADD CONSTRAINT AS_APP_FK
	FOREIGN KEY (LOADING_APPLICATION_ID)
	REFERENCES HDB_LOADING_APPLICATION(LOADING_APPLICATION_ID);


ALTER TABLE ALARM_CURRENT ADD LOADING_APPLICATION_ID INT;
ALTER TABLE ALARM_CURRENT DROP UNIQUE(TS_ID);
ALTER TABLE ALARM_CURRENT ADD CONSTRAINT AC_PK_UNIQUE UNIQUE(TS_ID, LOADING_APPLICATION_ID);
ALTER TABLE ALARM_CURRENT ADD CONSTRAINT AC_APP_FK
    FOREIGN KEY (LOADING_APPLICATION_ID)
    REFERENCES HDB_LOADING_APPLICATION(LOADING_APPLICATION_ID);

ALTER TABLE ALARM_HISTORY ADD LOADING_APPLICATION_ID INT;
ALTER TABLE ALARM_HISTORY DROP PRIMARY KEY;
ALTER TABLE ALARM_HISTORY ADD CONSTRAINT AH_PK_UNIQUE 
  UNIQUE(TS_ID, LIMIT_SET_ID, ASSERT_TIME, LOADING_APPLICATION_ID);
ALTER TABLE ALARM_HISTORY ADD CONSTRAINT AH_APP_FK
    FOREIGN KEY (LOADING_APPLICATION_ID)
    REFERENCES HDB_LOADING_APPLICATION(LOADING_APPLICATION_ID);

delete from tsdb_database_version;
insert into tsdb_database_version values(68, 'OPENDCS 6.8');

delete from decodesdatabaseversion;
insert into decodesdatabaseversion values(68, 'OPENDCS 6.8');

spool off
exit;
