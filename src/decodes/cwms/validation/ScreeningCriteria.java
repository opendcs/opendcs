/**
 * $Id$
 * 
 * Copyright 2015 U.S. Army Corps of Engineers, Hydrologic Engineering Center.
 * 
 * $Log$
 * Revision 1.7  2015/11/12 15:17:13  mmaloney
 * Added HEC headers.
 *
 */
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
import decodes.db.UnitConverter;
import decodes.tsdb.CTimeSeries;
import decodes.tsdb.DataCollection;
import decodes.tsdb.IntervalCodes;
import decodes.tsdb.IntervalIncrement;
import decodes.tsdb.TimeSeriesIdentifier;
import decodes.tsdb.VarFlags;
import decodes.tsdb.algo.AW_AlgorithmBase;

/**
 * Holds a collection of checks to perform on one or more
 * time series values, optionally during a specified season.
 */
public class ScreeningCriteria
{
	public static final String module = "ScreeningCriteria";
	
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
	
	private String estimateExpression = null;
	
	/** The owner of this criteria set */
	private Screening screening;
	
	public ScreeningCriteria(Calendar seasonStart)
	{
		this.seasonStart = seasonStart;
	}
	
	public ScreeningCriteria()
	{
		setSeasonStart(Calendar.JANUARY, 1);
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
		
		boolean setInputFlags = alg.setInputFlags;
		boolean noOverwrite = alg.noOverwrite;
		boolean noOutputOnReject = alg.noOutputOnReject;
		boolean setRejectMissing = alg.setRejectMissing;
		
		// MJM 20121002 Leave protected values completely unchanged. Don't even validate.
		if (alg.inputIsOutput() && (tv.getFlags() & CwmsFlags.PROTECTED) != 0)
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
		
//		//vvvvvvvvvvvvvvvvvvvvv snip
//		TimeSeriesIdentifier inputTsid = input.getTimeSeriesIdentifier();
//		IntervalIncrement tsinc = IntervalCodes.getIntervalCalIncr(inputTsid.getInterval());
//		boolean inputIrregular = tsinc == null || tsinc.getCount() == 0;
//		//^^^^^^^^^^^^^^^^^^^^^^ snip

		output.setValue(v);
		int flags = tv.getFlags();
		
//		//vvvvvvvvvvvvvvv snip
//		validity = ValidityOK;
//		testbits = 0;
//		//^^^^^^^^^^^^^^^ snip
		
		alg.debug3("Executing checks, value " + v + " at time " + 
			alg.debugSdf.format(dataTime)
			+ ", flags initially=0x" + Integer.toHexString(flags));

		flags |= doChecks(dc, input, dataTime, alg, v);
		
//		//=========================================
//		// ABS checks
//		flags |= CwmsFlags.SCREENED;
//		for(AbsCheck chk : absChecks)
//		{
//			alg.debug1(chk.toString());
//			if (compare(v, chk.getLow()) < 0 || compare(v,chk.getHigh()) > 0)
//			{
//				setValidity(chk.getFlag(), CwmsFlags.TEST_ABSOLUTE_VALUE);
//				alg.info(input.getTimeSeriesIdentifier().getUniqueString()
//					+ " value " + v + " at time " 
//					+ alg.debugSdf.format(dataTime)
//					+ " failed " + chk.toString());
//			}
//		}
//		
//		// CONST checks
//		Calendar aggCal = alg.aggCal;
//	nextConstCheck:
//		for(ConstCheck chk : constChecks)
//		{
//			alg.debug1(chk.toString());
//			
//			// Flag if value has not changed more than tolerance
//			// over specified duration.
//			// Abort check if more than allowedMissing are not present.
//			IntervalIncrement durinc = IntervalCodes.getIntervalCalIncr(chk.getDuration());
//			
//			double minvalue = Double.POSITIVE_INFINITY;
//			double maxvalue = Double.NEGATIVE_INFINITY;
//			aggCal.setTime(dataTime);
//			aggCal.add(durinc.getCalConstant(), -durinc.getCount());
//			Date startTime = aggCal.getTime();
//
//			// Compute the maximum allowable time gap in seconds.
//			// For regular inputs, this is expressed as nmiss * the interval of the input
//			IntervalIncrement maxGap = null;
//			if (!inputIrregular)
//			{
//				if (chk.getAllowedMissing() > 0)
//					maxGap = new IntervalIncrement(tsinc.getCalConstant(), 
//						tsinc.getCount() * chk.getAllowedMissing());
//				
//			}
//			else // maxGap can be specified in the DATCHK files
//				maxGap = chk.getMaxGap();
//			
//			// find the earliest index in the input TS >= aggCal. Then increment through CTS
//			// until !d.after(dataTime)
//			if (input.size() < 1)
//				continue nextConstCheck;
//			alg.debug3("Iterating for CONST check, maxGap=" + maxGap);
//			int idx = 0;
//			Date lastTime = null, firstTime = null;
//			for(; idx < input.size() && input.sampleAt(idx).getTime().before(startTime); idx++);
//			
//			// For irregular, we may need to backup to first sample before the start of period.
//			if (input.sampleAt(idx).getTime().after(startTime) && idx > 0)
//				idx--;
//			for(; idx < input.size() && !input.sampleAt(idx).getTime().after(dataTime); idx++)
//			{
//				TimedVariable x = input.sampleAt(idx);
//				
//				double xv = 0.0;
//				try { xv = x.getDoubleValue(); }
//				catch(NoConversionException ex)
//				{
//					continue; // treat as missing.
//				}
//				
//				if (firstTime == null)
//					firstTime = x.getTime();
////alg.debug3("    CONST value=" + xv + " at time " + alg.debugSdf.format(x.getTime()));
//
//				if (maxGap != null && lastTime != null)
//				{
//					aggCal.setTime(lastTime);
//					aggCal.add(maxGap.getCalConstant(), maxGap.getCount());
//					if (aggCal.getTime().before(x.getTime()))
//					{
//						alg.debug1("Value skipped because of time gap: "
//							+ alg.debugSdf.format(lastTime) + " - " + alg.debugSdf.format(x.getTime()));
//						lastTime = x.getTime();
//						continue nextConstCheck;
//					}
//				}
//				
//				if (xv < minvalue)
//					minvalue = xv;
//				if (xv > maxvalue)
//					maxvalue = xv;
//				lastTime = x.getTime();
//			}
//			
//			// If variance is less than the tolerance, flag the value.
//			double variance = maxvalue - minvalue;
////alg.debug3("  Iteration done, max=" + maxvalue + ", min=" + minvalue + ", variance=" + variance);
//			if (compare(variance, chk.getTolerance()) <= 0)
//			{
//				// Make sure that specified duration was exceeded.
//				aggCal.setTime(firstTime);
//				aggCal.add(durinc.getCalConstant(), durinc.getCount());
//				if (!lastTime.before(aggCal.getTime()))
//				{
//					setValidity(chk.getFlag(), CwmsFlags.TEST_CONSTANT_VALUE);
//					alg.info(input.getTimeSeriesIdentifier().getUniqueString()
//						+ " value " + v + " at time " + alg.debugSdf.format(dataTime)
//						+ " failed " + chk.toString()
//						+ " max=" + maxvalue + ", min=" + minvalue);
//				}
////else alg.debug3("No flag set because duration was not seen.");
//			}
////else alg.debug3("No flag set because variance exceeded.");
//		}
//
//
//		// RATE checks
//		TimedVariable prevtv = input.findWithin(
//			new Date(dataTime.getTime()-3600000L), alg.roundSec);
//		if (prevtv != null 
//		 && (prevtv.getFlags() & CwmsFlags.QC_MISSING_OR_REJECTED) == 0)
//		{
//			for(RocPerHourCheck chk : rocPerHourChecks)
//			{
//				alg.debug1(chk.toString());
//				try
//				{
//					double delta = v - prevtv.getDoubleValue();
//					if (compare(delta, chk.getFall()) < 0 || compare(delta,chk.getRise()) > 0)
//					{
//						setValidity(chk.getFlag(), CwmsFlags.TEST_RATE_OF_CHANGE);
//						alg.info(input.getTimeSeriesIdentifier().getUniqueString()
//							+ " value " + v + " at time " + alg.debugSdf.format(dataTime)
//							+ " failed " + chk.toString()
//							+ " prev=" + prevtv.getDoubleValue() + ", delta=" + delta);
//					}
//				}
//				catch(NoConversionException ex)
//				{
//					alg.warning("Crit-3: " + ex.toString());
//					continue;
//				}
//			}
//		}
//		
//		// DUR checks
//		// For Duration-Magnitude tests, first figure out what kind of 
//		// accumulation. If input duration is not 0 then we have periodic
//		// incremental numbers -- just add them. If duration
//		// IS 0 then we have a cumulative number and we have to take the delta.
//		String tsDur = inputTsid.getPart("duration");
//		IntervalIncrement tsDurCalInc = IntervalCodes.getIntervalCalIncr(tsDur);
//		boolean incremental = tsDurCalInc != null && tsDurCalInc.getCount() > 0;
//		
//		for(DurCheckPeriod chk : durCheckPeriods)
//		{
//			alg.debug1(chk.toString() + ", incremental=" + incremental);
//			
//			// Accumulate change over duration specified in the DATCHK file.
//			IntervalIncrement durinc = IntervalCodes.getIntervalCalIncr(chk.getDuration());
//			if (durinc.getCount() == 0)
//				continue;
////alg.debug3("   DUR period=" + tsinc);
//			
//			double prev = Double.NEGATIVE_INFINITY;
//			double tally = 0;
//			aggCal.setTime(dataTime);
//			aggCal.add(durinc.getCalConstant(), -durinc.getCount());
//			Date startTime = aggCal.getTime();
//			
//			// find the earliest index in the input TS >= aggCal. Then increment through CTS
//			// until !d.after(dataTime)
//			int idx = 0;
//			for(; idx < input.size() && input.sampleAt(idx).getTime().before(startTime); idx++);
//			if (!incremental && input.sampleAt(idx).getTime().after(startTime) && idx > 0)
//				idx--;
//			for(; idx < input.size() && !input.sampleAt(idx).getTime().after(dataTime); idx++)
//			{
//				TimedVariable x = input.sampleAt(idx);
//				double xv = 0;
//				try { xv = x.getDoubleValue(); }
//				catch(NoConversionException ex)
//				{
//					alg.warning("Crit-4: " + ex.toString());
//					continue;
//				}
////alg.debug3("   DUR sample[" + idx + "] time=" + alg.debugSdf.format(x.getTime()) + ", value=" + xv);
//				if (!incremental)
//				{
//					// Must take deltas of cumulative values and then add them.
//					if (prev != Double.NEGATIVE_INFINITY)
//					{
//						double delta = xv - prev;
//						tally += delta;
//					}
//					prev = xv;
//				}
//				else // Already have incremental numbers, just add them.
//					tally += xv;
////alg.debug3("    Tally is now " + tally);
//			}
////alg.debug3("  Looping done, tally=" + tally + ", low=" + chk.getLow() + ", high=" + chk.getHigh());
//			if (compare(tally,chk.getLow()) < 0 || compare(tally,chk.getHigh()) > 0)
//			{
//				setValidity(chk.getFlag(), CwmsFlags.TEST_DURATION_VALUE);
//				alg.info(input.getTimeSeriesIdentifier().getUniqueString()
//					+ " value " + v + " at time " + alg.debugSdf.format(dataTime)
//					+ " failed " + chk.toString() + ", tally=" + tally
//					+ "limits=(" + chk.getLow() + "," + chk.getHigh());
//			}
////else alg.debug3("  Not out of limits.");
//		}
//		
//		switch(validity)
//		{
//		case ValidityOK: flags |= CwmsFlags.VALIDITY_OKAY; break;
//		case ValidityQuestion: flags |= CwmsFlags.VALIDITY_QUESTIONABLE; break;
//		case ValidityReject: flags |= CwmsFlags.VALIDITY_REJECTED; break;
//		case ValidityMissing: flags |= CwmsFlags.VALIDITY_MISSING; break;
//		}
//		flags |= testbits;
//		
//		alg.debug1("After all checks, value " + v + " at time " + 
//			alg.debugSdf.format(dataTime)
//			+ ", origFlags = 0x" + Integer.toHexString(tv.getFlags())
//			+ ", newFlags = 0x" + Integer.toHexString(flags));
//		
//		
//		//=====================================================
		
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
			if (setInputFlags && (flags&mask) != (tv.getFlags()&mask))
				alg.setInputFlagBits("input", flags, mask);
			
			// Set NO_OVERWRITE if the property is set in the comp.
			if (noOverwrite)
				flags |= VarFlags.NO_OVERWRITE;
			
			// The 'setRejectedMissing' property means that, if the result of the
			// checks is that the value is 'REJECTED', then in the output, set it as MISSING.
			// It is typically used along with setInputFlags. The input value gets the 'REJECTED'
			// flag, where the output gets flagged as MISSING.
			if ((flags & CwmsFlags.VALIDITY_MASK) == CwmsFlags.VALIDITY_REJECTED)
			{
				if (noOutputOnReject)
				{
					return; // Simply don't write output if rejected.
				}
				else if (setRejectMissing)
				{
					flags = (flags & (~CwmsFlags.VALIDITY_MASK))
						  | CwmsFlags.VALIDITY_MISSING;
				}
			}
			alg.clearFlagBits(output, mask);
			alg.setFlagBits(output, flags);
		}
	}
	
	/**
	 * Perform the checks and return the flag results
	 * @param dc the data collection
	 * @param input the input time series
	 * @param dataTime the time of the value to check
	 * @param alg The algorithm executive (for log messages with context)
	 * @return the flag results
	 */
	public int doChecks(DataCollection dc, CTimeSeries input,
		Date dataTime, AW_AlgorithmBase alg, double value)
	{
//		TimedVariable tv = input.findWithin(dataTime, alg.roundSec);
//		if (tv == null)
//			throw new NoSuchObjectException(module + ".doChecks - no value at time "
//				+ alg.debugSdf.format(dataTime));
//		double value = 0.0;
//		try { value = tv.getDoubleValue(); }
//		catch(NoConversionException ex)
//		{
//			throw new NoSuchObjectException(module + ".doChecks - Value at time "
//				+ alg.debugSdf.format(dataTime) + " is not a number: " + tv.toString());
//		}
		
		TimeSeriesIdentifier inputTsid = input.getTimeSeriesIdentifier();
		IntervalIncrement tsinc = IntervalCodes.getIntervalCalIncr(inputTsid.getInterval());
		boolean inputIrregular = tsinc == null || tsinc.getCount() == 0;

		int resultFlags = CwmsFlags.SCREENED;
		validity = ValidityOK;
		testbits = 0;

		// ABS checks
		if (screening == null || screening.isRangeActive())
			for(AbsCheck chk : absChecks)
			{
				alg.debug1(chk.toString());
				if (compare(value, chk.getLow()) < 0 || compare(value,chk.getHigh()) > 0)
				{
					setValidity(chk.getFlag(), CwmsFlags.TEST_ABSOLUTE_VALUE);
					alg.info(input.getTimeSeriesIdentifier().getUniqueString()
						+ " value " + value + " at time " 
						+ alg.debugSdf.format(dataTime)
						+ " failed " + chk.toString());
				}
			}
		
		// CONST checks
		Calendar aggCal = alg.aggCal;
		if (screening == null || screening.isConstActive())
		{
		nextConstCheck:
			for(ConstCheck chk : constChecks)
			{
				alg.debug1(chk.toString());
				
				// Flag if value has not changed more than tolerance
				// over specified duration.
				// Abort check if more than allowedMissing are not present.
				IntervalIncrement durinc = IntervalCodes.getIntervalCalIncr(chk.getDuration());
				
				double minvalue = Double.POSITIVE_INFINITY;
				double maxvalue = Double.NEGATIVE_INFINITY;
				aggCal.setTime(dataTime);
				aggCal.add(durinc.getCalConstant(), -durinc.getCount());
				Date startTime = aggCal.getTime();
	
				// Compute the maximum allowable time gap in seconds.
				// For regular inputs, this is expressed as nmiss * the interval of the input
				IntervalIncrement maxGap = null;
				if (!inputIrregular)
				{
					if (chk.getAllowedMissing() > 0)
						maxGap = new IntervalIncrement(tsinc.getCalConstant(), 
							tsinc.getCount() * chk.getAllowedMissing());
					
				}
				else // maxGap can be specified in the DATCHK files
					maxGap = chk.getMaxGap();
				
				// find the earliest index in the input TS >= aggCal. Then increment through CTS
				// until !d.after(dataTime)
				if (input.size() < 1)
					continue nextConstCheck;
				alg.debug3("Iterating for CONST check, maxGap=" + maxGap);
				int idx = 0;
				Date lastTime = null, firstTime = null;
				for(; idx < input.size() && input.sampleAt(idx).getTime().before(startTime); idx++);
				
				// For irregular, we may need to backup to first sample before the start of period.
				if (input.sampleAt(idx).getTime().after(startTime) && idx > 0)
					idx--;
				for(; idx < input.size() && !input.sampleAt(idx).getTime().after(dataTime); idx++)
				{
					TimedVariable x = input.sampleAt(idx);
					
					double xv = 0.0;
					try { xv = x.getDoubleValue(); }
					catch(NoConversionException ex)
					{
						continue; // treat as missing.
					}
					
					if (firstTime == null)
						firstTime = x.getTime();
	//alg.debug3("    CONST value=" + xv + " at time " + alg.debugSdf.format(x.getTime()));
	
					if (maxGap != null && lastTime != null)
					{
						aggCal.setTime(lastTime);
						aggCal.add(maxGap.getCalConstant(), maxGap.getCount());
						if (aggCal.getTime().before(x.getTime()))
						{
							alg.debug1("Value skipped because of time gap: "
								+ alg.debugSdf.format(lastTime) + " - " + alg.debugSdf.format(x.getTime()));
							lastTime = x.getTime();
							continue nextConstCheck;
						}
					}
					
					if (xv < minvalue)
						minvalue = xv;
					if (xv > maxvalue)
						maxvalue = xv;
					lastTime = x.getTime();
				}
				
				// If variance is less than the tolerance, flag the value.
				double variance = maxvalue - minvalue;
	//alg.debug3("  Iteration done, max=" + maxvalue + ", min=" + minvalue + ", variance=" + variance);
				if (compare(variance, chk.getTolerance()) <= 0)
				{
					// Make sure that specified duration was exceeded.
					aggCal.setTime(firstTime);
					aggCal.add(durinc.getCalConstant(), durinc.getCount());
					if (!lastTime.before(aggCal.getTime()))
					{
						setValidity(chk.getFlag(), CwmsFlags.TEST_CONSTANT_VALUE);
						alg.info(input.getTimeSeriesIdentifier().getUniqueString()
							+ " value " + value + " at time " + alg.debugSdf.format(dataTime)
							+ " failed " + chk.toString()
							+ " max=" + maxvalue + ", min=" + minvalue);
					}
	//else alg.debug3("No flag set because duration was not seen.");
				}
	//else alg.debug3("No flag set because variance exceeded.");
			}
		}
		
		if (screening != null && !screening.isRocActive())
		alg.debug1("Skipping ROC checks because ROC is not active.");
		
		if (screening == null || screening.isRocActive())
		{
			// RATE checks
			TimedVariable prevtv = input.findWithin(
				new Date(dataTime.getTime()-3600000L), alg.roundSec);
			if (prevtv != null 
			 && (prevtv.getFlags() & CwmsFlags.FLAG_MISSING_OR_REJECTED) == 0)
			{
				for(RocPerHourCheck chk : rocPerHourChecks)
				{
					alg.debug1(chk.toString());
					try
					{
						double delta = value - prevtv.getDoubleValue();
						if (compare(delta, chk.getFall()) < 0 || compare(delta,chk.getRise()) > 0)
						{
							setValidity(chk.getFlag(), CwmsFlags.TEST_RATE_OF_CHANGE);
							alg.info(input.getTimeSeriesIdentifier().getUniqueString()
								+ " value " + value + " at time " + alg.debugSdf.format(dataTime)
								+ " failed " + chk.toString()
								+ " prev=" + prevtv.getDoubleValue() 
								+ ", prevFlags=0x" + Integer.toHexString(prevtv.getFlags()) + ", delta=" + delta);
						}
					}
					catch(NoConversionException ex)
					{
						alg.warning("Crit-3: " + ex.toString());
						continue;
					}
				}
			}
			else alg.debug1("Not checking dataTime=" + dataTime + " because prev is missing or rejected.");
				
		}
		
		if (screening == null || screening.isDurMagActive())
		{
			// DUR checks
			// For Duration-Magnitude tests, first figure out what kind of 
			// accumulation. If input duration is not 0 then we have periodic
			// incremental numbers -- just add them. If duration
			// IS 0 then we have a cumulative number and we have to take the delta.
			String tsDur = inputTsid.getPart("duration");
			IntervalIncrement tsDurCalInc = IntervalCodes.getIntervalCalIncr(tsDur);
			boolean incremental = tsDurCalInc != null && tsDurCalInc.getCount() > 0;
			
			for(DurCheckPeriod chk : durCheckPeriods)
			{
				alg.debug1(chk.toString() + ", incremental=" + incremental);
				
				// Accumulate change over duration specified in the DATCHK file.
				IntervalIncrement durinc = IntervalCodes.getIntervalCalIncr(chk.getDuration());
				if (durinc.getCount() == 0)
					continue;
	//alg.debug3("   DUR period=" + tsinc);
				
				double prev = Double.NEGATIVE_INFINITY;
				double tally = 0;
				aggCal.setTime(dataTime);
				aggCal.add(durinc.getCalConstant(), -durinc.getCount());
				Date startTime = aggCal.getTime();
				
				// find the earliest index in the input TS >= aggCal. Then increment through CTS
				// until !d.after(dataTime)
				int idx = 0;
				for(; idx < input.size() && input.sampleAt(idx).getTime().before(startTime); idx++);
				if (!incremental && input.sampleAt(idx).getTime().after(startTime) && idx > 0)
					idx--;
				for(; idx < input.size() && !input.sampleAt(idx).getTime().after(dataTime); idx++)
				{
					TimedVariable x = input.sampleAt(idx);
					double xv = 0;
					try { xv = x.getDoubleValue(); }
					catch(NoConversionException ex)
					{
						alg.warning("Crit-4: " + ex.toString());
						continue;
					}
	//alg.debug3("   DUR sample[" + idx + "] time=" + alg.debugSdf.format(x.getTime()) + ", value=" + xv);
					if (!incremental)
					{
						// Must take deltas of cumulative values and then add them.
						if (prev != Double.NEGATIVE_INFINITY)
						{
							double delta = xv - prev;
							tally += delta;
						}
						prev = xv;
					}
					else // Already have incremental numbers, just add them.
						tally += xv;
	//alg.debug3("    Tally is now " + tally);
				}
	//alg.debug3("  Looping done, tally=" + tally + ", low=" + chk.getLow() + ", high=" + chk.getHigh());
				if (compare(tally,chk.getLow()) < 0 || compare(tally,chk.getHigh()) > 0)
				{
					setValidity(chk.getFlag(), CwmsFlags.TEST_DURATION_VALUE);
					alg.info(input.getTimeSeriesIdentifier().getUniqueString()
						+ " value " + value + " at time " + alg.debugSdf.format(dataTime)
						+ " failed " + chk.toString() + ", tally=" + tally
						+ "limits=(" + chk.getLow() + "," + chk.getHigh());
				}
	//else alg.debug3("  Not out of limits.");
			}
		}
		
		switch(validity)
		{
		case ValidityOK: resultFlags |= CwmsFlags.VALIDITY_OKAY; break;
		case ValidityQuestion: resultFlags |= CwmsFlags.VALIDITY_QUESTIONABLE; break;
		case ValidityReject: resultFlags |= CwmsFlags.VALIDITY_REJECTED; break;
		case ValidityMissing: resultFlags |= CwmsFlags.VALIDITY_MISSING; break;
		}
		resultFlags |= testbits;
		
		alg.debug1("After all checks, value " + value + " at time " + 
			alg.debugSdf.format(dataTime)
			+ ", resultFlags = 0x" + Integer.toHexString(resultFlags));
		
		return resultFlags;
	}
	
	/**
	 * Compare value but only to 3 decimal places
	 * @return -1 if value is < limit, 0 if value==limit, 1 if value > limit
	 */
	private int compare(double value, double limit)
	{
		long lval = (long)(value*1000 + .5);
		long llim = (long)(limit*1000 + .5);
		long diff = lval - llim;
		return diff < 0 ? -1 : diff > 0 ? 1 : 0;
//		double x = value - limit;
//		int sign = (x < 0) ? -1 : 1;
//		if (x < 0) x = -x;
//		long diff = (long)(x*1000.0 + .5) * sign;
//		return diff < 0 ? -1 : diff > 0 ? 1 : 0;
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
		Calendar aggCal, AW_AlgorithmBase alg)
	{
		TimeSeriesIdentifier tsid = inTS.getTimeSeriesIdentifier();
		IntervalIncrement tsinc = 
			IntervalCodes.getIntervalCalIncr(tsid.getInterval());
		boolean inputIrregular = tsinc == null || tsinc.getCount() == 0;

		// Note: AbsChecks don't need any historical data
		
		// ConstChecks will need the range from t-duration through t.
		for(ConstCheck cc : constChecks)
		{
			alg.debug3("Adding dates for const check: " + cc.toString());
			addDatesThruDuration(inTS, tsinc, 
				IntervalCodes.getIntervalCalIncr(cc.getDuration()),
				aggCal,needed, alg, inputIrregular);
		}

		// ROC checks need the previous hour for each trigger value
		if (rocPerHourChecks.size() > 0)
		{
			alg.debug3("Adding dates for ROC checks");

			// With the changes to addToNeeded, the following works fine for regular and irregular.
			// Although, 1 hour deltas are an odd thing for irregular.
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
				addToNeeded(d, needed, inputIrregular);
			}
		}
		
		//DurCheckPeriod also needs the range
		for(DurCheckPeriod cc : durCheckPeriods)
		{
			alg.debug3("Adding dates for dur check: " + cc.toString());
			addDatesThruDuration(inTS, tsinc, 
				IntervalCodes.getIntervalCalIncr(cc.getDuration()),
				aggCal,needed, alg, inputIrregular);
		}

		// Finally remove from 'needed' anything we already have
		if (!inputIrregular)
		{
			// Don't do this if inputIrregular -- there should only be two dates and we need them both.
			alg.debug3("Removing needed dates we already have. needed.size=" + needed.size());
			for(Iterator<Date> dit = needed.iterator(); dit.hasNext(); )
			{
				Date d = dit.next();
				if (inTS.findWithin(d, alg.roundSec) != null)
					dit.remove();
			}
		}
		alg.debug1("ScreeningCriteria.fillTimesNeeded done.");
	}

	public Calendar getSeasonStart()
	{
		return seasonStart;
	}
	
	private void addDatesThruDuration(CTimeSeries inTS,
		IntervalIncrement tsinc, IntervalIncrement durinc, Calendar aggCal,
		TreeSet<Date> needed, AW_AlgorithmBase alg, boolean inputIrregular)
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
			aggCal.setTime(sampleTime);
			aggCal.add(durinc.getCalConstant(), -durinc.getCount());
			Date d = aggCal.getTime();
			if (inputIrregular)
			{
				addToNeeded(d, needed, inputIrregular);
				addToNeeded(sampleTime, needed, inputIrregular);
				alg.debug3("addDatesThruDuration (irregular input) first=" + needed.first()
					+ ", last=" + needed.last());
			}
			else // Regular input, iterate forward by interval to get discrete times we need.
			{
				while(d.before(sampleTime))
				{
					if (inTS.findWithin(d, 5) == null)
					{
						addToNeeded(d, needed, inputIrregular);
						nAdded++;
					}
					aggCal.add(tsinc.getCalConstant(), tsinc.getCount());
					d = aggCal.getTime();
				}
				alg.debug3("addDatesThruDuration inTS.size="
					+ inTS.size() + ", nProcessed=" + nProcessed
					+ ", nAdded=" + nAdded);
			}
		}
	}
	
	private void addToNeeded(Date d, TreeSet<Date> needed, boolean inputIrregular)
	{
		if (inputIrregular)
		{
			int sz = needed.size();
			if (sz < 2)
				needed.add(d);
			else // there are already 2 distinct Date objects in the list
			{
				if (d.before(needed.first()))
				{
					needed.remove(needed.first());
					needed.add(d);
				}
				else if (d.after(needed.last()))
				{
					needed.remove(needed.last());
					needed.add(d);
				}
				// else it's in the middle of the range. Do nothing.
			}
		}
		else // We are accumulating a set of distinct dates.
			needed.add(d);
	}

	public ArrayList<DurCheckPeriod> getDurCheckPeriods()
	{
		return durCheckPeriods;
	}
	
	public AbsCheck getAbsCheckFor(char flag)
	{
		for(AbsCheck ac : absChecks)
			if (ac.getFlag() == flag)
				return ac;
		return null;
	}
	
	public RocPerHourCheck getRocCheckFor(char flag)
	{
		for(RocPerHourCheck rocc : rocPerHourChecks)
			if (rocc.getFlag() == flag)
				return rocc;
		return null;
	}
	
	public ConstCheck getConstCheckFor(char flag)
	{
		for(ConstCheck cc : constChecks)
			if (cc.getFlag() == flag)
				return cc;
		return null;
	}

	public String getEstimateExpression()
	{
		return estimateExpression;
	}

	public void setEstimateExpression(String estimateExpression)
	{
		this.estimateExpression = estimateExpression;
	}

	public void setScreening(Screening screening)
	{
		this.screening = screening;
	}

	/**
	 * Set season start.
	 * @param monthConst - One of the MONTH constants defined in Calendar.
	 * @param day - the day of the month (1 = first day of month).
	 */
	public void setSeasonStart(int monthConst, int day)
	{
		if (seasonStart == null)
			seasonStart = Calendar.getInstance();
		seasonStart.set(Calendar.MONTH, monthConst);
		seasonStart.set(Calendar.DAY_OF_MONTH, day);
	}

	/**
	 * Converts units in this criteria object to the specified units using
	 * the passed converter.
	 * @param paramUnits
	 * @param uc
	 */
	public void convertUnits(String paramUnits, UnitConverter uc)
	{
		String what = "";
		try
		{
			for(AbsCheck ac : absChecks)
			{
				what = "abs " + ac.getFlag() + " high";
				if (ac.getHigh() != Double.POSITIVE_INFINITY)
					ac.setHigh(uc.convert(ac.getHigh()));
				what = "abs " + ac.getFlag() + " low";
				if (ac.getLow() != Double.NEGATIVE_INFINITY)
					ac.setLow(uc.convert(ac.getLow()));
			}
			for(RocPerHourCheck rc : rocPerHourChecks)
			{
				what = "roc " + rc.getFlag() + " rise";
				if (rc.getRise() != Double.POSITIVE_INFINITY)
					rc.setRise(uc.convert(rc.getRise()));
				what = "roc " + rc.getFlag() + " fall";
				if (rc.getFall() != Double.NEGATIVE_INFINITY)
					rc.setFall(uc.convert(rc.getFall()));
			}
			for(ConstCheck cc : constChecks)
			{
				what = "const " + cc.getFlag() + " tol";
				cc.setTolerance(uc.convert(cc.getTolerance()));
				what = "const " + cc.getFlag() + " min";
				cc.setMinToCheck(uc.convert(cc.getMinToCheck()));
			}
			for(DurCheckPeriod dcp : durCheckPeriods)
			{
				what = "durmag " + dcp.getFlag() + " high";
				if (dcp.getHigh() != Double.POSITIVE_INFINITY)
					dcp.setHigh(uc.convert(dcp.getHigh()));
				what = "durmag " + dcp.getFlag() + " low";
				if (dcp.getLow() != Double.NEGATIVE_INFINITY)
					dcp.setLow(uc.convert(dcp.getLow()));
			}
		}
		catch(Exception ex)
		{
			Logger.instance().warning("Screening '" + screening.getScreeningName()
				+ "' " + what + ": cannot convert to " + paramUnits + ": " + ex);
		}
	
		
	}

	public void clearChecks()
	{
		absChecks.clear();
		constChecks.clear();
		rocPerHourChecks.clear();
		durCheckPeriods.clear();
	}
}
