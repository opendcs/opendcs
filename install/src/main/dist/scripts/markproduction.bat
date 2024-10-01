@echo off
rem
rem usage: 
rem	markproduction
rem		Sets all isProduction flags in the database to true
rem	markproduction false
rem		Sets all isProduction flags in the database to false
rem
"%~dp0\decj" decodes.db.MarkProduction %*%
