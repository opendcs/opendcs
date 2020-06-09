/**
 * $Id$
 * 
 * $Log$
 */
package decodes.tsdb.alarm;

import java.util.Date;

import decodes.sql.DbKey;

/**
 * Bean class holding an alarm assertion from either the ALARM_CURRENT
 * or ALARM_HISTORY tables. Current alarms have a null endTime.
 * @author mmaloney
 *
 */
public class AlarmAssertion
{
	/** Surrogate key of time series that this alarm applies to */
	private DbKey tsKey = DbKey.NullKey;
	
	/** Limit set used to generate this alarm */
	private DbKey limitSetId = DbKey.NullKey;
	
	private Date assertTime = null;
	private double dataValue = 0.0;
	private Date dataTime = null;
	private int flags = 0;
	private String message = null;
	private Date lastNotifySent = null;
	private Date endTime = null;
	private String cancelledBy = null;

	public AlarmAssertion()
	{
	}

	public DbKey getTsKey()
	{
		return tsKey;
	}

	public void setTsKey(DbKey tsKey)
	{
		this.tsKey = tsKey;
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

	public int getFlags()
	{
		return flags;
	}

	public void setFlags(int flags)
	{
		this.flags = flags;
	}

	public String getMessage()
	{
		return message;
	}

	public void setMessage(String message)
	{
		this.message = message;
	}

	public Date getLastNotifySent()
	{
		return lastNotifySent;
	}

	public void setLastNotifySent(Date lastNotifySent)
	{
		this.lastNotifySent = lastNotifySent;
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
}
