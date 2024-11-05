-----------------------------------------------------------------------------
-- This software was written by Cove Software, LLC ("COVE") under contract 
-- to the United States Government. 
-- No warranty is provided or implied other than specific contractual terms
-- between COVE and the U.S. Government
-- 
-- Copyright 2014 U.S. Army Corps of Engineers, Hydrologic Engineering Center.
-- All rights reserved.
-----------------------------------------------------------------------------
--------------------------------------------------------------------------------
-- Create tablespaces for CCP CWMS 3.0 
--
-- Cove Software, LLC
--------------------------------------------------------------------------------
set echo on
spool tablespace.log

whenever sqlerror continue
set define on
@@defines.sql

DROP TABLESPACE &TBL_SPACE_DATA
   INCLUDING CONTENTS AND DATAFILES CASCADE CONSTRAINTS;
CREATE TABLESPACE &TBL_SPACE_DATA
  DATAFILE '&TBL_SPACE_DIR/&TBL_SPACE_DATA..dbf'
  SIZE 100M REUSE AUTOEXTEND ON NEXT 1M MAXSIZE 2000M
;

spool off
exit;
