
----------------------------------------------------------------------------
-- This trigger makes sure that the units involved in a conversion both
-- exist. It does a case-insensitive check on the EngineeringUnit table.
-- If either unit does not exist, raise NO_SUCH_UNIT
----------------------------------------------------------------------------
create or replace function unit_conv_check () returns trigger as
$unit_conf_check$
BEGIN
	if not exists (select * from engineeringunit where lower(unitabbr) = lower(NEW.fromunitsabbr))
	then raise exception 'NO_SUCH_UNIT';
	end if;
	if not exists (select * from engineeringunit where lower(unitabbr) = lower(NEW.tounitsabbr))
	then raise exception 'NO_SUCH_UNIT';
	end if;
	return NEW;
END;
$unit_conf_check$ language plpgsql;

create trigger unit_conv_check_trigger
before insert on unitconverter
for each row
execute procedure unit_conv_check();



----------------------------------------------------------------------------
-- This trigger makes sure that there is only one config sensor datatype
-- of a given data type standard. E.g., you cannot assign two CWMS datatypes
-- to the same sensor.
-- If a dt is already associated with the same standard it raises
-- 'CONFIG_DATATYPE_STANDARD_UNIQUE'
----------------------------------------------------------------------------
create or replace function cfg_dt_std_check () returns trigger as
$cfg_dt_std_check$
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
$cfg_dt_std_check$ language plpgsql;

create trigger cfg_dt_std_check_trigger
before insert on configsensordatatype
for each row
execute procedure cfg_dt_std_check();

