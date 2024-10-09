/*
 * $Id$
 * 
 * Copyright 2017 Cove Software, LLC. All rights reserved.
 * 
 * $Log$
 * Revision 1.1  2019/03/05 14:53:00  mmaloney
 * Checked in partial implementation of Alarm classes.
 *
 * Revision 1.4  2017/05/17 20:36:25  mmaloney
 * First working version.
 *
 * Revision 1.3  2017/03/30 20:55:19  mmaloney
 * Alarm and Event monitoring capabilities for 6.4 added.
 *
 * Revision 1.2  2017/03/21 12:17:09  mmaloney
 * First working XML and SQL I/O.
 *
 */
package decodes.tsdb.alarm;

import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import decodes.polling.DacqEvent;
import decodes.sql.DbKey;
import ilex.util.Logger;

/**
 * Holds a pattern to be matched that define alarms coming
 * from a process. If an event from the process matches one of these
 * patterns, the daemon will generate an alarm with the specified 
 * priority.
 * 
 * @author mmaloney
 */
public class AlarmEvent
{
	private DbKey alarmEventId = DbKey.NullKey;
	
	/** Note: -1 means match any priority */
	private int priority = Logger.E_WARNING;
	
	private String pattern = null;
	
	private transient Pattern rxPattern = null;
	
	public AlarmEvent(DbKey alarmEventId)
	{
		this.alarmEventId = alarmEventId;
	}
	
	public AlarmEvent copy()
	{
		AlarmEvent ret = new AlarmEvent(this.alarmEventId);
		ret.priority = this.priority;
		ret.pattern = this.pattern;
		
		return ret;
	}

	public int getPriority()
	{
		return priority;
	}

	public void setPriority(int priority)
	{
		this.priority = priority;
	}

	public String getPattern()
	{
		return pattern;
	}

	public DbKey getAlarmEventId()
	{
		return alarmEventId;
	}

	public void setAlarmEventId(DbKey alarmEventId)
	{
		this.alarmEventId = alarmEventId;
	}

	/**
	 * Return true if the event matches the priority and one of the patterns in this
	 * definition. Special case is if there are no patterns, then any
	 * event with matching priority matches.
	 * @param evtText
	 * @return true if matches one of the patterns, false if not.
	 */
	public boolean matches(DacqEvent evt)
	{
Logger.instance().debug2("AlarmEvent.matches() myPriority=" + priority + ", evt priority=" 
+ evt.getEventPriority() + ", pattern='" + pattern + "'");

		if (priority != evt.getEventPriority())
			return false;
		
		// If no pattern provided, than any evt with matching priority is good.
		if (pattern == null || pattern.trim().length() == 0)
			return true;
		
		// If first time, compile the regular expressions.
		if (rxPattern == null)
		{
			try { rxPattern = Pattern.compile(pattern); }
			catch(PatternSyntaxException ex)
			{
				Logger.instance().warning("Invalid regular expression '"
					+ pattern + "': " + ex + " -- this alarm event definition is invalid.");
				rxPattern = null;
				return false;
			}
		}
		if (rxPattern.matcher(evt.getEventText()).find())
			return true;
else Logger.instance().debug2("-- No match for pattern '" + pattern + "'");

		return false;
	}

	public void setPattern(String pattern)
	{
		this.pattern = pattern;
	}

}
