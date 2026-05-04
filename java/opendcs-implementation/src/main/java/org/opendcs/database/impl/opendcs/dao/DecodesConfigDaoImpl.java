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
import org.opendcs.database.impl.opendcs.jdbi.mapper.decodes.configs.ConfigSensorMapper;
import org.opendcs.database.impl.opendcs.jdbi.mapper.decodes.configs.DecodesConfigAccumulator;
import org.opendcs.utils.logging.OpenDcsLoggerFactory;
import org.opendcs.utils.sql.GenericColumns;
import org.opendcs.utils.sql.SqlErrorMessages;
import org.opendcs.utils.sql.SqlQueries;
import org.openide.util.lookup.ServiceProvider;
import org.slf4j.Logger;

import decodes.db.DatabaseException;
import decodes.db.PlatformConfig;
import decodes.sql.DbKey;
import decodes.sql.KeyGenerator;
import decodes.util.DecodesSettings;

import static org.opendcs.utils.sql.SqlQueries.addLimitOffset;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;

@ServiceProvider(service = DecodesConfigDao.class)
public class DecodesConfigDaoImpl implements DecodesConfigDao
{
    private static final Logger log = OpenDcsLoggerFactory.getLogger();

    @InjectDao
    UnitConverterDao ucDao;

    private static final String SELECT_QUERY = """
            with pc (id, name, description, equipmentId) as (
                select id, name, description, equipmentId
                  from PlatformConfig
                <where>
                <limit>
                order by name <collate> ASC
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
        var preferredType = ctx.getSettings(DecodesSettings.class)
                              .map(ds -> ds.siteNameTypePreference)
                              .orElseGet(() -> "CWMS");
        try (var query = handle.createQuery(SELECT_QUERY))
        {
            query.define(SqlQueries.COLLATE_CLAUSE, SqlQueries.collateClauseFor(dbEngine))
                 .define(SqlQueries.WHERE_CLAUSE, "where id = :id")
                 .define(SqlQueries.LIMIT_CLAUSE, "")
                 .bind("preferredType", preferredType)
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
        var dbEngine = ctx.getDatabase();
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
        try (var merge = handle.createUpdate(mergeSql))
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
                 .bindByType("equipmentid", em != null ? em.getId() : (DbKey)null, DbKey.class)
                 .execute()
                 ;

            return getById(tx, bindKey).orElseThrow(() -> new OpenDcsDataException("Unable to retrieve Platform Config we just saved."));
        }
        catch (DatabaseException ex)
        {
            throw new OpenDcsDataException("Unable to create surrogate key for new platform config.", ex);
        }
    }

    @Override
    public void delete(DataTransaction arg0, DbKey arg1) throws OpenDcsDataException
    {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'delete'");
    }

    @Override
    public List<PlatformConfig> getAll(DataTransaction arg0, int arg1, int arg2) throws OpenDcsDataException {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getAll'");
    }

}
