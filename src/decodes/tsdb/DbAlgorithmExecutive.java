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
*  Revision 1.11  2017/05/31 21:27:18  mmaloney
*  Improvement to getParmTsId
*
*  Revision 1.10  2017/05/25 21:18:45  mmaloney
*  In DbAlgorithmExecutive, apply roundSec when searching for values in database.
*  In CTimeSeries.findWithin, the upperbound should be t+fudge/2-1.
*  See comments in code dated 20170525.
*
*  Revision 1.9  2016/12/16 14:37:45  mmaloney
*  Improved debugs on exceeding max time for missing.
*
*  Revision 1.8  2016/09/23 15:57:21  mmaloney
*  Improve warning messages when MISSING values cannot be recovered because time is too long. The new messages show the relevant limit values.
*
*  Revision 1.7  2016/03/24 19:12:14  mmaloney
*  Refactoring for Python.
*
*  Revision 1.6  2016/01/27 22:02:08  mmaloney
*  Fix for CWMS-7386 that occurs when MISSING=PREV used in conjunction with
*  automatic deltas.
*
*  Revision 1.5  2015/11/18 14:06:23  mmaloney
*  Get rid of 'getBriefDescription()' after all.
*
*  Revision 1.4  2015/10/22 14:01:42  mmaloney
*  CCP bug fix: If tasklist contained both inputs and output values for a comp,
*  the old code was not converting the units of the existing values. It was just
*  writing values in the output units, leaving existing ones alone.
*
*  Revision 1.3  2015/08/31 00:38:33  mmaloney
*  added getDataCollection() method.
*
*  Revision 1.2  2015/01/15 19:25:45  mmaloney
*  RC01
*
*  Revision 1.1.1.1  2014/05/19 15:28:59  mmaloney
*  OPENDCS 6.0 Initial Checkin
*
*  Revision 1.74  2013/08/19 12:58:34  mmaloney
*  dev
*
*  Revision 1.73  2013/08/18 19:48:45  mmaloney
*  Implement EffectiveStart/End relative properties
*
*  Revision 1.72  2013/03/21 18:27:39  mmaloney
*  DbKey Implementation
*
*/
package decodes.tsdb;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.TreeSet;
import java.util.HashMap;
import java.util.GregorianCalendar;
import java.util.Calendar;
import java.util.TimeZone;

import opendcs.dai.TimeSeriesDAI;
import ilex.util.Logger;
import ilex.util.TextUtil;
import ilex.var.NamedVariableList;
import ilex.var.NoConversionException;
import ilex.var.NamedVariable;
import ilex.var.TimedVariable;
import decodes.db.Constants;
import decodes.db.EngineeringUnit;
import decodes.db.Site;
import decodes.db.SiteName;
import decodes.db.UnitConverter;
import decodes.sql.DbKey;
import decodes.util.DecodesException;
import decodes.util.DecodesSettings;
import decodes.util.TSUtil;

/**
 * This is the base class for all computational algorithms.
 * It provides the interface to the framework.
 * It also provides several helper methods available to the
 * subclasses.
 */
public abstract class DbAlgorithmExecutive
{
	/**
	 * The data collection passed to the 'apply' method.
	 */
	protected DataCollection dc;
	
	/**
	 * The time series database passed to the init method.
	 */
	protected TimeSeriesDb tsdb;
	
	/**
	 * The computation meta-data for this instantiation of the algorithm.
	 */
	protected DbComputation comp;

	/**
	 * Maps role name to a ParmRef object, providing an easy way to find
	 * meta and time-series data given a role name.
	 */
	private HashMap<String, ParmRef> parmMap;

	/** For time rounding -- number of seconds */
	public int roundSec;

	/** Override the aggregateTimeZone property if you need a time zone different
	 * from the one set in decodes.properties.
	 */
	protected String aggregateTimeZone = null;
	protected TimeZone aggTZ = null;

	/** Gregorian Calendar to use for determining aggregate periods: */
	public GregorianCalendar aggCal = null;

	private int maxMissingValuesForFill;
	private int maxMissingTimeForFill;

	/** Determines open/closed intervals for aggregate periods. 
	 * The default is [lower,upper)
	 */
	protected boolean aggLowerBoundClosed = true;

	/** Determines open/closed intervals for aggregate periods.
	 * The default is [lower,upper)
	 */
	protected boolean aggUpperBoundClosed = false;
	
	/** If true, than deltas can be interpolated up to maxDeltaInterp intervals */
	protected boolean interpDeltas = false;
	
	/** If (interpDeltas) this is max # of intervals to interp over. */
	protected int maxInterpIntervals = 10;

	public SimpleDateFormat debugSdf = new SimpleDateFormat("yyyy/MM/dd-HH:mm:ss z");
	
	protected Date effectiveStart = null;
	protected Date effectiveEnd = null;
	
	/**
	 * No-args Constructor because object is constructed from the class name.
	 */
	protected DbAlgorithmExecutive()
	{
		aggregateTimeZone = DecodesSettings.instance().aggregateTimeZone;
		aggTZ = TimeZone.getTimeZone(aggregateTimeZone);
		aggCal = new GregorianCalendar(aggTZ);
		dc = null;
		tsdb = null;
		comp = null;
		parmMap = new HashMap<String, ParmRef>();
		roundSec = 60;
		debugSdf.setTimeZone(aggTZ);
	}

	/**
	 * Sets the computation and time series database. Then calls
	 * initAlgorithm for any specific initialization.
	 * This method is called once after construction, not before each
	 * apply call.
	 * <p>
	 * The subclass should not overload this method. Rather, overload
	 * initAlgorithm().
	 *
	 * any one-time initialization. If it does, it should call super.init()
	 * defined here.
	 */
	public void init( DbComputation comp, TimeSeriesDb tsdb )
		throws DbCompException
	{
		this.comp = comp;
		this.tsdb = tsdb;

		DbCompAlgorithm algo = comp.getAlgorithm();

		this.maxMissingValuesForFill = DecodesSettings.instance().maxMissingValuesForFill;
		String s = comp.getProperty("maxMissingValuesForFill");
		if (s == null)
			s = algo.getProperty("maxMissingValuesForFill");
		if (s != null)
		{
			try { maxMissingValuesForFill = Integer.parseInt(s.trim()); }
			catch(NumberFormatException ex)
			{
				this.maxMissingValuesForFill = DecodesSettings.instance().maxMissingValuesForFill;
				warning("Bad maxMissingValuesForFill property '" + s 
					+ "' will use default of " + maxMissingValuesForFill);
			}
		}

		this.maxMissingTimeForFill = DecodesSettings.instance().maxMissingTimeForFill;
		s = comp.getProperty("maxMissingTimeForFill");
		if (s == null)
			s = algo.getProperty("maxMissingTimeForFill");
		if (s != null)
		{
			try { maxMissingTimeForFill = Integer.parseInt(s.trim()); }
			catch(NumberFormatException ex)
			{
				this.maxMissingTimeForFill = DecodesSettings.instance().maxMissingTimeForFill;
				warning("Bad maxMissingTimeForFill property '" + s 
					+ "' will use default of " + maxMissingTimeForFill);
			}
		}

		parseTimeRound();

		// MJM 20160312 Moved initAlgorithm to BEFORE the mapParm calls.
		// This give PythonAlgorithm a chance to replace the dummy input/output parm lists
		// with the parms defined in the algorithm record.
		initAlgorithm();
		
		// Construct the parm map as a convenience to the subclass.
		for(String role : getInputNames())
			mapParm(role, tsdb);
		for(String role : getOutputNames())
			mapParm(role, tsdb);

//		initAlgorithm();
	}

