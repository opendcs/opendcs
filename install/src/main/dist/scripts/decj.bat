@echo off
:: ============================================================================
:: OpenDCS Java Application Launcher
:: ============================================================================
::
:: Usage: decj.bat <classname> [options] [-- application args]
::
:: Arguments:
::   <classname>       Fully qualified Java class name to execute (required)
::
:: Options:
::   -d <level>        Set log level: -2=ERROR, -1=WARN, 0=INFO, 1=DEBUG, 2+=TRACE
::                     Can also be written as -d<level> (e.g., -d1)
::   -l <file>         Write logs to specified file
::                     Can also be written as -l<file> (e.g., -loutput.log)
::   -a <name>         Override application name (used in log identification)
::   -D<prop>=<value>  Pass system property to JVM (e.g., -Dmy.prop=value)
::
:: Environment Variables:
::   JAVA_HOME         Path to Java 21+ installation (required)
::   DCSTOOL_USERDIR   User config directory (default: %APPDATA%\.opendcs)
::   DECJ_MAXHEAP      Max heap size (e.g., -Xmx512m)
::   DECJ_OPTS         Additional JVM options
::   LOGBACK_OVERRIDE  Override all logback configuration
::
:: Examples:
::   decj.bat decodes.dbeditor.DecodesDbEditor
::   decj.bat decodes.dbeditor.DecodesDbEditor -d1 -lapp.log
::   decj.bat decodes.dbeditor.DecodesDbEditor -DLOG_DIR=c:\tmp
::
:: ============================================================================
setLocal EnableDelayedExpansion

:: Configuration
set "MIN_JAVA_VERSION=21"
set "DEFAULT_HEAP=-Xms240m"
set "DEBUG_DECJ=1"

:: Initialize paths
call :InitializePaths

:: Setup environment
call :InitializeUserDir
call :SetupClasspath
call :SetupLogback

:: Parse command line arguments
call :ParseArgs %*

:: Verify Java and launch
call :ValidateJavaHome || exit /B 1
call :VerifyJava || exit /B 1
call :LaunchApplication

exit /B %ERRORLEVEL%

:: ============================================================================
:: Functions
:: ============================================================================

:InitializePaths
    set "BIN_PATH=%~dp0"
    pushd "%BIN_PATH%.."
    set "APP_PATH=%CD%"
    popd
    if not defined DCSTOOL_USERDIR set "DCSTOOL_USERDIR=%APPDATA%\.opendcs"
    exit /B 0

:InitializeUserDir
    if exist "%DCSTOOL_USERDIR%\" exit /B 0
    call :LogStderr "Creating Local User Directory and initial properties in %DCSTOOL_USERDIR%"
    call :LogStderr "The default XML database has been copied to this directory."
    mkdir "%DCSTOOL_USERDIR%"
    copy "%APP_PATH%\decodes.properties" "%DCSTOOL_USERDIR%\user.properties" >nul
    xcopy "%APP_PATH%\edit-db" "%DCSTOOL_USERDIR%\edit-db" /E /I /Q >nul
    exit /B 0

:SetupClasspath
    set "CLASSPATH=%BIN_PATH%opendcs.jar;%BIN_PATH%hibernate.cfg.xml"

    :: Handle deprecated CP_SHARED_JAR_DIR
    if defined CP_SHARED_JAR_DIR (
        if exist "%CP_SHARED_JAR_DIR%" (
            set "CLASSPATH=!CLASSPATH!;%CP_SHARED_JAR_DIR%\*"
            call :LogStderr "The need of CP_SHARED_JAR_DIR is superseded by using a DCSTOOL_USERDIR for configuration."
            call :LogStderr "Support for this variable will be removed in a future release. Please update your configuration."
        ) else (
            call :LogStderr "Directory %CP_SHARED_JAR_DIR% does not exist."
        )
    )

    :: Add dependency directories
    if exist "%DCSTOOL_USERDIR%\dep\" set "CLASSPATH=!CLASSPATH!;%DCSTOOL_USERDIR%\dep\*"
    set "CLASSPATH=!CLASSPATH!;%APP_PATH%\dep\*"
    exit /B 0

