package org.opendcs.database.impl.opendcs.dao;

import static org.opendcs.utils.sql.SqlQueries.COLLATE_CLAUSE;
import static org.opendcs.utils.sql.SqlQueries.LEFT_OUTER;
import static org.opendcs.utils.sql.SqlQueries.WHERE_CLAUSE;
import static org.opendcs.utils.sql.SqlQueries.collateClauseFor;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;

import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.statement.Query;
import org.jdbi.v3.core.statement.SqlLogger;
import org.jdbi.v3.core.statement.StatementContext;
import org.jdbi.v3.stringtemplate4.StringTemplateEngine;
import org.opendcs.annotations.api.InjectDao;
import org.opendcs.database.api.DataTransaction;
import org.opendcs.database.api.DatabaseEngine;
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
                  join transportmedium tm on tm.platformid = p.id <if(medium_filter)><medium_filter><endif>
                <if(where)><where><endif>
                order by mediumtype <collate> asc, mediumid <collate> asc, platformdesignator <collate> asc
            )
            select
                <platform_columns>
                <if(medium_columns)>,<medium_columns><endif>
                <if(config_columns)>, <config_columns><endif>
                <if(site_columns)>, <site_columns><endif>
                <if(site_name_columns)>, <site_name_columns><endif>
                <if(site_props)>, <site_props><endif>
                <if(platform_props)>, <platform_props><endif>
                <if(platform_sensors)>, <platform_sensors><endif>
                <if(platform_sensor_props)>, <platform_sensor_props><endif>
            from platforms p
            left outer join transportmedium tm on tm.platformid = p.id <medium_filter>
            <if(config_join)><config_join><endif>
            <if(site_join)><site_join><endif>
            <if(site_name_join)><site_name_join><endif>
            <if(site_props_join)><site_props_join><endif>
            <if(platform_props_join)><platform_props_join><endif>
            <if(platform_sensor_join)><platform_sensor_join><endif>
            <if(platform_sensor_props_join)><platform_sensor_props_join><endif>

            order by p.mediumtype <collate> asc, p.mediumid <collate> asc, p.platformdesignator <collate> asc
            """;
    private static final Mappers ALL_DATA = new Mappers(
        PlatformMapper.withPrefix("p"),
        DecodesConfigMapper.withPrefix("config"),
        TransportMediumMapper.withPrefix("tm"),
        OpenDcsSiteMapper.withPrefix("s"),
        OpenDcsSiteNameMapper.withPrefix("sn"),
        PropertiesMapper.withPrefix("sp", true),
        new OpenDcsSiteReducer("s"),
        PropertiesMapper.withPrefix("pp", true),
        PlatformSensorMapper.withPrefix("ps"),
        PlatformSensorPropertyMapper.withPrefix("psp")
    );

    private static final Mappers REF_DATA = new Mappers(
        PlatformMapper.withPrefix("p"),
        null,
        TransportMediumMapper.withPrefix("tm"),
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null);

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
            setDefines(select, dbEngine, ALL_DATA)
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

            return select.bind("mediumtype", mediumType)
                         .bind("mediumid", mediumId)
                         .reduceRows(new PlatformReducer(ALL_DATA.platformMapper, ALL_DATA.siteReducer(),
                                                         ALL_DATA.platformSensorReducer()))
                         .findFirst();
        }
    }

    private static Query setDefines(Query select, DatabaseEngine dbEngine, Mappers mappers)
    {
        select.define("platform_columns", mappers.platformMapper().columnsForSelect())
              .define("medium_columns", mappers.tmMapper().columnsForSelect())
              .registerRowMapper(mappers.platformMapper())
              .registerRowMapper(mappers.tmMapper())
        ;
        
        if (mappers.siteMapper() != null)
        {
            select.define("site_columns", mappers.siteMapper().columnsForSelect())
                  .define("site_name_columns", mappers.siteNameMapper().columnsForSelect())
                  .define("site_props", "sp.prop_name sp_prop_name, sp.prop_value sp_prop_value")
                  .define("site_join", mappers.siteMapper()
                                                  .joinStatement(LEFT_OUTER, OpenDcsSiteMapper.Columns.ID,
                                                                 "p", PlatformMapper.Columns.SITE_ID.column()))
                  .define("site_name_join", mappers.siteNameMapper()
                                                       .joinStatement(LEFT_OUTER, OpenDcsSiteNameMapper.Columns.SITE_ID,
                                                                      "p", PlatformMapper.Columns.SITE_ID.column()))
                  .define("site_props_join", "left outer join site_property sp on sp.site_id = p.siteid")
                  .registerRowMapper(mappers.siteMapper())
                  .registerRowMapper(mappers.siteNameMapper())
                  .registerRowMapper(mappers.sitePropsMapper())
                  ;
        }
        
        if (mappers.platformPropsMapper() != null)
        {
            select.define("platform_props", mappers.platformPropsMapper().columnsForSelect())
                  .define("platform_sensors", mappers.platformSensorMapper().columnsForSelect())
                  .define("platform_sensor_props", mappers.platformSensorPropertiesMapper().columnsForSelect())
                  .define("platform_props_join", "left outer join platformproperty pp on pp.platformid = p.id")
                  .define("platform_sensor_join", mappers.platformSensorMapper().joinStatement(
                    "left outer", PlatformSensorMapper.Columns.PLATFORM_ID, "p", PlatformMapper.Columns.ID.column()))
                  .define("platform_sensor_props_join", mappers.platformSensorPropertiesMapper()
                                                                   .joinStatement(LEFT_OUTER, 
                                                                                  PlatformSensorPropertyMapper.Columns.PLATFORM_ID, 
                                                                                  "p", PlatformMapper.Columns.ID.column()))
                  .registerRowMapper(PlatformReducer.PLATFORM_PROPERTIES, mappers.platformPropsMapper())
                  .registerRowMapper(mappers.platformSensorMapper)
                  .registerRowMapper(mappers.platformSensorPropertiesMapper)
                  ;
        }
        return select.define(COLLATE_CLAUSE, collateClauseFor(dbEngine));
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
            final var mappers = fillAll ? ALL_DATA : REF_DATA;
            setDefines(select, dbEngine, mappers)
                  .define(COLLATE_CLAUSE, collateClauseFor(dbEngine));
            if (mediumType != null && !mediumType.isBlank())
            {
                select.define("medium_filter", " and tm.mediumtype = :mediumtype")
                      .define(WHERE_CLAUSE, "where mediumtype = :mediumtype")
                      .bind("mediumtype", mediumType);
            }

            return select.reduceRows(new PlatformReducer(mappers.platformMapper(),
                                                         mappers.siteReducer(),
                                                         mappers.platformSensorReducer()))
                         .toList();
        }
    }


    public static record Mappers(PlatformMapper platformMapper, DecodesConfigMapper configMapper,
        TransportMediumMapper tmMapper, OpenDcsSiteMapper siteMapper, OpenDcsSiteNameMapper siteNameMapper,
        PropertiesMapper sitePropsMapper,
        OpenDcsSiteReducer siteReducer, PropertiesMapper platformPropsMapper, PlatformSensorMapper platformSensorMapper,
        PlatformSensorPropertyMapper platformSensorPropertiesMapper, PlatformSensorReducer platformSensorReducer
    )
    {
        public Mappers(PlatformMapper platformMapper, DecodesConfigMapper configMapper,
                       TransportMediumMapper tmMapper, OpenDcsSiteMapper siteMapper,
                       OpenDcsSiteNameMapper siteNameMapper, PropertiesMapper sitePropsMapper,
                       OpenDcsSiteReducer siteReducer, PropertiesMapper platformPropsMapper,
                       PlatformSensorMapper platformSensorMapper,
                       PlatformSensorPropertyMapper platformSensorPropertiesMapper)
        {
            this(platformMapper, configMapper, tmMapper, siteMapper, siteNameMapper, sitePropsMapper, siteReducer,
                platformPropsMapper, platformSensorMapper, platformSensorPropertiesMapper,
                platformSensorMapper != null ?  new PlatformSensorReducer(platformSensorMapper) : null);
        }
    }
}
