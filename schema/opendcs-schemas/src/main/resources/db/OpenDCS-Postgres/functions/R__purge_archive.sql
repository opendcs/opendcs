create or replace function purge_archive()
returns trigger as $$
declare
    days int;
begin
    select (config->>'length_days')::int into days 
        from lrgs_data_interface
        where name = 'LrgsArchive';
    delete from lrgs_archive
    where created_at < now() - (days || ' days')::interval;
    return new;
end;
$$ language plpgsql;
