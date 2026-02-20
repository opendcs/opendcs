package org.opendcs.odcsapi.beans;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

public final class ApiCompResults
{
	@Schema(description = "List of time series IDs for which results are available.", example = "LOC.Elev.Total.1Day.1Day.CWMS,LOC2.Temp.Avg.1Day.1Day.CWMS")
	private List<ApiTimeSeriesIdentifier> tsIds;

	@Schema(description = "Start time for the results. Uses Instant for representation", example = "2025-01-01T00:00:00Z")
	private String startTime;

	@Schema(description = "End time for the results. Uses Instant for representation", example = "2025-01-01T00:00:00Z")
	private String endTime;

	public List<ApiTimeSeriesIdentifier> getTsIds()
	{
		return tsIds;
	}

	public String getStartTime()
	{
		return startTime;
	}

	public String getEndTime()
	{
		return endTime;
	}

	public void setEndTime(String endTime)
	{
		this.endTime = endTime;
	}

	public void setStartTime(String startTime)
	{
		this.startTime = startTime;
	}

	public void setTsIds(List<ApiTimeSeriesIdentifier> tsIds)
	{
		this.tsIds = tsIds;
	}
}
