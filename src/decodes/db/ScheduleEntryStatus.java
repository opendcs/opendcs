package decodes.db;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

import decodes.sql.DbKey;

/**
 * This bean holds the status of a schedule entry run.
 * Each time a schedule entry is executed, one of these structures is
 * generated and stored in the database.
 * <p>
 * For an XML database, status are stored in a binary file in the DCSTOOL home 
 * directory. In this case the DbKey ID of this entry will be the record number
 * within the file.
 * 
 * @author mmaloney, Mike Maloney, Cove Software LLC
 */
public class ScheduleEntryStatus
	extends IdDatabaseObject
{
	/** Link to Schedule Entry for which this is a status */
	private DbKey scheduleEntryId = Constants.undefinedId;
	
	/** Time this run was started */
	private Date runStart = null;
	
	/** Time this run terminated. Null means it's still going. */
	private Date runStop = null;
	
	/**
	 * Message time of the last message processed in this run.
	 * Null if no messages were successfully processed.
	 */
	private Date lastMessageTime = null;
	
	/** Host on which this run was performed. */
	private String hostname = null;
	
	/** Current or terminal status of this run */
	private String runStatus = null;
	
	/** Number of messages processed during this run */
	private int numMessages = 0;
	
	/** Number of decoding errors that occurred during this run */
	private int numDecodesErrors = 0;
	
	/** Number of distinct platforms from which this run processed messages */
	private int numPlatforms = 0;
	
	/** Last data source used (with detail) */
	private String lastSource = null;
	
	/** Last consumer used (with detail) */
	private String lastConsumer = null;
	
	/** Date/Time that this status entry was last written to the database */
	private Date lastModified = null;
	
	/** Used to refer to a schedule entry in an XML database */
	private String scheduleEntryName = "";
	
	private static SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd-HH:mm:ss");
	static { sdf.setTimeZone(TimeZone.getTimeZone("UTC")); }
	
	public ScheduleEntryStatus(DbKey id)
	{
		super(id);
	}

	@Override
	public String getObjectType()
	{
		return "ScheduleEntryStatus";
	}

	@Override
	public void prepareForExec() throws IncompleteDatabaseException,
		InvalidDatabaseException
	{
	}

	@Override
	public boolean isPrepared()
	{
		return false;
	}

	@Override
	public void validate() throws IncompleteDatabaseException,
		InvalidDatabaseException
	{
	}

	@Override
	public void read() throws DatabaseException
	{
	}

	@Override
	public void write() throws DatabaseException
	{
	}

	public DbKey getScheduleEntryId()
	{
		return scheduleEntryId;
	}

	public void setScheduleEntryId(DbKey scheduleEntryId)
	{
		this.scheduleEntryId = scheduleEntryId;
	}

	public Date getRunStart()
	{
		return runStart;
	}

	public void setRunStart(Date runStart)
	{
		this.runStart = runStart;
	}

	public Date getRunStop()
	{
		return runStop;
	}

	public void setRunStop(Date runStop)
	{
		this.runStop = runStop;
	}

	public Date getLastMessageTime()
	{
		return lastMessageTime;
	}

	public void setLastMessageTime(Date lastMessageTime)
	{
		this.lastMessageTime = lastMessageTime;
	}

	public String getHostname()
	{
		return hostname;
	}

	public void setHostname(String hostname)
	{
		this.hostname = hostname;
	}

	public String getRunStatus()
	{
		return runStatus;
	}

	public void setRunStatus(String runStatus)
	{
		this.runStatus = runStatus;
	}

	public int getNumMessages()
	{
		return numMessages;
	}

	public void setNumMessages(int numMessages)
	{
		this.numMessages = numMessages;
	}

	public int getNumDecodesErrors()
	{
		return numDecodesErrors;
	}

	public void setNumDecodesErrors(int numDecodesErrors)
	{
		this.numDecodesErrors = numDecodesErrors;
	}

	public int getNumPlatforms()
	{
		return numPlatforms;
	}

	public void setNumPlatforms(int numPlatforms)
	{
		this.numPlatforms = numPlatforms;
	}

	public Date getLastModified()
	{
		return lastModified;
	}

	public void setLastModified(Date lastModified)
	{
		this.lastModified = lastModified;
	}

	public String getScheduleEntryName()
	{
		return scheduleEntryName;
	}

	public void setScheduleEntryName(String scheduleEntryName)
	{
		this.scheduleEntryName = scheduleEntryName;
	}

	public String getLastSource()
	{
		return lastSource;
	}

	public void setLastSource(String lastSource)
	{
		this.lastSource = lastSource;
	}

	public String getLastConsumer()
	{
		return lastConsumer;
	}

	public void setLastConsumer(String lastConsumer)
	{
		this.lastConsumer = lastConsumer;
	}
	
	public String getRunStartStr()
	{
		synchronized(sdf) { return runStart == null ? "" : sdf.format(runStart); }
	}
	
	public String getRunStopStr()
	{
		synchronized(sdf) { return runStop == null ? "" : sdf.format(runStop); }
	}

	public String getLastMessageTimeStr()
	{
		synchronized(sdf) { return lastMessageTime == null ? "" : sdf.format(lastMessageTime); }
	}

	public String getLastModifiedStr()
	{
		synchronized(sdf) { return lastModified == null ? "" : sdf.format(lastModified); }
	}
	
	public String getStats()
	{
		return "#msgs=" + getNumMessages() + ", #plat=" + getNumPlatforms() + ", #errs="
			+ getNumDecodesErrors();
	}
}
