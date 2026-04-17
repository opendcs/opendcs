package org.opendcs.database.impl.cwms.dao;

import static org.opendcs.utils.sql.SqlQueries.addLimitOffset;

import java.util.Collection;
import java.util.Date;
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
import org.opendcs.utils.sql.SqlErrorMessages;
import org.opendcs.utils.sql.SqlKeywords;
import org.opendcs.utils.sql.SqlQueries;
import org.openide.util.lookup.ServiceProvider;

import decodes.db.Site;
import decodes.db.SiteName;
import decodes.sql.DbKey;
import decodes.util.DecodesSettings;

@ServiceProvider(service = SiteDao.class, path = "dao/CWMS-Oracle")
public final class CwmsSiteDaoImpl implements SiteDao
{
    /**
     * Use of systimestamp for modify_time is not ideal; however it allows for the OpenDCS
     * data flow to behave and have a value without a bunch of downstream null checks.
     */
    private static final String SELECT_QUERY = """
            with sites (location_code, location_id) as (
                select location_code, location_id
                  from cwms_v_loc
                  where unit_system='SI'
                  <where>
                  and db_office_id = 'SPK'
                order by location_id COLLATE BINARY asc
                <limit>
            )
            select site.location_code s_id, site.location_id cwms_id , site.latitude s_latitude, site.longitude s_longitude,
                   site.nearest_city s_nearestcity, site.state_initial s_state, '' s_region,
                   site.time_zone_name s_timezone, site.nation_id s_country, site.elevation s_elevation,
                   site.unit_system s_elevunitabbr, site.description s_description, site.active_flag s_active_flag,
                   site.location_type s_location_type, systimestamp s_modify_time, site.public_name s_public_name,

                   sn.nametype sn_nametype, sn.sitename sn_sitename, sn.dbnum sn_dbnum, sn.agency_cd sn_agency_cd,

                   prop.prop_name p_prop_name, prop.prop_value p_prop_value

             from sites sites
             left outer join cwms_v_loc site on sites.location_code = site.location_code and site.unit_system = 'SI'
             left outer join (
                select siteid, nametype, sitename, dbnum, agency_cd from sitename
                union all
                select location_code siteid, 'CWMS' nametype, location_id sitename, null dbnum, null agency_cd from sites
             ) sn on sn.siteid = sites.location_code
             left outer join site_property prop on prop.site_id = site.location_code
             order by
                cwms_id COLLATE BINARY asc,
                case
                    when sn_nametype = 'CWMS' then 0
                    else 1
                end, sn_nametype COLLATE BINARY asc
             
            """;

    private static final String DELETE_NAMES = "delete from sitename where siteid = :id";
    private static final String DELETE_PROPS = "delete from site_property where site_id = :id";


    @Override
    public Optional<Site> getById(DataTransaction tx, DbKey id) throws OpenDcsDataException {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getById'");
    }

    @Override
    public Optional<Site> getBySiteName(DataTransaction tx, SiteName siteName) throws OpenDcsDataException {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getBySiteName'");
    }

    @Override
    public Optional<Site> getByAnySiteName(DataTransaction tx, Collection<SiteName> siteNames)
            throws OpenDcsDataException {
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
                 //.bind("preferredType", preferredType);
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
                        .registerColumnMapper(Date.class, (r, columnNumber, stmtCtx) -> new Date())
                        .reduceRows(new OpenDcsSiteReducer("s"))
                        
                        .map(s -> s)
                        .toList();
        }
    }
    
}
