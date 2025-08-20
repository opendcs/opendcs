/*
 *	$Id$
 */
package decodes.decoder;

import java.util.*;
import java.text.SimpleDateFormat;

import ilex.util.Logger;
import ilex.util.PropertiesUtil;
import ilex.util.TextUtil;
import ilex.var.Variable;
import ilex.var.NoConversionException;
import ilex.var.TimedVariable;

import decodes.datasource.RawMessage;
import decodes.datasource.EdlPMParser;
import decodes.datasource.GoesPMParser;
import decodes.datasource.UnknownPlatformException;
import decodes.db.ConfigSensor;
import decodes.db.Constants;
import decodes.db.DataPresentation;
import decodes.db.DecodesScript;
import decodes.db.EquipmentModel;
import decodes.db.Platform;
import decodes.db.PlatformConfig;
import decodes.db.PlatformSensor;
import decodes.db.PresentationGroup;
import decodes.db.ScriptSensor;
import decodes.db.Site;
import decodes.db.TransportMedium;
import decodes.comp.IDataCollection;
import decodes.comp.ITimeSeries;

/**
 * DecodedMessage stores the entire results from decoding a message, including
 * <ul>
 * <li>the RawMessage object</li>
 * <li>the Platform reference</li>
 * <li>the (multiple) TimeSeries</li>
 * <li>message time stamp</li>
 * <li>time zone used by TimeSeries in this message</li>
 * </ul>
 */
public class DecodedMessage implements IDataCollection
{
	/** The raw data */
	private RawMessage rawMessage;
	/** The Platform */
	private Platform platform;
	/** Vector of TimeSeries objects */
	private ArrayList<TimeSeries> timeSeriesArray;

	/** Extracted from message (or file) header. */
	private Date messageTime;
	
	/** If the Truncation operator [T(x)] was used, this is set to original
	 * (pre-truncated) time. This is for use with MOFF.
	 */
	private Date untruncatedMessageTime;
	
	/** Set from time/date operations within the message. */
	private RecordedTimeStamp currentTime;

	/** Whether or not time adjustment was made to msg-begin time */
	private boolean timeAdjustmentMadeToBeginTime = false;

	/** Used to determine 1st samp time for manual daylight timezone setting */
	TimedVariable firstSample;

	/** The Time Zone used for time/date stamps within the message. */
	String tzName;

	/** The presentation group applied to this message, or null if none was. */
	private PresentationGroup presentationGroupApplied;

	private SimpleDateFormat loggerDateFmt = new SimpleDateFormat("yyyy MMM/dd HH:mm:ss");

	/**
	 * Set when we get a year. Cleared when we get non-year time component after
	 * some data.
	 */
	private boolean justGotFullDateTime = false;
	private boolean justAddedSample = false;
	
	boolean timeWasTruncated = false;

	/**
	 * This ctor used when raw message must contain platform linkage.
	 * 
	 * @param rawMessage
	 *            the RawMessage
	 */
	public DecodedMessage(RawMessage rawMessage) throws UnknownPlatformException
	{
		this(rawMessage, true);
	}

