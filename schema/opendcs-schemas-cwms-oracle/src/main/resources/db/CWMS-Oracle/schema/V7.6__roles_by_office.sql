alter table ${CCP_SCHEMA}.user_roles 
add (office_code number default 0 not null);
-- cannot add foreign key reference to table.