#!/bin/bash 


source /opt/opendcs/tsdb_config.sh
echo "***** GENERATED PROPERTIES FILE *****"
cat /dcs_user_dir/user.properties
echo "***** END GENERATED PROPERTIES FILE *****"

PLACEHOLDERS=()
unset IFS
for var in $(compgen -e); do
    name=$var
    value=${!var}
    if [[ "$name" =~ ^placeholder_.*$ ]]; then
        PLACEHOLDERS+=("-D${name/placeholder_/}=${value}")
    fi
done

# Build classpath
CP=$DCSTOOL_HOME/bin/opendcs.jar

# If a user-specific 'dep' (dependencies) directory exists, then
# add all the jars therein to the classpath.
if [ -d "$DCSTOOL_USERDIR/dep" ]; then
  CP=$CP:$DCSTOOL_USERDIR/dep/*
fi
CP=$CP:$DCSTOOL_HOME/dep/*

if [[ -n "${APP_USER_FILE}" ]]; then
  APP_USER=`cat ${APP_USER_FILE}`
fi

if [[ -n "${APP_PASSWORD_FILE}" ]]; then
  APP_PASSWORD=`cat ${APP_PASSWORD_FILE}`
fi

if [[ -n "${MIGRATION_USER_FILE}" ]]; then
  MIGRATION_USER=`cat ${MIGRATION_USER_FILE}`
fi

if [[ -n "${MIGRATION_PASSWORD_FILE}" ]]; then
  MIGRATION_PASSWORD=`cat ${MIGRATION_PASSWORD_FILE}`
fi

echo "Placeholders ${PLACEHOLDERS[@]}"
exec java  -Xms120m -cp $CP \
    -Dlogback.configurationFile=$DCSTOOL_HOME/logback.xml \
    -DAPP_NAME=migration \
    -DLOG_LEVEL=${LOG_LEVEL:-INFO} \
    -DDCSTOOL_HOME=$DCSTOOL_HOME -DDECODES_INSTALL_DIR=$DCSTOOL_HOME -DDCSTOOL_USERDIR=$DCSTOOL_USERDIR \
    org.opendcs.database.ManageDatabaseApp -I ${DATABASE_IMPLEMENTATION} \
    -P /dcs_user_dir/user.properties \
    -username "${MIGRATION_USER}" \
    -password "${MIGRATION_PASSWORD}" \
    -appUsername "${APP_USER}" \
    -appPassword "${APP_PASSWORD}" \
    "${PLACEHOLDERS[@]}"
echo $?