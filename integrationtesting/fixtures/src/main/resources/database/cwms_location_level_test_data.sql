-- Test data for CwmsLocationLevelDAO integration tests
-- This script creates test location levels in the CWMS database
-- It's executed during test container initialization

DECLARE
    v_office_id VARCHAR2(16) := 'DEFAULT_OFFICE';
BEGIN
    -- First ensure the test location exists
    BEGIN
        CWMS_LOCATION.STORE_LOCATION(
            P_LOCATION_ID  => 'TEST_LOCATION',
            P_LOCATION_TYPE => 'STREAM_LOCATION',
            P_ELEVATION    => 100.0,
            P_ELEV_UNIT_ID => 'ft',
            P_VERTICAL_DATUM => 'NGVD29',
            P_LATITUDE     => 40.0,
            P_LONGITUDE    => -90.0,
            P_HORIZONTAL_DATUM => 'NAD83',
            P_PUBLIC_NAME  => 'Test Location for DAO Tests',
            P_LONG_NAME    => 'Test Location for LocationLevelDAO Integration Tests',
            P_DESCRIPTION  => 'Automated test location for LocationLevelDAO tests',
            P_TIME_ZONE_ID => 'UTC',
            P_COUNTY_NAME  => 'Test County',
            P_STATE_INITIAL => 'TS',
            P_ACTIVE       => 'T',
            P_OFFICE_ID    => v_office_id
        );
    EXCEPTION
        WHEN OTHERS THEN
            -- Location might already exist, continue
            DBMS_OUTPUT.PUT_LINE('Test location may already exist: ' || SQLERRM);
    END;
    
    -- Create Stage location level spec first
    BEGIN
        CWMS_LEVEL.STORE_LOCATION_LEVEL(
            P_LOCATION_LEVEL_ID => 'TEST_LOCATION.Stage.Constant.Test',
            P_LEVEL_VALUE      => 123.45,
            P_LEVEL_UNITS      => 'ft',
            P_LEVEL_DATE       => SYSDATE,
            P_LEVEL_COMMENT    => 'Test stage level for LocationLevelDAO integration tests',
            P_ATTRIBUTE_VALUE  => NULL,
            P_ATTRIBUTE_UNITS  => NULL,
            P_ATTRIBUTE_PARAMETER_ID => NULL,
            P_INTERPOLATE      => 'F',
            P_OFFICE_ID        => v_office_id
        );
        DBMS_OUTPUT.PUT_LINE('Created TEST_LOCATION stage level');
    EXCEPTION
        WHEN OTHERS THEN
            DBMS_OUTPUT.PUT_LINE('Error creating stage level: ' || SQLERRM);
    END;
    
    -- Create Flow location level spec
    BEGIN
        CWMS_LEVEL.STORE_LOCATION_LEVEL(
            P_LOCATION_LEVEL_ID => 'TEST_LOCATION.Flow.Constant.Test',
            P_LEVEL_VALUE      => 500.0,
            P_LEVEL_UNITS      => 'cfs',
            P_LEVEL_DATE       => SYSDATE,
            P_LEVEL_COMMENT    => 'Test flow level for LocationLevelDAO integration tests',
            P_ATTRIBUTE_VALUE  => NULL,
            P_ATTRIBUTE_UNITS  => NULL,
            P_ATTRIBUTE_PARAMETER_ID => NULL,
            P_INTERPOLATE      => 'F',
            P_OFFICE_ID        => v_office_id
        );
        DBMS_OUTPUT.PUT_LINE('Created TEST_LOCATION flow level');
    EXCEPTION
        WHEN OTHERS THEN
            DBMS_OUTPUT.PUT_LINE('Error creating flow level: ' || SQLERRM);
    END;
    
    -- Create a metric stage level for unit conversion testing
    BEGIN
        CWMS_LEVEL.STORE_LOCATION_LEVEL(
            P_LOCATION_LEVEL_ID => 'TEST_LOCATION.Stage-Metric.Constant.Test',
            P_LEVEL_VALUE      => 37.64,  -- ~123.45 ft in meters
            P_LEVEL_UNITS      => 'm',
            P_LEVEL_DATE       => SYSDATE,
            P_LEVEL_COMMENT    => 'Test stage level in metric units',
            P_ATTRIBUTE_VALUE  => NULL,
            P_ATTRIBUTE_UNITS  => NULL,
            P_ATTRIBUTE_PARAMETER_ID => NULL,
            P_INTERPOLATE      => 'F',
            P_OFFICE_ID        => v_office_id
        );
        DBMS_OUTPUT.PUT_LINE('Created TEST_LOCATION metric stage level');
    EXCEPTION
        WHEN OTHERS THEN
            DBMS_OUTPUT.PUT_LINE('Error creating metric stage level: ' || SQLERRM);
    END;
    
    -- Create cache test location first
    BEGIN
        CWMS_LOCATION.STORE_LOCATION(
            P_LOCATION_ID  => 'CACHE_TEST',
            P_LOCATION_TYPE => 'STREAM_LOCATION',
            P_ELEVATION    => 200.0,
            P_ELEV_UNIT_ID => 'ft',
            P_VERTICAL_DATUM => 'NGVD29',
            P_LATITUDE     => 41.0,
            P_LONGITUDE    => -91.0,
            P_HORIZONTAL_DATUM => 'NAD83',
            P_PUBLIC_NAME  => 'Cache Test Location',
            P_LONG_NAME    => 'Cache Test Location for LocationLevelDAO Tests',
            P_DESCRIPTION  => 'Automated test location for cache testing',
            P_TIME_ZONE_ID => 'UTC',
            P_COUNTY_NAME  => 'Test County',
            P_STATE_INITIAL => 'TS',
            P_ACTIVE       => 'T',
            P_OFFICE_ID    => v_office_id
        );
    EXCEPTION
        WHEN OTHERS THEN
            -- Location might already exist, continue
            DBMS_OUTPUT.PUT_LINE('Cache test location may already exist: ' || SQLERRM);
    END;
    
    -- Create cache test location level (for caching tests)
    BEGIN
        CWMS_LEVEL.STORE_LOCATION_LEVEL(
            P_LOCATION_LEVEL_ID => 'CACHE_TEST.Stage.Constant.Test',
            P_LEVEL_VALUE      => 99.99,
            P_LEVEL_UNITS      => 'ft',
            P_LEVEL_DATE       => SYSDATE - 1,  -- Yesterday's date
            P_LEVEL_COMMENT    => 'Test level for cache testing',
            P_ATTRIBUTE_VALUE  => NULL,
            P_ATTRIBUTE_UNITS  => NULL,
            P_ATTRIBUTE_PARAMETER_ID => NULL,
            P_INTERPOLATE      => 'F',
            P_OFFICE_ID        => v_office_id
        );
        DBMS_OUTPUT.PUT_LINE('Created CACHE_TEST level');
    EXCEPTION
        WHEN OTHERS THEN
            DBMS_OUTPUT.PUT_LINE('Error creating cache test level: ' || SQLERRM);
    END;
    
    COMMIT;
    DBMS_OUTPUT.PUT_LINE('Location level test data setup complete');
END;