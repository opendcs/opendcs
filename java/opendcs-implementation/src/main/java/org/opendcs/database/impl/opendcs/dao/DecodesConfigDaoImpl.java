package org.opendcs.database.impl.opendcs.dao;

import org.jdbi.v3.core.Handle;
import org.opendcs.database.api.DataTransaction;
import org.opendcs.database.api.OpenDcsDataException;
import org.opendcs.database.dai.DecodesConfigDao;
import org.opendcs.database.model.mappers.datatype.DataTypeMapper;
import org.opendcs.database.model.mappers.equipmentmodel.EquipmentModelMapper;
import org.opendcs.database.model.mappers.properties.PropertiesMapper;
import org.opendcs.database.impl.opendcs.jdbi.mapper.decodes.configs.DecodesConfigMapper;
import org.opendcs.database.impl.opendcs.jdbi.mapper.decodes.scripts.DecodesScriptBuilderMapper;
import org.opendcs.database.impl.opendcs.jdbi.mapper.decodes.scripts.FormatStatementMapper;
import org.opendcs.database.impl.opendcs.jdbi.mapper.decodes.configs.ConfigSensorMapper;
import org.opendcs.database.impl.opendcs.jdbi.mapper.decodes.configs.DecodesConfigAccumulator;
import org.opendcs.utils.sql.GenericColumns;
import org.opendcs.utils.sql.SqlErrorMessages;
import org.opendcs.utils.sql.SqlQueries;
import org.openide.util.lookup.ServiceProvider;

import decodes.db.PlatformConfig;
import decodes.sql.DbKey;
import decodes.util.DecodesSettings;

import static org.opendcs.utils.sql.SqlQueries.addLimitOffset;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;

@ServiceProvider(service = DecodesConfigDao.class)
public class DecodesConfigDaoImpl implements DecodesConfigDao
{

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
                    cs.stat_cd cs_stat_cd, csp.sensornumber csp_sensornumber, csp.prop_name csp_prop_name, csp.prop_value csp_prop_value

                    ds.id ds_id, ds.name ds_name, ds.script_type ds_script_type, ds.dataorder ds_dataorder,
                    fs.sequencenum fs_sequencenum, fs.label fs_label, fs.format fs_format

                    // todo, script sensors

              from pc
            left outer join equipmentmodel em on em.id = pc.equipmentId
            left outer join equipmentproperty ep on em.id = ep.equipmentid
            left outer join configsensor cs on cs.configid = pc.id
            left outer join configsensorproperty csp on csp.configid = cs.configid and csp.sensornumber = cs.sensornumber
            left outer join configsensordatatype csdt on cs.configid = csdt.configid and cs.sensornumber = csdt.sensornumber
            left outer join datatype dt on csdt.datatypeid = dt.id
            left outer join decodesscript ds on ds.configid = pc.id
            left outer join formatstatement fs on fs.decodesscriptid = ds.id


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
                        .reduceResultSet(new LinkedHashMap<>(),
                                         new DecodesConfigAccumulator(
                                            "pc", DecodesConfigMapper.withPrefix("pc"),
                                            EquipmentModelMapper.withPrefix("em"), PropertiesMapper.withPrefix("ep"),
                                            ConfigSensorMapper.withPrefix("cs"), PropertiesMapper.withPrefix("csp", true),
                                            DataTypeMapper.withPrefix("dt"), DecodesScriptBuilderMapper.withPrefix("ds"),
                                            FormatStatementMapper.withPrefix("fs")
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
        var preferredType = ctx.getSettings(DecodesSettings.class)
                              .map(ds -> ds.siteNameTypePreference)
                              .orElseGet(() -> "CWMS");
        try (var query = handle.createQuery(SELECT_QUERY))
        {
            query.define(SqlQueries.COLLATE_CLAUSE, SqlQueries.collateClauseFor(dbEngine))
                 .define(SqlQueries.WHERE_CLAUSE, "where upper(name) = upper(:name)")
                 .define(SqlQueries.LIMIT_CLAUSE, "")
                 .bind("preferredType", preferredType)
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
                                            FormatStatementMapper.withPrefix("fs")
                                         )
                                        )
                        .values()
                        .stream()
                        .map(pcb -> pcb.build())
                        .findFirst();
        }
    }

    @Override
    public PlatformConfig save(DataTransaction arg0, PlatformConfig arg1) throws OpenDcsDataException
    {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'save'");
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
