create table identity_provider (
    id number(14) primary key,
    name varchar(256) not null unique,
    type varchar(256) not null,
    config varchar(4000) default '{}',
    updated_at timestamp with time zone not null
) ${TABLE_SPACE_SPEC};

create table opendcs_user(
    id number(14) primary key,
    preferred_name_provider number(14) references identity_provider(id),
    email varchar(256) not null unique,
    preferences varchar(4000) default '{}',
    created_at timestamp with time zone default systimestamp not null,
    updated_at timestamp with time zone not null
) ${TABLE_SPACE_SPEC};

create table opendcs_user_password(
    user_id number(14) references opendcs_user(id) not null primary key,
    password varchar(1028),
    updated_at timestamp with time zone not null
) ${TABLE_SPACE_SPEC};

create table opendcs_role(
    id number(14) primary key,
    name varchar(128) not null unique,
    description varchar(4000),
    updated_at timestamp with time zone not null
) ${TABLE_SPACE_SPEC};

create table user_roles(
    user_id number(14) references opendcs_user(id) not null,
    role_id number(14) references opendcs_role(id) not null,
    primary key (user_id, role_id)
) ${TABLE_SPACE_SPEC};


create table user_identity_provider(
    user_id number(14) references opendcs_user(id) not null,
    subject varchar(2000) not null ,
    identity_provider_id number(14) references identity_provider(id) not null,
    primary key (user_id, identity_provider_id),
    created_at timestamp with time zone default systimestamp not null,
    unique (identity_provider_id, subject)
) ${TABLE_SPACE_SPEC};
