/*
 * $Id$
 * 
 * Copyright 2017 Cove Software, LLC. All rights reserved.
 * 
 * $Log$
 * Revision 1.5  2018/03/23 20:12:20  mmaloney
 * Added 'Enabled' flag for process and file monitors.
 *
 * Revision 1.4  2017/05/18 12:28:42  mmaloney
 * Code cleanup. Remove System.out debugs.
 *
 * Revision 1.3  2017/05/17 20:36:25  mmaloney
 * First working version.
 *
 * Revision 1.2  2017/03/21 12:17:10  mmaloney
 * First working XML and SQL I/O.
 *
 */
package decodes.tsdb.alarm;

import ilex.util.Logger;

import java.util.ArrayList;

import decodes.sql.DbKey;
import decodes.tsdb.CompAppInfo;

/**
 * Defines alarms that are detected by matching patterns in events
 * coming from a particular process.
 * @author mmaloney
 *
 */
public class ProcessMonitor
{
	/** The application ID of the process being monitored. */
	private DbKey appId = DbKey.NullKey;
	
	ArrayList<AlarmDefinition> defs = new ArrayList<AlarmDefinition>();
	
	private boolean enabled = true;
	
	/** Reference to the app info matching the appId */
	private transient CompAppInfo appInfo = null;
	
	/** When read from an xml file, appId will be null and process name will be set. */
	private transient String xmlProcName = null;
	
	private transient boolean changed = true;
	private transient String summary = "";


	public ProcessMonitor(DbKey appId)
	{
		super();
		this.appId = appId;
	}
	
	public ProcessMonitor copy()
	{
		ProcessMonitor ret = new ProcessMonitor(this.appId);
		ret.appInfo = this.appInfo;
		for(AlarmDefinition ad : defs)
			ret.getDefs().add(ad.copy());
		return ret;
	}


	public CompAppInfo getAppInfo()
	{
		return appInfo;
	}

	public void setAppInfo(CompAppInfo appInfo)
	{
		this.appInfo = appInfo;
		appId = appInfo.getAppId();
		setChanged(true);
	}

	public DbKey getAppId()
	{
		return appId;
	}

	public ArrayList<AlarmDefinition> getDefs()
	{
		return defs;
	}

	public String getProcName()
	{
		return appInfo != null ? appInfo.getAppName() : xmlProcName;
	}

	public void setXmlProcName(String xmlProcName)
	{
		this.xmlProcName = xmlProcName;
	}

	public String getSummary()
	{
		if (summary == null || summary.length() == 0)
			makeSummary();
		return summary;
	}
	
	public void setChanged(boolean changed)
	{
		this.changed = changed;
		makeSummary();
	}

	private void makeSummary()
	{
		StringBuilder sb = new StringBuilder();
		for(AlarmDefinition def : defs)
		{
			if (sb.length() > 0)
				sb.append(", ");
			String pat = def.getPattern();
			if (pat == null) pat = "<any>";
			sb.append(Logger.priorityName[def.getPriority()] + ":" + pat);
		}
		summary = sb.toString();
	}

	public boolean isEnabled()
	{
		return enabled;
	}

	public void setEnabled(boolean enabled)
	{
		this.enabled = enabled;
		setChanged(true);
	}


}
