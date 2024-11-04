---------------------------------------------------------------------------
-- Create public synonyms for CCP objects
---------------------------------------------------------------------------
DECLARE
  l_sqlstr   varchar2(200);
BEGIN
  -- Create public synonyms for CCP tables and sequences
  for rec in (
    select object_name from sys.all_objects
      where owner = upper('${CCP_SCHEMA}') and object_type in('TABLE', 'SEQUENCE') order by object_id
    )
  loop
    l_sqlstr := 'CREATE OR REPLACE PUBLIC SYNONYM '||lower(rec.object_name)||' FOR ${CCP_SCHEMA}.'||lower(rec.object_name);
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
-- Create synonym for CWMS_CCP_VPD package
---------------------------------------------------------------------------
CREATE OR REPLACE PUBLIC SYNONYM cwms_ccp_vpd FOR ${CCP_SCHEMA}.cwms_ccp_vpd;
CREATE OR REPLACE PUBLIC SYNONYM cwms_ccp FOR ${CCP_SCHEMA}.cwms_ccp;