	/**
	 * This ctor used when raw message may contain no platform linkage.
	 * 
	 * @param rawMessage
	 *            the RawMessage
	 * @throws UnknownPlatformException
	 *             if requirePlatform and no Platform found
	 */
	public DecodedMessage(RawMessage rawMessage, boolean requirePlatform)
		throws UnknownPlatformException
	{
		loggerDateFmt.setTimeZone(TimeZone.getTimeZone("UTC"));
		timeAdjustmentMadeToBeginTime = false;
		this.rawMessage = rawMessage;
		firstSample = null;

		messageTime = rawMessage.getTimeStamp();
		if (messageTime == null)
			messageTime = new Date();

		TransportMedium tm = null;
		if (requirePlatform)
		{
			tm = rawMessage.getTransportMedium(); // throws if none.
			this.platform = rawMessage.getTransportMedium().platform;
		}
		else
			this.platform = null;

		timeSeriesArray = new ArrayList<TimeSeries>();
		if (this.platform != null)
		{
			// Construct time-series array for each sensor
			PlatformConfig pc = platform.getConfig();
			if (pc == null)
			{
				String msg = "Decoding failed - no config for platform '"
					+ platform.getDisplayName() + "' configName=" + platform.getConfigName()
					+ ", dcpaddr=" + tm.getMediumId();
				Logger.instance().warning(msg);
				Logger.instance().warning("Header: '" + new String(rawMessage.getHeader() + "'"));
				throw new UnknownPlatformException("The config must be saved before decoding. "
					+ "Please exit the 'Edit Decoding Script' dialog "
					+ "and press Commit. Then re-enter "
					+ "the dialog to decode your sample message.");
			}
			for (Iterator<ConfigSensor> it = pc.getSensors(); it.hasNext();)
			{
				// Find the individual sensor objects:
				ConfigSensor configSensor = it.next();
				int snum = configSensor.sensorNumber;
				ScriptSensor scriptSensor = tm.getDecodesScript().getScriptSensor(snum);
				PlatformSensor platformSensor = platform.getPlatformSensor(snum);

				// Construct a new time series to hold the data:
				TimeSeries ts = new TimeSeries(configSensor.sensorNumber);
				timeSeriesArray.add(ts);
				ts.setSensor(new Sensor(configSensor, scriptSensor, platformSensor, platform));

				// Set the time-interval for all fixed-interval sensors.
				if (configSensor.recordingMode == Constants.recordingModeFixed)
					ts.setTimeInterval(configSensor.recordingInterval);
				else
					ts.setTimeInterval(0);

				/*
				 * Data order can be controlled in several places, in the
				 * following order of rising precedence: Platform's
				 * EquipmentModel DataOrder or TimeOrder property
				 * TransportMedium's EquipmentModel DataOrder or TimeOrder prop
				 * DecodesScript dataOrder value ConfigSensor DataOrder or
				 * TimeOrder property PlatformSensor DataOrder or TimeOrder
				 * property
				 */
				String orderString = null;
				String s = null;

				// Platform's EquipmentModel
				EquipmentModel em = pc.equipmentModel;
				if (em != null)
				{
					s = PropertiesUtil.getIgnoreCase(em.properties, "DataOrder");
					if (s == null)
						s = PropertiesUtil.getIgnoreCase(em.properties, "TimeOrder");
					if (s != null)
						orderString = s;
				}

				// TransportMedium's EquipmentModel
				em = tm.equipmentModel;
				if (em != null)
				{
					s = PropertiesUtil.getIgnoreCase(em.properties, "DataOrder");
					if (s == null)
						s = PropertiesUtil.getIgnoreCase(em.properties, "TimeOrder");
					if (s != null)
						orderString = s;
				}

				// DecodesScript dataOrder value
				// DecodesScript ds = pc.getScript(tm.scriptName);
				// Josue added this line to fix the Data Order
				// problem June 14, 07
				DecodesScript ds = tm.getDecodesScript();
				if (ds != null)
				{
					char c = ds.getDataOrder();
					if (c != Constants.dataOrderUndefined)
						orderString = "" + c;
				}

				// ConfigSensor property
				if ((s = PropertiesUtil.getIgnoreCase(configSensor.getProperties(), "DataOrder")) != null
					|| (s = PropertiesUtil.getIgnoreCase(configSensor.getProperties(), "TimeOrder")) != null)
					orderString = s;

				// PlatformSensor property
				if (platformSensor != null
					&& ((s = platformSensor.getProperty("DataOrder")) != null 
						|| (s = platformSensor.getProperty("TimeOrder")) != null))
					orderString = s;

				if (orderString != null)
				{
					char c = orderString.charAt(0);
					if (c == 'a' || c == 'A')
						ts.setDataOrder(Constants.dataOrderAscending);
					else if (c == 'd' || c == 'D')
						ts.setDataOrder(Constants.dataOrderDescending);
					else
						ts.setDataOrder(Constants.dataOrderUndefined);
				}
			}

			int ta = tm.getTimeAdjustment();
			if (ta != 0)
				messageTime = new Date(messageTime.getTime() + (ta * 1000));
		}

		/*
		 * Initialize time zone, preferrably to the value stored in the
		 * transport medium, else the one stored in the site record. Else,
		 * defaults to UTC.
		 */
		tzName = "UTC";
		if (platform != null)
		{
			if (tm != null && tm.getTimeZone() != null)
				tzName = tm.getTimeZone();
			else
			{
				Site site = platform.getSite();
				if (site != null && site.timeZoneAbbr != null && site.timeZoneAbbr.length() > 0)
					tzName = site.timeZoneAbbr;
			}
		}
		currentTime = new RecordedTimeStamp(tzName);

		/*
		 * Some EDL files have a BEGIN DATE setting. If this is one, initialize
		 * the 'currentTime' to the supplied value.
		 */
		Variable v = rawMessage.getPM(EdlPMParser.BEGIN_TIME_STAMP);
		if (v != null)
		{
			try
			{
				currentTime.setComplete(v.getDateValue());
			}
			catch (NoConversionException ex)
			{ /* won't happen */
			}
		}

	}

	/**
	 * Some protocols, like Ott message, require setting the message time
	 * explicitly from the encoded data.
	 */
	public void setMessageTime(Date mt)
	{
		this.messageTime = mt;
		
		// If the time is explicitly set, any prior truncation operation is
		// invalid and needs to be cleared.
		if (timeWasTruncated)
		{
			untruncatedMessageTime = null;
			timeWasTruncated = false;
		}
	}

