package org.opendcs.database.impl.opendcs.dao;

import static org.opendcs.utils.sql.SqlQueries.COLLATE_CLAUSE;
import static org.opendcs.utils.sql.SqlQueries.LEFT_OUTER;
import static org.opendcs.utils.sql.SqlQueries.WHERE_CLAUSE;
import static org.opendcs.utils.sql.SqlQueries.collateClauseFor;

import java.sql.SQLException;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.Vector;
import java.util.function.UnaryOperator;

import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.statement.Query;
import org.jdbi.v3.stringtemplate4.StringTemplateSqlLocator;
import org.opendcs.annotations.api.InjectDao;
import org.opendcs.database.api.DataTransaction;
import org.opendcs.database.api.DatabaseEngine;
import org.opendcs.database.api.OpenDcsDataException;
import org.opendcs.database.api.OpenDcsDataRuntimeException;
import org.opendcs.database.dai.DecodesConfigDao;
import org.opendcs.database.dai.EquipmentModelDao;
import org.opendcs.database.dai.PlatformDao;
import org.opendcs.database.impl.opendcs.jdbi.arguments.decodes.TransportMediumArgumentFinder;
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
import org.opendcs.utils.logging.OpenDcsLoggerFactory;
import org.opendcs.utils.sql.SqlErrorMessages;
import org.openide.util.lookup.ServiceProvider;
import org.openide.util.lookup.ServiceProviders;
import org.slf4j.Logger;
import org.stringtemplate.v4.ST;
import org.stringtemplate.v4.STGroup;

import decodes.db.DatabaseException;
import decodes.db.Platform;
import decodes.db.PlatformSensor;
import decodes.db.Site;
import decodes.db.TransportMedium;
import decodes.sql.DbKey;
import decodes.sql.KeyGenerator;


@ServiceProviders({
    @ServiceProvider(service = PlatformDao.class, path = "dao/OpenDCS-Postgres"),
    @ServiceProvider(service = PlatformDao.class, path = "dao/OpenDCS-Oracle"),
    @ServiceProvider(service = PlatformDao.class, path = "dao/OPENTSDB"),
})
public class PlatformDaoImpl implements PlatformDao
{
    private static final Logger log = OpenDcsLoggerFactory.getLogger();

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

    private static final String SELECT = "select";
    private static final String MERGE = "platformMerge";
    private static final String DELETE_PLATFORM = "deletePlatform";
    private static final String DELETE_PROPERTIES = "deleteProperties";
    private static final String DELETE_PLATFORM_SENSORS = "deletePlatformSensor";
    private static final String DELETE_PLATFORM_SENSOR_PROPERTIES = "deletePlatformSensorProperties";
    private static final String DELETE_TRANSPORT_MEDIUM = "deleteTransportMedium";
    private static final String INSERT_TRANSPORT_MEDIUM = "insertTransportMedium";
    private static final String INSERT_PLATFORM_SENSOR = "insertPlatformSensor";
    private static final String INSERT_PLATFORM_SENSOR_PROPERTY = "insertPlatformSensorProperty";
    private static final String INSERT_PLATFORM_PROPERTY = "insertPlatformProperty";

    /**
     * It would be better to use the Config Mappers and columns
     * directly in a join; however, there is a <b>lot</b> of columns
     * involved and this will mostly be used when retrieving a single Platform
     * so the performance differences shouldn't be all that large.
     */
    @InjectDao
    DecodesConfigDao configDao;

    @InjectDao
    EquipmentModelDao equipmentDao;

