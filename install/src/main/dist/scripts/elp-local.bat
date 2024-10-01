@echo off
if not defined LRGSHOME (
    echo Error: Environment variable LRGSHOME is not set.
    exit /b 1
    )

@"%~dp0\decj" ilex.util.PasswordFileEditor -f %LRGSHOME%/.lrgs.passwd.local