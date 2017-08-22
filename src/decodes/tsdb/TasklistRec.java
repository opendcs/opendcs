/**
 * $Id$
 * 
 * Open Source Software
 * 
 * $Log$
 * Revision 1.1.1.1  2014/05/19 15:28:59  mmaloney
 * OPENDCS 6.0 Initial Checkin
 *
 * Revision 1.6  2013/07/12 11:50:53  mmaloney
 * Added tasklist queue stuff.
 *
 * Revision 1.5  2013/03/21 18:27:39  mmaloney
 * DbKey Implementation
 *
 */
package decodes.tsdb;

import java.util.Date;

import decodes.db.Constants;
import decodes.sql.DbKey;

public class TasklistRec
{
	private int recordNum = -1;
	private DbKey sdi = Constants.undefinedId;
	private double value = 0.0;
	private boolean valueWasNull = true;
	private Date timeStamp = null;
	private boolean deleted = false;
	private String unitsAbbr = null;
	private Date versionDate = null;
	private long qualityCode = 0;
	
	private int sourceId = Constants.undefinedIntKey;
	
	// MJM flags are really the same as qualityCode. Modify the get/set methods to
	// use that field.
//	private int flags = 0;
	
	// HDB-specific fields:
	private String interval = null;
	private String tableSelector = null;
	private int modelRunId = Constants.undefinedIntKey;
	
	/**
	 * This is the constructor for a tasklist record.
	 * @param recordNum
	 * @param sdi
	 * @param value
	 * @param valueWasNull
	 * @param timeStamp
	 * @param deleted
	 * @param interval
	 * @param tabsel
	 * @param modelRunId
	 * @param sourceId
	 * @param flags
	 */
	public TasklistRec(int recordNum, DbKey sdi, double value,
		boolean valueWasNull, Date timeStamp, boolean deleted,
		int sourceId, int flags)
	{
		super();
		this.recordNum = recordNum;
		this.sdi = sdi;
		this.value = value;
		this.valueWasNull = valueWasNull;
		this.timeStamp = timeStamp;
		this.deleted = deleted;
		this.sourceId = sourceId;
		this.qualityCode = flags;
	}
	
	/**
	 * Constructor for a CWMS tasklist record
	 * @param recordNum
	 * @param sdi
	 * @param value
	 * @param valueWasNull
	 * @param timeStamp
	 * @param deleted
	 * @param unitsAbbr
	 * @param versionDate
	 * @param qualityCode
	 */
	public TasklistRec(int recordNum, DbKey sdi, double value,
			boolean valueWasNull, Date timeStamp, boolean deleted,
			String unitsAbbr, Date versionDate, long qualityCode)
	{
		super();
		this.recordNum = recordNum;
		this.sdi = sdi;
		this.value = value;
		this.valueWasNull = valueWasNull;
		this.timeStamp = timeStamp;
		this.deleted = deleted;
		this.unitsAbbr = unitsAbbr;
		this.versionDate = versionDate;
		this.qualityCode = qualityCode;
	}
	
	/**
	 * Constructor for an HDB tasklist record
	 * @param recordNum
	 * @param sdi
	 * @param value
	 * @param timeStamp
	 * @param deleted
	 * @param flags
	 * @param interval
	 * @param tableSelector
	 * @param modelRunId
	 */
	public TasklistRec(int recordNum, DbKey sdi, double value,
		Date timeStamp, boolean deleted, int flags, 
		String interval, String tableSelector, int modelRunId)
	{
		super();
		this.recordNum = recordNum;
		this.sdi = sdi;
		this.value = value;
//		this.valueWasNull = valueWasNull;
		this.timeStamp = timeStamp;
		this.deleted = deleted;
		this.qualityCode = flags;
		this.interval = interval;
		this.tableSelector = tableSelector;
		this.modelRunId = modelRunId;
	}
	
	public int getRecordNum()
	{
		return recordNum;
	}
	public void setRecordNum(int recordNum)
	{
		this.recordNum = recordNum;
	}
	public DbKey getSdi()
	{
		return sdi;
	}
	public void setSdi(DbKey sdi)
	{
		this.sdi = sdi;
	}
	public double getValue()
	{
		return value;
	}
	public void setValue(double value)
	{
		this.value = value;
	}
	public boolean isValueWasNull()
	{
		return valueWasNull;
	}
	public void setValueWasNull(boolean valueWasNull)
	{
		this.valueWasNull = valueWasNull;
	}
	public Date getTimeStamp()
	{
		return timeStamp;
	}
	public void setTimeStamp(Date timeStamp)
	{
		this.timeStamp = timeStamp;
	}
	public boolean isDeleted()
	{
		return deleted;
	}
	public void setDeleted(boolean deleted)
	{
		this.deleted = deleted;
	}
	public String getUnitsAbbr()
	{
		return unitsAbbr;
	}
	public void setUnitsAbbr(String unitsAbbr)
	{
		this.unitsAbbr = unitsAbbr;
	}
	public Date getVersionDate()
	{
		return versionDate;
	}
	public void setVersionDate(Date versionDate)
	{
		this.versionDate = versionDate;
	}
	public long getQualityCode()
	{
		return qualityCode;
	}
	public void setQualityCode(long qualityCode)
	{
		this.qualityCode = qualityCode;
	}

	public int getSourceId()
	{
		return sourceId;
	}

	public int getFlags()
	{
		return (int)(qualityCode & 0xffffffff);
	}

	public String getInterval()
	{
		return interval;
	}

	public String getTableSelector()
	{
		return tableSelector;
	}

	public int getModelRunId()
	{
		return modelRunId;
	}

	public void setSourceId(int sourceId)
	{
		this.sourceId = sourceId;
	}

	public void setInterval(String interval)
	{
		this.interval = interval;
	}

	public void setTableSelector(String tableSelector)
	{
		this.tableSelector = tableSelector;
	}

	public void setModelRunId(int modelRunId)
	{
		this.modelRunId = modelRunId;
	}
	
	public String toString()
	{
		return "" + recordNum + ":"
			+ sdi + ":"
			+ value + ":"
			+ valueWasNull + ":"
			+ timeStamp + ":"
			+ deleted + ":"
			+ unitsAbbr + ":"
			+ versionDate + ":"
			+ "0x" + Long.toHexString(qualityCode) + ":"
			+ sourceId + ":"
			+ interval + ":"
			+ tableSelector + ":"
			+ modelRunId
			;
	}
}
