/*
* Where Applicable, Copyright 2025 OpenDCS Consortium and/or its contributors
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

import ilex.util.TextUtil;
import ilex.var.IFlags;
import ilex.var.NoConversionException;
import ilex.var.TimedVariable;

import org.opendcs.utils.logging.OpenDcsLoggerFactory;
import org.slf4j.Logger;

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
	private static final Logger log = OpenDcsLoggerFactory.getLogger();
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
}