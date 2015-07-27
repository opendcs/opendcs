/*
*  $Id$
*
*  This is open-source software written by ILEX Engineering, Inc., under
*  contract to the federal government. You are free to copy and use this
*  source code for your own purposes, except that no part of the information
*  contained in this file may be claimed to be proprietary.
*
*  Except for specific contractual terms between ILEX and the federal 
*  government, this source code is provided completely without warranty.
*  For more information contact: info@ilexeng.com
*  
*  $Log$
*  Revision 1.3  2015/07/14 18:33:40  mmaloney
*  CWMS-6160 fix. If an algorithm explicitly set flag bits, then don't automatically assume
*  that a quesntionable input implies a questionable output.
*
*  Revision 1.2  2015/04/14 18:19:26  mmaloney
*  Throw DbCompException on an invalid aggregate interval. Before it was throwing
*  NullPointer.
*
*  Revision 1.1.1.1  2014/05/19 15:28:59  mmaloney
*  OPENDCS 6.0 Initial Checkin
*
*  Revision 1.55  2013/07/31 15:43:00  mmaloney
*  dev
*
*  Revision 1.54  2013/07/31 15:41:31  mmaloney
*  Document the new "ifQuestionable" property.
*
*  Revision 1.53  2013/07/31 15:27:44  mmaloney
*  Added 'ifQuestionable' property feature.
*
*  Revision 1.52  2013/07/30 18:49:12  mmaloney
*  dev
*
*  Revision 1.51  2013/07/30 18:37:51  mmaloney
*  Implement description & proptypes for properties.
*
*  Revision 1.50  2013/07/26 17:57:48  mmaloney
*  Added setInputFlagBits method that takes a mask.
*
*  Revision 1.49  2013/07/23 13:13:15  mmaloney
*  setInputFlagBits must be public for USACE Validation Enhancement.
*
*  Revision 1.48  2013/07/05 16:14:42  shweta
*  Set DbKey values for algorithms having DbKey properties like 'LimitsOnValue'
*
*  Revision 1.47  2013/03/22 19:41:17  mmaloney
*  Correct handling of aggregate periods over a std/daylight time change.
*
*  Revision 1.46  2013/03/22 16:42:29  mmaloney
*  DST Fix
*
*  Revision 1.45  2013/03/22 15:53:44  mmaloney
*  debuging timzeon issue.
*
*  Revision 1.44  2013/03/21 18:27:40  mmaloney
*  DbKey Implementation
*
*/
package decodes.tsdb.algo;

import java.lang.reflect.Field;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.TimeZone;
import java.util.TreeSet;

import ilex.util.Logger;
import ilex.util.TextUtil;
import ilex.var.NamedVariableList;
import ilex.var.NamedVariable;
import ilex.var.TimedVariable;
import ilex.var.NoConversionException;
import decodes.sql.DbKey;
import decodes.tsdb.DbAlgorithmExecutive;
import decodes.tsdb.DbCompException;
import decodes.tsdb.DbIoException;
import decodes.tsdb.IntervalCodes;
import decodes.tsdb.IntervalIncrement;
import decodes.tsdb.MissingAction;
import decodes.tsdb.VarFlags;
import decodes.tsdb.ParmRef;
import decodes.util.PropertiesOwner;
import decodes.util.PropertySpec;
import decodes.hdb.HdbFlags;

