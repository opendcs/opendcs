package org.opendcs.odcsapi.beans;

public class ApiConfigScriptSensor
{
	private int sensorNumber = 0;
	private ApiUnitConverter unitConverter = null;
	
	public int getSensorNumber()
	{
		return sensorNumber;
	}
	public void setSensorNumber(int sensorNumber)
	{
		this.sensorNumber = sensorNumber;
	}
	public ApiUnitConverter getUnitConverter()
	{
		return unitConverter;
	}
	public void setUnitConverter(ApiUnitConverter unitConverter)
	{
		this.unitConverter = unitConverter;
	}
	
	public String prettyPrint()
	{
		return "sensor[" + sensorNumber + "] unitConv=" + unitConverter.getFromAbbr()
			+ "->" + unitConverter.getToAbbr() + " algo=" + unitConverter.getAlgorithm()
			+ " A=" + unitConverter.getA()
			+ " B=" + unitConverter.getB()
			+ " C=" + unitConverter.getC()
			+ " D=" + unitConverter.getD()
			+ " E=" + unitConverter.getE()
			+ " F=" + unitConverter.getF();
	}

}
