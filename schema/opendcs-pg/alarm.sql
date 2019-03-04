
/* Drop Indexes */

DROP INDEX IF EXISTS LIMIT_SET_SDI_UNIQUE;
DROP INDEX IF EXISTS AS_LAST_MODIFIED;



/* Drop Tables */

DROP TABLE IF EXISTS ALARM_CURRENT;
DROP TABLE IF EXISTS ALARM_EVENT;
DROP TABLE IF EXISTS FILE_MONITOR;
DROP TABLE IF EXISTS PROCESS_MONITOR;
DROP TABLE IF EXISTS EMAIL_ADDR;
DROP TABLE IF EXISTS ALARM_HISTORY;
DROP TABLE IF EXISTS ALARM_LIMIT_SET;
DROP TABLE IF EXISTS ALARM_SCREENING;
DROP TABLE IF EXISTS ALARM_GROUP;




/* Create Tables */

CREATE TABLE ALARM_CURRENT
(
	-- Surrogate key of time series that triggered the alarm.
	-- There can only be one current alarm assertion for a time series.
	TS_ID int NOT NULL UNIQUE,
	LIMIT_SET_ID int NOT NULL,
	-- Date/Time that alarm was asserted.
	-- May be different from the time stamp of the alarm value.
	ASSERT_TIME bigint NOT NULL,
	-- Value that caused alarm assertion.
	-- May be null in the case of MISSING VALUE alarms.
	DATA_VALUE double precision,
	-- Time stamp of the data value that triggered the alarm.
	-- May be null for missing value alarms.
	DATA_TIME bigint,
	-- Bit fields indicating the alarm conditions.
	ALARM_FLAGS int NOT NULL,
	-- Message constructed for this alarm assertion.
	MESSAGE varchar(256),
	-- Date/Time when the last email notification was sent for this alarm.
	-- NULL means no notification was sent.
	LAST_NOTIFICATION_SENT bigint,
	PRIMARY KEY (TS_ID)
) WITHOUT OIDS;


CREATE TABLE ALARM_EVENT
(
	ALARM_EVENT_ID int NOT NULL UNIQUE,
	ALARM_GROUP_ID int NOT NULL,
	LOADING_APPLICATION_ID int NOT NULL,
	PRIORITY int NOT NULL,
	PATTERN varchar(256),
	PRIMARY KEY (ALARM_EVENT_ID)
) WITHOUT OIDS;


CREATE TABLE ALARM_GROUP
(
	ALARM_GROUP_ID int NOT NULL UNIQUE,
	ALARM_GROUP_NAME varchar(32) NOT NULL UNIQUE,
	LAST_MODIFIED bigint NOT NULL,
	PRIMARY KEY (ALARM_GROUP_ID)
) WITHOUT OIDS;


CREATE TABLE ALARM_HISTORY
(
	TS_ID int NOT NULL,
	LIMIT_SET_ID int NOT NULL,
	ASSERT_TIME bigint NOT NULL,
	DATA_VALUE double precision,
	DATA_TIME bigint,
	ALARM_FLAGS int NOT NULL,
	MESSAGE varchar(256),
	-- Time alarm was de-asserted or cancelled.
	END_TIME bigint NOT NULL,
	-- If alarm was manually cancelled, this is the user name who cancelled it.
	-- If it was de-asserted automatically (e.g. by an out of range value coming back into range). This will be null.
	CANCELLED_BY varchar(32),
	PRIMARY KEY (TS_ID, LIMIT_SET_ID, ASSERT_TIME)
) WITHOUT OIDS;


