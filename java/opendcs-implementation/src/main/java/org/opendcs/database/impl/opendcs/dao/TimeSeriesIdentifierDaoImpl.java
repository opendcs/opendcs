package org.opendcs.database.impl.opendcs.dao;

import static org.opendcs.utils.sql.SqlQueries.COLLATE_CLAUSE;
import static org.opendcs.utils.sql.SqlQueries.LIMIT_CLAUSE;
import static org.opendcs.utils.sql.SqlQueries.WHERE_CLAUSE;
import static org.opendcs.utils.sql.SqlQueries.addLimitOffset;

import java.util.List;
import java.util.Optional;

import org.jdbi.v3.core.Handle;
import org.jdbi.v3.stringtemplate4.StringTemplateSqlLocator;
import org.opendcs.annotations.api.InjectDao;
import org.opendcs.annotations.api.InjectOperations;
import org.opendcs.database.api.DataTransaction;
import org.opendcs.database.api.OpenDcsDataException;
import org.opendcs.database.dai.CompDependsNotifyDao;
import org.opendcs.database.dai.DataTypeDao;
import org.opendcs.database.dai.IntervalDurationDao;
import org.opendcs.database.dai.PresentationGroupDao;
import org.opendcs.database.dai.SiteDao;
import org.opendcs.database.dai.TimeSeriesIdentifierDao;
import org.opendcs.database.impl.opendcs.jdbi.logging.DetailSqlLogger;
import org.opendcs.database.impl.opendcs.jdbi.mapper.timeseries.OpenDcsTimeSeriesIdentifierMapper;
import org.opendcs.database.impl.opendcs.jdbi.mapper.timeseries.OpenDcsTimeSeriesIdentifierMapper.Columns;
import org.opendcs.database.impl.opendcs.jdbi.mapper.timeseries.OpenDcsTimeSeriesIdentifierReducer;
import org.opendcs.database.model.mappers.datatype.DataTypeMapper;
import org.opendcs.database.model.mappers.sites.OpenDcsSiteMapper;
import org.opendcs.database.model.mappers.sites.OpenDcsSiteNameMapper;
import org.opendcs.operations.timeseries.TimeSeriesOperations;
import org.opendcs.util.Result;
import org.opendcs.utils.logging.OpenDcsLoggerFactory;
import org.opendcs.utils.sql.GenericColumns;
import org.opendcs.utils.sql.SqlErrorMessages;
import org.opendcs.utils.sql.SqlKeywords;
import org.opendcs.utils.sql.SqlQueries;
import org.openide.util.lookup.ServiceProvider;
import org.openide.util.lookup.ServiceProviders;
import org.slf4j.Logger;
import org.stringtemplate.v4.STGroup;

import decodes.cwms.CwmsTsId;
import decodes.db.DataType;
import decodes.db.DatabaseException;
import decodes.sql.DbKey;
import decodes.sql.KeyGenerator;
import decodes.tsdb.BadTimeSeriesException;
import decodes.tsdb.CpDependsNotify;
import decodes.tsdb.TimeSeriesIdentifier;
import opendcs.opentsdb.Interval;
import opendcs.opentsdb.OpenDcsDbSettings;
import opendcs.opentsdb.OpenTimeSeriesDAO;
import opendcs.opentsdb.StorageTableSpec;

/**
 * NOTE: this is intentionally doing no caching at this time. Current focus is simply "correctness."
 */
@ServiceProviders({
    @ServiceProvider(service = TimeSeriesIdentifierDao.class, path = "dao/OpenDCS-Postgres"),
    @ServiceProvider(service = TimeSeriesIdentifierDao.class, path = "dao/OPENTSDB")
})
@SuppressWarnings("java:S2143")
public class TimeSeriesIdentifierDaoImpl implements TimeSeriesIdentifierDao
{
    private static final Logger log = OpenDcsLoggerFactory.getLogger();

    private static final String SELECT = "select";

    @InjectDao
    SiteDao siteDao;

