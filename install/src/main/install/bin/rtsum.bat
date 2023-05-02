@echo off

#
# This script starts the LRGS Real Time Summary Status GUI
#
$INSTALL_PATH/bin/decj.bat -DDECODES_INSTALL_DIR=$INSTALL_PATH lrgs.rtstat.RtSummaryStat %*%

