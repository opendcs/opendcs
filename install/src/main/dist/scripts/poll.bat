@echo off
rem 
rem Poll a Modem or TCP Station
rem
rem  Usage: poll [-S sincetime] sitename
rem 
"%~dp0\decj" decodes.routing.Poll %*%
