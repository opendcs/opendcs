package org.opendcs.database.impl.opendcs.dao;

import org.jdbi.v3.core.Handle;
import org.opendcs.annotations.api.InjectDao;
import org.opendcs.database.api.DataTransaction;
import org.opendcs.database.api.OpenDcsDataException;
import org.opendcs.database.dai.DecodesConfigDao;
import org.opendcs.database.dai.UnitConverterDao;
import org.opendcs.database.model.mappers.datatype.DataTypeMapper;
import org.opendcs.database.model.mappers.equipmentmodel.EquipmentModelMapper;
import org.opendcs.database.model.mappers.properties.PropertiesMapper;
import org.opendcs.database.model.mappers.unitconverter.UnitConverterMapper;
import org.opendcs.database.impl.opendcs.jdbi.mapper.decodes.configs.DecodesConfigMapper;
import org.opendcs.database.impl.opendcs.jdbi.mapper.decodes.scripts.DecodesScriptBuilderMapper;
import org.opendcs.database.impl.opendcs.jdbi.mapper.decodes.scripts.FormatStatementMapper;
import org.opendcs.database.impl.opendcs.jdbi.column.numeric.NullableDoubleArgumentFactory;
import org.opendcs.database.impl.opendcs.jdbi.mapper.decodes.configs.ConfigSensorMapper;
import org.opendcs.database.impl.opendcs.jdbi.mapper.decodes.configs.DecodesConfigAccumulator;
import org.opendcs.utils.logging.OpenDcsLoggerFactory;
import org.opendcs.utils.sql.GenericColumns;
import org.opendcs.utils.sql.SqlErrorMessages;
import org.opendcs.utils.sql.SqlKeywords;
import org.opendcs.utils.sql.SqlQueries;
import org.openide.util.lookup.ServiceProvider;
import org.slf4j.Logger;

import decodes.db.DatabaseException;
import decodes.db.PlatformConfig;
import decodes.sql.DbKey;
import decodes.sql.KeyGenerator;

import static org.opendcs.utils.sql.SqlQueries.addLimitOffset;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;

@ServiceProvider(service = DecodesConfigDao.class)
public class DecodesConfigDaoImpl implements DecodesConfigDao
{
    private static final String SENSORNUMBER = "sensornumber";

    private static final String DECODESSCRIPTID = "decodesscriptid";

    private static final String CONFIGID = "configid";

    private static final Logger log = OpenDcsLoggerFactory.getLogger();

