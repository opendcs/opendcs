whenever sqlerror exit failure rollback
whenever oserror exit failure rollback
alter session set container=FREEPDB1;

declare
    memberships integer;
begin
    select count(*) into memberships from cwms_20.av_sec_users
     where upper(username) = 'BUILDUSER' and upper(user_group_id) like 'CCP%'
       and is_member = 'T';
    if memberships <> 0 then
        raise_application_error(-20001, 'Migration user must have no CCP office privileges');
    end if;
end;
/

insert into ccp.cp_algorithm (algorithm_id, algorithm_name, exec_class, db_office_code)
values (-2305, 'UpdaterCredentialTest', 'decodes.cwms.algo.ResEvapAlgo', 44);
insert into ccp.cp_algo_ts_parm (algorithm_id, algo_role_name, parm_type)
values (-2305, 'hourlyEvap', 'o');
commit;

create table ccp.updater_credential_audit (session_username varchar2(128));
create or replace trigger ccp.audit_updater_credentials
after insert or update or delete on ccp.cp_algo_ts_parm
for each row
when (new.algorithm_id = -2305 or old.algorithm_id = -2305)
begin
    insert into ccp.updater_credential_audit (session_username)
    values (sys_context('USERENV', 'SESSION_USER'));
end;
/
exit success
