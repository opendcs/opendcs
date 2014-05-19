--------------------------------------------------------------------------
-- Create roles for CCP CWMS 3.0
-- Maintainer: Cove Software, LLC
--------------------------------------------------------------------------
set echo on
spool roles.log

whenever sqlerror continue
set define on
@@defines.sql

DROP USER &CCP_SCHEMA CASCADE;

-- adm_role:  DB admin_role who can manipulate DB objects and data
DROP ROLE CCP_ADMS;
CREATE ROLE CCP_ADMS;

-- Grant the system privileges to the CCP_ADMS.
GRANT ALTER ANY TABLE,CREATE ANY TABLE,CREATE ANY INDEX,CREATE ANY SEQUENCE,
  CREATE ANY VIEW,CREATE ANY PROCEDURE,CREATE ANY TRIGGER,CREATE ANY JOB,
  CREATE ANY SYNONYM,DROP ANY SYNONYM,CREATE PUBLIC SYNONYM,DROP PUBLIC SYNONYM
  TO CCP_ADMS;
GRANT CREATE ANY CONTEXT,ADMINISTER DATABASE TRIGGER TO CCP_ADMS;

-- user_role:     DB user_role who can access the CWMS/CCP DB and APIs
DROP ROLE CCP_USERS;
CREATE ROLE CCP_USERS;

GRANT CREATE SESSION,RESOURCE,CONNECT TO CCP_USERS;

GRANT CCP_USERS TO CCP_ADMS;
GRANT CCP_USERS TO CWMS_USER;

-- MJM In CWMS 3.0, read/write privileges will be controlled by office
-- privileges and checked by the VPD policy functions.
-- Therefore, we don't need the following Oracle roles:
-- DROP ROLE "CCP_USERS._R";
-- DROP ROLE "CCP_USERS._W";
-- DROP ROLE "CCP_USERS._P";
-- CREATE ROLE "CCP_USERS._R";
-- CREATE ROLE "CCP_USERS._W";
-- CREATE ROLE "CCP_USERS._P";

---------------------------------------------------------------------------
-- Create the schema user and grant the privileges and permissions.
---------------------------------------------------------------------------
-- Create the DB schema user
DROP USER &CCP_SCHEMA CASCADE;
CREATE USER &CCP_SCHEMA IDENTIFIED BY &CCP_PASSWD
DEFAULT TABLESPACE &TBL_SPACE_DATA QUOTA UNLIMITED ON &TBL_SPACE_DATA
TEMPORARY TABLESPACE &TBL_SPACE_TEMP
PROFILE DEFAULT
ACCOUNT UNLOCK;

-- Grant the privileges and permissions to the &CCP_SCHEMA.
GRANT CCP_ADMS TO &CCP_SCHEMA WITH ADMIN OPTION;
GRANT CWMS_USER TO &CCP_SCHEMA;

-- Grant the aq object permissions to the &CCP_SCHEMA
GRANT SELECT ON dba_scheduler_jobs to &CCP_SCHEMA;
GRANT SELECT ON dba_queue_subscribers to &CCP_SCHEMA;
GRANT SELECT ON dba_subscr_registrations to &CCP_SCHEMA;
GRANT SELECT ON dba_queues to &CCP_SCHEMA;
GRANT EXECUTE ON dbms_aq TO &CCP_SCHEMA;
GRANT EXECUTE ON dbms_aqadm TO &CCP_SCHEMA;
-- Grant the vpd privileges to the &CCP_SCHEMA.
GRANT EXECUTE ON DBMS_SESSION to &CCP_SCHEMA;
GRANT EXECUTE ON DBMS_RLS to &CCP_SCHEMA;
-- Grant the aqadm privileges to the &CCP_SCHEMA.
BEGIN
  sys.dbms_aqadm.grant_system_privilege (
    privilege    => 'enqueue_any',
    grantee      => '&CCP_SCHEMA',
    admin_option => false);
  sys.dbms_aqadm.grant_system_privilege (
    privilege    => 'dequeue_any',
    grantee      => '&CCP_SCHEMA',
    admin_option => false);
  sys.dbms_aqadm.grant_system_privilege (
    privilege    => 'manage_any',
    grantee      => '&CCP_SCHEMA',
    admin_option => false);
END;
/
-- Grant the permissions on cwms tables, views, and packages to the &CCP_SCHEMA
GRANT SELECT ON cwms_v_loc TO &CCP_SCHEMA WITH GRANT OPTION;
GRANT SELECT ON cwms_v_ts_id TO &CCP_SCHEMA WITH GRANT OPTION;
GRANT SELECT ON cwms_v_tsv TO &CCP_SCHEMA;
GRANT SELECT ON cwms_20.cwms_seq TO &CCP_SCHEMA;
GRANT SELECT ON cwms_20.cwms_seq TO CCP_USERS;

GRANT EXECUTE ON cwms_t_date_table TO &CCP_SCHEMA;
GRANT EXECUTE ON cwms_t_jms_map_msg_tab TO &CCP_SCHEMA;

GRANT EXECUTE ON &CWMS_SCHEMA..cwms_ts TO &CCP_SCHEMA;
GRANT EXECUTE ON &CWMS_SCHEMA..cwms_msg TO &CCP_SCHEMA;
GRANT EXECUTE ON &CWMS_SCHEMA..cwms_util TO &CCP_SCHEMA;
GRANT EXECUTE ON &CWMS_SCHEMA..cwms_sec TO &CCP_SCHEMA;

GRANT EXECUTE ON &CWMS_SCHEMA..cwms_env TO &CCP_SCHEMA;
GRANT EXECUTE ON &CWMS_SCHEMA..cwms_env TO CCP_USERS;

-- Grant the permissions on cwms tables to the &CCP_SCHEMA for multiple office
-- GRANT SELECT ON &CWMS_SCHEMA..at_sec_user_office TO &CCP_SCHEMA;

ALTER USER &CCP_SCHEMA DEFAULT ROLE ALL;

spool off
exit;
