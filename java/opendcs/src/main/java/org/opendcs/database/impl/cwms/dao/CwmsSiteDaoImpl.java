package org.opendcs.database.impl.cwms.dao;

import static org.opendcs.utils.sql.SqlQueries.addLimitOffset;

import java.sql.SQLException;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.statement.UnableToExecuteStatementException;
import org.opendcs.database.api.DataTransaction;
import org.opendcs.database.api.OpenDcsDataException;
import org.opendcs.database.dai.SiteDao;
import org.opendcs.database.exceptions.RequiredSiteNameMissingException;
import org.opendcs.database.impl.opendcs.dao.OpenDcsSiteDaoImpl;
import org.opendcs.database.impl.opendcs.jdbi.column.numeric.NullableDoubleArgumentFactory;
import org.opendcs.database.model.mappers.properties.PropertiesMapper;
import org.opendcs.database.model.mappers.sites.OpenDcsSiteMapper;
import org.opendcs.database.model.mappers.sites.OpenDcsSiteNameMapper;
import org.opendcs.database.model.mappers.sites.OpenDcsSiteReducer;
import org.opendcs.model.cwms.CwmsSite;
import org.opendcs.utils.logging.OpenDcsLoggerFactory;
import org.opendcs.utils.sql.GenericColumns;
import org.opendcs.utils.sql.SqlErrorMessages;
import org.opendcs.utils.sql.SqlKeywords;
import org.opendcs.utils.sql.SqlQueries;
import org.openide.util.lookup.ServiceProvider;
import org.slf4j.Logger;

