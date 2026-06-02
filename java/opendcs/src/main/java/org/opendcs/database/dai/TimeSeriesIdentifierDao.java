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
	Optional<? extends TimeSeriesIdentifier> getByUniqueString(DataTransaction tx, String uniqueString) throws BadTimeSeriesException, OpenDcsDataException;

    /**
	 * Retrieve a time series identifier by unique surrogate key.
     * @param tx
	 * @param key the key
	 * @return the time series for this key
	 * @throws DbIoException on SQL errors
	 * @throws NoSuchObjectException if no such time series
	 */
	Optional<? extends TimeSeriesIdentifier> getById(DataTransaction tx, DbKey key) throws BadTimeSeriesException, OpenDcsDataException;

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
	FailableResult<Optional<? extends TimeSeriesIdentifier>,OpenDcsDataException> findBy(DataTransaction tx, String uniqueString);

    /**
     * As findBy by uniqueString. Returns filled out TimeSeriesIdentifier object if found, or the error
     * encountered.
     * @param tx
     * @param key
     * @return
     */
	FailableResult<Optional<? extends TimeSeriesIdentifier>,OpenDcsDataException> findBy(DataTransaction tx, DbKey key);

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
    List<? extends TimeSeriesIdentifier> getAll(DataTransaction tx, int limit, int offset);

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


    /**
     * It's debatable the the following operations should be their own "DAO?".
     * If not DAO than some similarly accessible "I need to pass the buck to an implementation" handler
     * style of interface.
     */

    /**
     * This method does the transformation of the unique string for the
     * time-series identifier.
     * IT MUST DO NO DATABASE I/O! Thus we do not provide at DataTransaction instance to for that situtation
     *
     * NOTE: <b>every</b> current implementation blatently ignores that statement. They think they don't, trusting,
     * that data is cached, but there are zero mechanisms in place to garauntee that.
     *
     * @param tsidRet the time-series identifier to transform
     * @param parm the parameter to transform by
     * @return tsidRet if not changed, otherwise a new instanced with the required changes.
     */
    TimeSeriesIdentifier transformUniqueString(TimeSeriesIdentifier tsidRet, DbCompParm parm);

    /**
	 * Take a time-series identifier and transform it by the values
	 * specified in the computation parameter. The param could change
	 * any or all of the parts of the ID.
	 * This is implemented by the database-specific subclass.
	 * Contract:
	 * Do not modify the input tsid object in any way.
	 * If modifications are made a new TimeSeriesIdentifier is returned
	 * If no modifications are made, the method must return then input
	 * object, not a copy of it.
	 * if 'create' is true, and the time-series doesn't exist, create it.
	 * Otherwise, return null if the time series does not exist in the database.
	 * If tsid is null, then create a new TimeSeriesIdentifier from the underlying
	 * database and fill in the parts from the DbCompParm.
	 * @param tsId The TimeSeriesIdentifier to transform
	 * @param parm the computation parameter
	 * @param createTS if true, create the time series if it doesn't exist.
	 * @param fillInParm if true, fill in the parm argument with the resulting
	 * concrete time-series information.
	 * @param timeSeriesDisplayName use this if createTS as the time-series display name.
	 * @return transformed identifier, or empty if after transformation, no matching
	 * time series is found in the database
	 * @throws DbIoException if database error
	 * @throws NoSuchObjectException if (createTS) and failed to create TS in database
	 * @throws BadTimeSeriesException on attempt to create new TS with invalid TSID.
	 */
	Optional<? extends TimeSeriesIdentifier> transformTsidByCompParm(DataTransaction tx,
		TimeSeriesIdentifier tsId, DbCompParm parm, boolean createTS,
		boolean fillInParm, String timeSeriesDisplayName)
		throws OpenDcsDataException, NoSuchObjectException, BadTimeSeriesException;

    /**
	 * Given a DbCompParm object containing an SDI and possibly a siteId
	 * and/or datatypeId, expand it into site and datatype objects.
	 * Store these back into  the parameter object.
	 *
	 * @param parm the object to expand
	 * @return TimeSeries Identifier is one can be identified, otherwise, null.
	 * @throws NoSuchObjectException if an SDI is present but it is invalid.
	 */
	Optional<? extends TimeSeriesIdentifier> expandSDI(DataTransaction tx, DbCompParm parm) throws OpenDcsDataException;


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