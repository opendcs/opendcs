--------------------------------------------------------------------------
-- This script updates DECODES tables from an USBR HDB 6.3 CCP Schema to 
-- OpenDCS 6.4 Schema.
--
-- See the README file in this folder for instructions.
--------------------------------------------------------------------------

-----------------------------------------------------------------------------
-- This software was written by Cove Software, LLC ("COVE") under contract 
-- to the United States Government. 
-- No warranty is provided or implied other than specific contractual terms
-- between COVE and the U.S. Government
-- 
-- Copyright 2017 U.S. Government.
-----------------------------------------------------------------------------

set echo on
spool combined.log
    
whenever sqlerror continue
set define on
@@defines.sql

----------- snip
--undefine TBL_SPACE_SPEC;
--define TBL_SPACE_SPEC = 'tablespace HDB_DATA'
--
--undefine IDX_TBL_SPACE_SPEC;
--define IDX_TBL_SPACE_SPEC = 'tablespace HDB_IDX'
--
--undefine DECODES_OWNER;
--define DECODES_OWNER = 'DECODES'
----------- snip

DELETE FROM DACQ_EVENT;

ALTER TABLE DACQ_EVENT ADD LOADING_APPLICATION_ID NUMBER(*,0);

ALTER TABLE DACQ_EVENT ADD CONSTRAINT DACQ_EVENT_FKLA
    FOREIGN KEY (LOADING_APPLICATION_ID) REFERENCES
    HDB_LOADING_APPLICATION(LOADING_APPLICATION_ID);

DROP SEQUENCE DACQ_EVENTIDSEQ;
CREATE SEQUENCE DACQ_EVENTIDSEQ MINVALUE 1 START WITH 1 NOCACHE;

-----------------------------------------------------------------
-- Finally, update the database version numbers in the database
-----------------------------------------------------------------
delete from DecodesDatabaseVersion;
insert into DecodesDatabaseVersion values(15, 'Updated to OpenDCS 6.4');
delete from tsdb_database_version;
insert into tsdb_database_version values(15, 'Updated to OpenDCS 6.4');

spool off
exit;
