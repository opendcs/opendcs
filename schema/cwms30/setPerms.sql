-----------------------------------------------------------------------------
-- This software was written by Cove Software, LLC ("COVE") under contract 
-- to the United States Government. 
-- No warranty is provided or implied other than specific contractual terms
-- between COVE and the U.S. Government
-- 
-- Copyright 2014 U.S. Army Corps of Engineers, Hydrologic Engineering Center.
-- All rights reserved.
-----------------------------------------------------------------------------
---------------------------------------------------------------------------
-- Set Permissions for CCP Database on ORACLE
-- Maintainer: Cove Software, LLC
---------------------------------------------------------------------------
set echo on
spool setPerms.log

whenever sqlerror continue
set define on
@@defines.sql

---------------------------------------------------------------------------
-- Grant privileges on CCP objects to cwms_user
---------------------------------------------------------------------------
set serveroutput on size 50000

DECLARE
  l_sqlstr   varchar2(200);
BEGIN
  -- Grant select privilege on all CCP tables to ccp_users_r, and
  -- grant insert, update, delete privileges on all CCP tables to ccp_users_w
  for rec in (
    select object_name from sys.all_objects
      where owner = upper('&CCP_SCHEMA') and object_type in('TABLE') order by object_id
    )
  loop
    begin
      l_sqlstr := 'GRANT SELECT,INSERT,UPDATE,DELETE ON &CCP_SCHEMA..'||lower(rec.object_name)||' TO CCP_USERS';
      dbms_output.put_line(l_sqlstr);
      execute immediate l_sqlstr;
    exception
      when others then
        dbms_output.put_line('==> Cannot perform "'||l_sqlstr||'": '||sqlerrm);
        raise;
    end;
  end loop;

  -- Grant select privilege on sequences TO ccp_users_w and ccp_users_p
  for rec in (
    select object_name from sys.all_objects
      where owner = upper('&CCP_SCHEMA') and object_type in('SEQUENCE') order by object_id
    )
  loop
    begin
      l_sqlstr := 'GRANT SELECT ON &CCP_SCHEMA..'||lower(rec.object_name)||' TO CCP_USERS';
      dbms_output.put_line(l_sqlstr);
      execute immediate l_sqlstr;
    exception
      when others then
        dbms_output.put_line('==> Cannot perform "'||l_sqlstr||'": '||sqlerrm);
        raise;
    end;
  end loop;

END;
/

---------------------------------------------------------------------------
-- Drop public synonyms for CCP objects
---------------------------------------------------------------------------
DECLARE
  l_sqlstr   varchar2(200);
BEGIN
  -- Drop public synonyms for CCP tables and sequences
  for rec in (
    select synonym_name from sys.all_synonyms
      where owner = 'PUBLIC' and table_owner = upper('&CCP_SCHEMA')
        and synonym_name not in('CWMS_CCP','CWMS_CCP_VPD') order by synonym_name
    )
  loop
    l_sqlstr := 'DROP PUBLIC SYNONYM '||lower(rec.synonym_name);
    begin
      dbms_output.put_line(l_sqlstr);
      execute immediate l_sqlstr;
    exception
      when others then
        dbms_output.put_line('==> Cannot perform "'||l_sqlstr||'": '||sqlerrm);
        raise;
    end;
  end loop;

END;
/

---------------------------------------------------------------------------
-- Create public synonyms for CCP objects
---------------------------------------------------------------------------
DECLARE
  l_sqlstr   varchar2(200);
BEGIN
  -- Create public synonyms for CCP tables
  for rec in (
    select object_name from sys.all_objects
      where owner = upper('&CCP_SCHEMA') and object_type in('TABLE') order by object_id
    )
  loop
    l_sqlstr := 'CREATE PUBLIC SYNONYM '||lower(rec.object_name)||' FOR &CCP_SCHEMA..'||lower(rec.object_name);
    begin
      dbms_output.put_line(l_sqlstr);
      execute immediate l_sqlstr;
    exception
      when others then
        dbms_output.put_line('==> Cannot perform "'||l_sqlstr||'": '||sqlerrm);
        raise;
    end;
  end loop;

END;
/

---------------------------------------------------------------------------
-- Create public synonyms for CCP sequences
---------------------------------------------------------------------------
-- used to assign record numbers for new task list entries and reset once hit the 2 billion
CREATE PUBLIC SYNONYM cp_comp_tasklistidseq FOR &CCP_SCHEMA..cp_comp_tasklistidseq;
CREATE PUBLIC SYNONYM DACQ_EVENTIDSEQ FOR &CCP_SCHEMA..DACQ_EVENTIDSEQ;
CREATE PUBLIC SYNONYM SCHEDULE_ENTRY_STATUSIDSEQ FOR &CCP_SCHEMA..SCHEDULE_ENTRY_STATUSIDSEQ;
CREATE PUBLIC SYNONYM CP_DEPENDS_NOTIFYIDSEQ FOR &CCP_SCHEMA..CP_DEPENDS_NOTIFYIDSEQ;


GRANT SELECT ON DACQ_EVENTIDSEQ TO CCP_USERS;
GRANT SELECT ON SCHEDULE_ENTRY_STATUSIDSEQ TO CCP_USERS;
GRANT SELECT ON CP_DEPENDS_NOTIFYIDSEQ TO CCP_USERS;

spool off
exit;

