package org.opendcs.database.impl.opendcs.jdbi.mapper.decodes.platforms;

import java.sql.SQLException;
import java.util.Map;
import java.util.function.BiConsumer;

import org.jdbi.v3.core.result.RowView;
import org.jdbi.v3.core.statement.UnableToExecuteStatementException;
import org.opendcs.database.model.platforms.PlatformSensorProperty;

import decodes.db.Platform;
import decodes.db.PlatformSensor;

public class PlatformSensorReducer implements BiConsumer<Map<Long,Platform>, RowView>
{

    final PlatformSensorMapper psMapper;

    public PlatformSensorReducer(PlatformSensorMapper psMapper)
    {
        this.psMapper = psMapper;
    }

    @Override
    public void accept(Map<Long, Platform> map, RowView view)
    {
        try
        {
            var id = view.getColumn(psMapper.column(PlatformSensorMapper.Columns.PLATFORM_ID), Long.class);
            if (id == null)
            {
                return;
            }
            
            var platform = map.get(id);
            platform.addPlatformSensor(view.getRow(PlatformSensor.class));
            var sensorProp = view.getRow(PlatformSensorProperty.class);
            if (sensorProp != null)
            {
                var sensor = platform.getPlatformSensor(sensorProp.sensorNumber());
                sensor.setProperty(sensorProp.propName(), sensorProp.propValue());
            }
        }
        catch (SQLException ex)
        {
            throw new UnableToExecuteStatementException("Unable to process result row.", ex, null);
        }
    }
}
