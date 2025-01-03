declare
    l_sqlstr varchar2(500);
begin
    for rec in (select object_name,owner from sys.all_objects
                 where owner = upper('${TSDB_ADM_SCHEMA}')
                   and object_type in('TABLE')
                   and upper(object_name) not like 'FLYWAY%' order by object_id)
    loop
        l_sqlstr := 'grant select on "' || rec.owner || '"."' || rec.object_name || '" to "OTSDB_USER"';
        execute immediate l_sqlstr;
        if regexp_like(upper(rec.object_name),'^DCP_TRANS.*') OR
           regexp_like(upper(rec.object_name),'^TS_NUM.*') OR
           regexp_like(upper(rec.object_name),'^TS_STRING.*') OR
           upper(rec.object_name) = 'TS_SPEC' OR
           upper(rec.object_name) = 'CP_COMP_PROC_LOCK'
        then
            l_sqlstr := 'grant all on "' || rec.owner || '"."' || rec.object_name || '" TO "OTSDB_DATA_ACQ"';
        elsif upper(rec.object_name) = 'CP_COMP_DEPENDS' OR
              upper(rec.object_name) = 'CP_COMP_TAKSLIST' OR
              upper(rec.object_name) = 'CP_DEPENDS_SCRATCHPAD'
        then
            l_sqlstr := 'grant all on "' || rec.owner || '"."' || rec.object_name || '" TO "OTSDB_COMP_EXEC"';
        else
            l_sqlstr := 'grant all on "' || rec.owner || '"."' || rec.object_name || '" TO "OTSDB_MGR"';
        end if;
        dbms_output.put_line('Running ' || l_sqlstr);
        execute immediate l_sqlstr;
    end loop;

    for rec in (
        select object_name,owner from sys.all_objects
         where owner = upper('${TSDB_ADM_SCHEMA}')
           and object_type in ('SEQUENCE')
           and upper(object_name) not like 'FLYWAY%' order by object_id
        )
    loop
        l_sqlstr := 'GRANT SELECT ON "' || rec.owner || '"."' || rec.object_name || '" TO "OTSDB_USER"';
        execute immediate l_sqlstr;
    end loop;

exception
when others then
    raise_application_error (-20000, 'From: ' || l_sqlstr || ' - ' || sqlerrm || chr(10) || dbms_utility.format_error_backtrace);
end;
/
