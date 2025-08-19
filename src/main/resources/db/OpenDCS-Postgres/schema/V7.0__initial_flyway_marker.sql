-- Long term these should go away in favor of the 
-- built in flyway version table.
-- however too many internal locations relay on them so removal will happen in 8.0
delete from DecodesDatabaseVersion;
insert into DecodesDatabaseVersion values(70, 'Additional information is available in the ''flyway_schema_history'' table.');
delete from tsdb_database_version;
insert into tsdb_database_version values(70, 'Additional information is available in the ''flyway_schema_history'' table.');