package opendcs.opentsdb.hydrojson.beans;

import java.io.Serializable;
import java.util.Date;

public class TimeSeriesValue
	implements Serializable
{
	private static final long serialVersionUID = 7594290440702017418L;
	private String sampleTime = "";
	private double value = 0.0;
	private long flags = 0;
	
	public TimeSeriesValue()
	{
	}
	
	protected TimeSeriesValue(String sampleTime, double value, long flags)
	{
		super();
		this.sampleTime = sampleTime;
		this.value = value;
		this.flags = flags;
	}

	public String getSampleTime()
	{
		return sampleTime;
	}

	public void setSampleTime(String sampleTime)
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
