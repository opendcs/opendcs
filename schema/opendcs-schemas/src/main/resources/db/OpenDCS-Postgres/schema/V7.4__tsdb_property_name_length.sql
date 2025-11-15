alter table tsdb_property alter column prop_name varchar(512);
alter table tsdb_property alter column prop_value varchar(1024) drop not null;