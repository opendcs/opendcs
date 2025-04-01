grant cwms_user to ${CCP_SCHEMA};
-- example below from https://stackoverflow.com/a/1199438
begin
  execute immediate 'create role ${CCP_SCHEMA}_users';
exception
  when others then
    --"ORA-01921: role name 'x' conflicts with another user or role name"
    if sqlcode = -01921 then 
      null;
    else
      raise;
    end if;
end;
/

grant create session,resource,connect to ${CCP_SCHEMA}_users;
grant ${CCP_SCHEMA}_USERS to cwms_user;
GRANT  ALTER ANY TABLE,CREATE ANY TABLE,CREATE ANY INDEX,CREATE ANY SEQUENCE,
      CREATE ANY VIEW,CREATE ANY PROCEDURE,CREATE ANY TRIGGER,CREATE ANY JOB,
      CREATE ANY SYNONYM,DROP ANY SYNONYM,CREATE PUBLIC SYNONYM,DROP PUBLIC SYNONYM
    TO ${CCP_SCHEMA};

GRANT CREATE ANY CONTEXT TO ${CCP_SCHEMA};
GRANT SELECT ON dba_scheduler_jobs to ${CCP_SCHEMA};
GRANT SELECT ON dba_queue_subscribers to ${CCP_SCHEMA};
GRANT SELECT ON dba_subscr_registrations to ${CCP_SCHEMA};
GRANT SELECT ON dba_queues to ${CCP_SCHEMA};
GRANT EXECUTE ON dbms_aq TO ${CCP_SCHEMA};
GRANT EXECUTE ON dbms_aqadm TO ${CCP_SCHEMA};
GRANT EXECUTE ON DBMS_SESSION to ${CCP_SCHEMA};
GRANT EXECUTE ON DBMS_RLS to ${CCP_SCHEMA};
begin
    sys.dbms_aqadm.grant_system_privilege (
      privilege    => 'enqueue_any',
      grantee      => '${CCP_SCHEMA}',
      admin_option => false);
    sys.dbms_aqadm.grant_system_privilege (
      privilege    => 'dequeue_any',
      grantee      => '${CCP_SCHEMA}',
      admin_option => false);
    sys.dbms_aqadm.grant_system_privilege (
      privilege    => 'manage_any',
      grantee      => '${CCP_SCHEMA}',
      admin_option => false);
end;
/

GRANT SELECT ON cwms_v_loc TO ${CCP_SCHEMA} WITH GRANT OPTION;
GRANT SELECT ON cwms_v_ts_id TO ${CCP_SCHEMA} WITH GRANT OPTION;
GRANT SELECT ON cwms_v_tsv TO ${CCP_SCHEMA};
GRANT SELECT ON cwms_20.cwms_seq TO ${CCP_SCHEMA};
GRANT SELECT ON cwms_20.cwms_seq TO ${CCP_SCHEMA}_USERS;

GRANT EXECUTE ON cwms_t_date_table TO ${CCP_SCHEMA};
GRANT EXECUTE ON cwms_t_jms_map_msg_tab TO ${CCP_SCHEMA};

GRANT EXECUTE ON ${CWMS_SCHEMA}.cwms_ts TO ${CCP_SCHEMA};
GRANT EXECUTE ON ${CWMS_SCHEMA}.cwms_msg TO ${CCP_SCHEMA};
GRANT EXECUTE ON ${CWMS_SCHEMA}.cwms_util TO ${CCP_SCHEMA};
GRANT EXECUTE ON ${CWMS_SCHEMA}.cwms_sec TO ${CCP_SCHEMA};

GRANT EXECUTE ON ${CWMS_SCHEMA}.cwms_env TO ${CCP_SCHEMA};
GRANT EXECUTE ON ${CWMS_SCHEMA}.cwms_env TO ${CCP_SCHEMA}_USERS;

ALTER USER ${CCP_SCHEMA} DEFAULT ROLE ALL;
