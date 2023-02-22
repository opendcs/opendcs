#!/bin/bash

if [ ! -d $LRGSHOME/netlist ]; then
    echo "Generating initial LRGS HOME Directory."
    cp -r $DCSTOOL_HOME/users .
    cp $DCSTOOL_HOME/*.conf .
    cp $DCSTOOL_HOME/lrgs.conf .

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

decj -DLRGSHOME=$LRGSHOME lrgs.lrgsmain.LrgsMain -l /dev/stdout