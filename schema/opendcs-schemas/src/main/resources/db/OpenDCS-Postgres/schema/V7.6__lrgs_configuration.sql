create table data_source_type (
    id bigserial not null,
    name varchar(256),
    java_class varchar(512),
    created_at timestamptz not null default now(),
    updated_at timestamptz not null,
    primary key (id),
    unique (name)

);


create table data_source
(
	id bigserial not null,	
	data_source_name varchar(64) NOT NULL,
	data_source_type_id integer references (data_source_type.id),
    config jsonb default '{}'::jsonb,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null
	primary key(id),
    unique (data_source_name)
);


create table archive (
    -- okay this needs some thought
)