/*
*  $Id$
*
*  This is open-source software written by ILEX Engineering, Inc., under
*  contract to the federal government. You are free to copy and use this
*  source code for your own purposes, except that no part of the information
*  contained in this file may be claimed to be proprietary.
*
*  Except for specific contractual terms between ILEX and the federal 
*  government, this source code is provided completely without warranty.
*  For more information contact: info@ilexeng.com
*  
*  $Log$
*  Revision 1.5  2019/07/02 13:57:59  mmaloney
*  Added findNextIdx method.
*
*  Revision 1.4  2018/11/14 15:49:12  mmaloney
*  Added deleteAll method.
*
*  Revision 1.3  2018/05/23 19:59:01  mmaloney
*  OpenTSDB Initial Release
*
*  Revision 1.2  2017/05/25 21:18:45  mmaloney
*  In DbAlgorithmExecutive, apply roundSec when searching for values in database.
*  In CTimeSeries.findWithin, the upperbound should be t+fudge/2-1.
*  See comments in code dated 20170525.
*
*  Revision 1.1.1.1  2014/05/19 15:28:59  mmaloney
*  OPENDCS 6.0 Initial Checkin
*
*  Revision 1.22  2013/03/21 18:27:39  mmaloney
*  DbKey Implementation
*
*/
package decodes.tsdb;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Date;
import java.util.Collections;
import java.util.Comparator;
import java.util.Properties;

import ilex.util.Logger;
import ilex.var.IFlags;
import ilex.var.TimedVariable;
import ilex.var.NoConversionException;
import decodes.db.Constants;
import decodes.sql.DbKey;

/**
Computational Time Series Class.
This class holds links to meta data and the actual time series data.
*/
public class CTimeSeries
{
	/** This is used to display the time series name
	 * which is site name "-" ts name */
	private String displayName;
	
	/** Site datatype ID */
	private DbKey sdi;
	
	/** The table selector */
	private String tableSelector;
	
	/** Interval code */
	private String interval;
	
	/** Model ID (modeled data only) */
	private int modelId;

	/** Model run ID (modeled data only) */
	private int modelRunId;

	/** The units that this data is currently in. */
	private String unitsAbbr;

	/** 
	 * The ID of the computation that produced this time series, or 
	 * Constants.undefinedId if this was read from the database.
	 */
	private DbKey computationId;

	/** The vector of variables. */
	private ArrayList<TimedVariable> vars;

	/** For tracking tasklist records of computations. */
	private ArrayList<Integer> taskListRecNums; // Note these ARE integers, not DbKey.
	
	/** timeseries  name .used for customised timeseries name in tables and plots **/
	private String briefDescription;
	
	
	/**  TimeSeries properties */
	private Properties props = new Properties();
	
	private static Comparator<TimedVariable> tvComparator =
		new Comparator<TimedVariable>()
		{
			public int compare(TimedVariable v1, TimedVariable v2)
			{
				return v1.getTime().compareTo(v2.getTime());
			}
		};

	/** Database-specific object containing additional meta-data. */
	private TimeSeriesIdentifier timeSeriesIdentifier;

	/** Transient storage for comp processor */
	private HashSet<DbKey> dependentCompIds;
	
	private boolean _isExpanded = false;
	
	public void setIsExpanded() { _isExpanded = true; }
	public boolean isExpanded() { return _isExpanded; }

	/**
	 * Constructor.
	 * @param sdi The site-datatype-id (or ddid for NWIS).
	 * @param interval one of the valid interval codes.
	 * @param sel The table selector ("R_" or "M_" for HDB, DBNO for NWIS).
	 * @see IntervalCodes
	 */
	public CTimeSeries( DbKey sdi, String interval, String sel )
	{
		this.sdi = sdi;
		this.interval = interval;
		this.tableSelector = sel;
		this.modelRunId = Constants.undefinedIntKey;
		this.modelId = Constants.undefinedIntKey;
		this.computationId = Constants.undefinedId;
		this.displayName = null;
		vars = new ArrayList<TimedVariable>();
		unitsAbbr = "unknown";
		dependentCompIds = null;
		taskListRecNums = new ArrayList<Integer>();
	}

