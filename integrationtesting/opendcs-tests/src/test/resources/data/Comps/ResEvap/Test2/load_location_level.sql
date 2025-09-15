-- SQL script to load location level data for ResEvap Test2
-- This creates a location level TESTSITE1.Depth.Const.0.Secchi Depth with value 13 ft

DECLARE
    l_office_id VARCHAR2(16) := 'SPK';
    l_location_level_id VARCHAR2(256) := 'TESTSITE1.Depth.Const.0.Secchi Depth';
    l_level_value NUMBER := 13;
    l_level_units VARCHAR2(16) := 'ft';
    l_level_date DATE := SYSDATE - 1;
BEGIN

    CWMS_20.CWMS_LEVEL.STORE_LOCATION_LEVEL(
            P_LOCATION_LEVEL_ID => l_location_level_id,
            P_LEVEL_VALUE      => l_level_value,
            P_LEVEL_UNITS      => l_level_units,
            P_EFFECTIVE_DATE   => l_level_date,
            P_TIMEZONE_ID      => 'UTC',
            P_OFFICE_ID        => l_office_id
        );
    
    DBMS_OUTPUT.PUT_LINE('Location level ' || l_location_level_id || ' created with value ' || l_level_value || ' ' || l_level_units);
END;