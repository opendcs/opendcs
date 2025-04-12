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