	/**
	 * Gets TimeSeries object for particular sensor.
	 * 
	 * @param sensorNumber
	 *            the sensor number
	 * @return TimeSeries or null if no match
	 */
	public TimeSeries getTimeSeries(int sensorNumber)
	{
		for (Iterator<TimeSeries> it = timeSeriesArray.iterator(); it.hasNext();)
		{
			TimeSeries ts = it.next();
			if (ts.getSensorNumber() == sensorNumber)
				return ts;
		}
		return null;
	}
	
	/**
	 * @return number of time series herein.
	 */
	public int getNumTimeSeries() { return timeSeriesArray.size(); }

	/**
	 * Gets TimeSeries object by data-type. Returns the first time-series in the
	 * message with a matching data type.
	 * 
	 * @param dataType
	 *            the data type code
	 * @return TimeSeries or null if no match
	 */
	public TimeSeries getTimeSeries(String dataType)
	{
		for (Iterator<TimeSeries> it = timeSeriesArray.iterator(); it.hasNext();)
		{
			TimeSeries ts = it.next();
			if (ts.hasDataType(dataType))
				return ts;
		}
		return null;
	}

	/**
	 * From IDataCollection interface, returns an iterator for all ITimeSeries
	 * objects stored in this message.
	 * 
	 * @return Iterator to time series, or null if this message is empty.
	 */
	public Iterator<TimeSeries> getAllTimeSeries()
	{
		if (timeSeriesArray == null)
			return null;
		return timeSeriesArray.iterator();
	}

	/**
	 * From IDataCollection interface.
	 * 
	 * @param sensorId
	 *            the sensor number
	 * @return generic interface to a time series.
	 */
	public ITimeSeries getITimeSeries(int sensorId)
	{
		return getTimeSeries(sensorId);
	}

	/**
	 * From IDataCollection interface, removes an interface to a time series.
	 * 
	 * @param ts
	 *            the time series to remove
	 */
	public void rmTimeSeries(ITimeSeries ts)
	{
		timeSeriesArray.remove(ts);
	}

	public void rmAllTimeSeries()
	{
		timeSeriesArray.clear();
	}
	
	/**
	 * Called when message is finished to examine all time series and upgrade
	 * partial time values to complete values using defaults. Example, if times
	 * were only specified to the month and day but not year, this method will
	 * upgrade each stored time, assuming the current year.
	 */
	public void upgradeStoredTimes()
	{
		int newstat = currentTime.getStatus();
		int curDOY = currentTime.getCalendar().get(Calendar.DAY_OF_YEAR);
		int curYear = currentTime.getCalendar().get(Calendar.YEAR);
		int curMonth = currentTime.getCalendar().get(Calendar.MONTH);
		int curDayOfMonth = currentTime.getCalendar().get(Calendar.DAY_OF_MONTH);
		boolean haveYDay = currentTime.getHaveYDay();

		// Make a working calendar in right TZ to do the arithmetic.
		GregorianCalendar cal = new GregorianCalendar(currentTime.getCalendar().getTimeZone());

		// Logger.instance().info("upgradeStoredTimes current time = " + cal);
		for (Iterator<TimeSeries> it = timeSeriesArray.iterator(); it.hasNext();)
		{
			int lastDOY = -1;
			int dayIncrement = 0;
			TimeSeries ts = it.next();
			int n = ts.size();
			if (n == 0)
			{
				// Logger.instance().debug3("Skipping empty time-series "
				// + ts.getDisplayName());
				continue; // empty time series.
			}

			// Only upgrade time series with partial dates.
			if (ts.timeStatus != RecordedTimeStamp.TIME_OF_DAY
				&& ts.timeStatus != RecordedTimeStamp.TIME_OF_YEAR)
			{
//				Logger.instance().debug3(
//					"Skipping time-series " + ts.getDisplayName() + " with time status="
//						+ ts.timeStatus);
				continue;
			}

			Logger.instance().debug3(
				"Time Series '" + ts.getDisplayName() + "' has partial times (status="
					+ ts.timeStatus + ", curDOY=" + curDOY + ", curYear=" + curYear + ", newstat="
					+ newstat + ")");

			for (int i = 0; i < n; i++)
			{
				// Get partial time in 1970 from sample.
				TimedVariable tv = ts.sampleAt(i);
				Date d = tv.getTime();
				cal.setTime(d);
				int sampDOY = cal.get(Calendar.DAY_OF_YEAR);
				int sampDayOfMonth = cal.get(Calendar.DAY_OF_MONTH);
				int sampMonth = cal.get(Calendar.MONTH);
				if (ts.timeStatus == RecordedTimeStamp.TIME_OF_DAY)
				{
					if (sampDOY != lastDOY)
					{
						if (lastDOY != -1)
						{
							dayIncrement = sampDOY - lastDOY;
						}
						lastDOY = sampDOY;
					}
					if (newstat == RecordedTimeStamp.TIME_OF_YEAR)
					{
						cal.set(Calendar.DAY_OF_YEAR, curDOY);
						if (dayIncrement != 0)
						{
							if ((ts.isAscending() && dayIncrement > 0)
								|| (!ts.isAscending() && dayIncrement < 0))
								cal.add(Calendar.DAY_OF_MONTH, dayIncrement);
						}
					}
					else if (newstat == RecordedTimeStamp.COMPLETE)
					{
						cal.set(Calendar.DAY_OF_YEAR, curDOY);
						cal.add(Calendar.DAY_OF_MONTH, dayIncrement);
						/*
						 * if (sampMonth > curMonth || (sampMonth == curMonth &&
						 * sampDayOfMonth > curDayOfMonth))
						 * cal.set(Calendar.YEAR, curYear-1); else
						 */
						cal.set(Calendar.YEAR, curYear);
					}
				}
				else if (ts.timeStatus == RecordedTimeStamp.TIME_OF_YEAR
					&& newstat == RecordedTimeStamp.COMPLETE)
				{
					// This accounts for a year rollover.
					// Example: current date is Jan 3, 2008, and samp-date is
					// Dec 28,
					// assume it was for the previous year.
					if ((haveYDay && sampDOY > curDOY)
						|| (!haveYDay && (sampMonth > curMonth || (sampMonth == curMonth && sampDayOfMonth > curDayOfMonth))))
						cal.set(Calendar.YEAR, curYear - 1);
					else
						cal.set(Calendar.YEAR, curYear);

					// MJM 11/6/2008 - Handle leap year by re-setting the date.
					// Year 1970 was NOT a leap year. the sample-year might be.
					if (haveYDay)
						cal.set(Calendar.DAY_OF_YEAR, sampDOY);
					else
					{
						cal.set(Calendar.MONTH, sampMonth);
						cal.set(Calendar.DAY_OF_MONTH, sampDayOfMonth);
					}
				}
				Date newTime = cal.getTime();
				Logger.instance().debug3(
					"Updating sample time '" + tv.getTime() + "' to '" + newTime + "'");
				tv.setTime(newTime);
			}
			Logger.instance().debug3("New time status = " + newstat);
			ts.timeStatus = newstat;
		}
	}

