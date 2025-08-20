-- This file is used for the CWMS-Oracle integration tests
-- If you are using a plain Oracle Database the following grants are required
-- If you are using AWS RDS, and likely the Azure equivalent, the created admin
--  admin user already has these permissions and can be used directly.
begin
    -- builduser is the "admin" user the ready dateabase was created with
    --execute immediate 'create user builduser identified by "adminuser"';
    execute immediate 'grant dba to builduser';

    execute immediate 'grant select on dba_queues to builduser with grant option';
    execute immediate 'grant select on dba_scheduler_jobs to builduser with grant option';
    execute immediate 'grant select on dba_queue_subscribers to builduser with grant option';
    execute immediate 'grant select on dba_subscr_registrations to builduser with grant option';
    execute immediate 'grant select on dba_scheduler_jobs to builduser with grant option';
    execute immediate 'grant select on dba_scheduler_job_log to builduser with grant option';
    execute immediate 'grant select on dba_scheduler_job_run_details to builduser with grant option';

    execute immediate 'grant execute on dbms_crypto to builduser with grant option';
    execute immediate 'grant execute on dbms_aq to builduser with grant option';
    execute immediate 'grant execute on dbms_aq_bqview to builduser with grant option';
    execute immediate 'grant execute on dbms_aqadm to builduser with grant option';
    execute immediate 'grant execute on dbms_lock to builduser with grant option';
    execute immediate 'grant execute on dbms_rls to builduser with grant option';
    execute immediate 'grant execute on dbms_lob to builduser with grant option';
    execute immediate 'grant execute on dbms_random to builduser with grant option';
    execute immediate 'grant execute on dbms_session to builduser with grant option';
    execute immediate 'grant execute on utl_smtp to builduser with grant option';
    execute immediate 'grant execute on utl_http to builduser with grant option';
    execute immediate 'grant execute on utl_recomp to builduser with grant option';
    execute immediate 'grant select on sys.v_$latch to builduser with grant option';
    execute immediate 'grant select on sys.v_$mystat to builduser with grant option';
    execute immediate 'grant select on sys.v_$statname to builduser with grant option';
    execute immediate 'grant select on sys.v_$timer to builduser with grant option';
    execute immediate 'grant ctxapp to builduser with admin option';
    execute immediate 'grant execute on ctxsys.ctx_ddl to builduser with grant option';
    execute immediate 'grant execute on ctxsys.ctx_doc to builduser with grant option';

    execute immediate 'grant execute any procedure to builduser';
end;
