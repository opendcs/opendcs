package org.opendcs.models;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.opendcs.database.api.OpenDcsDataRuntimeException;

import decodes.db.ConfigSensor;
import decodes.db.DataType;
import decodes.db.PlatformConfig;
import decodes.sql.DbKey;
import decodes.db.DecodesScript.DecodesScriptBuilder;
import decodes.db.DecodesScriptException;
import decodes.db.EquipmentModel;

public class PlatformConfigBuilder
{
    private PlatformConfig pc;
    private List<ConfigSensor> sensors = new ArrayList<>();
    private Map<DbKey, DecodesScriptBuilder> scriptBuilders = new HashMap<>();
    private EquipmentModel equipmentModel = null;


    public PlatformConfigBuilder(PlatformConfig pc)
    {
        this.pc = pc.copy();
    }

    public PlatformConfigBuilder withSensor(ConfigSensor cs)
    {
        if (!sensors.contains(cs))
        {
            sensors.add(cs);
        }
        return this;
    }

    public PlatformConfigBuilder withSensorProperty(int sensorNumber, String name, String value)
    {
        var sensor = sensors.stream()
                            .filter(cs -> cs.sensorNumber == sensorNumber)
                            .findFirst();
        sensor.ifPresent(s -> s.setProperty(name, value));
        return this;
    }

    public PlatformConfigBuilder withSensorDataType(int sensorNumber, DataType dataType)
    {
        var sensor = sensors.stream()
                            .filter(cs -> cs.sensorNumber == sensorNumber)
                            .findFirst();
        sensor.ifPresent(s -> s.addDataType(dataType));
        return this;
    }

    public PlatformConfigBuilder withEquipmentModel(EquipmentModel equipmentModel)
    {
        if (this.equipmentModel != null)
        {
            this.equipmentModel = equipmentModel;
        }
        return this;
    }

    public PlatformConfigBuilder withEquipmentModelProperty(String name, String value)
    {
        if (equipmentModel != null)
        {
            this.equipmentModel.properties.put(name, value);
        }
        return this;
    }

    public PlatformConfigBuilder withDecodesScriptBuilder(DecodesScriptBuilder scriptBuilder)
    {
        this.scriptBuilders.computeIfAbsent(scriptBuilder.getScriptId(), id -> scriptBuilder);
        return this;
    }


    public Optional<DecodesScriptBuilder> getDecodesScriptBuilder(DbKey id)
    {
        return Optional.ofNullable(scriptBuilders.get(id));
    }


    public PlatformConfig build() throws OpenDcsDataRuntimeException
    {
        for (var sensor: sensors)
        {
            pc.addSensor(sensor);
        }

        for (var scriptBuilder: scriptBuilders.values())
        {
            try
            {
                pc.addScript(scriptBuilder.build(false));
            }
            catch (IOException | DecodesScriptException ex)
            {
                throw new OpenDcsDataRuntimeException("Unable to build decodes scripts", ex);
            }
        }

        pc.equipmentModel = equipmentModel;

        return pc;
    }
}
