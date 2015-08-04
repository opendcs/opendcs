
------------------------------------------------------------------------------
-- Execute this script as user CCP so that the changes will effect
-- records for all offices.
------------------------------------------------------------------------------

-- This corrects the coefficient for converting psi to pa:
update ccp.unitconverter set A = 6894.74729 where lower(fromunitsabbr) 
	= 'psi' and lower(tounitsabbr) = 'pa';

-- Increase NetworkListEntry.PLATFORM_NAME to 64 chars
ALTER TABLE CCP.NETWORKLISTENTRY MODIFY PLATFORM_NAME VARCHAR2(64);

-- Mark the new database version so that the Java code knows about it.
delete from ccp.DecodesDatabaseVersion;
insert into ccp.DecodesDatabaseVersion values(12, 'OpenDCS 6.1 RC11');
