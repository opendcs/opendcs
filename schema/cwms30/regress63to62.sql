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
-- Undo the changes for 6.3
-------------------------------------------------------------------

DROP TABLE CP_COMP_DEPENDS_SCRATCHPAD;
CREATE TABLE CP_COMP_DEPENDS_SCRATCHPAD
(
    TS_ID NUMBER(18) NOT NULL,
    COMPUTATION_ID NUMBER(18) NOT NULL,
    PRIMARY KEY (TS_ID, COMPUTATION_ID)
) &TBL_SPACE_SPEC;

ALTER TABLE CP_COMP_DEPENDS_SCRATCHPAD
    ADD CONSTRAINT CP_COMP_DEPENDS_SCRATCHPAD_FK
    FOREIGN KEY (COMPUTATION_ID)
    REFERENCES CP_COMPUTATION (COMPUTATION_ID)
;

INSERT INTO CP_COMP_DEPENDS_SCRATCHPAD 
	SELECT a.TS_ID, a.COMPUTATION_ID
		FROM CP_COMP_DEPENDS a;

DROP TABLE CP_COMP_DEPENDS;
CREATE TABLE CP_COMP_DEPENDS
(
    TS_ID NUMBER(18) NOT NULL,
    COMPUTATION_ID NUMBER(18) NOT NULL,
    PRIMARY KEY (TS_ID, COMPUTATION_ID)
) &TBL_SPACE_SPEC;

ALTER TABLE CP_COMP_DEPENDS
    ADD CONSTRAINT CP_COMP_DEPENDS_FK
    FOREIGN KEY (COMPUTATION_ID)
    REFERENCES CP_COMPUTATION (COMPUTATION_ID)
;

INSERT INTO CP_COMP_DEPENDS SELECT * FROM CP_COMP_DEPENDS_SCRATCHPAD;
DELETE FROM CP_COMP_DEPENDS_SCRATCHPAD;

DROP TABLE CP_DEPENDS_NOTIFY;
CREATE TABLE CP_DEPENDS_NOTIFY
(
    RECORD_NUM NUMBER(18) NOT NULL,
    EVENT_TYPE CHAR NOT NULL,
    KEY NUMBER(18) NOT NULL,
    DATE_TIME_LOADED date NOT NULL,
    PRIMARY KEY (RECORD_NUM)
) &TBL_SPACE_SPEC;

DELETE FROM CP_DEPENDS_NOTIFY;

DROP SEQUENCE CP_DEPENDS_NOTIFYIDSEQ;

delete from DecodesDatabaseVersion;
insert into DecodesDatabaseVersion values(13, 'Regressed to OpenDCS 6.2');
delete from tsdb_database_version;
insert into tsdb_database_version values(13, 'Regressed to OpenDCS 6.2');

spool off
exit;
