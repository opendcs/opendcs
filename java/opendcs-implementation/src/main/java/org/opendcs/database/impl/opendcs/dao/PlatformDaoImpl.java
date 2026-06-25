package org.opendcs.database.impl.opendcs.dao;

import static org.opendcs.utils.sql.SqlQueries.COLLATE_CLAUSE;
import static org.opendcs.utils.sql.SqlQueries.WHERE_CLAUSE;
import static org.opendcs.utils.sql.SqlQueries.collateClauseFor;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;

import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.statement.SqlLogger;
import org.jdbi.v3.core.statement.StatementContext;
import org.jdbi.v3.stringtemplate4.StringTemplateEngine;
import org.opendcs.annotations.api.InjectDao;
import org.opendcs.database.api.DataTransaction;
import org.opendcs.database.api.OpenDcsDataException;
import org.opendcs.database.dai.DecodesConfigDao;
import org.opendcs.database.dai.EquipmentModelDao;
import org.opendcs.database.dai.PlatformDao;
import org.opendcs.database.dai.SiteDao;
import org.opendcs.database.impl.opendcs.jdbi.mapper.decodes.configs.DecodesConfigMapper;
import org.opendcs.database.impl.opendcs.jdbi.mapper.decodes.platforms.PlatformMapper;
import org.opendcs.database.impl.opendcs.jdbi.mapper.decodes.platforms.PlatformReducer;
import org.opendcs.database.impl.opendcs.jdbi.mapper.decodes.platforms.PlatformSensorMapper;
import org.opendcs.database.impl.opendcs.jdbi.mapper.decodes.platforms.PlatformSensorPropertyMapper;
import org.opendcs.database.impl.opendcs.jdbi.mapper.decodes.platforms.PlatformSensorReducer;
import org.opendcs.database.impl.opendcs.jdbi.mapper.decodes.platforms.TransportMediumMapper;
import org.opendcs.database.model.mappers.properties.PropertiesMapper;
import org.opendcs.database.model.mappers.sites.OpenDcsSiteMapper;
import org.opendcs.database.model.mappers.sites.OpenDcsSiteNameMapper;
import org.opendcs.database.model.mappers.sites.OpenDcsSiteReducer;
import org.opendcs.utils.sql.SqlErrorMessages;
import org.openide.util.lookup.ServiceProvider;
import org.openide.util.lookup.ServiceProviders;

import decodes.db.Platform;
import decodes.db.Site;
import decodes.sql.DbKey;


@ServiceProviders({
    @ServiceProvider(service = PlatformDao.class, path = "dao/OpenDCS-Postgres"),
    @ServiceProvider(service = PlatformDao.class, path = "dao/OpenDCS-Oracle"),
    @ServiceProvider(service = PlatformDao.class, path = "dao/OPENTSDB"),
})
public class PlatformDaoImpl implements PlatformDao
{
    private static final String SELECT_SQL = """
            with platforms(id, agency, isproduction, description, lastmodifytime, configid, expiration, mediumtype, mediumid, platformdesignator, siteid) as (
                select p.id id, p.agency agency, p.isproduction isproduction, p.description description, p.lastmodifytime lastmodifytime, p.configid configid,
                       p.expiration expiration, tm.mediumtype, tm.mediumid , p.platformdesignator , p.siteid
                 from platform p
                  join transportmedium tm on tm.platformid = p.id <medium_filter>
                <where>
                order by mediumtype <collate> asc, mediumid <collate> asc, platformdesignator <collate> asc
            )

            select
                <platform_columns>,
                <medium_columns>
                <if(config_columns)><config_columns><endif>
                <site_columns>
                <site_name_columns>
                <site_props>
                <platform_props>
                <platform_sensors>
                <platform_sensor_props>
            from platforms p
            left outer join transportmedium tm on tm.platformid = p.id <medium_filter>
            <config_join>
            <site_join>
            <site_name_join>
            <site_props_join>
            <platform_props_join>
            <if(platform_sensor_join)><platform_sensor_join><endif>
            <platform_sensor_props_join>

            order by p.mediumtype <collate> asc, p.mediumid <collate> asc, p.platformdesignator <collate> asc
            """;

    @InjectDao
    DecodesConfigDao configDao;

    @InjectDao
    EquipmentModelDao equipmentDao;

    @InjectDao
    SiteDao siteDao;

    @Override
    public Optional<Platform> getById(DataTransaction tx, DbKey id) throws OpenDcsDataException
    {
        return Optional.empty();
    }