    private static final String SELECT_QUERY = """
            with pc (id, name, description, equipmentId) as (
                select id, name, description, equipmentId
                  from PlatformConfig
                <where>
                order by name <collate> ASC
                <limit>
            )
            select pc.id pc_id, pc.name pc_name, pc.description pc_description, pc.equipmentId pc_equipmentId,
                    em.id e_id, em.name e_name, em.company e_company, em.model e_model, em.description e_description,
                    em.equipmentType e_equipmentType, ep.name ep_name, ep.prop_value ep_prop_value,

                    cs.sensornumber cs_sensornumber, cs.sensorname cs_sensorname, cs.recordingmode cs_recordingmode,
                    cs.recordinginterval cs_recordinginterval, cs.timeoffirstsample cs_timeoffirstsample,
                    cs.equipmentid cs_equipmentid, cs.absolutemin cs_absolutemin, cs.absolutemax cs_absolutemax,
                    cs.stat_cd cs_stat_cd, csp.sensornumber csp_sensornumber, csp.prop_name csp_prop_name, csp.prop_value csp_prop_value,

                    dt.id dt_id, dt.standard dt_standard, dt.code dt_code, dt.display_name dt_display_name,

                    ds.id ds_id, ds.name ds_name, ds.script_type ds_script_type, ds.dataorder ds_dataorder,
                    fs.sequencenum fs_sequencenum, fs.label fs_label, fs.format fs_format,


                    ss.decodesscriptid ss_decodesscriptid, ss.sensornumber ss_sensornumber,
                    uc.id uc_id, uc.fromunitsabbr uc_fromunitsabbr, uc.tounitsabbr uc_tounitsabbr, uc.algorithm uc_algorithm,
                    uc.a uc_a, uc.b uc_b, uc.c uc_c, uc.d uc_d, uc.e uc_e, uc.f uc_f,
                    from_eu.unitabbr from_unitabbr, from_eu.name from_name, from_eu.family from_family, from_eu.measures from_measures,
                    to_eu.unitabbr to_unitabbr, to_eu.name to_name, to_eu.family to_family, to_eu.measures to_measures

              from pc
            left outer join equipmentmodel em on em.id = pc.equipmentId
            left outer join equipmentproperty ep on em.id = ep.equipmentid
            left outer join configsensor cs on cs.configid = pc.id
            left outer join configsensorproperty csp on csp.configid = cs.configid and csp.sensornumber = cs.sensornumber
            left outer join configsensordatatype csdt on cs.configid = csdt.configid and cs.sensornumber = csdt.sensornumber
            left outer join datatype dt on csdt.datatypeid = dt.id
            left outer join decodesscript ds on ds.configid = pc.id
            left outer join formatstatement fs on fs.decodesscriptid = ds.id
            left outer join scriptsensor ss on ss.decodesscriptid = ds.id
            left outer join unitconverter uc on ss.unitconverterid = uc.id
            left join engineeringunit from_eu on from_eu.unitabbr = uc.fromunitsabbr
            left join engineeringunit to_eu on to_eu.unitabbr = uc.tounitsabbr


            order by
                pc.name <collate> asc,
                cs.sensornumber asc,
                fs.sequencenum asc
            """;

    private static final String DELETE_CONFIGSENSOR_PROPERTIES = "delete from configsensorproperty where configid = :id";
    private static final String DELETE_CONFIGSENSOR_DATATYPE = "delete from configsensordatatype where configid = :id";
    private static final String DELETE_CONFIGSENSOR = "delete from configsensor where configid = :id";
    private static final String DELETE_SCRIPTSENSOR = """
            delete from scriptsensor
             where decodesscriptid in (select id from decodesscript where configid = :id)
            """;
    private static final String GET_SCRIPTSENSOR_UC_ID = """
                select unitconverterid from scriptsensor
                 where decodesscriptid in (
                    select id
                      from decodesscript
                     where configid = :id
                 )
            """;
    private static final String DELETE_UNITCONVERTER = "delete from unitconverter where id = :id";
    private static final String DELETE_FORMATSTATEMENTS =
        "delete from formatstatement where decodesscriptid  in (select id from decodesscript where configid = :id)";
    private static final String DELETE_DECODESSCRIPT = "delete from decodesscript where configid = :id";

    @InjectDao
    UnitConverterDao ucDao;

    @Override
    public Optional<PlatformConfig> getById(DataTransaction tx, DbKey id) throws OpenDcsDataException
    {
        if (DbKey.isNull(id))
        {
            return Optional.empty();
        }
        var handle = tx.connection(Handle.class)
                       .orElseThrow(() -> new OpenDcsDataException(SqlErrorMessages.NO_JDBI_HANDLE));
        var ctx = tx.getContext();
        var dbEngine = ctx.getDatabase();

        try (var query = handle.createQuery(SELECT_QUERY))
        {
            query.define(SqlQueries.COLLATE_CLAUSE, SqlQueries.collateClauseFor(dbEngine))
                 .define(SqlQueries.WHERE_CLAUSE, "where id = :id")
                 .define(SqlQueries.LIMIT_CLAUSE, "")
                 .bind(GenericColumns.ID, id);

            return query.registerRowMapper(DecodesConfigMapper.withPrefix("pc"))
                        .registerRowMapper(EquipmentModelMapper.withPrefix("em"))
                        .registerRowMapper(PropertiesMapper.PAIR_STRING_STRING, PropertiesMapper.withPrefix("ep"))
                        .registerRowMapper(UnitConverterMapper.withPrefix("uc"))
                        .reduceResultSet(new LinkedHashMap<>(),
                                         new DecodesConfigAccumulator(
                                            "pc", DecodesConfigMapper.withPrefix("pc"),
                                            EquipmentModelMapper.withPrefix("em"), PropertiesMapper.withPrefix("ep"),
                                            ConfigSensorMapper.withPrefix("cs"), PropertiesMapper.withPrefix("csp", true),
                                            DataTypeMapper.withPrefix("dt"), DecodesScriptBuilderMapper.withPrefix("ds"),
                                            FormatStatementMapper.withPrefix("fs"), UnitConverterMapper.withPrefix("uc")
                                         )
                                        )
                        .values()
                        .stream()
                        .map(pcb -> pcb.build())
                        .findFirst();
        }
    }

