#!/bin/bash

INPUT=$1
OUTPUT=setPerms.sql

echo -n "-- created on " > $OUTPUT
date >> $OUTPUT

grep -ih "create table " $1 | sed -e "s/CREATE TABLE //" | sort>/tmp/table-list
#dos2unix /tmp/table-list

for TABLE in `cat /tmp/table-list`
do
	echo "ALTER TABLE $TABLE OWNER TO \"OTSDB_ADMIN\";" >> $OUTPUT
	# OTSDB_USER can select all tables
	echo "GRANT SELECT ON TABLE $TABLE TO \"OTSDB_USER\";" >> $OUTPUT
	if [[ $TABLE == DCP_TRANS_* ]] || [[ $TABLE == TS_NUM_* ]] || [[ $TABLE == TS_STRING_* ]] || [[ $TABLE == "TS_SPEC" ]] || [[ $TABLE == "CP_COMP_PROC_LOCK" ]]
	then
		echo "GRANT ALL ON TABLE $TABLE TO \"OTSDB_DATA_ACQ\";" >> $OUTPUT
	elif [[ $TABLE == "CP_COMP_DEPENDS" ]] || [[ $TABLE == "CP_COMP_TASKLIST" ]] || [[ $TABLE == "CP_DEPENDS_SCRATCHPAD" ]]
	then
		echo "GRANT ALL ON TABLE $TABLE TO \"OTSDB_COMP_EXEC\";" >> $OUTPUT
	else
		echo "GRANT ALL ON TABLE $TABLE TO \"OTSDB_MGR\";" >> $OUTPUT
	fi
	echo >> $OUTPUT
done

grep -ih "create sequence " $1 | sed -e "s/CREATE SEQUENCE //" | sed -e "s/;//" | sort>/tmp/seq-list
for SEQ in `cat /tmp/seq-list`
do
	echo "ALTER SEQUENCE $SEQ OWNER TO \"OTSDB_ADMIN\";" >> $OUTPUT
	echo "GRANT USAGE, SELECT ON SEQUENCE $SEQ TO \"OTSDB_USER\";" >>$OUTPUT
done

rm /tmp/table-list