	/** Post processing for message time stamps from partial date/times. */
	public void finishMessage()
	{
		int curStat = currentTime.getStatus();
		Logger.instance().debug2(
			"Finishing Message: final time = '" + currentTime.getCalendar().getTime()
				+ "', status=" + curStat + ", firstSample = " + firstSample);

		/*
		 * If we didn't get complete date/time in message data, see if we can
		 * fill in missing values from header END TIME values. Otherwise, we
		 * leave the date in 1970.
		 */
		if (curStat != RecordedTimeStamp.COMPLETE && firstSample != null)
		{
			GregorianCalendar calCurrent = new GregorianCalendar(currentTime.getCalendar()
				.getTimeZone());
			calCurrent.setTime(currentTime.getTime());
			GregorianCalendar calEnd = new GregorianCalendar(currentTime.getCalendar()
				.getTimeZone());
			Date endTime = new Date();
			if (curStat == RecordedTimeStamp.NOTHING)
				currentTime.getCalendar().setTime(endTime);
			else
			{
				Variable v = rawMessage.getPM(EdlPMParser.END_TIME_STAMP);
				if (v == null)
					v = rawMessage.getPM(GoesPMParser.MESSAGE_TIME);
				if (v != null)
				{
					try
					{
						endTime = v.getDateValue();
					}
					catch (NoConversionException ex)
					{
						/* Shouldn't happen */
						Logger.instance().warning("Internal decoder error: no end time.");
						return;
					}
				}
				calEnd.setTime(endTime);
				calCurrent.set(Calendar.YEAR, calEnd.get(Calendar.YEAR));
				if (curStat == RecordedTimeStamp.TIME_OF_DAY)
					calCurrent.set(Calendar.DAY_OF_YEAR, calEnd.get(Calendar.DAY_OF_YEAR));
				currentTime.getCalendar().setTime(calCurrent.getTime());
			}
			currentTime.setComplete();
			upgradeStoredTimes();
		}
		else
			Logger.instance().debug3("No need to fix dates.");

		if (currentTime.isTzManual() && firstSample != null)
		{
			/*
			 * The 'manual' setting means that we determine the daylight-flag
			 * for the entire file (message) from the time of the first sample.
			 * So, we just loaded all sample values with daylight turned off.
			 * Determine if 1st sample is in daylight. If so, add 1 hr to all
			 * sample times in all time series.
			 */

			// Construct timezone WITH daylight time.
			int idx = tzName.lastIndexOf('M');
			if (idx == -1)
				return;
			String dsttzn = tzName.substring(0, idx);
			dsttzn.trim();
			RecordedTimeStamp dstrc = new RecordedTimeStamp(dsttzn);
			TimeZone dsttz = dstrc.getTimeZone();

			// Is 1st sample in DST?
			if (dsttz.inDaylightTime(firstSample.getTime()))
			{
				for (Iterator<TimeSeries> it = timeSeriesArray.iterator(); it.hasNext();)
				{
					TimeSeries ts = it.next();
					int n = ts.size();
					if (n == 0)
						continue; // empty time series.
					for (int i = 0; i < n; i++)
					{
						TimedVariable tv = ts.sampleAt(i);
						Date d = tv.getTime();
						d.setTime(d.getTime() - 3600000L);
						tv.setTime(d);
					}
				}
			}
		}

		/*
		 * Apply time offset in Transport Medium to all times in each Time
		 * Series
		 */
		if (!timeAdjustmentMadeToBeginTime)
		{
			/*
			 * For non-GOES transmissions and files that have no BEGIN TIME set
			 * in the header, the adjustment has already been made
			 */
			/* Get time Adjustment specified in Transport Medium */
			int tmOffset = 0;
			TransportMedium tm = null;
			try
			{
				tm = rawMessage.getTransportMedium();
			}
			catch (UnknownPlatformException ex)
			{
				tm = null;
			}
			if (tm != null)
				tmOffset = tm.getTimeAdjustment();
			if (tmOffset != 0)
			{
				/* Apply offset */
				for (TimeSeries ts : timeSeriesArray)
				{
					int n = ts.size();
					if (n == 0)
						continue; // empty time series.
					ts.addTimeOffset(tmOffset);
				}
			}
		}
		
		// Apply any sensor "TimeOffsetSec" properties.
		for (TimeSeries ts : timeSeriesArray)
		{
			if (ts.size() == 0)
				continue;
			Sensor sensor = ts.getSensor();
			if (sensor == null)
				continue;
			String tos = ts.getSensor().getProperty("TimeOffsetSec");
			if (tos == null)
				continue;
			try
			{
				int to = Integer.parseInt(tos.trim());
				ts.addTimeOffset(to);
			}
			catch(Exception ex)
			{
				Logger.instance().warning("Sensor "
					+ sensor.getNumber() + " " + sensor.getName()
					+ " has bad TimeOffsetSec property '" + tos 
					+ "' must be integer. Ingored.");
			}
		}
	}