:SetupLogback
    set "LOGBACK="
    if exist "%DCSTOOL_USERDIR%\logback.xml" (
        set "LOGBACK=-Dlogback.configurationFile=%DCSTOOL_USERDIR%\logback.xml"
    ) else (
        set "LOGBACK=-Dlogback.configurationFile=%APP_PATH%\logback.xml"
    )
    exit /B 0

:ValidateJavaHome
    :: Ensures JAVA_HOME is set and points to a valid Java installation
    if not defined JAVA_HOME (
        call :LogStderr "ERROR: JAVA_HOME environment variable is not set."
        call :LogStderr "Please set JAVA_HOME to a Java %MIN_JAVA_VERSION%+ installation."
        exit /B 1
    )
    if not exist "%JAVA_HOME%\bin\java.exe" (
        call :LogStderr "ERROR: Java executable not found at %JAVA_HOME%\bin\java.exe"
        call :LogStderr "Please verify JAVA_HOME is set correctly."
        exit /B 1
    )
    exit /B 0

:VerifyJava
    set "_JAVA_CMD=%JAVA_HOME%\bin\java"
    set "_TMPFILE=%TEMP%\java_version_%RANDOM%.txt"
    "!_JAVA_CMD!" -version 2>"!_TMPFILE!"
    for /f "tokens=3" %%i in ('type "!_TMPFILE!" ^| findstr /i "version"') do (
        set "JAVA_VERSION=%%~i"
    )
    del "!_TMPFILE!" 2>nul
    for /f "tokens=1 delims=." %%a in ("!JAVA_VERSION!") do set "JAVA_MAJOR=%%a"
    if not defined JAVA_MAJOR set "JAVA_MAJOR=0"
    if !JAVA_MAJOR! LSS %MIN_JAVA_VERSION% (
        call :LogStderr "ERROR: Java %MIN_JAVA_VERSION% or higher is required. Found version !JAVA_VERSION!"
        call :LogStderr "Please set JAVA_HOME to a Java %MIN_JAVA_VERSION%+ installation."
        exit /B 1
    )
    exit /B 0

:LaunchApplication
    if defined DEBUG_DECJ (
        echo [DEBUG] === Launching Application ===
        echo [DEBUG] JAVA_HOME: %JAVA_HOME%
        echo [DEBUG] Full command:
        echo [DEBUG]   java %DEFAULT_HEAP% %DECJ_MAXHEAP% %DECJ_OPTS%
        echo [DEBUG]   JVM_ARGS: !JVM_ARGS!
        echo [DEBUG]   -cp "!CLASSPATH!"
        echo [DEBUG]   LOGBACK: !LOGBACK!
        echo [DEBUG]   ARGS: !ARGS!
    )
    else (
    "%JAVA_HOME%\bin\java" %DEFAULT_HEAP% %DECJ_MAXHEAP% %DECJ_OPTS% ^
        !JVM_ARGS! ^
        -cp "!CLASSPATH!" ^
        -DDCSTOOL_HOME="%APP_PATH%" ^
        -DDECODES_INSTALL_DIR="%APP_PATH%" ^
        -DDCSTOOL_USERDIR="%DCSTOOL_USERDIR%" ^
        !LOGBACK! !ARGS!
    exit /B %ERRORLEVEL%
    )

:: ----------------------------------------------------------------------------
:: Argument Parsing
:: ----------------------------------------------------------------------------

:ParseArgs
    set "LOG_FILE="
    set "LOG_LEVEL=-DLOG_LEVEL=INFO"
    set "JVM_ARGS="
    set "ARGS=%1"
    set "APP_NAME=%1"
    shift

:_ParseLoop
    if "%~1"=="" goto :_ParseDone
    set "CURRENT_ARG=%~1"
    if defined DEBUG_DECJ echo [DEBUG] Processing arg: [%1] CURRENT_ARG=[!CURRENT_ARG!]

    :: Check for each argument type using prefix matching
    :: Note: -D must be checked before -d to avoid -DFOO matching debug flag
    call :StartsWithPrefix "!CURRENT_ARG!" "-D" 2 && goto :_HandleSysProp
    call :StartsWithPrefix "!CURRENT_ARG!" "-d" 2 && goto :_HandleDebug
    call :StartsWithPrefix "!CURRENT_ARG!" "-l" 2 && goto :_HandleLog
    if "!CURRENT_ARG!"=="-a" goto :_HandleApp

    if defined DEBUG_DECJ echo [DEBUG] Adding to ARGS: !CURRENT_ARG!
    set "ARGS=!ARGS! !CURRENT_ARG!"
    shift
    goto :_ParseLoop

