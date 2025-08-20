/**
 * $Id$
 * 
 * Copyright 2015 U.S. Army Corps of Engineers, Hydrologic Engineering Center.
 * 
 * $Log$
 */
package decodes.cwms.validation;

/**
 * Absolute value range check
 */
public class AbsCheck
{
	private char flag;
	private double low;
	private double high;
	
	/**
	 * Constructor
	 * @param flag One of flagQuestion or flagReject (see ValidationConstants)
	 * @param low The low limit or -INFINITY if none defined.
	 * @param high the high limit or +INFINITY if none defined.
	 */
	public AbsCheck(char flag, double low, double high)
	{
		super();
		this.flag = flag;
		this.low = low;
		this.high = high;
	}

	public char getFlag()
	{
		return flag;
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
		return "ABS " + flag + " " + low + " " + high;
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