	/**
	 * @return the RawMessage used to decode this message.
	 */
	public RawMessage getRawMessage()
	{
		return rawMessage;
	}

	/**
	 * @return the RecordedTimeStamp associated with the message.
	 */
	public RecordedTimeStamp getTimer()
	{
		return currentTime;
	}

	/**
	 * Adds a sample using the current message time-stamp
	 * 
	 * @param sensorNumber the sensor number identifying the time series.
	 * @param v the sample value.
	 * @param lineNum the line number from which this was taken.
	 * @return the timed variable added to the specified time series, or null if error.
	 */
	public TimedVariable addSample(int sensorNumber, Variable v, int lineNum) 
	{
		if (sensorNumber < 1)
		{
			Logger.instance().warning("addSample() Invalid sensor number " + sensorNumber);
			return null;
		}

		TimeSeries ts = getTimeSeries(sensorNumber);
		if (ts == null)
		{
			Logger.instance().warning(
				"In platform " + platform.makeFileName()
					+ " Cannot add sample -- no time series for sensor " + sensorNumber);
			return null;
		}

		int tint = ts.getTimeInterval();
		boolean fixedInterval = tint != 0 && tint != -1;
		long msec = currentTime.getMsec();
		Date sampTime = new Date(msec);
		TimeZone tz = currentTime.getCalendar().getTimeZone();
		GregorianCalendar cal1 = new GregorianCalendar(tz);
		cal1.setTime(sampTime);
		Logger.instance().debug3(
			"addSample curTime=" + loggerDateFmt.format(sampTime) + ", status="
				+ currentTime.getStatus());

		if (currentTime.getStatus() == RecordedTimeStamp.NOTHING)
		{
			// This is the usual case for DCPs -- no embedded time in message.
			timeAdjustmentMadeToBeginTime = true;
			if (fixedInterval) // Fixed Interval Sensor?
			{
				if (ts.size() == 0) // This is first sample for this sensor?
					sampTime = ts.timeOfLastSampleBefore(messageTime);
				else if (!ts.isAscending())
				{
					// Descending, this time is last one minus interval.
					sampTime = new Date(ts.timeOfLastSampleInSeries().getTime()
						- (ts.getTimeInterval() * 1000L));
				}
				else
				// Ascending
				{
					ts.adjustAllTimesBackByInterval();
					sampTime = ts.timeOfLastSampleBefore(messageTime);
				}
			}
			else
				// Variable interval sensor
				sampTime = messageTime;
		}
		else
		// we have either TIME_OF_DAY, TIME_OF_YEAR, or COMPLETE time
		{
			if (ts.size() == 0 || !fixedInterval)
			{ // This is first sample for this sensor?
				sampTime = cal1.getTime();
			}
			else if (fixedInterval)
			{
				Logger.instance().debug3(
					"Adding fixed interval sensor, msec=" + msec + ", msecAtLastAdd="
						+ ts.msecAtLastAdd + ", isAscending=" + ts.isAscending());

				// Has time changed since last sample for this TS?
				// AND we haven't just set time with a date/time Field operator.
				// This is for cases like:
				// date/time HG HG HG HG
				// The date/time applies to the first HG, subsequent ones are
				// adjusted
				// by the interval.
				if (msec == ts.msecAtLastAdd && !ts.timeJustSet())
				{
					int sint = ts.getTimeInterval();
					Date lastSample = ts.timeOfLastSampleInSeries();
					long tls = lastSample.getTime();

					if (ts.isAscending())
						// Ascending, this time is last one minus interval.
						sampTime = new Date(tls + sint * 1000L);
					else
						// Descending, this time is last one minus interval.
						sampTime = new Date(tls - sint * 1000L);
					Logger.instance().debug3(
						"Running Series: sint=" + sint + " lastSampleTime="
							+ loggerDateFmt.format(lastSample) + ", thisSampleTime="
							+ loggerDateFmt.format(sampTime));
				}
				else if (!justGotFullDateTime)
				// We got a new time value.
				{
					if (ts.isAscending() && msec < ts.msecAtLastAdd)
					{
						// ascending, but time is before the previous: we
						// wrapped

						// Get day-of-year for last value added to this TS.
						Date last = ts.timeOfLastSampleInSeries();
						cal1.setTime(last);
						int lastDay = cal1.get(Calendar.DAY_OF_YEAR);
						cal1.setTime(sampTime); // Reset to current time.

						// If HHMMSS wrapped but day is same, increment day.
						if (lastDay == cal1.get(Calendar.DAY_OF_YEAR))
						{
							Logger.instance().debug3("HHMMSS wrapped, incrementing day.");
							cal1.add(Calendar.DAY_OF_MONTH, +1);
							currentTime.getCalendar().add(Calendar.DAY_OF_MONTH, +1);
						}
						else
						// Day must also have wrapped, increment year.
						{
							Logger.instance().debug3("Day wrapped, incrementing year.");
							cal1.add(Calendar.YEAR, +1);
							currentTime.incrementYear();
						}
						sampTime = cal1.getTime();
					}
					// GC's comment on 2010/06/20
					// Modified the following code with changing
					// !ts.isAscending() to ts.isDescending()
					// in order for user to use the Undefined data order.
					// Gang Chen's proposed mods, appears to break regression
					// test 14211820.
					// else if (ts.isDescending()
					// && msec > ts.msecAtLastAdd
					// && ts.msecAtLastAdd != Long.MIN_VALUE)
					// ORIGINAL CODE:
					else if (!ts.isAscending() && msec > ts.msecAtLastAdd
						&& ts.msecAtLastAdd != Long.MIN_VALUE)
					{
						// descending but time is after the previous: we wrapped

						// Get day-of-year for last value added to this TS.
						Date last = ts.timeOfLastSampleInSeries();
						cal1.setTime(last);
						int lastDay = cal1.get(Calendar.DAY_OF_YEAR);
						cal1.setTime(sampTime); // Reset to current time.

						// If HHMMSS wrapped but day is same, decrement day.
						if (lastDay == cal1.get(Calendar.DAY_OF_YEAR))
						{
							cal1.add(Calendar.DAY_OF_MONTH, -1);
							currentTime.getCalendar().add(Calendar.DAY_OF_MONTH, -1);
						}
						else
						// day also wrapped, decrement year
						{
							cal1.add(Calendar.YEAR, -1);
							currentTime.decrementYear();
						}
						sampTime = cal1.getTime();
					}
					// else we leave sampTime alone -- defaults to current time.

				}
			}
		}
		TimedVariable tv = new TimedVariable(v);
		tv.setTime(sampTime);
		tv.setLineNumber(lineNum);
		ts.addSample(tv);
		ts.msecAtLastAdd = msec;
		Logger.instance().debug3(
			"Added sample for sensor " + sensorNumber + " at time "
				+ loggerDateFmt.format(sampTime) + ", value=" + v.getStringValue());
		ts.timeStatus = currentTime.getStatus();
		if (firstSample == null)
			firstSample = tv;
		justAddedSample = true;
		return tv;
	}

