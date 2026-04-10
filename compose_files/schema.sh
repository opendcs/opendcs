#!/bin/bash

echo $DATABASE_URL
export DATABASE_TYPE=OPENTSDB
export DATABASE_DRIVER="org.postgresql.Driver"
export DATATYPE_STANDARD="CWMS"
export KEYGENERATOR="decodes.sql.SequenceKeyGenerator"

rm /dcs_user_dir/user.properties
bash /opt/opendcs/tsdb_config.sh
echo "***** GENERATED PROPERTIES FILE *****"
cat /dcs_user_dir/user.properties
echo "***** END GENERATED PROPERTIES FILE *****"

# Build classpath
CP=$DCSTOOL_HOME/bin/opendcs.jar

# If a user-specific 'dep' (dependencies) directory exists, then
# add all the jars therein to the classpath.
if [ -d "$DCSTOOL_USERDIR/dep" ]; then
  CP=$CP:$DCSTOOL_USERDIR/dep/*
fi
CP=$CP:$DCSTOOL_HOME/dep/*

PLACEHOLDERS=()
unset IFS
for var in $(compgen -e); do
    name=$var
    value=${!var}
    if [[ "$name" =~ ^placeholder_.*$ ]]; then
        PLACEHOLDERS+=("-D${name/placeholder_/}=${value}")
    fi
    
done
echo "Placeholders ${PLACEHOLDERS[@]}"
set -x
echo "Placeholders ${PLACEHOLDERS[@]}"
exec java  -Xms120m -cp $CP \
    -Dlogback.configurationFile=$DCSTOOL_HOME/logback.xml \
    -DAPP_NAME=migration \
    -DLOG_LEVEL=${LOG_LEVEL:-INFO} \
    -DDCSTOOL_HOME=$DCSTOOL_HOME -DDECODES_INSTALL_DIR=$DCSTOOL_HOME -DDCSTOOL_USERDIR=$DCSTOOL_USERDIR \
    org.opendcs.database.ManageDatabaseApp  -I ${DCS_IMPLEMENTATION} \
    -P /dcs_user_dir/user.properties \
    -username "${DCS_OWNER}" \
    -password "${DCS_OWNER_PASS}" \
    -appUsername "${DCS_USER}" \
    -appPassword "${DCS_PASS}" \
    "${PLACEHOLDERS[@]}"