/*
 * $Id$
 * 
 * $Log$
 * Revision 1.3  2014/10/07 13:02:09  mmaloney
 * dev
 *
 * 
 * Copyright 2007 Ilex Engineering, Inc. - All Rights Reserved.
 * No part of this file may be duplicated in either hard-copy or electronic
 * form without specific written permission.
*/
package decodes.tsdb;

import java.util.ArrayList;
import java.util.Iterator;
import decodes.db.Constants;
import decodes.db.DataType;
import decodes.db.Site;
import decodes.decoder.Sensor;

/**
Wraps a tsdb CTimeSeries object and implements the meta-data methods
for the DECODES Sensor class.
*/
public class DecodesSensorAdapter
	extends Sensor
{
	private CTimeSeries cts;

	/**
	 * Note: cts must be expanded. It must contain a DataDescriptor in its
	 * opaque openObj member.
	 */
	public DecodesSensorAdapter(CTimeSeries cts)
	{
		super(null, null, null, null);
		this.cts = cts;
	}

	/**
	 * Use the SDI as the DECODES sensor number. Some formatters print this so
	 * it may show up in output. Note that we are truncating the long to an int.
	 * So the value may not be accurate in CWMS which can have very large SDIs.
	 */
	public int getNumber() { return (int)cts.getSDI().getValue(); }

	public DataType getDataType()
	{
		TimeSeriesIdentifier tsid = (TimeSeriesIdentifier)cts.getTimeSeriesIdentifier();
		if (tsid == null)
			return null;
		return tsid.getDataType();
	}
	
	/** @return iterator for all data types defined. */
	public Iterator<DataType> getAllDataTypes()
	{
		ArrayList<DataType> aldt = new ArrayList<DataType>();
		aldt.add(getDataType());
		return aldt.iterator();
	}


	public DataType getDataType(String std)
	{
		TimeSeriesIdentifier tsid = (TimeSeriesIdentifier)cts.getTimeSeriesIdentifier();
		if (tsid == null)
			return null;
		DataType dt = tsid.getDataType();
		if (dt.getStandard().toLowerCase().equals(std))
			return dt;
		return dt.findEquivalent(std);
	}

	public String getName()
	{
		return cts.getDisplayName();
	}

	public char getRecordingMode()
	{
		String intvls = cts.getInterval();
		int intvli = IntervalCodes.getIntervalSeconds(intvls);
		return intvli >= 0 ? Constants.recordingModeFixed
			: Constants.recordingModeVariable;
	}

	public int getRecordingInterval()
	{
		String intvls = cts.getInterval();
		return IntervalCodes.getIntervalSeconds(intvls);
	}

	/** @return time-of-day for first recorded sample for this sensor */
	public int getTimeOfFirstSample()
	{
		return 0;
	}

	/**
	  If this method returns null, use the site name associated with the
	  platform.
	  @return sensor-specific site name if one is defined or null
	  if not. 
	*/
	public String getSensorSiteName()
	{
		Site site = getSensorSite();
		if (site == null)
			return null;
		return site.getDisplayName();
	}

	/**
	  If this method returns null, use the site name associated with the
	  platform.
	  @return sensor-specific site if one is defined or null if not. 
	*/
	public Site getSensorSite()
	{
		TimeSeriesIdentifier tsid = (TimeSeriesIdentifier)cts.getTimeSeriesIdentifier();
		if (tsid == null)
			return null;
		return tsid.getSite();
	}

	/**
	 * @return the site for this sensor, which can be explicitely defined
	 * or inherited from the platform.
	 */
	public Site getSite()
	{
		return getSensorSite();
	}

	public String getDisplayName()
	{
		TimeSeriesIdentifier tsid = (TimeSeriesIdentifier)cts.getTimeSeriesIdentifier();
		if (tsid == null)
			return null;
		return tsid.getDisplayName();
	}

	/**
	  @return the DBNO stored as a property on the platform sensor,
	  or -1 if there is no platform sensor or no DBNO property.
	*/
	public String getDBNO()
	{
		Site site = getSite();
		if (site == null)
			return null;
		String dbno = site.getUsgsDbno();
		if (dbno == null)
			dbno = getProperty("DBNO");
		return dbno;
	}

	/**
	  Currently this is stored in the ConfigSensor object, or in a "min"
	  property in the PlatformSensor object. The latter takes precidence if
	  both are defined.
	  @return absolute minimum for this sensor, or 'Constants.undefinedDouble'
	  if no minimum is defined.
	*/
	public double getMinimum()
	{
		TimeSeriesIdentifier tsid = (TimeSeriesIdentifier)cts.getTimeSeriesIdentifier();
//		if (tsid == null)
			return Constants.undefinedDouble;
//TODO Implement this with new limits mechanism w/o referencing Tempest
//		if (tsid instanceof DataDescriptor)
//		{
//			DataDescriptor dd = (DataDescriptor)tsid;
//			Limits limits = dd.getLimits();
//			if (limits.getRejectLowValue() != Limits.UNDEFINED_LOW)
//				return limits.getRejectLowValue();
//		}
//		return Constants.undefinedDouble;
	}

	/**
	  Currently this is stored in the ConfigSensor object, or in a "min"
	  property in the PlatformSensor object. The latter takes precidence if
	  both are defined.
	  @return absolute maximum for this sensor, or 'Constants.undefinedDouble'
	  if no minimum is defined.
	*/
	public double getMaximum()
	{
		TimeSeriesIdentifier tsid = (TimeSeriesIdentifier)cts.getTimeSeriesIdentifier();
//		if (tsid == null)
			return Constants.undefinedDouble;
//TODO Implement this with new limits mechanism w/o referencing Tempest
//		if (tsid instanceof DataDescriptor)
//		{
//			DataDescriptor dd = (DataDescriptor)tsid;
//			Limits limits = dd.getLimits();
//			if (limits.getRejectHighValue() != Limits.UNDEFINED_HIGH)
//				return limits.getRejectHighValue();
//		}
//		return Constants.undefinedDouble;
	}

	public String getUsgsStatCode()
	{
		TimeSeriesIdentifier tsid = (TimeSeriesIdentifier)cts.getTimeSeriesIdentifier();
		if (tsid == null)
			return null;
		return tsid.getPart("statcode");
	}

	public int getUsgsDdno()
	{
		return 0;
	}
	
	/**
	 * @return the time series identifier as it was defined in the database.
	 */
	public String getDbTsId()
	{
		return cts.getTimeSeriesIdentifier() != null ? cts.getTimeSeriesIdentifier().getUniqueString()
			: null;
	}
}
