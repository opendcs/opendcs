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
import org.opendcs.database.impl.opendcs.dao.OpenDcsSiteDaoImpl;
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
import org.slf4j.Logger;

import decodes.db.Constants;
import decodes.db.DatabaseException;
import decodes.db.Site;
import decodes.db.SiteName;
import decodes.sql.DbKey;
import decodes.sql.KeyGenerator;
import decodes.util.DecodesSettings;

@ServiceProvider(service = SiteDao.class, path = "dao/CWMS-Oracle")
public final class CwmsSiteDaoImpl extends OpenDcsSiteDaoImpl
{
    private static final Logger log = OpenDcsLoggerFactory.getLogger();
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
    public Optional<Site> getById(DataTransaction tx, DbKey id) throws OpenDcsDataException
    {
        var handle = tx.connection(Handle.class)
                       .orElseThrow(() -> new OpenDcsDataException(SqlErrorMessages.NO_JDBI_HANDLE));
        var ctx = tx.getContext();
        var dbEngine = ctx.getDatabase();

        try (var query = handle.createQuery(SELECT_QUERY))
        {
            query.define(SqlQueries.COLLATE_CLAUSE, SqlQueries.collateClauseFor(dbEngine))
                 .define(SqlQueries.WHERE_CLAUSE, "")
                 .define(SqlQueries.LIMIT_CLAUSE, "")
                 .define("where", "and location_code = :id")
                 .bind(GenericColumns.ID, id);

            return query.registerRowMapper(OpenDcsSiteMapper.withPrefix("s"))
                        .registerRowMapper(PropertiesMapper.withPrefix("p", true))
                        .registerRowMapper(OpenDcsSiteNameMapper.withPrefix("sn"))
                        .registerColumnMapper(Date.class, (r, columnNumber, stmtCtx) -> new Date())
                        .reduceRows(new OpenDcsSiteReducer("s"))
                        .findFirst();
        }
    }


    @Override
    public Optional<Site> getByAnySiteName(DataTransaction tx, Collection<SiteName> siteNames)
            throws OpenDcsDataException
    {
        var handle = tx.connection(Handle.class)
                       .orElseThrow(() -> new OpenDcsDataException(SqlErrorMessages.NO_JDBI_HANDLE));

        try (var query = handle.createQuery("""
            select distinct siteid from 
                (
                select siteid, nametype, sitename from sitename
                union all
                select location_code siteid, 'CWMS' nametype, location_id sitename from cwms_v_loc where unit_system='SI'
                )
            <where>
            """))
        {
            final StringBuilder whereClause = new StringBuilder();
            siteNames.forEach(sn ->
            {
                final var bindName = OpenDcsSiteDaoImpl.WHITE_SPACE.matcher(sn.getNameType()).replaceAll("_");
                final var bindValue = bindName + "Value";

                if (!whereClause.isEmpty())
                {
                    whereClause.append(" or ");
                }
                else
                {
                    whereClause.append(" where ");
                }
                whereClause.append(" (")
                           .append("nametype = :").append(bindName)
                           .append(" and ")
                           .append("sitename =:").append(bindValue)
                           .append(")");

                query.bind(bindName, sn.getNameType())
                     .bind(bindValue, sn.getNameValue());
            });
            var id = query.define("where", whereClause)
                          .mapTo(DbKey.class)
                          .findOne()
                          .orElse(DbKey.NullKey);
            return getById(tx, id);
        }
    }

