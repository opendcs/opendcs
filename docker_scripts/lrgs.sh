#!/bin/bash

if [ ! -d $LRGSHOME/netlist ]; then
    echo "Generating initial LRGS HOME Directory."
    cp -r $DCSTOOL_HOME/lrgs/users .
    cp $DCSTOOL_HOME/lrgs/*.conf .
    cp $DCSTOOL_HOME/lrgs/*.xml .
    cp $DCSTOOL_HOME/lrgs/lrgs.conf .
    cp -r $DCSTOOL_HOME/lrgs/netlist .

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

set_lrgs_property()
{
    key="$1"
    value="$2"
    if grep -q "^${key}=" "$LRGSHOME/lrgs.conf"; then
        sed -i "s|^${key}=.*|${key}=${value}|" "$LRGSHOME/lrgs.conf"
    else
        printf '%s=%s\n' "$key" "$value" >> "$LRGSHOME/lrgs.conf"
    fi
}

if [ "${LRGS_HTTP_ENABLED:-false}" = "true" ]; then
    set_lrgs_property "LrgsInput.web.class" "org.opendcs.lrgs.http.LrgsHttpInput"
    set_lrgs_property "LrgsInput.web.enabled" "true"
    set_lrgs_property "LrgsInput.web.port" "${LRGS_HTTP_PORT:-7000}"
fi

if [ "${DCPMON_FIXTURES_ENABLED:-false}" = "true" ]; then
    set_lrgs_property "noTimeout" "true"
    set_lrgs_property "LrgsInput.dcpmon-fixtures.class" "lrgs.lrgsmain.DcpMonFixtureInput"
    set_lrgs_property "LrgsInput.dcpmon-fixtures.enabled" "true"
    cat > "$LRGSHOME/netlist/SWT.nl" <<'EOF'
CE1F40D4:NIMB Nimbus complete:u
CE1F2532:BMOB Blue Mountain partial:u
CE000001:PRTY Parity and low battery:u
CE000002:MISS Missing transmissions:u
CE000003:UNKN Missing PDT schedule:u
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
     -Dlogback.configurationFile=$DCSTOOL_HOME/logback.xml \
     -DAPP_NAME=$APP_NAME \
     lrgs.lrgsmain.LrgsMain -F -k -