	/**
	 * Must be called at start of each apply(), not just in init() because
	 * init() is called only once after instantiation.
	 */
	private void evaluateEffectiveRange()
	{
		if ((effectiveStart = comp.getValidStart()) == null)
		{
			String s = comp.getProperty("EffectiveStart");
			if (s == null || s.trim().length() == 0)
				s = DecodesSettings.instance().CpEffectiveStart;

			// Should be in the form 'now - N intervalname'
			if (s != null && s.trim().length() > 0)
			{
				int idx = s.indexOf('-');
				if (idx == -1 || idx == s.length()-1)
					warning("Invalid EffectiveStart property '" + s + "' -- ignored.");
				else
				{
					s = s.substring(idx+1).trim();
					try
					{
						IntervalIncrement [] iia = IntervalIncrement.parseMult(s);
						IntervalIncrement ii = iia[0];
						aggCal.setTime(new Date());
						aggCal.add(ii.getCalConstant(), -ii.getCount());
						effectiveStart = aggCal.getTime();
					}
					catch(Exception ex)
					{
						Logger.instance().warning("Cannot parse EffectiveStart '" 
							+ s + "': " + ex.getMessage());
					}
				}
			}
		}
		debug1("Effective Start evaluates to: " + 
			(effectiveStart == null ? "NULL" : debugSdf.format(effectiveStart)));

		if ((effectiveEnd = comp.getValidEnd()) == null)
		{
			String s = comp.getProperty("EffectiveEnd");
			// Should be in the form 'now + intervalname'
			if (s != null && s.trim().length() > 0)
			{
				if (s.equalsIgnoreCase("now"))
					effectiveEnd = new Date();
				else
				{
					int idx = s.indexOf('+');
					if (idx == -1 || idx == s.length()-1)
						warning("Invalid EffectiveEnd property '" + s + "' -- ignored.");
					else
					{
						s = s.substring(idx+1).trim();
						try
						{
							IntervalIncrement [] iia = IntervalIncrement.parseMult(s);
							IntervalIncrement ii = iia[0];
							aggCal.setTime(new Date());
							aggCal.add(ii.getCalConstant(), ii.getCount());
							effectiveEnd = aggCal.getTime();
						}
						catch(Exception ex)
						{
							Logger.instance().warning("Cannot parse EffectiveEnd '" 
								+ s + "': " + ex.getMessage());
						}
					}
				}
			}
		}
		debug1("Effective End evaluates to: " + 
			(effectiveEnd == null ? "NULL" : debugSdf.format(effectiveEnd)));
	}

	/**
	 * Called when the computation process is about to exit. The algorithm
	 * should close any open resources, etc.
	 * The default implementation here does nothing.
	 */
	public void shutdown()
	{
	}
	
	/**
	 * Sets the internal 'dc' data collection variable and calls 
	 * applyAlgorithm().
	 * The subclass probably does not need to overload this method. Rather,
	 * overload 'applyAlgorithm'.
	 *
	 * @param dc the data collection to act on.
	 * @throws DbCompException on computation error.
	 * @throws DbIoException on IO error to database.
	 */
	public void apply( DataCollection dc )
		throws DbCompException, DbIoException
	{
		this.dc = dc;
debug3("DbAlgorithmExec.apply()");

		evaluateEffectiveRange();
		determineModelRunId(dc);

		// Add the time series to the parm-references for inputs.
		// If any are modeled, use the modelRunId we determined above.
		for(String role : getInputNames())
			addTsToParmRef(role, false);
		for(String role : getOutputNames())
			addTsToParmRef(role, true);

		applyAlgorithm();
	}

	/**
	 * We should have at least one input already present in the data.
	 * Use Case 1: Triggered on Modeled input: Go through inputs, if any 
	 * are modeled, and we have data for it, then grab its modelRunId. 
	 * Use Case 2: We use modeled data, but triggered on non-modeled input:
	 * find the latest modelRunId for the parameter's modelId.
	 * The resulting modelRunId is set in the DbComputation object.
	 */
	private void determineModelRunId(DataCollection dc)
	{
		// will get set if we have any modeled inputs:
		int modelId = Constants.undefinedIntKey;
		comp.setModelRunId(Constants.undefinedIntKey);

		for(String role : getInputNames())
		{
			ParmRef ref = getParmRef(role);
			if (ref == null || ref.compParm == null)
				continue;
			String tabsel = ref.compParm.getTableSelector();
			if (tabsel != null && tabsel.equals("M_"))
			{
				String intv = ref.compParm.getInterval();
				modelId = ref.compParm.getModelId();
				CTimeSeries cts = dc.getTimeSeriesForModelId(
					ref.compParm.getSiteDataTypeId(), intv, tabsel, modelId);
				if (cts != null)
				{
					debug1("Computation input with modelId=" + modelId
						+ " and modelRunId=" + cts.getModelRunId());
					comp.setModelRunId(cts.getModelRunId());

					// For multiple inputs, we assume all are from
					// same model and model-run.
					return;
				}
			}
		}
		// Handle case where this comp uses modeled data but it was triggered
		// by another non-modeled input.
		if (modelId != Constants.undefinedIntKey)
		{
			try { comp.setModelRunId(tsdb.findMaxModelRunId(modelId)); }
			catch(DbIoException ex)
			{
				warning("Cannot retrieve max model run ID for modelID="
					+ modelId + ": " + ex);
			}
		}
	}

	/**
	 * Maps a single parameter, called from apply for each input/output role.
	 */
	private void mapParm(String role, TimeSeriesDb tsdb)
	{
		debug1("mapParm(" + role + ")");
		DbCompParm parm = comp.getParm(role);
		if (parm == null)
		{
			debug1("No param defined for role '" + role + "'");
			return;
		}

		TimeSeriesIdentifier tsid = null;
		try
		{
			tsid = tsdb.expandSDI(parm);
		}
		catch(Exception ex)
		{
			warning("Cannot expand meta data for role '" + role + "': " + ex);
			return;
		}

		ParmRef parmRef = new ParmRef(role, parm, null);
		if (tsid != null)
			parmRef.tsid = tsid;

		// Retrieve the 'missing action' property, either specific to this
		// role, or global for the whole algorithm.
		// Specific one is <role>_MISSING
		String propval = comp.getProperty(role + "_MISSING");
		parmRef.setMissingAction(MissingAction.fromString(propval));

		parmMap.put(role, parmRef);
	}

	private void addTsToParmRef(String role, boolean isOutput)
	{
		// Some params may be optional and not defined in a computation.
		ParmRef ref = getParmRef(role);
		if (ref == null || ref.compParm == null)
			return;

		String intv = ref.compParm.getInterval();
		String tabsel = ref.compParm.getTableSelector();
		
		// Get modelRunId of the input(s) that triggered the computation.
		// This would have been set by determineModelRunId() above.
		int modelRunId = comp.getModelRunId();
		int modelId = ref.compParm.getModelId();
		
		// If this is a modeled output, we have to set its modelRunId
		if (isOutput && TextUtil.strEqualIgnoreCase(tabsel, "M_")
		 && modelId != Constants.undefinedIntKey)
		{
			// If a global config is set for the DB, this overrides the inputs
			if (tsdb.getWriteModelRunId() != Constants.undefinedIntKey)
				modelRunId = tsdb.getWriteModelRunId();
			
			// If a comp property "WriteModelRunId" is set, this overrides the global.
			String s = comp.getProperty("WriteModelRunId");
			if (s != null)
			{
				try { modelRunId = Integer.parseInt(s.trim()); }
				catch(Exception ex)
				{
					warning("Bad WriteModelRunId property '" + s + "' -- ignored.");
				}
			}
			
			// Finally, if no modeled input AND no property, use max RunId for this model.
			if (modelRunId == Constants.undefinedIntKey)
			{
				try
				{
					modelRunId = tsdb.findMaxModelRunId(modelId);
				}
				catch (DbIoException ex)
				{
					warning("Cannot determine modelRunId for modelId=" + modelId + ": " + ex);
				}
			}
			
		}
		ref.timeSeries = dc.getTimeSeries(ref.compParm.getSiteDataTypeId(), intv, tabsel, modelRunId);
		if (ref.timeSeries == null)
		{
			ref.timeSeries = new CTimeSeries(ref.compParm);
			if (TextUtil.strEqualIgnoreCase(tabsel, "M_"))
				ref.timeSeries.setModelRunId(modelRunId);
			if (ref.tsid != null)
				ref.timeSeries.setTimeSeriesIdentifier(ref.tsid);
			debug1("addTsToParmRef: Made empty time series for " 
				+ ref.getDescription() 
				+ ", sdiIsUnique=" + TimeSeriesDb.sdiIsUnique
				+ ", sdi=" + ref.compParm.getSiteDataTypeId() + ", intv='" 
				+ intv + "', + tabsel='" + tabsel + "'");
			

			try { dc.addTimeSeries(ref.timeSeries); }
			catch(DuplicateTimeSeriesException ex)
			{
				// won't happen, but warn if it does.
				warning("Can't add time series " + ex);
			}
		}
		else
			debug1("addTsToParmRef: Mapping existing time series for " 
				+ ref.getDescription() + " num samples = " + ref.timeSeries.size());

		if (isOutput)
			ref.timeSeries.setComputationId(comp.getId());
		
		// Make sure params are in the correct units.
		String propName = ref.role + "_EU";
		String neededEU = comp.getProperty(propName);
Logger.instance().debug3("addTsToParmRef: propName='" + propName + "' neededEU='" + neededEU + "'");
		if (neededEU != null)
		{
			String tsEU = ref.timeSeries.getUnitsAbbr();
			debug3("role='" + role + "', old units='" + tsEU + "' neededEU='" + neededEU + "'");
			if (tsEU != null && !tsEU.equals("unknown")
			 && !neededEU.equalsIgnoreCase(tsEU))
				TSUtil.convertUnits(ref.timeSeries, neededEU);
			
			// Note: Even if we did no conversion, still set the units abbreviation.
			ref.timeSeries.setUnitsAbbr(neededEU);
		}
	}

