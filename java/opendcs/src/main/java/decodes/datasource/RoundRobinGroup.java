/*
* Where Applicable, Copyright 2025 OpenDCS Consortium and/or its contributors
*
* Licensed under the Apache License, Version 2.0 (the "License"); you may not
* use this file except in compliance with the License. You may obtain a copy
* of the License at
*
*   http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
* WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
* License for the specific language governing permissions and limitations
* under the License.
*/
package decodes.datasource;

import java.util.Properties;
import java.util.Vector;

import org.opendcs.utils.logging.OpenDcsLoggerFactory;
import org.slf4j.Logger;

import java.util.Iterator;
import java.util.Date;

import decodes.db.DataSource;
import decodes.db.Database;
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
public class RoundRobinGroup extends DataSourceExec
{
	private static final Logger log = OpenDcsLoggerFactory.getLogger();
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

	/**
	 * @see decodes.datasource.DataSourceExec#DataSourceExec(DataSource, Database) DataSourceExec Constructor
	 *
	 * @param dataSource
	 * @param decodesDatabase
	 */
	public RoundRobinGroup(DataSource ds, Database db)
	{
		super(ds,db);

		props = null;
		since = null;
		until = null;
		networkLists = null;
		activeMember = null;
		lastInitAttempt = 0L;
		lastTimeStamp = null;
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
		log.debug("Initializing RoundRobinGroup for '{}', args='{}'",
		          dbDataSource.getName(), dbDataSource.getDataSourceArg());

		// This is a data-source-group, call prepareForExec() for all members
		for(Iterator it = dbDataSource.groupMembers.iterator(); it.hasNext(); )
		{
			DataSource ds = (DataSource)it.next();
			if (ds != null)  // there may be empty slots.
			{
				log.debug("Making delegate for '{}'", ds.getName());
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
		log.debug("RoundRobinGroup.init() for '{}' since='{}' until='{}", dbDataSource.getName(), since, until);
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
			catch(DataSourceException ex)
			{
				log.atWarn()
				   .setCause(ex)
				   .log("Could not initialize data source '{}'", ds.dbDataSource.getName());
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
	@Override
	protected RawMessage getSourceRawMessage()
		throws DataSourceException
	{
		long now = System.currentTimeMillis();

		//
		// First sort out which data source I'm going to use...
		//
		if (activeMember == null && now - lastInitAttempt >= initPeriod)
		{
			// No active member. Periodically attempt to get one.
			log.debug("Group '{}' Attempting to find a group member.", dbDataSource.getName());

			findActiveMember();

			// If no success, pause a couple seconds.
			if (activeMember == null)
			{
				log.error("No data sources in group '{}' are available now. Will retry later.",
						  dbDataSource.getName());
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
				if ( ret == null )
					close();
				return ret;
			}
			catch(DataSourceEndException e)
			{
				log.info("DataSource '{}' End of Data Stream.", activeMember.getName());
				close();
				throw e;
			}
			catch(UnknownPlatformException upex)
			{
				throw upex;
			}
			catch(DataSourceException ex)
			{
				log.atWarn().setCause(ex).log("DataSource '{}' failed.", activeMember.getName());
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
			log.debug("Aborting active LRGS member.");
			activeMember.abortGetRawMessage();
		}
		else
			log.debug("No currently active data source member.");
	}
}
