package decodes.cwms.validation;

import ilex.util.Logger;
import ilex.var.NamedVariable;
import ilex.var.NoConversionException;
import ilex.var.TimedVariable;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.TreeSet;

import decodes.cwms.CwmsFlags;
import decodes.tsdb.CTimeSeries;
import decodes.tsdb.DataCollection;
import decodes.tsdb.IntervalCodes;
import decodes.tsdb.IntervalIncrement;
import decodes.tsdb.TimeSeriesIdentifier;
import decodes.tsdb.VarFlags;

/**
 * Holds a collection of checks to perform on one or more
 * time series values, optionally during a specified season.
 */
public class ScreeningCriteria
{
	/** Start of season for this group of checks, or null if all-time */
	Calendar seasonStart = null;
	
	/** Absolute value checks */
	ArrayList<AbsCheck> absChecks = new ArrayList<AbsCheck>();
	
	/** Constant value checks */
	ArrayList<ConstCheck> constChecks = new ArrayList<ConstCheck>();

	/** Rate-of-Change per Hour checks */
	ArrayList<RocPerHourCheck> rocPerHourChecks = new ArrayList<RocPerHourCheck>();
	
	/** Duration Magnitude checks */
	ArrayList<DurCheckPeriod> durCheckPeriods = new ArrayList<DurCheckPeriod>();
	
	/** variable to track validity as tests are executed */
	public final static char ValidityOK = 'O';
	public final static char ValidityQuestion = 'Q';
	public final static char ValidityReject = 'R';
	public final static char ValidityMissing = 'M';
	private char validity = ValidityOK;
	private int testbits = 0;
	
	//TODO: Add Relative Value Checks and Distribution Checks
	
	public ScreeningCriteria(Calendar seasonStart)
	{
		this.seasonStart = seasonStart;
	}
	
	public void addAbsCheck(AbsCheck absCheck)
	{
		absChecks.add(absCheck);
	}
	
	public void addConstCheck(ConstCheck constCheck)
	{
		constChecks.add(constCheck);
	}
	
	public void addRocPerHourCheck(RocPerHourCheck rocPerHourCheck)
	{
		rocPerHourChecks.add(rocPerHourCheck);
	}
	
	public void addDurCheckPeriod(DurCheckPeriod durCheckPeriod)
	{
		durCheckPeriods.add(durCheckPeriod);
	}

