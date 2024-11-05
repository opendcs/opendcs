#!/bin/bash

if [ ! -d $LRGSHOME/netlist ]; then
    echo "Generating initial LRGS HOME Directory."
    cp -r $DCSTOOL_HOME/users .
    cp $DCSTOOL_HOME/*.conf .
    cp $DCSTOOL_HOME/*.xml .
    cp $DCSTOOL_HOME/lrgs.conf .
    cp -r $DCSTOOL_HOME/netlist .

    if [ "$LRGS_ADMIN_PASSWORD" == "" ]; then
        LRGS_ADMIN_PASSWORD=`tr -cd '[:alnum:]' < /dev/urandom | fold -w30 | head -n1`
        echo "Admin Password is $LRGS_ADMIN_PASSWORD"
        echo "This will not be printed on subsequent runs"
    fi
    
    cat <<EOF | editPasswd
adduser lrgsadmin
$LRGS_ADMIN_PASSWORD
$LRGS_ADMIN_PASSWORD
addrole lrgsadmin dds
addrole lrgsadmin admin
write
quit
EOF

fi

DH=$DCSTOOL_HOME

CP=$DH/bin/opendcs.jar

if [ -d "$LRGSHOME/dep" ]
then
  for f in $LRGSHOME/dep/*.jar
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
     -DDCSTOOL_USERDIR=$DCSTOOL_USERDIR -DLRGSHOME=$LRGSHOME \
     lrgs.lrgsmain.LrgsMain -d3 -l /dev/stdout -F -k -