	/**
	 * Adds a sample/time to a sensor without affecting the message's Recorded
	 * time. Ott requires this.
	 * 
	 * @param sensorNumber
	 * @param v
	 * @param hardTime
	 */
	public void addSampleWithTime(int sensorNumber, Variable v, Date hardTime, int lineNum)
	{
		if (sensorNumber < 0)
			return;
		TimeSeries ts = getTimeSeries(sensorNumber);
		if (ts == null)
		{
			Logger.instance().warning(
				"In platform " + platform.makeFileName()
					+ " Cannot add sample -- no time series for sensor " + sensorNumber);
			return;
		}
		TimedVariable tv = new TimedVariable(v, hardTime);
		tv.setLineNumber(lineNum);
		ts.addSample(tv);
		ts.msecAtLastAdd = hardTime.getTime();
		ts.timeStatus = RecordedTimeStamp.COMPLETE;
	}

	/**
	 * Sets the time interval for a time series.
	 * 
	 * @param sensorNumber
	 *            the sensor number identifying the time series.
	 * @param seconds
	 *            the time interval in seconds
	 */
	public void setTimeInterval(int sensorNumber, int seconds)
	{
		TimeSeries ts = getTimeSeries(sensorNumber);
		if (ts == null)
			return;
		ts.setTimeInterval(seconds);
	}