	/**
	 * Construct time series for a computation parameter.
	 */
	public CTimeSeries(DbCompParm parm)
	{
		this(parm.getSiteDataTypeId(), parm.getInterval(), 
			parm.getTableSelector());
		this.modelId = parm.getModelId();
		this.displayName = null;
	}

	/**
	 * @return the site-datatype-id (SDI for USBR HDB, ddid for USGS NWIS)
	 */
	public DbKey getSDI( )
	{
		return sdi;
	}
	
	/**
	 * Sets the site-datatype-id (or ddid for NWIS).
	 * @param sdi the id
	 */
	public void setSDI( DbKey sdi )
	{
		this.sdi = sdi;
	}
	
	/**
	 * @return the table selector
	 */
	public String getTableSelector( )
	{
		return tableSelector;
	}
	
	/**
	 * Sets the table selector.
	 * @param sel table selector
	 */
	public void setTableSelector( String sel )
	{
		this.tableSelector = sel;
	}
	
	/**
	 * @return string representing the interval of data in this time series.
	 * @see IntervalCodes
	 */
	public String getInterval( )
	{
		return interval;
	}
	
	/**
	 * Sets the string representing the interval of this time series.
	 * @see IntervalCodes
	 */
	public void setInterval( String interval )
	{
		this.interval = interval;
	}
	
	/**
	 * @return the model-run-id (HDB modeled data only).
	 */
	public int getModelRunId( )
	{
		return modelRunId;
	}
	
	/**
	 * Sets the model-Run-id (HDB modeled data only).
	 * @param modelRunId the id
	 */
	public void setModelRunId( int modelRunId )
	{
		this.modelRunId = modelRunId;
	}
	
	/**
	 * @return the model-id (HDB modeled data only).
	 */
	public int getModelId( )
	{
		return modelId;
	}
	
	/**
	 * Sets the model-id (HDB modeled data only).
	 * @param modelId the id
	 */
	public void setModelId( int modelId )
	{
		this.modelId = modelId;
	}
	
	/**
	 * @return the computation-id (Constants.undefinedId if this time series
	 * is not the result of a computation.
	 */
	public DbKey getComputationId( )
	{
		return computationId;
	}
	
	/**
	 * Sets the Computation ID.
	 * @param computationId the id
	 */
	public void setComputationId( DbKey computationId )
	{
		this.computationId = computationId;
	}

	/**
	 * Return sample at the specified index or null if none.
	 * @param idx the index
	 * @return sample at the specified index or null if none.
	 */
	public synchronized TimedVariable sampleAt( int idx )
	{
		if (idx >= 0 && idx < vars.size())
			return vars.get(idx);
		return null;
	}

	/**
	 * Add a sample to the time series. 
	 * Values are kept in ascending order, so this method finds the appropriate
	 * spot for insertion.
	 * If this sample has the exact same time stamp as a value already in the
	 * series, then the old value is replaced.
	 * <p>
	 * Warning: This method assumes that the time series is sorted. If you
	 * have changed time-tags of elements in the list, call sort() before
	 * adding new samples.
	 * @param tv the timed variable to insert.
	 */
	public synchronized void addSample( TimedVariable tv )
	{
		int sz = vars.size();
		int idx = 0;
		if (sz > 0)
		{
			idx = Collections.binarySearch(vars, tv, tvComparator);

			// If same time stamp, replace old value.
			if (idx >= 0)
			{
				vars.set(idx, tv);
				return;
			}
			idx = (-idx) - 1;
		}
		vars.add(idx, tv);
	}

