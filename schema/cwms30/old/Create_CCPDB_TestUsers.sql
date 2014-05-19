---------------------------------------------------------------------------
--
-- Sutron Ilex CCP Database on ORACLE
-- Database Version: 8        Date: 2013/05/02
-- Company: Sutron Corporation
--  Writer: GC
--
---------------------------------------------------------------------------
---------------------------------------------------------------------------
-- This script file is used to create several CCP DB users on CWMS/CCP DB.
-- Those DB users will be used for testing the CCP application against the
-- single office ID or the multi-office IDs with assigned CWMS App roles.
---------------------------------------------------------------------------
---------------------------------------------------------------------------
-- Here 8 CCP DB users are created against DB office IDs (MVR and NAE) with
-- the certain CWMS App roles described in the document
---------------------------------------------------------------------------
set echo on
spool create_CCPDB_TestUsers.out

whenever sqlerror continue
set define on
@@defines.sql

define sys_schema     = &1
define sys_passwd     = &2
define tns_name       = &3

connect &sys_schema/&sys_passwd@&tns_name as sysdba;

---------------------------------------------------------------------------
-- 1. The CCP user on an single office (MVR) with the CCP Reviewer App role
---------------------------------------------------------------------------
define cwms_office_id = MVR;
define user_schema = CCPMVR_R;
define user_passwd = CCPMVR_R;

DROP USER &user_schema CASCADE;
CREATE USER &user_schema IDENTIFIED BY &user_passwd
DEFAULT TABLESPACE &ts_data_name QUOTA UNLIMITED ON &ts_data_name
TEMPORARY TABLESPACE &ts_temp_name
PROFILE DEFAULT
ACCOUNT UNLOCK;

GRANT CWMS_USER,CCP_USERS TO &user_schema;
GRANT "CCP_USERS._R","CCP_USERS._W","CCP_USERS._P" TO &user_schema WITH ADMIN OPTION;
ALTER USER &user_schema DEFAULT ROLE ALL EXCEPT "CCP_USERS._W","CCP_USERS._P";

BEGIN
  begin
    for rec in
      (select user_group_id from table(&cwms_schema..cwms_sec.get_user_priv_groups_tab(
                                         p_username     => '&user_schema',
                                         p_db_office_id => '&cwms_office_id'))
        where is_member in('T') and user_group_id not in('All Users')
       )
    loop
      &cwms_schema..cwms_sec.remove_user_from_group (
        p_username              => '&user_schema',
        p_user_group_id         => rec.user_group_id,
        p_db_office_id          => '&cwms_office_id'
      );
    end loop;

    &cwms_schema..cwms_sec.create_user(
      p_username                => '&user_schema',
      p_password                => '&user_passwd',
      p_user_group_id_list      => CWMS_T_CHAR_32_ARRAY(
                                        'CCP Reviewer',
                                        'CWMS Users'),
      p_db_office_id            => '&cwms_office_id'
    );
  exception
    when others then
      raise_application_error(-20999, sqlerrm, true);
  end;
END;
/

---------------------------------------------------------------------------
-- 2. The CCP user on an single office (MVR) with the CCP Proc App role
---------------------------------------------------------------------------
define cwms_office_id = MVR;
define user_schema = CCPMVR_P;
define user_passwd = CCPMVR_P;

DROP USER &user_schema CASCADE;
CREATE USER &user_schema IDENTIFIED BY &user_passwd
DEFAULT TABLESPACE &ts_data_name QUOTA UNLIMITED ON &ts_data_name
TEMPORARY TABLESPACE &ts_temp_name
PROFILE DEFAULT
ACCOUNT UNLOCK;

GRANT CCP_USERS, CWMS_USER TO &user_schema;
GRANT "CCP_USERS._R","CCP_USERS._W","CCP_USERS._P" TO &user_schema WITH ADMIN OPTION;
ALTER USER &user_schema DEFAULT ROLE ALL EXCEPT "CCP_USERS._W";

