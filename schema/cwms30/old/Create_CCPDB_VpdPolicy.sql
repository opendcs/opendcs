---------------------------------------------------------------------------
--
-- Sutron Ilex CCP Database on ORACLE
-- Database Version: 8        Date: 2013/05/02
-- Company: Sutron Corporation
--  Writer: GC
--
---------------------------------------------------------------------------
set echo on
spool create_CCPDB_VpdPolicy.out

whenever sqlerror continue
set define on
@@defines.sql

define sys_schema     = &1
define sys_passwd     = &2
define tns_name       = &3

--connect &sys_schema/&sys_passwd@&tns_name as sysdba;
--alter session set current_schema = &ccp_schema;

connect &ccp_schema/&ccp_passwd@&tns_name;

---------------------------------------------------------------------------
-- Create the VPD policy for the CCP table objects
---------------------------------------------------------------------------
begin
  for rec in (
    select table_name from user_tab_columns where column_name in ('DB_OFFICE_CODE')
    )
  loop
    begin
      DBMS_RLS.drop_policy (
        object_schema       => '&ccp_schema',
        object_name         => rec.table_name,
        policy_name         => 'plcy_cwms_ccp_office_v'
      );
      DBMS_RLS.drop_policy (
        object_schema       => '&ccp_schema',
        object_name         => rec.table_name,
        policy_name         => 'plcy_cwms_ccp_office_u'
      );
    exception
      when others then null;
    end;

    DBMS_RLS.add_policy (
      object_schema       => '&ccp_schema',
      object_name         => rec.table_name,
      policy_name         => 'plcy_cwms_ccp_office_v',
      function_schema     => '&ccp_schema',
      policy_function     => 'cwms_ccp_vpd.get_pred_session_office_code_v',
      statement_types     => 'select'
    );

    DBMS_RLS.add_policy (
      object_schema       => '&ccp_schema',
      object_name         => rec.table_name,
      policy_name         => 'plcy_cwms_ccp_office_u',
      function_schema     => '&ccp_schema',
      policy_function     => 'cwms_ccp_vpd.get_pred_session_office_code_u',
      statement_types     => 'insert, update, delete'
    );

  end loop;
end;
/


spool off
exit;
