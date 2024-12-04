---------------------------------------------------------------------------
-- Create the context, package, and triger for multipe-offices
---------------------------------------------------------------------------
-- MJM 20190307 As per HEC, use CWMS_ENV, not CCPENV
-- create or replace context CCPENV using cwms_ccp_vpd;

create or replace package cwms_ccp_vpd authid current_user as
  procedure set_ccp_session_ctx(
    p_db_office_code  integer default null,
    p_ccp_priv_level  integer default null,
    p_db_office_id    varchar2 default null
  )
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
  k_ccp_env                 varchar2(20) := 'CWMS_ENV';
  k_ccp_office_code         varchar2(20) := 'SESSION_OFFICE_CODE';
  k_ccp_office_id           varchar2(20) := 'SESSION_OFFICE_ID';
  k_ccp_priv_level          varchar2(20) := 'CCP_PRIV_LEVEL';

  k_session_user_name       varchar2(20) := SYS_CONTEXT('USERENV', 'SESSION_USER');

  -------------------------------------------------------------------------------------
  -- CWMS 3.0 session context method.
  -- Java figures out the appropriate db_office_code and privilege
  -- Meaning of p_ccp_priv_level:
  --   0 = Administrator: Predicate is always '1 = 1'. Record creation disallowed.
  --       When called this way p_db_office_code can be null
  --   1 = Manager: Full read/write access to all tables. Predicate checks db_office_code
  --   2 = Process: Read access to all tables. Write access to cp_comp_tasklist and
  --       cp_comp_proc_lock, cp_comp_depends, and cp_comp_depends_scratchpad.
  --   3 = Reviewer: Read access to all tables only
  --   4 = Equivalent to never calling this method. Access denied to everything.
  -------------------------------------------------------------------------------------
  procedure set_ccp_session_ctx(
    p_db_office_code  integer default null,
    p_ccp_priv_level  integer default null,
    p_db_office_id    varchar2 default null
  )
  is
  begin
    CWMS_ENV.SET_SESSION_OFFICE_ID (p_db_office_id);
  end set_ccp_session_ctx;


  ------------------------------------------------------------------------------------
  -- VPD Predicate Method for SELECT statements
  ------------------------------------------------------------------------------------
  function get_pred_session_office_code_v (
    p_schema  in varchar2,
    p_table   in varchar2)
    return varchar2
  is
    l_pred    varchar2(400);
    l_session_ccp_office_code integer := null;
    l_ccp_priv_level  integer := 4;
  begin
    l_pred := '1 = 0';

	l_session_ccp_office_code := SYS_CONTEXT(k_ccp_env, k_ccp_office_code);

    -- This is required by the queue handler
    if upper(k_session_user_name) in ('SYS', '${CCP_SCHEMA}', '${CWMS_SCHEMA}')
    then
      l_pred := '1 = 1';
    else
      -- Read privilege level from context or default to level 4
      l_ccp_priv_level := SYS_CONTEXT(k_ccp_env, k_ccp_priv_level);
      if l_ccp_priv_level is null then
        l_ccp_priv_level := 4;
      end if;

      if l_ccp_priv_level = 0 then
        l_pred := '1 = 1';
      elsif l_ccp_priv_level >= 1 and l_ccp_priv_level <= 3 then
        l_pred := 'db_office_code = '||l_session_ccp_office_code;
      else
        l_pred := '1 = 0';
      end if;
    end if;

    dbms_output.put_line('session office code: '||l_session_ccp_office_code);
    dbms_output.put_line('predicate: '||l_pred);

    return l_pred;
  end get_pred_session_office_code_v;

  ------------------------------------------------------------------------------------
  -- VPD Predicate Method for UPDATE statements
  ------------------------------------------------------------------------------------
  function get_pred_session_office_code_u (
    p_schema  in varchar2,
    p_table   in varchar2)
    return varchar2
  is
    l_pred    varchar2(400);
    l_session_ccp_office_code integer := null;
    l_ccp_priv_level  integer := 4;
  begin
    l_pred := '1 = 0';

	l_session_ccp_office_code := SYS_CONTEXT(k_ccp_env, k_ccp_office_code);

    -- This is required by the queue handler
    if upper(k_session_user_name) in ('SYS', '${CCP_SCHEMA}', '${CWMS_SCHEMA}')
    then
      l_pred := '1 = 1';
    else
      -- Read privilege level from context or default to level 4
      l_ccp_priv_level := SYS_CONTEXT(k_ccp_env, k_ccp_priv_level);
      if l_ccp_priv_level is null then
        l_ccp_priv_level := 4;
      end if;

      -- sys or superuser
      if l_ccp_priv_level = 0 then
        l_pred := '1 = 1';
      -- manager
      elsif l_ccp_priv_level = 1 then
        l_pred := 'db_office_code = '||l_session_ccp_office_code;
      -- process can modify specific tables
      elsif l_ccp_priv_level = 2 then
        if upper(p_table) in ('CP_COMP_PROC_LOCK', 'CP_COMP_TASKLIST',
          'CP_COMP_DEPENDS', 'CP_COMP_DEPENDS_SCRATCHPAD')
        then
          l_pred := 'db_office_code = '||l_session_ccp_office_code;
        else
          raise_application_error(-20100, 'CCP Process cannot modify this table');
        end if;
      -- reviewer cannot modify anything
      elsif l_ccp_priv_level >= 3 then
        raise_application_error(-20100, 'CCP Reviewer cannot modify in this office');
      end if;
    end if;

    dbms_output.put_line('session office code: '||l_session_ccp_office_code);
    dbms_output.put_line('predicate: '||l_pred);

    return l_pred;
  end get_pred_session_office_code_u;

end cwms_ccp_vpd;
/
