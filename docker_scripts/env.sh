#!/bin/bash

export PATH=/opt/opendcs/bin:$PATH
if [[ -z "APP_NAME" ]]; then
    export APP_NAME=`hostname`
fi

exec $*
