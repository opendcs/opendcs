/*
 * Copyright 2025 The OpenDCS Consortium and contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package decodes.cwms.algo;

import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

import decodes.cwms.CwmsTsId;
import decodes.tsdb.CTimeSeries;
import decodes.tsdb.IntervalCodes;
import decodes.tsdb.TimeSeriesIdentifier;
import opendcs.opentsdb.Interval;

final class IntervalOffsetUtil
{
	private static final long MSEC_PER_UTC_DAY = 24 * 3600 * 1000L;
	private IntervalOffsetUtil()
	{
		throw new AssertionError("Utility class cannot be instantiated.");
	}

	private static int getIntervalOffsetForTime(Interval intv, Date time)
	{
		Calendar utcCal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
		utcCal.setTime(time);
		utcCal.set(Calendar.SECOND, 0);
		if(intv == null)
		{
			throw new IllegalArgumentException("Interval cannot be null.");
		}
		switch(intv.getCalConstant())
		{
			case Calendar.MINUTE:
				utcCal.set(Calendar.MINUTE, (utcCal.get(Calendar.MINUTE) / intv.getCalMultiplier()) * intv.getCalMultiplier());
				break;
			case Calendar.HOUR_OF_DAY:    // truncate to top of (hour*mult)
				utcCal.set(Calendar.MINUTE, 0);
				utcCal.set(Calendar.HOUR_OF_DAY,
						(utcCal.get(Calendar.HOUR_OF_DAY) / intv.getCalMultiplier()) * intv.getCalMultiplier());
				break;
			case Calendar.DAY_OF_MONTH: // truncate to top of (day*mult)
				utcCal.set(Calendar.HOUR_OF_DAY, 0);
				utcCal.set(Calendar.MINUTE, 0);
				// Now truncate back, using number of days since epoch
				utcCal.setTimeInMillis(
						(daysSinceEpoch(utcCal.getTimeInMillis()) / intv.getCalMultiplier())
								* intv.getCalMultiplier() * MSEC_PER_UTC_DAY);
				break;
			case Calendar.WEEK_OF_YEAR:
				utcCal.set(Calendar.HOUR_OF_DAY, 0);
				utcCal.set(Calendar.MINUTE, 0);
				utcCal.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY);
				utcCal.set(Calendar.WEEK_OF_YEAR,
						(utcCal.get(Calendar.WEEK_OF_YEAR) / intv.getCalMultiplier()) * intv.getCalMultiplier());
				break;
			case Calendar.MONTH:
				utcCal.set(Calendar.HOUR_OF_DAY, 0);
				utcCal.set(Calendar.MINUTE, 0);
				utcCal.set(Calendar.DAY_OF_MONTH, 1);
				utcCal.set(Calendar.MONTH,
						(utcCal.get(Calendar.MONTH) / intv.getCalMultiplier()) * intv.getCalMultiplier());
				break;
			case Calendar.YEAR:
				utcCal.set(Calendar.HOUR_OF_DAY, 0);
				utcCal.set(Calendar.MINUTE, 0);
				utcCal.set(Calendar.DAY_OF_MONTH, 1);
				utcCal.set(Calendar.MONTH, Calendar.JANUARY);
				utcCal.set(Calendar.YEAR,
						(utcCal.get(Calendar.YEAR) / intv.getCalMultiplier()) * intv.getCalMultiplier());
				break;
		}

		// Compute offset in seconds. Will always be positive because we truncate utcCal backward.
		return (int) ((time.getTime() - utcCal.getTimeInMillis()) / 1000L);
	}

	private static int daysSinceEpoch(long msecTime)
	{
		return (int) (msecTime / MSEC_PER_UTC_DAY);
	}

	static boolean matchesIntervalOffset(CTimeSeries timeSeries, Date date)
	{
		Interval intv = IntervalCodes.getInterval(timeSeries.getInterval());
		int offset = getIntervalOffsetForTime(intv, date);
		TimeSeriesIdentifier timeSeriesIdentifier = timeSeries.getTimeSeriesIdentifier();
		int offsetSeconds = 0;
		if(timeSeriesIdentifier instanceof CwmsTsId)
		{
			offsetSeconds = ((CwmsTsId) timeSeriesIdentifier).getUtcOffset();
		}
		int offsetError = offset - offsetSeconds;
		boolean violation = (offsetError != 0);

		if (!violation)
			return true;


		if (intv.getCalConstant() == Calendar.MINUTE
				|| (intv.getCalConstant() == Calendar.HOUR_OF_DAY && intv.getCalMultiplier() == 1))
		{
			violation = true;
		}
		else
		{
			boolean allowDstVariation = false;
			if(timeSeriesIdentifier instanceof CwmsTsId)
			{
				allowDstVariation = ((CwmsTsId) timeSeriesIdentifier).isAllowDstOffsetVariation();
			}
			if (allowDstVariation &&
					((intv.getCalConstant() == Calendar.HOUR_OF_DAY && intv.getCalMultiplier() > 1)
							|| intv.getCalConstant() == Calendar.DAY_OF_MONTH
							|| intv.getCalConstant() == Calendar.WEEK_OF_YEAR))
			{
				if (offsetError == -3600 || offsetError == 3600)
				{
					violation = false;
				}
			}
			else if (intv.getCalConstant() == Calendar.MONTH)
			{
				// In monthly value, offset is seconds since start of month.
				// It may span a DST change.
				if(allowDstVariation &&
						(offsetError == -3600 || offsetError == 3600))
				{
					violation = false;
				}

				//TODO Consider use case where end of month is stored.
				// In march offset is 30d (31-1). In feb this is 27.
				// What if stored offset is 30d but this is 27d?
				// What if stored offset is 27d but this is 30d?
				// So (I think) offsetError can be +/- 1d, 2d, or 3d.
				// Also if allowDstVariation, it may also be +/- 1h.
			}
			else if (intv.getCalConstant() == Calendar.YEAR)
			{
				// max offsetError is 1day * (mult/4) + 1 (i.e. as much as 1 day for every 4 years)
				// DST variation may apply because rules occasionally change as to
				// when DST starts/stops in the year. So if allowDstVariation, it may also be +/- 1h.
				for(int x = 1; x <= intv.getCalMultiplier()/4 + 1; x++)
				{
					if (offsetError == 3600*24
							|| (allowDstVariation && offsetError == 3600*24 + 3600)
							|| (allowDstVariation && offsetError == 3600*24 - 3600)
							|| offsetError == -3600*24
							|| (allowDstVariation && offsetError == -3600*24 + 3600)
							|| (allowDstVariation && offsetError == -3600*24 - 3600))
					{
						violation = false;
						break;
					}
				}
			}
		}
		return !violation;
	}
}
