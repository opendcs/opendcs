package org.opendcs.database.dai;

import java.util.List;
import java.util.Optional;

import ilex.util.Pair;

import org.opendcs.database.api.DataTransaction;
import org.opendcs.database.api.OpenDcsDao;
import org.opendcs.database.api.OpenDcsDataException;
import org.opendcs.utils.FailableResult;

import decodes.db.DataType;
import decodes.sql.DbKey;
import decodes.tsdb.BadTimeSeriesException;
import decodes.tsdb.DbCompParm;
import decodes.tsdb.NoSuchObjectException;
import decodes.tsdb.TimeSeriesIdentifier;

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
	Optional<TimeSeriesIdentifier> getByUniqueString(DataTransaction tx, String uniqueString) throws BadTimeSeriesException, OpenDcsDataException;

    /**
	 * Retrieve a time series identifier by unique surrogate key.
     * @param tx
	 * @param key the key
	 * @return the time series for this key
	 * @throws DbIoException on SQL errors
	 * @throws NoSuchObjectException if no such time series
	 */
	Optional<TimeSeriesIdentifier> getById(DataTransaction tx, DbKey key) throws BadTimeSeriesException, OpenDcsDataException;

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
	FailableResult<Optional<TimeSeriesIdentifier>,OpenDcsDataException> findBy(DataTransaction tx, String uniqueString);

    /**
     * As findBy by uniqueString. Returns filled out TimeSeriesIdentifier object if found, or the error
     * encountered.
     * @param tx
     * @param key
     * @return
     */
	FailableResult<Optional<TimeSeriesIdentifier>,OpenDcsDataException> findBy(DataTransaction tx, DbKey key);

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
     * Retrieve all time series, given the limit and offset.
     *
     * @param tx
     * @param limit
     * @param offset
     * @return
     */
    List<TimeSeriesIdentifier> getAll(DataTransaction tx, int limit, int offset) throws OpenDcsDataException;

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
        tsId.setStorageUnits(this.getStorageUnitsFor(tx, tsId.getDataType()).orElse(null));
        return tsId;
    }

    /**
     * Retrieve appropriate storage unit for the the given DataType.
     * @param tx
     * @param dataType
     * @return
     * @throws OpenDcsDataException
     */
    Optional<String> getStorageUnitsFor(DataTransaction tx, DataType dataType) throws OpenDcsDataException;

    /**
     * Retrieve appropriate storage unit for the given tsId
     * @param tx
     * @param tsId
     * @return
     * @throws OpenDcsDataException
     */
    default Optional<String> getStorageUnitsFor(DataTransaction tx, TimeSeriesIdentifier tsId) throws OpenDcsDataException
    {
        return getStorageUnitsFor(tx, tsId.getDataType());
    }

    /**
     * Extract the display name. Default implementation assume display name is contained within parenthesis.
     * e.g. <pre>Alder Springs.Precip.Total.1Hour.1Hour.comp (Hourly Precip for Alder Springs)</pre>
     *
     * Implementations may override as desired.
     * @param uniqueString time series identifier string to manipulate
     * @return pair of TimeSeriesIdentifier without display name and extracted displayName, if any.
     */
    default Pair<String,Optional<String>> extractDisplayName(String uniqueString)
    {
        String displayName = null;
        String retUniqueString = uniqueString;
        int paren = uniqueString.lastIndexOf('(');
		if (paren > 0 && uniqueString.trim().endsWith(")"))
		{
			displayName = uniqueString.substring(paren+1);
			retUniqueString = uniqueString.substring(0,  paren);
			int endParen = displayName.indexOf(')');
			if (endParen > 0)
			{
				displayName = displayName.substring(0,  endParen);
			}
		}
        return Pair.of(retUniqueString,Optional.ofNullable(displayName));
    }
}
