package org.opendcs.database.dai;

import java.util.List;

import org.opendcs.database.api.DataTransaction;
import org.opendcs.database.api.OpenDcsDao;
import org.opendcs.database.api.OpenDcsDataException;
import org.opendcs.utils.FailableResult;

import decodes.db.DataType;
import decodes.sql.DbKey;
import decodes.tsdb.BadTimeSeriesException;
import decodes.tsdb.NoSuchObjectException;
import decodes.tsdb.TimeSeriesIdentifier;
import decodes.tsdb.TsdbException;

public interface TimeSeriesIdentifierDao extends OpenDcsDao
{
    /**
	 * Each Database Implementation has some kind of unique string that
	 * can represent a time-series. This method uses that string to look
	 * up the time-series meta data.
     *
     * @param tx
	 * @param uniqueString unique string identifying a time series
	 * @return TimeSeriesIdentifier object for the underlying database
	 * @throws DbIoException if SQL exception occurs during operation
	 * @throws NoSuchObjectException if no matching time series exists.
	 */
	TimeSeriesIdentifier getByUniqueString(DataTransaction tx, String uniqueString) throws NoSuchObjectException;

    /**
	 * Retrieve a time series identifier by unique surrogate key.
     * @param tx
	 * @param key the key
	 * @return the time series for this key
	 * @throws DbIoException on SQL errors
	 * @throws NoSuchObjectException if no such time series
	 */
	TimeSeriesIdentifier getById(DataTransaction tx, DbKey key) throws OpenDcsDataException, NoSuchObjectException;

    /**
     * Retrieve by unique string, but return failure cause instead of throwing.
     *
     * This is primarily useful in situtations where a high rate of failure is expected (e.g. expanding
     * groups) or the point of usage has it's own way of dealing with errors.
     *
     * @param tx
     * @param uniqueString
     * @return
     */
	FailableResult<TimeSeriesIdentifier,TsdbException> findTimeSeriesIdentifier(DataTransaction tx, String uniqueString);

    /**
     * As findTimeSEriesIdentifier by uniqueString. Returns fillout TimeSeriesIdentifier object if found, or the error
     * encountered.
     * @param tx
     * @param key
     * @return
     */
	FailableResult<TimeSeriesIdentifier,TsdbException> findTimeSeriesIdentifier(DataTransaction tx, DbKey key);

    /**
     * Validates and save, returning a complete instance with DbKey, the provided TimeSeriesIdentifier
     *
     * @param tx
     * @param tsId
     * @return
     * @throws OpenDcsDataException
     * @throws BadTimeSeriesException
     */
    TimeSeriesIdentifier save(DataTransaction tx, TimeSeriesIdentifier tsId) throws OpenDcsDataException, BadTimeSeriesException;

    /**
     * Delete time series by the provided DbKey. Will fail if this timeseries is used by any element.
     * @param tx
     * @param id
     * @throws OpenDcsDataException
     */
    void delete(DataTransaction tx, DbKey id) throws OpenDcsDataException;

    /**
	 * Modify an existing Time Series descriptive info.
     * 
     * Primarily used by OpenTSDB to handle storage table and other settings.
     *
     * Default implementation returns the provided tsId object.
     * 
	 * @param tsid
     * @return A new instance of tsId with new values, or the original if unmodified
	 * @throws OpenDcsDataException
	 * @throws NoSuchObjectException
	 * @throws BadTimeSeriesException
	 */
	default TimeSeriesIdentifier modifyTSID(TimeSeriesIdentifier tsId) throws NoSuchObjectException, BadTimeSeriesException
    {
        return tsId;
    }


    /**
     * Retrieve all time series.
     *
     * @param tx
     * @param limit
     * @param offset
     * @return
     */
    List<TimeSeriesIdentifier> getAll(DataTransaction tx, int limit, int offset);

    /**
     * Construct a new {@see TimeSeriesIdentifier} object appropriate for this DB.
     *
     * Prefer doing no I/O in this operation.
     *
     * @return new, empty TSID object.
     */
    TimeSeriesIdentifier makeEmptyTsId();

    /**
     * Create a new {@see TimeSeriesIdentifier} with appropriate base information.
     *
     * Default implementation retrieves the database storage units for the given {@see DataType}.
     *
     * @param tx
     * @param uniqueString
     * @return
     * @throws BadTimeSeriesException
     * @throws OpenDcsDataException
     */
    default TimeSeriesIdentifier makeTsId(DataTransaction tx, String uniqueString) throws BadTimeSeriesException, OpenDcsDataException
    {
        TimeSeriesIdentifier tsId = this.makeEmptyTsId();
        tsId.setUniqueString(uniqueString);
        tsId.setStorageUnits(this.getStorageUnitsFor(tx, tsId.getDataType()));
        return tsId;
    }

    /**
     * Retrieve appropriate storage unit for the the given DataType.
     * @param tx
     * @param dataType
     * @return
     * @throws OpenDcsDataException
     */
    String getStorageUnitsFor(DataTransaction tx, DataType dataType) throws OpenDcsDataException;

    /**
     * Retrieve appropriate storage unit for the given tsId
     * @param tx
     * @param tsId
     * @return
     * @throws OpenDcsDataException
     */
    default String getStorageUnitsFor(DataTransaction tx, TimeSeriesIdentifier tsId) throws OpenDcsDataException
    {
        return getStorageUnitsFor(tx, tsId.getDataType());
    }

}