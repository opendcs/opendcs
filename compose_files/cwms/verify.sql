whenever sqlerror exit failure rollback
whenever oserror exit failure rollback
alter session set container=FREEPDB1;

declare
    renamed_roles integer;
    old_roles integer;
    added_roles integer;
    app_updates integer;
    other_updates integer;
begin
    select count(*) into renamed_roles from ccp.cp_algo_ts_parm
     where algorithm_id = -2305 and algo_role_name = 'hourlyEvapRate';
    select count(*) into old_roles from ccp.cp_algo_ts_parm
     where algorithm_id = -2305 and algo_role_name = 'hourlyEvap';
    select count(*) into added_roles from ccp.cp_algo_ts_parm
     where algorithm_id = -2305 and algo_role_name = 'hourlyEvapDepth';
    if renamed_roles <> 1 or old_roles <> 0 or added_roles <> 1 then
        raise_application_error(-20002, 'Manager did not commit the expected algorithm updates');
    end if;
    select count(*) into app_updates from ccp.updater_credential_audit
     where session_username = 'UPDATER_APP';
    select count(*) into other_updates from ccp.updater_credential_audit
     where session_username <> 'UPDATER_APP';
    if app_updates = 0 or other_updates <> 0 then
        raise_application_error(-20003, 'After-update changes must use the application account');
    end if;
end;
/
exit success