	/**
	 * Should be overloaded by subclass to return an array of all input
	 * parameter names.
	 * If no input params, return an empty array.
	 */
	abstract public String[] getInputNames( );
	
	/**
	 * Should be overloaded by subclass to return an array of all output
	 * parameter names.
	 * If no output params, return an empty array.
	 */
	abstract public String[] getOutputNames( );

	/**
	 * Find widest time range for all input params that are flagged
	 * DB_ADDED, taking into consideration the deltaT values.
	 * Retrieve correct time ranges for other input params.
	 * @return list of base time values sorted in ascending order.
	 */
	protected TreeSet<Date> getAllInputData( )
		throws DbIoException
	{
		// Step 1: Construct a list of base times for all DB_ADDED data.
		TreeSet<Date> baseTimes = determineInputBaseTimes();

		// Step 2: Query for missing data
		for(String role : getInputNames())
		{
			ParmRef parmRef = parmMap.get(role);
			if (parmRef == null)
			{
//				debug2("Skipping unassigned role '" + role + "'");
				continue;
			}
			TreeSet<Date> queryTimes = new TreeSet<Date>();
			for(Date bd : baseTimes)
			{
				Date paramTime = parmRef.compParm.baseTimeToParamTime(bd, aggCal);
				if (parmRef.timeSeries.findWithin(paramTime, roundSec/2) == null)
				{
debug3("getAllInputData: role=" + role + ", baseTime=" 
+ debugSdf.format(bd) + ", paramTime=" + debugSdf.format(paramTime) + ", nsamps=" + parmRef.timeSeries.size());
					queryTimes.add(paramTime);
				}
			}
			int qts = queryTimes.size();
			if (qts == 0)
				; // Already have everything -- Do nothing.
			else if (qts == 1)
				singleQuery(parmRef, queryTimes.first());
			else if (!tryRangeQuery(parmRef, queryTimes))
				inClauseQuery(parmRef, queryTimes);
		}

		expandForMissing(baseTimes);
		expandForDeltas(baseTimes);

		return baseTimes;
	}


	/**
	 * Handle cases where we need additional data outside the base times
	 * in order to compute an interpolated value.
	 * Called in getAllInputData AFTER we've already tried to read
	 * the input parm values at each base time.
	 * So if we're missing the first or last, get the one before or after it.
	 * Internal missing values can already be interpolated/snapped/etc.
	 */
	private void expandForMissing(TreeSet<Date> baseTimes)
		throws DbIoException
	{
		debug3("expandForMissing num baseTimes=" + baseTimes.size());
		if (baseTimes.size() == 0)
			return;

		TimeSeriesDAI timeSeriesDAO = tsdb.makeTimeSeriesDAO();
		try
		{
			Date firstBaseTime = baseTimes.first();
			Date lastBaseTime = baseTimes.last();
			
			for(String role : getInputNames())
			{
				ParmRef parmRef = parmMap.get(role);
				if (parmRef == null
				 || parmRef.missingAction == MissingAction.FAIL
				 || parmRef.missingAction == MissingAction.IGNORE)
					continue;
	
				Date firstParamTime = parmRef.compParm.baseTimeToParamTime(firstBaseTime, aggCal);
				Date lastParamTime = parmRef.compParm.baseTimeToParamTime(lastBaseTime, aggCal);
debug3("expandForMissing1 role=" + role);			
				TimedVariable firstTv = 
					parmRef.timeSeries.findWithin(firstParamTime.getTime()/1000L, roundSec/2);
debug3("expandForMissing2 role=" + role);			
				TimedVariable lastTv = 
					parmRef.timeSeries.findWithin(lastParamTime.getTime()/1000L, roundSec/2);
	
				if (firstTv == null
				 && (  parmRef.missingAction == MissingAction.PREV
					|| parmRef.missingAction == MissingAction.INTERP
					|| parmRef.missingAction == MissingAction.CLOSEST))
				{
					// The first value is missing & we need it!
					try 
					{
debug3("expandForMissing3 role=" + role);			
						timeSeriesDAO.getPreviousValue(parmRef.timeSeries, firstParamTime);
					}
					catch(BadTimeSeriesException ex) 
					{
						Logger.instance().warning("expandForMissing: " + ex);
					}
				}
	
				if (lastTv == null
				 && (  parmRef.missingAction == MissingAction.NEXT
					|| parmRef.missingAction == MissingAction.INTERP
					|| parmRef.missingAction == MissingAction.CLOSEST))
				{
					// The last value is missing & we need it!
					try
					{
debug3("expandForMissing4 role=" + role);			
						timeSeriesDAO.getNextValue(parmRef.timeSeries, lastParamTime);
					}
					catch(BadTimeSeriesException ex) 
					{
						Logger.instance().warning("expandForMissing: " + ex);
					}
				}
			}
		}
		finally
		{
			timeSeriesDAO.close();
		}
	}

