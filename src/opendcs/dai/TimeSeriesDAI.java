package opendcs.dai;

import ilex.var.TimedVariable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;

import opendcs.dao.DbObjectCache;

import decodes.sql.DbKey;
import decodes.tsdb.BadTimeSeriesException;
import decodes.tsdb.CTimeSeries;
import decodes.tsdb.DbIoException;
import decodes.tsdb.NoSuchObjectException;
import decodes.tsdb.TimeSeriesIdentifier;

/**
 * Public interface for accessing time series data.
 * @author mmaloney Mike Maloney, Cove Software, LLC
 */
public interface TimeSeriesDAI
{
	/**
	 * When calling the listTimeSeries() method, don't reload the cache
	 * if it has already been done within this time.
	 */
	public static final long cacheReloadMS = 300000L;
	
	/**
	 * Each Database Implementation has some kind of unique string that
	 * can represent a time-series. This method uses that string to look
	 * up the time-series meta data.
	 * @param uniqueString unique string identifying a time series
	 * @return TimeSeriesIdentifier object for the underlying database
	 * @throws DbIoException if SQL exception occurs during operation
	 * @throws NoSuchObjectException if no matching time series exists.
	 */
	public TimeSeriesIdentifier
		getTimeSeriesIdentifier(String uniqueString)
		throws DbIoException, NoSuchObjectException;

	/**
	 * Retrieve a time series identifier by unique surrogate key.
	 * @param key the key
	 * @return the time series for this key
	 * @throws DbIoException on SQL errors
	 * @throws NoSuchObjectException if no such time series
	 */
	public TimeSeriesIdentifier
		getTimeSeriesIdentifier(DbKey key)
		throws DbIoException, NoSuchObjectException;

	/**
	 * Fills the meta data for the time series.
	 * @param ts - the time series
	 * @throws DbIoException on Database IO error.
	 */
	public void fillTimeSeriesMetadata(CTimeSeries ts)
		throws DbIoException, BadTimeSeriesException;

	/**
	 * Fills a time series with values from the given date range (inclusive).
	 * Must handle any unit conversions required between the unitsAbbr
	 * in the CTimeSeries and the storage units in the database.
	 * @param from the lower-bound of the range
	 * @param until the upper-bound of the range
	 * @return number of values added to the time series.
	 * @throws DbIoException on Database IO error.
	 * @throws BadTimeSeriesException if time series doesn't exist.
	 */
	public int fillTimeSeries( CTimeSeries ts, Date from, Date until )
		throws DbIoException, BadTimeSeriesException;

	/**
	 * Fills a time series with values from the given date range, where
	 * you control how the boundaries are included.
	 * Must handle any unit conversions required between the unitsAbbr
	 * in the CTimeSeries and the storage units in the database.
	 * @param from the lower-bound of the range
	 * @param until the upper-bound of the range
	 * @param include_lower true to include value at the lower-bound time.
	 * @param include_upper true to include value at the upper-bound time.
	 * @param overwriteExisting 
	 * @return number of values added to the time series.
	 * @throws DbIoException on Database IO error.
	 * @throws BadTimeSeriesException if time series doesn't exist.
	 */
	public int fillTimeSeries( CTimeSeries ts, Date from, Date until,
		boolean include_lower, boolean include_upper, boolean overwriteExisting)
		throws DbIoException, BadTimeSeriesException;

	/**
	 * Fills a time series with values for specific times.
	 * Must handle any unit conversions required between the unitsAbbr
	 * in the CTimeSeries and the storage units in the database.
	 * @param ts the time series
	 * @param queryTimes the set of query times.
	 * @return number of values added to the time series.
	 * @throws DbIoException on Database IO error.
	 * @throws BadTimeSeriesException if time series doesn't exist.
	 */
	public int fillTimeSeries(CTimeSeries ts, Collection<Date> queryTimes)
		throws DbIoException, BadTimeSeriesException;

