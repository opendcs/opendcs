@echo off
rem 
rem  pxport - Export platform entities to XML
rem 
rem  usage: pxport <options>
rem  options:
rem 		-n <netlist>     Export platforms specified by network list.
rem 		-s <site>        Export platform corresponding to specific site.
rem 		-a               Export all platforms
rem 		-c <configname>  Export platforms using a specific configuration.
rem 		-i               Export from 'installed' database (default is edit dbrem 
rem 
$INSTALL_PATH\bin\decj decodes.dbimport.PlatformExport %*%
