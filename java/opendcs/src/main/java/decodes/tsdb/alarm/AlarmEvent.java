/*
 * Copyright 2017 Cove Software, LLC. All rights reserved.
 */
/*
* Where Applicable, Copyright 2025 OpenDCS Consortium and/or its contributors
* 
* Licensed under the Apache License, Version 2.0 (the "License"); you may not
* use this file except in compliance with the License. You may obtain a copy
* of the License at
* 
*   http://www.apache.org/licenses/LICENSE-2.0
* 
* Unless required by applicable law or agreed to in writing, software 
* distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
* WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
* License for the specific language governing permissions and limitations 
* under the License.
*/
package decodes.tsdb.alarm;

import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.opendcs.utils.logging.OpenDcsLoggerFactory;
import org.slf4j.Logger;

import decodes.polling.DacqEvent;
import decodes.sql.DbKey;

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
	private static final Logger log = OpenDcsLoggerFactory.getLogger();
	private DbKey alarmEventId = DbKey.NullKey;
	
	/** Note: -1 means match any priority */
	private int priority = -1;
	
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
	 * @param evt the event
	 * @return true if matches one of the patterns, false if not.
	 */
	public boolean matches(DacqEvent evt)
	{
		log.trace("AlarmEvent.matches() myPriority={}, evt priority={}, pattern='{}'",
				  priority, evt.getEventPriority(), pattern);

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
				log.atWarn()
				   .setCause(ex)
				   .log("Invalid regular expression '{}' -- this alarm event definition is invalid.", pattern);
				rxPattern = null;
				return false;
			}
		}
		if (rxPattern.matcher(evt.getEventText()).find())
			return true;
		else
		{
			log.trace("-- No match for pattern '{}'", pattern);
		}
		return false;
	}

	public void setPattern(String pattern)
	{
		this.pattern = pattern;
	}

}
