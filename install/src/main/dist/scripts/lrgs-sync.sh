#!/bin/sh

#
# This script synchronizes the master out to the subordinates.
# Usage:
#	lrgs/sync.sh [config-file]
# Default config file is $LRGSHOME/lrgs-sync.conf
#
tdir=$LRGSHOME/tmp

#
# Get the configuration file
#
cfgfile=$1
if [ -z "$cfgfile" ]
then
	cfgfile="$LRGSHOME/lrgs-sync.conf"
fi

#
# timestamp for the log
#
echo -n "lrgs-sync.sh running at "
date
echo "Configuration file is: $cfgfile"
mkdir -p $tdir

#
# Function to parse a config line into user, host, and lrgshome.
function parse_conf_line () {
#echo $line
linea=( $line )
user=${linea[0]}
host=${linea[1]}
lrgshome=${linea[2]}
}


#
# Determine if I am the master. Exit if not.
#
line=`grep -i MASTER $cfgfile`
parse_conf_line
myhost=`hostname`
if [ "$host" != "$myhost" ]
then
	echo "Exiting because I am NOT the master!"
fi

echo "continuing... I am the master."
mynetlistdir=`grep 'ddsNetlistDir=' $LRGSHOME/lrgs.conf | sed -e "s/^.*=//"`
if [ -z "$mynetlistdir" ]
then
  mynetlistdir="$LRGSHOME/netlist"
fi

myuserroot=`grep 'ddsUserRootDir=' $LRGSHOME/lrgs.conf | sed -e "s/^.*=//"`
if [ -z "$myuserroot" ]
then
  myuserroot="$lrgshome/users"
fi

echo "mynetlistdir=$mynetlistdir"
echo "myuserroot=$myuserroot"

#
# Parse the config file line by line.
#
cat $cfgfile | while read line
do
  parse_conf_line
  echo user=$user
  echo host=$host
  echo home=$lrgshome
  if [ "$host" != "$myhost" ] 
  then
    echo executing: scp $user@$host:$lrgshome/lrgs.conf $tdir/lrgs.conf-$host
    scp $user@$host:$lrgshome/lrgs.conf $tdir/lrgs.conf-$host
    scp .lrgs.passwd $user@$host:$lrgshome
    userroot=`grep 'ddsUserRootDir=' $tdir/lrgs.conf-$host | sed -e "s/^.*=//"`
    if [ -z "$userroot" ]
    then
      userroot="$lrgshome/users"
    fi
    echo "userroot: $userroot"
    echo "executing: rsync -v -a -e ssh --delete $myuserroot/ $user@$host:$userroot"
    rsync -v -a -e ssh --delete $myuserroot/ $user@$host:$userroot

    netlistdir=`grep 'ddsNetlistDir=' $tdir/lrgs.conf-$host | sed -e "s/^.*=//"`
    if [ -z "$netlistdir" ]
    then
      netlistdir="$lrgshome/netlist"
    fi
    echo "netlistdir: $netlistdir"
    echo "executing: rsync -v -a -e ssh --delete $mynetlistdir/ $user@$host:$netlistdir"
    rsync -v -a -e ssh --delete $mynetlistdir/ $user@$host:$netlistdir
  fi
done