	/**
	 * Find the sample at the specified time within the fudge factor.
	 * @param sec Second value (offset from epoch)
	 * @param fudge number of seconds fudge factor.
	 * @return TimedVariable or null if not found.
	 */
	public TimedVariable findWithin(long sec, int fudge)
	{
//Logger.instance().info("findWithin(" + sec + ", " + fudge + ")"
//+ "date value = " + (new Date(sec*1000L)));
		for(TimedVariable tv : vars)
		{
			int sect = (int)(tv.getTime().getTime() / 1000L);
			int dt = sect - (int)sec;
//Logger.instance().info("findWithin sect=" + sect + ", dt=" + dt);
			
			// 20170525 Changed second clause below from "dt <= fudge" to "dt < fudge".
			// This will prevent a value from being considered part of two different time slices.
			// Example roundSec=60, so fudge=30. If sec=12:00:00 we want to accept 11:59:30...12:00:29
			// because 12:00:30 would be considered part of the 12:01 timeslice.
			if (-fudge <= dt && dt < fudge)
				return tv;
		}
		return null;
	}
	
	/**
	 * Convenience method which can be called with a date object
	 * @param d
	 * @param fudge
	 * @return
	 */
	public TimedVariable findWithin(Date d, int fudge)
	{
		return findWithin(d.getTime()/1000L, fudge);
	}

	/**
	 * Delete any variables in this time series with the specified time
	 * within the specified fudge factor.
	 * @return true if a value was deleted.
	 */
	public boolean deleteWithin(long sec, int fudge)
	{
		for(int i=0; i<vars.size(); i++)
		{
			TimedVariable tv = vars.get(i);
			int dt = (int)(tv.getTime().getTime() / 1000L - sec);
			if (-fudge <= dt && dt <= fudge)
			{
				vars.remove(i);
				return true;
			}
		}
		return false;
	}
	
	public void deleteAll()
	{
		vars.clear();
	}

	/**
	 * @return number of samples in the time series.
	 */
	public int size( )
	{
		return vars.size();
	}

	/**
	 * After the fact sorting, do this if you change any of the time tags
	 * in the variables in this list and need to re-order them.
	 */
	public synchronized void sort()
	{
		Collections.sort(vars, tvComparator);
	}

	/**
	 * @return a string that identifies this time series, suitable for display
	 * in log messages.
	 */
	public String getNameString()
	{
		if (timeSeriesIdentifier != null)
			return timeSeriesIdentifier.getDisplayName();
		String r = tableSelector + interval + ",sdi=" + sdi;
		if (modelId != Constants.undefinedIntKey)
			r = r + ",modelId=" + modelId;
		if (modelRunId != Constants.undefinedIntKey)
			r = r + ",modelRunId=" + modelId;
		return r;
	}

	/**
	 * Find the previous sample to the passed time. That is, the latest sample
	 * with a time before the value specified.
	 * @param sec the time.
	 * @return the previous sample to the passed time, or null if none.
	 */
	public TimedVariable findPrev(long sec)
	{
		return findPrev(new Date(sec*1000L));
	}
	
	/**
	 * Find the previous sample to the reference. That is, the latest sample
	 * with a time before the value specified.
	 * @param ref the time.
	 * @return the previous sample to the passed time, or null if none.
	 */
	public TimedVariable findPrev(Date ref)
	{
		if (vars.isEmpty())
			return null;
		TimedVariable dummy = new TimedVariable(0);
		dummy.setTime(ref);
		int idx = Collections.binarySearch(vars, dummy, tvComparator);
		if (idx < 0)
			idx = (-idx) - 1;

		// The 'insertion point' would be the next one, so subtract one.
		// If now less than zero, there are no vars prev to 'sec'.
		if (--idx < 0)
			return null;
		return vars.get(idx);
	}
	
	/**
	 * Find the next sample to the passed time. That is, the earliest sample
	 * with a time greater than the value specified.
	 * @param sec the time.
	 * @return the next sample to the passed time, or null if none.
	 */
	public TimedVariable findNext(long sec)
	{
		return findNext(new Date(sec*1000L));
	}
	
	/**
	 * Find the next sample to the reference. That is, the earliest sample
	 * with a time greater than the value specified.
	 * @param ref the time.
	 * @return the next sample to the passed time, or null if none.
	 */
	public TimedVariable findNext(Date ref)
	{
		if (vars.isEmpty())
			return null;
		int sz = vars.size();

		TimedVariable dummy = new TimedVariable(0);
		dummy.setTime(ref);
		int idx = Collections.binarySearch(vars, dummy, tvComparator);

		if (idx < 0)
			idx = (-idx) - 1;
		else // the exact time was found. Increment to the next value
			idx++;
		
		if (idx < sz)
			return vars.get(idx);
		else
			return null;
	}
	
