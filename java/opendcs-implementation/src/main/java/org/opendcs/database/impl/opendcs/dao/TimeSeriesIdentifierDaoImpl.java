package org.opendcs.database.impl.opendcs.dao;

import static org.opendcs.utils.sql.SqlQueries.COLLATE_CLAUSE;
import static org.opendcs.utils.sql.SqlQueries.LIMIT_CLAUSE;
import static org.opendcs.utils.sql.SqlQueries.WHERE_CLAUSE;
import static org.opendcs.utils.sql.SqlQueries.addLimitOffset;

import java.util.List;
import java.util.Optional;

import org.jdbi.v3.core.Handle;
import org.opendcs.annotations.api.InjectDao;
import org.opendcs.database.api.DataTransaction;
import org.opendcs.database.api.OpenDcsDataException;
import org.opendcs.database.dai.DataTypeDao;
import org.opendcs.database.dai.IntervalDurationDao;
import org.opendcs.database.dai.PresentationGroupDao;
import org.opendcs.database.dai.SiteDao;
import org.opendcs.database.dai.TimeSeriesIdentifierDao;
import org.opendcs.database.impl.opendcs.jdbi.mapper.timeseries.OpenDcsTimeSeriesIdentifierMapper;
import org.opendcs.database.impl.opendcs.jdbi.mapper.timeseries.OpenDcsTimeSeriesIdentifierReducer;
import org.opendcs.database.model.mappers.datatype.DataTypeMapper;
import org.opendcs.database.model.mappers.sites.OpenDcsSiteMapper;
import org.opendcs.database.model.mappers.sites.OpenDcsSiteNameMapper;
import org.opendcs.utils.FailableResult;
import org.opendcs.utils.sql.GenericColumns;
import org.opendcs.utils.sql.SqlErrorMessages;
import org.opendcs.utils.sql.SqlKeywords;
import org.opendcs.utils.sql.SqlQueries;
import org.openide.util.lookup.ServiceProvider;
import org.openide.util.lookup.ServiceProviders;

import decodes.cwms.CwmsTsId;
import decodes.db.DataType;
import decodes.sql.DbKey;
import decodes.tsdb.BadTimeSeriesException;
import decodes.tsdb.DbCompParm;
import decodes.tsdb.NoSuchObjectException;
import decodes.tsdb.TimeSeriesIdentifier;
import decodes.util.DecodesSettings;
import opendcs.opentsdb.Interval;

/**
 * NOTE: this is intentionally doing no caching at this time. Current focus is simply "correctness."
 */
@ServiceProviders({
    @ServiceProvider(service = TimeSeriesIdentifierDao.class, path = "dao/OpenDCS-Postgres"),
    @ServiceProvider(service = TimeSeriesIdentifierDao.class, path = "dao/OPENTSDB")
})
public class TimeSeriesIdentifierDaoImpl implements TimeSeriesIdentifierDao
{
    @InjectDao
    SiteDao siteDao;

    @InjectDao
    DataTypeDao dataTypeDao;

    @InjectDao
    IntervalDurationDao intervalDurationDao;

    @InjectDao
    PresentationGroupDao presentationGroupDao;

