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
package decodes.tsdb.algo;

import java.lang.reflect.Field;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.TimeZone;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.opendcs.utils.AnnotationHelpers;
import org.opendcs.utils.logging.OpenDcsLoggerFactory;
import org.slf4j.Logger;

import ilex.util.TextUtil;
import ilex.var.IFlags;
import ilex.var.NamedVariableList;
import ilex.var.NamedVariable;
import ilex.var.TimedVariable;
import ilex.var.NoConversionException;
import decodes.sql.DbKey;
import decodes.tsdb.ComputationApp;
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
import decodes.cwms.CwmsFlags;
import decodes.hdb.HdbFlags;

/**
This is the base class of Algorithms built and maintained by the Algorithm
Wizard (AW)
*/
public abstract class AW_AlgorithmBase extends DbAlgorithmExecutive implements PropertiesOwner
{
	private static final Logger log = OpenDcsLoggerFactory.getLogger();
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
	protected boolean _saveOutputCalled;

	/** Used to track if any inputs in time slice were marked as 'DB_DELETED' */
	protected boolean _sliceInputsDeleted = false;

	/** Used to track if any inputs in agg period were marked as 'DB_DELETED' */
	protected boolean _aggInputsDeleted = false;

	/** Used to keep track if user code explicitly deleted any outputs. */
	private boolean _deleteOutputCalled = false;

