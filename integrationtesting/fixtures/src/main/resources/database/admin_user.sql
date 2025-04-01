-- This file is used for the CWMS-Oracle integration tests
-- If you are using a plain Oracle Database the following grants are required
-- If you are using AWS RDS, and likely the Azure equivalent, the created admin
--  admin user already has these permissions and can be used directly.
begin
    execute immediate 'create user dbadm identified by "adminuser"';
    execute immediate 'grant dba to dbadm';

    execute immediate 'grant select on dba_queues to dbadm with grant option';
    execute immediate 'grant select on dba_scheduler_jobs to dbadm with grant option';
    execute immediate 'grant select on dba_queue_subscribers to dbadm with grant option';
    execute immediate 'grant select on dba_subscr_registrations to dbadm with grant option';
    execute immediate 'grant select on dba_scheduler_jobs to dbadm with grant option';
    execute immediate 'grant select on dba_scheduler_job_log to dbadm with grant option';
    execute immediate 'grant select on dba_scheduler_job_run_details to dbadm with grant option';

    execute immediate 'grant execute on dbms_crypto to dbadm with grant option';
    execute immediate 'grant execute on dbms_aq to dbadm with grant option';
    execute immediate 'grant execute on dbms_aq_bqview to dbadm with grant option';
    execute immediate 'grant execute on dbms_aqadm to dbadm with grant option';
    execute immediate 'grant execute on dbms_lock to dbadm with grant option';
    execute immediate 'grant execute on dbms_rls to dbadm with grant option';
    execute immediate 'grant execute on dbms_lob to dbadm with grant option';
    execute immediate 'grant execute on dbms_random to dbadm with grant option';
    execute immediate 'grant execute on dbms_session to dbadm with grant option';
    execute immediate 'grant execute on utl_smtp to dbadm with grant option';
    execute immediate 'grant execute on utl_http to dbadm with grant option';
    execute immediate 'grant execute on utl_recomp to dbadm with grant option';
    execute immediate 'grant select on sys.v_$latch to dbadm with grant option';
    execute immediate 'grant select on sys.v_$mystat to dbadm with grant option';
    execute immediate 'grant select on sys.v_$statname to dbadm with grant option';
    execute immediate 'grant select on sys.v_$timer to dbadm with grant option';
    execute immediate 'grant ctxapp to dbadm with admin option';
    execute immediate 'grant execute on ctxsys.ctx_ddl to dbadm with grant option';
    execute immediate 'grant execute on ctxsys.ctx_doc to dbadm with grant option';

    execute immediate 'grant execute any procedure to dbadm';
end;
