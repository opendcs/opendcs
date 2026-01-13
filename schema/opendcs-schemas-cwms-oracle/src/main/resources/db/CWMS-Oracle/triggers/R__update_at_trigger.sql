

create or replace trigger update_at_idp
before insert or update
on identity_provider
for each row
begin
    NEW.updated_at := systimestamp;
end;
/


create or replace trigger update_at_opendcs_user
before insert or update
on opendcs_user
for each row
begin
    NEW.updated_at := systimestamp;
end;
/

create or replace trigger opendcs_user_password
before insert or update
on opendcs_user_password
for each row
begin
    NEW.updated_at := systimestamp;
end;
/

create or replace trigger update_at_role
before insert or update
on opendcs_role
for each row
begin
    NEW.updated_at := systimestamp;
end;
/