	/**
	 * Delegates to each time series to convert its values from the raw units to
	 * the inital EU's specified in the script.
	 */
	public void applyInitialEuConversions()
	{
		for (Iterator<TimeSeries> it = timeSeriesArray.iterator(); it.hasNext();)
		{
			TimeSeries ts = it.next();
			ts.convertUnits();
		}
	}

	/**
	 * Uses a PresentationGroup to format samples according to rounding rules
	 * and EU conversions.
	 * 
	 * @param pg
	 *            the PresentationGroup, null if none
	 */
	public void formatSamples(PresentationGroup pg)
	{
		Logger.instance().debug3(
			"Formatting Samples, pg=" + (pg == null ? "null" : pg.getDisplayName())
			+ ", there are " + (timeSeriesArray==null ? "null" : 
				(""+timeSeriesArray.size())) + " time series in the message.");
		if (timeSeriesArray == null)
			return;
		Vector<TimeSeries> omit = new Vector<TimeSeries>();
		for (TimeSeries ts : timeSeriesArray)
		{
			Sensor sensor = ts.getSensor();
			if (ts.size() == 0)
			{
				Logger.instance().debug3("Skipping empty time series for sensor " 
					+ (sensor==null ? "null" : sensor.getName()));
				continue;
			}
			String p = sensor.getProperty("omit");
			if (p != null && TextUtil.str2boolean(p))
			{
				Logger.instance().debug3(" will omit sensor " + sensor.getName()
					+ " due to omit property");
				omit.add(ts);
				continue;
			}
			if (pg != null)
			{
				DataPresentation dp = pg.findDataPresentation(sensor);
				if (dp != null)
				{
					if (dp.getUnitsAbbr() != null && dp.getUnitsAbbr().equalsIgnoreCase("omit"))
					{
						Logger.instance()
							.log(
								Logger.E_DEBUG2,
								"Omitting sensor '" + sensor.getName()
									+ "' as per Presentation Group.");
						omit.add(ts);
					}
					else
					{
						ts.formatSamples(dp);
					}
				}
			}
			else Logger.instance().debug3(" No pg element for sensor " 
				+ sensor.getName() + " with dt=" + sensor.getDataType());
			Season ignoreSeason = sensor.getIgnoreSeason();
			if (ignoreSeason == null)
				ignoreSeason = platform.getIgnoreSeason();
			Season processSeason = sensor.getProcessSeason();
			if (processSeason == null)
				processSeason = platform.getProcessSeason();
			if (ignoreSeason != null || processSeason != null)
			{
				for(int idx = 0; idx < ts.size(); )
				{
					Date d = ts.sampleAt(idx).getTime();
					if ((ignoreSeason != null && ignoreSeason.isInSeason(d))
					 || (processSeason != null && !processSeason.isInSeason(d)))
						ts.deleteSampleAt(idx);
					else
						idx++;
				}
			}
		}
		for (Iterator<TimeSeries> it = omit.iterator(); it.hasNext();)
			timeSeriesArray.remove(it.next());
		presentationGroupApplied = pg;
	}

	/**
	 * Applies calculations, if any, all time series.
	 */
	public void applyScaleAndOffset()
	{
		for (Iterator<TimeSeries> it = timeSeriesArray.iterator(); it.hasNext();)
		{
			TimeSeries ts = it.next();
			Sensor sensor = ts.getSensor();
			if (sensor == null)
				continue;
			String p = sensor.getProperty("preoffset");
			if (p != null)
			{
				try
				{
					double d = Double.parseDouble(p);
					ts.addToSamples(d);
				}
				catch (NumberFormatException ex)
				{
					Logger.instance().log(
						Logger.E_WARNING,
						"Platform " + rawMessage.getMediumId()
							+ " Invalid preoffset property in sensor " + ts.getSensorNumber()
							+ " '" + p + "' -- ignored.");
				}
			}
			p = sensor.getProperty("scale");
			if (p != null)
			{
				try
				{
					double d = Double.parseDouble(p);
					ts.multiplySamplesBy(d);
				}
				catch (NumberFormatException ex)
				{
					Logger.instance().log(
						Logger.E_WARNING,
						"Platform " + rawMessage.getMediumId()
							+ " Invalid scale property in sensor " + ts.getSensorNumber() + " '"
							+ p + "' -- ignored.");
				}
			}
			p = sensor.getProperty("offset");
			if (p != null)
			{
				try
				{
					double d = Double.parseDouble(p);
					ts.addToSamples(d);
				}
				catch (NumberFormatException ex)
				{
					Logger.instance().log(
						Logger.E_WARNING,
						"Platform " + rawMessage.getMediumId()
							+ " Invalid offset property in sensor " + ts.getSensorNumber() + " '"
							+ p + "' -- ignored.");
				}
			}
		}
	}