    @Override
    public Optional<PlatformConfig> getByName(DataTransaction tx, String name) throws OpenDcsDataException
    {
        if (name == null || name.isBlank())
        {
            return Optional.empty();
        }
        var handle = tx.connection(Handle.class)
                       .orElseThrow(() -> new OpenDcsDataException(SqlErrorMessages.NO_JDBI_HANDLE));
        var ctx = tx.getContext();
        var dbEngine = ctx.getDatabase();
        try (var query = handle.createQuery(SELECT_QUERY))
        {
            query.define(SqlQueries.COLLATE_CLAUSE, SqlQueries.collateClauseFor(dbEngine))
                 .define(SqlQueries.WHERE_CLAUSE, "where upper(name) = upper(:name)")
                 .define(SqlQueries.LIMIT_CLAUSE, "")
                 .bind(GenericColumns.NAME, name);

            return query.registerRowMapper(DecodesConfigMapper.withPrefix("pc"))
                        .registerRowMapper(EquipmentModelMapper.withPrefix("em"))
                        .registerRowMapper(PropertiesMapper.PAIR_STRING_STRING, PropertiesMapper.withPrefix("ep"))
                        .reduceResultSet(new LinkedHashMap<>(),
                                         new DecodesConfigAccumulator(
                                            "pc", DecodesConfigMapper.withPrefix("pc"),
                                            EquipmentModelMapper.withPrefix("em"), PropertiesMapper.withPrefix("ep"),
                                            ConfigSensorMapper.withPrefix("cs"), PropertiesMapper.withPrefix("csp", true),
                                            DataTypeMapper.withPrefix("dt"), DecodesScriptBuilderMapper.withPrefix("ds"),
                                            FormatStatementMapper.withPrefix("fs"), UnitConverterMapper.withPrefix("uc")
                                         )
                                        )
                        .values()
                        .stream()
                        .map(pcb -> pcb.build())
                        .findFirst();
        }
    }

