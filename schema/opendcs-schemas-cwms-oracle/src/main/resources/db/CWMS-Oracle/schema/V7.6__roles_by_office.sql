alter table ${CCP_SCHEMA}.user_roles 
add (office_code number default 0 not null);
-- cannot add foreign key reference to table.

create unique index user_roles_pk_new_idx
on ${CCP_SCHEMA}.user_roles (office_code, user_id, role_id)
invisible online;

declare
    index_name varchar2(256);
    constraint_name varchar2(256);
begin
    select index_name, constraint_name into index_name, constraint_name
      from dba_constraints 
     where constraint_type = 'P' 
       and lower(table_name) = 'user_roles'
       and owner='${flyway:defaultSchema}';

    execute immediate 'alter table ${CCP_SCHEMA}.user_roles rename constraint ' || constraint_name || 
                      ' to user_roles_pk';
    execute immediate 'alter table ${CCP_SCHEMA}.user_roles modify constraint user_roles_pk using index user_roles_pk_new_idx';

    execute immediate 'alter index user_roles_pk_new_idx visible';
    execute immediate 'alter index ' || index_name || ' invisible' ;
    execute immediate 'drop index ' || index_name;
end;
