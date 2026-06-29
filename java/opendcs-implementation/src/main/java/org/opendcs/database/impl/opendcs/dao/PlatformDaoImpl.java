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
import org.jdbi.v3.stringtemplate4.StringTemplateSqlLocator;
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
import org.stringtemplate.v4.ST;
import org.stringtemplate.v4.STGroup;

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
    private static final STGroup QUERIES = StringTemplateSqlLocator.findStringTemplateGroup(PlatformDaoImpl.class);
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
        var handle = tx.connection(Handle.class)
                       .orElseThrow(() -> new OpenDcsDataException(SqlErrorMessages.NO_JDBI_HANDLE));
        var ctx = tx.getContext();
        var dbEngine = ctx.getDatabaseEngine();

        var selectTemplate = QUERIES.getInstanceOf("select");

        if (selectTemplate == null)
        {
            throw new OpenDcsDataException("Could not find template");
        }
        selectTemplate.add(WHERE_CLAUSE, "where p.id = :id");
        try (var select = handle.createQuery(setDefines(selectTemplate, dbEngine, ALL_DATA)))
        {
            registerMappers(select, ALL_DATA);
            return select.bind(PlatformMapper.Columns.ID.column(), id)
                         .reduceRows(new PlatformReducer(ALL_DATA.platformMapper, ALL_DATA.siteReducer(),
                                                         ALL_DATA.platformSensorReducer()))
                         .findFirst();
        }
    }

    @Override
    public Optional<Platform> getByMediumId(DataTransaction tx, String mediumType, String mediumId,
            ZonedDateTime effectiveFor) throws OpenDcsDataException
    {
        var handle = tx.connection(Handle.class)
                       .orElseThrow(() -> new OpenDcsDataException(SqlErrorMessages.NO_JDBI_HANDLE));
        var ctx = tx.getContext();
        var dbEngine = ctx.getDatabaseEngine();

        var selectTemplate = QUERIES.getInstanceOf("select");

        if (selectTemplate == null)
        {
            throw new OpenDcsDataException("Could not find template");
        }
        selectTemplate.add("medium_filter", " and tm.mediumtype = :mediumtype and tm.mediumid = :mediumid")
                      .add(WHERE_CLAUSE, "where mediumtype = :mediumtype and mediumid = :mediumid");
        try (var select = handle.createQuery(setDefines(selectTemplate, dbEngine, ALL_DATA)))
        {
            registerMappers(select, ALL_DATA);

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

    private static Query registerMappers(Query select, Mappers mappers)
    {
        select.registerRowMapper(mappers.platformMapper())
              .registerRowMapper(mappers.tmMapper());


        if (mappers.siteMapper() != null)
        {
            select.registerRowMapper(mappers.siteMapper())
                  .registerRowMapper(mappers.siteNameMapper())
                  .registerRowMapper(mappers.sitePropsMapper())
            ;
        }

        if (mappers.platformPropsMapper() != null)
        {
            select.registerRowMapper(PlatformReducer.PLATFORM_PROPERTIES, mappers.platformPropsMapper())
                  .registerRowMapper(mappers.platformSensorMapper)
                  .registerRowMapper(mappers.platformSensorPropertiesMapper)
            ;
        }
        return select;
    }

    private static String setDefines(ST select, DatabaseEngine dbEngine, Mappers mappers)
    {
        select.add("platform_columns", mappers.platformMapper().columnsForSelect())
              .add("medium_columns", mappers.tmMapper().columnsForSelect())

        ;

        if (mappers.siteMapper() != null)
        {
            select.add("site_columns", mappers.siteMapper().columnsForSelect())
                  .add("site_name_columns", mappers.siteNameMapper().columnsForSelect())
                  .add("site_props", "sp.prop_name sp_prop_name, sp.prop_value sp_prop_value")
                  .add("site_join", mappers.siteMapper()
                                                  .joinStatement(LEFT_OUTER, OpenDcsSiteMapper.Columns.ID,
                                                                 "p", PlatformMapper.Columns.SITE_ID.column()))
                  .add("site_name_join", mappers.siteNameMapper()
                                                       .joinStatement(LEFT_OUTER, OpenDcsSiteNameMapper.Columns.SITE_ID,
                                                                      "p", PlatformMapper.Columns.SITE_ID.column()))
                  .add("site_props_join", "left outer join site_property sp on sp.site_id = p.siteid")

                  ;
        }

        if (mappers.platformPropsMapper() != null)
        {
            select.add("platform_props", mappers.platformPropsMapper().columnsForSelect())
                  .add("platform_sensors", mappers.platformSensorMapper().columnsForSelect())
                  .add("platform_sensor_props", mappers.platformSensorPropertiesMapper().columnsForSelect())
                  .add("platform_props_join", "left outer join platformproperty pp on pp.platformid = p.id")
                  .add("platform_sensor_join", mappers.platformSensorMapper().joinStatement(
                    "left outer", PlatformSensorMapper.Columns.PLATFORM_ID, "p", PlatformMapper.Columns.ID.column()))
                  .add("platform_sensor_props_join", mappers.platformSensorPropertiesMapper()
                                                                   .joinStatement(LEFT_OUTER,
                                                                                  PlatformSensorPropertyMapper.Columns.PLATFORM_ID,
                                                                                  "p", PlatformMapper.Columns.ID.column()))

                  ;
        }
        return select.add(COLLATE_CLAUSE, collateClauseFor(dbEngine)).render();
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
        var handle = tx.connection(Handle.class)
                       .orElseThrow(() -> new OpenDcsDataException(SqlErrorMessages.NO_JDBI_HANDLE));
        var deletePlatformTemplate = QUERIES.getInstanceOf("deletePlatform");
        var deletePlatformPropertiesTemplate = QUERIES.getInstanceOf("deleteProperties");
        var deletePlatformSensorTemplate = QUERIES.getInstanceOf("deletePlatformSensor");
        var deletePlatformSensorPropertiesTemplate = QUERIES.getInstanceOf("deletePlatformSensorProperties");
        var deleteTransportMediumTemplate = QUERIES.getInstanceOf("deleteTransportMedium");

        try (var deletePlatform = handle.createUpdate(deletePlatformTemplate.render());
             var deletePlatformProperties = handle.createUpdate(deletePlatformPropertiesTemplate.render());
             var deletePlatformSensor = handle.createUpdate(deletePlatformSensorTemplate.render());
             var deletePlatformSensorProperties = handle.createUpdate(deletePlatformSensorPropertiesTemplate.render());
             var deleteTransportMedium = handle.createUpdate(deleteTransportMediumTemplate.render()))
        {
            deleteTransportMedium.bind(PlatformMapper.Columns.ID.column(), id).execute();
            deletePlatformSensorProperties.bind(PlatformMapper.Columns.ID.column(), id).execute();
            deletePlatformSensor.bind(PlatformMapper.Columns.ID.column(), id).execute();
            deletePlatformProperties.bind(PlatformMapper.Columns.ID.column(), id).execute();
            deletePlatform.bind(PlatformMapper.Columns.ID.column(), id).execute();
        }
    }

    @Override
    public List<Platform> getAll(DataTransaction tx, int limit, int offset, boolean fillAll, String mediumType)
            throws OpenDcsDataException
    {
        var handle = tx.connection(Handle.class)
                       .orElseThrow(() -> new OpenDcsDataException(SqlErrorMessages.NO_JDBI_HANDLE));
        var ctx = tx.getContext();
        var dbEngine = ctx.getDatabaseEngine();
        var selectTemplate = QUERIES.getInstanceOf("select");
        if (mediumType != null && !mediumType.isBlank())
        {
            selectTemplate.add("medium_filter", " and tm.mediumtype = :mediumtype")
                          .add(WHERE_CLAUSE, "where mediumtype = :mediumtype");

        }
        final var mappers = fillAll ? ALL_DATA : REF_DATA;
        try (var select = handle.createQuery(setDefines(selectTemplate, dbEngine, mappers)))
        {
            registerMappers(select, mappers);
            if (mediumType != null && !mediumType.isBlank())
            {
                select.bind("mediumtype", mediumType);
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
