--------------------------------------------------------------------------
-- This script updates CP tables from an USBR HDB 6.4 CCP Schema to 
-- OpenDCS 6.6 Schema.
--
-- Important!!! This script should be executed as schema user that owns the CP tables.
-- Also: Edit the file defines.sql before executing this script and make 
-- sure CP_OWNER is set correctly.
-- Execute this script in the same directory that contains defines.sql
--------------------------------------------------------------------------

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
set define on
@@defines.sql

----------- snip
--undefine TBL_SPACE_SPEC;
--define TBL_SPACE_SPEC = 'tablespace HDB_DATA'
--
--undefine IDX_TBL_SPACE_SPEC;
--define IDX_TBL_SPACE_SPEC = 'tablespace HDB_IDX'
--
--define DECODES_OWNER;
--define DECODES_OWNER = 'DECODES'
--
--undefine CP_OWNER;
--define CP_OWNER = 'ECODBA'
----------- snip


-- CP_COMP_TS_PARM.SITE_DATATYPE_ID is nullable, modify archive table to match:
ALTER TABLE CP_COMP_TS_PARM_ARCHIVE MODIFY (SITE_DATATYPE_ID NULL);


------------------------------------------------------------------------------
-- OpenDCS Alarm Tables for Oracle
------------------------------------------------------------------------------

CREATE TABLE ALARM_CURRENT
(
    TS_ID int NOT NULL UNIQUE,
    LIMIT_SET_ID int NOT NULL,
    ASSERT_TIME NUMBER(19) NOT NULL,
    DATA_VALUE double precision,
    DATA_TIME NUMBER(19),
    ALARM_FLAGS int NOT NULL,
    MESSAGE varchar2(256),
    LAST_NOTIFICATION_SENT NUMBER(19)
) &TBL_SPACE_SPEC;

CREATE TABLE ALARM_EVENT
(
	ALARM_EVENT_ID INT NOT NULL UNIQUE,
	ALARM_GROUP_ID INT NOT NULL,
	LOADING_APPLICATION_ID INT NOT NULL,
	PRIORITY INT NOT NULL,
	PATTERN varchar2(256)
) &TBL_SPACE_SPEC;

CREATE TABLE ALARM_GROUP
(
	ALARM_GROUP_ID INT NOT NULL UNIQUE,
	ALARM_GROUP_NAME VARCHAR2(32) NOT NULL UNIQUE,
	LAST_MODIFIED NUMBER(19) NOT NULL
) &TBL_SPACE_SPEC;

CREATE TABLE ALARM_HISTORY
(
    TS_ID int NOT NULL,
    LIMIT_SET_ID int NOT NULL,
    ASSERT_TIME NUMBER(19) NOT NULL,
    DATA_VALUE double precision,
    DATA_TIME NUMBER(19),
    ALARM_FLAGS int NOT NULL,
    MESSAGE varchar2(256),
    END_TIME NUMBER(19) NOT NULL,
    CANCELLED_BY varchar2(32),
    PRIMARY KEY (TS_ID, LIMIT_SET_ID, ASSERT_TIME)
) &TBL_SPACE_SPEC;

CREATE TABLE ALARM_LIMIT_SET
(
    LIMIT_SET_ID int NOT NULL UNIQUE,
    SCREENING_ID int NOT NULL,
    season_name varchar2(24),
    reject_high double precision,
    critical_high double precision,
    warning_high double precision,
    warning_low double precision,
    critical_low double precision,
    reject_low double precision,
    stuck_duration varchar2(32),
    stuck_tolerance double precision,
    stuck_min_to_check double precision,
    stuck_max_gap varchar2(32),
    roc_interval varchar2(32),
    reject_roc_high double precision,
    critical_roc_high double precision,
    warning_roc_high double precision,
    warning_roc_low double precision,
    critical_roc_low double precision,
    reject_roc_low double precision,
    missing_period varchar2(32),
    missing_interval varchar2(32),
    missing_max_values int,
    hint_text varchar2(256),
	CONSTRAINT LIMIT_SET_SCRSEA_UNIQUE UNIQUE(SCREENING_ID, season_name)
) &TBL_SPACE_SPEC;

CREATE TABLE ALARM_SCREENING
(
    SCREENING_ID int NOT NULL UNIQUE,
    SCREENING_NAME varchar2(32) NOT NULL UNIQUE,
    SITE_ID int,
    DATATYPE_ID int NOT NULL,
    START_DATE_TIME NUMBER(19),
    LAST_MODIFIED NUMBER(19) NOT NULL,
    ENABLED VARCHAR2(5) DEFAULT 'true' NOT NULL,
    ALARM_GROUP_ID int,
    SCREENING_DESC varchar2(1024),
	CONSTRAINT AS_SDI_START_UNIQUE UNIQUE(SITE_ID, DATATYPE_ID, START_DATE_TIME)
) &TBL_SPACE_SPEC;

CREATE TABLE EMAIL_ADDR
(
	ALARM_GROUP_ID INT NOT NULL,
	ADDR VARCHAR2(256) NOT NULL,
	PRIMARY KEY (ALARM_GROUP_ID, ADDR)
) &TBL_SPACE_SPEC;

CREATE TABLE FILE_MONITOR
(
	ALARM_GROUP_ID INT NOT NULL,
	PATH VARCHAR2(256) NOT NULL,
	PRIORITY INT NOT NULL,
	MAX_FILES int,
	MAX_FILES_HINT VARCHAR2(128),
	-- Maximum Last Modify Time
	MAX_LMT VARCHAR2(32),
	MAX_LMT_HINT VARCHAR2(128),
	ALARM_ON_DELETE VARCHAR2(5),
	ON_DELETE_HINT VARCHAR2(128),
	MAX_SIZE NUMBER(19),
	MAX_SIZE_HINT VARCHAR2(128),
	ALARM_ON_EXISTS VARCHAR2(5),
	ON_EXISTS_HINT VARCHAR2(128),
	ENABLED VARCHAR2(5),
	PRIMARY KEY (ALARM_GROUP_ID, PATH)
) &TBL_SPACE_SPEC;

