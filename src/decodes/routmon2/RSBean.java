package decodes.routmon2;

import java.util.ArrayList;
import java.util.Date;

import decodes.db.ScheduleEntry;
import decodes.db.ScheduleEntryStatus;

public class RSBean
{
	private ScheduleEntry scheduleEntry = null;
	private boolean checked = true;
	
	// Always kept sorted in reverse time order by run start.
	private ArrayList<ScheduleEntryStatus> runHistory = new ArrayList<ScheduleEntryStatus>();
	
	public RSBean(ScheduleEntry scheduleEntry)
	{
		this.scheduleEntry = scheduleEntry;
	}
	
	public boolean isEnabled()
	{
		return scheduleEntry == null ? false : scheduleEntry.isEnabled();
	}
	
	public String getRsName()
	{
		return scheduleEntry == null ? "" : scheduleEntry.getRoutingSpecName();
	}
	
	public String getAppName()
	{
		return scheduleEntry == null ? "" : scheduleEntry.getLoadingAppName();
	}
	
	public String getInterval()
	{
		return scheduleEntry == null ? "" : scheduleEntry.getRunInterval();
	}

	public Date getLastActivityTime()
	{
		return runHistory.size() == 0 ? null
			: runHistory.get(0).getLastModified();
	}

	public Date getLastMsgTime()
	{
		return runHistory.size() == 0 ? null
			: runHistory.get(0).getLastMessageTime();
	}

	public String getLastStats()
	{
		if (runHistory.size() == 0)
			return "-";
		ScheduleEntryStatus ses = runHistory.get(0);
		return "" + ses.getNumMessages() + "/" + ses.getNumDecodesErrors();
	}

	public ArrayList<ScheduleEntryStatus> getRunHistory()
	{
		return runHistory;
	}

	public boolean isChecked()
	{
		return checked;
	}

	public void setChecked(boolean checked)
	{
		this.checked = checked;
	}

	public ScheduleEntry getScheduleEntry()
	{
		return scheduleEntry;
	}

	public void setScheduleEntry(ScheduleEntry scheduleEntry)
	{
		this.scheduleEntry = scheduleEntry;
	}

	public void setRunHistory(ArrayList<ScheduleEntryStatus> statusList)
	{
		runHistory = statusList;
	}

	

}
