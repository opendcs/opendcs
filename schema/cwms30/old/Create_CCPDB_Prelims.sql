---------------------------------------------------------------------------
-- CCP Database on ORACLE
-- Database Version: 9 Last Modified: 2014/02/28
-- Maintainer: Cove Software, LLC
---------------------------------------------------------------------------
set echo on
spool create_CCPDB_Prelims.out

whenever sqlerror continue
set define on
@@defines.sql

define sys_schema     = &1
define sys_passwd     = &2
define tns_name       = &3

connect &sys_schema/&sys_passwd@&tns_name as sysdba;

---------------------------------------------------------------------------
-- create the tablespaces
---------------------------------------------------------------------------
DROP TABLESPACE &ts_data_name
  INCLUDING CONTENTS AND DATAFILES CASCADE CONSTRAINTS;
CREATE TABLESPACE &ts_data_name
  DATAFILE '&ts_data_file'
  SIZE 100M REUSE AUTOEXTEND ON NEXT 1M MAXSIZE 2000M
;
DROP TABLESPACE &ts_temp_name
  INCLUDING CONTENTS AND DATAFILES CASCADE CONSTRAINTS;
CREATE TEMPORARY TABLESPACE &ts_temp_name
  TEMPFILE '&ts_temp_file'
  SIZE 20M REUSE AUTOEXTEND ON NEXT 1M MAXSIZE 200M
;

---------------------------------------------------------------------------
-- Create the DB roles
---------------------------------------------------------------------------
DROP USER &ccp_schema CASCADE;

-- adm_role:  DB admin_role who can manipulate DB objects and data
DROP ROLE &adm_role;
CREATE ROLE &adm_role;

-- Grant the system privileges to the adm_role.
GRANT ALTER ANY TABLE,CREATE ANY TABLE,CREATE ANY INDEX,CREATE ANY SEQUENCE,
  CREATE ANY VIEW,CREATE ANY PROCEDURE,CREATE ANY TRIGGER,CREATE ANY JOB,
  CREATE ANY SYNONYM,DROP ANY SYNONYM,CREATE PUBLIC SYNONYM,DROP PUBLIC SYNONYM
  TO &adm_role;
GRANT CREATE ANY CONTEXT,ADMINISTER DATABASE TRIGGER TO &adm_role;

-- user_role:     DB user_role who can access the CWMS/CCP DB and APIs
DROP ROLE &user_role;
CREATE ROLE &user_role;

GRANT CREATE SESSION,RESOURCE,CONNECT TO &user_role;

GRANT &user_role TO &adm_role;
GRANT &user_role TO CWMS_USER;

-- other CCP roles for reading, writing, and processing the CCP data
DROP ROLE "&user_role._R";
DROP ROLE "&user_role._W";
DROP ROLE "&user_role._P";
CREATE ROLE "&user_role._R";
CREATE ROLE "&user_role._W";
CREATE ROLE "&user_role._P";

---------------------------------------------------------------------------
-- Create the schema user and grant the privileges and permissions.
---------------------------------------------------------------------------
-- Create the DB schema user
--DROP USER &ccp_schema CASCADE;
CREATE USER &ccp_schema IDENTIFIED BY &ccp_passwd
DEFAULT TABLESPACE &ts_data_name QUOTA UNLIMITED ON &ts_data_name
TEMPORARY TABLESPACE &ts_temp_name
PROFILE DEFAULT
ACCOUNT UNLOCK;

-- Grant the privileges and permissions to the &ccp_schema.
GRANT &adm_role TO &ccp_schema WITH ADMIN OPTION;
GRANT CWMS_USER TO &ccp_schema;
-- Grant the aq object permissions to the &ccp_schema
GRANT SELECT ON dba_scheduler_jobs to &ccp_schema;
GRANT SELECT ON dba_queue_subscribers to &ccp_schema;
GRANT SELECT ON dba_subscr_registrations to &ccp_schema;
GRANT SELECT ON dba_queues to &ccp_schema;
GRANT EXECUTE ON dbms_aq TO &ccp_schema;
GRANT EXECUTE ON dbms_aqadm TO &ccp_schema;
-- Grant the vpd privileges to the &ccp_schema.
GRANT EXECUTE ON DBMS_SESSION to &ccp_schema;
GRANT EXECUTE ON DBMS_RLS to &ccp_schema;
-- Grant the aqadm privileges to the &ccp_schema.
BEGIN
  sys.dbms_aqadm.grant_system_privilege (
    privilege    => 'enqueue_any',
    grantee      => '&ccp_schema',
    admin_option => false);
  sys.dbms_aqadm.grant_system_privilege (
    privilege    => 'dequeue_any',
    grantee      => '&ccp_schema',
    admin_option => false);
  sys.dbms_aqadm.grant_system_privilege (
    privilege    => 'manage_any',
    grantee      => '&ccp_schema',
    admin_option => false);
END;
/
-- Grant the permissions on cwms tables, views, and packages to the &ccp_schema
GRANT SELECT ON cwms_v_loc TO &ccp_schema WITH GRANT OPTION;
GRANT SELECT ON cwms_v_ts_id TO &ccp_schema WITH GRANT OPTION;
GRANT SELECT ON cwms_v_tsv TO &ccp_schema;

GRANT EXECUTE ON cwms_t_date_table TO &ccp_schema;
GRANT EXECUTE ON cwms_t_jms_map_msg_tab TO &ccp_schema;

GRANT EXECUTE ON &cwms_schema..cwms_ts TO &ccp_schema;
GRANT EXECUTE ON &cwms_schema..cwms_msg TO &ccp_schema;
GRANT EXECUTE ON &cwms_schema..cwms_util TO &ccp_schema;
GRANT EXECUTE ON &cwms_schema..cwms_sec TO &ccp_schema;

-- Grant the permissions on cwms tables to the &ccp_schema for multiple office
GRANT SELECT ON &cwms_schema..at_sec_user_office TO &ccp_schema;

ALTER USER &ccp_schema DEFAULT ROLE ALL;

spool off
exit;
