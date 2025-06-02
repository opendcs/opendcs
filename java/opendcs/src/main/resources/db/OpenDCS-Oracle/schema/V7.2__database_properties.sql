
create table ${flyway:defaultSchema}.opendcs_properties(
    name varchar2(512) not null unique,
    value varchar2(4000)
) ${TABLE_SPACE_SPEC};

delete from DecodesDatabaseVersion;
insert into DecodesDatabaseVersion values(72, 'Additional information is available in the ''flyway_schema_history'' table.');
delete from tsdb_database_version;
insert into tsdb_database_version values(72, 'Additional information is available in the ''flyway_schema_history'' table.');