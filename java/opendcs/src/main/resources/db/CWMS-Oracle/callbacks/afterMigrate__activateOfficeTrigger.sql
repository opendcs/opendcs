declare
    the_sql varchar2(4000);
begin
 
  for rec in (
    select table_name from dba_tab_columns where column_name in ('DB_OFFICE_CODE') and owner='${flyway:defaultSchema}'
    )
  loop
    execute immediate 'alter table ${flyway:defaultSchema}.' || rec.table_name || ' modify db_office_code default null';
    the_sql := 
    'create or replace trigger ${flyway:defaultSchema}.' || rec.table_name || '_get_office' ||
    ' before insert or update on ${flyway:defaultSchema}.' || rec.table_name ||
    ' for each row begin set_office_column(:new.db_office_code,:old.db_office_code); end;';
    dbms_output.put_line(the_sql);
    execute immediate the_sql;
  end loop;
end;
/