BEGIN
  begin
    for rec in
      (select user_group_id from table(&cwms_schema..cwms_sec.get_user_priv_groups_tab(
                                         p_username     => '&user_schema',
                                         p_db_office_id => '&cwms_office_id'))
        where is_member in('T') and user_group_id not in('All Users')
       )
    loop
      &cwms_schema..cwms_sec.remove_user_from_group (
        p_username              => '&user_schema',
        p_user_group_id         => rec.user_group_id,
        p_db_office_id          => '&cwms_office_id'
      );
    end loop;

    &cwms_schema..cwms_sec.create_user(
      p_username                => '&user_schema',
      p_password                => '&user_passwd',
      p_user_group_id_list      => CWMS_T_CHAR_32_ARRAY(
                                        'CCP Proc',
                                        'CWMS PD Users',
                                        'TS ID Creator',
                                        'CWMS Users'),
      p_db_office_id            => '&cwms_office_id'
    );
  exception
    when others then
      raise_application_error(-20999, sqlerrm, true);
  end;
END;
/

---------------------------------------------------------------------------
-- 3. The CCP user on an single office (MVR) with the CCP Mgr App role
---------------------------------------------------------------------------
define cwms_office_id = MVR;
define user_schema = CCPMVR_M;
define user_passwd = CCPMVR_M;

DROP USER &user_schema CASCADE;
CREATE USER &user_schema IDENTIFIED BY &user_passwd
DEFAULT TABLESPACE &ts_data_name QUOTA UNLIMITED ON &ts_data_name
TEMPORARY TABLESPACE &ts_temp_name
PROFILE DEFAULT
ACCOUNT UNLOCK;

GRANT CWMS_USER,CCP_USERS TO &user_schema;
GRANT "CCP_USERS._R","CCP_USERS._W","CCP_USERS._P" TO &user_schema WITH ADMIN OPTION;
ALTER USER &user_schema DEFAULT ROLE ALL EXCEPT "CCP_USERS._P";

BEGIN
  begin
    for rec in
      (select user_group_id from table(&cwms_schema..cwms_sec.get_user_priv_groups_tab(
                                         p_username     => '&user_schema',
                                         p_db_office_id => '&cwms_office_id'))
        where is_member in('T') and user_group_id not in('All Users')
       )
    loop
      &cwms_schema..cwms_sec.remove_user_from_group (
        p_username              => '&user_schema',
        p_user_group_id         => rec.user_group_id,
        p_db_office_id          => '&cwms_office_id'
      );
    end loop;

    &cwms_schema..cwms_sec.create_user(
      p_username                => '&user_schema',
      p_password                => '&user_passwd',
      p_user_group_id_list      => CWMS_T_CHAR_32_ARRAY(
                                        'CCP Mgr',
                                        'CWMS PD Users',
                                        'TS ID Creator',
                                        'Data Acquisition Mgr',
                                        'CWMS Users'),
      p_db_office_id            => '&cwms_office_id'
    );
  exception
    when others then
      raise_application_error(-20999, sqlerrm, true);
  end;
END;
/

---------------------------------------------------------------------------
-- 4. The CCP user on multi-offices MVR/NAE with CCP Rvwr/Rvwr App role
---------------------------------------------------------------------------
define cwms_office_id1 = MVR;
define cwms_office_id2 = NAE;
define user_schema = CCPBOTH_R;
define user_passwd = CCPBOTH_R;

DROP USER &user_schema CASCADE;
CREATE USER &user_schema IDENTIFIED BY &user_passwd
DEFAULT TABLESPACE &ts_data_name QUOTA UNLIMITED ON &ts_data_name
TEMPORARY TABLESPACE &ts_temp_name
PROFILE DEFAULT
ACCOUNT UNLOCK;

GRANT CCP_USERS, CWMS_USER TO &user_schema;
GRANT "CCP_USERS._R","CCP_USERS._W","CCP_USERS._P" TO &user_schema WITH ADMIN OPTION;
ALTER USER &user_schema DEFAULT ROLE ALL EXCEPT "CCP_USERS._W","CCP_USERS._P";

