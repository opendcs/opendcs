DECLARE
  l_sqlstr   varchar2(200);
BEGIN
  for rec in (
    select object_name,owner 
      from sys.all_objects
     where owner = upper('${TSDB_ADM_SCHEMA}') and object_type in('TABLE', 'SEQUENCE')
     order by object_id asc
    )
  loop
    l_sqlstr := 'CREATE OR REPLACE PUBLIC SYNONYM ' || lower(rec.object_name)|| ' FOR '
             || lower(rec.owner) || '.' || lower(rec.object_name);
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
