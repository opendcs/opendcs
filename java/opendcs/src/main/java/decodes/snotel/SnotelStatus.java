package decodes.snotel;

/**
 * Bean class to hold various status variables.
 * When status changes, these are stored in snotel.stat
 * @author mmaloney
 *
 */
public class SnotelStatus
{
	/** Last time each type of file was read from controlM directory*/
	public long historyLMT = 0L;
	public long realtimeLMT = 0L;
	public long configLMT = 0L;
	
	public long lastHistoryRun = 0L;
	public long lastRealtimeRun = 0L;

	public SnotelStatus()
	{
	}

}
