package opendcs.dai;

import java.util.List;

import decodes.sql.DbKey;
import opendcs.opentsdb.Interval;
import decodes.tsdb.DbIoException;

public interface IntervalDAI
	extends DaiBase
{
	/**
	 * Read the interval codes from the database and store them into
	 * the arrays in decodes.tsdb.IntervalCodes.java.
	 * @throws DbIoException on database error
	 */
	public void loadAllIntervals()
		throws DbIoException;

	/**
	 * Read the interval codes from the database and return them
	 * in a list.
	 * @throws DbIoException on database error
	 */
	List<Interval> getAllIntervals()
		throws DbIoException;

	/**
	 * Write an Interval record to the database
	 * @param intv The interval to write
	 * @throws DbIoException on any error
	 */
	public void writeInterval(Interval intv)
		throws DbIoException;

	/**
	 * Delete an interval from the database by its ID.
	 * @param intervalId the database key of the interval to delete
	 * @throws DbIoException on any error
	 */
	void deleteInterval(DbKey intervalId)
		throws DbIoException;

	/**
	 * @return an array of all interval codes that are valid in a time series ID.
	 */
	public String[] getValidIntervalCodes();
	
	/**
	 * @return an array of all durations that are valid in a time series ID.
	 */
	public String[] getValidDurationCodes();

	
	/**
	 * Closes any resources opened by the DAO
	 */
	public void close();

}
