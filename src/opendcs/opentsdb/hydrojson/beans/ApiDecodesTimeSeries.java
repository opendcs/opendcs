package opendcs.opentsdb.hydrojson.beans;

import java.util.ArrayList;

public class ApiDecodesTimeSeries
{
	private int sensorNum = 0;
	private String sensorName = null;
	private String units = null;
	private ArrayList<ApiDecodesTSValue> values = new ArrayList<ApiDecodesTSValue>();
	public int getSensorNum()
	{
		return sensorNum;
	}
	public void setSensorNum(int sensorNum)
	{
		this.sensorNum = sensorNum;
	}
	public ArrayList<ApiDecodesTSValue> getValues()
	{
		return values;
	}
	public void setValues(ArrayList<ApiDecodesTSValue> values)
	{
		this.values = values;
	}
	public String getSensorName()
	{
		return sensorName;
	}
	public void setSensorName(String sensorName)
	{
		this.sensorName = sensorName;
	}
	public String getUnits()
	{
		return units;
	}
	public void setUnits(String units)
	{
		this.units = units;
	}
	

}
