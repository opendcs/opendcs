#!/bin/bash

source /opt/opendcs/tsdb_config.sh

DH=$DCSTOOL_HOME

CP=$DH/bin/opendcs.jar

if [ -d "$DCSTOOL_USERDIR/dep" ]
then
  for f in $DCSTOOL_USERDIR/dep/*.jar
  do
    CP=$CP:$f
  done
fi

# Add the OpenDCS standard 3rd party jars to the classpath
for f in `ls $DH/dep/*.jar | sort`
do
   CP=$CP:$f
done



exec java -Xms120m $DECJ_MAXHEAP -cp $CP \
     -DDCSTOOL_HOME=$DH -DDECODES_INSTALL_DIR=$DH \
     -DDCSTOOL_USERDIR=$DCSTOOL_USERDIR \
     decodes.tsdb.ComputationApp -d3 -l /dev/stdout -a $APPLICATION_NAME