import decodes.db.Constants;
import decodes.db.Site;
import decodes.db.SiteName;
import decodes.sql.DbKey;
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
                  and db_office_id = :officeId
                order by location_id COLLATE BINARY asc
                <limit>
            )
            select site.location_code s_id, site.location_id cwms_id , site.latitude s_latitude, site.longitude s_longitude,
                   site.nearest_city s_nearestcity, site.state_initial s_state, site.county_name s_region,
                   site.time_zone_name s_timezone, site.nation_id s_country, site.elevation s_elevation,
                   'm' s_elevunitabbr, site.description s_description, site.active_flag s_active_flag,
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

             left outer join (
                select site_id, prop_name, prop_value from site_property
                union all
                select location_code siteid, 'horizontal_datum' prop_name, horizontal_datum prop_value from cwms_v_loc where unit_system = 'SI'
                union all
                select location_code siteid, 'vertical_datum' prop_name, vertical_datum prop_value from cwms_v_loc where unit_system = 'SI'
                union all
                select location_code siteid, 'bounding_office' prop_name, bounding_office_id prop_value from cwms_v_loc where unit_system = 'SI'
            ) prop on prop.site_id = site.location_code


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
        var officeId = ctx.getSettings(DecodesSettings.class)
                          .orElseThrow(() -> new OpenDcsDataException("DecodesSettings are not available."))
                          .CwmsOfficeId;
        try (var query = handle.createQuery(SELECT_QUERY))
        {
            query.define(SqlQueries.COLLATE_CLAUSE, SqlQueries.collateClauseFor(dbEngine))
                 .define(SqlQueries.WHERE_CLAUSE, "and location_code = :id")
                 .define(SqlQueries.LIMIT_CLAUSE, "")
                 .bind("officeId", officeId)
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
        var ctx = tx.getContext();
        var officeId = ctx.getSettings(DecodesSettings.class)
                          .orElseThrow(() -> new OpenDcsDataException("DecodesSettings are not available."))
                          .CwmsOfficeId;

        try (var query = handle.createQuery("""
            select distinct siteid from
                (
                    select siteid, nametype, sitename from (
                        select siteid, nametype, sitename from sitename where db_office_code = cwms_util.get_office_code(:officeId)
                        union all
                        select location_code siteid,
                               'CWMS' nametype,
                               location_id sitename
                          from cwms_v_loc
                         where unit_system = 'SI' and db_office_id = :officeId
                    )
                <where>
                )
            """).bind("officeId", officeId))
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
                           .append("sitename = :").append(bindValue)
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
    @SuppressWarnings("java:S138") // this is really only long because the SQL and Binds are vertical for clarity.
    public Site save(DataTransaction tx, Site site) throws OpenDcsDataException
    {
        var ctx = tx.getContext();
        var settings = ctx.getSettings(DecodesSettings.class)
                          .orElseThrow(() -> new OpenDcsDataException("No DecodesSettings are available?"));
        final var officeId = settings.CwmsOfficeId;
        if (!settings.writeCwmsLocations)
        {
            throw new OpenDcsDataException("The Current profile does not allow writing CWMS locations.");
        }

        final var cwmsName = site.getName(Constants.snt_CWMS);
        if (cwmsName == null)
        {
            throw new RequiredSiteNameMissingException(Constants.snt_CWMS);
        }

        var handle = tx.connection(Handle.class)
                       .orElseThrow(() -> new OpenDcsDataException(SqlErrorMessages.NO_JDBI_HANDLE));


        final var storeSql = """
                {call cwms_loc.store_location2(
                    p_location_id => :name,
                    p_location_type => :type,
                    p_elevation => :elevation,
                    P_elev_unit_id => :elevation_units,
                    p_vertical_datum => :vertical_datum,
                    p_latitude => :latitude,
                    p_longitude => :longitude,
                    p_horizontal_datum => :horizontal_datum,
                    p_public_name => :public_name,
                    p_long_name => :long_name,
                    p_description => :description,
                    p_time_zone_id => :time_zone,
                    p_county_name => :county,
                    p_state_initial => :state_initial,
                    p_active => :active,
                    p_location_kind_id => NULL,
                    p_map_label => NULL,
                    p_published_latitude => NULL,
                    p_published_longitude => NULL,
                    p_bounding_office_id => :bounding_office,
                    p_nation_id => :country,
                    p_nearest_city => :nearest_city,

                    p_ignorenulls => :ignore_nulls,
                    p_db_office_id => NULL

                )}
                """;

         try (var store = handle.createCall(storeSql);
            var getCode = handle.createQuery("select cwms_loc.get_location_code(:officeId, :name) id from dual");
            var deleteProps = handle.createUpdate(DELETE_PROPS);
            var insertProps = handle.prepareBatch("insert into site_property(site_id, prop_name, prop_value) values (:id, :name, :value)");
            var deleteNames = handle.createUpdate(DELETE_NAMES);
            var insertNames = handle.prepareBatch("insert into sitename(siteid, nametype, sitename, dbnum, agency_cd) values (:id, :nametype, :sitename, :dbnum, :agency_code)")
            )
        {
            var ignoreNulls = 'F';
            var existing = getByAnySiteName(tx, site.getNames());
            if (existing.isPresent())
            {
                // If there's an existing app with this name, we'll just assume the provided id, if any, was in error
                ignoreNulls = 'T';
                log.trace("""
                    Updating an existing site. For Cwms Site update ignore nulls will be set to true,
                    Generally CWMS doesn't want values removed, just updated.
                    """);
            }
            var country = site.country == null ? "US" : site.country.trim();

            store.registerArgument(new NullableDoubleArgumentFactory())
                 .bind(GenericColumns.NAME, cwmsName.getNameValue())
                 .bind("type", site.getLocationType())
                 .bind("elevation", site.getElevation())
                 .bind("elevation_units", site.getElevationUnits())
                 .bind(CwmsSite.VERTICAL_DATUM, site.getProperty(CwmsSite.VERTICAL_DATUM))
                 .bind("latitude", site.latitude != null ? Double.parseDouble(site.latitude) : null)
                 .bind("longitude", site.longitude != null ? Double.parseDouble(site.longitude) : null)
                 .bind(CwmsSite.HORIZONTAL_DATUM, site.getProperty(CwmsSite.HORIZONTAL_DATUM))
                 .bind("public_name", site.getPublicName())
                 .bind("description", site.getDescription())
                 .bind("time_zone", site.timeZoneAbbr)
                 .bind("county", site.region)
                 .bind("state_initial", site.state)
                 .bind("country", country)
                 .bind("active", "T")
                 .bind("long_name", site.getBriefDescription())
                 .bind("nearest_city", site.nearestCity)
                 .bind("ignore_nulls", ignoreNulls)
                 .bind(CwmsSite.BOUNDING_OFFICE, site.getProperty(CwmsSite.BOUNDING_OFFICE))
                 .invoke();

            final var idOut = getCode.bind("officeId", officeId)
                                     .bind(GenericColumns.NAME, cwmsName.getNameValue())
                                     .mapTo(DbKey.class)
                                     .findOne()
                                     .orElseThrow(() -> new OpenDcsDataException("Unable to retrieve location code for the CWMS Location we just saved."))
                                     ;

            deleteProps.bind(GenericColumns.ID, idOut).execute();
            deleteNames.bind(GenericColumns.ID, idOut).execute();

            site.getNames().forEachRemaining(name ->
            {
                if (!Constants.snt_CWMS.equals(name.getNameType()))
                {
                    insertNames.bind(GenericColumns.ID, idOut)
                               .bind("nametype", name.getNameType())
                               .bind("sitename", name.getNameValue())
                               .bind("dbnum", name.getUsgsDbno())
                               .bind("agency_code", name.getAgencyCode())
                               .add();
                }
            });
            insertNames.execute();

            site.getProperties().forEach((k,v) ->
            {
                if (CwmsSite.CWMS_SITE_PROPERTIES.contains(k))
                {
                    return;
                }
                insertProps.bind(GenericColumns.ID, idOut);
                insertProps.bind(GenericColumns.NAME, k.toString());
                var toSave = v != null ? v.toString() : "";
                insertProps.bind("value", toSave);
                insertProps.add();
            });
            insertProps.execute();

            // we don't directly get the ID so we'll just look up by the well defined CWMS name instead.
            return getById(tx, idOut).orElseThrow(() -> new OpenDcsDataException("Unable to retrieve site we just saved."));
        }
    }

    @Override
    public void delete(DataTransaction tx, DbKey id) throws OpenDcsDataException
    {
        var handle = tx.connection(Handle.class)
                       .orElseThrow(() -> new OpenDcsDataException(SqlErrorMessages.NO_JDBI_HANDLE));

        try (var deleteNames = handle.createUpdate(DELETE_NAMES);
             var deleteProps = handle.createUpdate(DELETE_PROPS);
             var deleteLoc = handle.createCall("{call cwms_loc.delete_location(cwms_loc.get_location_id(:id))}"))
        {
            deleteNames.bind(GenericColumns.ID, id).execute();
            deleteProps.bind(GenericColumns.ID, id).execute();
            deleteLoc.bind(GenericColumns.ID, id).invoke();
            // as we don't have a foreign key we need to manually check everything within opendcs
        }
        catch (UnableToExecuteStatementException ex)
        {
            if (ex.getCause() instanceof SQLException sqlEx && sqlEx.getErrorCode() == 1403 /* ora-01403 no data found  */)
            {
                return; // No data, quietly return.
            }
            throw ex;
        }
    }

    @Override
    public List<Site> getAll(DataTransaction tx, int limit, int offset) throws OpenDcsDataException
    {
        var handle = tx.connection(Handle.class)
                       .orElseThrow(() -> new OpenDcsDataException(SqlErrorMessages.NO_JDBI_HANDLE));
        var ctx = tx.getContext();
        var dbEngine = ctx.getDatabase();
        var settings = ctx.getSettings(DecodesSettings.class)
                          .orElseThrow(() -> new OpenDcsDataException("No DecodesSettings are available?"));
        final var officeId = settings.CwmsOfficeId;

        try (var query = handle.createQuery(SELECT_QUERY))
        {
            query.define(SqlQueries.COLLATE_CLAUSE, SqlQueries.collateClauseFor(dbEngine))
                 .define(SqlQueries.WHERE_CLAUSE, "")
                 .define(SqlQueries.LIMIT_CLAUSE, addLimitOffset(limit, offset))
                 .bind("officeId", officeId);
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
                        .toList();
        }
    }

}
