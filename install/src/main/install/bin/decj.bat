@echo off

setLocal EnableDelayedExpansion

set "BIN_PATH=%~dp0"
pushd "%BIN_PATH%.."
set "APP_PATH=%CD%"
popd

set "ARG_FILE=%TEMP%\decj_argfile"

set "CLASSPATH=%APP_PATH%\dummy.jar"

if defined CP_SHARED_JAR_DIR (
 for /R "%CP_SHARED_JAR_DIR%" %%a in (*.jar) do (
   set "CLASSPATH=!CLASSPATH!;%%a"
 )
)

for /R "%APP_PATH%/dep" %%a in (*.jar) do (
  set "CLASSPATH=!CLASSPATH!;%%a"
  )

set "CLASSPATH=!CLASSPATH!"

if not defined DCSTOOL_USERDIR (
  set DCSTOOL_USERDIR="%APPDATA%\.opendcs"
)

if not exist %DCSTOOL_USERDIR%\ (
  echo "Creating Local User Directory and initial properties in %DCSTOOL_USERDIR%"
  echo "The default XML database has been copied to this directory."
  mkdir %DCSTOOL_USERDIR%
  copy %APP_PATH%\decodes.properties %DCSTOOL_USERDIR%\user.properties
  xcopy %APP_PATH%\edit-db %DCSTOOL_USERDIR%\edit-db /E /I
)

for /R "%DCSTOOL_USERDIR%/dep" %%a in (*.jar) do (
  set "CLASSPATH=!CLASSPATH!;%%a"
  )

echo -Xmx240m >> %ARG_FILE%
if defined DECJ_MAXHEAP (echo !DECJ_MAXHEAP! >> !ARG_FILE!)
if defined DECJ_OPTS (echo !DECJ_OPTS! >> !ARG_FILE!)
echo -cp !CLASSPATH! >> %ARG_FILE%
echo -DDCSTOOL_HOME="%APP_PATH:\=/%" >> %ARG_FILE%
echo -DDECODES_INSTALL_DIR="%APP_PATH:\=/%" >> %ARG_FILE%
echo -DDCSTOOL_USERDIR="%DCSTOOL_USERDIR:\=/%" >> %ARG_FILE%
cat %ARG_FILE%
@echo on
java @"%ARG_FILE%" %*%
del "%ARG_FILE%"
