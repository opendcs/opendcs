/*
 * $Id: DecodesSensorCnvt.java,v 1.2 2020/02/20 15:30:18 mmaloney Exp $
 * 
 * $Log: DecodesSensorCnvt.java,v $
 * Revision 1.2  2020/02/20 15:30:18  mmaloney
 * Added getProperty() that checks TSID parts before delegating to super.
 *
 * Revision 1.1  2017/08/22 19:49:29  mmaloney
 * Refactor
 *
 * 
 * Copyright 2014 U.S. Army Corps of Engineers, Hydrologic Engineering Center.
*/
package decodes.util;

import java.util.Iterator;
import java.util.Vector;
import decodes.tsdb.CTimeSeries;
import decodes.tsdb.IntervalCodes;
import decodes.db.DataType;
import decodes.db.Site;

/** 
 * Make a CTimeSeries object look like a DECODES Sensor using the
 * Adapter pattern.
*/
public class DecodesSensorCnvt extends decodes.decoder.Sensor
{
	private CTimeSeries cts;

	/** ctor provided with CTimeSeris Object. */
	public DecodesSensorCnvt(CTimeSeries cts)
	{
		super(null, null, null, null);
		this.cts = cts;
	}

	/**
	 * DECODES Data Type derived from the CTimeSeries TSID.
	 */
	@Override
	public DataType getDataType()
	{
		if (cts.getTimeSeriesIdentifier() == null) return null;
		return cts.getTimeSeriesIdentifier().getDataType();
	}

	/**
	 * Some formatters pick and choose which datatype to use from a list.
	 * But the CTimeSeries only has one. So put it in a mock list of 1 entry.
	 */
	@Override
	public Iterator<DataType> getAllDataTypes()
	{
		Vector<DataType> aldt = new Vector<DataType>();
		aldt.add(getDataType());
		return aldt.iterator();
	}

	/** Use Site Datatype ID as a surrogate for sensor number, in case formatter needs it. */
	@Override
	public int getNumber() { return (int)cts.getSDI().getValue(); }

	/**
	 * @Return displayname if one is defined or the tsid unique string if not.
	 */
	@Override
	public String getName()
	{
		if (cts.getDisplayName() != null) return cts.getDisplayName();
		if (cts.getTimeSeriesIdentifier() != null)
			return cts.getTimeSeriesIdentifier().getUniqueString();
		else
			return "unknown";
	}
	/**
	 * Some formatters require a specific data type type.
	 * If the one in CTimeSeries doesn't match, try to find an equivalent.
	 */
	@Override
	public DataType getDataType(String std)
	{
		if (cts.getTimeSeriesIdentifier() == null) return null;
		DataType dt = cts.getTimeSeriesIdentifier().getDataType();
		if (dt == null) return null;
		if (dt.getStandard().equalsIgnoreCase(std)) return dt;
		return dt.findEquivalent(std);
	}

	/**
	 * We can't know time of first sample. So just assume zero.
	 */
	@Override
	public int getTimeOfFirstSample()
	{
		return 0;
	}

	/**
	 * Try to convert time series interval string into # seconds.
	 */
	@Override
	public int getRecordingInterval()
	{
		return IntervalCodes.getIntervalSeconds(cts.getInterval());
	}

	/**
	 * If recording interval > 0 then assume this is a fixed interval sensor.
	 */
	@Override
	public char getRecordingMode()
	{
		if (getRecordingInterval() > 0)
			return 'F';
		else
			return 'V';
	}

	/**
	 * @return display name of site associated with the TSID in the CTimeSeries
	*/
	@Override
	public String getSensorSiteName()
	{
		Site site = getSensorSite();
		if (site == null) return null;
		return site.getDisplayName();
	}

	/**
	 * If this method returns null, use the site name associated with the
	 * platform.
	 * @return sensor-specific site if one is defined or null if not. 
	 */
	@Override
	public Site getSensorSite()
	{
		if (cts.getTimeSeriesIdentifier() == null)
			return null;
		return cts.getTimeSeriesIdentifier().getSite();
	}

	/** Always returns same as getSensorSite() */
	@Override
	public Site getSite() { return getSensorSite(); }

	@Override
	public String getDisplayName()
	{
		if (cts.getTimeSeriesIdentifier() == null) return null;
		return cts.getTimeSeriesIdentifier().getDisplayName();
	}

	/** @return null always */
	@Override
	public String getDBNO()
	{
		return null;
	}

	/**
	 * Always returns Double.MAX_VALUE, signifying no minimum defined.
	 */
	@Override
	public double getMinimum()
	{
		return Double.MAX_VALUE;
	}

	/**
	 * Always return zero
	 */
	@Override
	public int getUsgsDdno()
	{
		return 0;
	}
	
	/**
	 * Always returns Double.MAX_VALUE, signifying no maximum defined.
	 */
	@Override
	public double getMaximum()
	{
			return Double.MAX_VALUE;
	}

	/**
	 * Always return null;
	 */
	@Override
	public String getUsgsStatCode()
	{
		return null;
	}
	
	/**
	 * Special method added for HydroJSON Formatter
	 */
	public String getDbTsId()
	{
		return cts.getTimeSeriesIdentifier() != null 
			? cts.getTimeSeriesIdentifier().getUniqueString() : null;
	}
	
	@Override
	public String getProperty(String name)
	{
		// Return time series ID parts as properties.
		String ret = cts.getTimeSeriesIdentifier().getPart(name);
		if (ret != null)
			return ret;
		return super.getProperty(name);
	}
}
