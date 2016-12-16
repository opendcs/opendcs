Instructions for updating HDB from CP 5.2 to OpenDCS 6.3:

1. Edit the file defines.sql and make sure the settings are correct for this database.

2. Execute the DDL script cp52to63.sql as the CP_OWNER.

3. Execute the DDL script decodes52to63.sql as the DECODES_OWNER.

5. Run:
	dbimport HdbUnits.xml
