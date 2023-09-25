package org.opendcs.odcsapi.beans;

import java.util.ArrayList;

public class ApiTimeSeriesData
{
	private ApiTimeSeriesIdentifier tsid = null;
	
	private ArrayList<ApiTimeSeriesValue> values = new ArrayList<ApiTimeSeriesValue>();

	public ApiTimeSeriesIdentifier getTsid()
	{
		return tsid;
	}

	public void setTsid(ApiTimeSeriesIdentifier tsid)
	{
		this.tsid = tsid;
	}

	public ArrayList<ApiTimeSeriesValue> getValues()
	{
		return values;
	}

	public void setValues(ArrayList<ApiTimeSeriesValue> values)
	{
		this.values = values;
	}

}
