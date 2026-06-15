package org.opendcs.operations.timeseries;

import java.util.Optional;

import org.opendcs.database.api.DataTransaction;
import org.opendcs.database.api.OpenDcsDataException;
import org.opendcs.operations.OpenDcsOperations;

import decodes.tsdb.BadTimeSeriesException;
import decodes.tsdb.DbCompParm;
import decodes.tsdb.NoSuchObjectException;
import decodes.tsdb.TimeSeriesIdentifier;

/**
 * These are the basic time series manipulation operations required by 
 * the computation processor to handle group computations or to fill 
 * out additional meta-data for a Time Series Identifier.
 * 
 * While similar to a DAO in that some methods require a valid {@link DataTransaction}
 * instance some may not.
 */
public interface TimeSeriesOperations extends OpenDcsOperations
{
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
	Optional<TimeSeriesIdentifier> transformTsidByCompParm(DataTransaction tx,
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
	Optional<TimeSeriesIdentifier> expandSDI(DataTransaction tx, DbCompParm parm) throws OpenDcsDataException;

}
