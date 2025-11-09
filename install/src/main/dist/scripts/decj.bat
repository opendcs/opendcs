@echo off

setLocal EnableDelayedExpansion

set "BIN_PATH=%~dp0"
pushd "%BIN_PATH%.."
set "APP_PATH=%CD%"
popd

set "CLASSPATH=%BIN_PATH%opendcs.jar;%BIN_PATH%hibernate.cfg.xml"

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

if defined CP_SHARED_JAR_DIR (
    if exist "%CP_SHARED_JAR_DIR%" (
	      set "CLASSPATH=%CLASSPATH%;%CP_SHARED_JAR_DIR%\*"
        echo "The need of CP_SHARED_JAR_DIR is superseded by using a DCSTOOL_USERDIR for configuration." 1>&2
        echo "Support for this variable will be moved in a future release. Please update " 1>&2
        echo "your configuration." 1>&2
        echo !CLASSPATH!
    ) else (
        echo "Directory %CP_SHARED_JAR_DIR% does not exist." 1>&2
    )
)

set "LOGBACK="
if exist "%DCSTOOL_USERDIR%\logback.xml" (
  set "LOGBACK=!LOGBACK! -Dlogback.configurationFile=%DCSTOOL_USERDIR%\logback.xml"
) else (
  set "LOGBACK=!LOGBACK! -Dlogback.configurationFile=%APP_PATH%\logback.xml"
)

if exist "%DCSTOOL_USERDIR%\dep\" (
  set "CLASSPATH=!CLASSPATH!;%DCSTOOL_USERDIR%\dep\*"
  )

set "CLASSPATH=!CLASSPATH!;%APP_PATH%\dep\*"

echo before parsing ARGS: %*
call :ParseArgs %*

echo Parsed ARGS: !ARGS!
echo LOGBACK: !LOGBACK!
pause

java -Xms240m %DECJ_MAXHEAP% %DECJ_OPTS% -cp "!CLASSPATH!" -DDCSTOOL_HOME="%APP_PATH%" -DDECODES_INSTALL_DIR="%APP_PATH%" -DDCSTOOL_USERDIR="%DCSTOOL_USERDIR%" !LOGBACK! !ARGS!


:ParseArgs
set "LOG_FILE="
set "LOG_LEVEL=-DLOG_LEVEL=info"
set "ARGS=%1"
set "APP_NAME=%1"


:__parseLoop
if "%~1"=="" goto __parseDone

set "CURRENT_ARG=%~1"

echo "Parsing arg >: !CURRENT_ARG!"

:: -d level
if /I "!CURRENT_ARG!"=="-d" goto :HandleDebugLevel

:: -dlevel 
if not "!CURRENT_ARG!" == "" (
    if /I "!CURRENT_ARG:~0,2!"=="-d" goto :HandleDebugLevelNoSpace
)

:: -l file.log
if /I "!CURRENT_ARG!"=="-l" goto :HandleLog

:: -lfile.log
if not "!CURRENT_ARG!" == "" (
    if /I "!CURRENT_ARG:~0,2!"=="-l" goto :HandleLogNoSpace
)

if /I "!CURRENT_ARG!"=="-a" goto :HandleApp



:: echo "Adding to ARGS: !CURRENT_ARG!"
set "ARGS=!ARGS! !CURRENT_ARG!"
shift
goto __parseLoop

:HandleLog
set "LOG_FILE=-DLOG_FILE=%~2"
shift  
shift 
goto __parseLoop

:HandleLogNoSpace
set "LOGFILE=%~1"
::  trim off the -l
set "LOGFILE=%LOGFILE:~2%"
set "LOG_FILE=-DLOG_FILE=%LOGFILE%"
shift  
goto __parseLoop


:HandleApp
set "APP_NAME=%~2"
set "ARGS=!ARGS! -a %APP_NAME%"
shift
shift
echo args are now: !ARGS!
goto __parseLoop

:HandleDebugLevelNoSpace
:: -dlevel
set "x=%~1"
::  trim off the -d
set "x=%x:~2%"
set level=%x%
shift  
goto :MapLogLevel

:HandleDebugLevel
:: -d <level>
set "level=%~2"
shift
shift

:MapLogLevel
set "levelStr=INFO"
if "%level%"=="-2" set "levelStr=ERROR"
if "%level%"=="-1" set "levelStr=WARN"
if "%level%"=="0"  set "levelStr=INFO"
if "%level%"=="1"  set "levelStr=DEBUG"
if "%level%"=="2"  set "levelStr=TRACE"
if "%level%"=="3"  set "levelStr=TRACE"
set "LOG_LEVEL=-DLOG_LEVEL=!levelStr!"
goto __parseLoop

:__parseDone
  if defined LOGBACK_OVERRIDE (
    set "LOGBACK=%LOGBACK_OVERRIDE%"
  ) else (
    set "LOGBACK=%LOGBACK% -DAPP_NAME=%APP_NAME% %LOG_FILE% %LOG_LEVEL%"
  )
  exit /B

