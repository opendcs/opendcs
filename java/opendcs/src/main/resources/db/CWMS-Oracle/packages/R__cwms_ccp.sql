---------------------------------------------------------------------------
-- CWMS CCP Database on ORACLE
-- Maintainer: Cove Software, LLC
---------------------------------------------------------------------------
---------------------------------------------------------------------------
-- This script creates or modifies tables and views for DECODES and
-- Computations in order to work with CWMS database in USACE
---------------------------------------------------------------------------

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
-- create the callback procedure within the cwms_ccp package
---------------------------------------------------------------------------
create or replace package cwms_ccp authid current_user as
  procedure unregister_callback_proc (
    p_subscriber_name in varchar2,
    p_queue_name      in varchar2)
  ;

  procedure remove_subscriber (
    p_subscriber_name in varchar2,
    p_queue_name      in varchar2)
  ;

  procedure register_callback_proc (
    p_subscriber_name in varchar2,
    p_queue_name      in varchar2)
  ;

  procedure reload_callback_proc (
    p_subscriber_name in varchar2,
    p_queue_name      in varchar2 default null)
  ;

  procedure check_callback_proc_subscriber
  ;

  procedure start_check_callback_proc_job
  ;

  /*
     define the ccp notify computation callback procedure
     that is used in CWMS_TS.(UN)REGISTER_TS_CALLBACK
  */
  procedure notify_for_comp (       -- use this name in CWMS_TS.(UN)REGISTER_TS_CALLBACK
    context  in raw,                --
    reginfo  in sys.aq$_reg_info,   -- These parameters must be
    descr    in sys.aq$_descriptor, -- named as given in order
    payload  in raw,                -- for the callback to work
    payloadl in number)             --
  ;

  procedure set_log_msg_active (
    p_log_msg_active in integer)
  ;

end cwms_ccp;
/