	/**
	 * Base times are newly-written values. A changed value at this time
	 * will affect the delta from prev-this, and from this-next. Therefore
	 * we need to use the delta interval and retrieve the prev and next
	 * values.
	 * Example: Computing a 1-day delta with type "id1Day" or "id1440" and
	 * I get a message with 4 15Min values 12:00, 12:15, 12:30, and 12:45.
	 * Here's what should happen:
	 * 1. Retrieve 12:00, 12:15, 12:30, and 12:45 for the previous day
	 * 2. Retrieve 12:00, 12:15, 12:30, and 12:45 for the next day
	 * 3. Add to base times 12:00, 12:15, 12:30, and 12:45 for the next day
	 */
	private void expandForDeltas(TreeSet<Date> baseTimes)
		throws DbIoException
	{
		debug3("expandForDeltas num baseTimes=" + baseTimes.size());
		// Quick way to detect no input data.
		if (baseTimes.size() == 0)
			return;
		
		for(String role : getInputNames())
		{
			ParmRef parmRef = parmMap.get(role);
			if (parmRef == null)
				continue;
debug3("expandForDeltas 1 role='" + role + "'");

			String typ = parmRef.compParm.getAlgoParmType().toLowerCase();
			String intv = parmRef.compParm.getInterval();
			CTimeSeries cts = parmRef.timeSeries;
			int nsamps = cts.size();

			if (typ.length() <= 1 || typ.charAt(1) != 'd')
				continue; // not a delta.

			debug3("expandForDeltas parm '" + role + "' type='" 
				+ typ + "', nsamps=" + nsamps);

			// These are the times we will request with an IN() clause.
			ArrayList<Date> inTimes = new ArrayList<Date>();

			// This map keeps track of new base times that must be added.
			HashMap<Date, Date> paramTimeBaseTime = new HashMap<Date, Date>();

			for(Date baseTime : baseTimes)
			{
				// Add in this param's delta T to get param time
				Date paramTime = parmRef.compParm.baseTimeToParamTime(baseTime, aggCal);

				// Call computeDeltaMsec to subtract delta to get PREV value time
				// If this time is not currently in the time series, add to inTimes
				long prevMS = computeDeltaMsec(paramTime.getTime(), typ, intv, true);
				Date prevDate = new Date(prevMS);
				if (prevMS != 0L
				 && cts.findWithin(prevMS/1000, roundSec/2) == null
				 && !inTimes.contains(prevDate))
					inTimes.add(prevDate);
				
				// call computeDeltaMsec to add delta to get NEXT value time
				// if this time is not currently in the time series, add to inTimes
				long nextMS = computeDeltaMsec(paramTime.getTime(), typ, intv, false);
				Date nextDate = new Date(nextMS);
				if (nextMS != 0L
				 && cts.findWithin(nextMS/1000, roundSec/2) == null
				 && !inTimes.contains(nextDate))
					inTimes.add(nextDate);

debug3("expandForDeltas parm '" + role + "' baseTime="
+ debugSdf.format(baseTime) + ", prev=" + debugSdf.format(prevDate)
+ ", next=" + debugSdf.format(nextDate));
				
				// I will need to perform the comp at this NEXT time, so ADD 
				// deltaT back to get normalized baseTime
				//        Add it to paramTimeBaseTime
				Date nextParamTime = new Date(nextMS);
				Date nextBaseTime = parmRef.compParm.paramTimeToBaseTime(
					nextParamTime, aggCal);
				paramTimeBaseTime.put(nextParamTime, nextBaseTime);
			}
			
			// inTimes now contains all the values I need to read from the DB.
			if (inTimes.size() == 0)
				continue; // Don't need anything! Go to next param.
			
			TimeSeriesDAI timeSeriesDAO = tsdb.makeTimeSeriesDAO();
			try
			{
				int numRetrieved = timeSeriesDAO.fillTimeSeries(cts, inTimes);
	
				// Some may have not been retrieved. 
				// If interpDeltas is true, see if I can interpolate the missing ones.
				if (numRetrieved != inTimes.size() && interpDeltas)
				{
					for(Date t : inTimes)
						if (cts.findWithin(t.getTime()/1000, roundSec/2) == null)
						{
							// Try to put the flanking values in the time series.
							if (timeSeriesDAO.getPreviousValue(cts, t) != null)
								timeSeriesDAO.getNextValue(cts, t);
							// NOTE the interpolation is done in iterateTimeSlices, not here.
						}
				}
			}
			catch(BadTimeSeriesException ex) 
			{
				Logger.instance().warning("expandForDeltas: " + ex);
				ex.printStackTrace(Logger.instance().getLogOutput());
			}
			finally
			{
				timeSeriesDAO.close();
			}
			
			// We need to add the new base times so that the computation gets
			// executed at the NEXT time.
			for(Date paramTime : paramTimeBaseTime.keySet())
			{
				Date baseTime = paramTimeBaseTime.get(paramTime);
				if ((cts.findWithin(paramTime.getTime()/1000, roundSec/2) != null)
				 && !baseTimes.contains(baseTime))
				{
debug3("Adding new base time " + debugSdf.format(baseTime) 
	+ " to compute NEXT delta.");
					baseTimes.add(baseTime);
				}
			}
		}
	}

	/**
	 * Return the time (in milliseconds) of previous value for specified delta.
	 * @param firstMsec the millisecond of the value we need the delta for
	 * @param algoParmType the type code, e.g. "idh" for hourly delta
	 * @param tsInterval the interval code for the comp-parm = time-series interval.
	 * @return the millisecond time of previous value for specified delta.
	 */
	private long computeDeltaMsec(long firstMsec, String algoParmType, 
		String tsInterval, boolean subtract)
	{
		long qMsec = firstMsec;

		// Default to implicit interval set from the comp-parm record (ts interval)
		String deltaIntv = tsInterval;

		// Algo parm type has explicite interval if len > 2.
		if (algoParmType.length() > 2)
			deltaIntv = algoParmType.substring(2);
		deltaIntv = deltaIntv.toLowerCase();
		
		// if not 'last' specified, convert to a normalized representation.
		if (!deltaIntv.startsWith("l")) 
			deltaIntv = IntervalCodes.getDeltaSpec(deltaIntv);

		if (deltaIntv == null || deltaIntv.length() == 0)
			return 0L;
		
//Logger.instance().debug3("DbAlgorithmExecutive.computeDeltaMsec: intv='" + tsInterval
//+ "', deltaIntv=" + deltaIntv);
		
		int incr = subtract ? -1 : 1;

		aggCal.setTimeInMillis(firstMsec);
		if (deltaIntv.equals("h"))
			qMsec += (3600000L * incr);
		else if (deltaIntv.equals("d")) 
		{
			aggCal.add(Calendar.DAY_OF_YEAR, incr);
			qMsec = aggCal.getTimeInMillis();
		}
		else if (deltaIntv.equals("m")) 
		{
			aggCal.add(Calendar.MONTH, incr);
			qMsec = aggCal.getTimeInMillis();
		}
		else if (deltaIntv.equals("y")) 
		{
			aggCal.add(Calendar.YEAR, incr);
			qMsec = aggCal.getTimeInMillis();
		}
		else if (deltaIntv.startsWith("l") && deltaIntv.length() > 1)
		{
			// Cannot do forward deltas if they specify 'last'
			if (!subtract)
				return 0L;
			
			aggCal.setTime(new Date(firstMsec - 1000)); // 1 sec ago.
			aggCal.set(Calendar.MINUTE, 0);
			aggCal.set(Calendar.SECOND, 0);
			if (deltaIntv.equals("lh"))
			{
			}
			else if (deltaIntv.equals("ld"))
			{
				aggCal.set(Calendar.HOUR_OF_DAY, 0);
			}
			else if (deltaIntv.equals("lm"))
			{
				aggCal.set(Calendar.HOUR_OF_DAY, 0);
				aggCal.set(Calendar.DAY_OF_MONTH, 1);
			}
			else if (deltaIntv.equals("ly"))
			{
				aggCal.set(Calendar.HOUR_OF_DAY, 0);
				aggCal.set(Calendar.DAY_OF_MONTH, 1);
				aggCal.set(Calendar.DAY_OF_YEAR, 1);
			}
			else if (deltaIntv.equals("lwy"))
			{
				aggCal.set(Calendar.HOUR_OF_DAY, 0);
				aggCal.set(Calendar.DAY_OF_MONTH, 1);
				int month = aggCal.get(Calendar.MONTH);
				aggCal.set(Calendar.MONTH, 8);	// Note Sep == Month 8
				if (month < 8)
					aggCal.add(Calendar.YEAR, -1);
			}
			qMsec = aggCal.getTimeInMillis();
		}
		else if (deltaIntv.length() > 1
			&& Character.isDigit(deltaIntv.charAt(0)))
		{
			try
			{
				int e=1;
				while(e < deltaIntv.length() && Character.isDigit(deltaIntv.charAt(e)))
					e++;
				int minutes = Integer.parseInt(deltaIntv.substring(0, e));
				qMsec += (60000L*minutes*incr);
			}
			catch(NumberFormatException ex)
			{
				warning("Cannot determine # minutes from '" + deltaIntv + "': " + ex);
				qMsec = 0L;
			}
		}
		return qMsec;
	}