	/**
	 * This method does the actual work of the checks. The output value
	 * is written and the appropriate flag bits are set reflecting the
	 * results of the checks.
	 * @param dc The data collection from the computation processor
	 * @param input the input time series
	 * @param output the output time series
	 * @param dataTime the time-stamp of the input value being checked.
	 * @throws NoConversionException 
	 */
	public void executeChecks(DataCollection dc, CTimeSeries input,
		Date dataTime, NamedVariable output, ScreeningAlgorithm alg)
	{
		TimedVariable tv = input.findWithin(dataTime, alg.roundSec);
		if (tv == null)
			return;
		
		// MJM 20121002 Leave protected values completely unchanged. Don't even validate.
		if (alg.inputIsOutput()
		 && (tv.getFlags() & CwmsFlags.PROTECTED) != 0)
		{
			alg.debug1("Skipping PROTECTED value at " + alg.debugSdf.format(dataTime));
			return;
		}
		
		double v = 0.0;
		try { v = tv.getDoubleValue(); }
		catch(NoConversionException ex)
		{
			alg.warning("Crit-1: " + ex.toString());
			return;
		}
		
		TimeSeriesIdentifier tsid = input.getTimeSeriesIdentifier();
		IntervalIncrement tsinc = 
			IntervalCodes.getIntervalCalIncr(tsid.getInterval());

		output.setValue(v);
		int flags = tv.getFlags();
		
		validity = ValidityOK;
		testbits = 0;
		
		alg.debug3("Executing checks, value " + v + " at time " + 
			alg.debugSdf.format(dataTime)
			+ ", flags initially=0x" + Integer.toHexString(flags));

		flags |= CwmsFlags.SCREENED;
		for(AbsCheck chk : absChecks)
		{
			alg.debug1(chk.toString());
			if (compare(v, chk.getLow()) < 0 || compare(v,chk.getHigh()) > 0)
			{
				setValidity(chk.getFlag(), CwmsFlags.TEST_ABSOLUTE_VALUE);
				alg.info(input.getTimeSeriesIdentifier().getUniqueString()
					+ " value " + v + " at time " 
					+ alg.debugSdf.format(dataTime)
					+ " failed " + chk.toString());
			}
		}
		Calendar aggCal = alg.aggCal;
		for(ConstCheck chk : constChecks)
		{
			alg.debug1(chk.toString());
			// Flag if value has not changed more than tolerance
			// over specified duration.
			// Abort check if more than allowedMissing are not present.
			IntervalIncrement durinc = 
				IntervalCodes.getIntervalCalIncr(chk.getDuration());
			if (tsinc.getCount() == 0)
				continue;
			int nvalues = 0;
			double minvalue = Double.POSITIVE_INFINITY;
			double maxvalue = Double.NEGATIVE_INFINITY;
			int nmissing = 0;
			aggCal.setTime(dataTime);
			aggCal.add(durinc.getCalConstant(), -durinc.getCount());
			for(Date d = aggCal.getTime(); 
				!d.after(dataTime); 
				aggCal.add(tsinc.getCalConstant(), tsinc.getCount()), 
				d = aggCal.getTime())
			{
				TimedVariable x = input.findWithin(d, alg.roundSec);
				if (x == null)
				{
					if (++nmissing >= chk.getAllowedMissing())
						break;
					continue;
				}
				double xv = 0;
				try { xv = x.getDoubleValue(); }
				catch(NoConversionException ex)
				{
					alg.warning("Crit-2: " + ex.toString());
					if (++nmissing >= chk.getAllowedMissing())
						break;
					continue;
				}
				nvalues++;
				if (xv < minvalue)
					minvalue = xv;
				if (xv > maxvalue)
					maxvalue = xv;
			}
			if (nmissing < chk.getAllowedMissing())
			{
				double variance = maxvalue - minvalue;
				// MJM is this logic reversed? Shouldn't I check for variance >= tolerance?
				if (compare(variance, chk.getTolerance()) <= 0)
				{
					setValidity(chk.getFlag(), CwmsFlags.TEST_CONSTANT_VALUE);

					alg.info(input.getTimeSeriesIdentifier().getUniqueString()
						+ " value " + v + " at time " + alg.debugSdf.format(dataTime)
						+ " failed " + chk.toString());
				}
			}
		}
		
		TimedVariable prevtv = input.findWithin(
			new Date(dataTime.getTime()-3600000L), alg.roundSec);
		if (prevtv != null 
		 && (prevtv.getFlags() & CwmsFlags.QC_MISSING_OR_REJECTED) == 0)
		{
			for(RocPerHourCheck chk : rocPerHourChecks)
			{
				alg.debug1(chk.toString());
				try
				{
					double delta = v - prevtv.getDoubleValue();
					if (compare(delta, chk.getFall()) < 0 || compare(delta,chk.getRise()) > 0)
					{
						setValidity(chk.getFlag(), CwmsFlags.TEST_RATE_OF_CHANGE);
						alg.info(input.getTimeSeriesIdentifier().getUniqueString()
							+ " value " + v + " at time " + alg.debugSdf.format(dataTime)
							+ " failed " + chk.toString()
							+ " prev=" + prevtv.getDoubleValue() + ", delta=" + delta);
					}
				}
				catch(NoConversionException ex)
				{
					alg.warning("Crit-3: " + ex.toString());
					continue;
				}
			}
		}
		
		// For Duration-Magnitude tests, first figure out what kind of 
		// accumulation. If input duration is not 0 then we have periodic
		// incremental numbers -- just add them. If duration
		// IS 0 then we have a cumulative number and we have to take the delta.
		String tsDur = tsid.getPart("duration");
		IntervalIncrement tsDurCalInc = IntervalCodes.getIntervalCalIncr(tsDur);
		boolean incremental = tsDurCalInc != null && tsDurCalInc.getCount() > 0;
//		alg.debug1("incremental=" + incremental);
		
		for(DurCheckPeriod chk : durCheckPeriods)
		{
			alg.debug1(chk.toString());
			// Accumulate change over duration
			// appropriate for cumulative precip input.
			// Flag if accumulation is too high or low
			IntervalIncrement durinc = 
				IntervalCodes.getIntervalCalIncr(chk.getDuration());
			if (tsinc.getCount() == 0)
				continue;
			int nvalues = 0;
			double prev = Double.NEGATIVE_INFINITY;
			double tally = 0;
			int nmissing = 0;
			aggCal.setTime(dataTime);
			aggCal.add(durinc.getCalConstant(), -durinc.getCount());
			for(Date d = aggCal.getTime(); 
				!d.after(dataTime); 
				aggCal.add(tsinc.getCalConstant(), tsinc.getCount()), 
				d = aggCal.getTime())
			{
				TimedVariable x = input.findWithin(d, alg.roundSec);
				if (x == null)
				{
					++nmissing;
					continue;
				}
				double xv = 0;
				try { xv = x.getDoubleValue(); }
				catch(NoConversionException ex)
				{
					alg.warning("Crit-4: " + ex.toString());
					++nmissing;
					continue;
				}
				nvalues++;
				if (!incremental)
				{
					// Must take deltas and then add them.
					if (prev != Double.NEGATIVE_INFINITY)
					{
						double delta = xv - prev;
						tally += delta;
//						alg.debug2("TV=" + x + ", delta=" + delta
//							+ ", new tally=" + tally);
					}
					prev = xv;
				}
				else // Already have incremental numbers, just add them.
					tally += xv;
			}
			if (compare(tally,chk.getLow()) < 0 || compare(tally,chk.getHigh()) > 0)
			{
				setValidity(chk.getFlag(), CwmsFlags.TEST_DURATION_VALUE);
				alg.info(input.getTimeSeriesIdentifier().getUniqueString()
					+ " value " + v + " at time " + alg.debugSdf.format(dataTime)
					+ " failed " + chk.toString() + ", tally=" + tally
					+ "limits=(" + chk.getLow() + "," + chk.getHigh());
			}
		}
		
		switch(validity)
		{
		case ValidityOK: flags |= CwmsFlags.VALIDITY_OKAY; break;
		case ValidityQuestion: flags |= CwmsFlags.VALIDITY_QUESTIONABLE; break;
		case ValidityReject: flags |= CwmsFlags.VALIDITY_REJECTED; break;
		case ValidityMissing: flags |= CwmsFlags.VALIDITY_MISSING; break;
		}
		flags |= testbits;
		
		alg.debug1("After all checks, value " + v + " at time " + 
			alg.debugSdf.format(dataTime)
			+ ", origFlags = 0x" + Integer.toHexString(tv.getFlags())
			+ ", newFlags = 0x" + Integer.toHexString(flags));
		
		int mask = CwmsFlags.TEST_MASK|CwmsFlags.VALIDITY_MASK;
		// MJM 20121002 If input & output are the same time series
		// and the flags are unchanged. Then do nothing.
		if (alg.inputIsOutput())
		{
			if ((flags&mask) != (tv.getFlags()&mask))
				// Flags are changed as a result of validation.
			{
				alg.clearFlagBits(output, CwmsFlags.VALIDITY_MASK | CwmsFlags.TEST_MASK);
				alg.setFlagBits(output, flags);
			}
		}
		else // output is different from input.
		{
			// The 'setInputFlags' means that, in addition to setting output,
			// set the flag bits on the input param. Thus it is only used when
			// output and input are different time series.
			// Also, it must only be done if the flags have changed to avoid infinite loop.
			if (alg.setInputFlags && (flags&mask) != (tv.getFlags()&mask))
				alg.setInputFlagBits("input", flags, mask);
			
			// Set NO_OVERWRITE if the property is set in the comp.
			if (alg.noOverwrite)
				flags |= VarFlags.NO_OVERWRITE;
			
			// The 'setRejectedMissing' property means that, if the result of the
			// checks is that the value is 'REJECTED', then in the output, set it as MISSING.
			// It is typically used along with setInputFlags. The input value gets the 'REJECTED'
			// flag, where the output gets flagged as MISSING.
			if (alg.setRejectMissing 
			 && (flags & CwmsFlags.VALIDITY_MASK) == CwmsFlags.VALIDITY_REJECTED)
			{
				flags = (flags & (~CwmsFlags.VALIDITY_MASK))
					  | CwmsFlags.VALIDITY_MISSING;
			}
			alg.clearFlagBits(output, mask);
			alg.setFlagBits(output, flags);
		}
	}
	
