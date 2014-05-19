/*
*  $Id$
*/
package decodes.dcpmon1;

import java.util.ArrayList;

import ilex.util.Logger;
import ilex.util.StringPair;


/**
Singleton access for the data storage for the DCP Monitor Application
*/
public class RecentDataStore
{
	/** milliseconds per day */
	public static final long MSEC_PER_DAY = (24L * 60L * 60L * 1000L);

	/** Private singleton instance */
	private static RecentDataStore _instance = null;

	/** ScrubberThread if one is active, void if not. */
	decodes.dcpmon1.ScrubberThread scrubberThread;

	private int lastCheckDay = 0;
	/**
	  @return singleton instance of RecentDataStore.
	*/
	public static RecentDataStore instance()
	{
		if (_instance == null)
			_instance = new RecentDataStore();
		return _instance;
	}

	/** private constructor, use instance() method to get. */
	private RecentDataStore()
	{
		scrubberThread = null;
	}

	/** Removes any days that are older than the max. 
	 * @throws BadDateException */
	public void scrub(int day) throws BadDateException
	{
		//Verify if we have to remove any old records from old days.
		//This should happen once a day only, when we are in a new day
		int curDay = getCurrentDay();
		int n = DcpMonitorConfig.instance().numDaysStorage;
		if (day >= curDay - n && day <= curDay+1)
		{	//this code is so that call the remove method once a day only
			if (curDay == lastCheckDay)
			{
				return;
			}
			else
			{
				Logger.instance().info("RecentDataStore scrub() New Day" +
						" remove old records ");
				lastCheckDay = curDay;
				//The scrub method removes all records from the Database, 
				//if we say store 1 day - it will remove anything older than
				//1 day
				// Delete the Database records
				if (scrubberThread != null)
					return;
				scrubberThread = new ScrubberThread(this);
				scrubberThread.start();
				
			}
		}
		else
		{
			String m = "Invalid day number " + day + ", " +
					"curday=" + curDay + ", n=" + n;
			Logger.instance().warning(m);
			throw new BadDateException(m);
		}
	}

	/** 
	  Places status of the store into the passed properties.
	  @param status Vector of StringPair objects (name/value status vars)
	*/
//	public synchronized void setStatus(ArrayList<StringPair> status)
//	{
//Logger.instance().debug3("RDS - Setting Status");
//		status.add(new StringPair("No cache", "Everything store on DB"));
//Logger.instance().debug3("RDS - Set Status Done.");
//	}

	/** 
	  Convenience method to return the current day number since 1/1/1970.
	  @return day number (0 = Jan 1, 1970)
	*/
	public static int getCurrentDay()
	{
		return msecToDay(System.currentTimeMillis());
	}

	/** 
	  Convenience method to convert msec time value to day number. 
	  @param msec the msec value
	  @return day number (0 = Jan 1, 1970)
	*/
	public static int msecToDay(long msec)
	{
		return (int)(msec / MSEC_PER_DAY);
	}

	/** 
	  Convenience method to conver msec time value to second of day.
	  @param msec the Java time value.
	  @return second-of-day
	*/
	public static int msecToSecondOfDay(long msec)
	{
		return (int)((msec % MSEC_PER_DAY)/1000L);
	}
}

class ScrubberThread extends Thread
{
	RecentDataStore parent;

	ScrubberThread(RecentDataStore parent)
	{
		this.parent = parent;
	}

	public void run()
	{
		int numDays = DcpMonitorConfig.instance().numDaysStorage;
		int today = parent.getCurrentDay();
		decodes.dcpmon1.DcpMonitor.instance().deleteDcpXmitsBefore(
													today - numDays);
		parent.scrubberThread = null;
	}

}
