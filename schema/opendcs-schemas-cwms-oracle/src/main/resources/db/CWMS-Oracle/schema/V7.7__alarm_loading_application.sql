-- Alarm tables and sequences were missing elements for CWMS.
-- See: https://github.com/opendcs/opendcs/issues/485
--
-- For database version >= 68:
--   - AlarmDAO reads and writes LOADING_APPLICATION_ID on ALARM_SCREENING, ALARM_CURRENT, and ALARM_HISTORY.
--   - CwmsTimeSeriesDb uses OracleSequenceKeyGenerator, which expects a <table>IdSeq sequence for each table.
-- This brings the CWMS-Oracle alarm tables in line with the OpenDCS-Oracle schema.
--
-- Columns and sequences are only created if missing, in case a site already added them by hand.

declare
    procedure add_loading_app_column(p_table in varchar2) is
        v_cnt number;
    begin
        select count(*) into v_cnt
          from all_tab_columns
         where owner = upper('${CCP_SCHEMA}')
           and table_name = upper(p_table)
           and column_name = 'LOADING_APPLICATION_ID';

        if v_cnt = 0 then
            execute immediate 'alter table ${CCP_SCHEMA}.' || p_table
                || ' add (loading_application_id number(18))';
        end if;
    end;

    -- Like the other CCP sequences, keys step by 1000 with the office code in the last three digits.
    -- Start after any keys already in the table.
    procedure create_id_sequence(p_table in varchar2, p_id_column in varchar2) is
        v_cnt   number;
        v_max   number;
        v_start number;
    begin
        select count(*) into v_cnt
          from all_sequences
         where sequence_owner = upper('${CCP_SCHEMA}')
           and sequence_name = upper(p_table) || 'IDSEQ';

        if v_cnt = 0 then
            execute immediate 'select nvl(max(' || p_id_column || '), 0) from ${CCP_SCHEMA}.' || p_table
                into v_max;

            if v_max = 0 then
                v_start := ${DEFAULT_OFFICE_CODE};
            else
                v_start := (floor(v_max / 1000) + 1) * 1000 + ${DEFAULT_OFFICE_CODE};
            end if;

            execute immediate 'create sequence ${CCP_SCHEMA}.' || p_table || 'idseq'
                || ' increment by 1000 start with ' || v_start || ' nocache';
        end if;
    end;
begin
    add_loading_app_column('ALARM_SCREENING');
    add_loading_app_column('ALARM_CURRENT');
    add_loading_app_column('ALARM_HISTORY');

    create_id_sequence('ALARM_GROUP', 'ALARM_GROUP_ID');
    create_id_sequence('ALARM_EVENT', 'ALARM_EVENT_ID');
    create_id_sequence('ALARM_SCREENING', 'SCREENING_ID');
    create_id_sequence('ALARM_LIMIT_SET', 'LIMIT_SET_ID');
end;
/

-- A site/datatype may have different screenings for different loading applications.
alter table ${CCP_SCHEMA}.alarm_screening drop constraint as_sdi_start_unique;
alter table ${CCP_SCHEMA}.alarm_screening add constraint as_sdi_start_unique
    unique (db_office_code, site_id, datatype_id, start_date_time, loading_application_id);
alter table ${CCP_SCHEMA}.alarm_screening add constraint as_app_fk
    foreign key (loading_application_id)
    references ${CCP_SCHEMA}.hdb_loading_application (loading_application_id);

-- ALARM_CURRENT was unique on TS_ID alone, allowing only one loading application's alarm per time series.
-- UNIQUE rather than PRIMARY KEY (as in OpenDCS-Oracle) because existing rows have no loading application.
alter table ${CCP_SCHEMA}.alarm_current drop unique (ts_id);
alter table ${CCP_SCHEMA}.alarm_current add constraint alarm_current_ts_app_unique
    unique (ts_id, loading_application_id);
alter table ${CCP_SCHEMA}.alarm_current add constraint alarm_current_fkappid
    foreign key (loading_application_id)
    references ${CCP_SCHEMA}.hdb_loading_application (loading_application_id);

alter table ${CCP_SCHEMA}.alarm_history drop primary key;
alter table ${CCP_SCHEMA}.alarm_history add constraint alarm_history_ts_app_unique
    unique (ts_id, limit_set_id, assert_time, loading_application_id);
alter table ${CCP_SCHEMA}.alarm_history add constraint alarm_history_fkappid
    foreign key (loading_application_id)
    references ${CCP_SCHEMA}.hdb_loading_application (loading_application_id);
