/*
* Where Applicable, Copyright 2025 - 2026 OpenDCS Consortium and/or its contributors
*
* Licensed under the Apache License, Version 2.0 (the "License"); you may not
* use this file except in compliance with the License. You may obtain a copy
* of the License at
*
*   http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
* WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
* License for the specific language governing permissions and limitations
* under the License.
*/
package decodes.util;

import decodes.tsdb.*;
import ilex.util.TextUtil;
import ilex.var.IFlags;
import ilex.var.NoConversionException;
import ilex.var.TimedVariable;

import opendcs.dai.TimeSeriesDAI;
import org.opendcs.utils.logging.OpenDcsLoggerFactory;
import org.slf4j.Logger;

import decodes.db.Database;
import decodes.db.EngineeringUnit;
import decodes.db.UnitConverter;
import decodes.decoder.TimeSeries;
import decodes.sql.DbKey;

import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

import opendcs.opentsdb.Interval;

public class TSUtil
{
	private static final Logger log = OpenDcsLoggerFactory.getLogger();
	private static final long MSEC_PER_UTC_DAY = 24 * 3600 * 1000L;
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
		String tableSelector, String interval, boolean mustWrite, DbKey sourceId)
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
			log.warn("Cannot convert samples for time series '{}' from {} to {}" +
					 " -- assuming already correct units.",
					  cts.getNameString(), euOld.abbr, euNew.abbr);
			cts.setUnitsAbbr(newUnits);
			return;
		}
		log.trace("Converting samples for time series '{}' from '{}' to '{}'",
				  cts.getNameString(), euOld.abbr, euNew.abbr);
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
			}
			catch (DecodesException | NoConversionException ex)
			{
				log.atWarn()
				   .setCause(ex)
				   .log("Unable to convert sample '{}' at time {}",tv.getStringValue(), tv.getTime());
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


	public static void extendTimeSeries(TimeSeriesDAI timeSeriesDAI, CTimeSeries timeSeries, Date start, Date end) throws DbCompException
	{
		if(timeSeries.findWithin(start, 0) != null && timeSeries.findWithin(end, 0) != null)
		{
			return;
		}
		try
		{
			timeSeriesDAI.fillTimeSeries(timeSeries, start, end);
		}
		catch(DbIoException | BadTimeSeriesException e)
		{
			throw new DbCompException("Could not retrieve time series: " + timeSeries.getTimeSeriesIdentifier(), e);
		}
	}

	/**
	 * Returns seconds from the UTC interval boundary to {@code time}.
	 *
	 * @param interval interval to evaluate
	 * @param time timestamp to evaluate
	 * @return offset in seconds
	 */
	public static int getIntervalOffsetForTime(Interval interval, Date time)
	{
		if(interval == null)
		{
			throw new IllegalArgumentException("Interval cannot be null.");
		}
		Calendar utcCal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
		utcCal.setTime(time);
		utcCal.set(Calendar.SECOND, 0);
		switch(interval.getCalConstant())
		{
			case Calendar.MINUTE:
				utcCal.set(Calendar.MINUTE, (utcCal.get(Calendar.MINUTE) / interval.getCalMultiplier())
						* interval.getCalMultiplier());
				break;
			case Calendar.HOUR_OF_DAY:
				utcCal.set(Calendar.MINUTE, 0);
				utcCal.set(Calendar.HOUR_OF_DAY, (utcCal.get(Calendar.HOUR_OF_DAY) / interval.getCalMultiplier())
						* interval.getCalMultiplier());
				break;
			case Calendar.DAY_OF_MONTH:
				utcCal.set(Calendar.HOUR_OF_DAY, 0);
				utcCal.set(Calendar.MINUTE, 0);
				utcCal.setTimeInMillis((daysSinceUtcEpoch(utcCal.getTimeInMillis()) / interval.getCalMultiplier())
						* interval.getCalMultiplier() * MSEC_PER_UTC_DAY);
				break;
			case Calendar.WEEK_OF_YEAR:
				utcCal.set(Calendar.HOUR_OF_DAY, 0);
				utcCal.set(Calendar.MINUTE, 0);
				utcCal.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY);
				utcCal.set(Calendar.WEEK_OF_YEAR, (utcCal.get(Calendar.WEEK_OF_YEAR) / interval.getCalMultiplier())
						* interval.getCalMultiplier());
				break;
			case Calendar.MONTH:
				utcCal.set(Calendar.HOUR_OF_DAY, 0);
				utcCal.set(Calendar.MINUTE, 0);
				utcCal.set(Calendar.DAY_OF_MONTH, 1);
				utcCal.set(Calendar.MONTH, (utcCal.get(Calendar.MONTH) / interval.getCalMultiplier())
						* interval.getCalMultiplier());
				break;
			case Calendar.YEAR:
				utcCal.set(Calendar.HOUR_OF_DAY, 0);
				utcCal.set(Calendar.MINUTE, 0);
				utcCal.set(Calendar.DAY_OF_MONTH, 1);
				utcCal.set(Calendar.MONTH, Calendar.JANUARY);
				utcCal.set(Calendar.YEAR, (utcCal.get(Calendar.YEAR) / interval.getCalMultiplier())
						* interval.getCalMultiplier());
				break;
			default:
				throw new IllegalArgumentException("Unsupported interval calendar constant: " + interval.getCalConstant());
		}
		return (int)((time.getTime() - utcCal.getTimeInMillis()) / 1000L);
	}

	/**
	 * Checks whether {@code time} matches a stored UTC interval offset.
	 * The caller supplies any database-specific offset and DST policy.
	 *
	 * @param interval interval to evaluate
	 * @param time candidate timestamp
	 * @param storedOffsetSeconds persisted offset from the interval boundary
	 * @param allowDstOffsetVariation whether a one-hour DST variation is allowed
	 * @return {@code true} if the offset matches
	 */
	public static boolean matchesIntervalOffset(Interval interval, Date time,
			int storedOffsetSeconds, boolean allowDstOffsetVariation)
	{
		int offsetError = getIntervalOffsetForTime(interval, time) - storedOffsetSeconds;
		if(offsetError == 0)
		{
			return true;
		}

		if(interval.getCalConstant() == Calendar.MINUTE
				|| (interval.getCalConstant() == Calendar.HOUR_OF_DAY && interval.getCalMultiplier() == 1))
		{
			return false;
		}

		if(allowDstOffsetVariation
				&& ((interval.getCalConstant() == Calendar.HOUR_OF_DAY && interval.getCalMultiplier() > 1)
						|| interval.getCalConstant() == Calendar.DAY_OF_MONTH
						|| interval.getCalConstant() == Calendar.WEEK_OF_YEAR))
		{
			return offsetError == -3600 || offsetError == 3600;
		}
		if(interval.getCalConstant() == Calendar.MONTH)
		{
			// TODO: Define month-end offset handling; month lengths can shift this by days.
			return allowDstOffsetVariation && (offsetError == -3600 || offsetError == 3600);
		}
		if(interval.getCalConstant() == Calendar.YEAR)
		{
			// TODO: Define multi-year tolerance; this accepts one leap day and optional DST.
			return offsetError == 3600 * 24
					|| offsetError == -3600 * 24
					|| (allowDstOffsetVariation
							&& (offsetError == 3600 * 24 + 3600
									|| offsetError == 3600 * 24 - 3600
									|| offsetError == -3600 * 24 + 3600
									|| offsetError == -3600 * 24 - 3600));
		}
		return false;
	}

	private static int daysSinceUtcEpoch(long msecTime)
	{
		return (int)(msecTime / MSEC_PER_UTC_DAY);
	}
}
