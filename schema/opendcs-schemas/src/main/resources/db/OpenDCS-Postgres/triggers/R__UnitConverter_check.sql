----------------------------------------------------------------------------
-- This trigger makes sure that the units involved in a conversion both
-- exist. It does a case-insensitive check on the EngineeringUnit table.
-- If either unit does not exist, raise NO_SUCH_UNIT
----------------------------------------------------------------------------
create or replace function unit_conv_check () returns trigger as
$$
BEGIN
	if not exists (select * from engineeringunit where lower(unitabbr) = lower(NEW.fromunitsabbr))
		then raise exception 'NO_SUCH_UNIT(%)', NEW.fromunitsabbr;
	end if;
	
	if not exists (select * from engineeringunit where lower(unitabbr) = lower(NEW.tounitsabbr))
		then raise exception 'NO_SUCH_UNIT(%)', NEW.tounitsabbr;
	end if;
	return NEW;
END;
$$ language plpgsql;

-- or replace here does limit to Postgres 14+; however while other version are supported that
-- seems reasonable to me.
create or replace trigger unit_conv_check_trigger
	before insert on unitconverter
	for each row
	execute procedure unit_conv_check();
