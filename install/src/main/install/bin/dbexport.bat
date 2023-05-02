@echo off
rem 
rem  dbxport - Export entire database to XML
rem 
rem  usage: dbxport <options>
rem  options:
rem 		-i               Export from 'installed' database (default is edit dbrem 
rem 
$INSTALL_PATH\bin\decj decodes.dbimport.DbExport %*%
