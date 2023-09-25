package org.opendcs.odcsapi.beans;

import java.util.Date;

public class ApiTimeSeriesValue
{
	private Date sampleTime = null;
	private double value = 0.0;
	private long flags = 0;
	
	public ApiTimeSeriesValue()
	{
	}
	
	public ApiTimeSeriesValue(Date sampleTime, double value, long flags)
	{
		super();
		this.sampleTime = sampleTime;
		this.value = value;
		this.flags = flags;
	}

	public Date getSampleTime()
	{
		return sampleTime;
	}

	public void setSampleTime(Date sampleTime)
	{
		this.sampleTime = sampleTime;
	}

	public double getValue()
	{
		return value;
	}

	public void setValue(double value)
	{
		this.value = value;
	}

	public long getFlags()
	{
		return flags;
	}

	public void setFlags(long flags)
	{
		this.flags = flags;
	}
}
