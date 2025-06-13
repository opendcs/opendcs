create or replace procedure check_dynamic_sql(p_sql in varchar2)
is
    l_sql_no_quotes varchar2(32767);

    function remove_quotes(p_text in varchar2) return varchar2
    as
        l_test varchar2(32767);
        l_result varchar2(32767);
        l_pos    pls_integer;
    begin
        l_test := p_text;
        loop
        l_pos := regexp_instr(l_test, '[''"]');
        if l_pos > 0 then
            if substr(l_test, l_pos, 1) = '"' then
                ------------------------
                -- double-quote first --
                ------------------------
                l_result := regexp_replace(l_test, '"[^"]*?"', '#', 1, 1);
                l_result := regexp_replace(l_result, '''[^'']*?''', '$', 1, 1);
            else
                ------------------------
                -- single-quote first --
                ------------------------
                l_result := regexp_replace(l_test, '''[^'']*?''', '$', 1, 1);
                l_result := regexp_replace(l_result, '"[^"]*?"', '#', 1, 1);
            end if;
        else
    -----------------------
            -- no quotes in text --
    -----------------------
            l_result := l_test;
        end if;
        exit when l_result = l_test;
        l_test := l_result;
        end loop;
        return l_result;
    end;
begin
    l_sql_no_quotes := remove_quotes(p_sql);
    if regexp_instr(l_sql_no_quotes, '([''";]|--|/\*)') > 0 then
        raise_application_error(-20000,'UNSAFE DYNAMIC SQL : '||p_sql);
    end if;
end check_dynamic_sql;
/

create or replace procedure create_user(p_username varchar2, p_password varchar2) authid current_user
is
    l_sql varchar2(512);
    l_exists number;
begin
    select count(*) into l_exists from all_users where upper(username) = upper(p_username);
    if l_exists = 0 then
        l_sql := 'create user ' || p_username || ' identified by "' || p_password || '"';
    else
        l_sql := 'alter user ' || p_username || ' identified by "' || p_password || '"';
    end if;
    check_dynamic_sql(l_sql);
    execute immediate l_sql;

end;
/