BEGIN
  begin
    for rec in
      (select user_group_id from table(&cwms_schema..cwms_sec.get_user_priv_groups_tab(
                                         p_username     => '&user_schema',
                                         p_db_office_id => '&cwms_office_id1'))
        where is_member in('T') and user_group_id not in('All Users')
       )
    loop
      &cwms_schema..cwms_sec.remove_user_from_group (
        p_username              => '&user_schema',
        p_user_group_id         => rec.user_group_id,
        p_db_office_id          => '&cwms_office_id1'
      );
    end loop;
    &cwms_schema..cwms_sec.create_user(
      p_username                => '&user_schema',
      p_password                => '&user_passwd',
      p_user_group_id_list      => CWMS_T_CHAR_32_ARRAY(
                                        'CCP Reviewer',
                                        'CWMS Users'),
      p_db_office_id            => '&cwms_office_id1'
    );

    for rec in
      (select user_group_id from table(&cwms_schema..cwms_sec.get_user_priv_groups_tab(
                                         p_username     => '&user_schema',
                                         p_db_office_id => '&cwms_office_id2'))
        where is_member in('T') and user_group_id not in('All Users')
       )
    loop
      &cwms_schema..cwms_sec.remove_user_from_group (
        p_username              => '&user_schema',
        p_user_group_id         => rec.user_group_id,
        p_db_office_id          => '&cwms_office_id2'
      );
    end loop;
    &cwms_schema..cwms_sec.create_user(
      p_username                => '&user_schema',
      p_password                => '&user_passwd',
      p_user_group_id_list      => CWMS_T_CHAR_32_ARRAY(
                                        'CCP Reviewer',
                                        'CWMS Users'),
      p_db_office_id            => '&cwms_office_id2'
    );
  exception
    when others then
      raise_application_error(-20999, sqlerrm, true);
  end;
END;
/

---------------------------------------------------------------------------
-- 5. The CCP user on multi-offices MVR/NAE with CCP Proc/Proc App role
---------------------------------------------------------------------------
define cwms_office_id1 = MVR;
define cwms_office_id2 = NAE;
define user_schema = CCPBOTH_P;
define user_passwd = CCPBOTH_P;

DROP USER &user_schema CASCADE;
CREATE USER &user_schema IDENTIFIED BY &user_passwd
DEFAULT TABLESPACE &ts_data_name QUOTA UNLIMITED ON &ts_data_name
TEMPORARY TABLESPACE &ts_temp_name
PROFILE DEFAULT
ACCOUNT UNLOCK;

GRANT CCP_USERS, CWMS_USER TO &user_schema;
GRANT "CCP_USERS._R","CCP_USERS._W","CCP_USERS._P" TO &user_schema WITH ADMIN OPTION;
ALTER USER &user_schema DEFAULT ROLE ALL EXCEPT "CCP_USERS._W";

BEGIN
  begin
    for rec in
      (select user_group_id from table(&cwms_schema..cwms_sec.get_user_priv_groups_tab(
                                         p_username     => '&user_schema',
                                         p_db_office_id => '&cwms_office_id1'))
        where is_member in('T') and user_group_id not in('All Users')
       )
    loop
      &cwms_schema..cwms_sec.remove_user_from_group (
        p_username              => '&user_schema',
        p_user_group_id         => rec.user_group_id,
        p_db_office_id          => '&cwms_office_id1'
      );
    end loop;
    &cwms_schema..cwms_sec.create_user(
      p_username                => '&user_schema',
      p_password                => '&user_passwd',
      p_user_group_id_list      => CWMS_T_CHAR_32_ARRAY(
                                        'CCP Proc',
                                        'CWMS PD Users',
                                        'TS ID Creator',
                                        'CWMS Users'),
      p_db_office_id            => '&cwms_office_id1'
    );

    for rec in
      (select user_group_id from table(&cwms_schema..cwms_sec.get_user_priv_groups_tab(
                                         p_username     => '&user_schema',
                                         p_db_office_id => '&cwms_office_id2'))
        where is_member in('T') and user_group_id not in('All Users')
       )
    loop
      &cwms_schema..cwms_sec.remove_user_from_group (
        p_username              => '&user_schema',
        p_user_group_id         => rec.user_group_id,
        p_db_office_id          => '&cwms_office_id2'
      );
    end loop;
    &cwms_schema..cwms_sec.create_user(
      p_username                => '&user_schema',
      p_password                => '&user_passwd',
      p_user_group_id_list      => CWMS_T_CHAR_32_ARRAY(
                                        'CCP Proc',
                                        'CWMS PD Users',
                                        'TS ID Creator',
                                        'CWMS Users'),
      p_db_office_id            => '&cwms_office_id2'
    );
  exception
    when others then
      raise_application_error(-20999, sqlerrm, true);
  end;
END;
/

---------------------------------------------------------------------------
-- 6. The CCP user on multi-offices MVR/NAE with CCP Mgr/Mgr App role
---------------------------------------------------------------------------
define cwms_office_id1 = MVR;
define cwms_office_id2 = NAE;
define user_schema = CCPBOTH_M;
define user_passwd = CCPBOTH_M;

