@echo off

setLocal EnableDelayedExpansion

set "BIN_PATH=%~dp0"
pushd "%BIN_PATH%.."
set "APP_PATH=%CD%"
popd

set "CLASSPATH=%BIN_PATH%opendcs.jar;%BIN_PATH%hibernate.cfg.xml;"

if defined CP_SHARED_JAR_DIR (
 for /R "%CP_SHARED_JAR_DIR%" %%a in (*.jar) do (
   set "CLASSPATH=!CLASSPATH!;%%a"
 )
)

for /R "%APP_PATH%/dep" %%a in (*.jar) do (
  set "CLASSPATH=!CLASSPATH!;%%a"
  )

set "CLASSPATH=!CLASSPATH!"

java -Xmx240m -cp "!CLASSPATH!" -DDCSTOOL_HOME="%APP_PATH%" -DDECODES_INSTALL_DIR="%APP_PATH%" -DDCSTOOL_USERDIR="%APP_PATH%" %*%
