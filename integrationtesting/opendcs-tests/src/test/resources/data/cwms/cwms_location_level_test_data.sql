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
            P_LOCATION_ID  => 'FTPK-Lower',
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
            P_PUBLIC_NAME  => 'Fort Peck Lower',
            P_LONG_NAME    => 'Fort Peck Dam Lower Gauge',
            P_DESCRIPTION  => 'Test location for integration tests',
            P_ACTIVE       => 'T',
            P_DB_OFFICE_ID => v_office_id
        );
        DBMS_OUTPUT.PUT_LINE('Created FTPK-Lower location');
    EXCEPTION
        WHEN OTHERS THEN
            -- Location might already exist, continue
            DBMS_OUTPUT.PUT_LINE('Test location may already exist: ' || SQLERRM);
    END;
    
    -- Create YETL-Surface location
    BEGIN
        CWMS_20.CWMS_LOC.STORE_LOCATION(
            P_LOCATION_ID  => 'YETL-Surface',
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
            P_PUBLIC_NAME  => 'Yellowtail Surface',
            P_LONG_NAME    => 'Yellowtail Reservoir Surface Elevation',
            P_DESCRIPTION  => 'Yellowtail Reservoir surface elevation monitoring',
            P_ACTIVE       => 'T',
            P_DB_OFFICE_ID => v_office_id
        );
        DBMS_OUTPUT.PUT_LINE('Created YETL-Surface location');
    EXCEPTION
        WHEN OTHERS THEN
            DBMS_OUTPUT.PUT_LINE('YETL-Surface location may already exist: ' || SQLERRM);
    END;
    
    -- Store location levels using the simpler STORE_LOCATION_LEVEL procedure
    -- Create multiple values over time for range testing
    BEGIN
        -- Stage level for FTPK-Lower - multiple values over time
        -- Current value
        CWMS_20.CWMS_LEVEL.STORE_LOCATION_LEVEL(
            P_LOCATION_LEVEL_ID => 'FTPK-Lower.Stage.Const.0.Test',
            P_LEVEL_VALUE      => 123.45,
            P_LEVEL_UNITS      => 'ft',
            P_EFFECTIVE_DATE   => SYSDATE,
            P_TIMEZONE_ID      => 'UTC',
            P_OFFICE_ID        => v_office_id
        );
        DBMS_OUTPUT.PUT_LINE('Created FTPK-Lower.Stage.Const.0.Test level at current time');
        
        -- Value from 1 day ago
        CWMS_20.CWMS_LEVEL.STORE_LOCATION_LEVEL(
            P_LOCATION_LEVEL_ID => 'FTPK-Lower.Stage.Const.0.Test',
            P_LEVEL_VALUE      => 122.50,
            P_LEVEL_UNITS      => 'ft',
            P_EFFECTIVE_DATE   => SYSDATE - 1,
            P_TIMEZONE_ID      => 'UTC',
            P_OFFICE_ID        => v_office_id
        );
        DBMS_OUTPUT.PUT_LINE('Created FTPK-Lower.Stage.Const.0.Test level at -1 day');
        
        -- Value from 7 days ago
        CWMS_20.CWMS_LEVEL.STORE_LOCATION_LEVEL(
            P_LOCATION_LEVEL_ID => 'FTPK-Lower.Stage.Const.0.Test',
            P_LEVEL_VALUE      => 121.00,
            P_LEVEL_UNITS      => 'ft',
            P_EFFECTIVE_DATE   => SYSDATE - 7,
            P_TIMEZONE_ID      => 'UTC',
            P_OFFICE_ID        => v_office_id
        );
        DBMS_OUTPUT.PUT_LINE('Created FTPK-Lower.Stage.Const.0.Test level at -7 days');
        
        -- Value from 14 days ago
        CWMS_20.CWMS_LEVEL.STORE_LOCATION_LEVEL(
            P_LOCATION_LEVEL_ID => 'FTPK-Lower.Stage.Const.0.Test',
            P_LEVEL_VALUE      => 120.25,
            P_LEVEL_UNITS      => 'ft',
            P_EFFECTIVE_DATE   => SYSDATE - 14,
            P_TIMEZONE_ID      => 'UTC',
            P_OFFICE_ID        => v_office_id
        );
        DBMS_OUTPUT.PUT_LINE('Created FTPK-Lower.Stage.Const.0.Test level at -14 days');
        
        -- Value from 21 days ago
        CWMS_20.CWMS_LEVEL.STORE_LOCATION_LEVEL(
            P_LOCATION_LEVEL_ID => 'FTPK-Lower.Stage.Const.0.Test',
            P_LEVEL_VALUE      => 119.75,
            P_LEVEL_UNITS      => 'ft',
            P_EFFECTIVE_DATE   => SYSDATE - 21,
            P_TIMEZONE_ID      => 'UTC',
            P_OFFICE_ID        => v_office_id
        );
        DBMS_OUTPUT.PUT_LINE('Created FTPK-Lower.Stage.Const.0.Test level at -21 days');
        
        -- Value from 28 days ago
        CWMS_20.CWMS_LEVEL.STORE_LOCATION_LEVEL(
            P_LOCATION_LEVEL_ID => 'FTPK-Lower.Stage.Const.0.Test',
            P_LEVEL_VALUE      => 118.90,
            P_LEVEL_UNITS      => 'ft',
            P_EFFECTIVE_DATE   => SYSDATE - 28,
            P_TIMEZONE_ID      => 'UTC',
            P_OFFICE_ID        => v_office_id
        );
        DBMS_OUTPUT.PUT_LINE('Created FTPK-Lower.Stage.Const.0.Test level at -28 days');
        
    EXCEPTION
        WHEN OTHERS THEN
            DBMS_OUTPUT.PUT_LINE('Error creating stage levels: ' || SQLERRM);
    END;
    
    -- Flow level for FTPK-Lower - multiple values over time
    BEGIN
        -- Current flow value
        CWMS_20.CWMS_LEVEL.STORE_LOCATION_LEVEL(
            P_LOCATION_LEVEL_ID => 'FTPK-Lower.Flow.Const.0.Test',
            P_LEVEL_VALUE      => 500.0,
            P_LEVEL_UNITS      => 'cfs',
            P_EFFECTIVE_DATE   => SYSDATE,
            P_TIMEZONE_ID      => 'UTC',
            P_OFFICE_ID        => v_office_id
        );
        
        -- Flow from 1 day ago
        CWMS_20.CWMS_LEVEL.STORE_LOCATION_LEVEL(
            P_LOCATION_LEVEL_ID => 'FTPK-Lower.Flow.Const.0.Test',
            P_LEVEL_VALUE      => 475.5,
            P_LEVEL_UNITS      => 'cfs',
            P_EFFECTIVE_DATE   => SYSDATE - 1,
            P_TIMEZONE_ID      => 'UTC',
            P_OFFICE_ID        => v_office_id
        );
        
        -- Flow from 7 days ago
        CWMS_20.CWMS_LEVEL.STORE_LOCATION_LEVEL(
            P_LOCATION_LEVEL_ID => 'FTPK-Lower.Flow.Const.0.Test',
            P_LEVEL_VALUE      => 450.0,
            P_LEVEL_UNITS      => 'cfs',
            P_EFFECTIVE_DATE   => SYSDATE - 7,
            P_TIMEZONE_ID      => 'UTC',
            P_OFFICE_ID        => v_office_id
        );
        
        DBMS_OUTPUT.PUT_LINE('Created FTPK-Lower.Flow.Const.0.Test levels');
    EXCEPTION
        WHEN OTHERS THEN
            DBMS_OUTPUT.PUT_LINE('Error creating flow levels: ' || SQLERRM);
    END;
    
    -- Stage level for YETL-Surface - multiple values for cache testing
    BEGIN
        -- Current value
        CWMS_20.CWMS_LEVEL.STORE_LOCATION_LEVEL(
            P_LOCATION_LEVEL_ID => 'YETL-Surface.Stage.Const.0.Test',
            P_LEVEL_VALUE      => 100.50,
            P_LEVEL_UNITS      => 'ft',
            P_EFFECTIVE_DATE   => SYSDATE,
            P_TIMEZONE_ID      => 'UTC',
            P_OFFICE_ID        => v_office_id
        );
        
        -- Value from 1 day ago
        CWMS_20.CWMS_LEVEL.STORE_LOCATION_LEVEL(
            P_LOCATION_LEVEL_ID => 'YETL-Surface.Stage.Const.0.Test',
            P_LEVEL_VALUE      => 99.99,
            P_LEVEL_UNITS      => 'ft',
            P_EFFECTIVE_DATE   => SYSDATE - 1,
            P_TIMEZONE_ID      => 'UTC',
            P_OFFICE_ID        => v_office_id
        );
        
        -- Value from 3 days ago
        CWMS_20.CWMS_LEVEL.STORE_LOCATION_LEVEL(
            P_LOCATION_LEVEL_ID => 'YETL-Surface.Stage.Const.0.Test',
            P_LEVEL_VALUE      => 98.75,
            P_LEVEL_UNITS      => 'ft',
            P_EFFECTIVE_DATE   => SYSDATE - 3,
            P_TIMEZONE_ID      => 'UTC',
            P_OFFICE_ID        => v_office_id
        );
        
        -- Value from 7 days ago
        CWMS_20.CWMS_LEVEL.STORE_LOCATION_LEVEL(
            P_LOCATION_LEVEL_ID => 'YETL-Surface.Stage.Const.0.Test',
            P_LEVEL_VALUE      => 97.50,
            P_LEVEL_UNITS      => 'ft',
            P_EFFECTIVE_DATE   => SYSDATE - 7,
            P_TIMEZONE_ID      => 'UTC',
            P_OFFICE_ID        => v_office_id
        );
        
        DBMS_OUTPUT.PUT_LINE('Created YETL-Surface.Stage.Const.0.Test levels');
    EXCEPTION
        WHEN OTHERS THEN
            DBMS_OUTPUT.PUT_LINE('Error creating cache test levels: ' || SQLERRM);
    END;
    
    DBMS_OUTPUT.PUT_LINE('Location level test data setup complete');
END;