CREATE TABLE PROCESS_MONITOR
(
	ALARM_GROUP_ID INT NOT NULL,
	LOADING_APPLICATION_ID INT NOT NULL,
	ENABLED VARCHAR2(5),
	PRIMARY KEY (ALARM_GROUP_ID, LOADING_APPLICATION_ID)
) &TBL_SPACE_SPEC;


ALTER TABLE PROCESS_MONITOR
	ADD CONSTRAINT PROCESS_MONITOR_FK1
	FOREIGN KEY (ALARM_GROUP_ID)
	REFERENCES ALARM_GROUP (ALARM_GROUP_ID)
;


ALTER TABLE FILE_MONITOR
	ADD CONSTRAINT FILE_MONITOR_FK1
	FOREIGN KEY (ALARM_GROUP_ID)
	REFERENCES ALARM_GROUP (ALARM_GROUP_ID)
;


ALTER TABLE EMAIL_ADDR
	ADD CONSTRAINT EMAIL_ADDR_FK1
	FOREIGN KEY (ALARM_GROUP_ID)
	REFERENCES ALARM_GROUP (ALARM_GROUP_ID)
;


ALTER TABLE ALARM_EVENT
	ADD CONSTRAINT ALARM_EVENT_FK1
	FOREIGN KEY (ALARM_GROUP_ID, LOADING_APPLICATION_ID)
	REFERENCES PROCESS_MONITOR (ALARM_GROUP_ID, LOADING_APPLICATION_ID)
;

ALTER TABLE PROCESS_MONITOR
	ADD CONSTRAINT PROCESS_MONITOR_FKLA
	FOREIGN KEY (LOADING_APPLICATION_ID)
	REFERENCES HDB_LOADING_APPLICATION(LOADING_APPLICATION_ID)
;

ALTER TABLE ALARM_SCREENING
    ADD CONSTRAINT ALARM_SCREENING_FKAGI
	FOREIGN KEY (ALARM_GROUP_ID)
    REFERENCES ALARM_GROUP (ALARM_GROUP_ID)
;


ALTER TABLE ALARM_HISTORY
    ADD CONSTRAINT ALARM_HISTORY_FKLSI
	FOREIGN KEY (LIMIT_SET_ID)
    REFERENCES ALARM_LIMIT_SET (LIMIT_SET_ID)
;


ALTER TABLE ALARM_CURRENT
    ADD CONSTRAINT ALARM_CURRENT_FKLSI
	FOREIGN KEY (LIMIT_SET_ID)
    REFERENCES ALARM_LIMIT_SET (LIMIT_SET_ID)
;

ALTER TABLE ALARM_LIMIT_SET
    ADD CONSTRAINT LIMIT_SET_FKSI
	FOREIGN KEY (SCREENING_ID)
    REFERENCES ALARM_SCREENING (SCREENING_ID)
;


CREATE INDEX AS_LAST_MODIFIED ON ALARM_SCREENING (LAST_MODIFIED) &IDX_TBL_SPACE_SPEC;

CREATE SEQUENCE ALARM_EVENTIdSeq nocache;
CREATE SEQUENCE ALARM_SCREENINGIdSeq nocache;
CREATE SEQUENCE ALARM_LIMIT_SETIdSeq nocache;

CREATE PUBLIC SYNONYM ALARM_CURRENT FOR &CP_OWNER..ALARM_CURRENT;
CREATE PUBLIC SYNONYM ALARM_EVENT FOR &CP_OWNER..ALARM_EVENT;
CREATE PUBLIC SYNONYM ALARM_GROUP FOR &CP_OWNER..ALARM_GROUP;
CREATE PUBLIC SYNONYM ALARM_HISTORY FOR &CP_OWNER..ALARM_HISTORY;
CREATE PUBLIC SYNONYM ALARM_SCREENING FOR &CP_OWNER..ALARM_SCREENING;
CREATE PUBLIC SYNONYM EMAIL_ADDR FOR &CP_OWNER..EMAIL_ADDR;
CREATE PUBLIC SYNONYM FILE_MONITOR FOR &CP_OWNER..FILE_MONITOR;
CREATE PUBLIC SYNONYM PROCESS_MONITOR FOR &CP_OWNER..PROCESS_MONITOR;


CREATE PUBLIC SYNONYM ALARM_EVENTIdSeq FOR &CP_OWNER..ALARM_EVENTIdSeq;
CREATE PUBLIC SYNONYM ALARM_SCREENINGIdSeq FOR &CP_OWNER..ALARM_SCREENINGIdSeq;
CREATE PUBLIC SYNONYM ALARM_LIMIT_SETIdSeq FOR &CP_OWNER..ALARM_LIMIT_SETIdSeq;

-- The following 3 lines were missing from the initial release:
CREATE PUBLIC SYNONYM ALARM_LIMIT_SET FOR &CP_OWNER..ALARM_LIMIT_SET;
CREATE SEQUENCE ALARM_GROUPIdSeq nocache;
CREATE PUBLIC SYNONYM ALARM_GROUPIdSeq FOR &CP_OWNER..ALARM_GROUPIdSeq;


delete from tsdb_database_version;
insert into tsdb_database_version values(17, 'OPENDCS 6.6');

delete from decodesdatabaseversion;
insert into decodesdatabaseversion values(17, 'OPENDCS 6.6');

spool off
exit;