	/**
	 * Compare value but only to 3 decimal places
	 * @return -1 if value is < limit, 0 if value==limit, 1 if value > limit
	 */
	private int compare(double value, double limit)
	{
		double x = value - limit;
		int sign = (x < 0) ? -1 : 1;
		if (x < 0) x = -x;
		long diff = (long)(x*1000.0 + .5) * sign;
		return diff < 0 ? -1 : diff > 0 ? 1 : 0;
	}
	
	private void setValidity(char testFlag, int testbits)
	{
		switch(testFlag)
		{
		case ValidityQuestion:
			if (validity != ValidityMissing && validity != ValidityReject)
			{
				validity = ValidityQuestion;
				this.testbits |= testbits;
			}
			break;
		case ValidityReject:
			if (validity != ValidityMissing)
			{
				validity = ValidityReject;
				this.testbits |= testbits;
			}
			break;
		case ValidityMissing:
			validity = ValidityMissing;
			this.testbits |= testbits;
			break;
		}
	}
	
	public void fillTimesNeeded(CTimeSeries inTS, TreeSet<Date> needed,
		Calendar aggCal, ScreeningAlgorithm alg)
	{
		TimeSeriesIdentifier tsid = inTS.getTimeSeriesIdentifier();
		IntervalIncrement tsinc = 
			IntervalCodes.getIntervalCalIncr(tsid.getInterval());

		// AbsChecks don't need any historical data

		boolean isIrregular = tsinc == null || tsinc.getCount() == 0;
		if (isIrregular)
			return;
		
		// ConstChecks will need the range from t-duration through t.
		for(ConstCheck cc : constChecks)
		{
			alg.debug3("Adding dates for const check: " + cc.toString());
			addDatesThruDuration(inTS, tsinc, 
				IntervalCodes.getIntervalCalIncr(cc.getDuration()),
				aggCal,needed, alg);
		}

		// ROC checks need the previous hour for each trigger value
		if (rocPerHourChecks.size() > 0)
		{
			alg.debug3("Adding dates for ROC checks");
			for(int idx = 0; idx < inTS.size(); idx++)
			{
				TimedVariable tv = inTS.sampleAt(idx);
				if (!VarFlags.wasAdded(tv))
					continue;
				// Get this sample's time and subtract duration
				Date sampleTime = tv.getTime();
				aggCal.setTime(sampleTime);
				aggCal.add(Calendar.HOUR_OF_DAY, -1);
				Date d = aggCal.getTime();
				needed.add(d);
			}
		}
		
		//DurCheckPeriod also needs the range
		for(DurCheckPeriod cc : durCheckPeriods)
		{
			alg.debug3("Adding dates for dur check: " + cc.toString());
			addDatesThruDuration(inTS, tsinc, 
				IntervalCodes.getIntervalCalIncr(cc.getDuration()),
				aggCal,needed, alg);
		}

		// Finally remove from 'needed' anything we already have
		alg.debug3("Removing needed dates we already have. needed.size=" + needed.size());
		for(Iterator<Date> dit = needed.iterator(); dit.hasNext(); )
		{
			Date d = dit.next();
			if (inTS.findWithin(d, alg.roundSec) != null)
				dit.remove();
		}
		alg.debug1("ScreeningCriteria.fillTimesNeeded done.");
	}

