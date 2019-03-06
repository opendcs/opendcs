Before running any of the update scripts: Edit the file defines.sql and make sure 
the settings are correct for this database.


Instructions for updating HDB from CP 5.2 to OpenDCS 6.3:

1. Execute the DDL script cp52to63.sql as the CP_OWNER.

2. Execute the DDL script decodes52to63.sql as the DECODES_OWNER.

3. Run:
	dbimport HdbUnits.xml

Instructions for updating HDB from 6.3 to 6.4:

1. Execute the DDL script decodes63to64.sql as the DECODES_OWNER.

2. Execute the DDL script cp63to64.sql as the CP_OWNER.


Instructions for updating HDB from 6.4 to 6.6:

1. Execute the DDL script alarm.sql as the CP_OWNER.
