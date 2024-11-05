package decodes.snotel;

import java.util.Date;

/**
 * This bean holds a single line from a history retrieval request file
 * received for the SNOTEL system.
 * @author mmaloney
 */
public class HistoryRetrieval
{
	private SnotelPlatformSpec spec = null;
	private Date start = null;
	private Date end = null;
	
	public HistoryRetrieval(SnotelPlatformSpec spec, Date start, Date end)
	{
		this.spec = spec;
		this.start = start;
		this.end = end;
	}

	public Date getStart()
	{
		return start;
	}

	public Date getEnd()
	{
		return end;
	}

	public SnotelPlatformSpec getSpec()
	{
		return spec;
	}
	
	public String toString()
	{
		return spec.toString() + " start=" + start + ", end=" + end;
	}
}
