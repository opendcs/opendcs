#!/bin/bash

FILENAME=redefDcpmonSeq.sql

echo "DELETE from dcp_trans_day_map;" > $FILENAME
for x in `seq -f %02g 1 31`
do
	echo "DELETE FROM DCP_TRANS_${x};" >> $FILENAME
    echo "INSERT INTO DCP_TRANS_DAY_MAP VALUES('$x', null);" >> $FILENAME
	echo "DROP SEQUENCE DCP_TRANS_${x}IDSEQ;" >> $FILENAME
	echo "CREATE SEQUENCE DCP_TRANS_${x}IDSEQ nocache minvalue 0 maxvalue 99999999999999;" >> $FILENAME
done
