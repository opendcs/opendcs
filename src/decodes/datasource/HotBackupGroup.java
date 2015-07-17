/*
*  $Id$
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
import decodes.db.NetworkList;

/**
HotBackupGroup implements the data source interface. It manages a
group of subordinate data sources. One data source is used at a 
time. If (and only if) a failure is detected in the 'current' data 
source, one of the 'backup' data sources, will become the 'current'.
<p>
HotBackupGroup is designed to manage a group of LRGS/DRS interfaces.
When one connection goes bad (e.g. the server is not available), it
will switch to another and attempt to pick up where it left off.
<p>
The first data source in the group is considered the 'preferred' 
data source. Even when other sources are active, the HotBackupGroup
will periodically attempt to re-establish a connection to the preferred
source.
*/
public class HotBackupGroup
	extends DataSourceExec
{
	Properties props;          // Properties from routing spec
	String since, until;       // Since & until time from routing spec
	Vector networkLists;       // Network Lists from routing spec
	long lastInitAttempt;      // Last time we tried to find a delegate
	Date lastTimeStamp;        // Time stamp of last message returned
	long initPeriod;           // # msec to retry connecting.
	long recheckPeriod;        // # msec to retry higher-priority sources.

	Vector delegates;            // Group members to delegate requests to.
	DataSourceExec activeMember; // Current group member we're delegating to

	/** default constructor */
	public HotBackupGroup()
	{
		super();

		props = null;
		since = null;
		until = null;
		networkLists = null;
		activeMember = null;
		lastInitAttempt = 0L;
		lastTimeStamp = null;
		initPeriod = 10 * 1000L;         // 10 sec
		recheckPeriod = 15 * 60 * 1000L; // 15 minutes
		delegates = new Vector();
	}

	/**
	  Returns the currently active subordinate data source member.
	  @return the currently active subordinate data source member.
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
			"Initializing HotBackupGroup for '" + dbDataSource.getName() 
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
				if (del instanceof LrgsDataSource)
				{
					LrgsDataSource lds = (LrgsDataSource)del;
					lds.setTimeoutSecOnError(180);
					lds.setRoutingSpecThread(routingSpecThread);
				}
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
		Vector<NetworkList> networkLists)
		throws DataSourceException
	{
		// Save parameters to pass to group members as needed.
		this.props = props;
		this.since = since;
		this.until = until;
		this.networkLists = networkLists;

		// Parse data source properties.
		String s = PropertiesUtil.getIgnoreCase(dbDataSource.arguments,
			"recheck");
		if (s == null)
			s = PropertiesUtil.getIgnoreCase(props, "recheck");
		if (s != null)
		{
			try
			{
				int i = Integer.parseInt(s);
				recheckPeriod = (long)i * 1000L;
			}
			catch(NumberFormatException ex)
			{
				Logger.instance().log(Logger.E_FAILURE, 
					"Group '" + dbDataSource.getName()
					+ "' Invalid recheck period '" + s 
					+ "', using default of 15 min.");
				recheckPeriod = 15 * 60 * 1000L;
			}
		}
		activeMember = null;
		findActiveMember();
		Logger.instance().log(Logger.E_DEBUG1, 
			"HotBackupGroup.init() for '" + dbDataSource.getName() 
			+ "' since='" + since + "' until= '" + until + "', recheck=" 
			+ recheckPeriod);

	}

	/**
	  Find an active member to delegate to.
	  @throws DataSourceException if we can't find one.
	*/
	private void findActiveMember()
	{
		Logger.instance().debug3("HotBackupGroup.findActiveMember()"
			+ ", activeMember=" + activeMember);
		lastInitAttempt = System.currentTimeMillis();

		// If this is NOT the first time, adjust 'since' to start just after
		// the last good message we received.
		if (lastTimeStamp != null)
		{
			long millis = lastTimeStamp.getTime();
			millis += 10000L;
			Date d = new Date(millis);
			since = IDateFormat.toString(d, false);
		}

		// Go through group members - find the first one we can init.
		for(Iterator it = delegates.iterator(); it.hasNext(); )
		{
			DataSourceExec ds = (DataSourceExec)it.next();

			/*
			  If I already have an activeMember, break out of this loop when
			  I get to its place in the list. I only want to switch to higher
			  priority members.
			*/
			if (activeMember == ds)
				return;
			try
			{
				Logger.instance().debug3("HotBackupGroup init new active member " + ds);
				ds.setRoutingSpecThread(this.routingSpecThread);
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
				
				// Failsafe code: If ALL deligates are in error, don't
				// impose the timeout period.
				for(Iterator it = delegates.iterator(); it.hasNext(); )
				{
					DataSourceExec ds = (DataSourceExec)it.next();
					if (ds instanceof LrgsDataSource)
						((LrgsDataSource)ds).resetLastError();
				}

				try { Thread.sleep(2000L); }
				catch(InterruptedException ie) {};
			}
		}
		else if (delegates.size() == 0
		      || activeMember != (DataSourceExec)delegates.elementAt(0)
		         && now - lastInitAttempt >= recheckPeriod)
		{
			Logger.instance().log(Logger.E_DEBUG1, 
				"Group '" + dbDataSource.getName() + 
				"' Rechecking higher-priority group members.");

			// If activeMember is not the first in the list, recheck
			// periodically to see if we can connect to the primary.
			findActiveMember();
		}

		//
		// If I have a data source attempt to read a message from it.
		//
		if (activeMember != null)
		{
			try
			{
				RawMessage ret = activeMember.getRawMessage();
				if (ret == null)
					return null;
				lastTimeStamp = ret.getTimeStamp();
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
				if (activeMember != null)
					Logger.instance().log(Logger.E_WARNING, "DataSource '" + activeMember.getName()
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

