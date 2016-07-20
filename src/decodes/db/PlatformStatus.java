package decodes.db;

import ilex.util.Logger;
import ilex.util.TextUtil;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

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
	
	/** For displays, the DAO will retrieve site name and set it here. */
	private String siteName = null;
	
	/** For displays, the DAO will retrieve designator and set it here. */
	private String designator = null;
	
	private static SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd-HH:mm:ss");
	static { sdf.setTimeZone(TimeZone.getTimeZone("UTC")); }
	
	private transient boolean checked = false;
	
	public PlatformStatus(DbKey platformId)
	{
		super(platformId);
		this.platformId = platformId;
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

	public String getSiteName()
	{
		return siteName;
	}

	public void setSiteName(String siteName)
	{
		this.siteName = siteName;
	}

	public String getDesignator()
	{
		return designator;
	}

	public void setDesignator(String designator)
	{
		this.designator = designator;
	}
	
	public String getLastContactTimeStr()
	{
		if (lastContactTime == null)
			return "";
		synchronized(sdf) { return sdf.format(lastContactTime); }
	}

	public String getLastMessageTimeStr()
	{
		if (lastMessageTime == null)
			return "";
		synchronized(sdf) { return sdf.format(lastMessageTime); }
	}
	
	public String getLastErrorTimeStr()
	{
		if (lastErrorTime == null)
			return "";
		synchronized(sdf) { return sdf.format(lastErrorTime); }
	}

	public boolean isChecked()
	{
		return checked;
	}

	public void setChecked(boolean checked)
	{
		this.checked = checked;
	}

	@Override
	public boolean equals(Object rhs)
	{
		if (!(rhs instanceof PlatformStatus))
			return false;
		PlatformStatus p2 = (PlatformStatus)rhs;
		return this.platformId.equals(p2.platformId)
			&& dateCmp(this.lastContactTime, p2.lastContactTime) == 0
			&& dateCmp(this.lastMessageTime, p2.lastMessageTime) == 0
			&& TextUtil.strEqual(this.lastFailureCodes, p2.lastFailureCodes)
			&& dateCmp(this.lastErrorTime, p2.lastErrorTime) == 0
			&& TextUtil.strEqual(this.annotation, p2.annotation);
	}
	
	private int dateCmp(Date d1, Date d2)
	{
		if (d1 == null || d1.getTime() == 0L)
		{
			if (d2 == null || d2.getTime() == 0L)
				return 0;
			return -1;
		}
		else if (d2 == null || d2.getTime() == 0L)
		{
			return 1;
		}
		long d = d1.getTime() - d2.getTime();
		return d < 0 ? -1 : d > 0 ? 1 : 0;
	}
	
	public String getPlatformName()
	{
		String r = getSiteName();
		if (designator != null && designator.length() > 0)
			r = r + "-" + designator;
		return r;
	}
	
}
