package opendcs.dai;

import opendcs.opentsdb.Interval;
import decodes.tsdb.DbIoException;

public interface IntervalDAI
{
	/**
	 * Read the interval codes from the database and store them into
	 * the arrays in decodes.tsdb.IntervalCodes.java.
	 * @throws DbIoException on database error
	 */
	public void loadAllIntervals()
		throws DbIoException;

	/**
	 * Write an Interval record to the database
	 * @param intv The interval to write
	 * @throws DbIoException on any error
	 */
	public void writeInterval(Interval intv)
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
