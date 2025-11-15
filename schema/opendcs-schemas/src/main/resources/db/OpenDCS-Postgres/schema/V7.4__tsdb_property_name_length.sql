alter table tsdb_property alter column type prop_name varchar(512);
alter table tsdb_property alter column type prop_value varchar(1024),
                          alter column prop_value drop not null;