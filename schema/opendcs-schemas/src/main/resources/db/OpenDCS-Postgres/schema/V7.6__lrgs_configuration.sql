create table lrgs_data_interface_type (
    id bigserial not null primary key,
    name text unique,
    java_class text,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);

-- instead of "data source" we'll just have "data interfaces"
-- Those could be input, output, or both

create table lrgs_data_interface (
	id bigserial not null primary key,	
	name varchar(64) not null unique,
	type_id bigint references lrgs_data_interface_type(id),
    config jsonb default '{}'::jsonb,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);


insert into lrgs_data_interface_type(name, java_class) 
    values ('archive-database', 'org.opendcs.lrgs.archive.DatabaseArchive');

insert into lrgs_data_interface(name, type_id, config) 
    values ('LrgsArchive', (select id from lrgs_data_interface_type where name = 'archive-database'),
            '{"length_days": 30 }'::jsonb)
;

create function purge_archive()
returns trigger as $$
begin
    return new; -- re implementation will be in repeatable
end;
$$ language plpgsql;

create table lrgs_archive (
    -- okay this needs some thought
    created_at timestamptz not null default now(),
    data_interface_id bigint references lrgs_data_interface(id) not null,
    identifier text, -- arbitrary, text encoded, identifier as used by the given source
    raw_data bytea, -- null allowed here, we may get literal nothing from a source

    unique (created_at, data_interface_id, identifier)
);

create trigger trigger_purge_archive
after insert on lrgs_archive
for each statement
execute function purge_archive();