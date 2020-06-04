@echo off
rem
rem usage: 
rem	markproduction
rem		Sets all isProduction flags in the database to true
rem	markproduction false
rem		Sets all isProduction flags in the database to false
rem
$INSTALL_PATH\bin\decj decodes.db.MarkProduction %*%
