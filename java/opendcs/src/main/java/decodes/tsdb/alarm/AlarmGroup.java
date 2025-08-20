/*
 * $Id$
 * 
 * Copyright 2017 Cove Software, LLC. All rights reserved.
 * 
 * $Log$
 * Revision 1.1  2019/03/05 14:53:01  mmaloney
 * Checked in partial implementation of Alarm classes.
 *
 * Revision 1.4  2017/05/17 20:36:26  mmaloney
 * First working version.
 *
 * Revision 1.3  2017/03/30 20:55:20  mmaloney
 * Alarm and Event monitoring capabilities for 6.4 added.
 *
 * Revision 1.2  2017/03/21 12:17:10  mmaloney
 * First working XML and SQL I/O.
 *
 */
package decodes.tsdb.alarm;

import java.util.ArrayList;

import decodes.sql.DbKey;

/**
 * Alarms are segregated into named groups. Within each group
 * is a set of alarm definitions and a list of email addresses.
 */
public class AlarmGroup
{
	private DbKey alarmGroupId = DbKey.NullKey;
	private String alarmGroupName = null;
	private long lastModifiedMsec = 0L;
	private ArrayList<EmailAddr> emailAddrs = new ArrayList<EmailAddr>();
	private ArrayList<FileMonitor> fileMonitors = new ArrayList<FileMonitor>();
	private ArrayList<ProcessMonitor> processMonitors = new ArrayList<ProcessMonitor>();
	
	private transient boolean checked = false;
	private transient long lastRunMsec = 0L;
	private transient ArrayList<String> generatedAlarms = new ArrayList<String>();
	
	public AlarmGroup(DbKey alarmGroupId)
	{
		this.alarmGroupId = alarmGroupId;
	}
	
	public AlarmGroup noIdCopy()
	{
		AlarmGroup ret = new AlarmGroup(DbKey.NullKey);
		ret.alarmGroupName = this.alarmGroupName;
		for(EmailAddr ea : this.emailAddrs)
			ret.getEmailAddrs().add(new EmailAddr(ea.getAddr()));
		for(FileMonitor fm : this.fileMonitors)
			ret.getFileMonitors().add(fm.copy());
		for(ProcessMonitor pm : this.processMonitors)
			ret.getProcessMonitors().add(pm.copy());
		
		return ret;
	}

	public DbKey getAlarmGroupId()
	{
		return alarmGroupId;
	}

	public void setAlarmGroupId(DbKey alarmGroupId)
	{
		this.alarmGroupId = alarmGroupId;
	}

	public String getName()
	{
		return alarmGroupName;
	}

	public void setName(String name)
	{
		this.alarmGroupName = name;
	}

	public long getLastModifiedMsec()
	{
		return lastModifiedMsec;
	}

	public void setLastModifiedMsec(long lastModifiedMsec)
	{
		this.lastModifiedMsec = lastModifiedMsec;
	}

	public ArrayList<EmailAddr> getEmailAddrs()
	{
		return emailAddrs;
	}

	public void setEmailAddrs(ArrayList<EmailAddr> emailAddrs)
	{
		this.emailAddrs = emailAddrs;
	}

	public ArrayList<FileMonitor> getFileMonitors()
	{
		return fileMonitors;
	}

	public ArrayList<ProcessMonitor> getProcessMonitors()
	{
		return processMonitors;
	}

	public boolean isChecked()
	{
		return checked;
	}

	public void setChecked(boolean checked)
	{
		this.checked = checked;
	}

	public long getLastRunMsec()
	{
		return lastRunMsec;
	}

	public void setLastRunMsec(long lastRunMsec)
	{
		this.lastRunMsec = lastRunMsec;
	}

	public ArrayList<String> getGeneratedAlarms()
	{
		return generatedAlarms;
	}

}