	/** Sorted set of base times to execute over. */
	protected TreeSet<Date> baseTimes = null;

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
		new PropertySpec("OverwriteFlag", PropertySpec.BOOLEAN,
				"True to write 'O' to the Overwrite flag field (currently only supported for hdb)"),
		new PropertySpec("interpDeltas", PropertySpec.BOOLEAN,
			"True to allow interpolation when computing deltas"),
		new PropertySpec("maxInterpIntervals", PropertySpec.INT,
			"Max number of intervals that can be interpolated for missing"),
		new PropertySpec("ifQuestionable", PropertySpec.STRING,
			"ProcessAsNormal, QuestionOutput, or SkipTimeslice"),
		new PropertySpec("maxMissingValuesForFill", PropertySpec.INT,
			"When filling regular interval missing input data, do not fill more than this many intervals."),
		new PropertySpec("maxMissingTimeForFill", PropertySpec.INT,
			"When filling missing input data, fail if the missing gap is more than this many seconds."),
		new PropertySpec("timedCompInterval",PropertySpec.STRING,
			"Set for timed computations that are NOT triggered by inputs. e.g. '1 hour'"),
		new PropertySpec("timedCompOffset", PropertySpec.STRING,
			"(default=no offset) an optional offset after the regular interval for timed computations."
			+ " e.g. '13 minutes'"),
		new PropertySpec("timedCompDataSince",PropertySpec.STRING,
			"Control data window SINCE time for timed computations e.g. '150 minutes'."),
		new PropertySpec("timedCompDataUntil",PropertySpec.STRING,
			"Control data window UNTIL time for timed computations e.g. '15 minutes'.")
	};
	private PropertySpec aggAlgoPropertySpecs[] =
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
		new PropertySpec("aggregateTimeOffset", PropertySpec.STRING,
			"e.g. '8 hours', '1 day'. If supplied it is added to the output time of an aggregate."
			+ " An example would be to center an average within its period.")
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

	public void initForGUI()
	{
		try { initAWAlgorithm(); }
		catch(Exception ex) {}
	}

	/**
	 * Algorithm-specific initialization provided by the subclass.
	 */
	protected void initAlgorithm( )
		throws DbCompException
	{
		_inTimeSlice = false;

		// Get the "noAggregateFill" boolean if there is one.
		String t_string = comp.getProperty("noAggregateFill");
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
				log.warn("Invalid aggregateTimeZone property '{}' -- ignored.", t_string);
			}
			else
			{
				aggregateTimeZone = t_string;
				aggTZ = tz;
				aggCal.setTimeZone(aggTZ);
				log.trace("Setting aggregate TimeZone to '{}'' current time={}",
						  aggregateTimeZone, new Date());
			}
		}

		// Process property names declared in the algorithm.
		Class cls = this.getClass();
		for(String propName : getPropertyNames())
			setCompProperty(cls, propName);

		try { initAWAlgorithm(); }
		catch(DbCompException ex) { throw ex; }
		catch(Exception ex)
		{
			String msg = "Error initializing algorithm.";
			throw new DbCompException(msg, ex);
		}
		// MJM 6/27/2010 - This has to be done after the concrete initAWAlgorithm
		// so that _awAlgoType is set correctly:

		// Get the "aggLowerBoundClosed" boolean if there
		t_string = comp.getProperty("aggLowerBoundClosed");
		if (t_string != null)
			aggLowerBoundClosed = TextUtil.str2boolean(t_string);
		else // default is true for regular aggregates, false for running aggregates.
			aggLowerBoundClosed =
				_awAlgoType == AWAlgoType.RUNNING_AGGREGATE ? false : true;

		t_string = comp.getProperty("aggUpperBoundClosed");
		if (t_string != null)
			aggUpperBoundClosed = TextUtil.str2boolean(t_string);
		else // default is false for regular aggregates, true for running aggregates.
			aggUpperBoundClosed =
				_awAlgoType == AWAlgoType.RUNNING_AGGREGATE ? true : false;

		t_string = comp.getProperty("interpDeltas");
		if (t_string != null)
			interpDeltas = TextUtil.str2boolean(t_string);
		t_string = comp.getProperty("maxInterpIntervals");
		if (t_string != null)
		{
			try { maxInterpIntervals = Integer.parseInt(t_string); }
			catch(Exception ex)
			{
				log.atWarn()
				   .setCause(ex)
				   .log("Bad maxInterpIntervals property '{}' -- ignored.", maxInterpIntervals);
			}
		}

		t_string = comp.getProperty("aggregateTimeOffset");
		if (t_string != null)
		{
			try { aggregateTimeOffsetCalIncr = IntervalIncrement.parseMult(t_string); }
			catch(Exception ex)
			{
				log.atWarn()
				   .setCause(ex)
				   .log("Bad aggregateTimeOffset property '{}' -- ignored.", t_string);
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
		if (propVal == null || propVal.trim().length() == 0)
		{
			log.debug("Received property '{}' with null value -- ignored.", propName);
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
					log.atWarn()
					   .setCause(ex)
					   .log("Field '{}' requires an integer. Illegal value '{}' skipped.", propName, propVal);
				}

			}
			else
			{
				log.warn("Property '{}' has invalid local type -- ignored.", propName);
			}
		}
		catch(NumberFormatException ex)
		{
			log.atWarn()
			   .setCause(ex)
			   .log("Property '{}' could not be parsed. Required type is {}", propName, ftyp);
		}
		catch(Exception ex)
		{
			log.atWarn()
			   .setCause(ex)
			   .log("Property '{}' with no matching local variable -- ignored: ", propName);
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

		try
		{
			beforeAllTimeSlices();
			if (_awAlgoType == AWAlgoType.AGGREGATING
			 || _awAlgoType == AWAlgoType.RUNNING_AGGREGATE)
			{
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
			afterAllTimeSlices();
		}
		catch(RuntimeException ex)
		{
			throw new DbCompException("RunTime Error: "+ex+"\nAt: "+ex.getStackTrace()[0].toString(), ex);
		}
	}

	public void afterAllTimeSlices()
		throws DbCompException
	{
		// Nothing to do here.
	}

	public void beforeAllTimeSlices()
			throws DbCompException
	{
		// Nothing to do here.
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
				log.warn("Cannot do aggregating algorithm without a controlling output variable.");
				return;
			}
			ParmRef parmRef = getParmRef(_aggPeriodVarRoleName);
			if (parmRef == null)
			{
				log.warn("Unknown aggregate control output variable '{}'", _aggPeriodVarRoleName);
				return;
			}
			intervalS = parmRef.compParm.getInterval();
		}

		TreeSet<Date> inputBaseTimes = determineInputBaseTimes();

		log.debug("Aggregating period is {}', found {} base times in input data.",
				  intervalS, inputBaseTimes.size());
		if (inputBaseTimes.size() == 0)
			return;

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

			// 't' is last base time in the input data
			// Add agg period to it to find the upper limit of t's influence.
			aggCal.setTime(t);
			IntervalIncrement calIncr = IntervalCodes.getIntervalCalIncr(intervalS);
			if (calIncr == null)
				throw new DbCompException("Comp '" + comp.getName()
					+ "' Invalid interval string '" + intervalS + "'");
			aggCal.add(calIncr.getCalConstant(), calIncr.getCount());
			Date end = aggCal.getTime();


			// Now add times t+int, t+(2*int), t+(3*int), until we hit the end.
			String varName = getInputNames()[0];
			ParmRef inpParmRef = getParmRef(varName);
			calIncr = IntervalCodes.getIntervalCalIncr(
				inpParmRef.compParm.getInterval());
			aggCal.setTime(t);
			aggCal.add(calIncr.getCalConstant(), calIncr.getCount());

			while(aggCal.getTime().before(end)
				|| (this.aggUpperBoundClosed && aggCal.equals(end)))
			{
				t = aggCal.getTime();
				inputBaseTimes.add(t);

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
				log.trace("Special processing for double-closed boundaries. Just did period ending {}",baseTime);

				//MJM Original impl causes endless loop at DST change
				//				// bump the base time by a second temporarily to force it into
				//				// the next period. Then put it back.
				//				long msec = baseTime.getTime();
				//				baseTime.setTime(msec+1000L);
				//				aggPer = determineAggPeriod(baseTime, intervalS);
				//				baseTime.setTime(msec);
				// Fix provided by Mike Neilson SPK:
				if (!timesIterator.hasNext())
					break;
				baseTime = timesIterator.next();
				aggPer = determineAggPeriod(baseTime, intervalS);

				log.trace("New agg per: {} to {}", aggPer.getBegin(), aggPer.getEnd());
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

				log.debug("Doing aggregate period ({},{})",_aggregatePeriodBegin, _aggregatePeriodEnd);

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
				log.trace("Finished aggregate period that started at {}", _aggregatePeriodBegin);

				if (_awAlgoType == AWAlgoType.AGGREGATING
				 && !_saveOutputCalled
				 && !_deleteOutputCalled
				 && getOutputNames().length == 1
				 && _aggInputsDeleted)
				{
					log.trace("Auto-deleting output.");
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


		// int[2] containing Calendar constant and increment
		IntervalIncrement calIncr = IntervalCodes.getIntervalCalIncr(interval);

		log.trace("determineAggregatePeriod baseTime={}, interval={}, aggLowerBoundClosed={}, " +
				  "aggUpperBoundClosed={}",
				  baseTime, interval, this.aggLowerBoundClosed, this.aggUpperBoundClosed);

		if (calIncr == null)
		{
			log.warn("Aggregate control output variable '{}' is instantaneous " +
					 "-- cannot determine aggregate period.",
					 _aggPeriodVarRoleName);
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
			log.trace("... running aggregate, aggPeriodInterval set to '{}'", aggPeriodInterval);
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
			log.trace("lower={}, lowerInDaylight  = {}", dlower, lowerInDaylight);

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
				log.trace("Added 1 hour to upper because of daylight change.");
			}
			// else check for change from std to daylight
			else if (!lowerInDaylight && upperInDaylight
				&& calIncr.getCalConstant() == Calendar.HOUR_OF_DAY
				&& calIncr.getCount() >= 3 && calIncr.getCalConstant() <= 12)
			{
				aggCal.add(Calendar.HOUR_OF_DAY, -1);
				upper = aggCal.getTimeInMillis();
				dupper = new Date(upper);
				log.trace("Subtracted 1 hour to upper because of daylight change.");
			}
			log.trace("upper={}, upperInDaylight = {}", dupper, upperInDaylight);
		}

		AggregatePeriod ret = new AggregatePeriod(new Date(lower), new Date(upper));
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
	 * Clears all the application-level flag bits. These are the
	 * bits not reserved by the computation or variable infrastructure.
	 * @param v
	 */
	public void clearNonReservedFlags(NamedVariable v)
	{
		VarFlags.clearNonReserved(v);
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
			log.warn("Cannot get '{}' flag bits outside a time-slice.", name);
			return 0;
		}
		NamedVariable v = _timeSliceVars.findByName(name);
		if (v == null)
		{
			log.warn("Cannot get '{}' flag bits -- no variable with that name.", name);
			return 0;
		}
		return v.getFlags();
	}

	/**
	 * Sets the 1-bits in the 'bits' argument into the named variables flag.
	 * DOES NOT CLEAR ANY BITS. Use SetInputFlagBits with the additional mask
	 * arg to first clear bits.
	 * Also, sets the TO_WRITE flag causing this value to be written (along with the
	 * new flag bits) after all computations have been tried.
	 * @param name the name of the input variable
	 * @param bits the bits to set.
	 */
	public void setInputFlagBits(String name, int bits)
	{
		if (!_inTimeSlice)
		{
			log.warn("Cannot set '{}' flag bits outside a time-slice.", name);
			return;
		}
		NamedVariable v = _timeSliceVars.findByName(name);
		if (v == null)
		{
			log.warn("Cannot set '{}' flag bits -- no variable with that name.", name);
			return;
		}

		_saveOutputCalled = true;

		// If the specified bits are already set, do nothing.
		// This is crucial to avoid an endless loop in limit-check
		// algorithms.
		int oldFlags = v.getFlags();

		if ((oldFlags|bits) == oldFlags)
			return;

		// We also set ...
		//    TO_WRITE bit, causing it to be written back to DB.
		v.setFlags( v.getFlags() | bits | VarFlags.TO_WRITE );
	}

	/**
	 * Like setInputFlagBits above, but will first clear bits in the specified mask.
	 * @param name the name of the input variable
	 * @param bits the bits to set
	 * @param mask the bits to clear prior to setting
	 */
	public void setInputFlagBits(String name, int bits, int mask)
	{
		if (!_inTimeSlice)
		{
			log.warn("Cannot set '{}' flag bits outside a time-slice.", name);
			return;
		}
		NamedVariable v = _timeSliceVars.findByName(name);
		if (v == null)
		{
			log.warn("Cannot set '{}' flag bits -- no variable with that name.", name);
			return;
		}

		_saveOutputCalled = true;

		// If the specified bits are already set, do nothing.
		// This is crucial to avoid an endless loop in limit-check
		// algorithms.
		int oldFlags = v.getFlags();

		if ((oldFlags|bits) == oldFlags)
			return;


		// We also set ...
		//    TO_WRITE bit, causing it to be written back to DB.
		v.setFlags( (v.getFlags()&(~mask)) | bits | VarFlags.TO_WRITE );

		// CWMS-13771 Use the python repeat guard for validation comps to
		// prevent re-running the same computation.
		ParmRef parmRef = this.getParmRef(name);
		ComputationApp app = ComputationApp.instance();
		if (app != null)
			app.getResolver().pythonWrote(comp.getId(),
				parmRef.timeSeries.getTimeSeriesIdentifier().getKey());

	}

	protected void clearInputFlagBits(String name, int bits)
	{
		if (!_inTimeSlice)
		{
			log.warn("Cannot clear '{}' flag bits outside a time-slice.", name);
			return;
		}
		NamedVariable v = _timeSliceVars.findByName(name);
		if (v == null)
		{
			log.warn("Cannot clear '{}' flag bits -- no variable with that name.", name);
			return;
		}

		_saveOutputCalled = true;

		// If the specified bits are already clear, do nothing.
		// This is crucial to avoid an endless loop in limit-check
		// algorithms.
		int oldFlags = v.getFlags();

		if ((oldFlags & (~bits)) == oldFlags)
			return;

		// We also set ...
		//    TO_WRITE bit, causing it to be written back to DB.
		v.setFlags( (v.getFlags() & (~bits)) | VarFlags.TO_WRITE);
	}

	private void saveOutput(NamedVariable v)
	{
		if ((v.getFlags() & VarFlags.NO_OVERWRITE) != VarFlags.NO_OVERWRITE)
			VarFlags.setToWrite(v);
		_saveOutputCalled = true;

		// MJM 2015 06/30 If this is being called from setFlagBits, then it is
		// some kind of validation computation that has just derived the flag bits.
		// This method should not resort to the "Q-input = Q-output" rule.
		if (_questionOutput && !setFlagBitsCalled)
			tsdb.setQuestionable(v);

		// Set the output's source ID from the computation.
		v.setSourceId(comp.getDataSourceId());

		// Check overwrite flag property, only implemented for hdb (default false)
		if (tsdb.isHdb() && TextUtil.str2boolean(comp.getProperty("OverwriteFlag")))
		{
			v.setFlags(v.getFlags() | HdbFlags.HDBF_OVERWRITE_FLAG);
		}

		if (_inTimeSlice)
			_timeSliceVars.add(v);
		else // not in time slice save directly to time series
			 // with the aggregate base time.
		{
			if (_aggregatePeriodBegin == null)
			{
				log.warn("Cannot save '{}' Not an aggregating algorithm.", v.toString());
				return;
			}
			ParmRef parmRef = getParmRef(v.getName());
			if (parmRef == null)
			{
				log.warn("Cannot save '{}' no output parameter role defined!", v.toString());
				return;
			}

			// For normal aggregates, the output is based at the start of the
			// period. For running aggregates, the output is based at the end.
			Date varDate = this._awAlgoType == AWAlgoType.AGGREGATING ?
				_aggregatePeriodBegin : _aggregatePeriodEnd;
			Date aggD = parmRef.compParm.baseTimeToParamTime(varDate, aggCal);
			log.debug("Storing aggregate value={} basetime={}, parmtime={} parm deltaT={} ({})",
					  v.getStringValue(), varDate, aggD,
					  parmRef.compParm.getDeltaT(), parmRef.compParm.getDeltaTUnits());
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
				log.atWarn().setCause(ex).log("Error comparing existing aggregate output '{}'", oldTv);
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
			log.warn("Cannot save '{}' no output parameter role defined!", v.toString());
			return;
		}
		TimedVariable tv = new TimedVariable(v, t);
		tv.setSourceId(comp.getDataSourceId());
		VarFlags.setToWrite(tv);

		parmRef.timeSeries.addSample(tv);
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
				log.warn("Cannot delete '{}' Not an aggregating algorithm.", v.toString());
				return;
			}
			ParmRef parmRef = getParmRef(v.getName());
			if (parmRef == null)
			{
				log.warn("Cannot delete '{}' no output parameter role defined!", v.toString());
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
			log.warn("Cannot delete '{}' no output parameter role defined!", v.toString());
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
				log.atWarn().setCause(ex).log("Error in deleteAllOutputs.");
			}
		}
	}

	protected void getSliceInputs()
	{
		for(String varName : getInputNames())
		{
			ParmRef parmRef = getParmRef(varName);

			boolean unused = (parmRef == null || parmRef.compParm == null);
			// MJM 20150727 Optional parms that are unused in an algorithm should be
			// filled with the special value meaning MISSING.

			_sliceInputsDeleted = false;
			// Use reflection to find the variable with same name.
			// Then determine its type (double, long, or String).
			Class cls = null;
			NamedVariable v = null;
			try
			{
				if (!unused)
				{
					String nm = varName;
					String typ = parmRef.compParm.getAlgoParmType().toLowerCase();
				 	if (typ.length() > 1 && typ.charAt(1) == 'd')
				 		nm += "_d";
				 	v = _timeSliceVars.findByNameIgnoreCase(nm);
				}

				// note: getField only returns public members.
				// so first use getDeclaredFields to handle protected but accessible members.
				cls = this.getClass();
				Field field = getField(cls, varName);
				String ftyp = field.getType().getName();

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
					log.warn("Invalid input variable type '{}' -- ignored.", ftyp);
			}
			catch(IllegalAccessException ex)
			{
				log.atWarn()
				   .setCause(ex)
				   .log("Inconsistent class -- cannot access input field named '{}'", varName);
			}
			catch(NoSuchFieldException ex)
			{
				if (this instanceof PythonAlgorithm)
				{
					((PythonAlgorithm)this).setTimeSliceInput(varName, v);
				}
				else
				{
					log.atWarn().setCause(ex).log("Inconsistent class -- no input field named '{}'", varName);
				}
			}
			catch(NoConversionException ex)
			{
				log.atWarn().setCause(ex).log("Cannot convert input '{}' to correct input type.", varName);
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

	public String[] getPropertyNames()
	{
		List<String> ret = AnnotationHelpers.getFieldsWithAnnotation(this.getClass(), org.opendcs.annotations.PropertySpec.class)
										    .stream()
											.map(pair ->
											{
												return PropertySpec.getPropertyName(pair.first,pair.second);
											})
											.collect(Collectors.toList());
		return ret.toArray(new String[0]);
	}

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
					throw new DbCompException("Cannot find Coeff for null time, role="+rolename);
				}
				coeff = tsdb.getCoeff(sdi,ts,interval,_timeSliceBaseTime);
			}
			else if (_aggregatePeriodBegin == null)
			{
				throw new DbCompException("Cannot find Coeff for null time., role"+rolename);
			}
			else
			{
				coeff=tsdb.getCoeff(sdi,ts,interval,_aggregatePeriodBegin);
			}
		}
		catch (DbIoException ex)
		{
			throw new DbCompException("Cannot find Coeff for role "+ rolename, ex);
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
		final Class<?> clazz = this.getClass();
		return AnnotationHelpers.getFieldsWithAnnotation(clazz, org.opendcs.annotations.PropertySpec.class)
							    .stream()
							    .map(p ->
								{
									final Field f = p.first;
									final org.opendcs.annotations.PropertySpec propSpec = p.second;
									final String name = PropertySpec.getPropertyName(f, propSpec);
									String specType = PropertySpec.getSpecTypeFromAnnotation(propSpec, f);
									return new PropertySpec(name, specType, propSpec.description());
								})
								.collect(Collectors.toList())
								.toArray(new PropertySpec[0]);
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
		allprops = new PropertySpec[basePropertySpecs.length +
		    (_awAlgoType == AWAlgoType.TIME_SLICE ? 0 : aggAlgoPropertySpecs.length)
		    + algoProps.length];
		int idx = 0;
		for(int i=0; i<basePropertySpecs.length; i++)
			allprops[idx++] = basePropertySpecs[i];
		if (_awAlgoType != AWAlgoType.TIME_SLICE)
			for(int i=0; i<aggAlgoPropertySpecs.length; i++)
				allprops[idx++] = aggAlgoPropertySpecs[i];
		for(int i=0; i < algoProps.length; i++)
			allprops[idx++] = algoProps[i];
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


	/**
	 * Return true if the named param has a value in the current timeslice
	 * and that value is flagged as good quality. That is, it is not flagged
	 * as questionable, rejected, missing, or to-delete.
	 * This method only works for CWMS.
	 * @param rolename
	 * @return
	 */
	public boolean isGoodQuality(String rolename)
	{
		NamedVariable nv = _timeSliceVars.findByName(rolename);
		if (nv == null)
			return false;
		int f = nv.getFlags();

		if (tsdb.isCwms())
		{
			return (f & (CwmsFlags.VALIDITY_REJECTED | CwmsFlags.VALIDITY_QUESTIONABLE
				| IFlags.IS_MISSING | VarFlags.TO_DELETE)) == 0;
		}
		else if (tsdb.isHdb())
		{
			return HdbFlags.isGoodQuality(f);
		}
		else
			return false;
	}

	/**
	 * Return true if the named param is a triggering value for this computation
	 * at the current time slice.
	 * @return
	 */
	public boolean isTrigger(String rolename)
	{
		NamedVariable nv = _timeSliceVars.findByName(rolename);
		if (nv == null)
			return false;
		int f = nv.getFlags();
		boolean ret = (f & VarFlags.DB_ADDED) != 0;
		return ret;
	}
}