CREATE TABLE ALARM_LIMIT_SET
(
	LIMIT_SET_ID int NOT NULL UNIQUE,
	-- Surrogate Key
	SCREENING_ID int NOT NULL,
	-- If null, then limit set is good all year.
	-- If specified, name must match one of the season names defined in rledit.
	season_name varchar(24),
	-- If not null, values >= this are rejected.
	reject_high double precision,
	-- If not null, values >= this are considered in critical range.
	critical_high double precision,
	-- If not null, values >= are in warning range
	warning_high double precision,
	-- If not null, values <= this are in warning range
	warning_low double precision,
	-- If not null, values <= this are considered in critical range.
	critical_low double precision,
	-- if not null, values <= this are rejected.
	reject_low double precision,
	-- Duration over which to check for stuck sensor.
	-- If null, then no stuck-sensor checks are done
	stuck_duration varchar(32),
	-- Value must change by more than this amount over duration in order
	-- to be considered un-stuck.
	-- If zero, then any change means unstuck.
	stuck_tolerance double precision,
	-- If not null, don't check values <= this value.
	stuck_min_to_check double precision,
	-- An optional interval. If more than this amount of time has elapsed
	-- before the value being checked, don't do the stuck sensor check.
	stuck_max_gap varchar(32),
	-- Interval over which to do rate of change checks.
	-- If null, no roc checks are defined.
	roc_interval varchar(32),
	reject_roc_high double precision,
	critical_roc_high double precision,
	warning_roc_high double precision,
	warning_roc_low double precision,
	critical_roc_low double precision,
	reject_roc_low double precision,
	-- Defines the period over which missing-value checks are done.
	-- Null means no missing check performed.
	missing_period varchar(32),
	-- Valid storage interval in the underlying database.
	-- Must be less than the period.
	-- If period is defined, this may not be null.
	missing_interval varchar(32),
	-- Alarm is triggered if the number of missing values in the period
	-- is > this threshold.
	-- If period is defined, this may not be null.
	missing_max_values int,
	-- Optional text to be used in email notifications generated from these limits.
	hint_text varchar(256),
	PRIMARY KEY (LIMIT_SET_ID),
	UNIQUE (SCREENING_ID, season_name)
) WITHOUT OIDS;


CREATE TABLE ALARM_SCREENING
(
	-- Surrogate Key
	SCREENING_ID int NOT NULL UNIQUE,
	SCREENING_NAME varchar(32) NOT NULL UNIQUE,
	-- If NULL, then this is default screening for DataType
	SITE_ID int,
	-- Foreign Key to DataType table
	DATATYPE_ID int NOT NULL,
	-- Start of appicable time for this screening.
	-- If null, this screening goes back to beginning of time.
	START_DATE_TIME bigint,
	-- Time that this record was last written/modified
	LAST_MODIFIED bigint NOT NULL,
	-- Only do this screening if enabled
	ENABLED boolean DEFAULT 'true' NOT NULL,
	-- If not null, then alarms from this screening will cause email to the group.
	ALARM_GROUP_ID int,
	-- Description
	SCREENING_DESC varchar(1024),
	PRIMARY KEY (SCREENING_ID),
	UNIQUE (SITE_ID, DATATYPE_ID, START_DATE_TIME)
) WITHOUT OIDS;


CREATE TABLE EMAIL_ADDR
(
	ALARM_GROUP_ID int NOT NULL,
	ADDR varchar(256) NOT NULL,
	PRIMARY KEY (ALARM_GROUP_ID, ADDR)
) WITHOUT OIDS;


CREATE TABLE FILE_MONITOR
(
	ALARM_GROUP_ID int NOT NULL,
	PATH varchar(256) NOT NULL,
	PRIORITY int NOT NULL,
	MAX_FILES int,
	MAX_FILES_HINT varchar(128),
	-- Maximum Last Modify Time
	MAX_LMT varchar(32),
	MAX_LMT_HINT varchar(128),
	ALARM_ON_DELETE boolean,
	ON_DELETE_HINT varchar(128),
	MAX_SIZE bigint,
	MAX_SIZE_HINT varchar(128),
	ALARM_ON_EXISTS boolean,
	ON_EXISTS_HINT varchar(128),
	ENABLED boolean,
	PRIMARY KEY (ALARM_GROUP_ID, PATH)
) WITHOUT OIDS;


CREATE TABLE PROCESS_MONITOR
(
	ALARM_GROUP_ID int NOT NULL,
	LOADING_APPLICATION_ID int NOT NULL,
	ENABLED boolean,
	PRIMARY KEY (ALARM_GROUP_ID, LOADING_APPLICATION_ID)
) WITHOUT OIDS;



