-- Test data setup for CwmsLocationLevelDAO integration tests
-- This script creates test location level data in CWMS database
-- Note: This assumes CWMS schema and procedures are already available

-- Create test location if it doesn't exist
-- Note: In a real CWMS database, locations must be created through CWMS procedures
-- This is a simplified example - adjust based on your CWMS setup

-- Insert test location level specifications
-- These would normally be created through CWMS_LEVEL package procedures
-- Example (adjust syntax for your CWMS version):

/*
BEGIN
    -- Create a test location if needed
    CWMS_LOCATION.STORE_LOCATION(
        P_LOCATION_ID  => 'TEST_LOCATION',
        P_LOCATION_TYPE => 'STREAM',
        P_ELEVATION    => 100.0,
        P_ELEV_UNIT_ID => 'ft',
        P_VERTICAL_DATUM => 'NGVD29',
        P_LATITUDE     => 40.0,
        P_LONGITUDE    => -90.0,
        P_HORIZONTAL_DATUM => 'NAD83',
        P_PUBLIC_NAME  => 'Test Location for DAO Tests',
        P_LONG_NAME    => 'Test Location for LocationLevelDAO Integration Tests',
        P_DESCRIPTION  => 'Test location for automated integration tests',
        P_TIME_ZONE_ID => 'UTC',
        P_COUNTY_NAME  => 'Test County',
        P_STATE_INITIAL => 'TS',
        P_ACTIVE       => 'T',
        P_OFFICE_ID    => CWMS_UTIL.GET_DB_OFFICE_ID
    );
EXCEPTION
    WHEN OTHERS THEN
        -- Location might already exist, that's OK
        NULL;
END;
/

BEGIN
    -- Create test location level specifications
    CWMS_LEVEL.STORE_LOCATION_LEVEL(
        P_LOCATION_LEVEL_ID => 'TEST_LOCATION.Stage.Inst.0.0.USGS-NWIS',
        P_LEVEL_VALUE      => 123.45,
        P_LEVEL_UNITS      => 'ft',
        P_LEVEL_DATE       => SYSDATE,
        P_LEVEL_COMMENT    => 'Test stage level for integration tests',
        P_ATTRIBUTE_VALUE  => NULL,
        P_ATTRIBUTE_UNITS  => NULL,
        P_ATTRIBUTE_ID     => NULL,
        P_OFFICE_ID        => CWMS_UTIL.GET_DB_OFFICE_ID
    );
    
    CWMS_LEVEL.STORE_LOCATION_LEVEL(
        P_LOCATION_LEVEL_ID => 'TEST_LOCATION.Flow.Inst.0.0.USGS-NWIS',
        P_LEVEL_VALUE      => 500.0,
        P_LEVEL_UNITS      => 'cfs',
        P_LEVEL_DATE       => SYSDATE,
        P_LEVEL_COMMENT    => 'Test flow level for integration tests',
        P_ATTRIBUTE_VALUE  => NULL,
        P_ATTRIBUTE_UNITS  => NULL,
        P_ATTRIBUTE_ID     => NULL,
        P_OFFICE_ID        => CWMS_UTIL.GET_DB_OFFICE_ID
    );
    
    -- Create a location level with metric units for conversion testing
    CWMS_LEVEL.STORE_LOCATION_LEVEL(
        P_LOCATION_LEVEL_ID => 'TEST_LOCATION.Stage.Inst.0.0.METRIC',
        P_LEVEL_VALUE      => 37.64,  -- ~123.45 ft in meters
        P_LEVEL_UNITS      => 'm',
        P_LEVEL_DATE       => SYSDATE,
        P_LEVEL_COMMENT    => 'Test stage level in metric units',
        P_ATTRIBUTE_VALUE  => NULL,
        P_ATTRIBUTE_UNITS  => NULL,
        P_ATTRIBUTE_ID     => NULL,
        P_OFFICE_ID        => CWMS_UTIL.GET_DB_OFFICE_ID
    );
    
    COMMIT;
END;
/
*/

-- Alternative: Direct inserts into tables (if procedures are not available)
-- Note: This is NOT recommended for production CWMS databases
-- Only use for isolated test environments

-- For testing without actual CWMS procedures, you can mock the data:
-- This assumes you have a test schema that mimics CWMS structure

/*
-- Insert into location level view/table (adjust based on your test setup)
INSERT INTO AV_LOCATION_LEVEL (
    LOCATION_LEVEL_ID,
    SPECIFIED_LEVEL_ID,
    PARAMETER_ID,
    PARAMETER_TYPE_ID,
    DURATION_ID,
    LOCATION_ID,
    CONSTANT_LEVEL,
    LEVEL_UNIT,
    LEVEL_DATE,
    LEVEL_COMMENT,
    UNIT_SYSTEM
) VALUES (
    'TEST_LOCATION.Stage.Inst.0.0.USGS-NWIS',
    'Normal',
    'Stage',
    'Inst',
    '0',
    'TEST_LOCATION',
    123.45,
    'ft',
    SYSDATE,
    'Test stage level for integration tests',
    'EN'
);

INSERT INTO AV_LOCATION_LEVEL (
    LOCATION_LEVEL_ID,
    SPECIFIED_LEVEL_ID,
    PARAMETER_ID,
    PARAMETER_TYPE_ID,
    DURATION_ID,
    LOCATION_ID,
    CONSTANT_LEVEL,
    LEVEL_UNIT,
    LEVEL_DATE,
    LEVEL_COMMENT,
    UNIT_SYSTEM
) VALUES (
    'TEST_LOCATION.Flow.Inst.0.0.USGS-NWIS',
    'Normal',
    'Flow',
    'Inst',
    '0',
    'TEST_LOCATION',
    500.0,
    'cfs',
    SYSDATE,
    'Test flow level for integration tests',
    'EN'
);

COMMIT;
*/