/*
*  $Id$
*
*  $State$
*
*/
package decodes.datasource;

import java.util.Properties;
import java.util.Vector;
import java.util.Iterator;
import java.util.Date;

import ilex.util.Logger;
import ilex.util.IDateFormat;
import ilex.util.PropertiesUtil;

import decodes.db.DataSource;
import decodes.db.InvalidDatabaseException;

/**
RoundRobinGroup implements the data source interface. It manages a
group of coordinate data sources and requests data from each in 
a round robin fashiion.  
<p>
RoundRobinGroup is designed to manage a group of datasource interfaces.
It will process all the data available from one data source and then
move on to the next one.
*/
public class RoundRobinGroup
	extends DataSourceExec
{
	Properties props;          // Properties from routing spec
	String since, until;       // Since & until time from routing spec
	Vector networkLists;       // Network Lists from routing spec
	long lastInitAttempt;      // Last time we tried to find a delegate
	Date lastTimeStamp;        // Time stamp of last message returned
	long initPeriod;           // # msec to retry connecting.
	long recheckPeriod;        // # msec to retry higher-priority sources.

	Vector <DataSourceExec> delegates;            // Group members to delegate requests to.
    int currentSource = 0;
	DataSourceExec activeMember; // Current group member we're delegating to

	/** default constructor */
	public RoundRobinGroup()
	{
		super();

		props = null;
		since = null;
		until = null;
		networkLists = null;
		activeMember = null;
		lastInitAttempt = 0L;
		lastTimeStamp = null;
//		initPeriod = 10 * 1000L;         // 10 sec
		initPeriod = 0;
		delegates = new Vector<DataSourceExec>();
	}

	/**
	  Returns the currently active coordinate data source member.
	  @return the currently active coordinate data source member.
	*/
	public String getActiveSource()
	{
		if (activeMember == null)
			return "none";
		else
			return activeMember.getActiveSource();
	}

	/**
	  Called right after instantiation. Build the hierarchy of delegates.
	*/
	public void processDataSource()
		throws InvalidDatabaseException
	{
		Logger.instance().log(Logger.E_DEBUG1, 
			"Initializing RoundRobinGroup for '" + dbDataSource.getName() 
			+ "', args='" +dbDataSource.dataSourceArg+"'");

		// This is a data-source-group, call prepareForExec() for all members
		for(Iterator it = dbDataSource.groupMembers.iterator(); it.hasNext(); )
		{
			DataSource ds = (DataSource)it.next();
			if (ds != null)  // there may be empty slots.
			{
				Logger.instance().log(Logger.E_DEBUG1,
					"Making delegate for '" + ds.getName() + "'");
				DataSourceExec del = ds.makeDelegate(); // recursion!
				del.setAllowNullPlatform(getAllowNullPlatform());
				del.setAllowDapsStatusMessages(getAllowDapsStatusMessages());
				delegates.add(del);
			}
		}
	}

	/**
	  Called right before this datasource gets used by a routing spec.
	  @param props the routing spec properties.
	  @param since the since time from the routing spec.
	  @param until the until time from the routing spec.
	  @param networkLists contains NetworkList objects.
	*/
	public void init(Properties props, String since, String until,
		Vector networkLists)
		throws DataSourceException
	{
		// Save parameters to pass to group members as needed.
		this.props = props;
		this.since = since;
		this.until = until;
		this.networkLists = networkLists;
        this.currentSource=0;
		// Parse data source properties.
		activeMember = null;
		findActiveMember();
		Logger.instance().log(Logger.E_DEBUG1, 
			"RoundRobinGroup.init() for '" + dbDataSource.getName() 
			+ "' since='" + since + "' until= '" + until);
	}

	/**
	  Find an active member to delegate to.
	  @throws DataSourceException if we can't find one.
	*/
	private void findActiveMember()
	{
		lastInitAttempt = System.currentTimeMillis();

		// Go through group members - find the first one we can init.
		for(Iterator it = delegates.iterator(); it.hasNext(); )
		{
			DataSourceExec ds = (DataSourceExec)delegates.elementAt(currentSource);
            currentSource++;
            if ( currentSource >= delegates.size() )
              currentSource = 0;
			/*
			  If currentSource is activeMember, i.e. only one source in 
              group, then return;
			*/
			if (activeMember == ds)
				return;
			try
			{
				ds.init(props, since, until, networkLists);
				if (activeMember != null)
					activeMember.close();
			
				activeMember = ds;
				return;
			}
			catch(DataSourceException e)
			{
				Logger.instance().log(Logger.E_WARNING, 
					"Could not initialize data source '" 
					+ ds.dbDataSource.getName() + "': " + e);
			}
		}
	}

	/** Delegates to the currently active member, if there is one. */
	public void close()
	{
		if (activeMember != null)
			activeMember.close();
		activeMember = null;
	}

	/**
	  Returns the next RawMessage.
	  @return the next RawMessage.
	*/
	public RawMessage getRawMessage()
		throws DataSourceException
	{
		long now = System.currentTimeMillis();

		//
		// First sort out which data source I'm going to use...
		//
		if (activeMember == null && now - lastInitAttempt >= initPeriod)
		{
			// No active member. Periodically attempt to get one.
			Logger.instance().log(Logger.E_DEBUG1, 
				"Group '" + dbDataSource.getName() + 
				"' Attempting to find a group member.");

			findActiveMember();

			// If no success, pause a couple seconds.
			if (activeMember == null)
			{
				Logger.instance().log(Logger.E_FAILURE, 
					"No data sources in group '" + dbDataSource.getName()
					+ "' are available now. Will retry later.");
				try { Thread.sleep(2000L); }
				catch(InterruptedException ie) {};
			}
		}
		//
		// If I have a data source attempt to read a message from it.
		//
		if (activeMember != null)
		{
			try
			{
				RawMessage ret = activeMember.getRawMessage();
//				lastTimeStamp = ret.getTimeStamp();
				if ( ret == null )
					close();
				return ret;
			}
			catch(DataSourceEndException e)
			{
				Logger.instance().log(Logger.E_INFORMATION, 
					"DataSource '" + activeMember.getName()
					+ "' End of Data Stream: " + e.getMessage());
				close();
				throw e;
			}
			catch(UnknownPlatformException upex)
			{
				throw upex;
			}
			catch(DataSourceException e)
			{
				Logger.instance().log(Logger.E_WARNING, 
					"DataSource '" + activeMember.getName()
					+ "' failed: " + e);
				close();
			}
		}
		return null;
	}

	/** @return name of currently active member. */
	public String getActiveMemberName()
	{
		return activeMember != null ? activeMember.getName() : "(none)";
	}

	/** @return the currently active member. */
	public DataSourceExec getActiveMember()
	{
		return activeMember;
	}

	/**
	  Sets flag to allow null platforms (that is, messges for which there
	  is no DECODES database record).
	  @param tf the flag
	*/
	public void setAllowNullPlatform(boolean tf) 
	{
		super.setAllowNullPlatform(tf);
		for(Iterator it = delegates.iterator(); it.hasNext(); )
		{
			DataSourceExec ds = (DataSourceExec)it.next();
			ds.setAllowNullPlatform(tf);
		}
	}

	/**
	  Sets flag to allow daps status messages 
	  @param tf the flag
	*/
	public void setAllowDapsStatusMessages(boolean tf)
	{
		super.setAllowDapsStatusMessages(tf);
		for(Iterator it = delegates.iterator(); it.hasNext(); )
		{
			DataSourceExec ds = (DataSourceExec)it.next();
			ds.setAllowDapsStatusMessages(tf);
		}
	}
		
	/** Aborts the current get method, if one is in progress. */
	public void abortGetRawMessage()
	{
		if (activeMember != null)
		{
			Logger.instance().debug1("Aborting active LRGS member.");
			activeMember.abortGetRawMessage();
		}
		else
			Logger.instance().debug1("No currently active data source member.");
	}
}

