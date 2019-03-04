Instructions for installing CCP schema on top of a new CWMS Database
Last Modified $Date$

1. Install OPENDCS on the machine where sqlplus is available for your
   database. For example, if the release is 6.6 RC03, install with:
        java -jar opendcs-cwms-6-6-RC03.jar

   If X11 is not available add the argument -console to the end of the
   command:
	java -jar opendcs-cwms-6-6-RC03.jar -console

2. Set an environment variable pointing to the installation directory you
   chose:
        export DCSTOOL_HOME=insert your choice here

3. Under the OPENDCS installation find the directory 
       schema/cwms

   This directory contains scripts you will need to install CCP components 
   on a CWMS database. It also contains this README.txt file.

   CD to this directory. I.e.:
        cd $DCSTOOL_HOME/schema/cwms

   The installer may leave some of the executable scripts without the
   execute bit. Run the script:
       chmod 750 *.sh

4. Edit the file 'defines.sh'. Make settings appropriate for the database
   you will be installing. See comments there for each variable.

5. Run the script:
        createDefinesSql.sh

   This creates defines.sql, which is a SQL version of the defines you set.
   View this file and verify the contents.

6. Create the CCP tablespaces. For this you will need to run sqlplus as
   SYSDBA on the database you are creating. Once in sqlplus, execute
       @tablespace.sql

7. Define roles and permissions. While still running sqlplus as SYSDBA,
   execute the following:
       @roles.sql

   Exit from sqlplus.

8. A few items must be done as user CWMS_20. Run sqlplus as user CWMS_20
   and then execute:
        @cwmsAdmin.sql

   Exit from sqlplus.

9. Now run the script:
        installCCP.sh
   This script will use the defines you set to install the CCP and DECODES
   components under the CCP schema. It runs sqlplus several times, executing
   various SQL scripts from this directory.

10. DECODES template. At the end of the installCCP.sh script, take note of the
    choices for creating the DECODES template. This must be done for each
    district that will be using this CWMS database.

