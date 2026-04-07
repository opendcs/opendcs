#!/bin/bash

nohup ./proxy_auth.sh 2>&1 > /dev/null &
echo "auth proxy started now executing $*"
exec $*