/**
 * $Id$
 * 
 * $Log$
 * Revision 1.5  2013/03/21 18:27:39  mmaloney
 * DbKey Implementation
 *
 * Revision 1.4  2012/07/18 14:25:02  mmaloney
 * First cut of new daemon to update CP_COMP_DEPENDS.
 *
 * Revision 1.3  2012/07/11 18:09:07  mmaloney
 * First cut of new daemon to update CP_COMP_DEPENDS.
 *
 * Revision 1.2  2012/07/05 18:27:04  mmaloney
 * tsKey is stored as a long.
 *
 * Revision 1.1  2012/06/18 15:15:39  mmaloney
 * Moved TS ID cache to base class.
 *

 * This is open-source software written by Cove Software LLC under
 * contract to the federal government. You are free to copy and use this
 * source code for your own purposes, except that no part of the information
 * contained in this file may be claimed to be proprietary.
 *
 * This source code is provided completely without warranty.
 */
package decodes.tsdb;

import java.util.Date;

import decodes.sql.DbKey;

/**
 * This is a bean-class to hold a single record in the CP_DEPENDS_NOTIFY
 * table.
 * 
 * @author Mike Maloney, Cove Software LLC
 */
public class CpDependsNotify
{
	private long recordNum;
	private char eventType;
	private DbKey key;
	private Date dateTimeLoaded;
	
	/** Sent from trigger when a new Time Series is created */
	public static final char TS_CREATED   = 'T';
	/** Sent from trigger when a TS has its TSID Modified */
	public static final char TS_MODIFIED  = 'M';
	/** Sent from trigger when a Time Series is deleted */
	public static final char TS_DELETED   = 'D';
	/** Sent from trigger when a computation is modified */
	public static final char CMP_MODIFIED = 'C';
	/** Sent from trigger when a time-series group is modified */
	public static final char GRP_MODIFIED = 'G';
	/** Sent from external app to cause full re-evaluation of CP_COMP_DEPENDS */
	public static final char FULL_EVAL    = 'F';
	
	public long getRecordNum()
	{
		return recordNum;
	}
	public void setRecordNum(long recordNum)
	{
		this.recordNum = recordNum;
	}
	public char getEventType()
	{
		return eventType;
	}
	public void setEventType(char eventType)
	{
		this.eventType = eventType;
	}
	public DbKey getKey()
	{
		return key;
	}
	public void setKey(DbKey key)
	{
		this.key = key;
	}
	public Date getDateTimeLoaded()
	{
		return dateTimeLoaded;
	}
	public void setDateTimeLoaded(Date dateTimeLoaded)
	{
		this.dateTimeLoaded = dateTimeLoaded;
	}
	public String toString()
	{
		return "rec=" + recordNum + ", evt=" + eventType + ", key=" + key
			+ ", time=" + dateTimeLoaded;
	}
	
	/**
	 * @return true if this record is the same event, key, and dateTimeLoaded as rhs.
	 */
	public boolean equals(CpDependsNotify rhs)
	{
		// Do not include recordNum in the test. The purpose of this
		// method is to detect adjacent, identical notifications in the table.
		return this.eventType == rhs.eventType
		 && this.key.equals(rhs.key)
		 && this.dateTimeLoaded.equals(rhs.dateTimeLoaded);
	}
}
