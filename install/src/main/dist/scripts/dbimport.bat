@echo off
rem
rem dbimport - Import XML files into this editable database.
rem
rem usage: dbimport <options>
rem options:
rem	-v             Validate only. Issue messages about what would
rem	               have been done if the files were imported.
rem	-o             Keep OLD records in case of a clash (i.e. don't overwrite
rem	               existing records with the contents of the XML files.
rem
"%~dp0\decj" decodes.dbimport.DbImport %*%
