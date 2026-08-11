create or replace function current_organization() returns bigint as $$
begin
    return nullif(current_setting('opendcs.org_id', true), '')::bigint;
end;
$$ language plpgsql stable;
