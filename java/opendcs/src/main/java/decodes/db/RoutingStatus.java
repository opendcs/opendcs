package decodes.db;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

import decodes.sql.DbKey;

/**
 * This bean holds the status of a routing spec.
 *
 * This is used by the REST API.
 *
 * @author zack-rma, Zack Olson, GEI Consultants Inc.
 */
public class RoutingStatus extends IdDatabaseObject
{
	/** Link to Routing Spec for which this is a status */
	private DbKey routingSpecId = Constants.undefinedId;

	/** Link to Schedule Entry that contains the status */
	private DbKey scheduleEntryId = Constants.undefinedId;

	/** Name of the routing spec */
	private String name;

	/** Whether the route is enabled */
	private Boolean isEnabled = false;

	/** Time this routing spec last had activity */
	private Date lastActivityTime = null;

	/**
	 * Message time of the last message processed in this run.
	 * Null if no messages were successfully processed.
	 */
	private Date lastMessageTime = null;

	/** App ID associated with the Routing Spec. */
	private DbKey appId;

	/** Run interval for this routing spec. */
	private String runInterval = null;

	/** Number of messages processed during this run */
	private int numMessages = 0;

	/** Number of decoding errors that occurred during this run */
	private int numDecodesErrors = 0;

	/** Whether the status is manual */
	private Boolean isManual = false;

	/** Last data source used (with detail) */
	private String lastSource = null;

	/** The name of the app associated with the Routing Spec. */
	private String appName;

	private static SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd-HH:mm:ss");
	static { sdf.setTimeZone(TimeZone.getTimeZone("UTC")); }

	public RoutingStatus(DbKey id)
	{
		super(id);
	}

	@Override
	public String getObjectType()
	{
		return "RoutingStatus";
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

	public DbKey getRoutingSpecId()
	{
		return routingSpecId;
	}

	public void setRoutingSpecId(DbKey routingSpecId)
	{
		this.routingSpecId = routingSpecId;
	}

	public DbKey getScheduleEntryId()
	{
		return scheduleEntryId;
	}

	public void setScheduleEntryId(DbKey scheduleEntryId)
	{
		this.scheduleEntryId = scheduleEntryId;
	}

	public String getName()
	{
		return name;
	}

	public void setName(String name)
	{
		this.name = name;
	}

	public Boolean isEnabled()
	{
		return isEnabled;
	}

	public void setEnabled(Boolean isEnabled)
	{
		this.isEnabled = isEnabled;
	}

	public Boolean isManual()
	{
		return isManual;
	}

	public void setManual(Boolean isManual)
	{
		this.isManual = isManual;
	}

	public DbKey getAppId()
	{
		return appId;
	}

	public void setAppId(DbKey appId)
	{
		this.appId = appId;
	}

	public String getAppName()
	{
		return appName;
	}

	public void setAppName(String appName)
	{
		this.appName = appName;
	}

	public String getRunInterval()
	{
		return runInterval;
	}

	public void setRunInterval(String runInterval)
	{
		this.runInterval = runInterval;
	}

	public Date getLastActivityTime()
	{
		return lastActivityTime;
	}

	public void setLastActivityTime(Date lastActivityTime)
	{
		this.lastActivityTime = lastActivityTime;
	}

	public Date getLastMessageTime()
	{
		return lastMessageTime;
	}

	public void setLastMessageTime(Date lastMessageTime)
	{
		this.lastMessageTime =
				lastMessageTime == null || lastMessageTime.getTime() == 0L ? null:
						lastMessageTime;
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

	public String getLastSource()
	{
		return lastSource;
	}

	public void setLastSource(String lastSource)
	{
		this.lastSource = lastSource;
	}

	public String getLastMessageTimeStr()
	{
		synchronized(sdf) { return lastMessageTime == null ? "" : sdf.format(lastMessageTime); }
	}

	public String getStats()
	{
		return "#msgs=" + getNumMessages() + ", #errs="
				+ getNumDecodesErrors();
	}
}