    @InjectDao
    DataTypeDao dataTypeDao;

    @InjectDao
    IntervalDurationDao intervalDurationDao;

    @InjectDao
    PresentationGroupDao presentationGroupDao;

    @InjectDao
    CompDependsNotifyDao compDependsDao;

    @InjectOperations
    TimeSeriesOperations timeSeriesOps;

    private final STGroup queries;

    private final Mappers mappers = new Mappers(
                        OpenDcsTimeSeriesIdentifierMapper.withPrefix("tsi"),
                        DataTypeMapper.withPrefix("dt"),
                        OpenDcsSiteMapper.withPrefix("s"),
                        OpenDcsSiteNameMapper.withPrefix("sn"));

    public TimeSeriesIdentifierDaoImpl()
    {
        STGroup.verbose = true;
        queries = StringTemplateSqlLocator.findStringTemplateGroup(TimeSeriesIdentifierDaoImpl.class);
    }

    @Override
    public Result<Optional<TimeSeriesIdentifier>, OpenDcsDataException> findBy(DataTransaction tx,
            String uniqueString)
    {
        final var identifier = extractDisplayName(uniqueString);
        return findBy(tx, " where unique_string = :unique_string", "unique_string", identifier.first);
    }
    
    public Result<Optional<TimeSeriesIdentifier>, OpenDcsDataException> findBy(DataTransaction tx, String whereClause, String whereBind, Object bindVal)
    {
        try
        {
            var handle = tx.connection(Handle.class)
                           .orElseThrow(() -> new OpenDcsDataException(SqlErrorMessages.NO_JDBI_HANDLE));
            var dbEngine = tx.getContext().getDatabaseEngine();
            var selectTemplate = queries.getInstanceOf(SELECT);

            var selectQuery = selectTemplate.add(LIMIT_CLAUSE, "")
                                            .add(WHERE_CLAUSE, whereClause)
                                            .add(COLLATE_CLAUSE, SqlQueries.collateClauseFor(dbEngine))
                                            .add(OpenDcsSiteDaoImpl.SITE_COLUMN_KEY, mappers.siteMapper.columnsForSelect())
                                            .add(OpenDcsSiteDaoImpl.SITE_NAME_COLUMNS_KEY, mappers.siteNameMapper.columnsForSelect())
                                            ;

            try (var query = handle.createQuery(selectQuery.render()))
            {
                query.setSqlLogger(new DetailSqlLogger(log));
                return Result.success(
                    query.bind(whereBind,  bindVal)
                         .registerRowMapper(mappers.tsiMapper)
                         .registerRowMapper(mappers.dtMapper)
                         .registerRowMapper(mappers.siteMapper)
                         .registerRowMapper(mappers.siteNameMapper)
                         .reduceRows(new OpenDcsTimeSeriesIdentifierReducer(mappers))
                         .map(tsi -> tsi)
                         .findFirst());
            }
        }
        catch (OpenDcsDataException ex)
        {
            return Result.failure(ex);
        }
    }

