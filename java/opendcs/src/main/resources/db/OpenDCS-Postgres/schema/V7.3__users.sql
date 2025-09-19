create table opendcs_user(
    id uuid default gen_random_uuid() primary key,
    email text not null,
    preferences jsonb default '{}'::jsonb,
    created_at timestamptz not null default now()
);

create table opendcs_user_password(
    user_id uuid references opendcs_user(id) not null,
    password text,
    updated_at timestamptz not null --- TODO make trigger
);

create table opendcs_role(
    id uuid default gen_random_uuid() primary key,
    name varchar(128)
);

create table user_roles(
    user_id uuid references opendcs_user(id) not null,
    role_id uuid references opendcs_role(id) not null,
    primary key (user_id, role_id)
);

create table identity_provider (
    id uuid default gen_random_uuid() primary key,
    name varchar(256) not null unique,
    type varchar(256) not null,
    config json jsonb default '{}'::jsonb
);

create table user_identity_provider(
    user_id uuid references opendcs_user(id) not null,
    identity_provider_id uuid references identity_provider(id) not null,
    primary key (user_id, identity_provider_id)
);