	/**
	 * Retrieves the previous value to the specified time and stores it in the
	 * passed time series. That is the most recent value with a time stamp
	 * that is BEFORE the reference time.
	 * Must handle any unit conversions required between the unitsAbbr
	 * in the CTimeSeries and the storage units in the database.
	 * @param ts the time series
	 * @param refTime the referenced time.
	 * @return TimedVariable that was placed in the series, or null if none.
	 * @throws DbIoException on Database IO error.
	 * @throws BadTimeSeriesException if time series doesn't exist.
	 */
	public TimedVariable getPreviousValue(CTimeSeries ts, Date refTime)
		throws DbIoException, BadTimeSeriesException;

	/**
	 * Retrieves the next value to the specified time and stores it in the
	 * passed time series. That is the earliest value with a time stamp
	 * that is AFTER the reference time.
	 * Must handle any unit conversions required between the unitsAbbr
	 * in the CTimeSeries and the storage units in the database.
	 * @param ts the time series
	 * @param refTime the referenced time.
	 * @return TimedVariable that was placed in the series, or null if none.
	 * @throws DbIoException on Database IO error.
	 * @throws BadTimeSeriesException if time series doesn't exist.
	 */
	public TimedVariable getNextValue(CTimeSeries ts, Date refTime)
		throws DbIoException, BadTimeSeriesException;

	/**
	 * Save or delete data in the passed time series.
	 * Iterate through time series and save any values marked TO_WRITE.
	 * Also delete any values marked TO_DELETE.
	 * Must handle any unit conversions required between the unitsAbbr
	 * in the CTimeSeries and the storage units in the database.
	 * @throws BadTimeSeriesException if time series meta-data is invalid.
	 * @throws DbIoException on Database IO error.
	 */
	public void saveTimeSeries( CTimeSeries ts )
		throws DbIoException, BadTimeSeriesException;
	
	/**
	 * Delete values from the database that match the passed
	 * time series and are within the specified date/time range.
	 * @throws BadTimeSeriesException if time series doesn't exist.
	 * @throws DbIoException on Database IO error.
	 */
	public void deleteTimeSeriesRange( CTimeSeries ts, Date from, 
		Date until )
		throws DbIoException, BadTimeSeriesException;

	/**
	 * Deletes a time-series identifier and all of its data from the database.
	 * @param tsid The time series identifier
	 * @throws DbIoException on error
	 */
	public void deleteTimeSeries(TimeSeriesIdentifier tsid)
		throws DbIoException;

	/**
	 * Given a time-series identifier, make a CTimeSeries
	 * object, populated with meta-data from the database.
	 * @param tsid the time series identifier
	 * @return The CTimeSeries object
	 * @throws DbIoException on database I/O error
	 * @throws NoSuchObjectException if no such time series exists in the database.
	 */
	public CTimeSeries makeTimeSeries(TimeSeriesIdentifier tsid)
		throws DbIoException, NoSuchObjectException;

	/**
	 * @return a list of all time series defined in the database.
	 * @throws DbIoException on error
	 */
	public ArrayList<TimeSeriesIdentifier> listTimeSeries()
		throws DbIoException;

	/**
	 * @return a list of all time series defined in the database.
	 * @param forceRefresh if true, force cache clear and reload before returning list.
	 * @throws DbIoException on error
	 */
	public ArrayList<TimeSeriesIdentifier> listTimeSeries(boolean forceRefresh)
		throws DbIoException;

	/**
	 * Flush and reload the internal Time Series ID cache inside the DAO
	 * @throws DbIoException on error.
	 */
	public void reloadTsIdCache()
		throws DbIoException;

	/**
	 * Some applications need direct access to the internal cache to adjust
	 * expiry time, maintain in-memory tsids, etc.
	 * @return the internal cache.
	 */
	public DbObjectCache<TimeSeriesIdentifier> getCache();
	
	/**
	 * Validates the tsid and creates a time series in the database. 
	 * Sets the surrogate key in the passed TimeSeriesIdentifier object.
	 * @param tsid TimeSeriesIdentifier appropriate for the underlying dataabase.
	 * @return surrogate key for the new time series
	 * @throws DbIoException if SQL error in database
	 * @throws NoSuchObjectException if tsid is invalid and can't create time series.
	 */
	public DbKey createTimeSeries(TimeSeriesIdentifier tsid)
		throws DbIoException, NoSuchObjectException, BadTimeSeriesException;

	
	/**
	 * Closes any resources opened by the DAO
	 */
	public void close();

}