    @Override
    public Result<Optional<TimeSeriesIdentifier>, OpenDcsDataException> findBy(DataTransaction tx, DbKey key)
    {
        if (DbKey.isNull(key))
        {
            return Result.failure(new OpenDcsDataException("Cannot lookup by null DbKey value."));
        }
        return findBy(tx, " where id = :id", "id", key);
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
        var existing = (CwmsTsId)getByUniqueString(tx, cwmsTsId.getUniqueString())
                                    .orElse(cwmsTsId);
        final var existingId = existing.getKey();
        final var siteId = getSiteId(tx, cwmsTsId);
        final var dataTypeId = getDataTypeId(tx, cwmsTsId);

        final var intervalId = getIntervalId(tx, cwmsTsId.getInterval());
        final var durationId = getDurationId(tx, cwmsTsId.getDuration());

        final var storageUnits = getStorageUnits(tx, cwmsTsId);


        var handle = tx.connection(Handle.class)
                       .orElseThrow(() -> new OpenDcsDataException(SqlErrorMessages.NO_JDBI_HANDLE));
        var ctx = tx.getContext();
        var keyGen = ctx.getGenerator(KeyGenerator.class)
                        .orElseThrow(() -> new OpenDcsDataException("No key generator configured."));

        var toSave = cwmsTsId;
        if (existing != cwmsTsId) // some field should not be updated
        {
            validateChanges(existing, cwmsTsId);
        }
        else
        {
            toSave = allocateTable(handle, (CwmsTsId)cwmsTsId.copyNoKey());
        }


        final String MERGE_SQL = """
            merge into ts_spec
            using (select
                :id id, :active_flag active_flag, :allow_dst_offset_variation allow_dst_offset, :site_id site_id,
                :datatype_id datatype_id, :statistics_code statistics_code, :interval_id interval_id, :duration_id duration_id,
                :version version, :storage_units storage_units, :storage_table storage_table, :storage_type storage_type,
                :modify_time modify_time, :description description, :utc_offset utc_offset,
                :offset_error_action offset_error_action
            <dual>) input
            on (ts_spec.ts_id = input.id)
            when matched then
                update set
                    ts_id = input.id, active_flag = input.active_flag, allow_dst_offset_variation = input.allow_dst_offset, site_id = input.site_id,
                    datatype_id = input.datatype_id, statistics_code = input.statistics_code, interval_id = input.interval_id, duration_id = input.duration_id,
                    ts_version = input.version, storage_units = input.storage_units, storage_table = input.storage_table, storage_type = input.storage_type,
                    modify_time = input.modify_time, description = input.description, utc_offset = input.utc_offset,
                    offset_error_action = input.offset_error_action
            when not matched then
                insert(
                    ts_id, active_flag, allow_dst_offset_variation, site_id, datatype_id, statistics_code, interval_id, duration_id,
                    ts_version, storage_units, storage_table, storage_type, modify_time, description, utc_offset, offset_error_action)
                values(
                    input.id, input.active_flag, input.allow_dst_offset, input.site_id, input.datatype_id, input.statistics_code, input.interval_id,
                    input.duration_id, input.version, input.storage_units, input.storage_table, input.storage_type, input.modify_time,
                    input.description, input.utc_offset, input.offset_error_action
                )
                """;

        try (var merge = handle.createUpdate(MERGE_SQL))
        {
            DbKey id = toSave.getKey();

            if (!DbKey.isNull(existingId))
            {
                // If there's an existing app with this name, we'll just assume the provided id, if any, was in error
                id = existingId;
                log.trace("""
                    Using ID from existing Time Series Identifier, id={}, that was found. Provided ID was {}.
                    """,
                    id, existingId);
            }
            final var bindKey = !DbKey.isNull(id) ? id : keyGen.getKey("ts_spec", handle.getConnection());

            merge.bind(Columns.ID.column(), bindKey)
                 .bind(Columns.ACTIVE_FLAG.column(), toSave.isActive())
                 .bind(Columns.ALLOW_DST_OFFSET_VARIATION.column(), toSave.isAllowDstOffsetVariation())
                 .bind(Columns.INTERVAL_ID.column(), intervalId)
                 .bind(Columns.DURATION_ID.column(), durationId)
                 .bind(Columns.SITE_ID.column(), siteId)
                 .bind(Columns.MODIFY_TIME.column(), System.currentTimeMillis())
                 .bind(Columns.UTC_OFFSET.column(), toSave.getUtcOffset())
                 .bind(Columns.STATISTICS_CODE.column(), toSave.getStatisticsCode())
                 .bind(Columns.DESCRIPTION.column(), toSave.getDescription())
                 .bind(Columns.STORAGE_UNITS.column(), storageUnits)
                 .bind(Columns.STORAGE_TABLE.column(), toSave.getStorageTable())
                 .bind(Columns.STORAGE_TYPE.column(), toSave.getStorageType())
                 .bind(Columns.DATA_TYPE_ID.column(), dataTypeId)
                 .bind(Columns.VERSION.column(), toSave.getVersion())
                 .bind(Columns.OFFSET_ERROR_ACTION.column(), toSave.getOffsetErrorAction().name())
                 .execute();

            final var ret = getById(tx, bindKey)
                            .orElseThrow(() -> new OpenDcsDataException("Unable to retrieve Time Series Identifier we just saved."));
            saveNotify(tx, bindKey);
            return ret;
        }
        catch (DatabaseException ex)
        {
            throw new OpenDcsDataException("Unable to get key for new time series identifier.", ex);
        }
    }