/**
This is the base class of Algorithms built and maintained by the Algorithm
Wizard (AW)
*/
public abstract class AW_AlgorithmBase 
	extends DbAlgorithmExecutive
	implements PropertiesOwner
{
	/** List of all variables in the time slice. */
	protected NamedVariableList _timeSliceVars;

	/** The base date/time of the current timeslice */
	protected Date _timeSliceBaseTime;

	private boolean _inTimeSlice = false;

	/** The begin time of the aggregate period (for aggregating algorithms) */
	protected Date _aggregatePeriodBegin;

	/** The end time of the aggregate period (for aggregating algorithms) */
	protected Date _aggregatePeriodEnd;

	/** The algorithm type */
	protected AWAlgoType _awAlgoType = AWAlgoType.TIME_SLICE;

	/** The role-name of the output var that determines the period */
	protected String _aggPeriodVarRoleName = null;

	public static final long MS_PER_HOUR = 3600000L;
	public static final long MS_PER_DAY = MS_PER_HOUR * 24L;

	/** Used to detect if an algorithm produced results. */
	private boolean _saveOutputCalled;

	/** Used to track if any inputs in time slice were marked as 'DB_DELETED' */
	protected boolean _sliceInputsDeleted = false;

	/** Used to track if any inputs in agg period were marked as 'DB_DELETED' */
	protected boolean _aggInputsDeleted = false;

	/** Used to keep track if user code explicitly deleted any outputs. */
	private boolean _deleteOutputCalled = false;

	/** Sorted set of base times to execute over. */
	TreeSet<Date> baseTimes = null;

	/** Set to true to disable automatic filling of aggregate periods. */
	protected boolean noAggregateFill = false;
	
	/** Property that determines how to handle questionable input data */
	enum IfQuestionable { ProcessAsNormal, QuestionOutput, SkipTimeslice };
	protected IfQuestionable ifQuestionable = IfQuestionable.ProcessAsNormal;
	protected boolean _questionOutput = false;

	/**
	 * If an "aggregateTimeOffset" is supplied it must be in a form like
	 * "8 hours 15 minutes".
	 * That is: N label [N label] ...
	 * The init method will convert it to an array of IntervalIncrement
	 * objects.
	 * Default = null, meaning no offset supplied.
	 */
	protected IntervalIncrement aggregateTimeOffsetCalIncr[] = null;
	
	/**
	 * Override the aggregate period defined by the output parameter.
	 * Normally aggregate period is determined by the interval of one
	 * of the output parameters. If defined, this property can override 
	 * this. Thus you could have a running daily average computed every hour.
	 */
	protected String aggPeriodInterval = null;
	
	protected int debugLevel = 0;
	
	private PropertySpec basePropertySpecs[] = 
	{
		new PropertySpec("aggUpperBoundClosed", PropertySpec.BOOLEAN,
			"True to include end of period in aggregate"),
		new PropertySpec("aggLowerBoundClosed", PropertySpec.BOOLEAN,
			"True to include beginning of period in aggregate"),
		new PropertySpec("aggregateTimeZone", PropertySpec.TIMEZONE,
			"Java time zone for evaluating aggregate periods"),
		new PropertySpec("noAggregateFill", PropertySpec.BOOLEAN,
			"Set to false to disable filling aggregate periods before algo execution"),
		new PropertySpec("aggPeriodInterval", PropertySpec.STRING, 
			"Aggregate Period Interval"), 
		new PropertySpec("interpDeltas", PropertySpec.BOOLEAN,
			"True to allow interpolation when computing deltas"),
		new PropertySpec("maxInterpIntervals", PropertySpec.INT,
			"Max number of intervals that can be interpolated for missing"),
		new PropertySpec("ifQuestionable", PropertySpec.STRING,
			"ProcessAsNormal, QuestionOutput, or SkipTimeslice")
	};
	private PropertySpec allprops[] = null;
	
	/** Flag set from 'setFlagBits' method to let saveOutput know to not
	 * mess with the flags.
	 */
	private boolean setFlagBitsCalled = false;


	/**
	 * No-args Constructor because object is constructed from the class name.
	 */
	public AW_AlgorithmBase()
	{
		super();
	}

	/**
	 * Algorithm-specific initialization provided by the subclass.
	 */
	protected void initAlgorithm( )
		throws DbCompException
	{
		_inTimeSlice = false;

		// Check for the built-in properties first.
		String t_string = comp.getProperty("debugLevel");
		if (t_string != null)
		{
			try { debugLevel = Integer.parseInt(t_string.trim()); }
			catch(NumberFormatException ex)
			{
				debugLevel = 0;
				warning("Invalid 'debugLevel' property. May be 1, 2, or 3 only.");
			}
		}
		
		// Get the "noAggregateFill" boolean if there is one.
		t_string = comp.getProperty("noAggregateFill");
		if (t_string != null) 
			noAggregateFill = TextUtil.str2boolean(t_string);

		t_string = comp.getProperty("aggPeriodInterval");
		if (t_string != null)
			aggPeriodInterval = t_string;

		t_string = comp.getProperty("aggregateTimeZone");
		if (t_string != null)
		{
			TimeZone tz = TimeZone.getTimeZone(t_string);
			if (tz == null)
			{
				warning("Invalid aggregateTimeZone property '" + t_string
					+ "' -- ignored.");
			}
			else
			{
				aggregateTimeZone = t_string;
				aggTZ = tz;
				aggCal.setTimeZone(aggTZ);
				debugSdf.setTimeZone(TimeZone.getTimeZone(aggregateTimeZone));
				debug3("Setting aggregate TimeZone to '" + aggregateTimeZone + "'"
					+ " current time=" + debugSdf.format(new Date()));
			}
		}

		// Process property names declared in the algorithm.
		Class cls = this.getClass();
		for(String propName : getPropertyNames())
			setCompProperty(cls, propName);
				
		initAWAlgorithm();

		// MJM 6/27/2010 - This has to be done after the concrete initAWAlgorithm
		// so that _awAlgoType is set correctly:
		
		// Get the "aggLowerBoundClosed" boolean if there
		t_string = comp.getProperty("aggLowerBoundClosed");
		if (t_string != null) 
			aggLowerBoundClosed = TextUtil.str2boolean(t_string);
		else // default is true for regular aggregates, false for running aggregates.
			aggLowerBoundClosed = 
				_awAlgoType == AWAlgoType.RUNNING_AGGREGATE ? false : true;
//debug3("_awAlgoType=" + _awAlgoType.toString());
//debug3("prop str '" + t_string + "' aggLowerBoundClosed=" + aggLowerBoundClosed);
		
		t_string = comp.getProperty("aggUpperBoundClosed");
		if (t_string != null) 
			aggUpperBoundClosed = TextUtil.str2boolean(t_string);
		else // default is false for regular aggregates, true for running aggregates.
			aggUpperBoundClosed = 
				_awAlgoType == AWAlgoType.RUNNING_AGGREGATE ? true : false;
//debug3("prop str '" + t_string + "' aggUpperBoundClosed=" + aggUpperBoundClosed);

		t_string = comp.getProperty("interpDeltas");
		if (t_string != null)
			interpDeltas = TextUtil.str2boolean(t_string);
		t_string = comp.getProperty("maxInterpIntervals");
		if (t_string != null)
		{
			try { maxInterpIntervals = Integer.parseInt(t_string); }
			catch(Exception ex)
			{
				warning("Bad maxInterpIntervals property '" 
					+ maxInterpIntervals + "' -- ignored.");
			}
		}
		
		t_string = comp.getProperty("aggregateTimeOffset");
		if (t_string != null)
		{
			try { aggregateTimeOffsetCalIncr = IntervalIncrement.parseMult(t_string); }
			catch(Exception ex)
			{
				warning("Bad aggregateTimeOffset property '" + t_string + "' -- ignored.");
				aggregateTimeOffsetCalIncr = null;
			}
		}
		
		t_string = comp.getProperty("ifQuestionable");
		if (t_string != null)
		{
			switch(t_string.charAt(0))
			{
			case 'p': case 'P':
				ifQuestionable = IfQuestionable.ProcessAsNormal;
				break;
			case 's': case 'S':
				ifQuestionable = IfQuestionable.SkipTimeslice;
				break;
			case 'q': case 'Q':
				ifQuestionable = IfQuestionable.QuestionOutput;
				break;
			}
		}

		
	}
	
	private Field getField(Class cls, String varName)
		throws NoSuchFieldException
	{
		// note: Class.getField only returns public members.
		// so first use getDeclaredFields to handle protected but accessible members.
		try { return cls.getDeclaredField(varName); }
		catch(NoSuchFieldException ex)
		{
			return cls.getField(varName);
		}
	}
	
	/**
	 * Copy property value into local variables in the algorithm object.
	 */
	private void setCompProperty(Class cls, String propName)
	{
		String propVal = comp.getProperty(propName);		
		// Kludge for Oracle that can't store an empty string in a not null field.
		if (propVal == null)
		{
			debug1("Received property '" + propName 
				+ "' with null value -- ignored.");
			return;
		}
		if (propVal.equals("\"\""))
			propVal = "";
		String ftyp = "unkown";
		try
		{
			
			
			Field field = getField(cls, propName);
			ftyp = field.getType().getName();
					
			if (ftyp.equals("java.lang.String"))
			{
				field.set(this, propVal);
			}
			else if (ftyp.equals("double"))
			{
				if (propVal.equalsIgnoreCase("Double.MAX_VALUE"))
					field.setDouble(this, Double.MAX_VALUE);
				else if (propVal.equalsIgnoreCase("Double.MIN_VALUE")
					|| propVal.equalsIgnoreCase("Double.NEGATIVE_INIFINITY"))
					field.setDouble(this, Double.NEGATIVE_INFINITY);
				else
					field.setDouble(this, Double.parseDouble(propVal));
			}
			else if (ftyp.equals("long"))
			{
				if (propVal.equalsIgnoreCase("Long.MAX_VALUE"))
					field.setDouble(this, Long.MAX_VALUE);
				else if (propVal.equalsIgnoreCase("Long.MIN_VALUE"))
					field.setDouble(this, Long.MIN_VALUE);
				field.setLong(this, Long.parseLong(propVal));
			}
			else if (ftyp.equals("boolean"))
				field.setBoolean(this, TextUtil.str2boolean(propVal));
			
			//some computations like Limit checker have DbKey properties. Set DbKey values.
			else if (ftyp.endsWith("DbKey"))
			{
				try
				{					
					DbKey kval = DbKey.createDbKey(Long.parseLong(propVal));					
					if (kval!=null)
						field.set(this, kval);											
				}
				catch(NumberFormatException ex)
				{
					Logger.instance().warning(
						"Field '" + propName 
						+ "' requires an integer. Illegal value '"
						+ propVal + "' skipped.");
				}
				
			}
			else
				warning("Property '" + propName 
					+ "' has invalid local type -- ignored.");
		}
		catch(NumberFormatException ex)
		{
			warning("Property '" + propName + "' could not be parsed. "
				+ "Required type is " + ftyp);
		}
		catch(Exception ex)
		{
			warning("Property '" + propName + "' with no matching "
				+ "local variable -- ignored: " + ex);
		}
	}


	/**
	 * Concrete apply method to be supplied by subclass.
	 * @throws DbCompException on computation error.
	 */
	protected void applyAlgorithm( )
		throws DbCompException, DbIoException
	{
		debugSdf.setTimeZone(TimeZone.getTimeZone(aggregateTimeZone));
		debug3("Setting aggregate TimeZone to '" + aggregateTimeZone + "'"
			+ " current time=" + debugSdf.format(new Date()));

		int defLogPriority = Logger.instance().getMinLogPriority();
		if (debugLevel != 0)
		{
			switch(debugLevel)
			{
			case 1: Logger.instance().setMinLogPriority(Logger.E_DEBUG1); break;
			case 2: Logger.instance().setMinLogPriority(Logger.E_DEBUG2); break;
			case 3: Logger.instance().setMinLogPriority(Logger.E_DEBUG3); break;
			}
		}
		try
		{
			if (_awAlgoType == AWAlgoType.AGGREGATING
			 || _awAlgoType == AWAlgoType.RUNNING_AGGREGATE)
			{
//debug3("Starting ago fo type " + _awAlgoType.toString() + " lowerBoundClosed="
//+ aggLowerBoundClosed + " upperBoundClose=" + aggUpperBoundClosed);
				doAggregatePeriods();
			}
			else // Just iterate once over all time slices defined by input data.
			{
				baseTimes = getAllInputData();
				if (baseTimes.size() > 0)
				{
					beforeTimeSlices();
					_inTimeSlice = true;
					iterateTimeSlices( baseTimes );
					_inTimeSlice = false;
					afterTimeSlices();
				}
			}
		}
		finally
		{
			Logger.instance().setMinLogPriority(defLogPriority);
		}
	}
	

	/**
	 * Determine the aggregate periods then loop over each. For each period
	 * execute the time slices and save the output.
	 */
	protected void doAggregatePeriods()
		throws DbCompException, DbIoException
	{
		String intervalS = aggPeriodInterval;
		if (intervalS == null)
		{
			if (_aggPeriodVarRoleName == null)
			{
				warning("Cannot do aggregating algorithm without a controlling"
					+ " output variable.");
				return;
			}
			ParmRef parmRef = getParmRef(_aggPeriodVarRoleName);
			if (parmRef == null)
			{
				warning("Unknown aggregate control output variable '"
					+ _aggPeriodVarRoleName + "'");
				return;
			}
			intervalS = parmRef.compParm.getInterval();
		}

		TreeSet<Date> inputBaseTimes = determineInputBaseTimes();

		debug2("Aggregating period is '" + intervalS + "', found "
			+ inputBaseTimes.size() + " base times in input data.");
		if (inputBaseTimes.size() == 0)
			return;
//{int i=0;
//for(Date d: inputBaseTimes)
//debug3("baseTime[" + (i++) + "]=" + debugSdf.format(d));
//}
		if (_awAlgoType == AWAlgoType.RUNNING_AGGREGATE)
		{
			// For running aggregate, we must add input base times so that
			// we iterate forward over the aggregate period. Example:
			// A running daily average of hourly inputs, a value at 
			// time T influences the result at times T ... T+23hrs.
			// So we would add T+1hr, T+2hr ... T+23hr to base times.
			Date t = inputBaseTimes.last();
			if (t == null)
				return;
//debug3("Running aggregate, last time is " + debugSdf.format(t));

			// 't' is last base time in the input data
			// Add agg period to it to find the upper limit of t's influence.
			aggCal.setTime(t);
			IntervalIncrement calIncr = IntervalCodes.getIntervalCalIncr(intervalS);
			if (calIncr == null)
				throw new DbCompException("Comp '" + comp.getName() 
					+ "' Invalid interval string '" + intervalS + "'");
			aggCal.add(calIncr.getCalConstant(), calIncr.getCount());
			Date end = aggCal.getTime();
//debug3("Running aggregate, added " + calIncr + " end time is " + debugSdf.format(end));

			// Now add times t+int, t+(2*int), t+(3*int), until we hit the end.
			String varName = getInputNames()[0];
			ParmRef inpParmRef = getParmRef(varName);
			calIncr = IntervalCodes.getIntervalCalIncr(
				inpParmRef.compParm.getInterval());
			aggCal.setTime(t);
			aggCal.add(calIncr.getCalConstant(), calIncr.getCount());
//debug3("Running agg, last trigger T=" + debugSdf.format(t)
//+ ", end of last period will be " + debugSdf.format(end)
//+ ", adding increments of " + calIncr);
			while(aggCal.getTime().before(end)
				|| (this.aggUpperBoundClosed && aggCal.equals(end)))
			{
				t = aggCal.getTime();
				inputBaseTimes.add(t);
//Logger.instance().debug1("Added time " + debugSdf.format(t) 
//+ " to fill out running aggregate");
				aggCal.add(calIncr.getCalConstant(), calIncr.getCount());
			}
		}

		// Iterate thru base times of ALL input data, in ascending time order:
		_aggregatePeriodBegin = null;
		Date baseTime = null;
		for(Iterator<Date> timesIterator = inputBaseTimes.iterator(); ; )
		{
			AggregatePeriod aggPer = null;

			// Special case where upper & lower bounds are both closed and
			// this base time is a boundary. Thus it belongs to the end of the
			// lower period and the beginning of the upper period.
			// If the following are all true, then we just did the lower period.
			// Leave the base time alone and do the upper period.
			if (baseTime != null
			 && aggUpperBoundClosed && aggLowerBoundClosed
			 && baseTime.equals(_aggregatePeriodEnd))
			{
				debug3("Special processing for double-closed boundaries."
					+ " Just did period ending " + debugSdf.format(baseTime));
				// bump the base time by a second temporarily to force it into
				// the next period. Then put it back.
				long msec = baseTime.getTime();
				baseTime.setTime(msec+1000L);
				aggPer = determineAggPeriod(baseTime, intervalS);
				baseTime.setTime(msec);
				debug3("New agg per: " + debugSdf.format(aggPer.getBegin())
					+ " to " + debugSdf.format(aggPer.getEnd()));
			}
			else if (timesIterator.hasNext())
			{
				baseTime = timesIterator.next();
				aggPer = determineAggPeriod(baseTime, intervalS);
			}
			else
				break;
			
			// Is this sample the start of a new aggregate period?
			if (_aggregatePeriodBegin == null
			 || !aggPer.getBegin().equals(_aggregatePeriodBegin))
			{
				_aggregatePeriodBegin = aggPer.getBegin();
				_aggregatePeriodEnd = aggPer.getEnd();

				debug2("Doing aggregate period (" +
					debugSdf.format(_aggregatePeriodBegin) + ", "
					+ debugSdf.format(_aggregatePeriodEnd) + ")");

				// Do time slices for this aggregate period:
				if (!noAggregateFill)
					baseTimes = getAllInputData(_aggregatePeriodBegin, 
						_aggregatePeriodEnd);
				beforeTimeSlices();
				_inTimeSlice = true;
				_aggInputsDeleted = false;
				if (!noAggregateFill)
					iterateTimeSlices( baseTimes );
				_inTimeSlice = false;

				// User puts the aggregate computation in 'afterTimeSlices':
				_saveOutputCalled = false;
				_deleteOutputCalled = false;
				afterTimeSlices();
				debug2("Finished aggregate period that started at " 
					+ debugSdf.format(_aggregatePeriodBegin));

				if (_awAlgoType == AWAlgoType.AGGREGATING
				 && !_saveOutputCalled
				 && !_deleteOutputCalled
				 && getOutputNames().length == 1
				 && _aggInputsDeleted)
				{
					debug2("Auto-deleting output.");
					deleteAllOutputs(); // there is only 1.
				}
			}
		}
	}

	/**
	 * Determines the period start for the specified base time and interval.
	 * In the special case where upperBoundClosed and lowerBoundClosed are
	 * both true and the base time is on the boundary (and thus belongs to
	 * two different periods), this method will always return the lower.
	 * @return a date-pair containing the start/end of the period.
	 */
	protected AggregatePeriod determineAggPeriod(Date baseTime, String interval)
	{
		long lower = baseTime.getTime();
		long orig = baseTime.getTime();
		long upper = lower;

		debug3("determineAggregatePeriod baseTime=" + debugSdf.format(baseTime));
		
		// int[2] containing Calendar constant and increment
		IntervalIncrement calIncr = IntervalCodes.getIntervalCalIncr(interval);
//if (calIncr != null)
//debug2("determineAggPeriod baseTime=" + debugSdf.format(baseTime) + ", interval="+calIncr);

		if (calIncr == null)
		{
			warning("Aggregate control output variable '"
				+ _aggPeriodVarRoleName + "' is instantaneous "
				+ "-- cannot determine aggregate period.");
		}
		else if (interval.equalsIgnoreCase("wy"))
		{
			// Example: 2005 Water Year starts Oct 1, 2004.

			aggCal.setTime(baseTime);
				
			aggCal.set(Calendar.HOUR_OF_DAY, 0);
			aggCal.set(Calendar.MINUTE, 0);
			aggCal.set(Calendar.SECOND, 0);
			aggCal.set(Calendar.DAY_OF_MONTH, 1);

			int month = aggCal.get(Calendar.MONTH);
			aggCal.set(Calendar.MONTH, 9);		// Note Oct == Month 9
			if (month < 9)
				aggCal.add(Calendar.YEAR, -1);

			lower = aggCal.getTimeInMillis();
			if (aggUpperBoundClosed && lower == orig)
			{
				aggCal.add(Calendar.YEAR, -1);
				lower = aggCal.getTimeInMillis();
			}
			aggCal.add(Calendar.YEAR, 1);
			upper = aggCal.getTimeInMillis();
		}
		else if (aggPeriodInterval != null)
		{
			// This is a 'running' average, not based on the output
			// parameter interval.
			aggCal.setTime(baseTime);
			upper = baseTime.getTime();
			aggCal.add(calIncr.getCalConstant(), -calIncr.getCount());
			lower = aggCal.getTimeInMillis();
		}
		else // Normal Calendar Interval
		{
			// Example, baseTime is 11AM. Period is 6 hours.
			// Then calIncr = {Calendar.HOUR_OF_DAY, 6}
			// So I want to set seconds & minutes to zero.
			// Then I want to truncate 11AM to 6 AM.

			// Start with base Time.
			aggCal.setTime(baseTime);
			
			int cis[] = {Calendar.SECOND, Calendar.MINUTE, Calendar.HOUR_OF_DAY,
				Calendar.DAY_OF_MONTH, Calendar.MONTH };
			if (calIncr.getCalConstant() == Calendar.WEEK_OF_YEAR) // CWMS has week interval.
				cis = new int[]{Calendar.SECOND, Calendar.MINUTE, Calendar.HOUR_OF_DAY };
			
			for(int x = 0; x < cis.length && cis[x] != calIncr.getCalConstant(); x++)
			{
				int n = (cis[x] == Calendar.DAY_OF_MONTH) ? 1 : 0;
				
				if (aggregateTimeOffsetCalIncr != null)
				{
					// A time offset is used. Add it to 'n'. 
					for(IntervalIncrement ii : aggregateTimeOffsetCalIncr)
					{
						if (ii.getCalConstant() == cis[x])
							n += ii.getCount();
					}
				}
				aggCal.set(cis[x], n);
			}
				
			// Truncate to number of intervals
			int x = (aggCal.get(calIncr.getCalConstant()) / calIncr.getCount()) * calIncr.getCount();
			if (aggregateTimeOffsetCalIncr != null)
				// A time offset is used. Add it to 'n'. 
				for(IntervalIncrement ii : aggregateTimeOffsetCalIncr)
					if (ii.getCalConstant() == calIncr.getCalConstant())
						x += ii.getCount();

			aggCal.set(calIncr.getCalConstant(), x);
			Date dlower = new Date(lower);
			
			// MJM Because of the offset stuff above, we could end up with a
			// start of period after the base time. If that happens, decrement
			// back to the previous period.
			if (dlower.after(baseTime) && aggregateTimeOffsetCalIncr != null)
			{
				aggCal.add(calIncr.getCalConstant(), -calIncr.getCount());
				dlower = aggCal.getTime();
			}
			
			
			lower = aggCal.getTimeInMillis();
			boolean lowerInDaylight = aggCal.getTimeZone().inDaylightTime(dlower);
			debug3("lower=" + debugSdf.format(dlower) + ", lowerInDaylight = " + lowerInDaylight);
			
			// Special case where upperBoundClosed is true: Example 00:00 on May 1
			// is to be considered as part of the April Aggregate.
			// Thus, if the bottom of the period is the orig time then 
			// drop back one interval.
			if (aggUpperBoundClosed && lower == orig)
			{
				aggCal.add(calIncr.getCalConstant(), -calIncr.getCount());
				lower = aggCal.getTimeInMillis();
			}
			
			aggCal.add(calIncr.getCalConstant(), calIncr.getCount());
			upper = aggCal.getTimeInMillis();
			
			Date dupper = new Date(upper);
			boolean upperInDaylight = aggCal.getTimeZone().inDaylightTime(dupper);
			
			// MJM 2013/03/22 special processing when the aggregate period spans a
			// daylight time-change. For aggregates more than 2 hours and less than
			// 1 day, we either add or subtract an hour from the end time.
			// Thus on the 1st day of DT, for a 3 hour average we want the
			// time stamps to be 00:00 (ST) 03:00 (DT) 06:00 09:00, etc.
			// The first period of the day is actually only 2 hours long.
			// Likewise on the first day of ST we would have 00:00 (DT)
			// 03:00 (ST) 06:00 09:00, etc. The first period is 4 hours long.
			// In other words, the aggregate period spanning the change is either
			// long or short, but the rest are normal length.

			// Check for change from Daylight to Standard
			if (lowerInDaylight && !upperInDaylight
			 && calIncr.getCalConstant() == Calendar.HOUR_OF_DAY
			 && calIncr.getCount() >= 3 && calIncr.getCalConstant() <= 12)
			{
				aggCal.add(Calendar.HOUR_OF_DAY, 1);
				upper = aggCal.getTimeInMillis();
				dupper = new Date(upper);
				debug2("Added 1 hour to upper because of daylight change.");
			}
			// else check for change from std to daylight
			else if (!lowerInDaylight && upperInDaylight
				&& calIncr.getCalConstant() == Calendar.HOUR_OF_DAY
				&& calIncr.getCount() >= 3 && calIncr.getCalConstant() <= 12)
			{
				aggCal.add(Calendar.HOUR_OF_DAY, -1);
				upper = aggCal.getTimeInMillis();
				dupper = new Date(upper);
				debug2("Subtracted 1 hour to upper because of daylight change.");
			}
			debug3("upper=" + debugSdf.format(dupper) + ", upperInDaylight = " + upperInDaylight);
		}

//		if (!aggLowerBoundClosed) lower += 1000L;
//		if (!aggUpperBoundClosed) upper -= 1000L;
		AggregatePeriod ret = new AggregatePeriod(new Date(lower), new Date(upper));
//debug3("determineAggPeriod baseTime=" + debugSdf.format(baseTime)
//+ ", intv=" + interval 
//+ ", range=(" + debugSdf.format(ret.getBegin()) + ", " + debugSdf.format(ret.getEnd()) + ")");
		return ret;
	}

	/**
	 * Extract variables for a single time slice then call the sub-class
	 * doAWTimeSlice() method.
	 *
	 * @param timeSlice a set of input variables for a single time-slice
	 *        (the name of each variable will be the algorithm role name).
	 * @param baseTime The base-time of this slice. Any variables having 
	 *        non-zero deltaT may be before or after this time.
	 *
	 * @throws DbCompException (or subclass thereof) if execution of this
	 *        algorithm is to be aborted.
	 */
	protected void doTimeSlice( NamedVariableList timeSlice, Date baseTime)
		throws DbCompException
	{
		_timeSliceVars = timeSlice;
		_timeSliceBaseTime = baseTime;
		boolean _missing_found = false;

		getSliceInputs();
		_questionOutput = false;

//  now test for any deleted or missing required inputs
//  if any found then do a delete of all outputs
		for (String varName : getInputNames())
		{
			ParmRef parmRef = getParmRef(varName);
			if (parmRef == null)
				continue;
			if (parmRef.missingAction == MissingAction.FAIL
			 || ifQuestionable != IfQuestionable.ProcessAsNormal)
			{
				NamedVariable v = _timeSliceVars.findByName(varName);
				if (v == null)
				{
					_missing_found = true;
					// MJM - Don't deleteAllOutputs() delete if the input isn't there, 
					// only if it was explicitly deleted.
					break;
				}
				else if (VarFlags.wasDeleted(v))
				{
					_missing_found = true;
					deleteAllOutputs();
					break;
				}
				else if (tsdb.isQuestionable(v))
				{
					if (ifQuestionable == IfQuestionable.SkipTimeslice)
						// Treat like missing data. Will skip the time slice below.
						_missing_found = true;
					else // ifQuestionable must == QuestionOutput
						_questionOutput = true;
				}
			}
		}

		if (_missing_found)
		{
			 return;
		}
		else
		{

			if (_sliceInputsDeleted)
			_aggInputsDeleted = true;
			
//			debug2("Doing time slice for base time "
//				+ debugSdf.format(baseTime) + " vars: " + _timeSliceVars);
			_saveOutputCalled = false;
			_deleteOutputCalled = false;
			doAWTimeSlice();
		}

		// The auto delete function for time-slice algorithms:
		if (_awAlgoType == AWAlgoType.TIME_SLICE
		 && !_saveOutputCalled
		 && getOutputNames().length == 1
		 && _sliceInputsDeleted
		 && !_deleteOutputCalled)
		{
			deleteAllOutputs(); // there is only 1.
		}
	}


	/**
	 * Sets a double-type output variable.
	 * If in a time slice, add to _timeSliceVars.
	 */
	public void setOutput(NamedVariable v, double d)
	{
		if (d == Double.NEGATIVE_INFINITY
		 || d == Double.MIN_VALUE)
			deleteOutput(v);
		else
		{
			v.setValue(d);
			saveOutput(v);
		}
	}

	/**
	 * Saves a floating-point output variable at a specific time.
	 */
	protected void setOutput(NamedVariable v, double d, Date t)
	{
		if (d == Double.NEGATIVE_INFINITY
		 || d == Double.MIN_VALUE)
			deleteOutput(v, t);
		else
		{
			v.setValue(d);
			saveOutput(v, new Date(t.getTime()));
		}
	}

	/**
	 * Sets a long integer output.
	 */
	protected void setOutput(NamedVariable v, long li)
	{
		if (li == Long.MIN_VALUE)
			deleteOutput(v);
		else
		{
			v.setValue(li);
			saveOutput(v);
		}
	}

	protected void setOutput(NamedVariable v, String s)
	{
		if (s == null)
			deleteOutput(v);
		else
		{
			v.setValue(s);
			saveOutput(v);
		}
	}

	
	/**
	 * Sets bits in the flags, leaving other bits alone.
	 * @param v the named variable containing the flags
	 * @param bits the bits to set.
	 */
	public void setFlagBits(NamedVariable v, int bits)
	{
		v.setFlags(v.getFlags() | bits);
		setFlagBitsCalled = true;
		saveOutput(v);
		setFlagBitsCalled = false;
	}

	/**
	 * Clears bits in the flags, leaving other bits alone.
	 * @param v the named variable containing the flags
	 * @param bits the bits to clear.
	 */
	public void clearFlagBits(NamedVariable v, int bits)
	{
		v.setFlags(v.getFlags() & (~bits));
		saveOutput(v);
	}

	/**
	 * Retrieve the flag bits for the named input param.
	 * @param name the input variable name
	 * @return the flag bits
	 */
	protected int getInputFlagBits(String name)
	{
		if (!_inTimeSlice)
		{
			warning("Cannot get '" + name
				+ "' flag bits outside a time-slice.");
			return 0;
		}
		NamedVariable v = _timeSliceVars.findByName(name);
		if (v == null)
		{
			warning("Cannot get '" + name
					+ "' flag bits -- no variable with that name.");
			return 0;
		}
		return v.getFlags();
	}

	public void setInputFlagBits(String name, int bits)
	{
		if (!_inTimeSlice)
		{
			warning("Cannot set '" + name
				+ "' flag bits outside a time-slice.");
			return;
		}
		NamedVariable v = _timeSliceVars.findByName(name);
		if (v == null)
		{
			warning("Cannot set '" + name
					+ "' flag bits -- no variable with that name.");
			return;
		}

		_saveOutputCalled = true;

		// If the specified bits are already set, do nothing.
		// This is crucial to avoid an endless loop in limit-check
		// algorithms.
		int oldFlags = v.getFlags();
//debug3("setInputFlagBits: name='" + name
//+ "', bits=0x" + Integer.toHexString(bits)
//+ ", oldFlags=0x" + Integer.toHexString(oldFlags));
		if ((oldFlags|bits) == oldFlags)
			return;

		// We also set ...
		//    TO_WRITE bit, causing it to be written back to DB.
		v.setFlags( v.getFlags() | bits | VarFlags.TO_WRITE );
	}

	// Like setInputFlagBits above, but will first clear bits in the specified mask.
	public void setInputFlagBits(String name, int bits, int mask)
	{
		if (!_inTimeSlice)
		{
			warning("Cannot set '" + name
				+ "' flag bits outside a time-slice.");
			return;
		}
		NamedVariable v = _timeSliceVars.findByName(name);
		if (v == null)
		{
			warning("Cannot set '" + name
					+ "' flag bits -- no variable with that name.");
			return;
		}

		_saveOutputCalled = true;

		// If the specified bits are already set, do nothing.
		// This is crucial to avoid an endless loop in limit-check
		// algorithms.
		int oldFlags = v.getFlags();
//debug3("setInputFlagBits: name='" + name
//+ "', bits=0x" + Integer.toHexString(bits)
//+ ", oldFlags=0x" + Integer.toHexString(oldFlags));
		if ((oldFlags|bits) == oldFlags)
			return;

		
		// We also set ...
		//    TO_WRITE bit, causing it to be written back to DB.
		v.setFlags( (v.getFlags()&(~mask)) | bits | VarFlags.TO_WRITE );
	}

	protected void clearInputFlagBits(String name, int bits)
	{
		if (!_inTimeSlice)
		{
			warning("Cannot clear '" + name
				+ "' flag bits outside a time-slice.");
			return;
		}
		NamedVariable v = _timeSliceVars.findByName(name);
		if (v == null)
		{
			warning("Cannot clear '" + name
					+ "' flag bits -- no variable with that name.");
			return;
		}

		_saveOutputCalled = true;

		// If the specified bits are already clear, do nothing.
		// This is crucial to avoid an endless loop in limit-check
		// algorithms.
		int oldFlags = v.getFlags();
//debug3("clearInputFlagBits: name='" + name
//+ "', bits=0x" + Integer.toHexString(bits)
//+ ", oldFlags=0x" + Integer.toHexString(oldFlags));
		
		if ((oldFlags & (~bits)) == oldFlags)
			return;

		// We also set ...
		//    TO_WRITE bit, causing it to be written back to DB.
		v.setFlags( (v.getFlags() & (~bits)) | VarFlags.TO_WRITE);
	}

	private void saveOutput(NamedVariable v)
	{
		VarFlags.setToWrite(v);
		_saveOutputCalled = true;
		
		// MJM 2015 06/30 If this is being called from setFlagBits, then it is
		// some kind of validation computation that has just derived the flag bits.
		// This method should not resort to the "Q-input = Q-output" rule.
		if (_questionOutput && !setFlagBitsCalled)
			tsdb.setQuestionable(v);

		// Set the output's source ID from the computation.
		v.setSourceId(comp.getDataSourceId());

		if (_inTimeSlice)
			_timeSliceVars.add(v);
		else // not in time slice save directly to time series
			 // with the aggregate base time.
		{
			if (_aggregatePeriodBegin == null)
			{
				warning("Cannot save '" + v.toString()
					+ "' Not an aggregating algorithm.");
				return;
			}
			ParmRef parmRef = getParmRef(v.getName());
			if (parmRef == null)
			{
				warning("Cannot save '" + v.toString()
					+ "' no output parameter role defined!");
				return;
			}
			
			// For normal aggregates, the output is based at the start of the
			// period. For running aggregates, the output is based at the end.
			Date varDate = this._awAlgoType == AWAlgoType.AGGREGATING ?
				_aggregatePeriodBegin : _aggregatePeriodEnd;
			Date aggD = parmRef.compParm.baseTimeToParamTime(varDate, aggCal);
debug1("Storing aggregate value=" + v.getStringValue()
+ " basetime=" + debugSdf.format(varDate) + ", parmtime=" + debugSdf.format(aggD)
+ " parm deltaT=" + parmRef.compParm.getDeltaT() + " (" + parmRef.compParm.getDeltaTUnits() + ")");
			TimedVariable tv = new TimedVariable(v, aggD);

			// Obscure bug fix starts here ============================
			TimedVariable oldTv = parmRef.timeSeries.findWithin(varDate, 10);
			try
			{
				if (oldTv != null)
				{
					// If old value is the same, preserve the flags.
					double diff = v.getDoubleValue() - oldTv.getDoubleValue();
					if (diff >= -.0000001 && diff <= .0000001)
					{
						int f = oldTv.getFlags() | VarFlags.TO_WRITE;
						tv.setFlags(f);
					}
				}
			}
			catch(NoConversionException ex)
			{
				warning("Error comparing existing aggregate output '"
					+ oldTv + ": " + ex);
			}
			// End of Obscure bug fix ============================
			parmRef.timeSeries.addSample(tv);
		}
	}

	/**
	 * Saves an output at a specific time.
	 */
	private void saveOutput(NamedVariable v, Date t)
	{
		_saveOutputCalled = true;
		ParmRef parmRef = getParmRef(v.getName());
		if (parmRef == null)
		{
			warning("Cannot save '" + v.toString()
				+ "' no output parameter role defined!");
			return;
		}
		TimedVariable tv = new TimedVariable(v, t);
		tv.setSourceId(comp.getDataSourceId());
		VarFlags.setToWrite(tv);

		parmRef.timeSeries.addSample(tv);
//info("Added value to save: " + tv + ", flag=0x" 
//+ Integer.toHexString(tv.getFlags()) + ", to time series SDI="
//+ parmRef.timeSeries.getSDI() + ", size=" + parmRef.timeSeries.size());
	}

	/**
	 * Mark the passed variable as 'TO_DELETE' in the current time slice.
	 */
	protected void deleteOutput(NamedVariable v)
	{
		VarFlags.setToDelete(v);
		_deleteOutputCalled = true;
		if (_inTimeSlice)
		{
			_timeSliceVars.add(v);
		}
		else
		{
			if (_aggregatePeriodBegin == null)
			{
				warning("Cannot delete '" + v.toString()
					+ "' Not an aggregating algorithm.");
				return;
			}
			ParmRef parmRef = getParmRef(v.getName());
			if (parmRef == null)
			{
				warning("Cannot delete '" + v.toString()
					+ "' no output parameter role defined!");
				return;
			}
			
			Date paramTime = parmRef.compParm.baseTimeToParamTime(
				_aggregatePeriodBegin, aggCal);
			TimedVariable tv = new TimedVariable(v, paramTime);
			VarFlags.setToDelete(tv);
			parmRef.timeSeries.addSample(tv);
		}
	}

	/**
	 * Deletes an output at a specific time.
	 */
	protected void deleteOutput(NamedVariable v, Date t)
	{
		VarFlags.setToDelete(v);
		_deleteOutputCalled = true;
		ParmRef parmRef = getParmRef(v.getName());
		if (parmRef == null)
		{
			warning("Cannot delete '" + v.toString()
				+ "' no output parameter role defined!");
			return;
		}
		TimedVariable tv = new TimedVariable(v, t);
		VarFlags.setToDelete(tv);
		parmRef.timeSeries.addSample(tv);
	}

	/**
	 * Mark all output variables as 'TO_DELETE' in the current time slice.
	 */
	protected void deleteAllOutputs()
	{
		Class cls = this.getClass();
		for(String role : getOutputNames())
		{
			try
			{
				Field field = getField(cls, role);
				String ftyp = field.getType().getName();
				if (ftyp.equals("ilex.var.NamedVariable"))
				{
					NamedVariable nv = (NamedVariable)field.get(this);
					deleteOutput(nv);
				}
			}
			catch(Exception ex)
			{
				warning("Error in deleteAllOutputs: " + ex);
			}
		}
	}

	protected void getSliceInputs()
	{
//debug3("getSliceInputs declared fields are:");
//Field f[] = this.getClass().getDeclaredFields();
//for(int i=0; i<f.length; i++)
//debug3("f[" + i + "]='"+f[i].getName()+"' type="+f[i].getType().toString());
 
		for(String varName : getInputNames())
		{
			ParmRef parmRef = getParmRef(varName);
			
			boolean unused = (parmRef == null || parmRef.compParm == null);
			// MJM 20150727 Optional parms that are unused in an algorithm should be
			// filled with the special value meaning MISSING.
			
//			if (unused)
//				continue;

			_sliceInputsDeleted = false;
			// Use reflection to find the variable with same name.
			// Then determine its type (double, long, or String).
			Class cls = null;
			try
			{
				cls = this.getClass();
				
				// note: getField only returns public members.
				// so first use getDeclaredFields to handle protected but accessible members.
				Field field = getField(cls, varName);
				String ftyp = field.getType().getName();
				NamedVariable v = null;
				
//debug3("Found field matching varName '" + varName + "'");
				if (!unused)
				{
					String nm = varName;
					String typ = parmRef.compParm.getAlgoParmType().toLowerCase();
//debug3("parm-typ is '" + typ + "'");
				 	if (typ.length() > 1 && typ.charAt(1) == 'd')
				 		nm += "_d";
				 	v = _timeSliceVars.findByNameIgnoreCase(nm);
				}
//debug3("field type is '" + ftyp + "'");

				// NOTE: Missing data is handled by DbAlgorithmExecutive in
				// the iterateTimeSlices method. Assume if an input param is
				// missing that its MissingAction was set to Ignore.

				// Only 3 types allowed: String, double, long
				if (ftyp.equals("double"))
				{
					if (v == null)
						field.setDouble(this, Double.NEGATIVE_INFINITY);
					else if (VarFlags.wasDeleted(v))
					{
//debug3(varName + " - sample was deleted.");
						field.setDouble(this, Double.NEGATIVE_INFINITY);
						_sliceInputsDeleted = true;
					}
					else
						field.setDouble(this, v.getDoubleValue());
				}
				else if (ftyp.equals("long"))
				{
					if (v == null)
						field.setLong(this, Long.MIN_VALUE);
					else if (VarFlags.wasDeleted(v))
					{
						field.setLong(this, Long.MIN_VALUE);
						_sliceInputsDeleted = true;
					}
					else
						field.setLong(this, v.getLongValue());
				}
				else if (ftyp.equals("java.lang.String"))
				{
					if (v == null)
						field.set(this, (String)null);
					else if (VarFlags.wasDeleted(v))
					{
						field.set(this, (String)null);
						_sliceInputsDeleted = true;
					}
					else
						field.set(this, v.getStringValue());
				}
				else
					warning("Invalid input variable type '" + ftyp
						+ "' -- ignored.");
			}
			catch(IllegalAccessException ex)
			{
				String msg = 
					"Inconsistent class -- cannot access input field named '"
					+ varName + "'";
System.err.println(msg);
ex.printStackTrace(System.err);
				warning(msg);
			}
			catch(NoSuchFieldException ex)
			{
				warning("Inconsistent class -- no input field named '"
					+ varName + "'");
			}
			catch(NoConversionException ex)
			{
				warning("Cannot convert input '" + varName 
					+ "' to correct input type.");
			}
		}
	}

	/**
	 * Returns true if this variable is flagged as either missing or deleted.
	 * The algorithm should not then use it's value in an equation.
	 * @return true if this variable is flagged as either missing or deleted.
	 */
	protected boolean isMissing(double var)
	{
		return var == Double.MIN_VALUE || var == Double.NEGATIVE_INFINITY;
	}

	/**
	 * Returns true if this variable is flagged as either missing or deleted.
	 * The algorithm should not then use it's value in an equation.
	 * @return true if this variable is flagged as either missing or deleted.
	 */
	protected boolean isMissing(long var)
	{
		return var == Long.MIN_VALUE;
	}

	/**
	 * Returns true if this variable is flagged as either missing or deleted.
	 * The algorithm should not then use it's value in an equation.
	 * @return true if this variable is flagged as either missing or deleted.
	 */
	protected boolean isMissing(String var)
	{
		return var == null;
	}

	protected abstract void initAWAlgorithm()
		throws DbCompException;

	protected abstract void beforeTimeSlices()
		throws DbCompException;

	protected abstract void doAWTimeSlice()
		throws DbCompException;

	protected abstract void afterTimeSlices()
		throws DbCompException;
	
	public abstract String[] getPropertyNames();

 	/**
	 * Finds a coefficient from the stat tables matching the sdi,
	 * interval and tableselector for the given rolename
	 * @param rolename the name of the role
	 * @return double value returned from db.
	 * @throws DbCompException
	 */
	protected double getCoeff(String rolename)
		throws DbCompException
	{
		DbKey sdi = getSDI(rolename);
		String interval = getInterval(rolename);	
		String ts = getTableSelector(rolename);
		
		double coeff;
		try
		{ 
			if (_inTimeSlice)
			{
				if (_timeSliceBaseTime == null)
				{
					throw new DbCompException(
						"Cannot find Coeff for null time, role="+rolename);
				}
				coeff = tsdb.getCoeff(sdi,ts,interval,_timeSliceBaseTime);
			}
			else if (_aggregatePeriodBegin == null)
			{
				throw new DbCompException(
					"Cannot find Coeff for null time., role"+rolename);
			}
			else 
			{
				coeff=tsdb.getCoeff(sdi,ts,interval,_aggregatePeriodBegin);
			}
		}
		catch (DbIoException ex) 
		{
			throw new DbCompException(
				"Cannot find Coeff for role "+ rolename + ":"+ex);
		}
		return coeff;
	}

	//=====================================================================
	// Special methods for HDB Validation & Derivation Flags
	//=====================================================================

	/**
	 * Gets the USBR HDB 'VALIDATION' flag for passed variable.
	 * @param name the variable name
	 * @return the single-char HDB VALIDATION flag.
	 */
	protected char getInputHdbValidationFlag(String name)
	{
		return HdbFlags.flag2HdbValidation(getInputFlagBits(name));
	}

	/**
	 * Gets the USBR HDB 'DERIVATION' flag for passed variable.
	 * @param name the variable name
	 * @return the String HDB DERIVATION flags.
	 */
	protected String getInputHdbDerivationFlag(String name)
	{
		return HdbFlags.flag2HdbDerivation(getInputFlagBits(name));
	}

	/**
	 * Sets the USBR HDB 'VALIDATION' flag in the passed variable.
	 * @param v the named variable to set flag for.
	 * @param hdbValidationFlag the flag value
	 */
	protected void setHdbValidationFlag(NamedVariable v,
		char hdbValidationFlag)
	{
		int f = v.getFlags();
		f &= (~HdbFlags.HDBF_VALIDATION_MASK);
		f |= HdbFlags.hdbValidation2flag(hdbValidationFlag);
		v.setFlags(f);
		saveOutput(v);
	}

	/**
	 * Sets the USBR HDB 'VALIDATION' flag in the passed input var.
	 * @param name the name of the input variable
	 * @param hdbValidationFlag the flag value
	 */
	protected void setInputHdbValidationFlag(String name,
		char hdbValidationFlag)
	{
		clearInputFlagBits(name, HdbFlags.HDBF_VALIDATION_MASK);
		setInputFlagBits(name, HdbFlags.hdbValidation2flag(hdbValidationFlag));
	}

	/**
	 * Sets the USBR HDB 'DERIVATION' flags in the passed variable.
	 * @param v the named variable to set flags for.
	 * @param hdbDerivationFlags the flag value
	 */
	protected void setHdbDerivationFlag(NamedVariable v,
		String hdbDerivationFlags)
	{
		int f = v.getFlags();
		f &= (~HdbFlags.HDBF_DERIVATION_MASK);
		f |= HdbFlags.hdbDerivation2flag(hdbDerivationFlags);
		v.setFlags(f);
		saveOutput(v);
	}

	/**
	 * Sets the USBR HDB 'DERIVATION' flags for an input.
	 * @param name the name of the input param.
	 * @param hdbDerivationFlags the flag value
	 */
	protected void setInputHdbDerivationFlag(String name,
		String hdbDerivationFlags)
	{
		clearInputFlagBits(name, HdbFlags.HDBF_DERIVATION_MASK);
		setInputFlagBits(name, HdbFlags.hdbDerivation2flag(hdbDerivationFlags));
	}
	
	/**
	 * Added for USBR Convert2Groups utility, return a property setting from
	 * the fully initialized algorithm.
	 * @param name
	 * @return the property value or null if not defined.
	 */
	public String getEvaluatedProperty(String name)
	{
		// See if it's defined either in comp or algo record.
		String v = comp.getProperty(name);
		if (v != null)
			return v;
		if (name.equalsIgnoreCase("aggUpperBoundClosed"))
			return "" + aggUpperBoundClosed;
		else if (name.equalsIgnoreCase("aggLowerBoundClosed"))
			return "" + aggLowerBoundClosed;
		else if (name.equalsIgnoreCase("aggregateTimeZone"))
			return aggTZ.getID();
		else if (name.equalsIgnoreCase("noAggregateFill"))
			return "" + noAggregateFill;
		else if (name.equalsIgnoreCase("aggPeriodInterval"))
			return aggPeriodInterval;
		else if (name.equalsIgnoreCase("interpDeltas"))
			return "" + interpDeltas;
		else if (name.equalsIgnoreCase("maxInterpIntervals"))
			return "" + maxInterpIntervals;
		return null;
	}
	
	/**
	 * Algorithm should override this and return property specs for the individual algorithm.
	 * Legacy algorithms that have not overridden this method will use names only.
	 * @return array of property specifications supported by this algorithm
	 */
	protected PropertySpec[] getAlgoPropertySpecs()
	{
		return null;
	}
	
	@Override
	public PropertySpec[] getSupportedProps()
	{
		if (allprops != null)
			return allprops;
		
		PropertySpec[] algoProps = getAlgoPropertySpecs();
		if (algoProps == null)
		{
			String propNames[] = getPropertyNames();
			algoProps = new PropertySpec[propNames.length];
			for(int i=0; i<propNames.length; i++)
				algoProps[i] = new PropertySpec(propNames[i], PropertySpec.STRING, "");
		}
		allprops = new PropertySpec[basePropertySpecs.length + algoProps.length];
		for(int i=0; i<basePropertySpecs.length; i++)
			allprops[i] = basePropertySpecs[i];
		for(int i=0; i < algoProps.length; i++)
			allprops[i + basePropertySpecs.length] = algoProps[i];
		return allprops;
	}

	/**
	 * @return false. GUI should only allow the named properties.
	 */
	@Override
	public boolean additionalPropsAllowed()
	{
		return false;
	}
}
