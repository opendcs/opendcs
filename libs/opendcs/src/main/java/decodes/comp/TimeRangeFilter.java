package decodes.comp;

import ilex.util.Logger;
import ilex.var.IFlags;
import ilex.var.TimedVariable;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.TimeZone;

import decodes.db.Platform;
import decodes.decoder.DecodedMessage;
import decodes.decoder.TimeSeries;

/**
 * Filters time series data based on the specified time range. It discards future
 * data points that exceed a threshold or data that is too old.
 */
public class TimeRangeFilter extends Computation
{
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
		Logger.instance().debug1("Applying time range filter to message from "
			+ (p != null ? p.getSiteName() : "unknown platform")
			+ ", maxFutureMinutes=" + maxFutureMinutes + ", maxAgeHours=" + maxAgeHours);

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
					Logger.instance().debug3(module + " "
						+ ts.getSensor().getSite().getDisplayName() + " "
						+ ts.getSensorName() + " Discarding FUTURE TIME " 
						+ sampleTime);
					tv.setFlags(flags | IFlags.IS_ERROR | IFlags.IS_MISSING);
				}
				else if (deltaT < -maxAgeHours * 3600000L)
				{
					Logger.instance().debug3(module + " "
						+ ts.getSensor().getSite().getDisplayName() + " "
						+ ts.getSensorName() + " Discarding ANCIENT TIME " 
						+ sampleTime + ", deltaT=" + deltaT);
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
