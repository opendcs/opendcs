create or replace package ccp_help authid definer as
  procedure register_callback_proc;
end ccp_help;
/

create or replace package body ccp_help as
    procedure register_callback_proc is
    begin
        ${CCP_SCHEMA}.cwms_ccp.reload_callback_proc('CCP_SUBSCRIBER');
        ${CCP_SCHEMA}.cwms_ccp.start_check_callback_proc_job;
    end;
end ccp_help;
/