    private static final String TIMESERIES_IDENTIFIER_QUERY = """
        with time_series_identifier_limit(
                id, unique_string, site_name, data_type_standard, data_type_code, interval, duration, ts_version, active_flag,
                storage_units, storage_table, storage_type, modify_time, description, utc_offset, allow_dst_offset_variation, offset_error_action,
                datatype_id, site_id, interval_id, duration_id    
            ) as (
            select id, unique_string, site_name, data_type_standard, data_type_code, interval, duration, ts_version, active_flag,
                storage_units, storage_table, storage_type, modify_time, description, utc_offset, allow_dst_offset_variation, offset_error_action,
                datatype_id, site_id, interval_id, duration_id    
            from time_series_identifier
            <where>
            order by unique_string <collate> asc
            <limit>
        )
        select 
            tsi.id tsi_id, tsi.ts_version, tsi.site_name, tsi.unique_string tsi_unique_string, tsi.site_name tsi_site_name,  tsi.interval tsi_interval, tsi.duration tsi_duration, 
            tsi.active_flag tsi_active_flag, tsi.storage_units tsi_storage_units, tsi.storage_table tsi_storage_table, tsi.storage_type tsi_storage_type,
            tsi.modify_time tsi_modify_time, tsi.description tsi_description, tsi.utc_offset tsi_utc_offset, tsi.allow_dst_offset_variation tsi_allow_dst_offset_variation,
            tsi.offset_error_action tsi_offset_error_action,
            
            dt.id dt_id, dt.standard dt_standard, dt.code dt_code, dt.display_name dt_display_name,
            
            <site_columns>,
            <site_name_columns>,
            ic.interval_id ic_interval_id, ic.name ic_name, ic.cal_constant ic_cal_constant, ic.cal_multiplier ic_cal_multiplier,
            dc.interval_id dc_interval_id, dc.name dc_name, dc.cal_constant dc_cal_constant, dc.cal_multiplier dc_cal_multiplier,

            tsi.site_id, tsi.interval_id, tsi.duration_id
        
        from time_series_identifier_limit tsi
        join datatype dt on dt.id = tsi.datatype_id
        join site on site.id = tsi.site_id
        join sitename sn on sn.siteid = tsi.site_id
        join interval_code ic on ic.interval_id = tsi.interval_id
        join interval_code dc on dc.interval_id = tsi.duration_id
                
        order by tsi.unique_string <collate> asc
        """;

    @Override
    public Optional<? extends TimeSeriesIdentifier> getByUniqueString(DataTransaction tx, String uniqueString)
            throws BadTimeSeriesException, OpenDcsDataException
    {
        var ret = findBy(tx, uniqueString);
        if (ret.isSuccess())
        {
            return ret.getSuccess();
        }
        else if (ret.getFailure().getCause() instanceof BadTimeSeriesException btse)
        {
            throw btse;
        }
        else
        {
            throw ret.getFailure();
        }
    }

    @Override
    public Optional<? extends TimeSeriesIdentifier> getById(DataTransaction tx, DbKey key)
            throws BadTimeSeriesException, OpenDcsDataException {
        var ret = findBy(tx, key);
        if (ret.isSuccess())
        {
            return ret.getSuccess();
        }
        else if (ret.getFailure().getCause() instanceof BadTimeSeriesException btse)
        {
            throw btse;
        }
        else
        {
            throw ret.getFailure();
        }
    }

    @Override
    public FailableResult<Optional<? extends TimeSeriesIdentifier>, OpenDcsDataException> findBy(DataTransaction tx,
            String uniqueString) {
        // extract the display name, if it was used.
        final var identifier = extractDisplayName(uniqueString);

        try
        {
            var handle = tx.connection(Handle.class)
                           .orElseThrow(() -> new OpenDcsDataException(SqlErrorMessages.NO_JDBI_HANDLE));
            var dbEngine = tx.getContext().getDatabaseEngine();


            try (var query = handle.createQuery(TIMESERIES_IDENTIFIER_QUERY)
                                   .define(COLLATE_CLAUSE, SqlQueries.collateClauseFor(dbEngine))
                                   .define("site_columns", OpenDcsSiteDaoImpl.SITE_COLUMNS)
                                   .define("site_name_columns", OpenDcsSiteDaoImpl.SITE_NAME_COLUMNS)
                                   .define(WHERE_CLAUSE, " where unique_string = :unique_string ")
                                   .define(LIMIT_CLAUSE, ""))
            {
                return FailableResult.success(
                    query.bind("unique_string",  identifier.first)
                         .registerRowMapper(OpenDcsTimeSeriesIdentifierMapper.withPrefix("tsi"))
                         .registerRowMapper(DataTypeMapper.withPrefix("dt"))
                         .registerRowMapper(OpenDcsSiteMapper.withPrefix("s"))
                         .registerRowMapper(OpenDcsSiteNameMapper.withPrefix("sn"))
                         .reduceRows(new OpenDcsTimeSeriesIdentifierReducer("tsi"))
                         .map(tsi -> tsi)
                         .findFirst());
            }
        }
        catch (OpenDcsDataException ex)
        {
            return FailableResult.failure(ex);
        }
    }

