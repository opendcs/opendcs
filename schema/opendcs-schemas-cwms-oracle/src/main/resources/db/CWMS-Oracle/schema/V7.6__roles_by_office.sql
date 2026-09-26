alter table ${CCP_SCHEMA}.user_roles
add (office_code number default 0 not null);
-- cannot add foreign key reference to table due to permissions.

create unique index office_user_roles_pk_idx
on ${CCP_SCHEMA}.user_roles (office_code, user_id, role_id)
invisible online;

-- now drop the old primary key and put the new one in place
declare
    constraint_name varchar2(256);
begin
    select constraint_name into constraint_name
      from dba_constraints
     where constraint_type = 'P'
       and lower(table_name) = 'user_roles'
       and owner='${flyway:defaultSchema}';

    execute immediate 'alter table ${CCP_SCHEMA}.user_roles drop constraint ' || constraint_name;

    execute immediate 'alter table ${CCP_SCHEMA}.user_roles add constraint office_user_roles_pk ' ||
                      'primary key (office_code, user_id, role_id) using index office_user_roles_pk_idx';

    execute immediate 'alter index office_user_roles_pk_idx visible';
end;