/* Create Foreign Keys */

ALTER TABLE FILE_MONITOR
	ADD FOREIGN KEY (ALARM_GROUP_ID)
	REFERENCES ALARM_GROUP (ALARM_GROUP_ID)
	ON UPDATE RESTRICT
	ON DELETE RESTRICT
;


ALTER TABLE PROCESS_MONITOR
	ADD FOREIGN KEY (ALARM_GROUP_ID)
	REFERENCES ALARM_GROUP (ALARM_GROUP_ID)
	ON UPDATE RESTRICT
	ON DELETE RESTRICT
;


ALTER TABLE EMAIL_ADDR
	ADD FOREIGN KEY (ALARM_GROUP_ID)
	REFERENCES ALARM_GROUP (ALARM_GROUP_ID)
	ON UPDATE RESTRICT
	ON DELETE RESTRICT
;


ALTER TABLE ALARM_SCREENING
	ADD FOREIGN KEY (ALARM_GROUP_ID)
	REFERENCES ALARM_GROUP (ALARM_GROUP_ID)
	ON UPDATE RESTRICT
	ON DELETE RESTRICT
;


ALTER TABLE ALARM_HISTORY
	ADD FOREIGN KEY (LIMIT_SET_ID)
	REFERENCES ALARM_LIMIT_SET (LIMIT_SET_ID)
	ON UPDATE RESTRICT
	ON DELETE RESTRICT
;


ALTER TABLE ALARM_CURRENT
	ADD FOREIGN KEY (LIMIT_SET_ID)
	REFERENCES ALARM_LIMIT_SET (LIMIT_SET_ID)
	ON UPDATE RESTRICT
	ON DELETE RESTRICT
;


ALTER TABLE ALARM_LIMIT_SET
	ADD FOREIGN KEY (SCREENING_ID)
	REFERENCES ALARM_SCREENING (SCREENING_ID)
	ON UPDATE RESTRICT
	ON DELETE RESTRICT
;


ALTER TABLE ALARM_EVENT
	ADD FOREIGN KEY (ALARM_GROUP_ID, LOADING_APPLICATION_ID)
	REFERENCES PROCESS_MONITOR (ALARM_GROUP_ID, LOADING_APPLICATION_ID)
	ON UPDATE RESTRICT
	ON DELETE RESTRICT
;



/* Create Indexes */

-- Ensure that site/datatype/season combo is unique
CREATE INDEX LIMIT_SET_SDI_UNIQUE ON ALARM_LIMIT_SET USING BTREE (season_name);
-- For compproc to quickly detect any changes.
CREATE INDEX AS_LAST_MODIFIED ON ALARM_SCREENING USING BTREE (LAST_MODIFIED);



/* Comments */

