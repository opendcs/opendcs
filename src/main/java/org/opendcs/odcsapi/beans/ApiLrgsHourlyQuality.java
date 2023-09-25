package org.opendcs.odcsapi.beans;

public class ApiLrgsHourlyQuality
{
	private int hour = 0;
	private int numGood = 0;
	private int numBad = 0;
	private int numRecovered = 0;
	public int getHour()
	{
		return hour;
	}
	public void setHour(int hour)
	{
		this.hour = hour;
	}
	public int getNumGood()
	{
		return numGood;
	}
	public void setNumGood(int numGood)
	{
		this.numGood = numGood;
	}
	public int getNumBad()
	{
		return numBad;
	}
	public void setNumBad(int numBad)
	{
		this.numBad = numBad;
	}
	public int getNumRecovered()
	{
		return numRecovered;
	}
	public void setNumRecovered(int numRecovered)
	{
		this.numRecovered = numRecovered;
	}
}
