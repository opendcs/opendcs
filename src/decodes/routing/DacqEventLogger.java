package decodes.routing;

import opendcs.dai.DacqEventDAI;
import decodes.polling.DacqEvent;
import decodes.sql.DbKey;
import decodes.tsdb.DbIoException;
import ilex.util.Logger;

public class DacqEventLogger 
	extends Logger
{
	private Logger parent = null;
	private DbKey schedEntryStatusId = DbKey.NullKey;
	private DbKey platformId = DbKey.NullKey;
	private String subsystem = null;
	private DacqEventDAI dacqEventDAO = null;

	public DacqEventLogger(Logger parent)
	{
		super("");
		this.parent = parent;
		if (parent != null)
			this.setMinLogPriority(parent.getMinLogPriority());
	}

	@Override
	public void close()
	{
	}

	@Override
	public void doLog(int priority, String text)
	{
		parent.doLog(priority, text);
		if (priority < Logger.E_INFORMATION || dacqEventDAO == null)
			return;
		DacqEvent evt = new DacqEvent();
		evt.setPlatformId(platformId);
		evt.setSubsystem(subsystem);
		evt.setEventPriority(priority);
		evt.setEventText(text);
		writeDacqEvent(evt);
	}
	
	public void writeDacqEvent(DacqEvent evt)
	{
		if (dacqEventDAO == null)
			return;
		evt.setScheduleEntryStatusId(schedEntryStatusId);
		try
		{
			dacqEventDAO.writeEvent(evt);
		}
		catch (DbIoException ex)
		{
			System.err.println("DacqEventLogger cannot write event to database: " 
				+ ex + " -- will disable DB events until next run.");
			ex.printStackTrace();
			dacqEventDAO.close();
			dacqEventDAO = null;
		}

	}

	public void setSchedEntryStatusId(DbKey schedEntryStatusId)
	{
		this.schedEntryStatusId = schedEntryStatusId;
	}

	public void setPlatformId(DbKey platformId)
	{
		this.platformId = platformId;
	}

	public void setSubsystem(String subsystem)
	{
		this.subsystem = subsystem;
	}

	public void setDacqEventDAO(DacqEventDAI dacqEventDAO)
	{
		this.dacqEventDAO = dacqEventDAO;
	}
	public DacqEventDAI getDacqEventDAO()
	{
		return dacqEventDAO;
	}

}