    private CwmsTsId allocateTable(Handle handle, CwmsTsId inputTs) throws OpenDcsDataException
    {
        var ret = (CwmsTsId)inputTs.copyNoKey();
        try (var selectTable = handle.createQuery("""
            select table_num, storage_type, num_ts_present, est_annual_values
            from storage_table_list
            where storage_type = :storage_type
            and est_annual_values = (select min(est_annual_values) from storage_table_list where storage_type = :storage_type)
            order by table_num
            for update
        """);
            var updateTable = handle.createUpdate("""
                update storage_table_list set
                    num_ts_present = :num_ts_present,
                    est_annual_values = :est_annual_values
                    where storage_Type = :storage_type
                    and table_num = :table_num
                    """))
        {
            var storageTable  =
                selectTable.bind("storage_type", inputTs.getStorageType())
                           .map(rv ->
                            {
                                var spec = new StorageTableSpec(inputTs.getStorageType());
                                spec.setNumTsPresent(rv.getColumn("num_ts_present", Integer.class));
                                spec.setEstAnnualValues(rv.getColumn("est_annual_values", Integer.class));
                                spec.setTableNum(rv.getColumn("table_num", Integer.class));
                                return spec;
                            }
                           )
                           .findOne()
                           .orElseThrow(() -> new OpenDcsDataException("No storage tables available!"));

            ret.setStorageTable(storageTable.getTableNum());

            updateTable.bind("num_ts_present", storageTable.getNumTsPresent() + 1)
                       .bind("est_annual_values", OpenTimeSeriesDAO.interval2estAnnualValues(inputTs.getIntervalOb()))
                       .bind("storage_type", storageTable.getStorageType())
                       .bind("table_num", storageTable.getTableNum())
                       .execute();
        }


        return ret;
    }

    /**
     * Validate that only fields that can change are changed.
     * @param existing
     * @param input
     * @throws OpenDcsDataException
     */
    private void validateChanges(CwmsTsId existing, CwmsTsId input) throws OpenDcsDataException
    {
        if (!existing.getInterval().equalsIgnoreCase(input.getInterval()))
        {
            throw new OpenDcsDataException("Cannot change interval of data.");
        }
        if (!existing.getDuration().equalsIgnoreCase(input.getDuration()))
        {
            throw new OpenDcsDataException("Cannot change duration of data.");
        }
        if (!existing.getStatisticsCode().equalsIgnoreCase(input.getStatisticsCode()))
        {
            throw new OpenDcsDataException("Cannot change stastics code of data.");
        }
        if (existing.getStorageTable() != input.getStorageTable())
        {
            throw new OpenDcsDataException("Cannot alter storage table of data using this method.");
        }
        if (existing.getStorageType() != input.getStorageType())
        {
            throw new OpenDcsDataException("Cannot alter storage type of data.");
        }
        if (existing.getUtcOffset() != input.getUtcOffset())
        {
            throw new OpenDcsDataException("Cannot alter UTC offset using this method.");
        }
        if (!existing.getStorageUnits().equalsIgnoreCase(input.getStorageUnits()))
        {
            throw new OpenDcsDataException("Cannot alter Storage units using this method.");
        }
    }

