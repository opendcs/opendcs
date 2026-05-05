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

/**
 * PlatformConfigs are rather complex, containg possible sensor information, data about equipment used (including at the sensor level),
 * as well as the decodes scripts and assosciated format statements and sensor unit converters.
 *
 * This class is used by  {@see DecodesConfigAccumulator} to store information retrieved from the returns result rows.
 *
 */
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

    /**
     * Add a sensor if one with the given sensorNumber doesn't already exist.
     * @param cs
     * @return
     */
    public PlatformConfigBuilder withSensor(ConfigSensor cs)
    {
        sensors.computeIfAbsent(cs.sensorNumber, num -> cs);
        return this;
    }

    /**
     * Add property value to sensor. If sensor doesn't exist this is a no-op.
     *
     * This behavior is to account for the possiblity of bad data in existing databases.
     * @param sensorNumber
     * @param name
     * @param value
     * @return
     */
    public PlatformConfigBuilder withSensorProperty(int sensorNumber, String name, String value)
    {
        final var cs = sensors.get(sensorNumber);
        if (cs != null)
        {
            cs.setProperty(name, value);
        }
        return this;
    }

    /**
     * Add a DataType to the given sensor numer.
     *
     * If the sensor doesn't exist, this is a no-op
     * @param sensorNumber
     * @param dataType
     * @return
     */
    public PlatformConfigBuilder withSensorDataType(int sensorNumber, DataType dataType)
    {
        final var cs = sensors.get(sensorNumber);
        if (cs != null)
        {
         cs.addDataType(dataType);
        }
        return this;
    }

    /**
     * Set EquipmentModel if not already set.
     *
     * If equipment model was already set, this is a no-op
     * @param equipmentModel
     * @return
     */
    public PlatformConfigBuilder withEquipmentModel(EquipmentModel equipmentModel)
    {
        if (this.equipmentModel != null)
        {
            this.equipmentModel = equipmentModel;
        }
        return this;
    }

    /***
     * Set a property on the equipment model. If the current equipment model is null,
     * this is a noop.
     *
     * As this class is intended for use by the Decodes Config Mappers and Accumulators
     * this should be an unlikely occurance. However, some existing databases by have stray
     * data that remains so we aren't considering this condition an error.
     *
     * @param name
     * @param value
     * @return
     */
    public PlatformConfigBuilder withEquipmentModelProperty(String name, String value)
    {
        if (equipmentModel != null)
        {
            this.equipmentModel.properties.put(name, value);
        }
        return this;
    }

    /**
     * Add, if it does not already exist, a new decodes script builder to this config builder.
     * @param scriptBuilder
     * @return
     */
    public PlatformConfigBuilder withDecodesScriptBuilder(DecodesScriptBuilder scriptBuilder)
    {
        this.scriptBuilders.computeIfAbsent(scriptBuilder.getScriptId(), id -> scriptBuilder.platformConfig(this.pc));
        return this;
    }

    /**
     * Add a new Decodes Script Sensor if it does not already exist.
     * @param scriptId
     * @param sensor
     * @return
     */
    public PlatformConfigBuilder withScriptSensor(DbKey scriptId, ScriptSensor sensor)
    {
        var scriptSensorsList = scriptSensors.computeIfAbsent(scriptId, num -> new ArrayList<>());
        if (scriptSensorsList.stream().noneMatch(ss -> ss.sensorNumber == sensor.sensorNumber))
        {
            scriptSensorsList.add(sensor);
        }
        return this;
    }

    /**
     * Retrieve the script builder for the given DecodesScript ID
     * @param id Surrogate key for the desired DecodesScript
     * @return
     */
    public Optional<DecodesScriptBuilder> getDecodesScriptBuilder(DbKey id)
    {
        return Optional.ofNullable(scriptBuilders.get(id));
    }

    /**
     * Finalize the PlatformConfig given all of the retrieved data.
     * @return
     * @throws OpenDcsDataRuntimeException If there are issues with the decodes script itself.
     */
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
                if (scriptSensorsList != null)
                {
                    for (var sensor: scriptSensorsList)
                    {
                        sensor.decodesScript = script;
                        script.addScriptSensor(sensor);
                    }
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
