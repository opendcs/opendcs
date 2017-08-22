/*
 * $Id$
 * 
 * $Log$
 * 
 * Copyright 2014 U.S. Army Corps of Engineers, Hydrologic Engineering Center.
*/
package decodes.util;

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
import decodes.tsdb.CTimeSeries;
import decodes.tsdb.IntervalCodes;
import decodes.tsdb.VarFlags;

public class TSUtil
{
	// Assign decodes sensor #s to ascending sequence.
	private static int seqSensorNum = 0;
	
	/**
	 * Convert a DECODES ts to a TSDB CTimeSeries.
	 * @param ts the decodes ts
	 * @param sdi the site-datatype-id
	 * @param tableSelector
	 * @param interval
	 * @param mustWrite
	 * @param sourceId
	 * @return
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
	 * Convert units of a CTimeSeries.
	 * Does nothing if units already match.
	 * @param cts the CTimeSeries
	 * @param newUnits the required units
	 */
	public static void convertUnits(CTimeSeries cts, String newUnits)
	{
		if (TextUtil.strEqualIgnoreCase(cts.getUnitsAbbr(), newUnits) || newUnits == null)
			return;
		if (cts.getUnitsAbbr() == null)
		{
			cts.setUnitsAbbr(newUnits);
			return;
		}

		EngineeringUnit euOld =	EngineeringUnit.getEngineeringUnit(cts.getUnitsAbbr());
		EngineeringUnit euNew = EngineeringUnit.getEngineeringUnit(newUnits);
		UnitConverter converter = null;
		converter = Database.getDb().unitConverterSet.get(euOld, euNew);
		if (converter == null)
		{
			Logger.instance().warning(
				"Cannot convert samples for time series '" + 
				cts.getNameString()
				+ "' from " + euOld.abbr + " to " + euNew.abbr
				+ " -- assuming already correct units.");
			cts.setUnitsAbbr(newUnits);
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

	/**
	 * Convert CTimeSeries to DECODES ts
	 * @param cts
	 * @return
	 */
	public static TimeSeries convert2DecodesTimeSeries(CTimeSeries cts)
	{
		TimeSeries ts = new TimeSeries(seqSensorNum++);
		ts.setUnits(cts.getUnitsAbbr());
		ts.setTimeInterval(IntervalCodes.getIntervalSeconds(cts.getInterval()));
		ts.setSensor(new DecodesSensorCnvt(cts));

		int n=cts.size();
		for(int i=0; i<n; i++)
		{
			ts.addSample(cts.sampleAt(i));
		}
		return ts;
	}
}
