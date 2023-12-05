package org.opendcs.odcsapi.beans;

public class ApiPresentationElement
{
	private String dataTypeStd = null;
	private String dataTypeCode = null;
	private String units = null;
	private int fractionalDigits = 2;
	private Double min = null;
	private Double max = null;
	
	public String getDataTypeStd()
	{
		return dataTypeStd;
	}
	public void setDataTypeStd(String dataTypeStd)
	{
		this.dataTypeStd = dataTypeStd;
	}
	public String getDataTypeCode()
	{
		return dataTypeCode;
	}
	public void setDataTypeCode(String dataTypeCode)
	{
		this.dataTypeCode = dataTypeCode;
	}
	public String getUnits()
	{
		return units;
	}
	public void setUnits(String units)
	{
		this.units = units;
	}
	public int getFractionalDigits()
	{
		return fractionalDigits;
	}
	public void setFractionalDigits(int fractionalDigits)
	{
		this.fractionalDigits = fractionalDigits;
	}
	public Double getMin()
	{
		return min;
	}
	public void setMin(Double min)
	{
		this.min = min;
	}
	public Double getMax()
	{
		return max;
	}
	public void setMax(Double max)
	{
		this.max = max;
	}
	
	

}
