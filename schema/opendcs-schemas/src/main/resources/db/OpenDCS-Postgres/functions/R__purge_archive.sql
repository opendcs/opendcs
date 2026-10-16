create or replace function purge_archive()
returns trigger as $$
begin
    -- delete from lrgs_archive
    -- where created_at < now() - interval (select config->>'length_days'
    --                                        from lrgs_data_interface
    --                                        where name = 'LrgsArchive') || ' days';
    -- return new;
end;
$$ language plpgsql;
