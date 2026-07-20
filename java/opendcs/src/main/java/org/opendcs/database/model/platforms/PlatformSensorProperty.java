package org.opendcs.database.model.platforms;

import decodes.sql.DbKey;

public record PlatformSensorProperty(DbKey platformId, int sensorNumber, String propName, String propValue)
{
    
}
