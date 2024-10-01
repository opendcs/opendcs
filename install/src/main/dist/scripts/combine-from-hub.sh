#!/bin/bash

########################################################################
# Settable Parameters:
########################################################################

# DECODES_INSTALL_DIR should be set in the environment.
#DECODES_INSTALL_DIR=~/DECODES

# Set HUBDIR to the parent directory of the district directories.
HUBDIR=$HOME/dbhub

HUBCHKFILE=$HOME/dcpmon/combine-from-hub.chk

# A directory used for temporary storage of platforms being imported.
TMPPLATDIR=$DECODES_INSTALL_DIR/tmp/combine-from-hub

# Debug info log file
DEBUGLOG=$HOME/dcpmon/combine-from-hub.log

# Set this to -d1, -d2, or -d3 for debug info in the log files.
DEBUGARG="-d3"


########################################################################
# Shouldn't be any need to modify anything below this line.
########################################################################

cd $DECODES_INSTALL_DIR
touch hubchk.new

mkdir -p $TMPPLATDIR
echo "cd to $HUBDIR" >> $DEBUGLOG
cd $HUBDIR
rm -f $TMPPLATDIR/*
for district in `cat $HOME/scripts/district.list`
do
	if [ -d $district -a -d $district/current/platform ] ; then
		echo exporting $district >> $DEBUGLOG

		# Create the TOIMPORT.nl 
		echo Creating $district-TOIMPORT.nl >>$DEBUGLOG
		$DECODES_INSTALL_DIR/bin/generatetoimportnl -f $district/current/netlist/$district-RIVERGAGES-DAS.xml

		#$DECODES_INSTALL_DIR/bin/pxport -l $DEBUGLOG $DEBUGARG -E $district/current -n $district-RIVERGAGES-DAS -O $district > $TMPPLATDIR/$district-platforms.xml
		$DECODES_INSTALL_DIR/bin/pxport -l $DEBUGLOG $DEBUGARG -E $district/current -f $DECODES_INSTALL_DIR/dcptoimport/$district-TOIMPORT.nl -O $district > $TMPPLATDIR/$district-platforms.xml

#		cp $district/current/routing/$district-RIVERGAGES-DAS.xml $TMPPLATDIR/$district-routspec.xml
		cp $district/current/netlist/$district-RIVERGAGES-DAS.xml $TMPPLATDIR/$district-netlist.xml
		cd $TMPPLATDIR
		echo importing $district >>$DEBUGLOG
		$DECODES_INSTALL_DIR/bin/dbimport -l $DEBUGLOG $DEBUGARG $district*.xml
		cd $HUBDIR
	fi
done
echo "Touching $HUBCHKFILE" >> $DEBUGLOG
mv $DECODES_INSTALL_DIR/hubchk.new $HUBCHKFILE
echo "Combine Complete" >> $DEBUGLOG