    @Override
    public Optional<Platform> getById(DataTransaction tx, DbKey id) throws OpenDcsDataException
    {
        if (DbKey.isNull(id))
        {
            return Optional.empty();
        }
        var handle = tx.connection(Handle.class)
                       .orElseThrow(() -> new OpenDcsDataException(SqlErrorMessages.NO_JDBI_HANDLE));
        var ctx = tx.getContext();
        var dbEngine = ctx.getDatabaseEngine();

        var selectTemplate = QUERIES.getInstanceOf(SELECT);

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
                         .map(p -> mapConfig(tx, p))
                         .findFirst();
        }
    }

    private Platform mapConfig(DataTransaction tx, Platform p) throws OpenDcsDataRuntimeException
    {
        if (p.getConfig() != null)
        {
            try
            {
                var config = configDao.getById(tx, p.getConfig()
                                      .getId())
                                      .orElseGet(() ->
                                      {
                                          log.atWarn().log("No config exists with id {}", p.getConfig().getId());
                                          return null;
                                      });
                p.setConfig(config);
            }
            catch (OpenDcsDataException ex)
            {
                throw new OpenDcsDataRuntimeException("Unable to retrieve PlatformConfig.", ex);
            }
        }

        return p;
    }

    @Override
    public Optional<Platform> getByMediumId(DataTransaction tx, String mediumType, String mediumId,
            ZonedDateTime effectiveFor) throws OpenDcsDataException
    {
        var handle = tx.connection(Handle.class)
                       .orElseThrow(() -> new OpenDcsDataException(SqlErrorMessages.NO_JDBI_HANDLE));
        var ctx = tx.getContext();
        var dbEngine = ctx.getDatabaseEngine();

        var selectTemplate = QUERIES.getInstanceOf(SELECT);

        if (selectTemplate == null)
        {
            throw new OpenDcsDataException("Could not find template");
        }
        selectTemplate.add("medium_filter", " and tm.mediumtype = :mediumtype and tm.mediumid = :mediumid")
                      .add(WHERE_CLAUSE, "where mediumtype = :mediumtype and mediumid = :mediumid");
        try (var select = handle.createQuery(setDefines(selectTemplate, dbEngine, ALL_DATA)))
        {
            registerMappers(select, ALL_DATA);

            return select.bind("mediumtype", mediumType)
                         .bind("mediumid", mediumId)
                         .reduceRows(new PlatformReducer(ALL_DATA.platformMapper, ALL_DATA.siteReducer(),
                                                         ALL_DATA.platformSensorReducer()))
                         .map(p -> mapConfig(tx, p))
                         .findFirst();
        }
    }

    private Optional<Platform> getByMediumIds(DataTransaction tx, Iterable<TransportMedium> mediums) throws OpenDcsDataException
    {
        for (var tm: mediums)
        {
            var p = getByMediumId(tx, tm.getMediumType(), tm.getMediumId());
            if (p.isPresent())
            {
                return p;
            }
        }
        return Optional.empty();
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
        var handle = tx.connection(Handle.class)
                       .orElseThrow(() -> new OpenDcsDataException(SqlErrorMessages.NO_JDBI_HANDLE));
        var ctx = tx.getContext();
        var dbEngine = ctx.getDatabaseEngine();
        var keyGen = ctx.getGenerator(KeyGenerator.class)
                .orElseThrow(() -> new OpenDcsDataException("No key generator configured."));
        var mergeTemplate = QUERIES.getInstanceOf(MERGE)
                                   .add("dual", dbEngine == DatabaseEngine.ORACLE ? "from dual" : "");
        var insertPlatformProperties = QUERIES.getInstanceOf(INSERT_PLATFORM_PROPERTY).render();
        try (var merge = handle.createUpdate(mergeTemplate.render()).define("numeric_date", true);
             var insertProperties = handle.prepareBatch(insertPlatformProperties))
        {
            DbKey id = platform.getId();
            var existing = getByMediumIds(tx, platform.transportMedia );
            if (existing.isPresent())
            {
                // If there's an existing app with this name, we'll just assume the provided id, if any, was in error
                id = existing.get().getId();
                log.trace("""
                    Using ID from existing Platform, id={}, that was found. Provided ID was {}.
                    """,
                    id, platform.getId());
            }
            final var bindKey = !DbKey.isNull(id) ? id : keyGen.getKey("platform", handle.getConnection());
            merge.bind(PlatformMapper.Columns.ID.column(), bindKey)
                 .bind(PlatformMapper.Columns.AGENCY.column(), platform.agency)
                 .bindByType(PlatformMapper.Columns.CONFIG_ID.column(),
                       platform.getConfig() != null ? platform.getConfig().getId() : null, DbKey.class)
                 .bindByType(PlatformMapper.Columns.SITE_ID.column(),
                       platform.getSite() != null ? platform.getSite().getId(): null, DbKey.class)
                 .bind(PlatformMapper.Columns.DESCRIPTION.column(), platform.description)
                 .bind(PlatformMapper.Columns.DESIGNATOR.column(), platform.getPlatformDesignator())
                 .bind(PlatformMapper.Columns.LAST_MODIFY_TIME.column(),
                       ZonedDateTime.now(ZoneId.of("UTC")).toInstant().toEpochMilli())
                 .bindByType(PlatformMapper.Columns.EXPIRATION.column(), platform.expiration, Date.class)
                 .bind(PlatformMapper.Columns.IS_PRODUCTION.column(), platform.isProduction)
                 .execute();

            deletePlatformProperties(handle, bindKey);
            deleteTransportMediums(handle, bindKey);
            deletePlatformSensors(handle, bindKey);

            insertTransportMediums(handle, bindKey, platform.transportMedia);
            insertPlatformSensors(handle, bindKey, platform.platformSensors);
            final var propMapper = ALL_DATA.platformPropsMapper;
            final var propName = propMapper.column(PropertiesMapper.Columns.NAME);
            final var propValue = propMapper.column(PropertiesMapper.Columns.VALUE);
            platform.getProperties()
                    .forEach((k,v) ->
                        insertProperties.bind(PlatformMapper.Columns.ID.column(), bindKey)
                                        .bind(propName, (String)k)
                                        .bind(propValue, (String)v)
                                        .add()
                    );

            insertProperties.execute();
            return getById(tx, bindKey).orElseThrow(() -> new OpenDcsDataException("Unable to retrieve Platform we just saved."));
        }
        catch (SQLException ex)
        {
            throw new OpenDcsDataException("Unable to retrieve proper column name", ex);
        }
        catch (DatabaseException ex)
        {
            throw new OpenDcsDataException("Unable to generate key to save platform.", ex);
        }
    }

    private void insertPlatformSensors(Handle handle, DbKey bindKey, Vector<PlatformSensor> platformSensors)
    {
        var insertPs = QUERIES.getInstanceOf(INSERT_PLATFORM_SENSOR).render();
        var insertPsP = QUERIES.getInstanceOf(INSERT_PLATFORM_SENSOR_PROPERTY).render();
        try (var psBatch = handle.prepareBatch(insertPs);
             var pspBatch = handle.prepareBatch(insertPsP))
        {
            for (var ps: platformSensors)
            {
                psBatch.bind(PlatformSensorMapper.Columns.PLATFORM_ID.column(), bindKey)
                       .bind(PlatformSensorMapper.Columns.SENSOR_NUMBER.column(), ps.sensorNumber)
                       .bindByType(PlatformSensorMapper.Columns.SITE_ID.column(),
                                   ps.site != null ? ps.site.getId() : null, DbKey.class)
                       .bind(PlatformSensorMapper.Columns.DD_NU.column(), ps.getUsgsDdno())
                       .add();

                ps.getProperties()
                  .forEach((k,v) ->
                      pspBatch.bind(PlatformSensorPropertyMapper.Columns.PLATFORM_ID.column(), bindKey)
                              .bind(PlatformSensorPropertyMapper.Columns.SENSOR_NUMBER.column(), ps.sensorNumber)
                              .bind(PlatformSensorPropertyMapper.Columns.PROP_NAME.column(), (String)k)
                              .bind(PlatformSensorPropertyMapper.Columns.PROP_VALUE.column(), (String)v)
                              .add()
                  );
            }
            psBatch.execute();
            pspBatch.execute();
        }        
    }

    private void insertTransportMediums(Handle handle, DbKey bindKey, Vector<TransportMedium> transportMedia)
    {
        var insertTM = QUERIES.getInstanceOf(INSERT_TRANSPORT_MEDIUM).render();
        try (var tmBatch = handle.prepareBatch(insertTM))
        {
            for (var tm: transportMedia)
            {
                tmBatch.bindNamedArgumentFinder(new TransportMediumArgumentFinder(tm))
                       .bind(TransportMediumMapper.Columns.PLATFORM_ID.column(), bindKey)
                       .add();
            }

            tmBatch.execute();
        }
    }

    @Override
    public void delete(DataTransaction tx, DbKey id) throws OpenDcsDataException
    {
        var handle = tx.connection(Handle.class)
                       .orElseThrow(() -> new OpenDcsDataException(SqlErrorMessages.NO_JDBI_HANDLE));
        var deletePlatformTemplate = QUERIES.getInstanceOf(DELETE_PLATFORM);

        deleteTransportMediums(handle, id);
        deletePlatformSensors(handle, id);
        deletePlatformProperties(handle, id);

        try (var deletePlatform = handle.createUpdate(deletePlatformTemplate.render()))
        {
            deletePlatform.bind(PlatformMapper.Columns.ID.column(), id).execute();
        }
    }

    private static void deleteTransportMediums(Handle handle, DbKey id)
    {
        var deleteTransportMediumTemplate = QUERIES.getInstanceOf(DELETE_TRANSPORT_MEDIUM);
        try(var deleteTransportMedium = handle.createUpdate(deleteTransportMediumTemplate.render()))
        {
            deleteTransportMedium.bind(PlatformMapper.Columns.ID.column(), id).execute();
        }
    }

    private static void deletePlatformSensors(Handle handle, DbKey id)
    {
        var deletePlatformSensorTemplate = QUERIES.getInstanceOf(DELETE_PLATFORM_SENSORS);
        var deletePlatformSensorPropertiesTemplate = QUERIES.getInstanceOf(DELETE_PLATFORM_SENSOR_PROPERTIES);
        try (var deletePlatformSensor = handle.createUpdate(deletePlatformSensorTemplate.render());
             var deletePlatformSensorProperties = handle.createUpdate(deletePlatformSensorPropertiesTemplate.render()))
        {
            deletePlatformSensorProperties.bind(PlatformMapper.Columns.ID.column(), id).execute();
            deletePlatformSensor.bind(PlatformMapper.Columns.ID.column(), id).execute();
        }
    }

    private static void deletePlatformProperties(Handle handle, DbKey id)
    {
        var deletePlatformPropertiesTemplate = QUERIES.getInstanceOf(DELETE_PROPERTIES);
        try (var deletePlatformProperties = handle.createUpdate(deletePlatformPropertiesTemplate.render()))
        {
            deletePlatformProperties.bind(PlatformMapper.Columns.ID.column(), id).execute();
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
        var selectTemplate = QUERIES.getInstanceOf(SELECT);
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
            UnaryOperator<Platform> configMapper = fillAll ? (p -> mapConfig(tx, p)) : p -> p;
            return select.reduceRows(new PlatformReducer(mappers.platformMapper(),
                                                         mappers.siteReducer(),
                                                         mappers.platformSensorReducer()))
                         .map(configMapper)
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
