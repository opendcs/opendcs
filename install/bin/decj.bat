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
  echo "Creating Local User Directory and initial properties in %DCSTOOL_USERDIR%"
  echo "The default XML database has been copied to this directory."
  mkdir %DCSTOOL_USERDIR%
  copy %APP_PATH%\decodes.properties %DCSTOOL_USERDIR%\user.properties
  xcopy %APP_PATH%\edit-db %DCSTOOL_USERDIR%\edit-db /E /I
)

if exist "%DCSTOOL_USERDIR%/dep\" do (
  set "CLASSPATH=!CLASSPATH!;%DCSTOOL_USERDIR%/dep/*;%APP_PATH%/dep/*"
  )

java -Xmx240m %DECJ_MAXHEAP% %DECJ_OPTS% -cp "!CLASSPATH!" -DDCSTOOL_HOME="%APP_PATH%" -DDECODES_INSTALL_DIR="%APP_PATH%" -DDCSTOOL_USERDIR="%DCSTOOL_USERDIR%" %*%
