
create or replace procedure create_user(username text, password text)
language plpgsql
as $$
begin
    execute format('create user %I with password %L',  username, password);
end;
$$;

create or replace procedure assign_role(username text, role text)
language plpgsql
as $$
begin
    execute format('grant %I to %I', role, username);
end;
$$;