	/**
	 * Gets all input data within a given base-time range.
	 * That is, all data for all input time series where
	 * <p>
	 * (since  <=  sample-time  <=  until).
	 * <p>
	 * This method is intended for use by aggregating algorithms like a 
	 * periodic average or sum. The algorithm either knows the period 
	 * intrinsically, or it is supplied by properties.
	 * @param since the time range start.
	 * @param until the time range end.
	 * @return a sorted set of timestamps within the specified period.
	 */
	protected TreeSet<Date> getAllInputData( Date since, Date until )
		throws DbIoException
	{
		TreeSet<Date> baseTimes = new TreeSet<Date>();
		long sinceMsec = since.getTime();
		long untilMsec = until.getTime();

		TimeSeriesDAI timeSeriesDAO = tsdb.makeTimeSeriesDAO();
		try
		{
			for(String role : getInputNames())
			{
				ParmRef parmRef = parmMap.get(role);
				if (parmRef == null)
				{
					warning("(since,until)Skipping unassigned role '" + role + "'");
					continue;
				}
	
				try
				{
					Date paramSince = parmRef.compParm.baseTimeToParamTime(since, aggCal);
					Date paramUntil = parmRef.compParm.baseTimeToParamTime(until, aggCal);
					
					// Don't retrieve data we already have.
					
					// Don't call this method until we resolve the upper/lower bounds
					// issue. If we adjust the time range, we need to be inclusive
					// on both ends.
	//				trimRangeForDataAlreadyRetrieved(parmRef.timeSeries, st, ut);
	
					if (paramSince.compareTo(paramUntil) <= 0)
					{
						timeSeriesDAO.fillTimeSeries(parmRef.timeSeries, paramSince, paramUntil,
							aggLowerBoundClosed, aggUpperBoundClosed, false);
					}
					int sz = parmRef.timeSeries.size();
					for(int i=0; i<sz; i++)
					{
						Date sampParamTime = 
							parmRef.timeSeries.sampleAt(i).getTime();
						Date sampBaseTime = 
							parmRef.compParm.paramTimeToBaseTime(sampParamTime, aggCal);
						long sampMsec = sampBaseTime.getTime();
						
						boolean aboveLowerBound = 
							aggLowerBoundClosed ? sampMsec >= sinceMsec
							: sampMsec > sinceMsec;
						boolean belowUpperBound = 
							aggUpperBoundClosed ? sampMsec <= untilMsec
							: sampMsec < untilMsec;
						if (aboveLowerBound && belowUpperBound)
							baseTimes.add(sampBaseTime);
					}
				}
				catch(BadTimeSeriesException ex)
				{
					warning("Bad times series for '" + parmRef.role 
						+ "': " + ex.getMessage());
				}
			}
		}
		finally
		{
			timeSeriesDAO.close();
		}
		expandForMissing(baseTimes);
		expandForDeltas(baseTimes);

		return baseTimes;
	}
	


