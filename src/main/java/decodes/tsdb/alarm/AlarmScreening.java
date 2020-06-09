/**
 * $Id$
 * 
 * $Log$
 * Revision 1.1  2019/05/10 18:35:25  mmaloney
 * dev
 *
 */
package decodes.tsdb.alarm;

import java.util.ArrayList;
import java.util.Date;

import decodes.db.DataType;
import decodes.db.SiteName;
import decodes.sql.DbKey;
import ilex.util.TextUtil;
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

	/**
	 * Copies the GUI-editable fields from the passed screening object into THIS object,
	 * including the subordinate LimitSets.
	 * Does not change screening ID.
	 * @param scrn
	 */
	public void copyFrom(AlarmScreening scrn)
	{
		this.screeningName = scrn.screeningName;
		this.siteId = scrn.siteId;
		this.datatypeId = scrn.datatypeId;
		this.startDateTime = scrn.startDateTime;
		this.lastModified = scrn.lastModified;
		this.enabled = scrn.enabled;
		this.alarmGroupId = scrn.alarmGroupId;
		this.description = scrn.description;
		
		this.siteNames.clear();
		for(SiteName sn : scrn.siteNames)
			this.siteNames.add(sn);

		this.timeLoaded = scrn.timeLoaded;
		this.dataType = scrn.dataType;
		this.groupName = scrn.groupName;

		limitSets.clear();
		for(AlarmLimitSet als : scrn.limitSets)
		{
			AlarmLimitSet cp = new AlarmLimitSet();
			cp.copyFrom(als);
			limitSets.add(cp);
		}
	}
	
	@Override
	public boolean equals(Object rhs)
	{
		if (this == rhs)
			return true;
		if (!(rhs instanceof AlarmScreening))
			return false;
		AlarmScreening s2 = (AlarmScreening)rhs;
		
		if (!TextUtil.strEqual(screeningName, s2.screeningName))
			return false;
//System.out.println("se1");
		if (!siteId.equals(s2.siteId))
			return false;

		if (!datatypeId.equals(s2.datatypeId))
			return false;
		
		if (!TextUtil.dateEqual(startDateTime, s2.startDateTime))
			return false;

		// Skip lastModified
		
//System.out.println("se2");
		if (enabled != s2.enabled)
			return false;

		if (!alarmGroupId.equals(s2.alarmGroupId))
			return false;
		
		if (!TextUtil.strEqual(description, s2.description))
			return false;
		
		if (this.limitSets.size() != s2.getLimitSets().size())
			return false;

//System.out.println("se3");

		for(AlarmLimitSet thisls : this.limitSets)
		{
			boolean found = false;
			for(AlarmLimitSet s2ls : s2.getLimitSets())
				if (TextUtil.strEqual(thisls.getSeasonName(), s2ls.getSeasonName()))
				{
					found = true;
					if (!thisls.equals(s2ls))
						return false;
					break;
				}
			if (!found)
				return false;
		}

		return true;
	}
	
}
