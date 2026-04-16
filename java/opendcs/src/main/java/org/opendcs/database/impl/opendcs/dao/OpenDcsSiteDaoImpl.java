package org.opendcs.database.impl.opendcs.dao;

import static org.opendcs.utils.sql.SqlQueries.addLimitOffset;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;

import org.jdbi.v3.core.Handle;
import org.opendcs.database.api.DataTransaction;
import org.opendcs.database.api.OpenDcsDataException;
import org.opendcs.database.dai.SiteDao;
import org.opendcs.database.model.mappers.properties.PropertiesMapper;

import org.opendcs.utils.sql.GenericColumns;
import org.opendcs.utils.sql.SqlErrorMessages;
import org.opendcs.utils.sql.SqlKeywords;
import org.opendcs.utils.sql.SqlQueries;

import org.openide.util.lookup.ServiceProvider;
import org.openide.util.lookup.ServiceProviders;

import decodes.db.Site;
import decodes.db.SiteName;
import decodes.sql.DbKey;

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
    private static final String SELECT_QUERY = """
            with sitenames (siteid, sitename) as (
                select  siteid, min(sitename)
                order by case
                    when nametype = :prefered_type then 0
                    else 1
                end, sitename <collate> asc
            )
            with sites (siteid) as (
                select siteid, sitename
                  from sitenames
                <where>
                order by sitename
                <limit>
            )

            select site.id s_id, site.latitude s_latitude, site.longitude s_longitude,
                   site.nearestcity s_nearestcity, site.state s_state, site.region s_region,
                   site.timezone s_timezone, site.country s_country, site.elevation s_elevation,
                   site.elvunitabbr s_elevunitabbr, site.description s_description, site.active_flag s_active_flag,
                   site.location_type s_location_type, site.modify_type s_modify_time, site.public_name s_public_name

                   sn.nametype sn_nametype, sn.sitename sn_sitename, sn.dbnum sn_dbnum, sn.agency_cd sn_agency_cd

                   prop.prop_name p_prop_name, prop.prop_value p_prop_value

             from sites sites
             left outer join site on sites.siteid = site.id
             left outer join sitename sn on sn.siteid = sites.siteid
             left outer join site_property prop on props.site_id = site.id
            """;

    @Override
    public Optional<Site> getById(DataTransaction tx, DbKey id) throws OpenDcsDataException
    {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getById'");
    }

    @Override
    public Optional<Site> getBySiteName(DataTransaction tx, SiteName siteName) throws OpenDcsDataException
    {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getBySiteName'");
    }

    @Override
    public Optional<Site> getByAnySiteName(DataTransaction tx, String siteName) throws OpenDcsDataException
    {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getByAnySiteName'");
    }

    @Override
    public Site save(DataTransaction tx, Site site) throws OpenDcsDataException {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'save'");
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

            return query.registerRowMapper(SiteRowMapper.withPrefix("s"))
                        .registerRowMapper(PropertiesMapper.withPrefix("p", true))
                        .registerRowMapper(SiteNameMapper.writhPrefix("sn"))
                        .values()
                        .stream()
                        .toList();
        }
    }
}