    @Override
    public PlatformConfig save(DataTransaction tx, PlatformConfig pc) throws OpenDcsDataException
    {


        var handle = tx.connection(Handle.class)
                       .orElseThrow(() -> new OpenDcsDataException(SqlErrorMessages.NO_JDBI_HANDLE));
        var ctx = tx.getContext();
        var keyGen = ctx.getGenerator(KeyGenerator.class)
                        .orElseThrow(() -> new OpenDcsDataException("No key generator configured."));
        final var mergeSql = """
                merge into platformconfig pc
                using (
                    select :id id, :name name, :description description, :equipmentid equipmentid
                ) input
                on (pc.id = input.id)
                when matched then
                    update set
                        name = input.name, description = input.description, equipmentid = input.equipmentid
                when not matched then
                    insert (id, name, description, equipmentid)
                    values(input.id, input.name, input.description, input.equipmentid)
                """;
        try (var merge = handle.createUpdate(mergeSql);
             var deleteConfigSensorProps = handle.createUpdate(DELETE_CONFIGSENSOR_PROPERTIES);
             var deleteConfigSensorDataType = handle.createUpdate(DELETE_CONFIGSENSOR_DATATYPE);
             var deleteConfigSensor = handle.createUpdate(DELETE_CONFIGSENSOR);
             var deleteFormatStatements = handle.createUpdate(DELETE_FORMATSTATEMENTS);
             var deleteScriptSensorUc = handle.prepareBatch(DELETE_UNITCONVERTER);
             var getUnitConverterIds = handle.createQuery(GET_SCRIPTSENSOR_UC_ID);
             var deleteScriptSensor = handle.createUpdate(DELETE_SCRIPTSENSOR);
             var deleteScript = handle.createUpdate(DELETE_DECODESSCRIPT)
            )

        {
            DbKey id = pc.getId();
            var existing = getByName(tx, pc.getName());
            if (existing.isPresent())
            {
                // If there's an existing app with this name, we'll just assume the provided id, if any, was in error
                id = existing.get().getId();
                log.trace("""
                    Using ID from existing Config, id={}, that was found. Provided ID was {}.
                    """,
                    id, pc.getId());
            }
            final var bindKey = !DbKey.isNull(id) ? id : keyGen.getKey("platformconfig", handle.getConnection());

            final var em = pc.getEquipmentModel();
            merge.bind(GenericColumns.ID, bindKey)
                 .bind(GenericColumns.NAME, pc.getName())
                 .bind(GenericColumns.DESCRIPTION, pc.description)
                 .bindByType("equipmentid", em != null ? em.getId() : null, DbKey.class)
                 .execute()
                 ;

            // delete everything
            deleteConfigSensorProps.bind(GenericColumns.ID, bindKey).execute();
            deleteConfigSensorDataType.bind(GenericColumns.ID, bindKey).execute();

            var ucIds = getUnitConverterIds.bind(GenericColumns.ID, bindKey)
                                           .mapTo(DbKey.class)
                                           .collectIntoList();

            deleteScriptSensor.bind(GenericColumns.ID, bindKey).execute();

            for (var ucId: ucIds)
            {
                deleteScriptSensorUc.bind(GenericColumns.ID, ucId).add();
            }
            deleteScriptSensorUc.execute();

            deleteConfigSensor.bind(GenericColumns.ID, bindKey).execute();
            deleteFormatStatements.bind(GenericColumns.ID, bindKey).execute();
            deleteScript.bind(GenericColumns.ID, bindKey).execute();



            // put everything back
            insertConfigSensors(handle, bindKey, pc);
            insertScripts(tx, handle, bindKey, pc, keyGen);

            return getById(tx, bindKey).orElseThrow(() -> new OpenDcsDataException("Unable to retrieve Platform Config we just saved."));
        }
        catch (DatabaseException ex)
        {
            throw new OpenDcsDataException("Unable to create surrogate key for new platform config.", ex);
        }
    }


