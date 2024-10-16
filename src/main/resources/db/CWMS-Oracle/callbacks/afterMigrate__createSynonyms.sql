/*
create or replace public synonym platformidseq for ccp.platformidseq;
create or replace public synonym cp_algorithmidseq for ccp.cp_algorithmidseq;
create or replace public synonym cp_computationidseq for ccp.cp_computationidseq;
create or replace public synonym datapresentationidseq for ccp.datapresentationidseq;
create or replace public synonym datasourceidseq for ccp.datasourceidseq;
create or replace public synonym datatypeidseq for ccp.datatypeidseq;
create or replace public synonym decodesscriptidseq for ccp.decodesscriptidseq;
create or replace public synonym enumidseq for ccp.enumidseq;
create or replace public synonym equipmentidseq for ccp.equipmentidseq;
create or replace public synonym hdb_loading_applicationidseq for ccp.hdb_loading_applicationidseq;
create or replace public synonym networklistidseq for ccp.networklistidseq;
create or replace public synonym platformconfigidseq for ccp.platformconfigidseq;
create or replace public synonym presentationgroupidseq for ccp.presentationgroupidseq;
create or replace public synonym routingspecidseq for ccp.routingspecidseq;
create or replace public synonym schedule_entryidseq for ccp.schedule_entryidseq;
create or replace public synonym tsdb_groupidseq for ccp.tsdb_groupidseq;
create or replace public synonym unitconverteridseq for ccp.unitconverteridseq;
*/
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