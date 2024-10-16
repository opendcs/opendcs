-----------------------------------------------------------------------------
-- This software was written by Cove Software, LLC ("COVE") under contract 
-- to the United States Government. 
-- No warranty is provided or implied other than specific contractual terms
-- between COVE and the U.S. Government
-- 
-- Copyright 2014 U.S. Army Corps of Engineers, Hydrologic Engineering Center.
-- All rights reserved.
-----------------------------------------------------------------------------
---------------------------------------------------------------------------
-- CCP CWMS Database
-- Maintainer: Cove Software, LLC
-- Last Modified: March 5, 2014
---------------------------------------------------------------------------

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
        object_schema       => '${CCP_SCHEMA}',
        object_name         => rec.table_name,
        policy_name         => 'plcy_cwms_ccp_office_v'
      );
      DBMS_RLS.drop_policy (
        object_schema       => '&${CCP_SCHEMA}',
        object_name         => rec.table_name,
        policy_name         => 'plcy_cwms_ccp_office_u'
      );
    exception
      when others then null;
    end;

    DBMS_RLS.add_policy (
      object_schema       => '${CCP_SCHEMA}',
      object_name         => rec.table_name,
      policy_name         => 'plcy_cwms_ccp_office_v',
      function_schema     => '${CCP_SCHEMA}',
      policy_function     => 'cwms_ccp_vpd.get_pred_session_office_code_v',
      statement_types     => 'select'
    );

    DBMS_RLS.add_policy (
      object_schema       => '${CCP_SCHEMA}',
      object_name         => rec.table_name,
      policy_name         => 'plcy_cwms_ccp_office_u',
      function_schema     => '${CCP_SCHEMA}',
      policy_function     => 'cwms_ccp_vpd.get_pred_session_office_code_u',
      statement_types     => 'update, delete'
    );

  end loop;
end;
/