    private void saveNotify(DataTransaction tx, DbKey id)
    {
        try
        {
            CpDependsNotify cdn = new CpDependsNotify();
            cdn.setKey(id);
            cdn.setEventType(CpDependsNotify.TS_MODIFIED);
            compDependsDao.saveRecord(tx, cdn);
        }
        catch (OpenDcsDataException ex)
        {
            log.atWarn().setCause(ex).log("Unable to save notification record", ex);
        }
    }

    private String getStorageUnits(DataTransaction tx, CwmsTsId cwmsTsId) throws OpenDcsDataException
    {

        var dataType = cwmsTsId.getDataType();
        var setUnits = cwmsTsId.getStorageUnits();

        return getStorageUnitsFor(tx, dataType).orElse(setUnits);
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


    private DbKey getDataTypeId(DataTransaction tx, CwmsTsId cwmsTsId) throws OpenDcsDataException
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
        var handle = tx.connection(Handle.class)
                       .orElseThrow(() -> new OpenDcsDataException(SqlErrorMessages.NO_JDBI_HANDLE));
        try (var deleteTs = handle.createUpdate("delete from ts_spec where ts_id = :id"))
        {
            deleteTs.bind(Columns.ID.column(), id).execute();
        }
    }

    @Override
    public List<TimeSeriesIdentifier> getAll(DataTransaction tx, int limit, int offset) throws OpenDcsDataException
    {
        var handle = tx.connection(Handle.class)
                        .orElseThrow(() -> new OpenDcsDataException(SqlErrorMessages.NO_JDBI_HANDLE));
        var dbEngine = tx.getContext().getDatabaseEngine();
        var selectTemplate = queries.getInstanceOf(SELECT);
        var selectQuery = selectTemplate.add(LIMIT_CLAUSE, addLimitOffset(limit, offset))
                                            .add(WHERE_CLAUSE, "")
                                            .add(COLLATE_CLAUSE, SqlQueries.collateClauseFor(dbEngine))
                                            .add(OpenDcsSiteDaoImpl.SITE_COLUMN_KEY, OpenDcsSiteDaoImpl.SITE_COLUMNS)
                                            .add(OpenDcsSiteDaoImpl.SITE_NAME_COLUMNS_KEY, OpenDcsSiteDaoImpl.SITE_NAME_COLUMNS)
                                            ;

        try (var query = handle.createQuery(selectQuery.render()))
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
                query.registerRowMapper(mappers.tsiMapper)
                     .registerRowMapper(mappers.dtMapper)
                     .registerRowMapper(mappers.siteMapper)
                     .registerRowMapper(mappers.siteNameMapper)
                     .reduceRows(new OpenDcsTimeSeriesIdentifierReducer(mappers))
                     .map(tsi -> tsi)
                     .toList();
        }

    }

    @Override
    public TimeSeriesIdentifier makeEmptyTsId()
    {
        return new CwmsTsId();
    }

    @Override
    public Optional<String> getStorageUnitsFor(DataTransaction tx, DataType dataType) throws OpenDcsDataException
    {
        var dbSettings = tx.getContext()
                           .getSettings(OpenDcsDbSettings.class)
                           .orElseThrow(() -> new OpenDcsDataException("Required settings instance is not available."));
        var pgGroupName = dbSettings.storagePresentationGroup;

        var presentationGroup = presentationGroupDao.getByName(tx, pgGroupName)
                                                    .orElseThrow(() -> new OpenDcsDataException("No presentation grouped named '" + pgGroupName + "' is available in this database."));

        var presentation = presentationGroup.findDataPresentation(dataType);
        if (presentation != null)
        {
            return Optional.of(presentation.getUnitsAbbr());
        }
        else
        {
            return Optional.empty();
        }
    }

    public record Mappers(OpenDcsTimeSeriesIdentifierMapper tsiMapper, DataTypeMapper dtMapper,
                          OpenDcsSiteMapper siteMapper, OpenDcsSiteNameMapper siteNameMapper)
    {
    }
}