	/**
	 * Cycle through all input data and construct a sorted set of base-times.
	 * The base times are the sample time minus the delta T and according to
	 * the specified rounding.
	 * @return sorted set of base-times
	 */
	protected TreeSet<Date> determineInputBaseTimes()
	{
		TreeSet<Date> baseTimes = new TreeSet<Date>();
		for(String role : getInputNames())
		{
			ParmRef parmRef = parmMap.get(role);
			if (parmRef == null)
				continue;
			int n = parmRef.timeSeries.size();
			for(int i=0; i<n; i++)
			{
				TimedVariable tv = parmRef.timeSeries.sampleAt(i);
				if ((tv.getFlags() & 
					(VarFlags.DB_ADDED | VarFlags.DB_DELETED)) != 0)
				{
					Date baseTime = parmRef.compParm.paramTimeToBaseTime(tv.getTime(), aggCal);
					long sec = baseTime.getTime() / 1000L;
					if (roundSec > 1 && (sec % roundSec) != 0)
					{
						sec = ((sec+roundSec/2) / roundSec) * roundSec;
						baseTime = new Date(sec * 1000L);
					}
					if (baseTimeWithinCompRange(baseTime)) 
						baseTimes.add(baseTime);
				}
			}
		}
		return baseTimes;
	}
	
	
	/**
	 * 
	 * This method checks if the base time falls within the effective start and end dates  & season start and end 
	 * dates specified for the computaion. 
	 * @param baseTime
	 * @return
	 */
	private boolean baseTimeWithinCompRange(Date baseTime)
	{
		if (effectiveStart != null && baseTime.before(effectiveStart))
			return false;
		
		if (effectiveEnd != null && baseTime.after(effectiveEnd))
			return false;

		boolean retBln = true;
		
		if(comp.getProperties().containsKey("seasonName"))
		{
			try {				
				String startSeason = comp.getProperty("seasonStartDate");
				String endSeason = comp.getProperty("seasonEndDate");
				SimpleDateFormat sdformat = new SimpleDateFormat("MMM dd HH:mm:ss");	
				
				Calendar startCal = Calendar.getInstance();
				startCal.setTime(sdformat.parse(startSeason));
				startCal.setTimeZone( TimeZone.getTimeZone(comp.getProperty("seasonTz")));


				Calendar endCal = Calendar.getInstance();
				endCal.setTime(sdformat.parse(endSeason));
				endCal.setTimeZone( TimeZone.getTimeZone(comp.getProperty("seasonTz")));
				startCal.set(Calendar.YEAR, Calendar.getInstance().get(Calendar.YEAR));
				if(startCal.get(Calendar.MONTH)<=endCal.get(Calendar.MONTH))
				{					
					endCal.set(Calendar.YEAR, Calendar.getInstance().get(Calendar.YEAR));
				}
				else
				{					
					endCal.set(Calendar.YEAR, Calendar.getInstance().get(Calendar.YEAR)+1);
				}
				
				
				if(baseTime.compareTo(startCal.getTime())>=0  && baseTime.compareTo(endCal.getTime())<0)
				{
					retBln =true;
				}
				else
					retBln=false;
				
			} catch (ParseException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		    
		    

		}
		
		return retBln;
	}
	
	
	
	private void parseTimeRound()
	{
		roundSec = 60; // default
		String trs = comp.getProperty("TIMEROUND");
		if (trs == null)
			return;
		trs = trs.trim();
		try 
		{
			int msidx = trs.lastIndexOf(':');
			if (msidx == -1) // Just simple number of seconds.
				roundSec = Integer.parseInt(trs);
			else // Either HH:MM:SS or MM:SS
			{
				roundSec = Integer.parseInt(trs.substring(msidx+1));
				int hmidx = trs.indexOf(':');
				if (hmidx == msidx) // Just MM:SS
					roundSec += 
						(Integer.parseInt(trs.substring(0, msidx)) * 60);
				else // HH:MM:SS
				{
					roundSec += 
						(Integer.parseInt(trs.substring(hmidx+1, msidx)) * 60);
					roundSec += 
						(Integer.parseInt(trs.substring(0, hmidx)) * 3600);
				}
			}
		}
		catch(Exception ex)
		{
			warning("Bad time round string '" + trs + "' -- ignored.");
			roundSec = 60;
		}
	}

	private void singleQuery(ParmRef parmRef, Date qt)
		throws DbIoException
	{
		TimeSeriesDAI timeSeriesDAO = tsdb.makeTimeSeriesDAO();
		try
		{
			// MJM 20170525 Need to apply round sec to retrieval.
			Date lower = new Date(qt.getTime() - (roundSec*1000L / 2));
			Date upper = new Date(qt.getTime() + (roundSec*1000L / 2) - 1);
			if (timeSeriesDAO.fillTimeSeries(parmRef.timeSeries, lower, upper) == 0)
				debug1("Cannot retrieve '" + parmRef.role + "' for time "
					+ tsdb.sqlDate(qt) + ": data not int DB.");
		}
		catch(BadTimeSeriesException ex)
		{
			warning("Bad times series for '" + parmRef.role 
				+ "': " + ex.getMessage());
		}
		finally
		{
			timeSeriesDAO.close();
		}
	}

	/**
	 * If a range query is practical, do one & get the results & return true.
	 * Else return false.
	 * @return true if range query was accomplished.
	 */
	private boolean tryRangeQuery(ParmRef parmRef, TreeSet<Date> queryTimes)
		throws DbIoException
	{
		// MJM 20170525 the IN clause won't work because it doesn't apply the 
		// roundSec fudge factor -- It only looks for exact time matches.
		// Therefor this method ALWAYS tries the range, and always returns true.
		
//		String intcode = parmRef.compParm.getInterval();
//		if (IntervalCodes.int_instant.equalsIgnoreCase(intcode)
//		 || IntervalCodes.int_unit.equalsIgnoreCase(intcode))
//			return false;
//
//		// 'Nearly contiguous' means no more that 2 intervals in any gap.
//		int intsec = IntervalCodes.getIntervalSeconds(intcode);
//		long lastSec = 0L;
//		for(Date d : queryTimes)
//		{
//			long sec = d.getTime() / 1000L;
//			if (lastSec != 0 
//			 && sec - lastSec > intsec*2)
//				return false;
//			lastSec = sec;
//		}

		TimeSeriesDAI timeSeriesDAO = tsdb.makeTimeSeriesDAO();
		try
		{
			// MJM 20170525 Need to apply round sec to retrieval.
			Date lower = new Date(queryTimes.first().getTime() - (roundSec*1000L / 2));
			Date upper = new Date(queryTimes.last().getTime() + (roundSec*1000L / 2) - 1);

			int n = timeSeriesDAO.fillTimeSeries(parmRef.timeSeries, lower, upper);
			debug1("Retrieved " + n + " values for role '" + parmRef.role + "' for times "
					+ debugSdf.format(lower) + " thru " + debugSdf.format(upper));
			return true;
		}
		catch(BadTimeSeriesException ex)
		{
			warning("Bad times series for '" + parmRef.role 
				+ "': " + ex.getMessage());
			// Return true because there's no point in processing this TS
			// any further.
			return true;
		}
		finally
		{
			timeSeriesDAO.close();
		}
	}

	private void inClauseQuery(ParmRef parmRef, TreeSet<Date> queryTimes)
		throws DbIoException
	{
		TimeSeriesDAI timeSeriesDAO = tsdb.makeTimeSeriesDAO();
		try
		{
			if (timeSeriesDAO.fillTimeSeries(parmRef.timeSeries, queryTimes) == 0)
				debug1("Cannot retrieve '" + parmRef.role + "' for times "
					+ tsdb.sqlDate(queryTimes.first()) + " thru "
					+ tsdb.sqlDate(queryTimes.last()) + ": data not int DB.");
		}
		catch(BadTimeSeriesException ex)
		{
			warning("Bad times series for '" + parmRef.role 
				+ "': " + ex.getMessage());
		}
		finally
		{
			timeSeriesDAO.close();
		}
	}

	/**
	 * Iterate through the base times in the passed vector.
	 * For each base-time, construct a NamedVariableList containing
	 * the values of all input parameters. (The name will be the
	 * role name assigned by the algorithm). Then call 'doTimeSlice'.
	 * <p>
	 * If a value for an input time slice is missing, there are five
	 * possible ways to handle it:
	 * <ol>
	 *   <li>ignore (default) - Leave data missing in the slice.</li>
	 *   <li>prev - Take the previous value before the time slice.</li>
	 *   <li>next - Take the next value after the time slice.</li>
	 *   <li>interp - Interpolate between prev and next.</li>
	 *   <li>closest - choose prev or next closest in time.</li>
	 * </ol>
	 * You can place the above strings in a property called "MISSING" to
	 * apply to all input parameters. To apply to a specific parameter,
	 * place the above string in a property called "MISSING(rolename)".
	 *
	 * @param baseTimes sorted set of base times through which to iterate.
	 */
	protected void iterateTimeSlices( TreeSet<Date> baseTimes )
	{
		debug2("DbAlgorithmExecutive iterating over " + baseTimes.size()
			+ " time slices.");
		NamedVariableList timeSlice = new NamedVariableList();
	  nextBaseTime:
		for(Date baseTime : baseTimes)
		{
			debug3("DbAlgorithmExecutive starting base time slice " + debugSdf.format(baseTime));
			timeSlice.clear();

			// Place all input params for this baseTime into the time slice.
			for(String role : getInputNames())
			{
				ParmRef parmRef = parmMap.get(role);
				if (parmRef == null)
					continue;

				Date paramTime = parmRef.compParm.baseTimeToParamTime(baseTime, aggCal);
				long varSec = paramTime.getTime()/1000L;
				TimedVariable tv = parmRef.timeSeries.findWithin(paramTime, roundSec/2);
				

				if (tv == null) // Time series missing value for this slice?
				{
debug3("Value missing for '" + role + " at time " + debugSdf.format(paramTime)
+ ", missingAction=" + parmRef.missingAction.toString());
					if (parmRef.missingAction == MissingAction.FAIL)
						// Required param - fail this slice if not present.
//						continue nextBaseTime;
// MJM 20100820 - In order to handle deleted data properly, we just go on
// and process this time-slice. See the code in AW_AlgorithmBase.doTimeSlice
// for handling delete data.
						continue; // next input param.

					// IGNORE means Leave missing & let algorithm handle it.
					if (parmRef.missingAction == MissingAction.IGNORE)
						continue; // next input param.

					TimedVariable prevTv = parmRef.timeSeries.findPrev(varSec);
					if (prevTv == null)
					{
Logger.instance().debug3("... no previous value, skipping.");
						// Can't compute non-ignored param. Skip slice.
						continue nextBaseTime;
					}
					int prevSec = (int)(prevTv.getTime().getTime() / 1000L);
Logger.instance().debug3("... found prev value: " + debugSdf.format(prevTv.getTime()) + " : " + prevTv.getStringValue());

					int intvSecs = IntervalCodes.getIntervalSeconds(
						parmRef.compParm.getInterval());

					if (parmRef.missingAction == MissingAction.PREV)
					{
						if (varSec - prevSec > maxMissingTimeForFill)
						{
							warning("Missing time exceeded for role " + role
								+ ", max=" + maxMissingTimeForFill + " seconds, "
								+ "delta=" + (varSec - prevSec));
							continue nextBaseTime;
						}

						if (intvSecs != 0 
						 && (varSec-prevSec) / intvSecs 
							> maxMissingValuesForFill)
						{
							warning("Missing number exceeded for role " + role
								+ ", max#=" + maxMissingValuesForFill
								+ ", deltaT=" + (varSec-prevSec) + ", intvSecs=" + intvSecs);
							continue nextBaseTime;
						}

						// Else we have a recent-enough prev value - use it.
//						tv = prevTv;
						// MJM 2016-01-26: Mock up a new TV with current time and previous value.
						// This is necessary in case they're doing a delta below
						tv = new TimedVariable(prevTv);
						tv.setTime(paramTime);
						
						debug3("DbAlgorithmExecutive role '" + role + "' missing at base time "
							+ debugSdf.format(baseTime) + ", using prev value=" + tv.getStringValue());
					}
					else // one of NEXT, INTERP, or CLOSEST
					{
						TimedVariable nextTv = 
							parmRef.timeSeries.findNext(varSec);
						if (nextTv == null)
							continue nextBaseTime;

						int nextSec = (int)(nextTv.getTime().getTime() / 1000L);
				
						if (nextSec - prevSec > maxMissingTimeForFill)
						{
							warning("Missing time exceeded for role " + role
								+ ", prevSec=" + prevSec + ", nextSec=" + nextSec
								+ ", max=" + maxMissingTimeForFill + " secconds.");
							continue nextBaseTime;
						}

						if (intvSecs != 0 
						 && (nextSec - prevSec) / intvSecs 
							> maxMissingValuesForFill)
						{
							warning("Missing time exceeded for role " + role
								+ ", prevSec=" + prevSec + ", nextSec=" + nextSec
								+ ", intvSecs=" + intvSecs
								+ ", max=" + maxMissingValuesForFill + " secconds.");

							continue nextBaseTime;
						}

						switch(parmRef.missingAction)
						{
						case NEXT:
							tv = new TimedVariable(nextTv);
							tv.setTime(paramTime);
							break;
						case INTERP:
							tv = parmRef.timeSeries.findInterp(varSec);
							break;
						case CLOSEST:
							tv = parmRef.timeSeries.findClosest(varSec);
							break;
						}
						if (tv == null)
							continue nextBaseTime;
					}
				}
//else debug3("DbAlgorithmExecutive found value (" + debugSdf.format(tv.getTime()) + ":" + tv.getStringValue()
//+ ") paramTime=" + debugSdf.format(paramTime) + ", roundSec=" + roundSec);


				NamedVariable t_nvar = null;
				if (tv != null)
				{
					t_nvar = new NamedVariable(role, tv);
					timeSlice.add(t_nvar);
					//timeSlice.add(new NamedVariable(role, tv));
					// line replaced with variable if we need to remove
					// due to delta record not available 
				}
				else
					continue;

				// Compute automatic deltas.
				String typ = parmRef.compParm.getAlgoParmType().toLowerCase();
				if (typ.length() > 1 && typ.charAt(1) == 'd')
				{
					long qMsec = computeDeltaMsec(tv.getTime().getTime(), typ,
						parmRef.compParm.getInterval(), true);
					TimedVariable prev = parmRef.timeSeries.findWithin(
						(int)(qMsec/1000L), roundSec);
debug3("DbAlgorithmExecutive.iterateTimeSlices: "
+ "prev=" + (prev==null ? "null" : (debugSdf.format(prev.getTime()) + ":" + prev.getStringValue())) + ", " 
+ "this=" + (tv == null ? "null" : (debugSdf.format(tv.getTime()) +":" + tv.getStringValue()))
+ ", qMsec=" + (debugSdf.format(new Date(qMsec))) 
+ ", roundSec=" + roundSec);
					if (prev != null)
					{
						try
						{
							NamedVariable d = new NamedVariable(role + "_d",
								tv.getDoubleValue()-prev.getDoubleValue());
							timeSlice.add(d);
debug3("DbAlgorithmExecutive.iterateTimeSlices: delta computed: " + d);
						}
						catch(NoConversionException ex)
						{
							warning("Error with exact delta: " + ex);
						}
					}
					else if (interpDeltas
					 && (prev = parmRef.timeSeries.findPrev(
						(int)(tv.getTime().getTime()/1000L)-1)) != null)
					{
						try
						{
							Date D_v = tv.getTime();
							double T_v = D_v.getTime();
							Date D_p = prev.getTime(); 
							double T_p = D_p.getTime(); 
							double T_q = qMsec;
							debug1("Interpolating delta D_v=" + D_v
								+ ", D_p=" + D_p 
								+ ", T_q=" + (new Date(qMsec)));

							// Check maxInterpIntervals
							double dT = T_v - T_p;
							if (dT > (T_v-T_q) * maxInterpIntervals)
							{
								warning("Prev value too old, can't interp delta");
								timeSlice.rm(t_nvar);
							}
							else
							{
								double v = tv.getDoubleValue();
								double p = prev.getDoubleValue();
								double q = p + ((T_q-T_p)/(T_v-T_p)) * (v-p);
								timeSlice.add(
									new NamedVariable(role + "_d",
										v-q));
								debug1("Interpolated delta of " + (v-q)
									+ " from time " + new Date(qMsec)
									+ ", v=" + v + ", q=" + q + ", p=" + p);
							} 
						}
						catch(NoConversionException ex)
						{
							warning("Error with interpolated delta: " + ex);
						}
					}
					else if (t_nvar != null)
					{	
					// there was no previous value so delta impossible
					// so remove original timeslice for this variable
						timeSlice.rm(t_nvar);
					}
				}
			}

			// Call the concrete sub-class time-slice method.
			try { doTimeSlice(timeSlice, baseTime); }
			catch(DbCompException ex)
			{
				warning("Error doing time slice for " + baseTime + ": " + ex);
				continue;
			}

			// Retrieve outputs & save back into time series.
			for(String role : getOutputNames())
			{
				ParmRef parmRef = parmMap.get(role);
				if (parmRef == null)
					continue;

				NamedVariable v = timeSlice.findByNameIgnoreCase(role);
				if (v != null)
				{
					Date paramTime = parmRef.compParm.baseTimeToParamTime(baseTime, aggCal);
//debug3("DbAlgorithmExecutive.iterateTimeSlices output baseTime=" + debugSdf.format(baseTime)
//+ ", paramTime=" + debugSdf.format(paramTime));
					// NOTE: It's up to the algorithm to set the TO_WRITE
					// and/or TO_DELETE flags.
					TimedVariable tv = new TimedVariable(v, paramTime);

					// Obscure bug fix starts here ============================
					if (tsdb.isHdb())
					{
						TimedVariable oldTv = parmRef.timeSeries.findWithin(paramTime, 10);
						try
						{
							if (oldTv != null)
							{
								debug2("Attempting to fix altered db_written and to_write value! v:" +
								v.toString() + " tv:" + tv.toString() + " oldTv:"+ oldTv.toString());
							
								// If value is the same, preserve the old flags.
								double diff = v.getDoubleValue() - oldTv.getDoubleValue();
								if (diff >= -1e-7 && diff <= 1e-7) // matches HDB duplicate value detection
								{
									int f = oldTv.getFlags() | VarFlags.TO_WRITE;
									tv.setFlags(f);
								}
							}
						}
						catch(NoConversionException ex)
						{
							debug2("Error comparing existing aggregate output '"
								+ oldTv + ": " + ex);
						}
					}
					// End of Obscure bug fix ============================					
					parmRef.timeSeries.addSample(tv);
				}
			}

			// Some computations modify input values or flags. Do the same for inputs.
			for(String role : getInputNames())
			{
				ParmRef parmRef = parmMap.get(role);
				if (parmRef == null)
					continue;
				NamedVariable v = timeSlice.findByNameIgnoreCase(role);

				// Same as above, but only if var is marked to-write.
				if (v != null && (v.getFlags() & VarFlags.TO_WRITE) != 0)
				{
					Date paramTime = parmRef.compParm.baseTimeToParamTime(baseTime, aggCal);

					TimedVariable tv = new TimedVariable(v, paramTime);
					parmRef.timeSeries.addSample(tv);
				}
			}
		}
	}
	
	/**
	 * Deletes any outputs of this
	 * algorithm for a given base-time stamp. In actuality, it
	 * adds a TO_DELETE value to all output time series with the
	 * given time stamp. Later when results are saved, the values will
	 * then be deleted from the database.
	 *
	 * @param basetime the base time.
	 */
	protected void deleteOutputs( Date basetime )
	{
		for(String role : getOutputNames())
			deleteOutput(role, basetime);
	}

	/**
	 * Deletes a particular output with a given base time stamp.
	 * @param role the role name
	 * @param basetime the base time.
	 */
	protected void deleteOutput(String role, Date basetime)
	{
		ParmRef parmRef = getParmRef(role);
		if (parmRef == null)
			return;
		Date paramTime = parmRef.compParm.baseTimeToParamTime(basetime, aggCal);
		
		long sec = paramTime.getTime() / 1000L;
		TimedVariable tv = parmRef.timeSeries.findWithin(sec, roundSec/2);
		if (tv != null)
		{
			VarFlags.clearToWrite(tv);
			VarFlags.setToDelete(tv);
		}
	}
	
	/**
	 * Algorithm-specific initialization provided by the subclass.
	 */
	protected abstract void initAlgorithm( )
		throws DbCompException;
	
	/**
	 * Concrete apply method to be supplied by subclass.
	 * @throws DbCompException on computation error.
	 */
	protected abstract void applyAlgorithm( )
		throws DbCompException, DbIoException;
	
	/**
	 * Do the algorithm for a single time slice. The default implementation
	 * here does nothing. Non-time-slice algorithms do not need to overload
	 * this method.
	 *
	 * @param timeSlice a set of input variables for a single time-slice
	 *        (the name of each variable will be the algorithm role name).
	 * @param baseTime The base-time of this slice. Any variables having 
	 *        non-zero deltaT may be before or after this time.
	 *
	 * @throw DbCompException (or subclass thereof) if execution of this
	 *        algorithm is to be aborted.
	 */
	protected void doTimeSlice( NamedVariableList timeSlice, Date baseTime)
		throws DbCompException
	{
		// Base class does nothing. Some algorithms may not do time slices
		// and we don't burden them to provide this method.
	}
	
	public void warning(String msg)
	{
		Logger.instance().warning("Comp '" + comp.getName() + "' " + msg);
	}

	public void info(String msg)
	{
		Logger.instance().info("Comp '" + comp.getName() + "' " + msg);
	}

	public void debug1(String msg)
	{
		Logger.instance().debug1("Comp '" + comp.getName() + "' " + msg);
	}
	public void debug2(String msg)
	{
		Logger.instance().debug2("Comp '" + comp.getName() + "' " + msg);
	}
	public void debug3(String msg)
	{
		Logger.instance().debug3("Comp '" + comp.getName() + "' " + msg);
	}

	public ParmRef getParmRef(String role)
	{
		return parmMap.get(role);
	}
	
	/**
	 * Convenience method to get the Time Series Identifier associated with this role.
	 * @param role the name of the parameter (input or output)
	 * @return the Time Series Identifier, or null if undefined.
	 */
	public TimeSeriesIdentifier getParmTsId(String role)
	{
		ParmRef pr = getParmRef(role);
		if (pr == null)
			return null;
		if (pr.tsid != null)
			return pr.tsid;
		if (pr.timeSeries == null)
			return null;
		return pr.timeSeries.getTimeSeriesIdentifier();
	}

	/**
	 * Convenience method to get the unique time series identifier string in the database.
	 * @param role the name of the parameter (input or output)
	 * @return the unique database identifier, or if none, returns "<role>-undefined".
	 */
	public String getParmTsUniqueString(String role)
	{
		TimeSeriesIdentifier tsid = getParmTsId(role);
		if (tsid == null)
			return role + "-undefined";
		return tsid.getUniqueString();
	}
	
	/**
	 * Return true if a time series is assigned to the passed role name. False if not.
	 * @param role the role name in the algorithm
	 */
	public boolean isAssigned(String role)
	{
		return getParmTsId(role) != null;
	}
	
	/**
	 * Returns the interval of the selected parameter for a role
	 * @param rolename the role of interest
	 * @return the string interval name or null if can't determine.
	 */
	protected String getInterval(String rolename)
	{
		ParmRef ref = getParmRef(rolename);
		if (ref == null) 
			warning("No parmRef for '" + rolename + "'");
		else if (ref.compParm == null) 
			warning("No compParm for '" + rolename + "'");

		if (ref != null && ref.compParm != null)
			return ref.compParm.getInterval();
		return null;
	}
	
	/**
	 * Returns the table selector of the selected parameter for a role
	 * @param rolename the role of interest
	 * @return the string tableselector
	 */
	protected String getTableSelector(String rolename)
	{
		ParmRef ref = getParmRef(rolename);
		if (ref == null) 
			warning("No parmRef for '" + rolename + "'");
		else if (ref.compParm == null) 
			warning("No compParm for '" + rolename + "'");

		if (ref != null && ref.compParm != null)
			return ref.compParm.getTableSelector();
		return null;
	}

	/**
	* Given a double, round it to the specified number of places.
	* @param num the number to be rounded
	* @param place how many decimal places to round to
	* @return the rounded double
	*/
	protected double round(double num, int place)
	{
		double factor = Math.pow(10,place);
		double result = Math.round(num*factor)/factor;
		
		return result;
	}

	/**
	 * Returns the site datatype id (SDI) of the param assigned to a role.
	 * Note: For HDB the SDI is not the surrogate key for the time series.
	 * For CWMS, SDI and time series key are the same.
	 * @param rolename the role
	 * @return the numeric site datatype id for a role, or -1 if unassigned.
	 */
	protected DbKey getSDI(String rolename)
	{
		ParmRef ref = getParmRef(rolename);
		if (ref == null)
			warning("No parmRef for '" + rolename + "'");
		else if (ref.compParm == null)
			warning("No compParm for '" + rolename + "'");

		if (ref != null && ref.compParm != null)
			return ref.compParm.getSiteDataTypeId();
		return Constants.undefinedId;
	}

	/**
	 * Given a role name, return the site name of the specified type.
	 * @param rolename the the role name, null will return preferred type.
	 * @return the site name of the preferred type or null if not found.
	 */
	protected String getSiteName(String rolename, String nameType)
	{
		ParmRef ref = getParmRef(rolename);
		if (ref == null)
		{
			warning("No parmRef for '" + rolename + "'");
			return null;
		}
		
		// MJM 20121023 Prefer to get the Site and its names from the TSID
		// object stored in the time series.
		if (ref.timeSeries != null)
		{
			TimeSeriesIdentifier tsid =	ref.timeSeries.getTimeSeriesIdentifier();
			if (tsid != null)
			{
				Site site = tsid.getSite();
				if (site != null)
				{
					SiteName sn = site.getName(nameType);
					if (sn != null)
						return sn.getNameValue();
					else
						warning("Site '" + site.getDisplayName() + "' has no name with type '"
							+ nameType + "'");
				}
				else
					warning("tsid '" + tsid.getUniqueString() + "' has no site object.");
			}
			else
				warning("Time Series for role '" + rolename + "' has no TSID.");
		}
		else
			warning("Role '" + rolename + "' has no associated time series.");
		
		if (ref.compParm == null)
			warning("No compParm for '" + rolename + "'");
		if (ref != null && ref.compParm != null)
		{
			if (nameType != null)
			{
				SiteName sn = ref.compParm.getSiteName(nameType);
				if (sn == null)
				{
					warning("No name of type '" + nameType + "' for role '"
						+rolename+ "' sdi=" + ref.compParm.getSiteDataTypeId());
					return null;
				}
				return sn.getNameValue();
			}
			else // Assume site has at least one name!
				return ref.compParm.getSiteName().getNameValue();
		}
		return null;
	}

	/**
	 * @return the default site name for the specified role name
	 */
	protected String getSiteName(String rolename)
	{
		String s = getSiteName(rolename, 
			DecodesSettings.instance().siteNameTypePreference);
		if (s != null)
			return s;
		return getSiteName(rolename, null);
	}

	/**
	 * Algorithm code can call this method to ensure that the correct
	 * engineering units are associated with an output parameter.
	 */
	protected void setOutputUnitsAbbr(String rolename, String unitsAbbr)
	{
		ParmRef ref = getParmRef(rolename);
		//MJM 20151012 fix to obscure NAE bug. If values are already in the
		//output ts, I must convert them.
		if (ref != null && ref.timeSeries != null)
		{
			// old bad code:
			// ref.timeSeries.setUnitsAbbr(unitsAbbr);
			// Correct code:
			TSUtil.convertUnits(ref.timeSeries, unitsAbbr);
		}
	}

	/**
	 * @return units associated with an input parameter, or "unknown".
	 */
	protected String getInputUnitsAbbr(String rolename)
	{
		ParmRef ref = getParmRef(rolename);
		if (ref != null && ref.timeSeries != null)
			return ref.timeSeries.getUnitsAbbr();
		else 
			return "unknown";
	}
	
	/**
	 * @return units associated with a parameter, or null.
	 */
	protected String getParmUnitsAbbr(String rolename)
	{
		ParmRef ref = getParmRef(rolename);
		if (ref != null && ref.timeSeries != null)
			return ref.timeSeries.getUnitsAbbr();
		else 
			return null;
	}
	
	public DataCollection getDataCollection() { return dc; }
	
	/**
	 * Often, especially when filling an aggregate period, we already
	 * have all of the data we need within a time range. This method 
	 * adjusts the since & until times toward each other if data at the
	 * edges already exists in the time series.
	 * <p>
	 * At return, if since > until, then no retrieval is necessary.
	 * @param ts
	 * @param since
	 * @param until
	 */
//	private void trimRangeForDataAlreadyRetrieved(
//		CTimeSeries ts, Date since, Date until)
//	{
//		String intv = ts.getInterval();
//		if (intv == null)
//			return;
//		IntervalIncrement calincr = IntervalCodes.getIntervalCalIncr(intv);
//		if (calincr == null)
//			return;
//		int fudge = IntervalCodes.getIntervalSeconds(intv) / 2;
//		GregorianCalendar cal = new GregorianCalendar();
//		cal.setTimeZone(aggTZ);
//		cal.setTime(since);
//		boolean alreadyHave = true;
//		
//		// MJM Bug in this method. If we adjust the since/until times we also
//		// need to modify the upper/lower bounds flags.
//		
//		while(alreadyHave && since.compareTo(until) <= 0)
//		{
//			alreadyHave = 
//				ts.findWithin(since.getTime()/1000L, fudge) != null;
//			if (alreadyHave)
//			{
//				cal.add(calincr.getCalConstant(), calincr.getCount());
//				since.setTime(cal.getTimeInMillis());
//			}
//		}
//		alreadyHave = true;
//		cal.setTime(until);
//		while(alreadyHave && since.compareTo(until) < 0)
//		{
//			alreadyHave = 
//				ts.findWithin(until.getTime()/1000L, fudge) != null;
//			if (alreadyHave)
//			{
//				cal.add(calincr.getCalConstant(), -calincr.getCount());
//				until = cal.getTime();
//			}
//		}
//	}
}
