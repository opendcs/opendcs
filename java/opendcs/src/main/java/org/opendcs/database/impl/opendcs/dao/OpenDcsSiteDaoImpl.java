package org.opendcs.database.impl.opendcs.dao;

import static org.opendcs.utils.sql.SqlQueries.addLimitOffset;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;

import org.jdbi.v3.core.Handle;
import org.opendcs.database.api.DataTransaction;
import org.opendcs.database.api.OpenDcsDataException;
import org.opendcs.database.dai.SiteDao;
import org.opendcs.database.model.mappers.properties.PropertiesMapper;
import org.opendcs.database.model.mappers.sites.OpenDcsSiteMapper;
import org.opendcs.database.model.mappers.sites.OpenDcsSiteNameMapper;
import org.opendcs.database.model.mappers.sites.OpenDcsSiteReducer;
import org.opendcs.utils.logging.OpenDcsLoggerFactory;
import org.opendcs.utils.sql.GenericColumns;
import org.opendcs.utils.sql.SqlErrorMessages;
import org.opendcs.utils.sql.SqlKeywords;
import org.opendcs.utils.sql.SqlQueries;

import org.openide.util.lookup.ServiceProvider;
import org.openide.util.lookup.ServiceProviders;
import org.slf4j.Logger;

import decodes.db.DatabaseException;
import decodes.db.Site;
import decodes.db.SiteName;
import decodes.sql.DbKey;
import decodes.sql.KeyGenerator;
import decodes.util.DecodesSettings;

// This uses Postgres specific features and is not compatible with Oracle
@ServiceProviders({
    @ServiceProvider(service = SiteDao.class, path = "dao/OpenDCS-Postgres"),
    @ServiceProvider(service = SiteDao.class, path = "dao/OpenDCS-Oracle"),
    // deprecated and also implies supported by the Oracle impl which is false.
    // Unfortunately correct behavior requires eliminating the use of the editDatabaseCode so the names are used.
    // This will be done in a follow up PR.
    @ServiceProvider(service = SiteDao.class, path = "dao/OPENTSDB")
})
public final class OpenDcsSiteDaoImpl implements SiteDao
{
    private static final Logger log = OpenDcsLoggerFactory.getLogger();
    /**
     * order asc on the inner nametype is to keep the general output predicatable.
     * There <b>shouldn't</b> be any Sites without a Preferred SiteName so this is
     * just to cover edge cases in existing databases.
     */
    private static final String SELECT_QUERY = """
            with sites (siteid) as (
                select siteid, sitename
                  from (
                    select  siteid, nametype, min(sitename) sitename
                      from sitename
                     group by siteid, nametype
                     order by
                        case
                            when nametype = :preferredType then 0
                            else 1
                        end, nametype <collate> asc
                     
                  ) sorted_sitenames
                <where>
                order by sitename <collate> asc
                <limit>
            )
            select site.id s_id, site.latitude s_latitude, site.longitude s_longitude,
                   site.nearestcity s_nearestcity, site.state s_state, site.region s_region,
                   site.timezone s_timezone, site.country s_country, site.elevation s_elevation,
                   site.elevunitabbr s_elevunitabbr, site.description s_description, site.active_flag s_active_flag,
                   site.location_type s_location_type, site.modify_time s_modify_time, site.public_name s_public_name,

                   sn.nametype sn_nametype, sn.sitename sn_sitename, sn.dbnum sn_dbnum, sn.agency_cd sn_agency_cd,

                   prop.prop_name p_prop_name, prop.prop_value p_prop_value

             from sites sites
             left outer join site on sites.siteid = site.id
             left outer join sitename sn on sn.siteid = sites.siteid
             left outer join site_property prop on prop.site_id = site.id
            """;