:_HandleDebug
    call :ExtractValue "!CURRENT_ARG!" "-d" 2
    if "!EXTRACTED_VALUE!"=="" (
        set "EXTRACTED_VALUE=%~2"
        shift
    )
    call :MapLogLevel "!EXTRACTED_VALUE!"
    set "LOG_LEVEL=-DLOG_LEVEL=!LOG_LEVEL_RESULT!"
    shift
    goto :_ParseLoop

:_HandleLog
    call :ExtractValue "!CURRENT_ARG!" "-l" 2
    if "!EXTRACTED_VALUE!"=="" (
        set "EXTRACTED_VALUE=%~2"
        shift
    )
    set "LOG_FILE=-DLOG_FILE=!EXTRACTED_VALUE!"
    shift
    goto :_ParseLoop

:_HandleSysProp
    :: Preserve the full -D argument - reassemble if split on =
    :: CMD splits args on = so -DKEY=VALUE becomes two args: -DKEY and VALUE
    set "_sysprop=%~1"
    shift
    :: Check if next arg exists and current doesn't contain = (was split)
    if not "%~1"=="" (
        echo "!_sysprop!" | findstr "=" >nul
        if errorlevel 1 (
            :: No = found, so reassemble with next arg
            set "_sysprop=!_sysprop!=%~1"
            shift
        )
    )
    if defined DEBUG_DECJ echo [DEBUG] _HandleSysProp: reassembled=[!_sysprop!] adding to JVM_ARGS
    set "JVM_ARGS=!JVM_ARGS! !_sysprop!"
    if defined DEBUG_DECJ echo [DEBUG] JVM_ARGS is now: [!JVM_ARGS!]
    goto :_ParseLoop

:_HandleApp
    set "APP_NAME=%~2"
    shift & shift
    goto :_ParseLoop

:_ParseDone
    if defined LOGBACK_OVERRIDE (
        set "LOGBACK=%LOGBACK_OVERRIDE%"
    ) else (
        set "LOGBACK=!LOGBACK! -DAPP_NAME=%APP_NAME% %LOG_FILE% %LOG_LEVEL%"
    )
    if defined DEBUG_DECJ (
        echo [DEBUG] === Parse Complete ===
        echo [DEBUG] ARGS: [!ARGS!]
        echo [DEBUG] JVM_ARGS: [!JVM_ARGS!]
        echo [DEBUG] LOGBACK: [!LOGBACK!]
        echo [DEBUG] APP_NAME: [%APP_NAME%]
    )
    exit /B 0

:: ----------------------------------------------------------------------------
:: Utility Functions
:: ----------------------------------------------------------------------------

:MapLogLevel
    :: Maps numeric log level to string: -2=ERROR, -1=WARN, 0=INFO, 1=DEBUG, 2+=TRACE
    :: Usage: call :MapLogLevel "level"
    :: Result returned in LOG_LEVEL_RESULT variable
    set "_level=%~1"
    set "LOG_LEVEL_RESULT=INFO"
    if "!_level!"=="-2" set "LOG_LEVEL_RESULT=ERROR"
    if "!_level!"=="-1" set "LOG_LEVEL_RESULT=WARN"
    if "!_level!"=="0"  set "LOG_LEVEL_RESULT=INFO"
    if "!_level!"=="1"  set "LOG_LEVEL_RESULT=DEBUG"
    if "!_level!" GEQ 2 set "LOG_LEVEL_RESULT=TRACE"
    exit /B 0

:StartsWithPrefix
    :: Checks if %1 starts with prefix %2, returns 0 if true
    set "_str=%~1"
    set "_prefix=%~2"
    set "_len=%~3"
    if "!_str:~0,%_len%!"=="!_prefix!" exit /B 0
    exit /B 1

:ExtractValue
    :: Extracts value after prefix from argument (e.g., -d3 -> 3, -lfile.log -> file.log)
    set "_arg=%~1"
    set "_prefixLen=%~3"
    set "EXTRACTED_VALUE=!_arg:~%_prefixLen%!"
    exit /B 0

:LogStderr
    echo %~1 1>&2
    exit /B 0
