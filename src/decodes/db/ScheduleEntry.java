package decodes.db;

import ilex.util.Logger;
import ilex.util.TextUtil;

import java.util.Date;

import opendcs.dai.LoadingAppDAI;
import opendcs.dai.ScheduleEntryDAI;

import decodes.sql.DbKey;
import decodes.tsdb.DbIoException;

/**
 * A schedule entry executes a routing spec on a specified schedule.
 * 
 * @author mmaloney Mike Maloney, Cove Software, LLC.
 */
public class ScheduleEntry extends IdDatabaseObject
{
	/** Unique name for this schedule entry */
	private String name = null;
	
	/** Loading app that is assigned to execute this schedule entry */
	private DbKey loadingAppId = Constants.undefinedId;
	
	/** Routing spec to execute */
	private DbKey routingSpecId = Constants.undefinedId;

	/** 
	 * Start executing this schedule entry at this time.
	 * Null means run continuously.
	 * Non-null means either one-time (if runInterval==null) or periodic.
	 */
	private Date startTime = null;
	
	/** Used in adding interval to start time */
	private String timezone = null;
	
	/** 
	 * Parsable interval at which to run this entry. Null means run once only
	 * if startTime != null. OR continuously if startTime == null.
	 */
	private String runInterval = null;
	
	/** Only execute when enabled */
	private boolean enabled = true;
	
	/** Time that this entry was last modified in the database */
	private Date lastModified = null;
	
	/** When read from XML, this is the name of the loading app */
	private String loadingAppName = null;
	
	/** When read from XML, this is the routing spec name */
	private String routingSpecName = null;
	
	private DbKey lastScheduleEntryStatusId = DbKey.NullKey;
	
	/**
	 * Constructor for SQL where the ID is the key
	 * @param id the key
	 */
	public ScheduleEntry(DbKey id)
	{
		super(id);
	}
	
	/**
	 * Constructor for XML where name is the key.
	 */
	public ScheduleEntry(String name)
	{
		this.name = name;
	}
	
	@Override
	public String getObjectType()
	{
		return "ScheduleEntry";
	}

	@Override
	public void prepareForExec() throws IncompleteDatabaseException,
		InvalidDatabaseException
	{
		// TODO Auto-generated method stub

	}

