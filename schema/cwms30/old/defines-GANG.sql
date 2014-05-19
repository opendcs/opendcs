----------------------------------------------------------------------------
-- Definitions for installing CCP Schema into CWMS 3.0
----------------------------------------------------------------------------
undefine sys_schema;
undefine sys_passwd;
undefine tns_name;
undefine ts_data_name;
undefine ts_data_file;
undefine ts_temp_name;
undefine ts_temp_file;
undefine ts_name;
undefine adm_role;
undefine user_role;
undefine cwms_schema;
undefine ccp_schema;
undefine ccp_passwd;
undefine user_schema;
undefine user_passwd;
undefine queue_subscriber;
undefine queue_name;
undefine callback_proc;
undefine dflt_office_code;

-- SYS user name is almost always 'SYS'
define sys_schema       = sys;

-- Enter password for SYS here. You can remove it when you're finished.
define sys_passwd       = xxxxxx;

-- Enter the SID (a.k.a. TNS name) for the database
define tns_name         = cwms22m;

-- The user that owns the CWMS schema:
define cwms_schema      = CWMS_20

-- Table space definitions: Leave the names alone, but modify the file paths
-- so they are appropriate for your system:
define ts_data_name     = CCP_DATA;
define ts_data_file     = '/opt/app/oracle/oradata/cwms22m/CCP_DATA1.dbf';
define ts_temp_name     = CCP_TEMP;
define ts_temp_file     = '/opt/app/oracle/oradata/cwms22m/CCP_TEMP.dbf';

-- This is a suffix used in table references. Do not change it!
define ts_name          = 'tablespace &ts_data_name';

-- Two roles control access to CCP. Do not change these names!
define adm_role         = CCP_ADMS;
define user_role        = CCP_USERS;

-- The user that will own the CCP schema:
define ccp_schema       = CCP;
define ccp_passwd       = CCP;

-- A user account for testing. By default no test users are created.
define user_schema      = ccpuser;
define user_passwd      = ccpuser;

-- Defines for queue access:
define queue_subscriber = CCP_SUBSCRIBER;
define queue_name       = &cwms_schema..cwms_sec.get_user_office_id||'_TS_STORED';
define callback_proc    = &ccp_schema..CWMS_CCP.NOTIFY_FOR_COMP;

-- Default office code definition:
define dflt_office_code = sys_context('CCPENV','CCP_OFFICE_CODE');
