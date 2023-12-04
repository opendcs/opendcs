package org.opendcs.odcsapi.beans;

import java.util.ArrayList;
import java.util.Date;

/** This is the data structure passed to POST testComputation */
public class ApiCompTestRequest
{
	/** The comp to be tested. May or may not match one in the database. */
	private ApiComputation computation = null;
	
	/** The triggering input time series to use in executing the computation */
	private ApiTimeSeriesIdentifier tsid = null;
	
	/** Start date for input data fetch. If null, go back to earliest values in DB. */
	private Date since = null;
	
	/** End date for input data fetch, if null, use up to latest values in DB */
	private Date until = null;
	
	/** Set to true to have trace log messages returned with output. */
	private boolean traceOutput = false;

	public ApiComputation getComputation()
	{
		return computation;
	}

	public void setComputation(ApiComputation computation)
	{
		this.computation = computation;
	}

	public Date getSince()
	{
		return since;
	}

	public void setSince(Date since)
	{
		this.since = since;
	}

	public Date getUntil()
	{
		return until;
	}

	public void setUntil(Date until)
	{
		this.until = until;
	}

	public boolean isTraceOutput()
	{
		return traceOutput;
	}

	public void setTraceOutput(boolean traceOutput)
	{
		this.traceOutput = traceOutput;
	}

	public ApiTimeSeriesIdentifier getTsid()
	{
		return tsid;
	}

	public void setTsid(ApiTimeSeriesIdentifier tsid)
	{
		this.tsid = tsid;
	}


}
