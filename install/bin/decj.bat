@echo off

setLocal EnableDelayedExpansion

set "BIN_PATH=%~dp0"
pushd "%BIN_PATH%.."
set "APP_PATH=%CD%"
popd

set "CLASSPATH=%BIN_PATH%opendcs.jar;%BIN_PATH%hibernate.cfg.xml"

set "CLASSPATH=!CLASSPATH!"

if not defined DCSTOOL_USERDIR (
  set "DCSTOOL_USERDIR=%APPDATA%\.opendcs"
)

if not exist %DCSTOOL_USERDIR%\ (
  echo "Creating Local User Directory and initial properties in %DCSTOOL_USERDIR%" 1>&2
  echo "The default XML database has been copied to this directory." 1>&2
  mkdir %DCSTOOL_USERDIR%
  copy %APP_PATH%\decodes.properties %DCSTOOL_USERDIR%\user.properties
  xcopy %APP_PATH%\edit-db %DCSTOOL_USERDIR%\edit-db /E /I
)

if exist "%CP_SHARED_JAR_DIR" do (
  set "CLASSPATH=!CLASSPATH!;%CP_SHARED_JAR_DIR/*"
  echo "The need of this is superseded by using a DCSTOOL_USERDIR for configuration." 1>&2
  echo "Support for this variable will be moved in a future release. Please update " 1>&2
  echo "your configuration." 1>&2
)

if exist "%DCSTOOL_USERDIR%/dep\" do (
  set "CLASSPATH=!CLASSPATH!;%DCSTOOL_USERDIR%/dep/*"
  )

set "CLASSPATH=!CLASSPATH!;%APP_PATH%/dep/*"

java -Xmx240m %DECJ_MAXHEAP% %DECJ_OPTS% -cp "!CLASSPATH!" -DDCSTOOL_HOME="%APP_PATH%" -DDECODES_INSTALL_DIR="%APP_PATH%" -DDCSTOOL_USERDIR="%DCSTOOL_USERDIR%" %*%
