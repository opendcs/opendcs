/*
*  $Id$
*/
package decodes.decoder;

import java.util.Vector;
import java.util.Date;
import java.util.Collections;
import java.util.Iterator;
import java.text.NumberFormat;

import ilex.var.TimedVariable;
import ilex.var.NoConversionException;
import ilex.var.IFlags;
import ilex.util.Logger;

import decodes.db.Database;
import decodes.db.EngineeringUnit;
import decodes.db.Constants;
import decodes.db.UnitConverterDb;
import decodes.db.UnitConverter;
import decodes.db.DatabaseException;
import decodes.db.DataPresentation;
import decodes.db.DataType;
import decodes.util.DecodesException;

/**
This holds a series of timed samples for a single sensor on a single
platform. It also contains references to meta-data from the DECODES
database.
*/
public class TimeSeries
	implements decodes.comp.ITimeSeries
{
	/** Relation to Sensor objects */
	private int sensorNumber;
    /** EU-converted samples extracted from message */
	private Vector<TSSample> samples;
	/** Composite object pointing to sensor data. */
	private Sensor sensor;
	/** Engineering Units for these samples */
	private EngineeringUnit eu;

	/** Used to EU convert raw samples */
	private UnitConverter converter;  
	/** # seconds between samples */
	private int timeInterval;   
	/** 'A'=ascending, 'D' = descending */
	private char dataOrder;     

	/** dummy EU used if none is supplied */
	private static EngineeringUnit unknownEU
		= EngineeringUnit.getEngineeringUnit("unknown");

	/** Constant defined int RecordedTimeStamp; */
	public int timeStatus;

	/** Msec value from last call to addSample, without adjustments, if any. */
	public long msecAtLastAdd;

	/** Msec value from first call to addSample, without adjustments, if any. */
    public long msecAtFirstAdd;

	private static NumberFormat defaultNumberFormat;
	static
	{
		defaultNumberFormat = NumberFormat.getNumberInstance();
		defaultNumberFormat.setMaximumFractionDigits(3);
		defaultNumberFormat.setGroupingUsed(false);
	}
	
	private boolean _timeJustSet = false;

	/**
	  Constructor.
	  @param sensorNumber the sensor number -- every time series must have a
	  unique sensor number.
	*/
	public TimeSeries(int sensorNumber)
	{
		this.sensorNumber = sensorNumber;
//		beginTime = new Date();
//		endTime = new Date(0L);
		samples = new Vector<TSSample>();
		sensor = null;
		eu = unknownEU;
		converter = null;
		timeInterval = 0;
		dataOrder = Constants.dataOrderUndefined;
		timeStatus = RecordedTimeStamp.NOTHING;
		msecAtLastAdd = Long.MIN_VALUE;
		msecAtFirstAdd = Long.MIN_VALUE;
	}

	/** @return sensor number */
	public int getSensorNumber() { return sensorNumber; }

	/** @return sensor object */
	public Sensor getSensor() { return sensor; }

	/** 
	  Sets sensor object.
	  @param sensor the Sensor object in the DECODES database
	*/
	public void setSensor(Sensor sensor) 
	{
		this.sensor = sensor; 

		// If EU is not defined, try to determine it from script sensor.
		if (eu == unknownEU)
		{
			if (sensor.scriptSensor == null)
			{
				Logger.instance().debug1(
					"No EU assignment and no script sensor defined for '"
					+ sensor.getName() + "'");
			}
			else if (sensor.scriptSensor.rawConverter == null)
			{
				Logger.instance().debug1(
					"No EU assignment and no raw converter defined for '"
					+ sensor.getName() + "'");
			}
			else
			{
				UnitConverterDb ucdb = sensor.scriptSensor.rawConverter;
				if (!ucdb.isPrepared())
				{
					try 
					{
						Logger.instance().log(Logger.E_DEBUG3,
							"preparing unit converter for raw to " + ucdb.toAbbr);
						ucdb.prepareForExec(); 
					}
					catch(DatabaseException e)
					{
					
						Logger.instance().log(Logger.E_FAILURE,
							"Cannot prepare raw EU converter for sensor '"
							+ sensor.getName() + "'");
						return;
					}
				}
				converter = ucdb.execConverter;
				if (converter != null)
					eu = converter.getTo();
				else
				{
					Logger.instance().log(Logger.E_DEBUG3,
						"No converter defined for sensor '" 
						+ sensor.getName() + "'");
				}
			}
		}
	}

	/**
	  @return the Engineering Units object for this time series.
	*/
	public EngineeringUnit getEU()
	{
		return eu;
	}

	/**
	  Sets the Engineering Units object for this time series.
	  Normally the EU is set from the script sensor. However for
	  derived parameters this method allows direct assignment.
	  @param eu the EngineeringUnit
	*/
	public void setEU(EngineeringUnit eu)
	{
		this.eu = eu;
	}

	/**
	From ITimeSeries interface.
	@return units as a string.
	*/
	public String getUnits()
	{
		if (eu == null)
			return "unknown";
		else return eu.abbr;
	}

	/**
	From ITimeSeries interface, sets units as a string.
	@param eu the EU abbreviation or name.
	*/
	public void setUnits(String eu)
	{
		setEU(EngineeringUnit.getEngineeringUnit(eu));
	}

	/** @return the time interval value. */
	public int getTimeInterval()
	{
		if (sensor != null && sensor.configSensor!= null
		 && Character.toUpperCase(sensor.configSensor.recordingMode)
		 	  == Character.toUpperCase(Constants.recordingModeVariable))
			return 0;
		return timeInterval;
	}

	/** 
	Sets the time interval value. 
	@param v the time interval value
	*/
	public void setTimeInterval(int v) { timeInterval = v; }

	/**
	  Valid for fixed interval sensors only.
	  @return the time of the last sample that would be sent prior to 
	  the passed message time.
	*/
	public Date timeOfLastSampleBefore(Date msgTime)
	{
		int interval = getTimeInterval();
		if (interval == 0)
			return msgTime; // Not fixed interval sensor!

		long msec = msgTime.getTime();
		int msgSecOfDay = (int)((msec / 1000L) % (24 * 60 * 60));
		msec -= (msgSecOfDay * 1000L);
		int sod = sensor.getTimeOfFirstSample();
		if (sod > msgSecOfDay)
		{
			// Most recent sample was at the end of yesterday.
			sod -= interval; // now negative
		}
		else while(sod+interval < msgSecOfDay)
			sod += interval;

		return new Date(msec + (sod * 1000L));
	}

	/** @return the time of the last sample in the series. */
	public Date timeOfLastSampleInSeries()
	{
		int sz = size();
		if (sz == 0)
			return null;
		return timeAt(sz - 1);
	}

	/**
	Add offset ( in +/- seconds ) to every time in time series
	*/
	public void addTimeOffset(int offset)
	{
		for(Iterator<TSSample> it = samples.iterator(); it.hasNext(); )
		{
			TSSample tss = it.next();
			tss.tv.setTime(new Date(tss.tv.getTime().getTime() + (offset * 1000L)));
		}
	}
	/**
	Subtracts interval from all samples currently in series.
	*/
	public void adjustAllTimesBackByInterval()
	{
		int iv = getTimeInterval();
		for(Iterator<TSSample> it = samples.iterator(); it.hasNext(); )
		{
			TSSample tss = it.next();
			tss.tv.setTime(new Date(tss.tv.getTime().getTime() - (iv * 1000L)));
		}
	}

	/**
	  Adds a sample to the end of the series. No sorting is done in this method.
	  @param tv the time-stamped value
	*/
	public void addSample(TimedVariable tv)
	{
		samples.add(new TSSample(tv));
		_timeJustSet = false;
	}

	/**
	  @return the time-stamped value at the specified index.
	*/
	public TimedVariable sampleAt(int idx)
	{
		if (idx < 0 || idx >= samples.size())
			return null;

		return (samples.elementAt(idx)).tv;
	}
	
	/**
	 * Deletes the sample at the specified index. All subsequent samples' index 
	 * are moved up.
	 * @param idx the index within the samples array
	 * @return true if it was deleted, false if idx is out of bounds.
	 */
	public boolean deleteSampleAt(int idx)
	{
		if (idx < 0 || idx >= samples.size())
			return false;
		samples.remove(idx);
		return true;
	}

	/**
	  @return the time at the specified index.
	*/
	public Date timeAt(int idx)
	{
		return sampleAt(idx).getTime();
	}

	/**
	  @return the formated sample at the specified index.
	*/
	public String formattedSampleAt(int idx)
	{
		if (idx < 0 || idx >= samples.size())
			return null;

		TSSample tss = samples.elementAt(idx);
		if (tss.fv != null)
		{
			return tss.fv;
		}

		try 
		{
			if ((tss.tv.getFlags() & IFlags.IS_MISSING) != 0)
				return "missing";
			else if ((tss.tv.getFlags() & IFlags.IS_ERROR) != 0)
				return "error";
			if (!tss.tv.isNumeric())
				return tss.tv.getStringValue();
			return defaultNumberFormat.format(tss.tv.getDoubleValue());
		}
		catch(Exception e) { return "nodata"; }
	}

	public int size() { return samples.size(); }

/*DO NOT USE - we don't set times this way anymore.
	public void setTimeAt(int idx, Date timeStamp)
	{
		if (timeStamp.before(beginTime))
			beginTime = timeStamp;
		if (timeStamp.after(endTime))
			endTime = timeStamp;

		TimedVariable tv = sampleAt(idx);
		tv.setTime(timeStamp);
	}
*/

	/**
	  Sorts samples by time & sets beginning & ending times.
	*/
	public void sort()
	{
		sort(false);
	}
	
	public void sort(boolean descending)
	{
		if (samples.size() == 0)
			return;

		SampleComparator sc = new SampleComparator();
		sc.descending = descending;
		Collections.sort(samples, sc);
		
		// Find and remove duplicates (samples with same time)
		Date prevTime = null;
		for(Iterator<TSSample> tsit = samples.iterator(); tsit.hasNext();)
		{
			TSSample tss = tsit.next();
			if (prevTime != null && prevTime.equals(tss.tv.getTime()))
				tsit.remove();
			prevTime = tss.tv.getTime();
		}
	}

	/** 
	Returns the time of earliest sample int this time series. 
	@return Date representing time of earliest sample, or null if empty.
	*/
	public Date getBeginTime() 
	{
		if (samples.size() == 0)
			return null;
		Date d = new Date(Long.MAX_VALUE);
		for(Iterator<TSSample> it = samples.iterator(); it.hasNext(); )
		{
			TSSample tss = it.next();
			Date tvd = tss.tv.getTime();
			if (tvd.before(d))
				d = tvd;
		}
		
		return d; 
	}

//
//	/** Sets the begin time for this time series. */
//	public void setBeginTime(Date d) { beginTime = d; }
//
//	/** Returns the end time for this time series. */
//	public Date getEndTime() { return endTime; }
//
//	/** Sets the end time for this time series. */
//	public void setEndTime(Date d) { endTime = d; }

	/**
	From ITimeSeries interface, sets data order.
	@param c 'A' for ascending, 'D' for descending
	*/
	public void setDataOrder(char c) { dataOrder = Character.toUpperCase(c); }

	/** @return true if data order is ascending. */
	public boolean isAscending()
	{ return dataOrder == Constants.dataOrderAscending; }
	
	public boolean isDescending()
	{
		return dataOrder == Constants.dataOrderDescending;
	}

	/**
	  Convert the units for all values stored in this time series.
	*/
	public void convertUnits()
	{
		if (converter == null)
			return;

		for(int i=0; i<samples.size(); i++)
		{
			TimedVariable tv = sampleAt(i);
			if (!tv.isNumeric())
				continue;

			if ((tv.getFlags() & IFlags.IS_MISSING) != 0
			 || (tv.getFlags() & IFlags.IS_ERROR) != 0)
				continue;

			double v = 0.0;

			try { v = tv.getDoubleValue(); }
			catch (NoConversionException e)
			{
				Logger.instance().log(Logger.E_FAILURE,
					"Cannot EU convert a non-numeric value for sensor '"
					+ sensor.configSensor.sensorName + "'");
				continue;
			}

			try { v = converter.convert(v); }
			catch(DecodesException e)
			{
				Logger.instance().log(Logger.E_FAILURE,
					"EU Converter exception for sensor '"
					+ sensor.configSensor.sensorName + "': " + e);
				continue;
			}

			// See if this can be represented as an integer.
			long lv = (long)v;
			if ((double)lv == v)
				tv.setValue(lv);
			else
				tv.setValue(v);
		}
	}

	/**
	  Formats all samples in this time series according to presentation group.
	  Samples are unit-converted according to the PG and then formatted
	  into strings according to the rounding rules in the PG.
	  @param pg the PresentationGroup
	*/
	public void formatSamples(DataPresentation dp)
	{
		// Unit conversion.
		UnitConverter converter = null;
		EngineeringUnit dpEU = null;

		if (dp == null)
		{
			return;
		}
		else
		{
			if (dp.getUnitsAbbr() != null
			 && !dp.getUnitsAbbr().equalsIgnoreCase(eu.getAbbr()))
			{
				dpEU = EngineeringUnit.getEngineeringUnit(dp.getUnitsAbbr());
				converter = Database.getDb().unitConverterSet.get(eu, dpEU);
				if (converter == null)
				{
					Logger.instance().log(Logger.E_WARNING,
						"Cannot convert samples for '" + getDisplayName()
						+ "' from " + eu.abbr + " to " + dpEU.getAbbr());
				}
			}
		}

		for(Iterator<TSSample> it = samples.iterator(); it.hasNext(); )
		{
			TSSample tss = it.next();

			if ((tss.tv.getFlags() & IFlags.IS_MISSING) != 0)
				tss.fv = "missing";
			else if ((tss.tv.getFlags() & IFlags.IS_ERROR) != 0)
				tss.fv = "error";
			else
			{
				try
				{
					if (!tss.tv.isNumeric())
						continue;
					double x = tss.tv.getDoubleValue();
					if (converter != null)
					{
						x = converter.convert(x);
						tss.tv.setValue(x);
					}
					if (dp != null)
					{
						if (dp.getMaxValue() != Constants.undefinedDouble 
						 && x > dp.getMaxValue())
						{
							tss.tv.setFlags(IFlags.IS_ERROR);
							tss.fv = ">max";
						}
						else if (dp.getMinValue() != Constants.undefinedDouble 
							 && x < dp.getMinValue())
							{
								tss.tv.setFlags(IFlags.IS_ERROR);
								tss.fv = "<min";
							}
						else
							tss.fv = dp.applyRoundingRules(x);
					}
					else
						tss.fv = defaultNumberFormat.format(x);
				}
				catch(NoConversionException e)  // means non-numeric sample
				{
					Logger.instance().log(Logger.E_WARNING, e.toString());
				}
				catch(DecodesException e)
				{
					Logger.instance().log(Logger.E_WARNING, e.toString());
				}
			}
		}
		if (converter != null && dpEU != null)
		{
			eu = dpEU; // Units are now changed
		}
	}

	/**
	  Apply sensor min/max limits, if they are defined, by setting the 
	  'MISSING' flag on out of limits values.
	*/
	public void applySensorLimits()
	{
		if (sensor == null)
			return;

		// Retrieve the limits
		double min = sensor.getMinimum();
		double max = sensor.getMaximum();
		if (min == Constants.undefinedDouble 
		 && max == Constants.undefinedDouble)
			return;

		double minReplaceValue = Constants.undefinedDouble;
		String s = sensor.getProperty("minReplaceValue");
		if (s != null)
		{
			try { minReplaceValue = Double.parseDouble(s); }
			catch(Exception ex)
			{
				Logger.instance().warning(sensor.getDisplayName()
					+ ": Invalid minReplaceValue '" + s + "'");
				minReplaceValue = Constants.undefinedDouble;
			}
		}
		double maxReplaceValue = Constants.undefinedDouble;
		s = sensor.getProperty("maxReplaceValue");
		if (s != null)
			try { maxReplaceValue = Double.parseDouble(s); }
			catch(Exception ex)
			{
				Logger.instance().warning(sensor.getDisplayName()
					+ ": Invalid maxReplaceValue '" + s + "'");
				minReplaceValue = Constants.undefinedDouble;
			}
			
		// Step through values.
		for(Iterator<TSSample> it = samples.iterator(); it.hasNext(); )
		{
			TSSample tss = it.next();
			if (tss.tv == null)
				continue;

			// Convert value to double, skip if not a number.
			double d = 0.0;
			try { d = tss.tv.getDoubleValue(); }
			catch(NoConversionException ex) { continue; }

			if (min != Constants.undefinedDouble && d < min)
			{
				String what = "discarded";
				int f = tss.tv.getFlags() | IFlags.LIMIT_VIOLATION;
				if (minReplaceValue != Constants.undefinedDouble)
				{
					tss.tv.setValue(minReplaceValue);
					what = "replaced with " + minReplaceValue;
				}
				else
					f |= IFlags.IS_MISSING;
				tss.tv.setFlags(f);
				Logger.instance().debug1(sensor.getDisplayName()
					+ ": Value " + d + " below minimum of " + min
					+ " -- " + what);
			}
			if (max != Constants.undefinedDouble && d > max)
			{
				String what = "discarded";
				int f = tss.tv.getFlags() | IFlags.LIMIT_VIOLATION;
				if (maxReplaceValue != Constants.undefinedDouble)
				{
					tss.tv.setValue(maxReplaceValue);
					what = "replaced with " + maxReplaceValue;
				}
				else
					f |= IFlags.IS_MISSING;
				tss.tv.setFlags(f);
				Logger.instance().debug1(sensor.getDisplayName()
					+ ": Value " + d + " above maximum of " + max
					+ " -- " + what);
			}
		}
	}

	/**
	  Discards all samples with times before the passed previous-message-time.
	  This is called by decoded message when the user has specified that 
	  redundant data should be discarded.
	  @param prevMsgTime the Java msec time value before which all data is to
	  be discarded.
	*/
	public void discardSamplesBefore(long prevMsgTime)
	{
		for(Iterator<TSSample> it = samples.iterator(); it.hasNext(); )
		{
			TSSample tss = it.next();
			if (tss.tv == null)
				continue;
			long t = tss.tv.getTime().getTime();
			if (t < prevMsgTime)
				it.remove();
		}
	}

	/** 
	  Adds a specified value to all samples in the series.
	  @param v the value to add
	*/
	public void addToSamples(double v)
	{
		for(Iterator<TSSample> it = samples.iterator(); it.hasNext(); )
		{
			TSSample tss = it.next();
			if (tss.tv == null)
				continue;
			try
			{
				tss.tv.setValue(tss.tv.getDoubleValue() + v);
			}
			catch(NoConversionException ex) {}
		}
	}

	/**
	  Multiplies a specified value by all samples in the series.
	  @param v the value 
	*/
	public void multiplySamplesBy(double v)
	{
		for(Iterator<TSSample> it = samples.iterator(); it.hasNext(); )
		{
			TSSample tss = it.next();
			if (tss.tv == null)
				continue;
			try
			{
				tss.tv.setValue(tss.tv.getDoubleValue() * v);
			}
			catch(NoConversionException ex) {}
		}
	}

	/**
	From ITimeSeries interface
	@return the character code representing 
	data order ('A'=ascending, 'D' = descending)
	*/
	public char getDataOrder()
	{
		return dataOrder;
	}


	/**
	From ITimeSeries interface, searches all sensor data for a matching
	property in the following order: ConfigSensor, ScriptSensor, 
	PlatformSensor.

	@param name  Name of property to search for
	@return String property value of null if no match found.
	*/
	public String getProperty(String name)
	{
		if (sensor == null)
			return null;
		return sensor.getProperty(name);
	}

	/**
	* From ITimeSeries interface, sets periodicity parameters
	  @param  mode F=fixed, V=variable
	  @param  firstSamp second-of-day of first sample
	  @param  sampInt interval between samples in seconds
	*/
	public void setPeriodicity(char mode, int firstSamp, int sampInt)
	{
		sensor.configSensor.recordingMode = mode;
		sensor.configSensor.timeOfFirstSample = firstSamp;
		sensor.configSensor.recordingInterval = sampInt;
	}

	/**
	From ITimeSeries interface
	@return the sensor number, which is unique
	for this time series within this message.
	*/
	public int getSensorId()
	{
		return getSensorNumber();
	}

	/**
	From ITimeSeries interface
	@return the sensor name.
	*/
	public String getSensorName()
	{
		return sensor == null || sensor.configSensor == null ? 
			(String)null : sensor.configSensor.sensorName;
	}

	public String getDisplayName()
	{
		return sensor == null ? "(unknown)" : sensor.getDisplayName();
	}

	/** 
	@return time of first sample in this series, assumes samples are sorted. 
	*/
	public int getTimeOfFirstSample()
	{
		return sensor.getTimeOfFirstSample();
	}

	/**
    * @return recording mode
    */
    public char getRecordingMode()
	{
		return sensor.getRecordingMode();
	}

	/**
	  From ITimeSeries interface, associates a data type with this time series.
	  @param standard the data type standard
	  @param code the data type code
	*/
	public void addDataType(String standard, String code)
	{
		sensor.configSensor.addDataType(DataType.getDataType(standard, code));
	}

	/** @return true if this sensor has a matching data type. */
	public boolean hasDataType(String code)
	{
		for(Iterator<DataType> it = sensor.configSensor.getDataTypes(); 
			it.hasNext(); )
		{
			DataType dt = it.next();
			if (code.equalsIgnoreCase(dt.getCode()))
				return true;
		}
		return false;
	}

	/**
	 * In order to handle certain times of auto-incrementing of time, as multiple
	 * samples are added to a sensor, the caller (DecodedMessage) needs to know
	 * for each time series, if this is the first sample added since time was
	 * set.
	 * <p>
	 * DecodedMessage will call setTimeJustSet whenever time is set by a time or
	 * date field. The flag is cleared above in this TimeSeries' addSample method.
	 * @return true if time was set since the last sample-add.
	 */
	public boolean timeJustSet() { return _timeJustSet; }
	
	/**
	 * Set flag indicating that time was set. See discussion for timeJustSet().
	 */
	public void setTimeJustSet() { _timeJustSet = true; }
	

}


class SampleComparator implements java.util.Comparator<TSSample>
{
	public boolean descending = false;
	
	public int compare(TSSample ts1, TSSample ts2)
	{
		long r = ts1.tv.getTime().getTime() - ts2.tv.getTime().getTime();
		int ret = r < 0L ? -1 : r > 0L ? 1 : 0;
		return descending ? -ret : ret;
	}

	public boolean equals(Object obj)
	{
		return obj == this;
	}
}

/** holds a TimedVariable and its formatted string value. */
class TSSample
{
	TimedVariable tv;  // Contains time and numeric value
	String fv;        // Value formatted by presentation group (may be null)

	TSSample(TimedVariable tv)
	{
		this.tv = tv;
		fv = null;
	}	
}
