--------------------------------------------------------------------------
-- This script updates CP tables from HDB 6.3 CCP Schema to OpenDCS 6.4 Schema.
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
-- Copyright 2016 U.S. Government.
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

alter table cp_algorithm_archive modify exec_class varchar2(240);

alter table cp_algorithm_archive modify algorithm_name varchar2(64);

spool off
exit;
