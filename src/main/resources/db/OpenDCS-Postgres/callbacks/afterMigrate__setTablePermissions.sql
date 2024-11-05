do $$
declare
    r record;
begin
    for r in select table_schema, table_name from information_schema.tables
             WHERE table_type = 'BASE TABLE' and table_schema = 'public'
    loop
        execute 'GRANT SELECT ON ' || quote_ident(r.table_schema) || '.'
                || quote_ident(r.table_name) || ' TO "OTSDB_USER"';
        if starts_with(upper(r.table_name), 'DCP_TRANS') OR
           starts_with(upper(r.table_name), 'TS_NUM') OR
           starts_with(upper(r.table_name), 'TS_STRING') OR
           upper(r.table_name) = 'TS_SPEC' OR
           upper(r.table_name) = 'CP_COMP_PROC_LOCK'
        then
            execute  'GRANT ALL ON TABLE ' || quote_ident(r.table_schema) || '.' || quote_ident(r.table_name) || ' TO "OTSDB_DATA_ACQ"';
        elsif upper(r.table_name) = 'CP_COMP_DEPENDS' OR
                upper(r.table_name) = 'CP_COMP_TASKLIST' OR
                upper(r.table_name) = 'CP_DEPENDS_SCRATCHPAD'
        then
            execute 'GRANT ALL ON TABLE ' || quote_ident(r.table_schema) || '.' || quote_ident(r.table_name) || ' TO "OTSDB_COMP_EXEC"';
        else
            execute 'GRANT ALL ON TABLE ' || quote_ident(r.table_schema) || '.' || quote_ident(r.table_name) || ' TO "OTSDB_MGR"';
        end if;
    end loop;

    for r in select sequence_schema, sequence_name from information_schema.Sequences
        where sequence_schema = 'public'
    loop
        EXECUTE 'GRANT USAGE, SELECT ON SEQUENCE ' || quote_ident(r.sequence_schema) || '.'
                || quote_ident(r.sequence_name) || ' TO "OTSDB_USER"';
    end loop;

END$$;
-- This isn't need for a fresh install but if you're dropping the public database
-- as part of testing it doesn't get automatically set.
grant usage on schema public to "OTSDB_USER";