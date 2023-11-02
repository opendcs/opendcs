/**
 * $Id$
 * 
 * Copyright 2015 U.S. Army Corps of Engineers, Hydrologic Engineering Center.
 * 
 * $Log$
 */
package decodes.cwms.validation;

import decodes.tsdb.IntervalIncrement;

/**
 * Constant Value (stuck-sensor) Check.
 * Checks to make sure a minimum amount of change occurred in a
 * value over a specified duration.
 */
public class ConstCheck
{
	private char flag;
	private String duration;
	private double minToCheck;
	private double tolerance;
	private IntervalIncrement maxGap = null;
	private int allowedMissing;
	
	/**
	 * Constructor
	 * @param flag One of flagQuestion or flagReject (see ValidationConstants)
	 * @param duration duration string over which to check for change
	 * @param minToCheck don't check any values below this value.
	 * @param tolerance change over duration must be more than this
	 * @param allowedMissing # of values allowed missing in duration (if more
	 *    than this are missing, don't do the check).
	 */
	public ConstCheck(char flag, String duration, double minToCheck,
			double tolerance, int allowedMissing)
	{
		super();
		this.flag = flag;
		this.duration = duration;
		this.minToCheck = minToCheck;
		this.tolerance = tolerance;
		this.allowedMissing = allowedMissing;
	}

	public char getFlag()
	{
		return flag;
	}

	public String getDuration()
	{
		return duration;
	}

	public double getMinToCheck()
	{
		return minToCheck;
	}

	public double getTolerance()
	{
		return tolerance;
	}

	public int getAllowedMissing()
	{
		return allowedMissing;
	}
	//TODO for new impl, #missing may be a float representing a fraction of the duration.

	public String toString()
	{
		return "CONST " + flag + " " + duration + " " + minToCheck + " "
			+ tolerance + " " 
			+ (allowedMissing>0 ? (""+allowedMissing) : 
				maxGap != null ? maxGap.toString() : "");
	}

	public IntervalIncrement getMaxGap()
	{
		return maxGap;
	}

	public void setMaxGap(IntervalIncrement maxGap)
	{
		this.maxGap = maxGap;
	}

	public void setFlag(char flag)
	{
		this.flag = flag;
	}

	public void setDuration(String duration)
	{
		this.duration = duration;
	}

	public void setMinToCheck(double minToCheck)
	{
		this.minToCheck = minToCheck;
	}

	public void setTolerance(double tolerance)
	{
		this.tolerance = tolerance;
	}

	public void setAllowedMissing(int allowedMissing)
	{
		this.allowedMissing = allowedMissing;
	}

}
