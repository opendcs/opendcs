---------------------------------------------------------------------------
--
-- Sutron Ilex CCP Database on ORACLE
-- Database Version: 8        Date: 2013/05/02
-- Company: Sutron Corporation
--  Writer: GC
--
---------------------------------------------------------------------------
set echo on
spool setup_CCPDB_Privs.out

whenever sqlerror continue
set define on
@@defines.sql

define sys_schema     = &1
define sys_passwd     = &2
define tns_name       = &3

connect &sys_schema/&sys_passwd@&tns_name as sysdba;

alter session set current_schema = &ccp_schema;


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
      where owner = upper('&ccp_schema') and object_type in('TABLE') order by object_id
    )
  loop
    begin
      l_sqlstr := 'GRANT SELECT ON &ccp_schema..'||lower(rec.object_name)||' TO CCP_USERS._R';
      dbms_output.put_line(l_sqlstr);
      execute immediate l_sqlstr;
      l_sqlstr := 'GRANT INSERT,UPDATE,DELETE ON &ccp_schema..'||lower(rec.object_name)||' TO CCP_USERS._W';
      dbms_output.put_line(l_sqlstr);
      execute immediate l_sqlstr;
    exception
      when others then
        dbms_output.put_line('==> Cannot perform "'||l_sqlstr||'": '||sqlerrm);
        raise;
    end;
  end loop;

  -- Grant insert, update, delete privileges on few CCP tables to ccp_users_p
  begin
    l_sqlstr := 'GRANT INSERT,UPDATE,DELETE ON &ccp_schema..cp_comp_depends TO CCP_USERS._P';
    dbms_output.put_line(l_sqlstr);
    execute immediate l_sqlstr;
    l_sqlstr := 'GRANT INSERT,UPDATE,DELETE ON &ccp_schema..cp_comp_tasklist TO CCP_USERS._P';
    dbms_output.put_line(l_sqlstr);
    execute immediate l_sqlstr;
    l_sqlstr := 'GRANT INSERT,UPDATE,DELETE ON &ccp_schema..cp_comp_proc_lock TO CCP_USERS._P';
    dbms_output.put_line(l_sqlstr);
    execute immediate l_sqlstr;
  exception
    when others then
      dbms_output.put_line('==> Cannot perform "'||l_sqlstr||'": '||sqlerrm);
      raise;
  end;

  -- Grant select privilege on sequences TO ccp_users_w and ccp_users_p
  for rec in (
    select object_name from sys.all_objects
      where owner = upper('&ccp_schema') and object_type in('SEQUENCE') order by object_id
    )
  loop
    begin
      l_sqlstr := 'GRANT SELECT ON &ccp_schema..'||lower(rec.object_name)||' TO CCP_USERS._W';
      dbms_output.put_line(l_sqlstr);
      execute immediate l_sqlstr;
      l_sqlstr := 'GRANT SELECT ON &ccp_schema..'||lower(rec.object_name)||' TO CCP_USERS._P';
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
      where owner = 'PUBLIC' and table_owner = upper('&ccp_schema')
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
      where owner = upper('&ccp_schema') and object_type in('TABLE') order by object_id
    )
  loop
    l_sqlstr := 'CREATE PUBLIC SYNONYM '||lower(rec.object_name)||' FOR &ccp_schema..'||lower(rec.object_name);
    begin
      dbms_output.put_line(l_sqlstr);
      execute immediate l_sqlstr;
    exception
      when others then
        dbms_output.put_line('==> Cannot perform "'||l_sqlstr||'": '||sqlerrm);
        raise;
    end;
  end loop;

  -- Create public synonyms for CCP sequences
  /*
  for rec in (
    select object_name from sys.all_objects
      where owner = upper('&ccp_schema') and object_type in('SEQUENCE') order by object_id
    )
  loop
    l_sqlstr := 'CREATE PUBLIC SYNONYM '||lower(rec.object_name)||' FOR &ccp_schema..'||lower(rec.object_name);
    begin
      dbms_output.put_line(l_sqlstr);
      execute immediate l_sqlstr;
    exception
      when others then
        dbms_output.put_line('==> Cannot perform "'||l_sqlstr||'": '||sqlerrm);
        raise;
    end;
  end loop;
  */

END;
/

---------------------------------------------------------------------------
-- Create public synonyms for CCP sequences
---------------------------------------------------------------------------
-- used to assign record numbers for new task list entries and reset once hit the 2 billion
CREATE PUBLIC SYNONYM ccp_seq FOR &ccp_schema..ccp_seq;
CREATE PUBLIC SYNONYM cp_comp_tasklistidseq FOR &ccp_schema..cp_comp_tasklistidseq;
CREATE PUBLIC SYNONYM siteidseq FOR &ccp_schema..ccp_seq;
CREATE PUBLIC SYNONYM equipmentidseq FOR &ccp_schema..ccp_seq;
CREATE PUBLIC SYNONYM enumidseq FOR &ccp_schema..ccp_seq;
CREATE PUBLIC SYNONYM datatypeidseq FOR &ccp_schema..ccp_seq;
CREATE PUBLIC SYNONYM platformidseq FOR &ccp_schema..ccp_seq;
CREATE PUBLIC SYNONYM platformconfigidseq FOR &ccp_schema..ccp_seq;
CREATE PUBLIC SYNONYM decodesscriptidseq FOR &ccp_schema..ccp_seq;
CREATE PUBLIC SYNONYM routingspecidseq FOR &ccp_schema..ccp_seq;
CREATE PUBLIC SYNONYM datasourceidseq FOR &ccp_schema..ccp_seq;
CREATE PUBLIC SYNONYM networklistidseq FOR &ccp_schema..ccp_seq;
CREATE PUBLIC SYNONYM presentationgroupidseq FOR &ccp_schema..ccp_seq;
CREATE PUBLIC SYNONYM datapresentationidseq FOR &ccp_schema..ccp_seq;
CREATE PUBLIC SYNONYM unitconverteridseq FOR &ccp_schema..ccp_seq;
CREATE PUBLIC SYNONYM hdb_loading_applicationidseq FOR &ccp_schema..ccp_seq;
CREATE PUBLIC SYNONYM cp_algorithmidseq FOR &ccp_schema..ccp_seq;
CREATE PUBLIC SYNONYM cp_computationidseq FOR &ccp_schema..ccp_seq;
CREATE PUBLIC SYNONYM tsdb_groupidseq FOR &ccp_schema..ccp_seq;

spool off
exit;



