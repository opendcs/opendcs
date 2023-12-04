package org.opendcs.odcsapi.beans;

public class ApiInterval
{
	private Long intervalId = null;
	private String name = null;
	private String calConstant = null;
	private int calMultilier = 1;
	
	public ApiInterval()
	{
	}
	
	public ApiInterval(Long intervalId, String name, String calConstant, int calMultilier)
	{
		super();
		this.intervalId = intervalId;
		this.name = name;
		this.calConstant = calConstant;
		this.calMultilier = calMultilier;
	}

	public Long getIntervalId()
	{
		return intervalId;
	}
	public void setIntervalId(Long intervalId)
	{
		this.intervalId = intervalId;
	}
	public String getName()
	{
		return name;
	}
	public void setName(String name)
	{
		this.name = name;
	}
	public String getCalConstant()
	{
		return calConstant;
	}
	public void setCalConstant(String calConstant)
	{
		this.calConstant = calConstant;
	}
	public int getCalMultilier()
	{
		return calMultilier;
	}
	public void setCalMultilier(int calMultilier)
	{
		this.calMultilier = calMultilier;
	}
	
}
