package decodes.routing;

import java.util.Date;

import opendcs.dai.DacqEventDAI;
import opendcs.dao.DatabaseConnectionOwner;
import decodes.db.Database;
import decodes.polling.DacqEvent;
import decodes.sql.DbKey;
import decodes.sql.SqlDatabaseIO;
import decodes.tsdb.DbIoException;
import decodes.tsdb.TsdbAppTemplate;
import ilex.util.Logger;

public class DacqEventLogger 
	extends Logger
{
	private Logger parent = null;
	private DbKey schedEntryStatusId = DbKey.NullKey;
	private DbKey platformId = DbKey.NullKey;
	private String subsystem = null;
	private DbKey appId = DbKey.NullKey;
	private Date msgStart = null;

	public DacqEventLogger(Logger parent)
	{
		super("", parent != null ? parent.getMinLogPriority() : Logger.E_INFORMATION);
		this.parent = parent;
		
		if (TsdbAppTemplate.getAppInstance() != null)
		{
			appId = TsdbAppTemplate.getAppInstance().getAppId();
		}
	}

	@Override
	public void close()
	{
	}

	@Override
	public void doLog(int priority, String text)
	{
		if (parent != null)
			parent.doLog(priority, text);
		if (priority < Logger.E_INFORMATION)
			return;
		DacqEvent evt = new DacqEvent();
		evt.setPlatformId(platformId);
		evt.setSubsystem(subsystem);
		evt.setEventPriority(priority);
		evt.setEventText(text);
		evt.setAppId(appId);
		evt.setMsgRecvTime(msgStart);
		
		writeDacqEvent(evt);
	}
	
	public void writeDacqEvent(DacqEvent evt)
	{
		if (!(Database.getDb().getDbIo() instanceof SqlDatabaseIO))
			return;
		
		try (DacqEventDAI dacqEventDAO = ((SqlDatabaseIO)Database.getDb().getDbIo()).makeDacqEventDAO())
		{
			evt.setScheduleEntryStatusId(schedEntryStatusId);
			dacqEventDAO.writeEvent(evt);
		}
		catch (DbIoException ex)
		{
			parent.debug3("DacqEventLogger cannot write event to database: " 
				+ ex + " -- will disable DB events until next run.");
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

	public void setAppId(DbKey appId)
	{
		this.appId = appId;
	}

	public void setMsgStart(Date timeStamp)
	{
		msgStart = timeStamp;
	}

}