DROP USER &user_schema CASCADE;
CREATE USER &user_schema IDENTIFIED BY &user_passwd
DEFAULT TABLESPACE &ts_data_name QUOTA UNLIMITED ON &ts_data_name
TEMPORARY TABLESPACE &ts_temp_name
PROFILE DEFAULT
ACCOUNT UNLOCK;

GRANT CCP_USERS, CWMS_USER TO &user_schema;
GRANT "CCP_USERS._R","CCP_USERS._W","CCP_USERS._P" TO &user_schema WITH ADMIN OPTION;
ALTER USER &user_schema DEFAULT ROLE ALL EXCEPT "CCP_USERS._P";

BEGIN
  begin
    for rec in
      (select user_group_id from table(&cwms_schema..cwms_sec.get_user_priv_groups_tab(
                                         p_username     => '&user_schema',
                                         p_db_office_id => '&cwms_office_id1'))
        where is_member in('T') and user_group_id not in('All Users')
       )
    loop
      &cwms_schema..cwms_sec.remove_user_from_group (
        p_username              => '&user_schema',
        p_user_group_id         => rec.user_group_id,
        p_db_office_id          => '&cwms_office_id1'
      );
    end loop;
    &cwms_schema..cwms_sec.create_user(
      p_username                => '&user_schema',
      p_password                => '&user_passwd',
      p_user_group_id_list      => CWMS_T_CHAR_32_ARRAY(
                                        'CCP Mgr',
                                        'CWMS PD Users',
                                        'TS ID Creator',
                                        'Data Acquisition Mgr',
                                        'CWMS Users'),
      p_db_office_id            => '&cwms_office_id1'
    );

    for rec in
      (select user_group_id from table(&cwms_schema..cwms_sec.get_user_priv_groups_tab(
                                         p_username     => '&user_schema',
                                         p_db_office_id => '&cwms_office_id2'))
        where is_member in('T') and user_group_id not in('All Users')
       )
    loop
      &cwms_schema..cwms_sec.remove_user_from_group (
        p_username              => '&user_schema',
        p_user_group_id         => rec.user_group_id,
        p_db_office_id          => '&cwms_office_id2'
      );
    end loop;
    &cwms_schema..cwms_sec.create_user(
      p_username                => '&user_schema',
      p_password                => '&user_passwd',
      p_user_group_id_list      => CWMS_T_CHAR_32_ARRAY(
                                        'CCP Mgr',
                                        'CWMS PD Users',
                                        'TS ID Creator',
                                        'Data Acquisition Mgr',
                                        'CWMS Users'),
      p_db_office_id            => '&cwms_office_id2'
    );
  exception
    when others then
      raise_application_error(-20999, sqlerrm, true);
  end;
END;
/

---------------------------------------------------------------------------
-- 7. The CCP user on multi-offices MVR/NAE with CCP Mgr/Reviewer App role
---------------------------------------------------------------------------
define cwms_office_id1 = MVR;
define cwms_office_id2 = NAE;
define user_schema = CCPBOTH_MR;
define user_passwd = CCPBOTH_MR;

DROP USER &user_schema CASCADE;
CREATE USER &user_schema IDENTIFIED BY &user_passwd
DEFAULT TABLESPACE &ts_data_name QUOTA UNLIMITED ON &ts_data_name
TEMPORARY TABLESPACE &ts_temp_name
PROFILE DEFAULT
ACCOUNT UNLOCK;

GRANT CCP_USERS, CWMS_USER TO &user_schema;
GRANT "CCP_USERS._R","CCP_USERS._W","CCP_USERS._P" TO &user_schema WITH ADMIN OPTION;
ALTER USER &user_schema DEFAULT ROLE ALL EXCEPT "CCP_USERS._W","CCP_USERS._P";

