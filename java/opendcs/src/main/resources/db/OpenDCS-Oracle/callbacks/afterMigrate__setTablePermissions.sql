
begin
    l_sqlstr varchar2(500);
    for rec in (select object_name from sys.all_object 
                 where owner = upper("${TSDB_ADM_SCHEMA}")
                   and object_type in('TABLE')
                   and upper(object_name) not like 'FLYWAY%' order by object_id)
    loop
        l_sqlstr := 'grant select on ' || rec.object_name || ' to "OTSDB_USER"';
        execute immediate l_sqlstr;
        if starts_with(upper(rec.object_name),'DCP_TRANS') OR
           starts_with(upper(rec.object_name),'TS_NUM') OR
           starts_with(upper(rec.object_name),'TS_STRING') OR
           upper(rec.object_name) = 'TS_SPEC' OR
           upper(rec.object_name) = 'CP_COMP_PROC_LOCK'
        then
            l_sqlstr := 'grant all on table ' || rec.object_name || ' TO "OTSDB_DATA_ACQ"';
            execute immediate l_sqlstr;
        elsif upper(rec.object_name) = 'CP_COMP_DEPENDS' OR
              upper(rec.object_name) = 'CP_COMP_TAKSLIST' OR
              upper(rec.object_name) = 'CP_DEPENDS_SCRATCHPAD'
        then
            l_sqlstr := 'grant all on table ' || rec.object_name || ' TO "OTSDB_COMP_EXEC"';
            execute immediate l_sqlstr;
        else
            l_sqlstr := 'grant all on table ' || rec.object_name || ' TO "OTSDB_MGR"';
        end if;
    end loop;

    for rec in (
        select object_name from sys.all_objects
         where owner = upper("${TSDB_ADM_SCHEMA}")
           and object_type in ('SEQUENCE')
           and upper(object_name) not like 'FLYWAY%' order by object_id
        )
    loop
        l_sqlstr := 'GRANT SELECT ON ${TSDB_ADM_SCHEMA}.' || rec.object_name || 'TO "OTSDB_USER"';
        execute immediate l_sqlstr;
    end loop;


end;
/