	/**
	 * Applies specified limits to all time series.
	 */
	public void applySensorLimits()
	{
		for (Iterator<TimeSeries> it = timeSeriesArray.iterator(); it.hasNext();)
		{
			TimeSeries ts = it.next();
			ts.applySensorLimits();
		}
	}

	/**
	 * Removes redundant data from the message's time-series. Done by
	 * calculating the previous message time for this transport medium,
	 * including the specified time-adjustment value. Then, discarding all
	 * samples with time-stamps before this value.
	 */
	public void removeRedundantData()
	{
		if (platform == null || timeSeriesArray == null || rawMessage == null || messageTime == null)
			return;
		try
		{
			TransportMedium tm = rawMessage.getTransportMedium();
			if (tm.transmitInterval <= 0)
				return;

			// Note: timeAdjustment already incorporated in messageTime.
			long prevMsgMsec = messageTime.getTime() - (tm.transmitInterval * 1000L);
			for (TimeSeries ts : timeSeriesArray)
				ts.discardSamplesBefore(prevMsgMsec);
		}
		catch (UnknownPlatformException ex)
		{
			return;
		}
	}

	/*
	 * From IDataCollection interface, creates a new time series in this
	 * message.
	 * 
	 * @param sensorId the sensor number uniquely identifying the time series.
	 * 
	 * @param name the sensor name (need not be unique)
	 */
	public ITimeSeries newTimeSeries(int sensorId, String name)
	{
		TimeSeries ts = new TimeSeries(sensorId);
		PlatformSensor ps = new PlatformSensor(getPlatform(), sensorId);
		ConfigSensor cs = new ConfigSensor(null, sensorId);
		cs.sensorName = name;
		ts.setSensor(new Sensor(cs, null, ps, platform));
		timeSeriesArray.add(ts);
		return ts;
	}

	public void addTimeSeries(TimeSeries ts)
	{
		timeSeriesArray.add(ts);
	}

	/**
	 * @return the platform associated with this message, or null if undefined.
	 */
	public Platform getPlatform()
	{
		return platform;
	}

	/**
	 * @param platform
	 *            the platform to set
	 */
	public void setPlatform(Platform platform)
	{
		this.platform = platform;
	}

	/**
	 * @return the PresentationGroup applied to this message or null if none has
	 *         been.
	 */
	public PresentationGroup getPresentationGroup()
	{
		return presentationGroupApplied;
	}

	public Date getMessageTime()
	{
		return messageTime;
	}

	// The following 2 methods are to fix a problem where the special auto-
	// increment cases are messing up the simple case like this:
	// 106,2008,218,1200,209.32,202.81,185.54,31.427,12.268
	// 106,2008,218,1300,209.31,202.81,185.58,33.9,12.306
	// 106,2008,218,1400,209.31,202.81,185.59,34.705,12.305
	// 106,2008,218,1300,130.32,202.81,185.58,33.9,12.306
	// 106,2008,218,1400,140.31,202.81,185.59,34.705,12.305
	// 106,2008,218,1500,150.31,200.00,185.63,33.23,12.299
	// In these cases a full date/time is given followed by a series of data
	// samples.
	// Note the duplicate data on the 4th & 5th data line. This was causing
	// auto-increment
	// to get the sample times wrong.
	// The code now turns off auto-incrementing when we get a YEAR field
	// (or a DATE field containing a year). It stays off until we get a NON-YEAR
	// DATE/TIME field following some data.

	public void setJustGotFullDateTime(boolean justGotFullDateTime)
	{
		this.justGotFullDateTime = justGotFullDateTime;
		justAddedSample = false;
		for (TimeSeries ts : timeSeriesArray)
			ts.setTimeJustSet();
	}

	/** Called when we parse a non-year time/date field */
	public void justGotNonYearField()
	{
		if (justAddedSample)
			justGotFullDateTime = false;
		for (TimeSeries ts : timeSeriesArray)
			ts.setTimeJustSet();
	}

	public ArrayList<TimeSeries> getTimeSeriesArray()
	{
		return timeSeriesArray;
	}
	
	/** This should be passed a time that has already been truncated
	 * appropriately.
	 * Do not use the return from getMessageTime() without making a copy first.
	 */
	void truncateTime(Date time)
	{
		untruncatedMessageTime = messageTime;
		messageTime = time;
		timeWasTruncated = true;
	}
	
	Date getUntruncatedMessageTime()
	{
		return timeWasTruncated ? untruncatedMessageTime : messageTime;
	}
}
