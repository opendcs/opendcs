@echo off
rem
rem platformxref - Create platform cross reference file
rem
rem usage: platformxref
rem options:
rem		-e   Create cross reference in editable database (default = installed)
rem
decj decodes.xml.CreatePlatformXref %*%