BEGIN
  begin
    for rec in
      (select user_group_id from table(&cwms_schema..cwms_sec.get_user_priv_groups_tab(
                                         p_username     => '&user_schema',
                                         p_db_office_id => '&cwms_office_id1'))
        where is_member in('T') and user_group_id not in('All Users')
       )
    loop
      &cwms_schema..cwms_sec.remove_user_from_group (
        p_username              => '&user_schema',
        p_user_group_id         => rec.user_group_id,
        p_db_office_id          => '&cwms_office_id1'
      );
    end loop;
    &cwms_schema..cwms_sec.create_user(
      p_username                => '&user_schema',
      p_password                => '&user_passwd',
      p_user_group_id_list      => CWMS_T_CHAR_32_ARRAY(
                                        'CCP Mgr',
                                        'CWMS PD Users',
                                        'TS ID Creator',
                                        'Data Acquisition Mgr',
                                        'CWMS Users'),
      p_db_office_id            => '&cwms_office_id1'
    );

    for rec in
      (select user_group_id from table(&cwms_schema..cwms_sec.get_user_priv_groups_tab(
                                         p_username     => '&user_schema',
                                         p_db_office_id => '&cwms_office_id2'))
        where is_member in('T') and user_group_id not in('All Users')
       )
    loop
      &cwms_schema..cwms_sec.remove_user_from_group (
        p_username              => '&user_schema',
        p_user_group_id         => rec.user_group_id,
        p_db_office_id          => '&cwms_office_id2'
      );
    end loop;
    &cwms_schema..cwms_sec.create_user(
      p_username                => '&user_schema',
      p_password                => '&user_passwd',
      p_user_group_id_list      => CWMS_T_CHAR_32_ARRAY(
                                        'CCP Reviewer',
                                        'CWMS Users'),
      p_db_office_id            => '&cwms_office_id2'
    );
  exception
    when others then
      raise_application_error(-20999, sqlerrm, true);
  end;
END;
/

---------------------------------------------------------------------------
-- 8. The CCP user on multi-offices MVR/NAE with no CCP App role
---------------------------------------------------------------------------
define cwms_office_id1 = MVR;
define cwms_office_id2 = NAE;
define user_schema = CCPBOTH_N;
define user_passwd = CCPBOTH_N;

DROP USER &user_schema CASCADE;
CREATE USER &user_schema IDENTIFIED BY &user_passwd
DEFAULT TABLESPACE &ts_data_name QUOTA UNLIMITED ON &ts_data_name
TEMPORARY TABLESPACE &ts_temp_name
PROFILE DEFAULT
ACCOUNT UNLOCK;

GRANT CCP_USERS, CWMS_USER TO &user_schema;
GRANT "CCP_USERS._R","CCP_USERS._W","CCP_USERS._P" TO &user_schema WITH ADMIN OPTION;
ALTER USER &user_schema DEFAULT ROLE ALL EXCEPT "CCP_USERS._R","CCP_USERS._W","CCP_USERS._P";

BEGIN
  begin
    for rec in
      (select user_group_id from table(&cwms_schema..cwms_sec.get_user_priv_groups_tab(
                                         p_username     => '&user_schema',
                                         p_db_office_id => '&cwms_office_id1'))
        where is_member in('T') and user_group_id not in('All Users')
       )
    loop
      &cwms_schema..cwms_sec.remove_user_from_group (
        p_username              => '&user_schema',
        p_user_group_id         => rec.user_group_id,
        p_db_office_id          => '&cwms_office_id1'
      );
    end loop;
    &cwms_schema..cwms_sec.create_user(
      p_username                => '&user_schema',
      p_password                => '&user_passwd',
      p_user_group_id_list      => CWMS_T_CHAR_32_ARRAY(
                                        'CWMS Users'),
      p_db_office_id            => '&cwms_office_id1'
    );

    for rec in
      (select user_group_id from table(&cwms_schema..cwms_sec.get_user_priv_groups_tab(
                                         p_username     => '&user_schema',
                                         p_db_office_id => '&cwms_office_id2'))
        where is_member in('T') and user_group_id not in('All Users')
       )
    loop
      &cwms_schema..cwms_sec.remove_user_from_group (
        p_username              => '&user_schema',
        p_user_group_id         => rec.user_group_id,
        p_db_office_id          => '&cwms_office_id2'
      );
    end loop;
    &cwms_schema..cwms_sec.create_user(
      p_username                => '&user_schema',
      p_password                => '&user_passwd',
      p_user_group_id_list      => CWMS_T_CHAR_32_ARRAY(
                                        'CWMS Users'),
      p_db_office_id            => '&cwms_office_id2'
    );
  exception
    when others then
      raise_application_error(-20999, sqlerrm, true);
  end;
END;
/

spool off
exit;
