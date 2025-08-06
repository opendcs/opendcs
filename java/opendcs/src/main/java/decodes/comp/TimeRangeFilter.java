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
package decodes.comp;

import ilex.var.IFlags;
import ilex.var.TimedVariable;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.TimeZone;

import org.opendcs.utils.logging.OpenDcsLoggerFactory;
import org.slf4j.Logger;

import decodes.db.Platform;
import decodes.decoder.DecodedMessage;
import decodes.decoder.TimeSeries;

/**
 * Filters time series data based on the specified time range. It discards future
 * data points that exceed a threshold or data that is too old.
 */
public class TimeRangeFilter extends Computation
{
	private static final Logger log = OpenDcsLoggerFactory.getLogger();

	private int maxFutureMinutes;
	private int maxAgeHours;
	public final static String module = "TimeRangeFilter";
	private SimpleDateFormat sdf = new SimpleDateFormat("yyyy MM/dd HH:mm");

	/**
	 * Constructs a TimeRangeFilter with the specified future time and age limits.
	 *
	 * @param maxFutureMinutes the max number of minutes in the future a data point can be.
	 * @param maxAgeHours the max age in hours for a valid data point.
	 */
	public TimeRangeFilter(int maxFutureMinutes, int maxAgeHours)
	{
		this.maxFutureMinutes = maxFutureMinutes;
		this.maxAgeHours = maxAgeHours;
	}


	/**
	 * Applies the time range filter to the provided data collection. Iterates through
	 * all time series and marks data points that are too far in the future or too old
	 * as missing.
	 *
	 * @param msg the data collection.
	 */
	@Override
	public void apply(IDataCollection msg)
	{
		// It will always be a DecodedMessage
		if (!(msg instanceof DecodedMessage))
			return;
		DecodedMessage dm = (DecodedMessage)msg;

		Platform p = dm.getPlatform();
		log.debug("Applying time range filter to message from {}, maxFutureMinutes={}, maxAgeHours={}",
				  (p != null ? p.getSiteName() : "unknown platform"), maxFutureMinutes, maxAgeHours);

		for(Iterator<TimeSeries> tsit = dm.getAllTimeSeries(); tsit.hasNext(); )
		{
			TimeSeries ts = tsit.next();
			for(int tsIdx = 0; tsIdx < ts.size(); tsIdx++)
			{
				TimedVariable tv = ts.sampleAt(tsIdx);
				int flags = tv.getFlags();
				if ((flags & (IFlags.IS_MISSING | IFlags.IS_ERROR)) != 0)
					continue;

				Date sampleTime = tv.getTime();

				// deltaT is difference between sample time and now.
				// Future data is positive, past data is negative
				long deltaT = sampleTime.getTime() - System.currentTimeMillis();
				if (deltaT > maxFutureMinutes * 60000L)
				{
					log.trace("{} {} Discarding FUTURE TIME {}", ts.getSensor().getSite().getDisplayName(), ts.getSensorName(), sampleTime);
					tv.setFlags(flags | IFlags.IS_ERROR | IFlags.IS_MISSING);
				}
				else if (deltaT < -maxAgeHours * 3600000L)
				{
					log.trace("{} {} Discarding ANCIENT TIME  {}, deltaT={}",
							  ts.getSensor().getSite().getDisplayName(), ts.getSensorName(), sampleTime, deltaT);
					tv.setFlags(flags | IFlags.IS_ERROR | IFlags.IS_MISSING);
				}
			}
		}
	}

	/**
	 * Sets the logger's time zone for formatting timestamps in log messages.
	 *
	 * @param tz Desired time zone for the logger; if null, default time zone is used.
	 */
	public void setLoggerTz(TimeZone tz)
	{
		if (tz != null)
			sdf.setTimeZone(tz);
	}

}