    @Override
    public Optional<Site> getById(DataTransaction tx, DbKey id) throws OpenDcsDataException
    {
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
                 .define(SqlQueries.WHERE_CLAUSE, "where siteid = :id")
                 .define(SqlQueries.LIMIT_CLAUSE, "")
                 .bind("preferredType", preferredType)
                 .bind(GenericColumns.ID, id);

            return query.registerRowMapper(OpenDcsSiteMapper.withPrefix("s"))
                        .registerRowMapper(PropertiesMapper.withPrefix("p", true))
                        .registerRowMapper(OpenDcsSiteNameMapper.withPrefix("sn"))
                        .reduceRows(new OpenDcsSiteReducer("s"))
                        .map(s -> s)
                        .findFirst();
        }
    }

    @Override
    public Optional<Site> getBySiteName(DataTransaction tx, SiteName siteName) throws OpenDcsDataException
    {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getBySiteName'");
    }

    @Override
    public Optional<Site> getByAnySiteName(DataTransaction tx, Collection<SiteName> siteNames) throws OpenDcsDataException
    {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getByAnySiteName'");
    }

    
    private Optional<Site> getByAnySiteName(DataTransaction tx, Iterator<SiteName> siteNames) throws OpenDcsDataException
    {
        final List<SiteName> names = new ArrayList<>();
        siteNames.forEachRemaining(names::add);
        return getByAnySiteName(tx, names);
    }

    @Override
    public Site save(DataTransaction tx, Site site) throws OpenDcsDataException
    {
        var handle = tx.connection(Handle.class)
                       .orElseThrow(() -> new OpenDcsDataException(SqlErrorMessages.NO_JDBI_HANDLE));
        var ctx = tx.getContext();
        var keyGen = ctx.getGenerator(KeyGenerator.class)
                        .orElseThrow(() -> new OpenDcsDataException("No key generator configured."));
        final var mergeSql = """
                    merge into site
                """;
        try (var merge = handle.createUpdate(mergeSql);
            var deleteProps = handle.createUpdate("delete from site_property where site_id = :id");
            var insertProps = handle.prepareBatch("insert into site_property(site_id, prop_name, prop_value) values (:id, :name, :value)");
            var deleteNames = handle.createUpdate("delete from sitename where siteid = :id");
            var insertNames = handle.prepareBatch("insert into sitename(siteid, nametype, sitename, dbnum, agency_cd) values (:id, :nametype, :sitename, :dbnum, :agency_code)");
            )        
        {
            DbKey id = site.getId();
            var existing = getByAnySiteName(tx, site.getNames());
            if (existing.isPresent())
            {
                // If there's an existing app with this name, we'll just assume the provided id, if any, was in error
                id = existing.get().getId();
                log.trace("""
                    Using ID from existing Site, id={}, that was found. Provided ID was {}.
                    Existing Name is {}. New Name is {}.
                    """,
                    id, site.getId());
            }
            final var bindKey = !DbKey.isNull(id) ? id : keyGen.getKey("site", handle.getConnection());
        }
        catch (DatabaseException ex)
        {
            throw new OpenDcsDataException("Unable to generate key to save site", ex);
        }
        return null;
    }

    @Override
    public void delete(DataTransaction tx, DbKey id) throws OpenDcsDataException {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'delete'");
    }

    @Override
    public List<Site> getAll(DataTransaction tx, int limit, int offset) throws OpenDcsDataException
    {
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
                 .define(SqlQueries.WHERE_CLAUSE, "")
                 .define(SqlQueries.LIMIT_CLAUSE, addLimitOffset(limit, offset))
                 .bind("preferredType", preferredType);
            if (limit >= 0)
            {
                query.bind(SqlKeywords.LIMIT, limit);
            }
            if (offset >= 0)
            {
                query.bind(SqlKeywords.OFFSET, offset);
            }

            return query.registerRowMapper(OpenDcsSiteMapper.withPrefix("s"))
                        .registerRowMapper(PropertiesMapper.withPrefix("p", true))
                        .registerRowMapper(OpenDcsSiteNameMapper.withPrefix("sn"))
                        .reduceRows(new OpenDcsSiteReducer("s"))
                        .map(s -> s)
                        .toList();
        }
    }
}