	/**
	 * 
	 * @param ref
	 * @return index of first value after or equal to the reference date, or -1 if none.
	 */
	public int findNextIdx(Date ref)
	{
		for(int idx = 0; idx < vars.size(); idx++)
			if (!vars.get(idx).getTime().before(ref))
				return idx;
		return -1;
	}

	/**
	 * Attempt to interpolate a value at the specified time by looking at
	 * the previous & next values.
	 * @param sec the time.
	 * @return the interpolated value, or null if can't compute.
	 */
	public TimedVariable findInterp(long sec)
	{
		// If we have a value at the specified time, just return it.
		TimedVariable tv = this.findWithin(sec, 0);
		if (tv != null)
			return tv;
		
		TimedVariable prev = findPrev(sec);
		TimedVariable next = findNext(sec);
		if (prev == null || next == null)
			return null;

		long prevSec = (prev.getTime().getTime() / 1000L);
		long nextSec = (next.getTime().getTime() / 1000L);
		long timeRange = nextSec - prevSec;
		if (timeRange == 0)
			return null;

		double pos = (double)(sec - prevSec) / (double)timeRange;

		double prevVal = 0.0;
		double nextVal = 0.0;
		try
		{
			prevVal = prev.getDoubleValue();
			nextVal = next.getDoubleValue();
		}
		catch(ilex.var.NoConversionException ex)
		{
			Logger.instance().warning("Attempt to interpolate a non-number.");
			return null;
		}
		double val = prevVal + (nextVal - prevVal) * pos;
		TimedVariable ret = new TimedVariable(val);
		ret.setTime(new Date(sec * 1000L));
		return ret;
	}

	/**
	 * Find the closest sample to the passed time.
	 * @param sec the time.
	 * @return the closest sample to the passed time, or null if empty.
	 */
	public TimedVariable findClosest(long sec)
	{
		TimedVariable prev = findPrev(sec);
		TimedVariable next = findNext(sec);
		if (prev == null || next == null)
			return null;

		long prevSec = (prev.getTime().getTime() / 1000L);
		long nextSec = (next.getTime().getTime() / 1000L);
		return new TimedVariable(
			(sec - prevSec) <= (nextSec - sec) ? prev : next, 
			new Date(sec * 1000L));
	}

	/**
	 * Returns true if this time series contains data marked DB_ADDED or
	 * DB_DELETED. This method is used by the resolver to determine if this
	 * time series should be considered a trigger for computations.
	 *
	 * @return true if this time series contains data marked DB_ADDED or
	 * DB_DELETED.
	 */
	public boolean hasAddedOrDeleted()
	{
		for(TimedVariable tv : vars)
			if ((tv.getFlags() & (VarFlags.DB_ADDED|VarFlags.DB_DELETED)) != 0)
				return true;
		return false;
	}

	/**
	 * @return the units that this time series is currently in.
	 */
	public String getUnitsAbbr() { return unitsAbbr; }

	/** Sets the units that this time series is in. */
	public void setUnitsAbbr(String abbr) { unitsAbbr = abbr; }

	public String getDisplayName()
	{
		return displayName != null ? displayName : ("sdi=" + sdi);
	}

	public void setDisplayName(String displayName)
	{
		this.displayName = displayName;
	}

	/**
	 * @return the minimum value in the series, or the
	 * specified default if the series is empty.
	 */
	public double getMinValue(double defaultMin)
	{
		return getMinValue(defaultMin, null, null);
	}

