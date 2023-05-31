-------------------------------------------------------------------------------
-- $Id: data_trig_tmpl.sql,v 1.1 2020/02/03 21:30:04 mmaloney Exp $
--
-- This file contains the template for assigning the comp_trigger to a numeric
-- data table. Before running, run this script through 'sed' to replace '0000'
-- with the suffix of the data table, which must already exist.
--
-- $Log: data_trig_tmpl.sql,v $
-- Revision 1.1  2020/02/03 21:30:04  mmaloney
-- Added trigger for OpenTSDB 6.7
--
-------------------------------------------------------------------------------

drop trigger if exists TS_NUM_<TableNumber>_TRIG
	on TS_NUM_0000;

create trigger TS_NUM_<TableNumber>_TRIG
    before update or insert or delete
    on TS_NUM_<TableNumber>
    for each row execute procedure comp_trigger();

