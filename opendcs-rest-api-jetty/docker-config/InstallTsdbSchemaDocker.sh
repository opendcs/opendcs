#!/bin/bash

cd /opendcs/schema/opendcs-pg
sed -i "s/-h \$DBHOST//g" /opendcs/schema/opendcs-pg/createDb.sh
sed -i "s/CREATE SEQUENCE tsdb_data_sourceIdSeq ;//g" /opendcs/schema/opendcs-pg/sequences.sql
echo -e "${OPENDCS_USERNAME}\n${OPENDCS_PASSWORD}\n${OPENDCS_PASSWORD}\n${OPENDCS_PASSWORD}\n${OPENDCS_NUM_STORAGE_TABLES}\n${OPENDCS_STRING_STORAGE_TABLES}\nyes" | ./createDb.sh
