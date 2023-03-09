#!/bin/bash

if [ ! -f $DCSTOOL_USERDIR/user.properties ]; then
    echo "Generating Initial properties file."
    sed -e "s/OPENDCS_DATABASE_TYPE/$DATABASE_TYPE/" \
        -e "s~OPENDCS_DATABASE_URL~$DATABASE_URL~" \
        -e "s/OPENDCS_DATABASE_DRIVER/$DATABSE_DRIVER/" \
        -e "s/OPENDCS_CWMS_OFFICE/$CWMS_OFFICE/" \
        -e "s/OPENDCS_DATATYPE_STANDARD/$DATATYPE_STANDARD/" \
        /opt/opendcs/decodes.properties.template \
        > $DCSTOOL_USERDIR/user.properties
fi
