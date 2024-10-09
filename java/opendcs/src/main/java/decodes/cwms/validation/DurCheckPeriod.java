/**
 * $Id$
 * 
 * Copyright 2015 U.S. Army Corps of Engineers, Hydrologic Engineering Center.
 * 
 * $Log$
 */
package decodes.cwms.validation;

/**
 * Duration Magnitude Check for a single Period
 * Similar to absolute-value check, but instead of checking the value,
 * it accumulates the value over a period and checks the accumulation.
 * Primarily used for rainfall.
 */
public class DurCheckPeriod
{
	private char flag;
	private String duration;
	private double low;
	private double high;
	
	/**
	 * Constructor
	 * @param flag One of flagQuestion or flagReject (see ValidationConstants)
	 * @param duration duration string over which to check for change
	 * @param low Lower limit of accumulation, flag if less than this amount
	 * @param high Upper limit of accumulation, flag if more than this amount
	 */
	public DurCheckPeriod(char flag, String duration, double low, double high)
	{
		super();
		this.flag = flag;
		this.duration = duration;
		this.low = low;
		this.high = high;
	}

	public char getFlag()
	{
		return flag;
	}

	public String getDuration()
	{
		return duration;
	}

	public double getLow()
	{
		return low;
	}

	public double getHigh()
	{
		return high;
	}

	public String toString()
	{
		return "DUR " + flag + " " + duration + " " + low + " " + high;
	}

	public void setFlag(char flag)
	{
		this.flag = flag;
	}

	public void setDuration(String duration)
	{
		this.duration = duration;
	}

	public void setLow(double low)
	{
		this.low = low;
	}

	public void setHigh(double high)
	{
		this.high = high;
	}
}
