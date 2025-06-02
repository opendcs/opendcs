create table ${flyway:defaultSchema}.database_properties(
    name text not null unique,
    value text
);

delete from DecodesDatabaseVersion;
insert into DecodesDatabaseVersion values(72, 'Additional information is available in the ''flyway_schema_history'' table.');
delete from tsdb_database_version;
insert into tsdb_database_version values(72, 'Additional information is available in the ''flyway_schema_history'' table.');