rem
rem This script is to be placed in the $DCSTOOL/bin directory.
rem It is a cygwin bash shell script which must be run as follows on Windows machines:
rem
rem	bash.exe sfwmd-import <options ...>
rem

rem Supported options:
rem
rem -G			Import GOES Platforms Only
rem -I			Ignore designators (match site name only)
rem -L			Preserve location (site & platform description) info
rem -F			Employ SFWMD rules and sensor code suffixes
rem -S			Preserve additional sensor info
rem -U			Update only (don't take any new platforms)
rem -O owner		Set the platform owner to 'owner'
rem -N            Only import platforms that have a Newer last-modify-date.
rem -M updated.out Make a list of platforms that were updated in 'updated.out'.
rem
"%~dp0\decj" com.ilexeng.decodes.dbimport.ImportXml -G -I -U -O SFMWD -L -F -S -N -M updated.out %*%