    @Override
    public Optional<Platform> getByMediumId(DataTransaction tx, String mediumType, String mediumId,
            ZonedDateTime effectiveFor) throws OpenDcsDataException
    {
        var handle = tx.connection(Handle.class)
                       .orElseThrow(() -> new OpenDcsDataException(SqlErrorMessages.NO_JDBI_HANDLE));
        var ctx = tx.getContext();
        var dbEngine = ctx.getDatabaseEngine();
        try (var select = handle.createQuery(SELECT_SQL))
        {
            select.setTemplateEngine(new StringTemplateEngine());
            var platformMapper = PlatformMapper.withPrefix("p");
            var configMapper = DecodesConfigMapper.withPrefix("config");
            var mediumMapper = TransportMediumMapper.withPrefix("tm");
            var siteMapper = OpenDcsSiteMapper.withPrefix("s");
            var siteNameMapper = OpenDcsSiteNameMapper.withPrefix("sn");
            var siteReducer = new OpenDcsSiteReducer("s");
            var platformPropsMapper = PropertiesMapper.withPrefix("pp", true);
            var platformSensorMapper = PlatformSensorMapper.withPrefix("ps");
            var platformSensorPropertiesMapper = PlatformSensorPropertyMapper.withPrefix("psp");
            var platformSensorReducder = new PlatformSensorReducer(platformSensorMapper);

            select.define("platform_columns", platformMapper.columnsForSelect())
                  .define("medium_columns", mediumMapper.columnsForSelect() + ",")
                  .define("site_columns", siteMapper.columnsForSelect() + ",")
                  .define("site_name_columns", siteNameMapper.columnsForSelect() + ",")
                  .define("site_props", "sp.prop_name sp_prop_name, sp.prop_value sp_prop_value,")
                  .define("platform_props", platformPropsMapper.columnsForSelect() + ",")
                  .define("platform_sensors", platformSensorMapper.columnsForSelect() + ",")
                  .define("platform_sensor_props", platformSensorPropertiesMapper.columnsForSelect())
                  .define("config_columns", "")
                  .define("config_join", "")
                  .define("site_join", siteMapper.joinStatement("left outer", OpenDcsSiteMapper.Columns.ID,
                                                                     "p", PlatformMapper.Columns.SITE_ID.column()))
                  .define("site_name_join", siteNameMapper.joinStatement("left outer", OpenDcsSiteNameMapper.Columns.SITE_ID,
                                                                              "p", PlatformMapper.Columns.SITE_ID.column()))
                  .define("site_props_join", "left outer join site_property sp on sp.site_id = p.siteid")
                  .define("platform_props_join", "left outer join platformproperty pp on pp.platformid = p.id")
                  .define("platform_sensor_join", platformSensorMapper.joinStatement(
                    "left outer", PlatformSensorMapper.Columns.PLATFORM_ID, "p", PlatformMapper.Columns.ID.column()))
                  .define("platform_sensor_props_join", platformSensorPropertiesMapper.joinStatement(
                    "left outer", PlatformSensorPropertyMapper.Columns.PLATFORM_ID, "p", PlatformMapper.Columns.ID.column()))
                  .define(COLLATE_CLAUSE, collateClauseFor(dbEngine))
                  .define("medium_filter", " and tm.mediumtype = :mediumtype and tm.mediumid = :mediumid")
                  .define(WHERE_CLAUSE, "where mediumtype = :mediumtype and mediumid = :mediumid")
                  ;

            /** Leaving in place for debugging for now.
             * Default logging cutoff the sql which is somewhat... long.
             */
            select.setSqlLogger(new SqlLogger()
            {
                @Override
                public void logBeforeExecution(StatementContext context)
                {
                    System.out.println(context.getRenderedSql());
                }
            });

            return select.registerRowMapper(platformMapper)
                         .registerRowMapper(configMapper)
                         .registerRowMapper(mediumMapper)
                         .registerRowMapper(siteMapper)
                         .registerRowMapper(siteNameMapper)
                         .registerRowMapper(platformSensorMapper)
                         .registerRowMapper(platformSensorPropertiesMapper)
                         .registerRowMapper(PropertiesMapper.withPrefix("sp", true))
                         .registerRowMapper(PlatformReducer.PLATFORM_PROPERTIES, platformPropsMapper)
                         .bind("mediumtype", mediumType)
                         .bind("mediumid", mediumId)
                         .reduceRows(new PlatformReducer(platformMapper, siteReducer, platformSensorReducder))
                         .findFirst();
        }
    }

    @Override
    public List<Platform> findPlatformsFor(DataTransaction tx, Site site, String designator)
            throws OpenDcsDataException
    {
        return List.of();
    }

    @Override
    public Platform save(DataTransaction tx, Platform platform) throws OpenDcsDataException
    {
        return null;
    }

    @Override
    public void delete(DataTransaction tx, DbKey id) throws OpenDcsDataException
    {
        /* not yet */
    }

    @Override
    public List<Platform> getAll(DataTransaction tx, int limit, int offset, boolean fillAll, String mediumType)
            throws OpenDcsDataException
    {
        var handle = tx.connection(Handle.class)
                       .orElseThrow(() -> new OpenDcsDataException(SqlErrorMessages.NO_JDBI_HANDLE));
        var ctx = tx.getContext();
        var dbEngine = ctx.getDatabaseEngine();
        try (var select = handle.createQuery(SELECT_SQL))
        {
            select.setTemplateEngine(new StringTemplateEngine());
            var platformMapper = PlatformMapper.withPrefix("p");
            var mediumMapper = TransportMediumMapper.withPrefix("tm");
            select.define("platform_columns", platformMapper.columnsForSelect())
                  .define("medium_columns", mediumMapper.columnsForSelect())
                  .define(COLLATE_CLAUSE, collateClauseFor(dbEngine));
            if (mediumType != null && !mediumType.isBlank())
            {
                select.define("medium_filter", " and tm.mediumtype = :mediumtype")
                      .define(WHERE_CLAUSE, "where mediumtype = :mediumtype")
                      .bind("mediumtype", mediumType);
            }
            else
            {
                select.define("medium_filter", "").define(WHERE_CLAUSE,"");
            }

            return select.registerRowMapper(platformMapper)
                         .registerRowMapper(mediumMapper)
                         .reduceRows(new PlatformReducer(platformMapper, null, null))
                         .toList();
        }
    }

}