create or replace package body cwms_ccp as
  k_log_msg_active  integer := 0;    -- 0: deactivated; 1: activated

  -------------------------------------------------------------------------
  -- unregister the callback if it is registered
  -------------------------------------------------------------------------
  procedure unregister_callback_proc (
    p_subscriber_name in varchar2,
    p_queue_name      in varchar2)
  is
    l_subscription_name  varchar2(128);
    l_queue_name         varchar2(80);
    l_subscriber_name    varchar2(32);
  begin
    if p_queue_name is null then
      l_subscription_name := '"${CWMS_SCHEMA}"."%":"'||p_subscriber_name||'"';
    else
      l_subscription_name := '"${CWMS_SCHEMA}"."'||p_queue_name||'":"'||p_subscriber_name||'"';
    end if;

    for rec in
      (select subscription_name from dba_subscr_registrations
         where subscription_name like l_subscription_name
           and location_name = 'plsql://${CCP_SCHEMA}.CWMS_CCP.NOTIFY_FOR_COMP'
      )
    loop
      l_subscriber_name := trim(both '"' from regexp_substr(rec.subscription_name, '[^:]+', 1, 2));
      l_queue_name := trim(both '"' from regexp_substr(regexp_substr(rec.subscription_name, '[^:]+', 1, 1), '[^.]+', 1, 2));

      ${CWMS_SCHEMA}.cwms_ts.unregister_ts_callback(
        '${CCP_SCHEMA}.CWMS_CCP.NOTIFY_FOR_COMP',
        l_subscriber_name,
        l_queue_name);
    end loop;
  end unregister_callback_proc;

  -------------------------------------------------------------------------
  -- unsubscribe from the queue if still subscribed
  -------------------------------------------------------------------------
  procedure remove_subscriber (
    p_subscriber_name in varchar2,
    p_queue_name      in varchar2)
  is
    l_queue_name      varchar2(80);
  begin
    for rec in
      (select name from dba_queues
         where owner = upper('${CWMS_SCHEMA}') and queue_type in('NORMAL_QUEUE')
           and name like '%_TS_STORED'
      )
    loop
      if  ((p_queue_name is not null) and (upper(p_queue_name) <> upper(rec.name))) then
        continue;
      end if;

      l_queue_name := '${CWMS_SCHEMA}.'||rec.name;
      begin
        dbms_aqadm.remove_subscriber(
          l_queue_name,
          sys.aq$_agent(p_subscriber_name, l_queue_name, 0));
      exception
        when others then null;
      end;
    end loop;
  end remove_subscriber;

  -------------------------------------------------------------------------
  -- register the callback procedure
  -------------------------------------------------------------------------
  procedure register_callback_proc (
    p_subscriber_name in varchar2,
    p_queue_name      in varchar2)
  is
    l_subscriber_name    varchar2(32);
  begin
    for rec in
      (select name from dba_queues
         where owner = '${CWMS_SCHEMA}' and queue_type in('NORMAL_QUEUE')
           and name like '%_TS_STORED'
      )
    loop
      if  ((p_queue_name is not null) and (upper(p_queue_name) <> upper(rec.name))) then
        continue;
      end if;

      l_subscriber_name := ${CWMS_SCHEMA}.cwms_ts.register_ts_callback(
        '${CCP_SCHEMA}.CWMS_CCP.NOTIFY_FOR_COMP',
        p_subscriber_name,
        rec.name);
    end loop;
  end register_callback_proc;

  -------------------------------------------------------------------------
  -- reload the callback procedure
  -------------------------------------------------------------------------
  procedure reload_callback_proc (
    p_subscriber_name in varchar2,
    p_queue_name      in varchar2 default null)
  is
  begin
    unregister_callback_proc(
        p_subscriber_name  => p_subscriber_name,
        p_queue_name       => p_queue_name);

    remove_subscriber(
        p_subscriber_name  => p_subscriber_name,
        p_queue_name       => p_queue_name);

    register_callback_proc(
        p_subscriber_name  => p_subscriber_name,
        p_queue_name       => p_queue_name);
  end reload_callback_proc;

  -------------------------------------------------------------------------
  -- check if the callback procedure subscriber exists
  -------------------------------------------------------------------------
  procedure check_callback_proc_subscriber
  is
    l_queue_name          varchar2(80);
    l_subscriber_name     varchar2(32)  := 'CCP_SUBSCRIBER';
  begin
    for rec1 in
      (select * from dba_queues
         where owner = upper('${CWMS_SCHEMA}') and queue_type in('NORMAL_QUEUE')
           and name like '%_TS_STORED'
      )
    loop
      l_queue_name := rec1.name;

      for rec2 in
        (select * from dba_queue_subscribers
           where owner = upper('${CWMS_SCHEMA}') and consumer_name in(l_subscriber_name)
             and queue_name like '%_TS_STORED'
        )
      loop
        /* Search for the subscribed queue name and set l_queue_name as null if found. */
        if (upper(rec2.queue_name) = upper(l_queue_name)) then
          l_queue_name := null;
          exit;
        end if;
      end loop;   /* The end of for rec2 loop */

      if l_queue_name is null then
        continue;
      end if;

      reload_callback_proc(l_subscriber_name, l_queue_name);
    end loop;     /* The end of for rec1 loop */
  end check_callback_proc_subscriber;

  -------------------------------------------------------------------------
  -- start to check the callback procedure subscriber job
  -------------------------------------------------------------------------
  procedure start_check_callback_proc_job
  is
    l_count           integer;
    l_run_interval    varchar2(8);
    l_comment         varchar2(256);
    l_user_id         varchar2(30) := '${CCP_SCHEMA}';
    l_job_id          varchar2(30) := 'CHECK_NOTIFY_CALLBACK_PROC_JOB';

    function job_count
      return integer
    is
    begin
      select count (*) into l_count
        from sys.dba_scheduler_jobs
        where job_name = l_job_id and owner = l_user_id;

      return l_count;
    end;
  begin
    /* drop the job if it is already running */
    if job_count > 0
    then
      dbms_output.put ('Dropping existing job ' || l_job_id || '...');
      dbms_scheduler.drop_job (l_job_id);

      if job_count = 0
      then
        dbms_output.put_line ('done.');
      else
        dbms_output.put_line ('failed.');
        raise_application_error(-20999, 'Failed to drop the existing job '||l_job_id, true);
      end if;
    end if;

    /* restart the job */
    if job_count = 0
    then
      begin
        dbms_scheduler.create_job(
            job_name             => l_job_id,
            job_type             => 'stored_procedure',
            job_action           => 'cwms_ccp.check_callback_proc_subscriber',
            start_date           => null,
            repeat_interval      => 'freq=minutely; interval= 15',
            end_date             => null,
            job_class            => 'default_job_class',
            enabled              => true,
            auto_drop            => false,
            comments             => 'check if the subscriber for callback_proc exists.'
        );

        if job_count = 1
        then
          dbms_output.put_line('Job '||l_job_id||' is successfully scheduled to execute every 15 minutes.');
        else
          raise_application_error(-20999, 'Failed to create the job '||l_job_id, true);
        end if;
      exception
        when others
        then
          raise_application_error(-20999, sqlerrm, true);
      end;
    end if;
  end start_check_callback_proc_job;

  -------------------------------------------------------------------------
  -- notify the ts data stored
  -------------------------------------------------------------------------
  procedure notify_tsdatastored (
    p_tsid            in varchar2,
    p_ts_code         in integer,
    p_start_time      in timestamp,
    p_end_time        in timestamp,
    p_store_time      in timestamp,
    p_enqueue_time    in timestamp)
  is
    c_timestamp_fmt   constant varchar2(32) := 'yyyy/mm/dd hh24:mi:ss';
    l_delete_flag     varchar2(1);
    l_unit_id         varchar2(32);
    l_start_time      date;
    l_end_time        date;
  begin
    l_start_time := to_date(to_char(p_start_time, c_timestamp_fmt), c_timestamp_fmt);
    l_end_time   := to_date(to_char(p_end_time, c_timestamp_fmt), c_timestamp_fmt);
    l_unit_id    := ${CWMS_SCHEMA}.cwms_ts.get_db_unit_id(p_tsid);

    for r2 in
      (select distinct cc.loading_application_id
         from cp_comp_depends cd, cp_computation cc
         where cd.ts_id = p_ts_code
           and cc.loading_application_id is not null and cd.computation_id = cc.computation_id
      )
    loop
      insert into cp_comp_tasklist
      (  record_num,
         loading_application_id,
         site_datatype_id,
         date_time_loaded,
         start_date_time,
         value,
         unit_id,
         delete_flag,
         quality_code,
         flags)
      select
         cp_comp_tasklistidseq.nextval,
         r2.loading_application_id,
         p_ts_code,
         sysdate,
         r1.date_time,
         nvl(r1.value,0),
         l_unit_id,
         decode (r1.quality_code,5,'Y','N') delete_flag,
         r1.quality_code,
         r1.quality_code
      from cwms_v_tsv r1
      where r1.ts_code = p_ts_code
        and r1.date_time >= l_start_time and r1.date_time <= l_end_time
        and r1.data_entry_date <= p_enqueue_time;
    end loop;   /* end of for r2 loop */

    commit;
  end notify_tsdatastored;

  -------------------------------------------------------------------------
  -- notify the ts code created
  -------------------------------------------------------------------------
  procedure notify_tscreated (
    p_tsid            in varchar2,
    p_ts_code         in integer)
  is
  begin
    insert into cp_depends_notify(record_num, event_type, key, date_time_loaded, db_office_code)
        values(cp_depends_notifyidseq.nextval, 'T', p_ts_code, SYSDATE,
			(select db_office_code from cwms_v_ts_id where ts_code = p_ts_code));
    commit;
  end notify_tscreated;

  -------------------------------------------------------------------------
  -- notify the ts code deleted
  -------------------------------------------------------------------------
  procedure notify_tsdeleted (
    p_ts_code       in integer,
    p_office_id     in varchar2)
  is
    l_office_cd   integer;
  begin
    l_office_cd := ${CWMS_SCHEMA}.cwms_util.get_db_office_code (p_office_id);

    insert into cp_depends_notify(record_num, event_type, key, date_time_loaded, db_office_code)
      values(cp_depends_notifyidseq.nextval, 'D', p_ts_code, SYSDATE, l_office_cd);
    commit;
  end notify_tsdeleted;

  -------------------------------------------------------------------------
  -- notify the ts data deleted
  -------------------------------------------------------------------------
  procedure notify_tsdatadeleted (
    p_tsid            in varchar2,
    p_ts_code         in integer,
    p_start_time      in timestamp,
    p_end_time        in timestamp,
    p_deleted_time    in integer,
    p_version_date    in integer)
  is
    c_timestamp_fmt   constant varchar2(32) := 'yyyy/mm/dd hh24:mi:ss';
    l_delete_flag     varchar2(1);
    l_deleted_times   cwms_t_date_table;
    l_deleted_count   integer;
  begin
    l_deleted_times := cwms_t_date_table();
    l_deleted_times := cwms_ts.retrieve_deleted_times_f(p_deleted_time, p_ts_code, p_version_date);
    l_deleted_count := 0;
    if l_deleted_times is not null then
      l_deleted_count := l_deleted_times.count;
      dbms_output.put_line('# of Deleted TS Data: '||l_deleted_count);
    end if;

    for r2 in
      (select distinct cc.loading_application_id
         from cp_comp_depends cd, cp_computation cc
         where cd.ts_id = p_ts_code and cc.enabled = 'Y'
           and cc.loading_application_id is not null and cd.computation_id = cc.computation_id
      )
    loop
      l_delete_flag := 'Y';

      for i in 1..l_deleted_count
      loop
        insert into cp_comp_tasklist(record_num,loading_application_id,site_datatype_id,
          date_time_loaded,start_date_time,delete_flag,flags)
        values(cp_comp_tasklistidseq.nextval,r2.loading_application_id,p_ts_code,
          cwms_util.to_timestamp(p_deleted_time),l_deleted_times(i),l_delete_flag,0);
      end loop; /* end of for i loop */
    end loop;   /* end of for r2 loop */

    l_deleted_times.delete;
    commit;
  end notify_tsdatadeleted;

  -------------------------------------------------------------------------
  -- notify the ts code deleted -- NOT SUPPORTED
  -------------------------------------------------------------------------
  procedure notify_tscodechanged(
    p_tsid            in varchar2,
    p_ts_code         in integer,
    p_ts_code_old     in integer)
  is
  begin
    insert into cp_depends_notify(record_num, event_type, key, date_time_loaded, db_office_code)
        values(cp_depends_notifyidseq.nextval, 'H', p_ts_code, SYSDATE,
			(select db_office_code from cwms_v_ts_id where ts_code = p_ts_code));
    commit;
  end notify_tscodechanged;

  -------------------------------------------------------------------------
  -- notify the ts id renamed
  -------------------------------------------------------------------------
  procedure notify_tsrenamed(
    p_tsid_old        in varchar2,
    p_tsid_new        in varchar2,
    p_ts_code         in integer)
  is
  begin
    insert into cp_depends_notify(record_num, event_type, key, date_time_loaded, db_office_code)
        values(cp_depends_notifyidseq.nextval, 'M', p_ts_code, SYSDATE,
			(select db_office_code from cwms_v_ts_id where ts_code = p_ts_code));
    commit;
  end notify_tsrenamed;


  -------------------------------------------------------------------------
  -- create the callback procedure
  -------------------------------------------------------------------------
  procedure notify_for_comp (       -- use this name in CWMS_TS.(UN)REGISTER_TS_CALLBACK
    context  in raw,                --
    reginfo  in sys.aq$_reg_info,   -- These parameters must be
    descr    in sys.aq$_descriptor, -- named as given in order
    payload  in raw,                -- for the callback to work
    payloadl in number)             --
  is
    QUEUE_TIMEOUT         exception;
    pragma exception_init (QUEUE_TIMEOUT, -25228);
    l_store_rule_len      constant pls_integer := 32;
    l_msgtype_len         constant pls_integer := 32;
    l_office_id_len       constant pls_integer := 16;
    l_tsid_len            constant pls_integer := 183;
    l_batch_size          constant pls_integer := 10;
    l_dequeue_options     dbms_aq.dequeue_options_t;
    l_msgprops            dbms_aq.message_properties_array_t;
    l_msgids              dbms_aq.msgid_array_t;
    l_errors              dbms_aq.error_array_t;
    l_messages            cwms_t_jms_map_msg_tab;
    l_message             sys.aq$_jms_map_message;
    l_msgcount            pls_integer;
    l_msgid               pls_integer;
    l_msgtype             varchar2(32);  -- l_msgtype_len
    l_office_id           varchar2(16);  -- l_office_id_len
    l_tsid                varchar2(183); -- l_tsid_len
    l_new_tsid            varchar2(183); -- l_tsid_len
    l_store_rule          varchar2(32);  -- l_store_rule_len
    l_comment             varchar2(256);
    l_lock_handle         varchar2(128);
    l_ts_code             integer;
    l_ts_code_old         integer;
    l_start_millis        integer;
    l_end_millis          integer;
    l_enqueue_millis      integer;
    l_store_millis        integer;
    l_queue_delay         integer;
    l_enqueue_time        timestamp;
    l_dequeue_time        timestamp;
    l_start_time          timestamp;
    l_end_time            timestamp;
    l_store_time          timestamp;
    l_deleted_time        integer;
    l_version_date        integer;
    ----------------------------------------------------
    -- helper function to retrieve text from messages --
    ----------------------------------------------------
    function get_string(
      p_message   in out nocopy sys.aq$_jms_map_message,
      p_msgid     in pls_integer,
      p_item_name in varchar2,
      p_max_len   in pls_integer)
    return varchar2
    is
      l_clob clob;
    begin
      p_message.get_string(p_msgid, p_item_name, l_clob);
      return dbms_lob.substr(l_clob, p_max_len, 1);
    end;
  begin
    l_dequeue_options.consumer_name := descr.consumer_name;
    l_dequeue_options.wait := dbms_aq.no_wait;
    l_dequeue_options.navigation := dbms_aq.first;
    l_dequeue_options.visibility := dbms_aq.immediate;
    l_messages := cwms_t_jms_map_msg_tab();
    l_msgprops := dbms_aq.message_properties_array_t();
    l_msgids := dbms_aq.msgid_array_t();
    l_errors := dbms_aq.error_array_t();
    l_messages.extend(l_batch_size);
    l_msgprops.extend(l_batch_size);
    l_msgids.extend(l_batch_size);
    l_errors.extend(l_batch_size);

    begin
      loop
        l_msgcount := dbms_aq.dequeue_array(
           queue_name                => descr.queue_name,
           dequeue_options           => l_dequeue_options,
           array_size                => l_messages.count,
           message_properties_array  => l_msgprops,
           payload_array             => l_messages,
           msgid_array               => l_msgids,
           error_array               => l_errors);

        exit when l_msgcount = 0;

        l_dequeue_time := systimestamp at time zone 'UTC';

        for i in 1..l_msgcount loop
          l_message := l_messages(i);
          l_msgid := l_message.prepare(-1);

          begin
            -----------------------------------------------------------------
            -- retrieve the common info from the message
            -----------------------------------------------------------------
            l_msgtype        := get_string(l_message, l_msgid, 'type', l_msgtype_len);
            l_office_id      := get_string(l_message, l_msgid, 'office_id', l_office_id_len);
            l_enqueue_millis := l_message.get_long(l_msgid, 'millis');
            l_enqueue_time   := ${CWMS_SCHEMA}.cwms_util.to_timestamp(l_enqueue_millis);
            l_queue_delay    := ${CWMS_SCHEMA}.cwms_util.to_millis(l_dequeue_time) - l_enqueue_millis;
            l_comment  := 'Starting Case';
            -----------------------------------------------------------------
            -- operate on the message based on its type
            -----------------------------------------------------------------
            case l_msgtype
              when 'TSDataStored' then
                l_tsid         := get_string(l_message, l_msgid, 'ts_id', l_tsid_len);
                l_store_rule   := get_string(l_message, l_msgid, 'store_rule', l_store_rule_len);

                l_ts_code      := l_message.get_long(l_msgid, 'ts_code');
                l_start_millis := l_message.get_long(l_msgid, 'start_time');
                l_end_millis   := l_message.get_long(l_msgid, 'end_time');
                l_store_millis := l_message.get_long(l_msgid, 'store_time');
                l_version_date := l_message.get_long(l_msgid, 'version_date');

                l_start_time   := ${CWMS_SCHEMA}.cwms_util.to_timestamp(l_start_millis);
                l_end_time     := ${CWMS_SCHEMA}.cwms_util.to_timestamp(l_end_millis);
                l_store_time   := ${CWMS_SCHEMA}.cwms_util.to_timestamp(l_store_millis);
                l_comment      := 'start time= '||l_start_time||
                                  ', end time= '||l_end_time;

                if l_store_rule = cwms_util.delete_insert
                then
                  notify_tsdatadeleted (
                    p_tsid            => l_tsid,
                    p_ts_code         => l_ts_code,
                    p_start_time      => l_start_time,
                    p_end_time        => l_end_time,
                    p_deleted_time    => l_store_millis,
                    p_version_date    => l_version_date);
                end if;

                notify_tsdatastored(
                  p_tsid            => l_tsid,
                  p_ts_code         => l_ts_code,
                  p_start_time      => l_start_time,
                  p_end_time        => l_end_time,
                  p_store_time      => l_store_time,
                  p_enqueue_time    => l_enqueue_time);

              when 'TSDataDeleted' then
                l_tsid         := get_string(l_message, l_msgid, 'ts_id', l_tsid_len);
                l_ts_code      := l_message.get_long(l_msgid, 'ts_code');
                l_start_millis := l_message.get_long(l_msgid, 'start_time');
                l_end_millis   := l_message.get_long(l_msgid, 'end_time');
                l_start_time   := ${CWMS_SCHEMA}.cwms_util.to_timestamp(l_start_millis);
                l_end_time     := ${CWMS_SCHEMA}.cwms_util.to_timestamp(l_end_millis);
                l_deleted_time := l_message.get_long(l_msgid, 'deleted_time');
                l_version_date := l_message.get_long(l_msgid, 'version_date');
                l_comment      := 'start time= '||l_start_time||
                                  ', end time= '||l_end_time||
                                  ', deleted time= '||l_deleted_time||
                                  ', version date= '||l_version_date;

                notify_tsdatadeleted(
                  p_tsid            => l_tsid,
                  p_ts_code         => l_ts_code,
                  p_start_time      => l_start_time,
                  p_end_time        => l_end_time,
                  p_deleted_time    => l_deleted_time,
                  p_version_date    => l_version_date);

              when 'TSCreated' then
                l_tsid         := get_string(l_message, l_msgid, 'ts_id', l_tsid_len);
                l_ts_code      := l_message.get_long(l_msgid, 'ts_code');
                l_start_millis := null;
                l_end_millis   := null;
                l_start_time   := null;
                l_end_time     := null;
                l_comment      := null;

                notify_tscreated(
                  p_tsid            => l_tsid,
                  p_ts_code         => l_ts_code);

              when 'TSDeleted' then
                l_ts_code      := l_message.get_long(l_msgid, 'ts_code');
                l_start_millis := null;
                l_end_millis   := null;
                l_start_time   := null;
                l_end_time     := null;

                l_comment      := 'calling notify_tsdelete with ts_code='||l_ts_code
			||',office_id='||l_office_id;

                notify_tsdeleted(
                  p_ts_code => l_ts_code,
                  p_office_id => l_office_id);

              when 'TSCodeChanged' then
                l_tsid         := get_string(l_message, l_msgid, 'ts_id', l_tsid_len);
                l_start_millis := null;
                l_end_millis   := null;
                l_start_time   := null;
                l_end_time     := null;
                l_ts_code      := l_message.get_long(l_msgid, 'new_ts_code');
                l_ts_code_old  := l_message.get_long(l_msgid, 'old_ts_code');
                l_comment      := 'old ts code= '||l_ts_code_old;

                notify_tscodechanged(
                  p_tsid            => l_tsid,
                  p_ts_code         => l_ts_code,
                  p_ts_code_old     => l_ts_code_old);

              when 'TSRenamed' then
                l_tsid         := get_string(l_message, l_msgid, 'ts_id', l_tsid_len);
                l_ts_code      := l_message.get_long(l_msgid, 'ts_code');
                l_start_millis := null;
                l_end_millis   := null;
                l_start_time   := null;
                l_end_time     := null;
                l_new_tsid     := get_string(l_message, l_msgid, 'new_ts_id', l_tsid_len);
                l_comment      := 'new ts id= '||l_new_tsid;

                notify_tsrenamed(
                  p_tsid_old        => l_tsid,
                  p_tsid_new        => l_new_tsid,
                  p_ts_code         => l_ts_code);

              else
                -------------------------------------------------------------
                -- put an error message in the table
                -------------------------------------------------------------
                l_comment      := 'Unexpected message type: '||l_msgtype;
                l_start_millis := null;
                l_end_millis   := null;
                l_start_time   := null;
                l_end_time     := null;
                l_msgtype      := 'ERR';
                l_office_id    := 'ERR';
                l_tsid         := 'ERR';
                l_ts_code      := -1;

                cwms_msg.log_db_message(
                  'notify_for_comp',
                  cwms_msg.msg_level_normal,
                  'Dequeued '
                  || l_msgtype
                  ||' for '
                  || l_office_id
                  ||'/'
                  || l_ts_code
                  ||'/'
                  || l_tsid
                  ||', enqueue time='
                  || l_enqueue_time
                  ||', delay='
                  || l_queue_delay
                  ||', other info: '
                  ||l_comment);

            end case;
            -----------------------------------------------------------------
            -- insert the message info into the log table
            -----------------------------------------------------------------
            if k_log_msg_active > 0 then
              cwms_msg.log_db_message(
                'notify_for_comp',
                cwms_msg.msg_level_normal,
                'Dequeued '
                || l_msgtype
                ||' for '
                || l_office_id
                ||'/'
                || l_ts_code
                ||'/'
                || l_tsid
                ||', enqueue time='
                || l_enqueue_time
                ||', delay='
                || l_queue_delay
                ||', other info: '
                ||l_comment);
            end if;

            l_comment := sqlerrm;
          exception
            when others then
              cwms_msg.log_db_message(
                'notify_for_comp',
                cwms_msg.msg_level_normal,
                'Error in processing message: '
                ||l_msgtype
                ||'/'
                ||l_comment);
          end; /* end of exception */

          l_message.clean(l_msgid);
        end loop; /* end of for i loop */
      end loop;
    exception
      when QUEUE_TIMEOUT then null;
        -------------------------------------------------------------------
        -- timed out trying to dequeue at least one message
        -------------------------------------------------------------------
        if k_log_msg_active > 0 then
          cwms_msg.log_db_message(
            'notify_for_comp',
            cwms_msg.msg_level_normal,
            'Dequeued '
            ||'TIMEOUT'
            ||', enqueue time='
            || l_enqueue_time
            ||', delay='
            || l_queue_delay
            ||', other info: '
            ||l_comment);
        end if;
      when others then null;
        -------------------------------------------------------------------
        -- put an error message in the table
        -------------------------------------------------------------------
        l_comment := sqlerrm;
        cwms_msg.log_db_message(
           'notify_for_comp',
           cwms_msg.msg_level_normal,
           'Dequeued '
           ||'ERR'
           ||', enqueue time='
           || l_enqueue_time
           ||', delay='
           || l_queue_delay
           ||', other info: '
           ||l_comment);

    end;
  end notify_for_comp;

  procedure set_log_msg_active (
    p_log_msg_active  in integer)
  is
  begin
    if p_log_msg_active is not null then
      k_log_msg_active := p_log_msg_active;
    end if;
  end set_log_msg_active;

end cwms_ccp;
/
