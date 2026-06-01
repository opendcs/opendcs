package org.opendcs.database.impl.opendcs.dao;

import static org.opendcs.utils.sql.SqlQueries.COLLATE_CLAUSE;
import static org.opendcs.utils.sql.SqlQueries.WHERE_CLAUSE;

import java.util.List;
import java.util.Optional;

import org.jdbi.v3.core.Handle;
import org.opendcs.database.api.DataTransaction;
import org.opendcs.database.api.OpenDcsDataException;
import org.opendcs.database.dai.TimeSeriesIdentifierDao;
import org.opendcs.database.impl.opendcs.jdbi.mapper.timeseries.OpenDcsTimeSeriesIdentifierMapper;
import org.opendcs.utils.FailableResult;
import org.opendcs.utils.sql.SqlErrorMessages;
import org.opendcs.utils.sql.SqlQueries;
import org.openide.util.lookup.ServiceProvider;
import org.openide.util.lookup.ServiceProviders;

import decodes.db.DataType;
import decodes.sql.DbKey;
import decodes.tsdb.BadTimeSeriesException;
import decodes.tsdb.DbCompParm;
import decodes.tsdb.NoSuchObjectException;
import decodes.tsdb.TimeSeriesIdentifier;

/**
 * NOTE: this is intentionally doing no caching at this time. Current focus is simply "correctness."
 */
@ServiceProviders({
    @ServiceProvider(service = TimeSeriesIdentifierDao.class, path = "dao/OpenDCS-Postgres"),
    @ServiceProvider(service = TimeSeriesIdentifierDao.class, path = "dao/OPENTSDB")
})
public class TimeSeriesIdentifierDaoImpl implements TimeSeriesIdentifierDao
{

    private static final String TIMESERIES_IDENTIFIER_QUERY = """
            select 
                id,site_name, data_type_standard, data_type_code,
                interval, duration, ts_version,  unique_string,
                active_flag, storage_units, storage_table, storage_type,
                modify_time, description, utc_offset, allow_dst_offset_variation,
                offset_error_action, datatype_id, site_id, interval_id, duration_id
            
            from time_series_identifier
            
            <where>
            order by unique_string <collate> asc
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
            var dbEngine = tx.getContext().getDatabase();


            try (var query = handle.createQuery(TIMESERIES_IDENTIFIER_QUERY)
                                   .define(COLLATE_CLAUSE, SqlQueries.collateClauseFor(dbEngine))
                                   .define(WHERE_CLAUSE, " where unique_string = :unique_string "))
            {
                return FailableResult.success(
                    query.bind("unique_string",  identifier.first)
                         .registerRowMapper(OpenDcsTimeSeriesIdentifierMapper.withPrefix(""))
                         .mapTo(TimeSeriesIdentifier.class)
                         .findFirst());
            }
        }
        catch (OpenDcsDataException ex)
        {
            return FailableResult.failure(ex);
        }
    }

    @Override
    public FailableResult<Optional<? extends TimeSeriesIdentifier>, OpenDcsDataException> findBy(DataTransaction tx, DbKey key) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'findBy'");
    }

    @Override
    public TimeSeriesIdentifier save(DataTransaction tx, TimeSeriesIdentifier tsId)
            throws OpenDcsDataException, BadTimeSeriesException {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'save'");
    }

    @Override
    public void delete(DataTransaction tx, DbKey id) throws OpenDcsDataException {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'delete'");
    }

    @Override
    public List<? extends TimeSeriesIdentifier> getAll(DataTransaction tx, int limit, int offset) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getAll'");
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
