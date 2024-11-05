-----------------------------------------------------------------------------
-- This software was written by Cove Software, LLC ("COVE") under contract 
-- to the United States Government. 
-- No warranty is provided or implied other than specific contractual terms
-- between COVE and the U.S. Government
-- 
-- Copyright 2014 U.S. Army Corps of Engineers, Hydrologic Engineering Center.
-- All rights reserved.
-----------------------------------------------------------------------------
-- 
-- Sequences for CWMS CCP Tables
--
drop sequence cp_comp_tasklistidseq;
create sequence cp_comp_tasklistidseq minvalue 1 start with 1 maxvalue 2000000000 nocache cycle;
drop SEQUENCE SCHEDULE_ENTRY_STATUSIDSEQ;
CREATE SEQUENCE SCHEDULE_ENTRY_STATUSIDSEQ MINVALUE 1 START WITH 1 MAXVALUE 2000000000 NOCACHE CYCLE;
drop SEQUENCE CP_DEPENDS_NOTIFYIDSEQ;
CREATE SEQUENCE CP_DEPENDS_NOTIFYIDSEQ MINVALUE 1 START WITH 1 MAXVALUE 2000000000 NOCACHE CYCLE;
drop SEQUENCE DACQ_EVENTIDSEQ;
CREATE SEQUENCE DACQ_EVENTIDSEQ MINVALUE 1 START WITH 1 NOCACHE;
