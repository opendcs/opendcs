--------------------------------------------------------------------------
-- This script updates a DCSTOOL 6.1 CCP Schema (corresponding to CWMS 3.0
-- to OpenDCS 6.2 Schema
--------------------------------------------------------------------------

-----------------------------------------------------------------------------
-- This software was written by Cove Software, LLC ("COVE") under contract 
-- to the United States Government. 
-- No warranty is provided or implied other than specific contractual terms
-- between COVE and the U.S. Government
-- 
-- Copyright 2016 U.S. Army Corps of Engineers, Hydrologic Engineering Center.
-- All rights reserved.
-----------------------------------------------------------------------------

set echo on
spool combined.log
    
whenever sqlerror continue
set define on
@@defines.sql

-------------------------------------------------------------------
-- Drop and recreate DACQ_EVENT with new LOADING APP ID FK
-------------------------------------------------------------------

DROP TABLE DACQ_EVENT;
CREATE TABLE DACQ_EVENT
(
	-- Surrogate Key. Events are numbered from 0...MAX
	DACQ_EVENT_ID NUMBER(18) NOT NULL,
	SCHEDULE_ENTRY_STATUS_ID NUMBER(18),
	PLATFORM_ID NUMBER(18),
	EVENT_TIME date NOT NULL,
	-- INFO = 3, WARNING = 4, FAILURE = 5, FATAL = 6
	-- 
	EVENT_PRIORITY INT NOT NULL,
	-- Software subsystem that generated this event
	SUBSYSTEM VARCHAR2(24),
    -- If this is related to a message, this holds the message's local_recv_time.
    MSG_RECV_TIME DATE,
	EVENT_TEXT VARCHAR2(256) NOT NULL,
    LOADING_APPLICATION_ID NUMBER(18) NOT NULL,
	db_office_code integer default &dflt_office_code,
	PRIMARY KEY (DACQ_EVENT_ID)
) &TBL_SPACE_SPEC;

ALTER TABLE DACQ_EVENT
	ADD CONSTRAINT DACQ_EVENT_FKPL
	FOREIGN KEY (PLATFORM_ID)
	REFERENCES PLATFORM (ID)
;

ALTER TABLE DACQ_EVENT
	ADD CONSTRAINT DACQ_EVENT_FKSE
	FOREIGN KEY (SCHEDULE_ENTRY_STATUS_ID)
	REFERENCES SCHEDULE_ENTRY_STATUS (SCHEDULE_ENTRY_STATUS_ID)
;

ALTER TABLE DACQ_EVENT
	ADD CONSTRAINT DACQ_EVENT_FKLA
	FOREIGN KEY (LOADING_APPLICATION_ID)
	REFERENCES HDB_LOADING_APPLICATION(LOADING_APPLICATION_ID)
;

CREATE INDEX PLATFORM_ID_IDX ON DACQ_EVENT (PLATFORM_ID) &TBL_SPACE_SPEC;
CREATE INDEX EVT_PLAT_MSG_IDX ON DACQ_EVENT (PLATFORM_ID, MSG_RECV_TIME);
CREATE INDEX EVT_SCHED_IDX ON DACQ_EVENT (SCHEDULE_ENTRY_STATUS_ID);
CREATE INDEX EVT_TIME_IDX ON DACQ_EVENT (EVENT_TIME);

DROP SEQUENCE DACQ_EVENTIDSEQ;
CREATE SEQUENCE DACQ_EVENTIDSEQ as NUMBER(18) MINVALUE 1 START WITH 1 NOCACHE;

-----------------------------------------------------------------
-- Finally, update the database version numbers in the database
-----------------------------------------------------------------
delete from DecodesDatabaseVersion;
insert into DecodesDatabaseVersion values(15, 'Updated to OpenDCS 6.4');
delete from tsdb_database_version;
insert into tsdb_database_version values(15, 'Updated to OpenDCS 6.4');

spool off
exit;