	public Calendar getSeasonStart()
	{
		return seasonStart;
	}
	
	private void addDatesThruDuration(CTimeSeries inTS,
		IntervalIncrement tsinc, IntervalIncrement durinc, Calendar aggCal,
		TreeSet<Date> needed, ScreeningAlgorithm alg)
	{
		int nProcessed = 0;
		int nAdded = 0;
		for(int idx = 0; idx < inTS.size(); idx++)
		{
			TimedVariable tv = inTS.sampleAt(idx);
			if (!VarFlags.wasAdded(tv))
				continue;
			nProcessed++;
			// Get this sample's time and subtract duration
			Date sampleTime = tv.getTime();
//			alg.debug3("addDatesThruDuration: sampleTime=" + alg.debugSdf.format(sampleTime));
			aggCal.setTime(sampleTime);
			aggCal.add(durinc.getCalConstant(), -durinc.getCount());
			Date d = aggCal.getTime();
			// Now iterate forward by interval
			while(d.before(sampleTime))
			{
//				alg.debug3("addDatesThruDuration: checking=" + alg.debugSdf.format(d));
				if (inTS.findWithin(d, 5) == null)
				{
					needed.add(d);
					nAdded++;
				}
				aggCal.add(tsinc.getCalConstant(), tsinc.getCount());
				d = aggCal.getTime();
			}
		}
		alg.debug3("addDatesThruDuration inTS.size="
			+ inTS.size() + ", nProcessed=" + nProcessed
			+ ", nAdded=" + nAdded);
	}
}
