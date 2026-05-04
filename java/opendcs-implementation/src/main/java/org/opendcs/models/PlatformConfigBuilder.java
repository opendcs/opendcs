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
import decodes.db.ScriptSensor;
import decodes.sql.DbKey;
import decodes.db.DecodesScript.DecodesScriptBuilder;
import decodes.db.DecodesScriptException;
import decodes.db.EquipmentModel;

public class PlatformConfigBuilder
{
    private PlatformConfig pc;
    private Map<Integer,ConfigSensor> sensors = new HashMap<>();
    private Map<DbKey, DecodesScriptBuilder> scriptBuilders = new HashMap<>();
    private EquipmentModel equipmentModel = null;
    private Map<DbKey, List<ScriptSensor>> scriptSensors = new HashMap<>();


    public PlatformConfigBuilder(PlatformConfig pc)
    {
        this.pc = pc.copy();
    }

    public PlatformConfigBuilder withSensor(ConfigSensor cs)
    {
        sensors.computeIfAbsent(cs.sensorNumber, num -> cs);
        return this;
    }

    public PlatformConfigBuilder withSensorProperty(int sensorNumber, String name, String value)
    {
        final var cs = sensors.get(sensorNumber);
        if (cs != null)
        {
            cs.setProperty(name, value);
        }
        return this;
    }

    public PlatformConfigBuilder withSensorDataType(int sensorNumber, DataType dataType)
    {
        final var cs = sensors.get(sensorNumber);
        if (cs != null)
        {
         cs.addDataType(dataType);
        }
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
        this.scriptBuilders.computeIfAbsent(scriptBuilder.getScriptId(), id -> scriptBuilder.platformConfig(this.pc));
        return this;
    }

    public PlatformConfigBuilder withScriptSensor(DbKey scriptId, ScriptSensor sensor)
    {
        var scriptSensorsList = scriptSensors.computeIfAbsent(scriptId, num -> new ArrayList<>());
        if (!scriptSensorsList.stream().anyMatch(ss -> ss.sensorNumber == sensor.sensorNumber))
        {
            scriptSensorsList.add(sensor);
        }
        return this;
    }

    public Optional<DecodesScriptBuilder> getDecodesScriptBuilder(DbKey id)
    {
        return Optional.ofNullable(scriptBuilders.get(id));
    }

    public PlatformConfig build() throws OpenDcsDataRuntimeException
    {
        for (var sensor: sensors.values())
        {
            pc.addSensor(sensor);
        }

        for (var scriptBuilder: scriptBuilders.values())
        {
            try
            {
                var script = scriptBuilder.build(true);
                var scriptSensorsList = scriptSensors.get(script.getId());
                for (var sensor: scriptSensorsList)
                {
                    sensor.decodesScript = script;
                    script.addScriptSensor(sensor);
                }

                pc.addScript(script);
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
