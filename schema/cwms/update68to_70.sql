create or replace procedure set_office_column(new_office IN OUT cwms_20.av_office.office_code%type,
                                              old_office cwms_20.av_office.office_code%type)
is
begin
    if new_office is null and old_office is null
    then
        new_office := cwms_20.cwms_util.get_office_code(null);
    elsif new_office is null and old_office is not null
    then
        new_office := old_office;
    end if;
end set_office_column;
/
set serveroutput on;
declare
    the_sql varchar2(4000);
begin

  for rec in (
    select table_name from user_tab_columns where column_name in ('DB_OFFICE_CODE')
    )
  loop
    execute immediate 'alter table ' || rec.table_name || ' modify db_office_code default null';
    the_sql := 
    'create or replace trigger ' || rec.table_name || '_get_office' ||
    ' before insert or update on ' || rec.table_name ||
    ' for each row begin set_office_column(:new.db_office_code,:old.db_office_code); end;';
    dbms_output.put_line(the_sql);
    execute immediate the_sql;
  end loop;
end;
/
