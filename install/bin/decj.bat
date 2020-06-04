@echo off

setLocal EnableDelayedExpansion

if defined CP_SHARED_JAR_DIR (
 for /R %CP_SHARED_JAR_DIR% %%a in (*.jar) do (
   set CLASSPATH=!CLASSPATH!;%%a
 )
)

set CLASSPATH="$INSTALL_PATH/bin/opendcs.jar;$INSTALL_PATH/bin/hibernate.cfg.xml;
for /R $INSTALL_PATH/dep %%a in (*.jar) do (
  set CLASSPATH=!CLASSPATH!;%%a
)

set CLASSPATH=!CLASSPATH!"

java -Xmx240m -cp !CLASSPATH! -DDCSTOOL_HOME=$INSTALL_PATH -DDECODES_INSTALL_DIR=$INSTALL_PATH -DDCSTOOL_USERDIR=$INSTALL_PATH -Dcwms.db.impl.classpaths=$INSTALL_PATH\cwmsDbAPI %*%
