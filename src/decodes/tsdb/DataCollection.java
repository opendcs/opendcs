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
*  Revision 1.11  2013/03/21 18:27:39  mmaloney
*  DbKey Implementation
*
*/
package decodes.tsdb;

import java.util.ArrayList;
import java.util.List;

import ilex.util.TextUtil;
import decodes.db.Constants;
import decodes.sql.DbKey;

/**
This class holds a set of time series.
*/
public class DataCollection
{
	/** The time series */
	private ArrayList<CTimeSeries> tseries;

	/** Handle storing tasklist Record Ranges for computation processor. */
	private RecordRangeHandle rrHandle;

	/** Constructor -- builds an empty collection with a null handle. */
	public DataCollection()
	{
		tseries = new ArrayList<CTimeSeries>();
		rrHandle = null;
	}

	/**
	 * Returns the time series for the specified site-datatype-id, interval,
	 * and table-selector, or null if there is none.
	 * @param sdi the site datatype id (or ddid for NWIS).
	 * @param interval one of the valid interval codes.
	 * @param sel the table selector.
	 * @see IntervalCodes
	 * @return matching time series or null if there is none.
	 */
	public CTimeSeries getTimeSeries( DbKey sdi, String interval, String sel )
	{
		return getTimeSeries(sdi, interval, sel, Constants.undefinedIntKey);
	}

	/**
	 * Method for USBR that incorporates the model run ID.
	 */
	public CTimeSeries getTimeSeries( DbKey sdi, String interval, String sel, 
		int modelRunId )
	{
		for(CTimeSeries ts : tseries)
		{
			// For tempest and CWMS, the SDI is a unique key to a time-series.
			// so don't worry about matching interval, selector, and model run.
			if (TimeSeriesDb.sdiIsUnique)		// For Tempest and CWMS DB
			{
			  if (ts.getSDI().equals(sdi)) 
				  return ts;
			}
			// For HDB we need to check interval, selector and model run
			// Also, if selector is M_ that means this is modeled data, so we
			// also have to check the model run ID for a match.
			else
			{
			  if (ts.getSDI().equals(sdi)
				&& (TextUtil.strEqualIgnoreCase(interval, ts.getInterval())
			    && TextUtil.strEqualIgnoreCase(sel, ts.getTableSelector()))
			    && (sel == null || !sel.equalsIgnoreCase("M_") ||
				      modelRunId == ts.getModelRunId()))
				  return ts;
			}
		}  // end of for-loop
		return null;
	}
	
	public CTimeSeries getTimeSeriesByUniqueSdi(DbKey sdi)
	{
		for(CTimeSeries ts : tseries)
			  if (ts.getSDI().equals(sdi))
				  return ts;
		return null;
	}

	/**
	 * Method for USBR that incorporates the model ID.
	 */
	public CTimeSeries getTimeSeriesForModelId( DbKey sdi, String interval, String sel, 
		int modelId )
	{
		for(CTimeSeries ts : tseries)
			if (ts.getSDI().equals(sdi)
			 && (TimeSeriesDb.sdiIsUnique
			  || (  TextUtil.strEqualIgnoreCase(interval, ts.getInterval())
			     && TextUtil.strEqualIgnoreCase(sel, ts.getTableSelector())))
			 && modelId == ts.getModelId())
				return ts;
		return null;
	}

	/**
	 * Adds a new time series to the collection.
	 * @param ts the time series.
	 * @throws DuplicateTimeSeriesException if this collection already has a 
	    time series with matching sdi, table selector, and interval.
	 */
	public synchronized void addTimeSeries( CTimeSeries ts )
		throws DuplicateTimeSeriesException
	{
		if (getTimeSeries(ts.getSDI(), ts.getInterval(), ts.getTableSelector(),
			ts.getModelRunId())
			!= null)
			throw new DuplicateTimeSeriesException(
				"Time Series already exists with SDI=" + ts.getSDI()
				+ ", interval=" + ts.getInterval() 
				+ ", selector=" + ts.getTableSelector());
		tseries.add(ts);
	}
	
	/**
	 * Removes a time series from the collection.
	 * No action is taken if the passed time series is not present.
	 * @param ts the time series to remove.
	 */
	public synchronized void rmTimeSeries( CTimeSeries ts )
	{
		int sz = tseries.size();
		for(int idx=0; idx < sz; idx++)
			if (ts == tseries.get(idx))
			{
				tseries.remove(idx);
				return;
			}
	}
	
	/**
	 * Return a clone of the collection of time series.
	 * @return a clone of the collection of time series.
	 */
	public synchronized List<CTimeSeries> getAllTimeSeries( )
	{
		return new ArrayList<CTimeSeries>(tseries);
	}

	/** @return the tasklist record-range handle. */
	public RecordRangeHandle getTasklistHandle() { return rrHandle; }

	/**
	 * Sets the tasklist record-range handle.
	 * @param dbh the new database handle value.
	 */
	public void setTasklistHandle(RecordRangeHandle dbh)
	{
		rrHandle = dbh;
	}

	/**
	 * @return the number of CTimeSeries objects in the collection.
	 */
	public int size() { return tseries.size(); }

	public CTimeSeries getTimeSeriesAt(int idx)
	{
		return tseries.get(idx);
	}
	
	public boolean isEmpty()
	{
		return tseries.size() == 0;
	}
}
