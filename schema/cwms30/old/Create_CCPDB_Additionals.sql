---------------------------------------------------------------------------
--
-- Sutron Ilex CCP Database on ORACLE
-- Database Version: 8        Date: 2013/05/02
-- Company: Sutron Corporation
--  Writer: GC
--
---------------------------------------------------------------------------
---------------------------------------------------------------------------
-- This script creates or modifies tables and views for DECODES and
-- Computations in order to work with CWMS database in USACE
---------------------------------------------------------------------------
set echo on
spool create_CCPDB_Additionals.out

whenever sqlerror continue
set define on
@@defines.sql

define sys_schema     = &1
define sys_passwd     = &2
define tns_name       = &3

define queue_name       = &cwms_schema..cwms_sec.get_user_office_id||'_TS_STORED';
define callback_proc    = &ccp_schema..CWMS_CCP.NOTIFY_FOR_COMP;

--connect &sys_schema/&sys_passwd@&tns_name as sysdba;
--alter session set current_schema = &ccp_schema;

connect &ccp_schema/&ccp_passwd@&tns_name;

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

  function get_computation_ts_members(
    p_computation_id  integer,
    p_inout_flag      varchar2,
    p_db_office_code  integer)
    return sys_refcursor
  ;

  function get_group_ts_members(
    p_group_id        integer,
    p_db_office_code  integer)
    return sys_refcursor
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
      l_subscription_name := '"&cwms_schema"."%":"'||p_subscriber_name||'"';
    else
      l_subscription_name := '"&cwms_schema"."'||p_queue_name||'":"'||p_subscriber_name||'"';
    end if;

    for rec in
      (select subscription_name from dba_subscr_registrations
         where subscription_name like l_subscription_name
           and location_name = 'plsql://&callback_proc'
      )
    loop
      l_subscriber_name := trim(both '"' from regexp_substr(rec.subscription_name, '[^:]+', 1, 2));
      l_queue_name := trim(both '"' from regexp_substr(regexp_substr(rec.subscription_name, '[^:]+', 1, 1), '[^.]+', 1, 2));

      &cwms_schema..cwms_ts.unregister_ts_callback(
        '&callback_proc',
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
         where owner = upper('&cwms_schema') and queue_type in('NORMAL_QUEUE')
           and name like '%_TS_STORED'
      )
    loop
      if  ((p_queue_name is not null) and (upper(p_queue_name) <> upper(rec.name))) then
        continue;
      end if;

      l_queue_name := '&cwms_schema..'||rec.name;
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
         where owner = '&cwms_schema' and queue_type in('NORMAL_QUEUE')
           and name like '%_TS_STORED'
      )
    loop
      if  ((p_queue_name is not null) and (upper(p_queue_name) <> upper(rec.name))) then
        continue;
      end if;

      l_subscriber_name := &cwms_schema..cwms_ts.register_ts_callback(
        '&callback_proc',
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
         where owner = upper('&cwms_schema') and queue_type in('NORMAL_QUEUE')
           and name like '%_TS_STORED'
      )
    loop
      l_queue_name := rec1.name;

      for rec2 in
        (select * from dba_queue_subscribers
           where owner = upper('&cwms_schema') and consumer_name in(l_subscriber_name)
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
    l_user_id         varchar2(30) := '&ccp_schema';
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

      /* verify if it has been dropped */
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


  /*
    This ccp notify computation callback procedure is used to figure out
    that, if any cwms TS data record is created, modified, or deleted,
    a computation notification record will be added to ccp task list table.
  */
  function build_comp_condition (
    p_comp_arg        in varchar2,
    p_comp_arg_type   in varchar2)
  return varchar2
  is
    l_query_str varchar2(1000);
  begin
    l_query_str := '';
    if p_comp_arg <> '*'
    then
      case upper(p_comp_arg_type)
        when 'PARAM' then
          l_query_str := ' and parameter_id = '''||rtrim(p_comp_arg)||''' ';
        when 'PARAMTYPE' then
          l_query_str := ' and parameter_type_id = '''||rtrim(p_comp_arg)||''' ';
        when 'INTERVAL' then
          l_query_str := ' and interval_id = '''||rtrim(p_comp_arg)||''' ';
        when 'DURATION' then
          l_query_str := ' and duration_id = '''||rtrim(p_comp_arg)||''' ';
        when 'VERSION' then
          l_query_str := ' and version_id = '''||rtrim(p_comp_arg)||''' ';
        else
          l_query_str := '';
      end case;
    end if;

    return l_query_str;
  end build_comp_condition;

  function get_group_condition_query (
    p_group_id        in integer,
    p_group_op        in varchar2)
  return varchar2
  is
    l_group_op        varchar2(10);
    l_query_str       varchar2(5000);
    l_query_str1      varchar2(1000);
    l_query_str2      varchar2(1000);
    l_query_str3      varchar2(1000);
    l_query_str4      varchar2(1000);
  begin
    l_query_str := null;

    case upper(nvl(p_group_op, ' '))
      when 'A' then
        l_group_op := ' in ';
      when 'S' then
        l_group_op := ' not in ';
      when 'I' then
        l_group_op := ' in ';
      else
        l_group_op := ' in ';
    end case;

    /* Get the location condition */
    l_query_str1 := null;
    for rr in
      (select distinct b.location_id
        from tsdb_group_member_site a, cwms_v_loc b
        where a.group_id = p_group_id and a.site_id = b.location_code
      )
    loop
      if (l_query_str1 is null)
      then
        l_query_str1 := ''''||rr.location_id||'''';
      else
        l_query_str1 := l_query_str1||','''||rr.location_id||'''';
      end if;
    end loop; /* end of for rr loop */
    if (l_query_str1 is not null)
    then
      if (l_query_str is null)
      then
        l_query_str := 'location_id'||l_group_op||'('||l_query_str1||') ';
      else
        l_query_str := l_query_str||'and location_id'||l_group_op||'('||l_query_str1||') ';
      end if;
    end if;

    /* Get the param condition */
    l_query_str1 := null;
    for rr in
      (select b.code from tsdb_group_member_dt a, datatype b
        where a.group_id = p_group_id and upper(b.standard) = 'CWMS'
          and a.data_type_id = b.id
      )
    loop
      if (l_query_str1 is null)
      then
        l_query_str1 := ''''||rr.code||'''';
      else
        l_query_str1 := l_query_str1||','''||rr.code||'''';
      end if;
    end loop; /* end of for rr loop */
    if (l_query_str1 is not null)
    then
      if (l_query_str is null)
      then
        l_query_str := 'parameter_id'||l_group_op||'('||l_query_str1||') ';
      else
        l_query_str := l_query_str||'and parameter_id'||l_group_op||'('||l_query_str1||') ';
      end if;
    end if;

    /* Get the param_type, interval, duration, and version conditions */
    l_query_str1 := null;
    l_query_str2 := null;
    l_query_str3 := null;
    l_query_str4 := null;
    for rr in
      (select member_type, member_value from tsdb_group_member_other
        where group_id = p_group_id
      )
    loop
      case upper(rr.member_type)
        when 'PARAMTYPE' then
          if (l_query_str1 is null)
          then
            l_query_str1 := ''''||rr.member_value||'''';
          else
            l_query_str1 := l_query_str1||','''||rr.member_value||'''';
          end if;
        when 'INTERVAL' then
          if (l_query_str2 is null)
          then
            l_query_str2 := ''''||rr.member_value||'''';
          else
            l_query_str2 := l_query_str2||','''||rr.member_value||'''';
          end if;
        when 'DURATION' then
          if (l_query_str3 is null)
          then
            l_query_str3 := ''''||rr.member_value||'''';
          else
            l_query_str3 := l_query_str3||','''||rr.member_value||'''';
          end if;
        when 'VERSION' then
          if (l_query_str4 is null)
          then
            l_query_str4 := ''''||rr.member_value||'''';
          else
            l_query_str4 := l_query_str4||','''||rr.member_value||'''';
          end if;
        else
          continue;
      end case;
    end loop; /* end of for rr loop */
    if (l_query_str1 is not null)
    then
      if (l_query_str is null)
      then
        l_query_str := 'parameter_type_id'||l_group_op||'('||l_query_str1||') ';
      else
        l_query_str := l_query_str||'and parameter_type_id'||l_group_op||'('||l_query_str1||') ';
      end if;
    end if;
    if (l_query_str2 is not null)
    then
      if (l_query_str is null)
      then
        l_query_str := 'interval_id'||l_group_op||'('||l_query_str2||') ';
      else
        l_query_str := l_query_str||'and interval_id'||l_group_op||'('||l_query_str2||') ';
      end if;
    end if;
    if (l_query_str3 is not null)
    then
      if (l_query_str is null)
      then
        l_query_str := 'duration_id'||l_group_op||'('||l_query_str3||') ';
      else
        l_query_str := l_query_str||'and duration_id'||l_group_op||'('||l_query_str3||') ';
      end if;
    end if;
    if (l_query_str4 is not null)
    then
      if (l_query_str is null)
      then
        l_query_str := 'version_id'||l_group_op||'('||l_query_str4||') ';
      else
        l_query_str := l_query_str||'and version_id'||l_group_op||'('||l_query_str4||') ';
      end if;
    end if;

    return l_query_str;
  end get_group_condition_query;

  -- This function builds the query conditions defined in a computation
  function build_comp_condition_query (
    p_param           in varchar2,
    p_paramtype       in varchar2,
    p_interval        in varchar2,
    p_duration        in varchar2,
    p_version         in varchar2)
  return varchar2
  is
    l_sql_txt         varchar2(5000);
  begin
    l_sql_txt := '';

    l_sql_txt := rtrim(l_sql_txt) ||
                 rtrim(build_comp_condition(p_comp_arg       => p_param,
                                            p_comp_arg_type  => 'PARAM'));
    l_sql_txt := rtrim(l_sql_txt) ||
                 rtrim(build_comp_condition(p_comp_arg       => p_paramtype,
                                            p_comp_arg_type  => 'PARAMTYPE'));
    l_sql_txt := rtrim(l_sql_txt) ||
                 rtrim(build_comp_condition(p_comp_arg       => p_interval,
                                            p_comp_arg_type  => 'INTERVAL'));
    l_sql_txt := rtrim(l_sql_txt) ||
                 rtrim(build_comp_condition(p_comp_arg       => p_duration,
                                            p_comp_arg_type  => 'DURATION'));
    l_sql_txt := rtrim(l_sql_txt) ||
                 rtrim(build_comp_condition(p_comp_arg       => p_version,
                                            p_comp_arg_type  => 'VERSION'));
    return l_sql_txt;
  end build_comp_condition_query;

  -- This function builds the query conditions defined in a TS group
  function build_group_condition_query(
    p_parent_group_id  in integer,
    p_parent_group_str in varchar2)
    return varchar2
  is
    l_group_str        varchar2(1000);
    l_group_str_a      varchar2(1000);
    l_group_str_s      varchar2(1000);
    l_group_str_i      varchar2(1000);
    l_return_str       varchar2(5000);
  begin
    l_return_str     := null;
    l_group_str      := null;
    l_group_str_a    := null;
    l_group_str_s    := null;
    l_group_str_i    := null;

    -- return null if no parent group id
    if (p_parent_group_id is null)
    then
      return l_return_str;
    end if;

    -- get all child groups under the same parent group id
    for rr in
      (select parent_group_id, child_group_id, include_group
         from tsdb_group_member_group where parent_group_id = p_parent_group_id
      )
    loop
      l_group_str := get_group_condition_query(rr.child_group_id, rr.include_group);

      l_return_str := build_group_condition_query(rr.child_group_id, l_group_str);
      if (l_return_str is null)    /* when a subgroup is a leaf */
      then
        case upper(rr.include_group)
          when 'A' then
            if (l_group_str_a is null)
            then
              l_group_str_a := l_group_str;
            else
              l_group_str_a := l_group_str_a ||' or '||l_group_str;
            end if;
          when 'S' then
            if (l_group_str_s is null)
            then
              l_group_str_s := l_group_str;
            else
              l_group_str_s := l_group_str_s ||' and '||l_group_str;
            end if;
          when 'I' then
            if (l_group_str_i is null)
            then
              l_group_str_i := l_group_str;
            else
              l_group_str_i := l_group_str_i ||' and '||l_group_str;
            end if;
        end case;
      else                         /* when a subgroup is not a leaf */
        case upper(rr.include_group)
          when 'A' then
            if (l_group_str_a is null)
            then
              l_group_str_a := l_return_str;
            else
              l_group_str_a := l_group_str_a ||' or '||l_return_str;
            end if;
          when 'S' then
            if (l_group_str_s is null)
            then
              l_group_str_s := l_return_str;
            else
              l_group_str_s := l_group_str_s ||' and '||l_return_str;
            end if;
          when 'I' then
            if (l_group_str_i is null)
            then
              l_group_str_i := l_return_str;
            else
              l_group_str_i := l_group_str_i ||' and '||l_return_str;
            end if;
        end case;
      end if;
    end loop; /* end of for rr ... loop */

    /* build the return query string */
    l_return_str := null;
    if (p_parent_group_str is not null)
    then
      l_return_str := '('||p_parent_group_str||')';
    end if;
    if (l_group_str_a is not null)
    then
      if (l_return_str is null)
      then
        l_return_str := l_group_str_a;
      else
        l_return_str := '('||l_return_str||' or '||l_group_str_a||')';
      end if;
    end if;
    if (l_group_str_s is not null)
    then
      if (l_return_str is null)
      then
        l_return_str := l_group_str_s;
      else
        l_return_str := l_return_str||' and '||l_group_str_s;
      end if;
    end if;
    if (l_group_str_i is not null)
    then
      if (l_return_str is null)
      then
        l_return_str := l_group_str_i;
      else
        l_return_str := l_return_str||' and ('||l_group_str_i||')';
      end if;
    end if;
    if l_return_str is not null
    then
      l_return_str := '('||l_return_str||')';
    end if;

    return l_return_str;
  end build_group_condition_query;

  -- retrieve all ts_id members defined in a computation
  function get_computation_ts_members(
    p_computation_id  integer,
    p_inout_flag      varchar2,
    p_db_office_code  integer)
    return sys_refcursor
  is
    l_sql_text        varchar2(5000);
    l_qry_cursor      sys_refcursor;
  begin
    l_sql_text := null;

    /* retrieve all input ts id members defined in a computation or its TS group */
    if upper(p_inout_flag) = 'IN' then
      /* for the computation without a TS group */
      for r1 in
        (select cc.computation_id,ctp.site_datatype_id
           from cp_computation cc, cp_comp_ts_parm ctp,cp_algo_ts_parm atp
           where cc.computation_id = p_computation_id and upper(cc.enabled) = 'Y'
             and nvl(cc.group_id, -1) = -1 and lower(atp.parm_type) like 'i%'
             and ctp.computation_id = cc.computation_id and atp.algorithm_id = cc.algorithm_id
             and atp.algo_role_name = ctp.algo_role_name
        )
      loop
        l_sql_text := 'select cwms_ts_id from cwms_v_ts_id '||
                      'where db_office_code = '||rtrim(to_char(p_db_office_code))||
                      ' and ts_code = '||to_char(r1.site_datatype_id);

        dbms_output.put_line(l_sql_text);

        open l_qry_cursor for l_sql_text;
        return l_qry_cursor;
      end loop;

      /* for the computation with a TS group */
      for r1 in
        (select cc.computation_id,cc.group_id,tgp.db_office_code,ctp.site_datatype_id,
           nvl((select distinct dt.code from datatype dt where dt.id = ctp.datatype_id), '*') param,
           nvl(regexp_substr(ctp.table_selector,'[^.]*'), '*') param_type,
           nvl(ctp.interval, '*') interval,
           nvl(trim('.' from regexp_substr(ctp.table_selector, '(\.[[:alnum:]]\.)')), '*') duration,
           nvl(regexp_substr(ctp.table_selector,'[^.]*$'), '*') version
           from cp_computation cc, cp_comp_ts_parm ctp,cp_algo_ts_parm atp,tsdb_group tgp
           where cc.computation_id = p_computation_id and upper(cc.enabled) = 'Y'
           and nvl(cc.group_id, -1) <> -1 and lower(atp.parm_type) like 'i%'
           and ctp.computation_id = cc.computation_id and atp.algorithm_id = cc.algorithm_id
           and atp.algo_role_name = ctp.algo_role_name and tgp.group_id = cc.group_id
        )
      loop
        /* Build a sql query with all conditions in a computation */
        l_sql_text := 'select cwms_ts_id from cwms_v_ts_id '||
                      'where db_office_code = '||rtrim(to_char(r1.db_office_code));

        /* Add the query conditions defined in a computation */
        l_sql_text := rtrim(l_sql_text)||
                      rtrim(build_comp_condition_query(p_param     => r1.param,
                                                       p_paramtype => r1.param_type,
                                                       p_interval  => r1.interval,
                                                       p_duration  => r1.duration,
                                                       p_version   => r1.version));

        /* Add the query conditions defined in a TS group with its subgroups */
        l_sql_text := rtrim(l_sql_text)||' and '||
                      rtrim(build_group_condition_query(r1.group_id, get_group_condition_query(r1.group_id, '')))||
                      ' order by location_id';

        dbms_output.put_line(l_sql_text);

        open l_qry_cursor for l_sql_text;
        return l_qry_cursor;
      end loop;

      l_sql_text := 'select cwms_ts_id from cwms_v_ts_id '||
                    'where db_office_code = -1';
      open l_qry_cursor for l_sql_text;
      return l_qry_cursor;
    elsif upper(p_inout_flag) = 'OUT' then
      /* for the computation without a TS group */
      for r1 in
        (select cc.computation_id,ctp.site_datatype_id
           from cp_computation cc, cp_comp_ts_parm ctp,cp_algo_ts_parm atp
           where cc.computation_id = p_computation_id and upper(cc.enabled) = 'Y'
             and nvl(cc.group_id, -1) = -1 and lower(atp.parm_type) not like 'i%'
             and ctp.computation_id = cc.computation_id
             and atp.algorithm_id = cc.algorithm_id
             and atp.algo_role_name = ctp.algo_role_name
        )
      loop
        l_sql_text := 'select cwms_ts_id from cwms_v_ts_id '||
                      'where db_office_code = '||rtrim(to_char(p_db_office_code))||
                      ' and ts_code = '||to_char(r1.site_datatype_id);

        dbms_output.put_line(l_sql_text);

        open l_qry_cursor for l_sql_text;
        return l_qry_cursor;
      end loop;

      /* for the computation with a TS group */
      for r1 in
        (select cc.computation_id,cc.group_id,tgp.db_office_code,ctp.site_datatype_id,
           nvl((select distinct dt.code from datatype dt where dt.id = ctp.datatype_id), '*') param,
           nvl(regexp_substr(ctp.table_selector,'[^.]*'), '*') param_type,
           nvl(ctp.interval, '*') interval,
           nvl(trim('.' from regexp_substr(ctp.table_selector, '(\.[[:alnum:]]\.)')), '*') duration,
           nvl(regexp_substr(ctp.table_selector,'[^.]*$'), '*') version
           from cp_computation cc, cp_comp_ts_parm ctp,cp_algo_ts_parm atp,tsdb_group tgp
           where cc.computation_id = p_computation_id and upper(cc.enabled) = 'Y'
           and nvl(cc.group_id, -1) <> -1 and lower(atp.parm_type) not like 'i%'
           and ctp.computation_id = cc.computation_id and atp.algorithm_id = cc.algorithm_id
           and atp.algo_role_name = ctp.algo_role_name and tgp.group_id = cc.group_id
        )
      loop
        /* Build a sql query with all conditions in a computation */
        l_sql_text := 'select cwms_ts_id from cwms_v_ts_id '||
                      'where db_office_code = '||rtrim(to_char(r1.db_office_code));

        /* Add the query conditions defined in a computation */
        l_sql_text := rtrim(l_sql_text)||
                      rtrim(build_comp_condition_query(p_param     => r1.param,
                                                       p_paramtype => r1.param_type,
                                                       p_interval  => r1.interval,
                                                       p_duration  => r1.duration,
                                                       p_version   => r1.version));

        /* Add the query conditions defined in a TS group with its subgroups */
        l_sql_text := rtrim(l_sql_text)||' and '||
                      rtrim(build_group_condition_query(r1.group_id, get_group_condition_query(r1.group_id, '')))||
                      ' order by location_id';

        dbms_output.put_line(l_sql_text);

        open l_qry_cursor for l_sql_text;
        return l_qry_cursor;
      end loop;

      l_sql_text := 'select cwms_ts_id from cwms_v_ts_id '||
                    'where db_office_code = -1';
      open l_qry_cursor for l_sql_text;
      return l_qry_cursor;
    else
      l_sql_text := 'select cwms_ts_id from cwms_v_ts_id '||
                    'where db_office_code = -1';
      open l_qry_cursor for l_sql_text;
      return l_qry_cursor;
    end if;
  end get_computation_ts_members;

  -- retrieve all ts_id members defined in a TS group
  function get_group_ts_members(
    p_group_id        integer,
    p_db_office_code  integer)
    return sys_refcursor
  is
    l_sql_text      varchar2(5000);
    l_qry_cursor    sys_refcursor;
  begin
    l_sql_text := 'select cwms_ts_id from cwms_v_ts_id '||
                  'where db_office_code = '||to_char(p_db_office_code)||' and '||
                  rtrim(build_group_condition_query(p_group_id, get_group_condition_query(p_group_id, '')));

    dbms_output.put_line(l_sql_text);

    open l_qry_cursor for l_sql_text;

    return l_qry_cursor;
  end get_group_ts_members;

  -------------------------------------------------------------------------
  -- notify the ts data stored
  -------------------------------------------------------------------------
  procedure notify_tsdatastored (
    p_tsid            in varchar2,
    p_ts_code         in integer,
    p_start_time      in timestamp,
    p_end_time        in timestamp,
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
    l_unit_id    := &cwms_schema..cwms_ts.get_db_unit_id(p_tsid);

    for r2 in
      (select distinct cc.loading_application_id
         from cp_comp_depends cd, cp_computation cc
         where cd.site_datatype_id = p_ts_code and cc.enabled = 'Y'
           and cc.loading_application_id is not null and cd.computation_id = cc.computation_id
      )
    loop
      for r1 in
        (select date_time,value,quality_code from cwms_v_tsv
           where ts_code = p_ts_code and date_time between l_start_time and l_end_time
             and p_enqueue_time >= data_entry_date
        )
      loop
        l_delete_flag := 'N';
        if (r1.quality_code = 5) then
          l_delete_flag := 'Y';
        end if;

        insert into cp_comp_tasklist(record_num,loading_application_id,
          site_datatype_id,date_time_loaded,start_date_time,value,
          unit_id,delete_flag,quality_code,flags)
        values(cp_comp_tasklistidseq.nextval,r2.loading_application_id,
          p_ts_code,sysdate,r1.date_time,nvl(r1.value,0),l_unit_id,
          l_delete_flag,r1.quality_code,r1.quality_code);
      end loop; /* end of for r1 loop */
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
    l_rec_count       number;
    l_level_count     number;
    l_sql_txt         varchar2(20000);
  begin
    /* Search all computations with TS groups */
    for r1 in
      (select cc.computation_id,cc.group_id,tgp.db_office_code,ctp.site_datatype_id,
         nvl((select distinct dt.code from datatype dt where dt.id = ctp.datatype_id), '*') param,
         nvl(regexp_substr(ctp.table_selector,'[^.]*'), '*') param_type,
         nvl(ctp.interval, '*') interval,
         nvl(trim('.' from regexp_substr(ctp.table_selector, '(\.[[:alnum:]]\.)')), '*') duration,
         nvl(regexp_substr(ctp.table_selector,'[^.]*$'), '*') version
         from cp_computation cc, cp_comp_ts_parm ctp,cp_algo_ts_parm atp,tsdb_group tgp
         where upper(cc.enabled) = 'Y' and nvl(cc.group_id, -1) <> -1 and lower(atp.parm_type) like 'i%'
         and ctp.computation_id = cc.computation_id and atp.algorithm_id = cc.algorithm_id
         and atp.algo_role_name = ctp.algo_role_name and tgp.group_id = cc.group_id
      )
    loop
      /* Build a sql query with all conditions in a computation */
      l_sql_txt := 'select count(*) into :1 from cwms_v_ts_id '||
        'where db_office_code = '||rtrim(to_char(r1.db_office_code))||' ';

      /* Add the query conditions defined in a computation */
      l_sql_txt := rtrim(l_sql_txt)||
                   rtrim(build_comp_condition_query(p_param     => r1.param,
                                                    p_paramtype => r1.param_type,
                                                    p_interval  => r1.interval,
                                                    p_duration  => r1.duration,
                                                    p_version   => r1.version));

      /* Add the query condition for new ts_id */
      l_sql_txt := rtrim(l_sql_txt)||' and cwms_ts_id in('''||p_tsid||''') ';

      /* Add the query conditions defined in a TS group with its subgroups */
      l_sql_txt := rtrim(l_sql_txt)||' and '||
                   rtrim(build_group_condition_query(r1.group_id, get_group_condition_query(r1.group_id, '')));

      /* Make an executable query block */
      l_sql_txt := 'begin '||rtrim(l_sql_txt)||'; end;';

      dbms_output.put_line('Computation ID: '||r1.computation_id||' Group ID: '||r1.group_id);
      dbms_output.put_line(l_sql_txt);

      execute immediate l_sql_txt using out l_rec_count;

      /* Add an entry in the cp_comp_depends table if the new ts_id is the member defined in a computation and its group */
      if l_rec_count > 0
      then
        dbms_output.put_line('Computation_ID: '||r1.computation_id||' TS_Code: '||p_ts_code);

        delete from cp_comp_depends where computation_id = r1.computation_id and site_datatype_id = p_ts_code;
        insert into cp_comp_depends(computation_id,site_datatype_id) values(r1.computation_id, p_ts_code);
        commit;
      end if;

    end loop;   /* end of for r1 loop */
  end notify_tscreated;

  -------------------------------------------------------------------------
  -- notify the ts code deleted
  -------------------------------------------------------------------------
  procedure notify_tsdeleted (
    p_tsid            in varchar2,
    p_ts_code         in integer)
  is
  begin
    delete from cp_comp_depends where site_datatype_id = p_ts_code;
    update cp_computation set enabled = 'N' where computation_id in(
      select distinct computation_id from cp_comp_ts_parm where site_datatype_id = p_ts_code);
    update cp_comp_ts_parm set site_datatype_id = -1,
                               interval = null,
                               table_selector = '..',
                               datatype_id = -1
      where site_datatype_id = p_ts_code;
    delete from tsdb_group_member_ts where data_id = p_ts_code;
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
         where cd.site_datatype_id = p_ts_code and cc.enabled = 'Y'
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
  -- notify the ts code deleted
  -------------------------------------------------------------------------
  procedure notify_tscodechanged(
    p_tsid            in varchar2,
    p_ts_code         in integer,
    p_ts_code_old     in integer)
  is
  begin
    for r2 in
      (select distinct cc.loading_application_id
         from cp_comp_depends cd, cp_computation cc
         where cd.site_datatype_id = p_ts_code_old and cc.enabled = 'Y'
           and cc.loading_application_id is not null and cd.computation_id = cc.computation_id
      )
    loop
      insert into cp_comp_tasklist(record_num,loading_application_id,site_datatype_id,
        model_run_id,date_time_loaded,start_date_time,delete_flag,flags)
      values(cp_comp_tasklistidseq.nextval,r2.loading_application_id,p_ts_code_old,
        p_ts_code,sysdate,sysdate,'U',0);
    end loop; /* end of for r2 loop */

    update cp_comp_depends set site_datatype_id = p_ts_code where site_datatype_id = p_ts_code_old;
    update cp_comp_ts_parm set site_datatype_id = p_ts_code where site_datatype_id = p_ts_code_old;
    update tsdb_group_member_ts set data_id = p_ts_code where data_id = p_ts_code_old;
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
    l_rec_count       number;
    l_level_count     number;
    l_sql_txt         varchar2(20000);
    l_interval        varchar2(50);
    l_param           varchar2(50);
    l_table_selector  varchar2(100);
    l_datatype_id     number;
  begin
    /* Search all computations without TS groups */
    l_param := regexp_substr(p_tsid_new, '[^.]+', 1, 2);
    l_interval := regexp_substr(p_tsid_new, '[^.]+', 1, 4);
    l_table_selector := regexp_substr(p_tsid_new, '[^.]+', 1, 3)||'.'||
                        regexp_substr(p_tsid_new, '[^.]+', 1, 5)||'.'||
                        regexp_substr(p_tsid_new, '[^.]+', 1, 6);
    select distinct dt.id into l_datatype_id from datatype dt where dt.code = l_param;

    for r1 in
      (select ctp.computation_id,ctp.algo_role_name,ctp.datatype_id from cp_comp_ts_parm ctp
         where ctp.site_datatype_id = p_ts_code and ctp.computation_id in(
           select computation_id from cp_computation
           where upper(enabled) = 'Y' and nvl(group_id, -1) = -1)
      )
    loop
      update cp_comp_ts_parm set interval = l_interval,
          table_selector = l_table_selector,datatype_id = nvl(l_datatype_id, r1.datatype_id)
        where computation_id = r1.computation_id and algo_role_name = r1.algo_role_name
          and site_datatype_id = p_ts_code;
      commit;
    end loop;   /* end of for r1 loop */

    /* Search all computations with TS groups */
    for r1 in
      (select cc.computation_id,cc.group_id,tgp.db_office_code,ctp.site_datatype_id,
         nvl((select distinct dt.code from datatype dt where dt.id = ctp.datatype_id), '*') param,
         nvl(regexp_substr(ctp.table_selector,'[^.]*'), '*') param_type,
         nvl(ctp.interval, '*') interval,
         nvl(trim('.' from regexp_substr(ctp.table_selector, '(\.[[:alnum:]]\.)')), '*') duration,
         nvl(regexp_substr(ctp.table_selector,'[^.]*$'), '*') version
         from cp_computation cc, cp_comp_ts_parm ctp,cp_algo_ts_parm atp,tsdb_group tgp
         where cc.enabled = 'Y' and nvl(cc.group_id, -1) <> -1 and atp.parm_type like 'i%'
         and ctp.computation_id = cc.computation_id and atp.algorithm_id = cc.algorithm_id
         and atp.algo_role_name = ctp.algo_role_name and tgp.group_id = cc.group_id
      )
    loop
      /* Build a sql query with all conditions in a computation */
      l_sql_txt := 'select count(*) into :1 from cwms_v_ts_id '||
        'where db_office_code = '||to_char(r1.db_office_code)||' ';

      /* Add the query conditions defined in a computation */
      l_sql_txt := rtrim(l_sql_txt)||
                   rtrim(build_comp_condition_query(p_param     => r1.param,
                                                    p_paramtype => r1.param_type,
                                                    p_interval  => r1.interval,
                                                    p_duration  => r1.duration,
                                                    p_version   => r1.version));

      /* Add the query condition for new ts_id */
      l_sql_txt := rtrim(l_sql_txt)||' and cwms_ts_id in('''||p_tsid_new||''') ';

      /* Add the query conditions defined in a TS group with its subgroups */
      l_sql_txt := rtrim(l_sql_txt)||' and '||
                   rtrim(build_group_condition_query(r1.group_id, get_group_condition_query(r1.group_id, '')));

      /* Make an executable query block */
      l_sql_txt := 'begin '||l_sql_txt||'; end;';

      dbms_output.put_line('Computation ID: '||r1.computation_id||' Group ID: '||r1.group_id);
      dbms_output.put_line(l_sql_txt);

      execute immediate l_sql_txt using out l_rec_count;

      /* Add an entry in the cp_comp_depends table if the new ts_id is
         the member defined in a computation and its group; otherwise, remove it. */
      delete from cp_comp_depends where computation_id = r1.computation_id and site_datatype_id = p_ts_code;
      if l_rec_count > 0
      then
        dbms_output.put_line('Computation_ID: '||r1.computation_id||' TS_Code: '||p_ts_code);

        insert into cp_comp_depends(computation_id,site_datatype_id) values(r1.computation_id, p_ts_code);
        commit;
      end if;

    end loop;   /* end of for r1 loop */

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
            l_tsid           := get_string(l_message, l_msgid, 'ts_id', l_tsid_len);
            l_enqueue_millis := l_message.get_long(l_msgid, 'millis');
            l_enqueue_time   := &cwms_schema..cwms_util.to_timestamp(l_enqueue_millis);
            l_queue_delay    := &cwms_schema..cwms_util.to_millis(l_dequeue_time) - l_enqueue_millis;
            -----------------------------------------------------------------
            -- operate on the message based on its type
            -----------------------------------------------------------------
            case l_msgtype
              when 'TSDataStored' then
                l_store_rule   := get_string(l_message, l_msgid, 'store_rule', l_store_rule_len);

                l_ts_code      := l_message.get_long(l_msgid, 'ts_code');
                l_start_millis := l_message.get_long(l_msgid, 'start_time');
                l_end_millis   := l_message.get_long(l_msgid, 'end_time');
                l_store_millis := l_message.get_long(l_msgid, 'store_time');
                l_version_date := l_message.get_long(l_msgid, 'version_date');

                l_start_time   := &cwms_schema..cwms_util.to_timestamp(l_start_millis);
                l_end_time     := &cwms_schema..cwms_util.to_timestamp(l_end_millis);
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
                  p_enqueue_time    => l_enqueue_time);

              when 'TSDataDeleted' then
                l_ts_code      := l_message.get_long(l_msgid, 'ts_code');
                l_start_millis := l_message.get_long(l_msgid, 'start_time');
                l_end_millis   := l_message.get_long(l_msgid, 'end_time');
                l_start_time   := &cwms_schema..cwms_util.to_timestamp(l_start_millis);
                l_end_time     := &cwms_schema..cwms_util.to_timestamp(l_end_millis);
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
                l_comment      := null;

                notify_tsdeleted(
                  p_tsid            => l_tsid,
                  p_ts_code         => l_ts_code);

              when 'TSCodeChanged' then
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
                'Error in processing messages: '
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

show errors;

---------------------------------------------------------------------------
-- Grant execute privilege on packages TO cwms_user
---------------------------------------------------------------------------
GRANT EXECUTE ON &ccp_schema..cwms_ccp TO CCP_USERS;

---------------------------------------------------------------------------
-- Create public synonyms for CCP packages
---------------------------------------------------------------------------
DROP PUBLIC SYNONYM cwms_ccp;
CREATE PUBLIC SYNONYM cwms_ccp FOR &ccp_schema..cwms_ccp;


---------------------------------------------------------------------------
-- Start the check callback procedure job
---------------------------------------------------------------------------
begin
  &ccp_schema..cwms_ccp.reload_callback_proc(
    'CCP_SUBSCRIBER');

  &ccp_schema..cwms_ccp.start_check_callback_proc_job;
end;
/

---------------------------------------------------------------------------
-- Create the context, package, and triger for multipe-offices
---------------------------------------------------------------------------
create or replace context CCPENV using cwms_ccp_vpd;

create or replace package cwms_ccp_vpd authid current_user as
  procedure set_session_office_code (
    p_db_office_code  integer default null)
  ;

  procedure set_session_office_id (
    p_db_office_id    varchar2 default null)
  ;

  procedure set_session_cwms_group_id (
    p_db_office_id    varchar2)
  ;

  procedure set_session_ccp_priv
  ;

  procedure set_session_ccp_debug_flag (
    p_ccp_debug_flag  integer default 0)
  ;

  -- View data VPD policy function
  function get_pred_session_office_code_v (
    p_schema  in varchar2,
    p_table   in varchar2)
    return varchar2
  ;

  -- Update data VPD policy function
  function get_pred_session_office_code_u (
    p_schema  in varchar2,
    p_table   in varchar2)
    return varchar2
  ;

end cwms_ccp_vpd;
/

create or replace package body cwms_ccp_vpd as
  k_cwms_ccp_proc           varchar2(20) := 'CCP PROC';
  k_cwms_ccp_mgr            varchar2(20) := 'CCP MGR';
  k_cwms_ccp_reviewer       varchar2(20) := 'CCP REVIEWER';
  k_cwms_dba_user           varchar2(20) := 'CWMS DBA USERS';

  k_ccp_priv_none           varchar2(20) := 'CCP_PRIV_NONE';
  k_ccp_priv_proc           varchar2(20) := 'CCP_PRIV_PROC';
  k_ccp_priv_mngr           varchar2(20) := 'CCP_PRIV_MNGR';
  k_ccp_priv_rvwr           varchar2(20) := 'CCP_PRIV_RVWR';

  k_ccp_env                 varchar2(20) := 'CCPENV';
  k_ccp_priv                varchar2(20) := 'CCP_PRIV';
  k_ccp_office_code         varchar2(20) := 'CCP_OFFICE_CODE';
  k_ccp_group_id            varchar2(20) := 'CCP_GROUP_ID';
  k_ccp_debug_flag          varchar2(20) := 'CCP_DEBUG_FLAG';

  k_session_user_name       varchar2(20) := SYS_CONTEXT('USERENV', 'SESSION_USER');
  k_session_ccp_group_id    varchar2(20) := SYS_CONTEXT(k_ccp_env, k_ccp_group_id);
  k_session_ccp_office_code varchar2(20) := SYS_CONTEXT(k_ccp_env, k_ccp_office_code);
  k_session_ccp_debug_flag  varchar2(20) := SYS_CONTEXT(k_ccp_env, k_ccp_debug_flag);

  procedure set_session_office_code (
    p_db_office_code  integer default null)
  is
    l_db_office_code  integer;
    l_db_office_id    varchar2(10);
  begin
    if p_db_office_code is null then
/*
      select user_db_office_code into l_db_office_code from &cwms_schema..at_sec_user_office
        where username = k_session_user_name;
*/
      l_db_office_code := &cwms_schema..cwms_util.get_db_office_code;
    else
      l_db_office_code := p_db_office_code;
    end if;
    DBMS_SESSION.SET_CONTEXT(k_ccp_env, k_ccp_office_code, l_db_office_code);

    l_db_office_id := &cwms_schema..cwms_util.get_db_office_id_from_code(l_db_office_code);
    set_session_cwms_group_id(l_db_office_id);

    set_session_ccp_priv();

  exception
    when NO_DATA_FOUND then null;
  end set_session_office_code;

  procedure set_session_office_id (
    p_db_office_id    varchar2 default null)
  is
    l_db_office_code  integer;
  begin
    if p_db_office_id is null then
      l_db_office_code := null;
    else
      l_db_office_code := &cwms_schema..cwms_util.get_office_code(
                              p_office_id   =>  p_db_office_id);
    end if;

    set_session_office_code(p_db_office_code =>  l_db_office_code);
  end set_session_office_id;

  procedure set_session_cwms_group_id (
    p_db_office_id    varchar2)
  is
    l_user_group_id        varchar2(20);
    l_db_office_id         varchar2(10);
    l_ccp_priv             varchar2(20);
  begin
    l_db_office_id  := p_db_office_id;
    l_ccp_priv      := k_ccp_priv_none;
    l_user_group_id := null;

    if l_db_office_id is not null then
      for r1 in
        (select distinct t.user_group_id
            from table (&cwms_schema..cwms_sec.get_assigned_priv_groups_tab) t
            where t.username = k_session_user_name and t.db_office_id = l_db_office_id
              and upper(t.user_group_id) in(k_cwms_dba_user,k_cwms_ccp_mgr,k_cwms_ccp_proc,k_cwms_ccp_reviewer)
              order by t.user_group_id desc
        )
      loop
        l_user_group_id := r1.user_group_id;
        case upper(l_user_group_id)
            when k_cwms_dba_user then
                l_ccp_priv := k_ccp_priv_mngr;
            when k_cwms_ccp_mgr then
                l_ccp_priv := k_ccp_priv_mngr;
            when k_cwms_ccp_proc then
                l_ccp_priv := k_ccp_priv_proc;
            when k_cwms_ccp_reviewer then
                l_ccp_priv := k_ccp_priv_rvwr;
            else
                l_ccp_priv := k_ccp_priv_none;
        end case;
        exit when l_ccp_priv = k_ccp_priv_mngr;
      end loop;
    end if;
    DBMS_SESSION.SET_CONTEXT(k_ccp_env, k_ccp_priv, l_ccp_priv);
    DBMS_SESSION.SET_CONTEXT(k_ccp_env, k_ccp_group_id, l_user_group_id);

  exception
    when NO_DATA_FOUND then null;
  end set_session_cwms_group_id;

  procedure set_session_ccp_priv
  is
    l_session_ccp_priv        varchar2(20) := null;
  begin
    l_session_ccp_priv := SYS_CONTEXT(k_ccp_env, k_ccp_priv);
    case upper(l_session_ccp_priv)
        when k_ccp_priv_rvwr then
            execute immediate 'set role all except "CCP_USERS._W","CCP_USERS._P"';
        when k_ccp_priv_mngr then
            execute immediate 'set role all except "CCP_USERS._P"';
        when k_ccp_priv_proc then
            execute immediate 'set role all except "CCP_USERS._W"';
        when k_ccp_priv_none then
            execute immediate 'set role all except "CCP_USERS._R","CCP_USERS._W","CCP_USERS._P"';
        else
            execute immediate 'set role all except "CCP_USERS._R","CCP_USERS._W","CCP_USERS._P"';
    end case;
  end set_session_ccp_priv;

  procedure set_session_ccp_debug_flag (
    p_ccp_debug_flag  integer default 0)
  is
    l_ccp_debug_flag  integer;
  begin
    l_ccp_debug_flag := p_ccp_debug_flag;

    DBMS_SESSION.SET_CONTEXT(k_ccp_env, k_ccp_debug_flag, l_ccp_debug_flag);

  exception
    when NO_DATA_FOUND then null;
  end set_session_ccp_debug_flag;

  function get_pred_session_office_code_v (
    p_schema  in varchar2,
    p_table   in varchar2)
    return varchar2
  is
    l_pred    varchar2(400);
    l_session_ccp_office_code varchar2(20) := null;
    l_session_ccp_priv        varchar2(20) := null;
  begin
    l_pred := '1 = 0';

    -- This is required by the queue handler
    if upper(k_session_user_name) in ('&sys_schema')
    then
      l_pred := '1 = 1';
    elsif upper(k_session_user_name) in ('&ccp_schema', '&cwms_schema')
    then
      if k_session_ccp_debug_flag > 0 then
        l_pred := '1 = 1';
      end if;
    else
      l_session_ccp_office_code := SYS_CONTEXT(k_ccp_env, k_ccp_office_code);
      if l_session_ccp_office_code is not null then
        l_session_ccp_priv := upper(SYS_CONTEXT(k_ccp_env, k_ccp_priv));
        if (l_session_ccp_priv = k_ccp_priv_mngr) or (l_session_ccp_priv = k_ccp_priv_proc) or (l_session_ccp_priv = k_ccp_priv_rvwr) then
          l_pred := 'db_office_code = '||l_session_ccp_office_code;
        end if;
      end if;
    end if;

    dbms_output.put_line('session office code: '||l_session_ccp_office_code);
    dbms_output.put_line('predicate: '||l_pred);

    return l_pred;
  end get_pred_session_office_code_v;

  function get_pred_session_office_code_u (
    p_schema  in varchar2,
    p_table   in varchar2)
    return varchar2
  is
    l_pred    varchar2(400);
    l_session_ccp_office_code varchar2(20) := null;
    l_session_ccp_priv        varchar2(20) := null;
  begin
    l_pred := '1 = 0';

    -- This is required by the queue handler
    if upper(k_session_user_name) in ('&sys_schema')
    then
      l_pred := '1 = 1';
    elsif upper(k_session_user_name) in ('&ccp_schema', '&cwms_schema')
    then
      if k_session_ccp_debug_flag > 0 then
        l_pred := '1 = 1';
      end if;
    else
      l_session_ccp_office_code := SYS_CONTEXT(k_ccp_env, k_ccp_office_code);
      if l_session_ccp_office_code is not null then
        l_session_ccp_priv := upper(SYS_CONTEXT(k_ccp_env, k_ccp_priv));
        if (l_session_ccp_priv = k_ccp_priv_mngr) or (l_session_ccp_priv = k_ccp_priv_proc) then
          l_pred := 'db_office_code = '||l_session_ccp_office_code;
        end if;
      end if;
    end if;

    dbms_output.put_line('session office code: '||l_session_ccp_office_code);
    dbms_output.put_line('predicate: '||l_pred);

    return l_pred;
  end get_pred_session_office_code_u;

end cwms_ccp_vpd;
/

show errors;

---------------------------------------------------------------------------
-- Grant execute privilege on packages TO cwms_user
---------------------------------------------------------------------------
GRANT EXECUTE ON &ccp_schema..cwms_ccp_vpd TO CCP_USERS;

---------------------------------------------------------------------------
-- Create synonym for CWMS_CCP_VPD package
---------------------------------------------------------------------------
DROP PUBLIC SYNONYM cwms_ccp_vpd;
CREATE PUBLIC SYNONYM cwms_ccp_vpd FOR &ccp_schema..cwms_ccp_vpd;


spool off
exit;
