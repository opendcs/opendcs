-- These are the sequences used in Postgres for generating
-- new surrogate keys.
--
-- This file contains definitions for the SEQUENCES used to
-- generate surrogate keys. Do not execute this file if your
-- database uses some other mechanism to generate surrogate
-- keys.

-- Used to assign IDs for new system_outage/domsat_gap/damsnt_outage:
-- MJM OpenDCS 6.2 does not support Outage Recovery
-- CREATE SEQUENCE OutageIdSeq ;

-- Used to assign IDs to new data_source:
CREATE SEQUENCE Data_sourceIdSeq ;

-- Used to assign IDs to new dds_connection:
CREATE SEQUENCE ConnectionIdSeq MINVALUE 0 MAXVALUE 2000000000 CYCLE;
