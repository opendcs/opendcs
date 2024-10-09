package decodes.tsdb.algo;

import decodes.tsdb.CTimeSeries;
import decodes.tsdb.DbCompException;
import decodes.tsdb.DbCompParm;
import decodes.tsdb.DbComputation;
import decodes.tsdb.DbIoException;
import decodes.tsdb.MissingAction;
import decodes.tsdb.TimeSeriesIdentifier;
import decodes.tsdb.TsGroup;
import decodes.util.TSUtil;
import ilex.var.NamedVariable;
import ilex.var.NoConversionException;
import ilex.var.TimedVariable;
import opendcs.dai.ComputationDAI;
import org.opendcs.annotations.algorithm.Algorithm;
import org.opendcs.annotations.algorithm.Input;
import org.opendcs.annotations.algorithm.Output;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;

@Algorithm(description = "Apply a mask to a group to obtain a list of time series.\n" +
		"Retrieve all of the input values and add them together\n" +
		"The MISSING value determines how values are fetched if they are not all present at the triggered time slice.\n" +
		"Can be triggered by all (masked) TSID values in the group or can be run on a timer.")
public class GroupAdder
	extends decodes.tsdb.algo.AW_AlgorithmBase
{
	@Input
	public double mask;

	private ArrayList<CTimeSeries> ts2sum = new ArrayList<CTimeSeries>();

    @Output
	public NamedVariable sum = new NamedVariable("sum", 0);
	@Output
	public NamedVariable count = new NamedVariable("count", 0);
	@Output
	public NamedVariable average = new NamedVariable("average", 0);

	/**
	 * Algorithm-specific initialization provided by the subclass.
	 */
	protected void initAWAlgorithm( )
		throws DbCompException
	{
		_awAlgoType = AWAlgoType.TIME_SLICE;
	}
	
	/**
	 * This method is called once before iterating all time slices.
	 * For GroupAdder, ALL the work is done in this method.
	 */
	protected void beforeTimeSlices()
		throws DbCompException
	{
		// Fetch the TsGroup object and expand it.
		TsGroup group = comp.getGroup();
		if (group == null)
			throw new DbCompException("Cannot execute GroupAdder -- no group assigned.");
		if (!group.getIsExpanded())
		{
			try
			{
				tsdb.expandTsGroup(group);
			}
			catch (DbIoException ex)
			{
				throw new DbCompException("Cannot expand group " + group.getKey() + ": " + group.getGroupName()
					+ ": " + ex);
			}
		}
		ArrayList<TimeSeriesIdentifier> tsidList = group.getExpandedList();
		if (tsidList.isEmpty())
			throw new DbCompException("Group " + group.getKey() + ": " + group.getGroupName() 
				+ " after expansion has no members.");
		
		if (baseTimes.isEmpty())
			throw new DbCompException("GroupAdder called with empty baseTimes array. Nothing to sum.");
		
		// Build a list of CTimeSeries objects to sum.
		// Apply the input param 'mask' to the group to obtain a list of TSIDs to sum.
		// Make CTimeSeries objects for each one and add to the DataCollection.
		// Note: the compParm in the ParmRef will be 'filled in' by the resolver.
		// It will no longer be useful as a mask. I need to re-read the original (unfilled) 
		// computation either from the DAO's cache.
		ts2sum.clear();
		ComputationDAI computationDAO = tsdb.makeComputationDAO();
		MissingAction missingAction = MissingAction.IGNORE;
		String maskUnits = null;
		
		try
		{
			DbComputation origComp = computationDAO.getComputationById(comp.getId());
			DbCompParm maskParm = origComp.getParm("mask");
			missingAction = MissingAction.fromString(origComp.getProperty("mask_MISSING"));
			
			maskUnits = origComp.getProperty("mask_EU");
			
			// Use a set to make sure I don't duplicate any TSIDs after masking.
			HashSet<TimeSeriesIdentifier> transformedTsids = new HashSet<TimeSeriesIdentifier>();
			for(TimeSeriesIdentifier tsid : tsidList)
			{
				try
				{
					TimeSeriesIdentifier transformedTsid = tsdb.transformTsidByCompParm(tsid, maskParm, 
						false,    // No - do not create TSID if it doesn't exist
						false,    // No - do not modify the maskParm DbCompParm object
						"masked tsid");
					if (transformedTsid == null)
						continue;
					if (transformedTsids.contains(transformedTsid))
						continue;
					transformedTsids.add(transformedTsid);
					CTimeSeries cts = dc.getTimeSeriesByTsidKey(transformedTsid);
					if (cts == null)
						cts = tsdb.makeTimeSeries(transformedTsid);
					TSUtil.convertUnits(cts, maskUnits);
					ts2sum.add(cts);
				}
				catch (Exception ex)
				{
					warning("Masking group tsid '" + tsid.getUniqueString() + "' with mask failed: " + ex);
					continue;
				}
			}
		}
		catch (Exception ex)
		{
			throw new DbCompException("Error masking expanded group by mask parameter: " + ex);
		}
		finally
		{
			computationDAO.close();
		}
		if (ts2sum.isEmpty())
			throw new DbCompException("After applying mask, there are no time series to sum.");
		
		// Fill in the CTimeSeries objects with all the potential data I'll need.
//debug3("Prior to fill ...");
		for(CTimeSeries cts : ts2sum)
		{
//debug3("    tsid: " + cts.getTimeSeriesIdentifier().getUniqueString() + ", units=" + cts.getUnitsAbbr());
//for(int idx=0; idx < cts.size(); idx++) debug3("        " + cts.sampleAt(idx));
			try
			{
				tsdb.fillTimeSeries(cts, baseTimes.first(), baseTimes.last(), true, true, false);
				tsdb.getPreviousValue(cts, baseTimes.first());
				tsdb.getNextValue(cts, baseTimes.last());
			}
			catch (Exception ex)
			{
				warning("Error filling time series '" + cts.getTimeSeriesIdentifier().getUniqueString() + "': " + ex);
				continue;
			}
		}
		
debug3("After fill ...");
for(CTimeSeries cts : ts2sum)
{
	debug3("    tsid: " + cts.getTimeSeriesIdentifier().getUniqueString() + ", units=" + cts.getUnitsAbbr()
			+ ", num samples=" + cts.size());
}
		
	  nextTimeSlice:
		for(Date timeSlice : baseTimes)
		{
			double _sum = 0.0;
			int numSummed = 0;
//debug3("base time " + debugSdf.format(timeSlice));
			for (CTimeSeries cts : ts2sum)
			{

				TimedVariable tv = cts.findWithin(timeSlice, roundSec);

				if (tv == null)
				{
					switch (missingAction)
					{
						case CLOSEST:
							tv = cts.findClosest(timeSlice.getTime() / 1000L);
							break;
						case PREV:
							tv = cts.findPrev(timeSlice);
							break;
						case NEXT:
							tv = cts.findNext(timeSlice);
							break;
						case INTERP:
							tv = cts.findInterp(timeSlice.getTime() / 1000L);
							break;
						case FAIL:
							debug1("Skipping time slice " + debugSdf.format(timeSlice)
									+ " because there is no value for '"
									+ cts.getTimeSeriesIdentifier().getUniqueString() + "'");
							continue nextTimeSlice;
						default: // IGNORE
							debug1("Skipping time series '" + cts.getTimeSeriesIdentifier().getUniqueString()
									+ "' because no value at " + debugSdf.format(timeSlice));
							continue;
					}
				}
				if (tv != null)
					try
					{
						_sum += tv.getDoubleValue();
						numSummed++;
//debug3("   after count=" + numSummed + ", sum=" + _sum);
					}
					catch (NoConversionException e)
					{
						warning("Non numeric time series value in '"
							+ cts.getTimeSeriesIdentifier().getUniqueString() + "' -- skipped.");
					}
			}
			if (numSummed > 0)
			{
				debug1("" + numSummed + " values summed from group " + group.getGroupName()
					+ " at time " + debugSdf.format(timeSlice));
				this.setOutput(sum, _sum, timeSlice);
				if (isAssigned("count"))
					setOutput(count, numSummed, timeSlice);
				if (isAssigned("average"))
					setOutput(average, _sum / (double)numSummed, timeSlice);
			}
			else
				warning("No values found for group " + group.getGroupName()
					+ " at time " + debugSdf.format(timeSlice));
		}
		
	}

	/**
	 * Do the algorithm for a single time slice.
	 * AW will fill in user-supplied code here.
	 * Base class will set inputs prior to calling this method.
	 * User code should call one of the setOutput methods for a time-slice
	 * output variable.
	 *
	 * @throws DbCompException (or subclass thereof) if execution of this
	 *        algorithm is to be aborted.
	 */
	protected void doAWTimeSlice()
		throws DbCompException
	{
	}

	/**
	 * This method is called once after iterating all time slices.
	 */
	protected void afterTimeSlices()
		throws DbCompException
	{
	}

}