	/**
	 * Get minimum value within specified time range.
	 * @param start start of time-range or null to consider earliest data.
	 * @param end end of time-range or null to consider latest data.
	 * @return  the minimum value in the specified time range, or the default 
	 * value if no values in the range.
	 */
	public double getMinValue(double defaultMin, Date start, Date end)
	{
		double min = Double.MAX_VALUE;
		for(TimedVariable tv : vars)
		{
			Date t = tv.getTime();
			if (start != null && start.compareTo(t) > 0)
				continue;
			if (end != null && end.compareTo(t) < 0)
				continue;
			int fl = tv.getFlags();
			if ((fl & IFlags.IS_MISSING) == 0)
			{
				try 
				{
					double d = tv.getDoubleValue();
					if (d < min)
						min = d;
				}
				catch(NoConversionException ex) {}
			}
		}
		return min == Double.MAX_VALUE ? defaultMin : min;
	}

	/** 
	 * @return  the maximum value in the entire time series, or the default 
	 * value if the time series is empty.
	 */
	public double getMaxValue(double defaultMax)
	{
		return getMaxValue(defaultMax, null, null);
	}

	/** 
	 * Get maximum value within specified time range.
	 * @param start start of time-range or null to consider earliest data.
	 * @param end end of time-range or null to consider latest data.
	 * @return  the maximum value in the specified time range, or the default 
	 * value if no values in the range.
	 */
	public double getMaxValue(double defaultMax, Date start, Date end)
	{
		double max = Double.NEGATIVE_INFINITY;
		for(TimedVariable tv : vars)
		{
			Date t = tv.getTime();
			if (start != null && start.compareTo(t) > 0)
				continue;
			if (end != null && end.compareTo(t) < 0)
				continue;
			int fl = tv.getFlags();
			if ((fl & IFlags.IS_MISSING) == 0)
			{
				try 
				{
					double d = tv.getDoubleValue();
					if (d > max)
						max = d;
				}
				catch(NoConversionException ex) {}
			}
		}
		return max == Double.NEGATIVE_INFINITY ? defaultMax : max;
	}
	

	/**
	 * Each database implementation can store additional meta-data in an
	 * opaque object inside the time-series.
	 */
	public void setTimeSeriesIdentifier(TimeSeriesIdentifier timeSeriesIdentifier)
	{
		this.timeSeriesIdentifier = timeSeriesIdentifier;
		if (unitsAbbr == null || unitsAbbr.trim().length() == 0 || unitsAbbr.trim().equalsIgnoreCase("unknown"))
			unitsAbbr = this.timeSeriesIdentifier.getStorageUnits();
	}

	/**
	 * Each database implementation can store additional meta-data in an
	 * opaque object inside the time-series.
	 */
	public TimeSeriesIdentifier getTimeSeriesIdentifier()
	{
		return timeSeriesIdentifier;
	}

	/** 
	 * @return list of IDs to computations that use this TS as input,
	 *  or null if none.
	 **/
	public HashSet<DbKey> getDependentCompIds()
	{
		if (dependentCompIds == null)
			dependentCompIds = new HashSet<DbKey>();
		return dependentCompIds;
	}

	/** Adds a computation ID to this TS's list. */
	public void addDependentCompId(DbKey compId)
	{
		if (dependentCompIds == null)
			dependentCompIds = new HashSet<DbKey>();
		dependentCompIds.add(compId);
	}

	/** @return the list of tasklist record nums used to populate this ts. */
	public ArrayList<Integer> getTaskListRecNums() { return taskListRecNums; }

	/** Adds a recnum to the list of tasklist record nums. */
	public void addTaskListRecNum(int recnum) { taskListRecNums.add(recnum); }

	/** Clears the list of tasklist record nums. */
	public void clearTaskListRecNums() { taskListRecNums.clear(); }
	
	/**
	 * @return the tsName
	 */
	public String getBriefDescription()
	{
		return briefDescription;
	}

	/**
	 * @param tsName
	 *            the tsName to set
	 */
	public void setBriefDescription(String briefDescription)
	{
		this.briefDescription = briefDescription;
	}

	/**
	 * @return the props
	 */
	public Properties getProperties() {
		return props;
	}
	
	/** 
	 * Adds a property to this time series.
	 * @param name the property name.
	 * @param value the property value.
	 */
	public void setProperty(String name, String value)
	{
		props.setProperty(name, value);
	}
}
