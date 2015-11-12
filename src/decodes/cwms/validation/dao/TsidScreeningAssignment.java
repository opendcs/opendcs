package decodes.cwms.validation.dao;

import decodes.cwms.validation.Screening;
import decodes.tsdb.TimeSeriesIdentifier;

public class TsidScreeningAssignment
{
	private TimeSeriesIdentifier tsid;
	private Screening screening;
	private boolean active;
	
	public TsidScreeningAssignment(TimeSeriesIdentifier tsid, Screening screening, boolean active)
	{
		this.tsid = tsid;
		this.screening = screening;
		this.active = active;
	}
	
	public TimeSeriesIdentifier getTsid()
	{
		return tsid;
	}

	public Screening getScreening()
	{
		return screening;
	}

	public boolean isActive()
	{
		return active;
	}

	public void setTsid(TimeSeriesIdentifier tsid)
	{
		this.tsid = tsid;
	}

	public void setActive(boolean active)
	{
		this.active = active;
	}
}