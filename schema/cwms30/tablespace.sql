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
  DATAFILE '&TBL_SPACE_DIR/&TBL_SPACE_DATA.dbf'
  SIZE 100M REUSE AUTOEXTEND ON NEXT 1M MAXSIZE 2000M
;

DROP TABLESPACE &TBL_SPACE_TEMP
   INCLUDING CONTENTS AND DATAFILES CASCADE CONSTRAINTS;
CREATE TEMPORARY TABLESPACE &TBL_SPACE_TEMP
  TEMPFILE '&TBL_SPACE_DIR/&TBL_SPACE_TEMP.dbf'
  SIZE 200M REUSE AUTOEXTEND ON NEXT 1M MAXSIZE UNLIMITED
;

spool off
exit;
