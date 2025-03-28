---------------------------------------------------------------------------
-- Start the check callback procedure job
-- NOTE: not to be confused with the flyway callbacks
---------------------------------------------------------------------------
begin
  -- This is a kludge: TS API requires caller to be registered in an office.    
  -- This kludge does not work if not run as the CCP user or an actual user.
  --cwms_20.cwms_env.set_session_user_direct('CCP');
  ccp_help.register_callback_proc;
end;
/