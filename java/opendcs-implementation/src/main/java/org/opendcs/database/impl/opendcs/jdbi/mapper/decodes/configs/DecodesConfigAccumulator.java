package org.opendcs.database.impl.opendcs.jdbi.mapper.decodes.configs;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;

import org.jdbi.v3.core.mapper.ColumnMapper;
import org.jdbi.v3.core.result.ResultSetAccumulator;
import org.jdbi.v3.core.statement.StatementContext;
import org.opendcs.database.impl.opendcs.jdbi.decodesscript.InMemoryDecodesScriptReader;
import org.opendcs.database.impl.opendcs.jdbi.mapper.decodes.scripts.DecodesScriptBuilderMapper;
import org.opendcs.database.impl.opendcs.jdbi.mapper.decodes.scripts.FormatStatementMapper;
import org.opendcs.database.model.mappers.PrefixRowMapper;
import org.opendcs.database.model.mappers.datatype.DataTypeMapper;
import org.opendcs.database.model.mappers.equipmentmodel.EquipmentModelMapper;
import org.opendcs.database.model.mappers.properties.PropertiesMapper;
import org.opendcs.database.model.mappers.unitconverter.UnitConverterMapper;
import org.opendcs.models.PlatformConfigBuilder;
import org.opendcs.utils.sql.GenericColumns;
import org.opendcs.utils.sql.SqlErrorMessages;

import decodes.db.ScriptSensor;
import decodes.sql.DbKey;

public class DecodesConfigAccumulator implements ResultSetAccumulator<Map<Long, PlatformConfigBuilder>>
{
    private final String configPrefix;
    private final DecodesConfigMapper configMapper;
    private final EquipmentModelMapper equipmentModelMapper;
    private final PropertiesMapper equipmentPropertiesMapper;
    private final ConfigSensorMapper sensorMapper;
    private final PropertiesMapper sensorPropertiesMapper;
    private final DataTypeMapper dataTypeMapper;
    private final DecodesScriptBuilderMapper scriptBuilderMapper;
    private final FormatStatementMapper formatStatementMapper;
    private final UnitConverterMapper unitConverterMapper;


    public DecodesConfigAccumulator(String configPrefix, DecodesConfigMapper configMapper, 
                                EquipmentModelMapper equipmentModelMapper, PropertiesMapper equipmentPropertiesMapper,
                                ConfigSensorMapper sensorMapper, PropertiesMapper sensorPropertiesMapper,
                                DataTypeMapper dataTypeMapper, DecodesScriptBuilderMapper scriptBuilderMapper,
                                FormatStatementMapper formatStatementMapper, UnitConverterMapper unitConverterMapper)
    {
        this.configPrefix = PrefixRowMapper.addUnderscoreIfMissing(configPrefix);
        this.configMapper = configMapper;
        this.equipmentModelMapper = equipmentModelMapper;
        this.equipmentPropertiesMapper = equipmentPropertiesMapper;
        this.sensorMapper = sensorMapper;
        this.sensorPropertiesMapper = sensorPropertiesMapper;        
        this.dataTypeMapper = dataTypeMapper;
        this.scriptBuilderMapper = scriptBuilderMapper;
        this.formatStatementMapper = formatStatementMapper;
        this.unitConverterMapper = unitConverterMapper;
    }

    
    @Override
    public Map<Long, PlatformConfigBuilder> apply(Map<Long, PlatformConfigBuilder> previous, ResultSet rs, StatementContext ctx)
            throws SQLException
    {
        ColumnMapper<DbKey> columnMapperForKey = ctx.findColumnMapperFor(DbKey.class)
                                                    .orElseThrow(() -> new SQLException(SqlErrorMessages.DBKEY_MAPPER_NOT_FOUND));
        PlatformConfigBuilder pc = null;
        try
        {
            pc = previous.computeIfAbsent(rs.getLong(configPrefix+GenericColumns.ID),
                pcId ->
                { 
                    try
                    {
                        return new PlatformConfigBuilder(configMapper.map(rs, ctx));
                    }
                catch (SQLException ex)
                {
                    // We have to use RuntimeException to escape the computeIfAbsent when there
                    // is an error.
                    throw new RuntimeException(ex); // NOSONAR
                }
            });
        }
        catch (RuntimeException ex)
        {
            if (ex.getCause() instanceof SQLException sqlException)
            {
                throw sqlException;
            }
            throw ex;
        }
        var ignored = rs.getLong(configPrefix+"equipmentid");
        if (!rs.wasNull())
        {
            pc.withEquipmentModel(equipmentModelMapper.map(rs, ctx));
        }

        
        var emProps = equipmentPropertiesMapper.map(rs, ctx);
        if (emProps.first != null)
        {
            pc.withEquipmentModelProperty(emProps.first, emProps.second);
        }

        var configSensor = sensorMapper.map(rs, ctx);
        if (configSensor != null)
        {
            pc.withSensor(configSensor);
        }
        final var datatype = dataTypeMapper.map(rs, ctx);
        if (datatype != null)
        {
            pc.withSensorDataType(configSensor.sensorNumber, datatype);
        }

        var sensorProperties = sensorPropertiesMapper.map(rs, ctx);
        if (sensorProperties.first != null)
        {
            pc.withSensorProperty(configSensor.sensorNumber, sensorProperties.first, sensorProperties.second);
        }

        var scriptId = columnMapperForKey.map(rs, "ds_" + GenericColumns.ID , ctx);
        var scriptBuilder = pc.getDecodesScriptBuilder(scriptId).orElse(null);
        if (scriptBuilder == null && !(scriptId == null || DbKey.isNull(scriptId)))
        {
            scriptBuilder = scriptBuilderMapper.map(rs, ctx);
            pc.withDecodesScriptBuilder(scriptBuilder);
        }
        
        var formatStatement = formatStatementMapper.map(rs, ctx);
        if (formatStatement != null)
        {
            // we set this so we know it's the In Memory reader
            ((InMemoryDecodesScriptReader)scriptBuilder.getReader()).addStatement(formatStatement);
        }

        var sensorScriptId = columnMapperForKey.map(rs, "ss_decodesscriptid", ctx);
        var sensorScriptNum = rs.getInt("ss_sensornumber");

        if (!DbKey.isNull(sensorScriptId))
        {
            var scriptSensor = new ScriptSensor(null, sensorScriptNum);
            var uc = unitConverterMapper.map(rs, ctx);
            scriptSensor.rawConverter = uc;
            pc.withScriptSensor(sensorScriptId, scriptSensor);
        }

        return previous;
    }
    
}
