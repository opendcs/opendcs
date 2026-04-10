/**
This file contains the trigger function attached to numeric data tables.
It uses the comp_depends table to determine if any computations need to
be run when values are inserted, modified, or deleted, and creates
cp_comp_tasklist records accordingly.
*/
create or replace function comp_trigger () returns trigger
AS $$
DECLARE
	l_is_delete CHAR;
	l_ts_id INTEGER;
	l_sample_time BIGINT;
	l_ts_value DOUBLE PRECISION;
	l_flags INTEGER;
	l_source_id INTEGER;
	l_app_id_rec RECORD;
BEGIN
	IF TG_OP = 'DELETE' THEN
		l_is_delete := 'Y';
		l_ts_id := OLD.ts_id;
		l_sample_time := OLD.sample_time;
		l_ts_value := OLD.ts_value;
		l_flags := OLD.flags;
		l_source_id := OLD.source_id;
	ELSE
		l_is_delete := 'N';
		l_ts_id := NEW.ts_id;
		l_sample_time := NEW.sample_time;
		l_ts_value := NEW.ts_value;
		l_flags := NEW.flags;
		l_source_id := NEW.source_id;
	END IF;
	FOR l_app_id_rec IN 
		select distinct loading_application_id 
		from cp_comp_depends, cp_computation
		where cp_comp_depends.ts_id = l_ts_id
		and cp_comp_depends.computation_id = cp_computation.computation_id
	LOOP
		insert into cp_comp_tasklist(record_num, loading_application_id, 
			ts_id, num_value, txt_value, date_time_loaded, sample_time, 
			delete_flag, flags, source_id)
			values (nextval('cp_comp_tasklistidseq'), 
				l_app_id_rec.loading_application_id, 
				l_ts_id, l_ts_value, null, 
				(extract(epoch from now()) * 1000),
				l_sample_time, l_is_delete, l_flags, l_source_id);
	END LOOP;
	IF TG_OP = 'DELETE' THEN
		RETURN OLD;
	ELSE
		RETURN NEW;
	END IF;
END;
$$ LANGUAGE 'plpgsql';
