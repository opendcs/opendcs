/**
 * $Id$
 * 
 * $Log$
 */
package decodes.tsdb.alarm.xml;

import java.util.ArrayList;

import decodes.tsdb.alarm.AlarmGroup;
import decodes.tsdb.alarm.AlarmScreening;

/**
 * Encpasulates information read from or to write to, an XML Alarm File
 * @author mmaloney
 *
 */
public class AlarmFile
{
	private ArrayList<AlarmScreening> screenings = new ArrayList<AlarmScreening>();
	
	private ArrayList<AlarmGroup> groups = new ArrayList<AlarmGroup>();

	public ArrayList<AlarmScreening> getScreenings()
	{
		return screenings;
	}

	public ArrayList<AlarmGroup> getGroups()
	{
		return groups;
	}

}
