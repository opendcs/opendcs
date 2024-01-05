
create or replace function create_user(username text, password text)
as
$$
    execute format('create user %s with password ''%s''',  quote_ident(username), password);
$$ plpgsql;