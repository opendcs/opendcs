create table lrgs_data_source_type (
    id bigserial not null,
    name varchar(256),
    java_class varchar(512),
    created_at timestamptz not null default now(),
    updated_at timestamptz not null,
    primary key (id),
    unique (name)

);

-- instead of "data source" we'll just have "data interfaces"
-- Those could be input, output, or both
create table lrgs_data_interface (
	id bigserial not null,	
	data_source_name varchar(64) NOT NULL,
	data_source_type_id bigint references lrgs_data_source_type(id),
    config jsonb default '{}'::jsonb,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null
	primary key(id),
    unique (data_source_name)
);

create table lrgs_config (
    archive_length_days int not null default 30,

    archive_type text not null default 'database-postgres', -- default to this database
    archive_config jsonb default '{}'::jsonb
)

create function purge_archive()
returns trigger as $$
begin
    return new; -- re implementation will be in repeatable
end;
$$ language plpgsql;

create table lrgs_archive (
    -- okay this needs some thought
    created_at timestamptz not null default now(),
    data_source_id bigint references lrgs_data_source(id) not null,
    bytea raw_data, -- null allowed here, we may get literal nothing from a source

);

create trigger trigger_purge_archive()
after insert on lrgs_archive
for each statement
execute function purge_archive();