    @Override
    public Site save(DataTransaction tx, Site site) throws OpenDcsDataException
    {
        var ctx = tx.getContext();
        var canWrite = ctx.getSettings(DecodesSettings.class)
                          .orElseGet(() ->
                          {
                              var ret = new DecodesSettings();
                              ret.writeCwmsLocations = false;
                              return ret;
                          })
                          .writeCwmsLocations;
        if (!canWrite)
        {
            throw new OpenDcsDataException("The Current profile does not allow writing CWMS locations.");
        }

        final var cwmsName = site.getName(Constants.snt_CWMS);
        if (cwmsName == null)
        {
            throw new OpenDcsDataException("CWMS SiteNameType was not provided CWMS Database requires a CWMS name to be present on any Site.");
        }

        var handle = tx.connection(Handle.class)
                       .orElseThrow(() -> new OpenDcsDataException(SqlErrorMessages.NO_JDBI_HANDLE));
        
        var keyGen = ctx.getGenerator(KeyGenerator.class)
                        .orElseThrow(() -> new OpenDcsDataException("No key generator configured."));

        final var storeSql = """
                {cwms_loc.store_location2(
                    p_location_id => :name,
                    p_elevation => :elevation,
                    P_elev_unit_id => :elevation_unit,
                    p_vertical_datum => :vertical_datum,
                    p_latitude => :latitude,
                    p_longtidue => :longitude,
                    p_horizontal_datum => :horizontal_datum,
                    p_public_name => :public_name,
                    p_long_name => :long_name,
                    p_description => :description,
                    p_time_zone_id => :time_zone,
                    p_county_name => NULL,
                    p_state_initial => :state_initial,
                    p_active => :active,
                    p_location_kind_id => NULL,
                    p_map_label => NULL,
                    p_published_latitude => NULL,
                    p_published_longitude => NULL,
                    p_nation_id => :country,
                    p_nearest_city => :nearest_city,

                    p_ignore_nulls => 'T',
                    p_db_office_id => NULL

                )}
                """;

         try (var store = handle.createCall(storeSql);
            var deleteProps = handle.createUpdate(DELETE_PROPS);
            var insertProps = handle.prepareBatch("insert into site_property(site_id, prop_name, prop_value) values (:id, :name, :value)");
            var deleteNames = handle.createUpdate(DELETE_NAMES);
            var insertNames = handle.prepareBatch("insert into sitename(siteid, nametype, sitename, dbnum, agency_cd) values (:id, :nametype, :sitename, :dbnum, :agency_code)")
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
                    """,
                    id, site.getId());
            }
            final var bindKey = id;
            var country = site.country;
            if (country == null || country.isBlank() || country.trim().toLowerCase().startsWith("us"))
            {
					country = "US"; // required
            }
            store.bind(GenericColumns.ID, bindKey)
                 .bind(GenericColumns.NAME, cwmsName.getNameValue())
                 .bind("elevation", site.getElevation())
                 .bind("elevation_units", site.getElevationUnits())
                 .bind("vertical_datum", "NAVD88") // TODO : from props
                 .bind("latitude", site.latitude)
                 .bind("longitude", site.longitude)
                 .bind("horizontal_datum", "WGS84") // TODO: also from props
                 .bind("public_name", site.getPublicName())
                 .bind("description", site.getDescription())
                 .bind("time_zone", site.timeZoneAbbr)
                 .bind("state_initial", site.state)
                 .bind("country", country)
                 .bind("long_name", site.getBriefDescription())
                 .bind("nearest_city", site.nearestCity)
                 .invoke();

            deleteProps.bind(GenericColumns.ID, bindKey).execute();
            deleteNames.bind(GenericColumns.ID, bindKey).execute();

            site.getNames().forEachRemaining(name ->
            {
                if (!Constants.snt_CWMS.equals(name.getNameType()))
                {
                    insertNames.bind(GenericColumns.ID, bindKey)
                               .bind("nametype", name.getNameType())
                               .bind("sitename", name.getNameValue())
                               .bind("db_num", name.getUsgsDbno())
                               .bind("agency_code", name.getAgencyCode())
                               .add();
                }
            });
            insertNames.execute();

            // TODO: exclude values that would be defined above in the store
            site.getProperties().forEach((k,v) ->
            {
                insertProps.bind(GenericColumns.ID, bindKey);
                insertProps.bind(GenericColumns.NAME, k.toString());
                var toSave = v != null ? v.toString() : "";
                insertProps.bind("value", toSave);
                insertProps.add();
            });
            insertProps.execute();

            // we don't directly get the ID so we'll just look up by the well defined CWMS name instead.
            return getBySiteName(tx, cwmsName).orElseThrow(() -> new OpenDcsDataException("Unable to retrieve site we just saved."));
        }
    }

    @Override
    public void delete(DataTransaction tx, DbKey id) throws OpenDcsDataException
    {
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