    @Override
    public FailableResult<Optional<? extends TimeSeriesIdentifier>, OpenDcsDataException> findBy(DataTransaction tx, DbKey key)
    {
        if (DbKey.isNull(key))
        {
            return FailableResult.failure(new OpenDcsDataException("Cannot lookup by null DbKey value."));
        }
        try
        {
            var handle = tx.connection(Handle.class)
                           .orElseThrow(() -> new OpenDcsDataException(SqlErrorMessages.NO_JDBI_HANDLE));
            var dbEngine = tx.getContext().getDatabaseEngine();


            try (var query = handle.createQuery(TIMESERIES_IDENTIFIER_QUERY)
                                   .define(COLLATE_CLAUSE, SqlQueries.collateClauseFor(dbEngine))
                                   .define("site_columns", OpenDcsSiteDaoImpl.SITE_COLUMNS)
                                   .define("site_name_columns", OpenDcsSiteDaoImpl.SITE_NAME_COLUMNS)
                                   .define(WHERE_CLAUSE, " where ts_id = :id ")
                                   .define(LIMIT_CLAUSE, ""))
            {
                return FailableResult.success(
                    query.bind(GenericColumns.ID.column(),  key)
                         .registerRowMapper(OpenDcsTimeSeriesIdentifierMapper.withPrefix("tsi"))
                         .registerRowMapper(DataTypeMapper.withPrefix("dt"))
                         .registerRowMapper(OpenDcsSiteMapper.withPrefix("s"))
                         .registerRowMapper(OpenDcsSiteNameMapper.withPrefix("sn"))
                         .reduceRows(new OpenDcsTimeSeriesIdentifierReducer("tsi"))
                         .map(tsi -> tsi)
                         .findFirst());
            }
        }
        catch (OpenDcsDataException ex)
        {
            return FailableResult.failure(ex);
        }
    }

    @Override
    public TimeSeriesIdentifier save(DataTransaction tx, TimeSeriesIdentifier tsId)
            throws OpenDcsDataException, BadTimeSeriesException
    {
        if (tsId instanceof CwmsTsId cwmsTsId)
        {
            return internalSaveTsId(tx, cwmsTsId);
        }
        else
        {
            throw new UnsupportedOperationException("This implementation can only save TimeSeriesIdentifiers of type " + CwmsTsId.class.getName());
        }
    }

    private TimeSeriesIdentifier internalSaveTsId(DataTransaction tx, CwmsTsId cwmsTsId)
            throws OpenDcsDataException, BadTimeSeriesException
    {
        final var existingId = getByUniqueString(tx, cwmsTsId.getUniqueString())
                                    .map(tsId -> tsId.getDataTypeId())
                                    .orElse(DbKey.NullKey);
        final var siteId = getSiteId(tx, cwmsTsId);
        final var dataTypeId = getDataTypeId(tx, cwmsTsId);

        final var intervalId = getIntervalId(tx, cwmsTsId.getInterval());
        final var durationId = getDurationId(tx, cwmsTsId.getDuration());

        final var storageUnits = getStorageUnits(tx, cwmsTsId);
        // storageUnits (from data presentation or what's provided)
        // only on save? not update?

        // acquire id


        // TODO: CompDependsNotify (however no Impl yet.)

        return cwmsTsId;
    }

    private String getStorageUnits(DataTransaction tx, CwmsTsId cwmsTsId) throws OpenDcsDataException
    {
        var dataType = cwmsTsId.getDataType();
        var setUnits = cwmsTsId.getStorageUnits();

        var settings = tx.getContext().getSettings(DecodesSettings.class).orElseThrow();
        


        return null;
    }

    private DbKey getIntervalId(DataTransaction tx, String name) throws OpenDcsDataException
    {
        return intervalDurationDao.findIntervalByName(tx, name)
                                  .map(Interval::getKey)
                                  .orElseThrow(() -> new OpenDcsDataException("No interval named " + name + " is configured in this database."));
    }

    private DbKey getDurationId(DataTransaction tx, String name) throws OpenDcsDataException
    {
        return intervalDurationDao.findDurationByName(tx, name)
                                  .map(Interval::getKey)
                                  .orElseThrow(() -> new OpenDcsDataException("No Duration named " + name + " is configured in this database."));
    }

