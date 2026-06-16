package org.opendcs.database.impl.opendcs.dao;

import static org.opendcs.utils.sql.SqlQueries.COLLATE_CLAUSE;
import static org.opendcs.utils.sql.SqlQueries.collateClauseFor;

import java.lang.foreign.Linker.Option;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;

import org.jdbi.v3.core.Handle;
import org.opendcs.annotations.api.InjectDao;
import org.opendcs.database.api.DataTransaction;
import org.opendcs.database.api.OpenDcsDataException;
import org.opendcs.database.dai.DecodesConfigDao;
import org.opendcs.database.dai.EquipmentModelDao;
import org.opendcs.database.dai.PlatformDao;
import org.opendcs.database.dai.SiteDao;
import org.opendcs.database.impl.opendcs.jdbi.mapper.decodes.configs.DecodesConfigMapper;
import org.opendcs.database.impl.opendcs.jdbi.mapper.decodes.platforms.PlatformMapper;
import org.opendcs.database.impl.opendcs.jdbi.mapper.decodes.platforms.TransportMediumMapper;
import org.opendcs.utils.sql.SqlErrorMessages;
import org.openide.util.lookup.ServiceProvider;
import org.openide.util.lookup.ServiceProviders;

import decodes.db.Platform;
import decodes.db.Site;
import decodes.sql.DbKey;


@ServiceProviders({
    @ServiceProvider(service = PlatformDao.class, path = "dao/OpenDCS-Postgres"),
    @ServiceProvider(service = PlatformDao.class, path = "dao/OpenDCS-Oracle"),
    @ServiceProvider(service = PlatformDao.class, path = "dao/OTSDB"),
})
public class PlatformDaoImpl implements PlatformDao
{
    private static final String SELECT_SQL = """
            with platforms(id, mediumtype, mediumid, designator, siteid) as (
                select p.id id, tm.mediumtype, tm.mediumid , p.designator , p.siteid
                 from platform p
                  join transportmedium tm on tm.platformid = p.id <medium_filter>
                <where>
                order by medium_type <collate> asc, medium_id <collate> asc, designator <collate> asc
            )

            select 
                <platform_columns>,
                <medium_columns>
                <config_columns>
                <site_columns>
            from platforms p
            join transportmedium tm on tm.platformid = p.id <medium_filter>
            <config_join>
            <site_join>

            order by medium_type <collate> asc, medium_id <collate> asc, , designator <collate> asc
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
            var platformMapper = PlatformMapper.withPrefix("p");
            var configMapper = DecodesConfigMapper.withPrefix("config");
            var mediumMapper = TransportMediumMapper.withPrefix("tm");

            //var mediumJoin = mediumMapper.columnsAndJoin("left outer", TransportMediumMapper.Columns.PLATFORM_ID, "p", PlatformMapper.Columns.ID.column());
            select.define("config_join", "")
                  .define("site_join", "")
                  .define(COLLATE_CLAUSE, collateClauseFor(dbEngine))
                  ;
        

            return select.registerRowMapper(platformMapper)
                         .registerRowMapper(configMapper)
                         .registerRowMapper(mediumMapper)
                         .reduceRows(null)
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
        return List.of();
    }
    
}
