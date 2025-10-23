create table identity_provider (
    id bigserial primary key,
    name varchar(256) not null unique,
    type varchar(256) not null,
    config jsonb default '{}'::jsonb,
    updated_at timestamptz not null --- TODO make trigger
);

create table opendcs_user(
    id bigserial primary key,
    preferred_name_provider bigint references identity_provider(id),
    email text not null unique,
    preferences jsonb default '{}'::jsonb,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null --- TODO make trigger
);

create table opendcs_user_password(
    user_id bigint references opendcs_user(id) not null,
    password text,
    updated_at timestamptz not null --- TODO make trigger
);

create table opendcs_role(
    id bigserial primary key,
    name varchar(128) not null unique,
    description text,
    updated_at timestamptz not null --- TODO make trigger
);

create table user_roles(
    user_id bigint references opendcs_user(id) not null,
    role_id bigint references opendcs_role(id) not null,
    primary key (user_id, role_id)
);


create table user_identity_provider(
    user_id bigint references opendcs_user(id) not null,
    subject text not null unique,
    identity_provider_id bigint references identity_provider(id) not null,
    primary key (user_id, identity_provider_id),
    created_at timestamptz not null default now()
);