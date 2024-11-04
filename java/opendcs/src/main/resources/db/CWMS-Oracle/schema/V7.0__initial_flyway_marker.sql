

create table dcp_trans_day_map
(
	table_suffix varchar2(4) not null,
	-- day 0 = jan 1, 1970. null means this suffix not used.
	day_number int,
	primary key (table_suffix)
) ${TABLE_SPACE_SPEC};

COMMENT ON COLUMN DCP_TRANS_DAY_MAP.DAY_NUMBER IS 'Day 0 = Jan 1, 1970. Null means this suffix not used.';
create or replace public synonym dcp_trans_day_map for ccp.dcp_trans_day_map;

declare
	i int;
	suffix varchar2(4);
    stmt varchar2(4000);
begin
    -- Create Tables 
	for i in 1..31
	loop
		suffix := to_char(i,'fm09');
		stmt := 'CREATE TABLE DCP_TRANS_DATA_' || suffix || q'[(
			RECORD_ID NUMBER(18) NOT NULL,
			BLOCK_NUM INT NOT NULL,
			MSG_DATA VARCHAR2(4000) NOT NULL,
			PRIMARY KEY (RECORD_ID, BLOCK_NUM)
		) ${TABLE_SPACE_SPEC}]';
        
        execute immediate stmt;
        
		stmt := 'CREATE TABLE DCP_TRANS_' || suffix || q'[(
			RECORD_ID NUMBER(18) NOT NULL,
			-- ''G'' = GOES, ''L'' = Data Logger, ''I'' = Iridium.
			-- This field determines how the header should be parsed.
			MEDIUM_TYPE VARCHAR2(1) NOT NULL,
			MEDIUM_ID VARCHAR2(64) NOT NULL,
			LOCAL_RECV_TIME NUMBER(19) NOT NULL,
			TRANSMIT_TIME NUMBER(19) NOT NULL,
			FAILURE_CODES VARCHAR2(8) NOT NULL,
			-- Second of day when the transmit window started
			WINDOW_START_SOD INT,
			-- Transmit window length in seconds
			WINDOW_LENGTH INT,
			XMIT_INTERVAL INT,
			CARRIER_START NUMBER(19),
			CARRIER_STOP NUMBER(19),
			FLAGS INT NOT NULL,
			CHANNEL INT NOT NULL,
			BATTERY FLOAT,
			-- Total message length, determines number of additional blocks
			-- required to store message.
			MSG_LENGTH INT NOT NULL,
			-- First block of data. Very long messages will have additional blocks.
			MSG_DATA VARCHAR2(4000) NOT NULL,
			PRIMARY KEY (RECORD_ID)
		) ]';
        --dbms_output.put_line('Creating ' || stmt);
        execute immediate stmt;		
        stmt := 'ALTER TABLE DCP_TRANS_DATA_' || suffix 
             || ' ADD CONSTRAINT DCP_TRANS_DATA_FK_' || suffix
             || ' FOREIGN KEY (RECORD_ID)'
             || ' REFERENCES DCP_TRANS_' || suffix || '(RECORD_ID)';
        execute immediate stmt;

		execute immediate 'CREATE SEQUENCE DCP_TRANS_' || suffix || 'IDSEQ';

		insert into dcp_trans_day_map(table_suffix,day_number) values(suffix, null);
        execute immediate 'create or replace public synonym dcp_trans_data_' || suffix || ' for ccp.dcp_trans_data_' || suffix;
        execute immediate 'create or replace public synonym dcp_trans_' || suffix || ' for ccp.dcp_trans_' || suffix;
		/* Create Indexes */

		execute immediate 'CREATE INDEX DCP_TRANS_DATA_REC_IDX_' || suffix || ' ON DCP_TRANS_DATA_' || suffix || '(RECORD_ID)';
		execute immediate 'CREATE INDEX DCP_TRANS_ADDR_IDX_' || suffix || ' ON DCP_TRANS_' || suffix || '(MEDIUM_TYPE, MEDIUM_ID) ';
		-- Used for GOES channel expansion in DCP Monitor
		execute immediate 'CREATE INDEX DCP_TRANS_CHAN_IDX_' || suffix || ' ON DCP_TRANS_' || suffix || '(CHANNEL)';
		execute immediate 'CREATE INDEX DCP_TRANS_MEDIUM_TYPE_' || suffix || ' ON DCP_TRANS_' || suffix || '(MEDIUM_TYPE)';

		/* Comments */

		execute immediate 'COMMENT ON COLUMN DCP_TRANS_' || suffix || '.MEDIUM_TYPE IS '' G = GOES, L = Data Logger, I = Iridium. ' ||
				'This field determines how the header should be parsed.''';
		execute immediate 'COMMENT ON COLUMN DCP_TRANS_' || suffix || '.WINDOW_START_SOD IS ''Second of day when the transmit window started''';
		execute immediate 'COMMENT ON COLUMN DCP_TRANS_' || suffix || '.WINDOW_LENGTH IS ''Transmit window length in seconds''';
		execute immediate 'COMMENT ON COLUMN DCP_TRANS_' || suffix || '.MSG_LENGTH IS ''Total message length, determines number of additional blocks' ||
				'required to store message.''';
		execute immediate 'COMMENT ON COLUMN DCP_TRANS_' || suffix || '.MSG_DATA IS ''First block of data. Very long messages will have additional blocks.''';
	end loop;
end;
/

-- Long term these should go away in favor of the 
-- built in flyway version table.
-- however too many internal locations relay on them so removal will happen in 8.0
delete from DecodesDatabaseVersion;
insert into DecodesDatabaseVersion values(70, 'Additional information is available in the ''flyway_schema_history'' table.');
delete from tsdb_database_version;
insert into tsdb_database_version values(70, 'Additional information is available in the ''flyway_schema_history'' table.');