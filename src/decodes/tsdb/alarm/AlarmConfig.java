/*
 * $Id$
 * 
 * Copyright 2017 Cove Software, LLC. All rights reserved.
 * 
 * $Log$
 * Revision 1.2  2017/03/21 12:17:10  mmaloney
 * First working XML and SQL I/O.
 *
 */
package decodes.tsdb.alarm;

import java.util.ArrayList;

/**
 * Holds collection of AlarmGroups read from database.
 */
public class AlarmConfig
{
	private long lastReadMsec = 0L;
	private ArrayList<AlarmGroup> groups = new ArrayList<AlarmGroup>();

	public long getLastReadMsec()
	{
		return lastReadMsec;
	}

	public void setLastReadMsec(long lastReadMsec)
	{
		this.lastReadMsec = lastReadMsec;
	}
	
	public ArrayList<AlarmGroup> getGroups()
	{
		return groups;
	}
	
	

}
