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

CREATE TABLE CP_ALGO_SCRIPT
(
	ALGORITHM_ID NUMBER(18) NOT NULL,
	SCRIPT_TYPE CHAR NOT NULL,
	BLOCK_NUM NUMBER(4) NOT NULL,
	SCRIPT_DATA VARCHAR2(4000) NOT NULL,
	PRIMARY KEY (ALGORITHM_ID, SCRIPT_TYPE, BLOCK_NUM)
) &TBL_SPACE_SPEC;


ALTER TABLE CP_ALGO_SCRIPT
    ADD CONSTRAINT CP_ALGO_SCRIPT_FK
    FOREIGN KEY (ALGORITHM_ID)
    REFERENCES CP_ALGORITHM (ALGORITHM_ID)
;

-----------------------------------------------------------------
-- Delete the unused tables from the previous composites design.
-----------------------------------------------------------------
DROP PUBLIC SYNONYM CP_COMPOSITE_DIAGRAM;
DROP TABLE CP_COMPOSITE_DIAGRAM;
DROP PUBLIC SYNONYM CP_COMPOSITE_MEMBER;
DROP TABLE CP_COMPOSITE_MEMBER;

-----------------------------------------------------------------
-- two new sequences for the high volume ccp/decodes tables so they don't use CWMS_SEQ:
-----------------------------------------------------------------
CREATE SEQUENCE DACQ_EVENTIDSEQ MINVALUE 1 START WITH 1 MAXVALUE 2000000000 NOCACHE CYCLE;
CREATE SEQUENCE SCHEDULE_ENTRY_STATUSIDSEQ MINVALUE 1 START WITH 1 MAXVALUE 2000000000 NOCACHE CYCLE;

-----------------------------------------------------------------
-- permissions for the new stuff.
-----------------------------------------------------------------
GRANT SELECT,INSERT,UPDATE,DELETE ON CP_ALGO_SCRIPT TO CCP_USERS;
GRANT SELECT ON DACQ_EVENTIDSEQ TO CCP_USERS;
GRANT SELECT ON SCHEDULE_ENTRY_STATUSIDSEQ TO CCP_USERS;

-----------------------------------------------------------------
-- public synonyms for the new stuff
-----------------------------------------------------------------
CREATE PUBLIC SYNONYM CP_ALGO_SCRIPT FOR CCP.CP_ALGO_SCRIPT;
CREATE PUBLIC SYNONYM DACQ_EVENTIDSEQ FOR CCP.DACQ_EVENTIDSEQ;
CREATE PUBLIC SYNONYM SCHEDULE_ENTRY_STATUSIDSEQ FOR CCP.SCHEDULE_ENTRY_STATUSIDSEQ;

-----------------------------------------------------------------
-- Finally, update the database version numbers in the database
-----------------------------------------------------------------
delete from DecodesDatabaseVersion;
insert into DecodesDatabaseVersion values(13, 'Updated to OpenDCS 6.2 RC01');
delete from tsdb_database_version;
insert into tsdb_database_version values(13, 'Updated to OpenDCS 6.2 RC01');

-----------------------------------------------------------------
-- Just in case, delete existing dacq event and se status records with old sequence nums
-----------------------------------------------------------------
delete from DACQ_EVENT;
delete from PLATFORM_STATUS;
delete from SCHEDULE_ENTRY_STATUS;

spool off
exit;
