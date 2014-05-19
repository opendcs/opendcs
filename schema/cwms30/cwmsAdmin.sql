---------------------------------------------------------------------------
-- Perform stuff as CWMS_20
-- Maintainer: Cove Software, LLC
---------------------------------------------------------------------------
set echo on
spool cwmsAdmin.log

whenever sqlerror continue
set define on
@@defines.sql

---------------------------------------------------------------------------
-- This file is executed as CWMS_20
---------------------------------------------------------------------------

--DECLARE
--  l_sqlstr   varchar2(200);
BEGIN
-- this is a kludge. In order to register callbacks, the ts api requires that
-- the caller be registered in an office as a CWMS user.
-- This call registers 'CCP' as a user in the SPA office.
  &CWMS_SCHEMA..cwms_sec.create_user('CCP',NULL,cwms_20.char_32_array_type('CWMS PD Users'), 'SPA');
END;
/

spool off
exit;

