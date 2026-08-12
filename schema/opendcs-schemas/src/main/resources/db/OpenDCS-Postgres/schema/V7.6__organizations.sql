create table org_type (
    id bigint generated always as identity (start with 1) primary key,
    name varchar(64) not null unique,
    description text not null
);

create table organization (
    id bigint generated always as identity (start with 1) primary key,
    name varchar(128) not null unique,
    org_type bigint references org_type(id),
    parent_id bigint references organization(id)
);


insert into org_type(id, name, description) OVERRIDING SYSTEM VALUE
    values(0, '<default>', 'Basic organization with not distinct attributes');
insert into organization(id, name, org_type, parent_id) OVERRIDING SYSTEM VALUE
    values (0, 'Default', 0, null);

-- Create the initial definition of the method. Full Implementation is in the 
-- repeatable functions section.
create function current_organization() returns bigint as $$
begin
    return 0;
end;
$$ language plpgsql stable;

-- we don't need to set the org for below as the default implementation above always returns the default org.

-- Loop through everything and add the colunmn and policies
do $$
declare
 r record;
begin
    for r in (select *
                from information_schema.tables
               where table_schema = 'public'   -- These tables are "global"
                 and lower(table_name) not in ('org_type', 'opendcs_user', 'opendcs_user_password',
                                               'opendcs_role', 'identity_provider', 'user_identity_provider',
                                               'organization', 'flyway_schema_history', 'tsdb_database_version',
                                               'decodesdatabaseversion')) loop
        execute format('alter table %I add column org_id bigint not null default current_organization()', r.table_name);
        execute format('alter table %I enable row level security', r.table_name);
        execute format('drop policy if exists org_read_isolation on %I', r.table_name);
        execute format('drop policy if exists org_insert_isolation on %I', r.table_name);
        execute format('drop policy if exists org_update_isolation on %I', r.table_name);
        execute format('drop policy if exists org_delete_isolation on %I', r.table_name);
        execute format('create policy org_read_isolation on %I for select using (org_id = 0 or org_id = current_organization())',
                       r.table_name);
        execute format('create policy org_delete_isolation on %I for delete using (org_id = current_organization())',
                       r.table_name);
        execute format('create policy org_insert_isolation on %I for insert with check (org_id = current_organization())',
                       r.table_name);
        execute format('create policy org_update_isolation on %I for insert with check (org_id = current_organization())',
                       r.table_name);
        --raise notice '%', r.table_name;
    end loop; 
end $$;

--raise notice 'The following will wait for indexes to be constructed. It is possible there could be some long waits.';
-- it would be nice to just loop through the data dictionary but given the various
-- unique contraints that have been built over time it would be difficult to get right
-- additonal instead of plain unique constraints, primary key constraints were (reasonably) used.
-- While correct, it does make the sequence of events a bit more difficult.

create unique index /*concurrently*/ configsensor_pkey_idx on configsensor(org_id, configid, sensornumber);
--begin;

alter table configsensorproperty drop CONSTRAINT configsensorproperty_configid_sensornumber_fkey;
alter table configsensordatatype drop constraint configsensordatatype_configid_sensornumber_fkey;

alter table configsensor drop constraint configsensor_pkey,
    add constraint configsensor_pkey primary key using index configsensor_pkey_idx;

alter table configsensorproperty
	add foreign key (org_id, configid, sensornumber)
	references configsensor (org_id, configid, sensornumber)
    on update restrict
    on delete restrict
;

alter table configsensordatatype
	add foreign key (org_id, configid, sensornumber)
	references configsensor (org_id, configid, sensornumber)
    on update restrict
    on delete restrict
;

--end;