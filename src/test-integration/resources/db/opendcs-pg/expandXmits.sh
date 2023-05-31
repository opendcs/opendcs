#!/bin/bash

echo "DELETE from dcp_trans_day_map;" > dcp_trans_expanded.sql
for x in `seq -f %02g 1 31`
do
	sed -e s/SUFFIX/$x/g dcp_trans_template.sql >> dcp_trans_expanded.sql
	echo "INSERT INTO DCP_TRANS_DAY_MAP VALUES('$x', null);" >>dcp_trans_expanded.sql
	echo "DROP SEQUENCE IF EXISTS DCP_TRANS_${x}IDSEQ;" >>dcp_trans_expanded.sql
	echo "CREATE SEQUENCE DCP_TRANS_${x}IDSEQ;" >>dcp_trans_expanded.sql
done