    private void insertConfigSensors(Handle handle, DbKey configId, PlatformConfig pc)
    {
        try (var insertSensor = handle.prepareBatch("""
                    insert into
                    configsensor(
                            configid, sensornumber, sensorname, recordingmode,
                            recordinginterval, timeoffirstsample, equipmentid,
                            absolutemin, absolutemax, stat_cd
                        )
                    values (:configid, :sensornumber, :sensorname, :recordingmode,
                            :recordinginterval, :timeoffirstsample, :equipmentid,
                            :absolutemin, :absolutemax, :stat_cd)
                    """).registerArgument(new NullableDoubleArgumentFactory());
            var insertSensorProps = handle.prepareBatch("""
                    insert into
                     configsensorproperty(configid, sensornumber, prop_name, prop_value)
                     values (:configid, :sensornumber, :prop_name, :prop_value)
                    """);
            var insertSensorDataType = handle.prepareBatch("""
                    insert into configsensordatatype (configid, sensornumber, datatypeid)
                    values(:configid, :sensornumber, :datatypeid)
                    """)
                )
        {
            for (var sensor: pc.getSensorVec())
            {
                insertSensor.bind(CONFIGID, configId)
                            .bind(SENSORNUMBER, sensor.sensorNumber)
                            .bind("sensorname", sensor.sensorName)
                            .bind("recordingmode", sensor.recordingMode)
                            .bind("recordinginterval", sensor.recordingInterval)
                            .bind("timeoffirstsample", sensor.timeOfFirstSample)
                            .bindByType("equipmentid", sensor.equipmentModel != null ? sensor.equipmentModel.getId() : null, DbKey.class)
                            .bind("absolutemin", sensor.absoluteMin)
                            .bind("absolutemax", sensor.absoluteMax)
                            .bind("stat_cd", sensor.getUsgsStatCode())
                            .add();
                for (var dt: sensor.getDataTypeVec())
                {
                    insertSensorDataType.bind(CONFIGID, configId)
                                        .bind(SENSORNUMBER, sensor.sensorNumber)
                                        .bind("datatypeid", dt.getId())
                                        .add();
                }

                sensor.getProperties().forEach((k,v) ->
                    insertSensorProps.bind(CONFIGID, configId)
                                     .bind(SENSORNUMBER, sensor.sensorNumber)
                                     .bind("prop_name", k)
                                     .bind("prop_value", v)
                                     .add()
                );
            }

            insertSensor.execute();
            insertSensorProps.execute();
            insertSensorDataType.execute();

        }

    }

    private void insertScripts(DataTransaction tx, Handle handle, DbKey configId, PlatformConfig pc, KeyGenerator keyGen) throws OpenDcsDataException, DatabaseException
    {
        try (var insertScript = handle.prepareBatch("""
                insert into
                    decodesscript(id, configid, name, script_type, dataorder)
                           values(:id, :configid, :name, :script_type, :dataorder)
                """);
            var insertScriptSensor = handle.prepareBatch("""
                    insert into scriptsensor (decodesscriptid, sensornumber, unitconverterid)
                        values (:decodesscriptid, :sensornumber, :unitconverterid)
                    """);
            var insertFormatStatement = handle.prepareBatch("""
                    insert into
                        formatstatement(decodesscriptid, sequencenum, label, format)
                        values (:decodesscriptid, :sequencenum, :label, :format)
                    """))
        {
            for (var script: pc.decodesScripts)
            {
                final var id = script.idIsSet() ? script.getId() : keyGen.getKey("decodesscript", handle.getConnection());
                insertScript.bind(GenericColumns.ID, id)
                            .bind(CONFIGID, configId)
                            .bind(GenericColumns.NAME, script.scriptName)
                            .bind("script_type", script.scriptType)
                            .bind("dataorder", script.getDataOrder())
                            .add();

                for (var sensor: script.scriptSensors)
                {

                    final var uc = ucDao.save(tx, sensor.rawConverter);

                    insertScriptSensor.bind(DECODESSCRIPTID, id)
                                      .bind(SENSORNUMBER, sensor.sensorNumber)
                                      .bind("unitconverterid", uc.getId())
                                      .add();
                }

                for (var fs: script.getFormatStatements())
                {
                    insertFormatStatement.bind(DECODESSCRIPTID, id)
                                         .bind("sequencenum", fs.sequenceNum)
                                         .bind("label", fs.label)
                                         .bind("format", fs.format)
                                         .add();
                }
            }

            insertScript.execute();
            insertScriptSensor.execute();
            insertFormatStatement.execute();
        }
    }


