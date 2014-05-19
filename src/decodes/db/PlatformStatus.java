package decodes.db;

import ilex.util.Logger;

import java.util.Date;

import opendcs.dai.PlatformStatusDAI;

import decodes.sql.DbKey;
import decodes.tsdb.DbIoException;

/**
 * Bean class for PLATFORM_STATUS record in the DECODES database
 * @author Mike Maloney, Cove Software, LLC
 */
public class PlatformStatus
	extends IdDatabaseObject
{
	/** One status rec per platform, so platformId serves as primary key here. */
	private DbKey platformId = Constants.undefinedId;
	
	/** Last time a contact was made with this station (null means never) */
	private Date lastContactTime = null;
	
	/** Last time a message was received from this platform (null means never) */
	private Date lastMessageTime = null;
	
	/** Failure code(s) from last message received */
	private String lastFailureCodes = null;
	
	/** Last time an error occurred in a station contact */
	private Date lastErrorTime = null;
	
	/** Context-dependent annotation on this status record */
	private String annotation = null;
	
	/** Link to ScheduleEntryStatus rec for the last contact */
	private DbKey lastScheduleEntryStatusId = Constants.undefinedId;
	
	public PlatformStatus(DbKey platformId)
	{
		super(platformId);
		this.platformId = platformId;
		Logger.instance().debug3("New PlatformStatus for id=" + platformId);
	}

	public DbKey getPlatformId()
	{
		return platformId;
	}

	public void setPlatformId(DbKey platformId)
	{
		this.platformId = platformId;
	}

	public Date getLastContactTime()
	{
		return lastContactTime;
	}

	public void setLastContactTime(Date lastContactTime)
	{
		this.lastContactTime = lastContactTime;
	}

	public Date getLastMessageTime()
	{
		return lastMessageTime;
	}

	public void setLastMessageTime(Date lastMessageTime)
	{
		this.lastMessageTime = lastMessageTime;
	}

	public String getLastFailureCodes()
	{
		return lastFailureCodes;
	}

	public void setLastFailureCodes(String lastFailureCodes)
	{
		this.lastFailureCodes = lastFailureCodes;
	}

	public Date getLastErrorTime()
	{
		return lastErrorTime;
	}

	public void setLastErrorTime(Date lastErrorTime)
	{
		this.lastErrorTime = lastErrorTime;
	}

	public String getAnnotation()
	{
		return annotation;
	}

	public void setAnnotation(String annotation)
	{
		this.annotation = annotation;
	}

	public DbKey getLastScheduleEntryStatusId()
	{
		return lastScheduleEntryStatusId;
	}

	public void setLastScheduleEntryStatusId(DbKey lastScheduleEntryStatusId)
	{
		this.lastScheduleEntryStatusId = lastScheduleEntryStatusId;
	}

	@Override
	public String getObjectType()
	{
		return "PlatformStatus";
	}

	@Override
	public void prepareForExec() throws IncompleteDatabaseException, InvalidDatabaseException
	{
		// Does nothing
	}

	@Override
	public boolean isPrepared()
	{
		return false;
	}

	@Override
	public void validate() throws IncompleteDatabaseException, InvalidDatabaseException
	{
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
			PlatformStatusDAI platformStatusDAO = getDatabase().getDbIo().makePlatformStatusDAO();
			if (platformStatusDAO == null)
			{
				Logger.instance().debug1("Cannot write PlatformStatus -- not supported in this database.");
				return;
			}
				
			try
			{
				platformStatusDAO.writePlatformStatus(this);
			}
			catch (DbIoException ex)
			{
				String msg = "Cannot write " + getObjectType() + " platformId=" + getKey()
					+ ": " + ex;
				Logger.instance().warning(msg);
				throw new DatabaseException(msg);
			}
			finally
			{
				platformStatusDAO.close();
			}
		}
	}
}
