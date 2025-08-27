-- Test data for CwmsLocationLevelDAO integration tests
-- This script creates test location levels in the CWMS database

DECLARE
    v_office_id VARCHAR2(16) := 'SPK';
    v_office_code NUMBER;
BEGIN
    -- Get the office code
    BEGIN
        SELECT office_code INTO v_office_code 
        FROM CWMS_20.AV_OFFICE 
        WHERE office_id = v_office_id;
    EXCEPTION
        WHEN NO_DATA_FOUND THEN
            -- Default to SPK office code if not found
            v_office_code := 44;
    END;

    -- First ensure the test location exists
    BEGIN
        CWMS_20.CWMS_LOC.STORE_LOCATION(
            P_LOCATION_ID  => 'TEST_LOCATION',
            P_STATE_INITIAL => 'CA',
            P_COUNTY_NAME  => 'Test County',
            P_TIME_ZONE_ID => 'US/Pacific',
            P_LOCATION_TYPE => 'STREAM',
            P_LATITUDE     => 40.0,
            P_LONGITUDE    => -120.0,
            P_ELEVATION    => 100.0,
            P_ELEV_UNIT_ID => 'ft',
            P_VERTICAL_DATUM => 'NGVD29',
            P_HORIZONTAL_DATUM => 'NAD83',
            P_PUBLIC_NAME  => 'Test Location',
            P_LONG_NAME    => 'Test Location for LocationLevelDAO',
            P_DESCRIPTION  => 'Test location for integration tests',
            P_ACTIVE       => 'T',
            P_DB_OFFICE_ID => v_office_id
        );
        DBMS_OUTPUT.PUT_LINE('Created TEST_LOCATION');
    EXCEPTION
        WHEN OTHERS THEN
            -- Location might already exist, continue
            DBMS_OUTPUT.PUT_LINE('Test location may already exist: ' || SQLERRM);
    END;
    
    -- Create CACHE_TEST location
    BEGIN
        CWMS_20.CWMS_LOC.STORE_LOCATION(
            P_LOCATION_ID  => 'CACHE_TEST',
            P_STATE_INITIAL => 'CA',
            P_COUNTY_NAME  => 'Test County',
            P_TIME_ZONE_ID => 'US/Pacific',
            P_LOCATION_TYPE => 'STREAM',
            P_LATITUDE     => 41.0,
            P_LONGITUDE    => -121.0,
            P_ELEVATION    => 200.0,
            P_ELEV_UNIT_ID => 'ft',
            P_VERTICAL_DATUM => 'NGVD29',
            P_HORIZONTAL_DATUM => 'NAD83',
            P_PUBLIC_NAME  => 'Cache Test Location',
            P_LONG_NAME    => 'Cache Test Location',
            P_DESCRIPTION  => 'Test location for cache tests',
            P_ACTIVE       => 'T',
            P_DB_OFFICE_ID => v_office_id
        );
        DBMS_OUTPUT.PUT_LINE('Created CACHE_TEST location');
    EXCEPTION
        WHEN OTHERS THEN
            DBMS_OUTPUT.PUT_LINE('Cache test location may already exist: ' || SQLERRM);
    END;
    
    -- Store location levels using the simpler STORE_LOCATION_LEVEL procedure
    BEGIN
        -- Stage level for TEST_LOCATION
        CWMS_20.CWMS_LEVEL.STORE_LOCATION_LEVEL(
            P_LOCATION_LEVEL_ID => 'TEST_LOCATION.Stage.Const.0.Test',
            P_LEVEL_VALUE      => 123.45,
            P_LEVEL_UNITS      => 'ft',
            P_EFFECTIVE_DATE   => SYSDATE,
            P_TIMEZONE_ID      => 'UTC',
            P_OFFICE_ID        => v_office_id
        );
        DBMS_OUTPUT.PUT_LINE('Created TEST_LOCATION.Stage.Const.0.Test level');
    EXCEPTION
        WHEN OTHERS THEN
            DBMS_OUTPUT.PUT_LINE('Error creating stage level: ' || SQLERRM);
    END;
    
    -- Flow level for TEST_LOCATION
    BEGIN
        CWMS_20.CWMS_LEVEL.STORE_LOCATION_LEVEL(
            P_LOCATION_LEVEL_ID => 'TEST_LOCATION.Flow.Const.0.Test',
            P_LEVEL_VALUE      => 500.0,
            P_LEVEL_UNITS      => 'cfs',
            P_EFFECTIVE_DATE   => SYSDATE,
            P_TIMEZONE_ID      => 'UTC',
            P_OFFICE_ID        => v_office_id
        );
        DBMS_OUTPUT.PUT_LINE('Created TEST_LOCATION.Flow.Const.0.Test level');
    EXCEPTION
        WHEN OTHERS THEN
            DBMS_OUTPUT.PUT_LINE('Error creating flow level: ' || SQLERRM);
    END;
    
    -- Stage level for CACHE_TEST
    BEGIN
        CWMS_20.CWMS_LEVEL.STORE_LOCATION_LEVEL(
            P_LOCATION_LEVEL_ID => 'CACHE_TEST.Stage.Const.0.Test',
            P_LEVEL_VALUE      => 99.99,
            P_LEVEL_UNITS      => 'ft',
            P_EFFECTIVE_DATE   => SYSDATE - 1,
            P_TIMEZONE_ID      => 'UTC',
            P_OFFICE_ID        => v_office_id
        );
        DBMS_OUTPUT.PUT_LINE('Created CACHE_TEST.Stage.Const.0.Test level');
    EXCEPTION
        WHEN OTHERS THEN
            DBMS_OUTPUT.PUT_LINE('Error creating cache test level: ' || SQLERRM);
    END;
    
    DBMS_OUTPUT.PUT_LINE('Location level test data setup complete');
END;
