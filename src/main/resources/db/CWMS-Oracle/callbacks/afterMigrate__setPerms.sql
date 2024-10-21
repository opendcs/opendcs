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
---------------------------------------------------------------------------
-- Grant privileges on CCP objects to cwms_user
---------------------------------------------------------------------------
DECLARE
  l_sqlstr   varchar2(200);
BEGIN
  -- Grant select privilege on all CCP tables to ccp_users_r, and
  -- grant insert, update, delete privileges on all CCP tables to ccp_users_w
  for rec in (
    select object_name from sys.all_objects
      where owner = upper('${CCP_SCHEMA}') and object_type in('TABLE') 
      and upper(object_name) not like 'FLYWAY%' order by object_id
    )
  loop
    begin
      l_sqlstr := 'GRANT SELECT,INSERT,UPDATE,DELETE ON ${CCP_SCHEMA}.'||lower(rec.object_name)||' TO CCP_USERS';
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
      where owner = upper('${CCP_SCHEMA}') and object_type in('SEQUENCE')
        and upper(object_name) not like 'FLYWAY%' order by object_id
    )
  loop
    begin
      l_sqlstr := 'GRANT SELECT ON ${CCP_SCHEMA}.'||lower(rec.object_name)||' TO CCP_USERS';
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

GRANT SELECT ON DACQ_EVENTIDSEQ TO CCP_USERS;
GRANT SELECT ON SCHEDULE_ENTRY_STATUSIDSEQ TO CCP_USERS;
GRANT SELECT ON CP_DEPENDS_NOTIFYIDSEQ TO CCP_USERS;
grant select on ccp.unitconverteridseq to ccp_users;
grant select on ccp.tsdb_groupidseq to ccp_users;
grant select on ccp.schedule_entryidseq to ccp_users;
grant select on ccp.routingspecidseq to ccp_users;
grant select on ccp.presentationgroupidseq to ccp_users;
grant select on ccp.platformconfigidseq to ccp_users;
grant select on ccp.networklistidseq to ccp_users;
grant select on ccp.hdb_loading_applicationidseq to ccp_users;
grant select on ccp.equipmentidseq to ccp_users;
grant select on ccp.enumidseq to ccp_users;
grant select on ccp.decodesscriptidseq to ccp_users;
grant select on ccp.datatypeidseq to ccp_users;
grant select on ccp.datasourceidseq to ccp_users;
grant select on ccp.datapresentationidseq to ccp_users;
grant select on ccp.cp_computationidseq to ccp_users;
grant select on ccp.cp_algorithmidseq to ccp_users;
grant select on ccp.platformidseq to ccp_users;

---------------------------------------------------------------------------
-- Grant execute privilege on packages TO cwms_user
---------------------------------------------------------------------------
GRANT EXECUTE ON ${CCP_SCHEMA}.cwms_ccp_vpd TO CCP_USERS;
---------------------------------------------------------------------------
-- Grant execute privilege on packages TO cwms_user
---------------------------------------------------------------------------
GRANT EXECUTE ON ${CCP_SCHEMA}.cwms_ccp TO CCP_USERS;
