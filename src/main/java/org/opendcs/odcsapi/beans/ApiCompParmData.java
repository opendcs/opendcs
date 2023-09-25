package org.opendcs.odcsapi.beans;

/**
 * This class holds data used as input/output to/from a computation test.
 */
public class ApiCompParmData
{
	/** Algorithm role name. */
	private String algoRoleName = null;

	private ApiTimeSeriesData parmData = new ApiTimeSeriesData();

	public String getAlgoRoleName()
	{
		return algoRoleName;
	}

	public void setAlgoRoleName(String algoRoleName)
	{
		this.algoRoleName = algoRoleName;
	}

	public ApiTimeSeriesData getParmData()
	{
		return parmData;
	}

	public void setParmData(ApiTimeSeriesData parmData)
	{
		this.parmData = parmData;
	}
}
