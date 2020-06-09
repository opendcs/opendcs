/**
 * $Id$
 * 
 * $Log$
 */
package decodes.tsdb.alarm;

import java.util.Date;

import decodes.sql.DbKey;
import decodes.tsdb.TimeSeriesIdentifier;

/**
 * Represents either a current or historical alarm in HDB.
 * A Historical alarm will have a non-null endTime.
 */
public class Alarm
{
	/** ID of Time Series being alarmed */
	private DbKey tsidKey = DbKey.NullKey;
	
	/** ID of ALARM_LIMIT_SET that generated the alarm */
	private DbKey limitSetId = DbKey.NullKey;
	
	/** Time alarm was initially asserted */
	private Date assertTime = null;
	
	/** Data Value that caused the alarm */
	private double dataValue = 0.0;
	
	/** Time of the data value that caused the alarm */
	private Date dataTime = null;
	
	/** Flags describing the alarm event */
	private int alarmFlags;
	
	/** Message generated for this alarm */
	private String message;
	
	/** Last time notifications were sent for this alarm (null means never) */
	private Date lastNotificationTime = null;
	
	/** Time that the alarm ended (either cancelled, or value came back within limits */
	private Date endTime;
	
	/** If cancelled, this is the username */
	private String cancelledBy = null;
	
	// Transient values derived from the IDs stored in the database
	private TimeSeriesIdentifier tsid = null;
	private AlarmLimitSet limitSet = null;
	
	// Last time the alarm was read from or written to the database
	private long lastDbSyncMs = 0L;
	
	// Used for database synching.
	private boolean checked = false;
	
	
	public DbKey getTsidKey()
	{
		return tsidKey;
	}
	public void setTsidKey(DbKey tsidKey)
	{
		this.tsidKey = tsidKey;
	}
	public DbKey getLimitSetId()
	{
		return limitSetId;
	}
	public void setLimitSetId(DbKey limitSetId)
	{
		this.limitSetId = limitSetId;
	}
	public Date getAssertTime()
	{
		return assertTime;
	}
	public void setAssertTime(Date assertTime)
	{
		this.assertTime = assertTime;
	}
	public double getDataValue()
	{
		return dataValue;
	}
	public void setDataValue(double dataValue)
	{
		this.dataValue = dataValue;
	}
	public Date getDataTime()
	{
		return dataTime;
	}
	public void setDataTime(Date dataTime)
	{
		this.dataTime = dataTime;
	}
	public int getAlarmFlags()
	{
		return alarmFlags;
	}
	public void setAlarmFlags(int alarmFlags)
	{
		this.alarmFlags = alarmFlags;
	}
	public String getMessage()
	{
		return message;
	}
	public void setMessage(String message)
	{
		this.message = message;
	}
	public Date getLastNotificationTime()
	{
		return lastNotificationTime;
	}
	public void setLastNotificationTime(Date lastNotificationTime)
	{
		this.lastNotificationTime = lastNotificationTime;
	}
	public Date getEndTime()
	{
		return endTime;
	}
	public void setEndTime(Date endTime)
	{
		this.endTime = endTime;
	}
	public String getCancelledBy()
	{
		return cancelledBy;
	}
	public void setCancelledBy(String cancelledBy)
	{
		this.cancelledBy = cancelledBy;
	}
	public TimeSeriesIdentifier getTsid()
	{
		return tsid;
	}
	public void setTsid(TimeSeriesIdentifier tsid)
	{
		this.tsid = tsid;
	}
	public AlarmLimitSet getLimitSet()
	{
		return limitSet;
	}
	public void setLimitSet(AlarmLimitSet limitSet)
	{
		this.limitSet = limitSet;
	}
	public long getLastDbSyncMs()
	{
		return lastDbSyncMs;
	}
	public void setLastDbSyncMs(long lastDbSyncMs)
	{
		this.lastDbSyncMs = lastDbSyncMs;
	}
	public boolean isChecked()
	{
		return checked;
	}
	public void setChecked(boolean checked)
	{
		this.checked = checked;
	}
	
	
	

}
