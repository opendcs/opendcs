/* Create Tables */

CREATE TABLE DCP_TRANS_DATA_<SUFFIX>
(
	RECORD_ID BIGINT NOT NULL,
	BLOCK_NUM INT NOT NULL,
	MSG_DATA VARCHAR(4000) NOT NULL,
	PRIMARY KEY (RECORD_ID, BLOCK_NUM)
) WITHOUT OIDS;


CREATE TABLE DCP_TRANS_<SUFFIX>
(
	RECORD_ID BIGINT NOT NULL UNIQUE,
	-- 'G' = GOES, 'L' = Data Logger, 'I' = Iridium.
	-- This field determines how the header should be parsed.
	MEDIUM_TYPE VARCHAR(1) NOT NULL,
	MEDIUM_ID VARCHAR(64) NOT NULL,
	LOCAL_RECV_TIME BIGINT NOT NULL,
	TRANSMIT_TIME BIGINT NOT NULL,
	FAILURE_CODES VARCHAR(8) NOT NULL,
	-- Second of day when the transmit window started
	WINDOW_START_SOD INT,
	-- Transmit window length in seconds
	WINDOW_LENGTH INT,
	XMIT_INTERVAL INT,
	CARRIER_START BIGINT,
	CARRIER_STOP BIGINT,
	FLAGS INT NOT NULL,
	CHANNEL INT NOT NULL,
	BATTERY FLOAT,
	-- Total message length, determines number of additional blocks
	-- required to store message.
	MSG_LENGTH INT NOT NULL,
	-- First block of data. Very long messages will have additional blocks.
	MSG_DATA VARCHAR(4000) NOT NULL,
	PRIMARY KEY (RECORD_ID)
) WITHOUT OIDS;



/* Create Foreign Keys */

ALTER TABLE DCP_TRANS_DATA_<SUFFIX>
	ADD FOREIGN KEY (RECORD_ID)
	REFERENCES DCP_TRANS_<SUFFIX> (RECORD_ID)
	ON UPDATE RESTRICT
	ON DELETE RESTRICT
;

/* Create Indexes */

CREATE INDEX DCP_TRANS_DATA_REC_IDX_<SUFFIX> ON DCP_TRANS_DATA_<SUFFIX> USING BTREE (RECORD_ID);
CREATE INDEX DCP_TRANS_ADDR_IDX_<SUFFIX> ON DCP_TRANS_<SUFFIX> USING BTREE (MEDIUM_TYPE, MEDIUM_ID);
-- Used for GOES channel expansion in DCP Monitor
CREATE INDEX DCP_TRANS_CHAN_IDX_<SUFFIX> ON DCP_TRANS_<SUFFIX> USING BTREE (CHANNEL);
CREATE INDEX DCP_TRANS_MEDIUM_TYPE_<SUFFIX> ON DCP_TRANS_<SUFFIX> USING BTREE (MEDIUM_TYPE);



/* Comments */

COMMENT ON COLUMN DCP_TRANS_<SUFFIX>.MEDIUM_TYPE IS '''G'' = GOES, ''L'' = Data Logger, ''I'' = Iridium.
This field determines how the header should be parsed.';
COMMENT ON COLUMN DCP_TRANS_<SUFFIX>.WINDOW_START_SOD IS 'Second of day when the transmit window started';
COMMENT ON COLUMN DCP_TRANS_<SUFFIX>.WINDOW_LENGTH IS 'Transmit window length in seconds';
COMMENT ON COLUMN DCP_TRANS_<SUFFIX>.MSG_LENGTH IS 'Total message length, determines number of additional blocks
required to store message.';
COMMENT ON COLUMN DCP_TRANS_<SUFFIX>.MSG_DATA IS 'First block of data. Very long messages will have additional blocks.';



