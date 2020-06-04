#!/bin/bash

#
# Arguments #numeric-tables #string-tables
#

echo -n "-- created on " > ts_tables_expanded.sql
date >> ts_tables_expanded.sql

for x in `seq -f %04g 1 $1`
do
	sed -e s/0000/$x/ ts_num_template.sql > tt.sql
	grep -v '^--' tt.sql >> ts_tables_expanded.sql
done

for x in `seq -f %04g 1 $2`
do
	sed -e s/0000/$x/ ts_string_template.sql > tt.sql
	grep -v '^--' tt.sql >> ts_tables_expanded.sql
done

rm tt.sql
