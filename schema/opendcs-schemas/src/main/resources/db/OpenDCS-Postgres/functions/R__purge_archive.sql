create or replace function purge_archive()
returns trigger as $$
begin
    delete from lrgs_archive
    where created_at < now() - interval (select archive_length_days from lrgs_config) || ' days';
    return new;
end;
$$ language plpgsql;