    /**
     * Given a TimeSeriesIdentifier, either return the SiteId or throw an exception
     * @param tx
     * @param cwmsTsId
     * @return
     * @throws OpenDcsDataException
     * @throws BadTimeSeriesException
     */
    private DbKey getSiteId(DataTransaction tx, CwmsTsId cwmsTsId) throws OpenDcsDataException, BadTimeSeriesException
    {
        var site = cwmsTsId.getSite();
        DbKey ret = site != null && DbKey.isNull(site.getId()) ? site.getId() : DbKey.NullKey;
        if (DbKey.isNull(ret)) // have to lookup the site
        {
            var siteLookup = siteDao.getByAnySiteName(tx, site.getNameArray());
            ret = siteLookup.map(s -> s.getId())
                            .orElseThrow(() -> new BadTimeSeriesException(
                                "No such site '" + cwmsTsId.getSiteName() + "' for provided time series '" + cwmsTsId.getUniqueString() +"'"
                            ));
        }
        return ret;
    }


    private DbKey getDataTypeId(DataTransaction tx, CwmsTsId cwmsTsId) throws OpenDcsDataException, BadTimeSeriesException
    {
        DbKey dtId = cwmsTsId.getDataTypeId();
        if (DbKey.isNull(dtId))
        {
            // NOTE: we should really let the admin configure if this is okay or if there should be fixed data types only.
            dtId = dataTypeDao.save(tx, cwmsTsId.getDataType()).getId();
        }

        return dtId;
    }

    @Override
    public void delete(DataTransaction tx, DbKey id) throws OpenDcsDataException
    {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'delete'");
    }

    @Override
    public List<? extends TimeSeriesIdentifier> getAll(DataTransaction tx, int limit, int offset) throws OpenDcsDataException
    {
        var handle = tx.connection(Handle.class)
                        .orElseThrow(() -> new OpenDcsDataException(SqlErrorMessages.NO_JDBI_HANDLE));
        var dbEngine = tx.getContext().getDatabaseEngine();


        try (var query = handle.createQuery(TIMESERIES_IDENTIFIER_QUERY)
                                .define(COLLATE_CLAUSE, SqlQueries.collateClauseFor(dbEngine))
                                .define("site_columns", OpenDcsSiteDaoImpl.SITE_COLUMNS)
                                .define("site_name_columns", OpenDcsSiteDaoImpl.SITE_NAME_COLUMNS)
                                .define(WHERE_CLAUSE, "")
                                .define(LIMIT_CLAUSE, addLimitOffset(limit, offset)))
        {
            if (limit > -1)
            {
                query.bind(SqlKeywords.LIMIT, limit);
            }

            if (offset > -1)
            {
                query.bind(SqlKeywords.OFFSET, offset);
            }
            return 
                query.registerRowMapper(OpenDcsTimeSeriesIdentifierMapper.withPrefix("tsi"))
                        .registerRowMapper(DataTypeMapper.withPrefix("dt"))
                        .registerRowMapper(OpenDcsSiteMapper.withPrefix("s"))
                        .registerRowMapper(OpenDcsSiteNameMapper.withPrefix("sn"))
                        .reduceRows(new OpenDcsTimeSeriesIdentifierReducer("tsi"))
                        .map(tsi -> tsi)
                        .toList();
        }
            
    }

    @Override
    public TimeSeriesIdentifier makeEmptyTsId() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'makeEmptyTsId'");
    }

    @Override
    public String getStorageUnitsFor(DataTransaction tx, DataType dataType) throws OpenDcsDataException {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getStorageUnitsFor'");
    }

    @Override
    public TimeSeriesIdentifier transformUniqueString(TimeSeriesIdentifier tsidRet, DbCompParm parm) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'transformUniqueString'");
    }

    @Override
    public Optional<? extends TimeSeriesIdentifier> transformTsidByCompParm(DataTransaction tx, TimeSeriesIdentifier tsId,
            DbCompParm parm, boolean createTS, boolean fillInParm, String timeSeriesDisplayName)
            throws OpenDcsDataException, NoSuchObjectException, BadTimeSeriesException {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'transformTsidByCompParm'");
    }

    @Override
    public Optional<? extends TimeSeriesIdentifier> expandSDI(DataTransaction tx, DbCompParm parm) throws OpenDcsDataException {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'expandSDI'");
    }

}
