create or replace view time_series_identifier as
    select
        spec.ts_id id,
        sn.sitename site_name,
        dt.standard data_type_standard,
        dt.code data_type_code,
        ic.name interval,
        dc.name duration,
        spec.ts_version,
        sn.sitename || '.' || dt.code || '.' || spec.statistics_code || '.' || ic.name || '.' || dc.name || '.' || spec.ts_version as unique_string,
        spec.active_flag,
        spec.storage_units,
        spec.storage_table,
        spec.storage_type,
        spec.modify_time,        
        spec.description,
        spec.utc_offset,
        spec.allow_dst_offset_variation,
        spec.offset_error_action,
        
        spec.datatype_id,
        spec.site_id,
        spec.interval_id,
        spec.duration_id

    from ts_spec spec
    join sitename sn on spec.site_id = sn.siteid and nametype = 'CWMS' -- a CWMS SiteName is currently required
    join datatype dt on spec.datatype_id = dt.id
    join interval_code ic on ic.interval_id = spec.interval_id
    join interval_code dc on dc.interval_id = spec.duration_id


;

grant select on time_series_identifier to "OTSDB_USER";