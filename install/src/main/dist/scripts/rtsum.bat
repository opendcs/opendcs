@echo off

#
# This script starts the LRGS Real Time Summary Status GUI
#
@"%~dp0\decj" -DDECODES_INSTALL_DIR=$INSTALL_PATH lrgs.rtstat.RtSummaryStat %*%
