package org.opendcs.odcsapi.beans;

public class ApiTokenPosition
{
	private int start=0;
	private int end=0;
	
	public ApiTokenPosition()
	{
	}
	
	public ApiTokenPosition(int start, int end)
	{
		this.start = start;
		this.end = end;
	}

	/**
	 * @return start position of the token
	 */
	public int getStart()
	{
		return start;
	}

	/**
	 * @return character position after the end of the token
	 */
	public int getEnd()
	{
		return end;
	}
	
	public void setStart(int start)
	{
		this.start = start;
	}

	public void setEnd(int end)
	{
		this.end = end;
	}

}
