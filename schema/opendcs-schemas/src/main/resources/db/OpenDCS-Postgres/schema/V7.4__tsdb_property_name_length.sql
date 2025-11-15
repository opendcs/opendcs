alter table tsdb_property alter column prop_name type varchar(512);
alter table tsdb_property alter column prop_value type varchar(1024) drop not null;