	@Override
	public boolean isPrepared()
	{
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void validate() throws IncompleteDatabaseException,
		InvalidDatabaseException
	{
		// TODO Auto-generated method stub

	}

	@Override
	public void read() throws DatabaseException
	{
		// TODO Auto-generated method stub

	}

	@Override
	public void write() throws DatabaseException
	{
		if (getDatabase() != null)
		{
			ScheduleEntryDAI scheduleEntryDAO = getDatabase().getDbIo().makeScheduleEntryDAO();
			if (scheduleEntryDAO == null)
			{
				Logger.instance().debug1("Cannot write schedule entry. Not supported on this database.");
				return;
			}
			try
			{
				scheduleEntryDAO.writeScheduleEntry(this);
			}
			catch (DbIoException ex)
			{
				String msg = "Cannot write scheduleEntry '" + getName()
					+ "': " + ex;
				Logger.instance().warning(msg);
				throw new DatabaseException(msg);
			}
			finally
			{
				scheduleEntryDAO.close();
			}
		}
	}

	public String getName()
	{
		return name;
	}

	public void setName(String name)
	{
		this.name = name;
	}

	public DbKey getLoadingAppId()
	{
		return loadingAppId;
	}

	public void setLoadingAppId(DbKey loadingAppId)
	{
		this.loadingAppId = loadingAppId;
	}

	public DbKey getRoutingSpecId()
	{
		return routingSpecId;
	}

	public void setRoutingSpecId(DbKey routingSpecId)
	{
		this.routingSpecId = routingSpecId;
	}

	public Date getStartTime()
	{
		return startTime;
	}

	public void setStartTime(Date startTime)
	{
		this.startTime = startTime;
	}

	public String getRunInterval()
	{
		return runInterval;
	}

	public void setRunInterval(String runInterval)
	{
		this.runInterval = runInterval;
	}

	public boolean isEnabled()
	{
		return enabled;
	}

	public void setEnabled(boolean enabled)
	{
		this.enabled = enabled;
	}

	public Date getLastModified()
	{
		return lastModified;
	}

	public void setLastModified(Date lastModified)
	{
		this.lastModified = lastModified;
	}

	public String getLoadingAppName()
	{
		return loadingAppName;
	}

	public void setLoadingAppName(String loadingAppName)
	{
		this.loadingAppName = loadingAppName;
	}

	public String getRoutingSpecName()
	{
		return routingSpecName;
	}

	public void setRoutingSpecName(String routingSpecName)
	{
		this.routingSpecName = routingSpecName;
	}
	
	/**
	 * Used by update/check methods.
	 * @param rhs the schedule entry to copy from.
	 */
	public void copyFrom(ScheduleEntry rhs)
	{
		this.name = rhs.name;
		this.loadingAppId = rhs.loadingAppId;
		this.routingSpecId = rhs.routingSpecId;
		this.startTime = rhs.startTime;
		this.runInterval = rhs.runInterval;
		this.enabled = rhs.enabled;
		this.lastModified = rhs.lastModified;
		this.loadingAppName = rhs.loadingAppName;
		this.routingSpecName = rhs.routingSpecName;
	}

	public String getTimezone()
	{
		return timezone;
	}

	public void setTimezone(String timezone)
	{
		this.timezone = timezone;
	}
	
	@Override
	public boolean equals(Object rhs)
	{
		if (!(rhs instanceof ScheduleEntry))
			return false;
		ScheduleEntry se = (ScheduleEntry)rhs;
		if (this == se)
			return true;
		
		if (!TextUtil.strEqual(name, se.name)
		 || !getId().equals(se.getId())
		 || !loadingAppId.equals(se.loadingAppId)
		 || !routingSpecId.equals(se.routingSpecId))
		{
			Logger.instance().debug1("id, appId, or rsId is different this=" + toString() + "\n rhs="
				+ se.toString());
			return false;
		}
		
		if (startTime != se.startTime)
		{
			if (startTime == null || se.startTime == null)
			{
				Logger.instance().debug1("one start time is null this=" + toString() + "\n rhs="
					+ se.toString());
				return false;
			}
			if (!startTime.equals(se.startTime))
			{
				Logger.instance().debug1("Start times are null this=" + toString() + "\n rhs="
					+ se.toString());
				return false;
			}
		}

		if (!TextUtil.strEqual(timezone, se.timezone)
		 || !TextUtil.strEqual(runInterval, se.runInterval)
		 || enabled != se.enabled
		 || !TextUtil.strEqual(loadingAppName, se.loadingAppName)
		 || !TextUtil.strEqual(routingSpecName, se.routingSpecName))
		{
			Logger.instance().debug1("tz, runInt, enab, appName or rsName different this=" + toString() + "\n rhs="
				+ rhs.toString());
			return false;
		}

		return true;
	}
	
	public String toString()
	{
		return "ScheduleEntry(" + this.getId() + ")"
			+ " name='" + name + "'"
			+ " loadingAppId=" + loadingAppId
			+ " loadingAppName='" + loadingAppName + "'"
			+ " routingSpecId=" + routingSpecId
			+ " routingSpecName='" + routingSpecName + "'"
			+ " startTime=" + startTime
			+ " timezone='" + timezone + "'"
			+ " runInterval='" + runInterval + "'"
			+ " enabled=" + enabled
			+ " lastModified=" + lastModified;
	}

	public DbKey getLastScheduleEntryStatusId()
	{
		return lastScheduleEntryStatusId;
	}

	public void setLastScheduleEntryStatusId(DbKey lastScheduleEntryStatusId)
	{
		this.lastScheduleEntryStatusId = lastScheduleEntryStatusId;
	}

}
