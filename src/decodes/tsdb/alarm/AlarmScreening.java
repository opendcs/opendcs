/**
 * $Id$
 * 
 * $Log$
 */
package decodes.tsdb.alarm;

import java.util.ArrayList;
import java.util.Date;

import decodes.db.DataType;
import decodes.db.SiteName;
import decodes.sql.DbKey;
import opendcs.dao.CachableDbObject;

/**
 * Bean class to store a record in the ALARM_SCREENING table.
 * @author mmaloney
 *
 */
public class AlarmScreening
	implements CachableDbObject
{
	// Underlying data from ALARM_SCREENING_TABLE:
	private DbKey screeningId = DbKey.NullKey;
	private String screeningName = null;
	private DbKey siteId = DbKey.NullKey;
	private DbKey datatypeId = DbKey.NullKey;
	private Date startDateTime = null;
	private Date lastModified = null;
	private boolean enabled = true;
	private DbKey alarmGroupId = DbKey.NullKey;
	private String description = null;

	/** Child Limit Sets */
	private ArrayList<AlarmLimitSet> limitSets = new ArrayList<AlarmLimitSet>();
	
	/** Time this object was loaded from database */
	private Date timeLoaded = null;
	
	/** Temporary storage for names when reading XML, before resolving to siteID */
	private ArrayList<SiteName> siteNames = new ArrayList<SiteName>();
	
	/** Temporary storage for datatype when reading XML, before resolving to datatypeID */
	private DataType dataType = null;
	
	/** Temporary storage for group name when reading XML, before resolving to alarmGroupId */
	private String groupName = null;
	
	public AlarmScreening()
	{
	}

	public DbKey getScreeningId()
	{
		return screeningId;
	}

	public void setScreeningId(DbKey screeningId)
	{
		this.screeningId = screeningId;
	}

	public String getScreeningName()
	{
		return screeningName;
	}

	public void setScreeningName(String screeningName)
	{
		this.screeningName = screeningName;
	}

	public DbKey getSiteId()
	{
		return siteId;
	}

	public void setSiteId(DbKey siteId)
	{
		this.siteId = siteId;
	}

	public DbKey getDatatypeId()
	{
		return datatypeId;
	}

	public void setDatatypeId(DbKey datatypeId)
	{
		this.datatypeId = datatypeId;
	}

	public Date getStartDateTime()
	{
		return startDateTime;
	}

	public void setStartDateTime(Date startDateTime)
	{
		this.startDateTime = startDateTime;
	}

	public Date getLastModified()
	{
		return lastModified;
	}

	public void setLastModified(Date lastModified)
	{
		this.lastModified = lastModified;
	}

	public boolean isEnabled()
	{
		return enabled;
	}

	public void setEnabled(boolean enabled)
	{
		this.enabled = enabled;
	}

	public DbKey getAlarmGroupId()
	{
		return alarmGroupId;
	}

	public void setAlarmGroupId(DbKey alarmGroupId)
	{
		this.alarmGroupId = alarmGroupId;
	}

	public String getDescription()
	{
		return description;
	}

	public void setDescription(String description)
	{
		this.description = description;
	}

	@Override
	public DbKey getKey()
	{
		return screeningId;
	}

	@Override
	public String getUniqueName()
	{
		return screeningName;
	}

	public void addLimitSet(AlarmLimitSet als)
	{
		limitSets.add(als);
	}
	
	public ArrayList<AlarmLimitSet> getLimitSets()
	{
		return limitSets;
	}

	public Date getTimeLoaded()
	{
		return timeLoaded;
	}

	public void setTimeLoaded(Date timeLoaded)
	{
		this.timeLoaded = timeLoaded;
	}

	public ArrayList<SiteName> getSiteNames()
	{
		return siteNames;
	}

	public DataType getDataType()
	{
		return dataType;
	}

	public void setDataType(DataType dataType)
	{
		this.dataType = dataType;
		if (!DbKey.isNull(dataType.getId()))
			datatypeId = dataType.getId();
	}

	public String getGroupName()
	{
		return groupName;
	}

	public void setGroupName(String groupName)
	{
		this.groupName = groupName;
	}
}
