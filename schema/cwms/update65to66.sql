--------------------------------------------------------------------------
-- This script updates OPENDCS 6.5 CCP Schema (corresponding to CWMS 3.1
-- to OpenDCS 6.6 (CWMS 3.2) Schema
--------------------------------------------------------------------------

-----------------------------------------------------------------------------
-- This software was written by Cove Software, LLC ("COVE") under contract 
-- to the United States Government. 
-- No warranty is provided or implied other than specific contractual terms
-- between COVE and the U.S. Government
-- 
-- Copyright 2019 U.S. Army Corps of Engineers, Hydrologic Engineering Center.
-- All rights reserved.
-----------------------------------------------------------------------------

set echo on
spool combined.log
    
whenever sqlerror continue
set define on
@@defines.sql

@@alarm.sql

begin
  for rec in (
    select table_name from user_tab_columns where column_name in ('DB_OFFICE_CODE')
    )
  loop
    execute immediate 'alter table ' || rec.table_name || ' modify db_office_code default &&dflt_office_code';
  end loop;
endl;

-----------------------------------------------------------------
-- Finally, update the database version numbers in the database
-----------------------------------------------------------------
delete from DecodesDatabaseVersion;
insert into DecodesDatabaseVersion values(17, 'Updated to OpenDCS 6.6');
delete from tsdb_database_version;
insert into tsdb_database_version values(17, 'Updated to OpenDCS 6.6');




spool off
exit;
