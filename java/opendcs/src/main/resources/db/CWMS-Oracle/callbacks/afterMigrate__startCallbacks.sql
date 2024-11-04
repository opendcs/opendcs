---------------------------------------------------------------------------
-- Start the check callback procedure job
-- NOTE: not to be confused with the flyway callbacks
---------------------------------------------------------------------------
begin
  -- in 'addCcpUser.sql' we registered user CCP in the default office.
  -- This is a kludge: TS API requires caller to be registered in an office.
--  ${CWMS_SCHEMA}.cwms_env.set_session_office_id('&DEFAULT_OFFICE_ID');
  ${CCP_SCHEMA}.cwms_ccp.reload_callback_proc(
    'CCP_SUBSCRIBER');

  ${CCP_SCHEMA}.cwms_ccp.start_check_callback_proc_job;
end;
/