COMMENT ON COLUMN ALARM_CURRENT.TS_ID IS 'Surrogate key of time series that triggered the alarm.
There can only be one current alarm assertion for a time series.';
COMMENT ON COLUMN ALARM_CURRENT.ASSERT_TIME IS 'Date/Time that alarm was asserted.
May be different from the time stamp of the alarm value.';
COMMENT ON COLUMN ALARM_CURRENT.DATA_VALUE IS 'Value that caused alarm assertion.
May be null in the case of MISSING VALUE alarms.';
COMMENT ON COLUMN ALARM_CURRENT.DATA_TIME IS 'Time stamp of the data value that triggered the alarm.
May be null for missing value alarms.';
COMMENT ON COLUMN ALARM_CURRENT.ALARM_FLAGS IS 'Bit fields indicating the alarm conditions.';
COMMENT ON COLUMN ALARM_CURRENT.MESSAGE IS 'Message constructed for this alarm assertion.';
COMMENT ON COLUMN ALARM_CURRENT.LAST_NOTIFICATION_SENT IS 'Date/Time when the last email notification was sent for this alarm.
NULL means no notification was sent.';
COMMENT ON COLUMN ALARM_HISTORY.END_TIME IS 'Time alarm was de-asserted or cancelled.';
COMMENT ON COLUMN ALARM_HISTORY.CANCELLED_BY IS 'If alarm was manually cancelled, this is the user name who cancelled it.
If it was de-asserted automatically (e.g. by an out of range value coming back into range). This will be null.';
COMMENT ON COLUMN ALARM_LIMIT_SET.SCREENING_ID IS 'Surrogate Key';
COMMENT ON COLUMN ALARM_LIMIT_SET.season_name IS 'If null, then limit set is good all year.
If specified, name must match one of the season names defined in rledit.';
COMMENT ON COLUMN ALARM_LIMIT_SET.reject_high IS 'If not null, values >= this are rejected.';
COMMENT ON COLUMN ALARM_LIMIT_SET.critical_high IS 'If not null, values >= this are considered in critical range.';
COMMENT ON COLUMN ALARM_LIMIT_SET.warning_high IS 'If not null, values >= are in warning range';
COMMENT ON COLUMN ALARM_LIMIT_SET.warning_low IS 'If not null, values <= this are in warning range';
COMMENT ON COLUMN ALARM_LIMIT_SET.critical_low IS 'If not null, values <= this are considered in critical range.';
COMMENT ON COLUMN ALARM_LIMIT_SET.reject_low IS 'if not null, values <= this are rejected.';
COMMENT ON COLUMN ALARM_LIMIT_SET.stuck_duration IS 'Duration over which to check for stuck sensor.
If null, then no stuck-sensor checks are done';
COMMENT ON COLUMN ALARM_LIMIT_SET.stuck_tolerance IS 'Value must change by more than this amount over duration in order
to be considered un-stuck.
If zero, then any change means unstuck.';
COMMENT ON COLUMN ALARM_LIMIT_SET.stuck_min_to_check IS 'If not null, don''t check values <= this value.';
COMMENT ON COLUMN ALARM_LIMIT_SET.stuck_max_gap IS 'An optional interval. If more than this amount of time has elapsed
before the value being checked, don''t do the stuck sensor check.';
COMMENT ON COLUMN ALARM_LIMIT_SET.roc_interval IS 'Interval over which to do rate of change checks.
If null, no roc checks are defined.';
COMMENT ON COLUMN ALARM_LIMIT_SET.missing_period IS 'Defines the period over which missing-value checks are done.
Null means no missing check performed.';
COMMENT ON COLUMN ALARM_LIMIT_SET.missing_interval IS 'Valid storage interval in the underlying database.
Must be less than the period.
If period is defined, this may not be null.';
COMMENT ON COLUMN ALARM_LIMIT_SET.missing_max_values IS 'Alarm is triggered if the number of missing values in the period
is > this threshold.
If period is defined, this may not be null.';
COMMENT ON COLUMN ALARM_LIMIT_SET.hint_text IS 'Optional text to be used in email notifications generated from these limits.';
COMMENT ON COLUMN ALARM_SCREENING.SCREENING_ID IS 'Surrogate Key';
COMMENT ON COLUMN ALARM_SCREENING.SITE_ID IS 'If NULL, then this is default screening for DataType';
COMMENT ON COLUMN ALARM_SCREENING.DATATYPE_ID IS 'Foreign Key to DataType table';
COMMENT ON COLUMN ALARM_SCREENING.START_DATE_TIME IS 'Start of appicable time for this screening.
If null, this screening goes back to beginning of time.';
COMMENT ON COLUMN ALARM_SCREENING.LAST_MODIFIED IS 'Time that this record was last written/modified';
COMMENT ON COLUMN ALARM_SCREENING.ENABLED IS 'Only do this screening if enabled';
COMMENT ON COLUMN ALARM_SCREENING.ALARM_GROUP_ID IS 'If not null, then alarms from this screening will cause email to the group.';
COMMENT ON COLUMN ALARM_SCREENING.SCREENING_DESC IS 'Description';
COMMENT ON COLUMN FILE_MONITOR.MAX_LMT IS 'Maximum Last Modify Time';



