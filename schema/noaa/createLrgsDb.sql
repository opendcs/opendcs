-- This script creates the tables for the LRGS Database.
--

--------------------------------------------------------------
-- Single record LrgsDatabaseVersion table.
-- Identifies the database and contains version number,
-- which identifies the table structure.
--------------------------------------------------------------
CREATE TABLE lrgs_database
(
	db_ver INTEGER NOT NULL,
	create_time TIMESTAMP,
	created_by VARCHAR(24),
	description VARCHAR(1024)
) ;

INSERT into lrgs_database VALUES(1, NOW(), NULL, NULL);

-- MJM: OpenDCS 6.2 does not support Outage Recovery
--------------------------------------------------------------
-- 3 different kinds of gaps. LRGS does recovery in 3 
-- different ways. The system_outage, domsat_gap and damsnt_outage
-- will share the same sequence.
--
-- system_outage
--------------------------------------------------------------
-- CREATE TABLE system_outage
-- (
-- 	outage_id INTEGER NOT NULL PRIMARY KEY,
-- 	begin_time TIMESTAMP,
-- 	end_time TIMESTAMP,
-- 	status_code CHAR
-- );

-- Guarantees no two System_outages have the same ID:
-- CREATE UNIQUE INDEX System_Outage_IdIdx on system_outage (outage_id) ;

--------------------------------------------------------------
-- domsat_gap
--------------------------------------------------------------
-- CREATE TABLE domsat_gap
-- (
-- 	outage_id INTEGER NOT NULL PRIMARY KEY,
-- 	begin_time TIMESTAMP,
-- 	begin_seq INTEGER,
-- 	end_time TIMESTAMP,
-- 	end_seq INTEGER,
-- 	status_code CHAR
-- );

-- Guarantees no two domsat_gap have the same ID:
-- CREATE UNIQUE INDEX Domsat_Gap_IdIdx on domsat_gap (outage_id) ;

--------------------------------------------------------------
-- damsnt_outage
--------------------------------------------------------------
-- CREATE TABLE damsnt_outage
-- (
-- 	outage_id INTEGER NOT NULL PRIMARY KEY,
-- 	data_source_id INTEGER,
-- 	begin_time TIMESTAMP,
-- 	end_time TIMESTAMP,
-- 	status_code CHAR
-- );

-- Guarantees no two damsnt_outage have the same ID:
-- CREATE UNIQUE INDEX Damsnt_Outage_IdIdx on damsnt_outage (outage_id) ;

--------------------------------------------------------------
-- data_source
-- Each msg in storage holds a data_source_id.
-- Data Source uniquely identified by type and name.
-- DDS & DAMS_NT: name is host/ip-address of server.
-- Name not used for DOMSAT data source.
-- Data source record cannot be deleted as long as 
-- any messages in storage hold it's ID.
--------------------------------------------------------------
CREATE TABLE data_source
(
	data_source_id INTEGER NOT NULL,

	-- For multi-LRGS databases, specify which LRGS this is a data source for.
	lrgs_host VARCHAR(64) NOT NULL,
	data_source_name VARCHAR(64) NOT NULL,
	data_source_type VARCHAR(24) NOT NULL,
	PRIMARY KEY(data_source_id, lrgs_host)
);

-- Guarantees no two data_source have the same ID:
-- CREATE UNIQUE INDEX data_source_IdIdx on data_source (data_source_id) ;

--CREATE UNIQUE INDEX data_source_nameIdx on data_source (data_source_name,data_source_type) ;

--------------------------------------------------------------
-- dds_period_stats
--------------------------------------------------------------
CREATE TABLE dds_period_stats
(
	start_time TIMESTAMP NOT NULL,
	lrgs_host VARCHAR(64) NOT NULL,
	period_duration CHAR NOT NULL,
	num_auth INTEGER NOT NULL,
	num_unauth INTEGER NOT NULL,
	bad_passwords INTEGER NOT NULL,
	bad_usernames INTEGER NOT NULL,
	max_clients INTEGER NOT NULL,
	min_clients INTEGER NOT NULL,
	ave_clients DOUBLE PRECISION NOT NULL,
	msgs_delivered INTEGER NOT NULL,
	PRIMARY KEY(start_time, lrgs_host)
);
-- CREATE UNIQUE INDEX start_time_IdIdx on dds_period_stats (start_time) ;

--------------------------------------------------------------
-- dds_connection
--------------------------------------------------------------
CREATE TABLE dds_connection
(
	connection_id INTEGER NOT NULL PRIMARY KEY,
	lrgs_host VARCHAR(64) NOT NULL,
	start_time TIMESTAMP,
	end_time TIMESTAMP,
	from_ip_addr VARCHAR(64),
	success_code CHAR,
	username VARCHAR(24),
	msgs_received INTEGER,
	admin_done CHAR,
	protocol_version INTEGER NOT NULL,
	last_activity TIMESTAMP
);

--------------------------------------------------------------
-- pw_history - store historical password hashes
--------------------------------------------------------------
CREATE TABLE pw_history
(
	username VARCHAR(24),
	set_time TIMESTAMP,   -- time this password was set
	pw_hash VARCHAR(128), -- Hex rep of password hash
	PRIMARY KEY(username, set_time)
);

CREATE TABLE dds_user
(
	username VARCHAR(24) NOT NULL PRIMARY KEY,
	pw_hash VARCHAR(64),
	dds_perm  CHAR,           -- Y means can login via DDS
	is_admin CHAR,            -- Y means this user is an administrator
	is_local CHAR,            -- Y means local user
	props VARCHAR(512),       -- Encoded properties
	last_modified TIMESTAMP
);

CREATE INDEX DDS_CON_END_IDX ON dds_connection(end_time);

CREATE INDEX DDS_CON_USRCOD_IDX ON dds_connection(username, success_code);


