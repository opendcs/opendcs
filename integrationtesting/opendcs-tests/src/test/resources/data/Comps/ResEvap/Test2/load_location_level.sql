-- SQL script to load location level data for ResEvap Test2
-- This creates a location level FTPK.Depth.Const.0.Secchi Depth with value 13 ft

DECLARE
    l_office_id VARCHAR2(16) := 'DEFAULT_OFFICE';
    l_location_id VARCHAR2(57) := 'FTPK';
    l_location_level_id VARCHAR2(256) := 'FTPK.Depth.Const.0.Secchi Depth';
    l_parameter_id VARCHAR2(49) := 'Depth';
    l_parameter_type_id VARCHAR2(16) := 'Const';
    l_duration_id VARCHAR2(16) := '0';
    l_specified_level_id VARCHAR2(256) := 'Secchi Depth';
    l_level_value NUMBER := 13;
    l_level_units VARCHAR2(16) := 'ft';
    l_level_date DATE := SYSDATE;
BEGIN
    -- Store the location level specification
    cwms_level.store_location_level3(
        p_location_level_id => l_location_level_id,
        p_specified_level_id => l_specified_level_id,
        p_parameter_id => l_parameter_id,
        p_parameter_type_id => l_parameter_type_id,
        p_duration_id => l_duration_id,
        p_level_value => l_level_value,
        p_level_units => l_level_units,
        p_level_date => l_level_date,
        p_level_comment => 'Test location level for ResEvap Test2',
        p_office_id => l_office_id,
        p_fail_if_exists => 'F'
    );
    
    COMMIT;
    
    DBMS_OUTPUT.PUT_LINE('Location level ' || l_location_level_id || ' created with value ' || l_level_value || ' ' || l_level_units);
END;