    @Override
    public void delete(DataTransaction tx, DbKey id) throws OpenDcsDataException
    {
        var handle = tx.connection(Handle.class)
                       .orElseThrow(() -> new OpenDcsDataException(SqlErrorMessages.NO_JDBI_HANDLE));
        try(var deleteConfigSensorProps = handle.createUpdate(DELETE_CONFIGSENSOR_PROPERTIES);
            var deleteConfigSensorDataType = handle.createUpdate(DELETE_CONFIGSENSOR_DATATYPE);
            var deleteConfigSensor = handle.createUpdate(DELETE_CONFIGSENSOR);
            var deleteFormatStatements = handle.createUpdate(DELETE_FORMATSTATEMENTS);
            var deleteScriptSensorUc = handle.prepareBatch(DELETE_UNITCONVERTER);
            var getUnitConverterIds = handle.createQuery(GET_SCRIPTSENSOR_UC_ID);
            var deleteScriptSensor = handle.createUpdate(DELETE_SCRIPTSENSOR);
            var deleteScript = handle.createUpdate(DELETE_DECODESSCRIPT);
            var deleteConfig = handle.createUpdate("delete from platformconfig where id = :id"))
        {
            deleteConfigSensorProps.bind(GenericColumns.ID, id).execute();
            deleteConfigSensorDataType.bind(GenericColumns.ID, id).execute();
            deleteConfigSensor.bind(GenericColumns.ID, id).execute();
            var ucIds = getUnitConverterIds.bind(GenericColumns.ID, id)
                                           .mapTo(DbKey.class)
                                           .collectIntoList();

            deleteScriptSensor.bind(GenericColumns.ID, id).execute();

            for (var ucId: ucIds)
            {
                deleteScriptSensorUc.bind(GenericColumns.ID, ucId).add();
            }
            deleteScriptSensorUc.execute();
            deleteFormatStatements.bind(GenericColumns.ID, id).execute();
            deleteScriptSensor.bind(GenericColumns.ID, id).execute();
            deleteScript.bind(GenericColumns.ID, id).execute();
            deleteConfig.bind(GenericColumns.ID, id).execute();

        }
    }

    @Override
    public List<PlatformConfig> getAll(DataTransaction tx, int limit, int offset) throws OpenDcsDataException
    {
        var handle = tx.connection(Handle.class)
                       .orElseThrow(() -> new OpenDcsDataException(SqlErrorMessages.NO_JDBI_HANDLE));
        var ctx = tx.getContext();
        var dbEngine = ctx.getDatabase();

        try (var query = handle.createQuery(SELECT_QUERY))
        {
            query.define(SqlQueries.COLLATE_CLAUSE, SqlQueries.collateClauseFor(dbEngine))
                 .define(SqlQueries.WHERE_CLAUSE, "")
                 .define(SqlQueries.LIMIT_CLAUSE, addLimitOffset(limit, offset));

            if (limit >= 0)
            {
                query.bind(SqlKeywords.LIMIT, limit);
            }
            if (offset >= 0)
            {
                query.bind(SqlKeywords.OFFSET, offset);
            }

            return query.registerRowMapper(DecodesConfigMapper.withPrefix("pc"))
                        .registerRowMapper(EquipmentModelMapper.withPrefix("em"))
                        .registerRowMapper(PropertiesMapper.PAIR_STRING_STRING, PropertiesMapper.withPrefix("ep"))
                        .registerRowMapper(UnitConverterMapper.withPrefix("uc"))
                        .reduceResultSet(new LinkedHashMap<>(),
                                         new DecodesConfigAccumulator(
                                            "pc", DecodesConfigMapper.withPrefix("pc"),
                                            EquipmentModelMapper.withPrefix("em"), PropertiesMapper.withPrefix("ep"),
                                            ConfigSensorMapper.withPrefix("cs"), PropertiesMapper.withPrefix("csp", true),
                                            DataTypeMapper.withPrefix("dt"), DecodesScriptBuilderMapper.withPrefix("ds"),
                                            FormatStatementMapper.withPrefix("fs"), UnitConverterMapper.withPrefix("uc")
                                         )
                                        )
                        .values()
                        .stream()
                        .map(pcb -> pcb.build())
                        .toList();
        }
    }

}
