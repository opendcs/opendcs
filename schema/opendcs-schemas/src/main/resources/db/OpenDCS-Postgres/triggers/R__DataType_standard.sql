----------------------------------------------------------------------------
-- This trigger makes sure that there is only one config sensor datatype
-- of a given data type standard. E.g., you cannot assign two CWMS datatypes
-- to the same sensor.
-- If a dt is already associated with the same standard it raises
-- 'CONFIG_DATATYPE_STANDARD_UNIQUE'
----------------------------------------------------------------------------
create or replace function cfg_dt_std_check () returns trigger as
$$
DECLARE
	newstd varchar;
BEGIN
	select standard into newstd from datatype where id = NEW.datatypeid;
	if exists (select * from configsensordatatype a, datatype b
		where a.configid = NEW.configid and a.sensornumber = NEW.sensornumber
		  and a.datatypeid = b.id and b.standard = newstd)
	    then raise exception 'CONFIG_DATATYPE_STANDARD_UNIQUE';
	end if;
	return NEW;
END;
$$ language plpgsql;

create or replace trigger cfg_dt_std_check_trigger
    before insert on configsensordatatype
    for each row
    execute procedure cfg_dt_std_check();
