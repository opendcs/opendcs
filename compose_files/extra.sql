
-- NOTE: when other code that will expect that to be OpenDCS-Postgres
-- Is merged in this needs to be updated... or removed, the migration should
-- really do this.
merge into tsdb_property p
using (select 'editDatabaseType' as name) prop
on (p.prop_name = prop.name)
when matched then
    update set prop_value = 'OPENTSDB'
when not matched then
    insert (prop_name, prop_value) values (prop.name, 'OPENTSDB')
;