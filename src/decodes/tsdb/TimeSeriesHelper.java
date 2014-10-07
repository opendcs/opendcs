/*
* Copyright 2007 Ilex Engineering, Inc. - All Rights Reserved.
* No part of this file may be duplicated in either hard-copy or electronic
* form without specific written permission.
* 
 * 2014 Notice: Cove Software, LLC believes the above copyright notice to be
 * in error. This module was 100% funded by the U.S. Federal Government under
 * contracts requiring that it be Government-Owned. It has been delivered to
 * U.S. Bureau of Reclamation, U.S. Geological Survey, and U.S. Army Corps of
 * Engineers under contract.

*/
package decodes.tsdb;

import ilex.util.Logger;
import ilex.util.TextUtil;
import ilex.var.IFlags;
import ilex.var.NoConversionException;
import ilex.var.TimedVariable;

import decodes.db.Database;
import decodes.db.EngineeringUnit;
import decodes.db.UnitConverter;
import decodes.decoder.TimeSeries;
import decodes.sql.DbKey;
import decodes.util.DecodesException;

/**
Methods for manipulating CTimeSeries and DECODES TimeSeries objects.
*/
public class TimeSeriesHelper
{
	/**
	 * DECODES TimeSeries requires a sensor number that is supposed to be
	 * unique within a platform. When we convert a TSDB CTimeSeries to a 
	 * DECODES time series, just give it the next sequence value.
	 */
	private static int sensorNumberSequence = 0;
	
	/**
	 * Converts a DECODES TimeSeries into a TSDB CTimeSeries.
	 */
	public static CTimeSeries convert2CTimeSeries(TimeSeries ts, DbKey sdi, 
		String tableSelector, String interval, boolean mustWrite, int sourceId)
	{
		CTimeSeries ret = new CTimeSeries(sdi, interval, tableSelector);
		int n = ts.size();
		for(int i=0; i<n; i++)
		{
			TimedVariable tv = ts.sampleAt(i);
			
			String useFormattedSamples = ts.getProperty("useformattedsample");
			if (useFormattedSamples != null &&useFormattedSamples.length() > 0)
			{
			try {				
				if (ts.sampleAt(i).isNumeric())
					tv.setValue(Double.valueOf(ts.formattedSampleAt(i)));
			} catch (NumberFormatException e) {
				
			}
			}
			
			int f = tv.getFlags();
			if ((f & (IFlags.IS_ERROR | IFlags.IS_MISSING)) != 0)
				continue;
			if (mustWrite)
				tv.setFlags(tv.getFlags() | VarFlags.TO_WRITE);
			tv.setSourceId(sourceId);
			ret.addSample(tv);
		}
		ret.setUnitsAbbr(ts.getUnits());
		return ret;
	}

	/**
	 * Converts a TSDB CTimeSeries object into a DECODES TimeSeries.
	 */
	public static TimeSeries convert2DecodesTimeSeries(CTimeSeries cts)
	{
		TimeSeries ts = new TimeSeries(sensorNumberSequence++);
		ts.setUnits(cts.getUnitsAbbr());
		ts.setTimeInterval(IntervalCodes.getIntervalSeconds(cts.getInterval()));
		ts.setSensor(new DecodesSensorAdapter(cts));

		int n=cts.size();
		for(int i=0; i<n; i++)
		{
			ts.addSample(cts.sampleAt(i));
		}
		return ts;
	}

	/**
	 * Converts time series values when the CTimeSeries eng units
	 * does not match with the Data Descriptor eng units
	 * 
	 * @param cts Time Series values
	 * @param newUnits units from Data Descriptor
	 */
	public static void convertUnits(CTimeSeries cts, String newUnits)
	{
		if (TextUtil.strEqualIgnoreCase(cts.getUnitsAbbr(), newUnits)
		 || newUnits == null)
			return;
		if (cts.getUnitsAbbr() == null)
		{
			cts.setUnitsAbbr(newUnits);
			return;
		}

		EngineeringUnit euOld =	EngineeringUnit.getEngineeringUnit(
			cts.getUnitsAbbr());
		EngineeringUnit euNew = EngineeringUnit.getEngineeringUnit(
			newUnits);
		UnitConverter converter = null;
		converter = Database.getDb().unitConverterSet.get(euOld, euNew);
		if (converter == null)
		{
			Logger.instance().warning(
				"Cannot convert samples for time series '" + 
				cts.getNameString()
				+ "' from " + euOld.abbr + " to " + euNew.abbr
				+ " -- assuming already correct units.");
			return;
		}
		Logger.instance().debug3(
			"Converting samples for time series '" + 
			cts.getNameString()
			+ "' from " + euOld.abbr + " to " + euNew.abbr);
		for(int i=0; i<cts.size(); i++)
		{
			TimedVariable tv = cts.sampleAt(i);
			if (tv == null || !tv.isNumeric())
				continue;
			double newValue;
			try
			{
				newValue = converter.convert(tv.getDoubleValue());
				tv.setValue(newValue);
			} catch (NoConversionException e)
			{
				Logger.instance().warning(e.toString());			
			} 
			catch (DecodesException e)
			{
				Logger.instance().warning(e.toString());
			}
		}
		cts.setUnitsAbbr(newUnits);
	}

}
