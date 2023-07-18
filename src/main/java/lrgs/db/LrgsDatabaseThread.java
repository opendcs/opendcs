/*
*  $Id$
*
*  This is open-source software written by ILEX Engineering, Inc., under
*  contract to the federal government. You are free to copy and use this
*  source code for your own purposes, except that no part of the information
*  contained in this file may be claimed to be proprietary.
*
*  Except for specific contractual terms between ILEX and the federal 
*  government, this source code is provided completely without warranty.
*  For more information contact: info@ilexeng.com
*/
package lrgs.db;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.TreeSet;

import org.opendcs.authentication.AuthSourceService;

import java.util.Properties;

import ilex.util.AuthException;
import ilex.util.Logger;
import lrgs.lrgsmain.LrgsConfig;

/**
This class provides synchronized access to the LRGS database for all
LRGS modules. It performs the following functions:
<ul>
  <li>Connects (and reconnects) to LRGS database as needed</li>
  <li>Fields database exceptions and takes appropriate action</li>
  <li>Maintains a local cache for certain types of database objects</li>
  <li>Works wether or not an actual SQL database is in use on this LRGS</li>
  <li>Provides queues and a separate thread to do all database I/O</li>
</ul>
*/
public class LrgsDatabaseThread
	extends Thread
{
	public static final String module = "LrgsDb";

	/** Event num meaning Can't connect. */
	public static final int EVT_CANT_CONNECT = 1;
	/** Event num meaning Database Error */
	public static final int EVT_DB_ERROR = 2;
	/** Event num meaning queue sync error (should never happen) */
	public static final int EVT_Q_ERROR = 3;

	/** Max number of outages that can be stored. */
	public static final int MAX_NUM_OUTAGES = 500;
	private ArrayList<DataSource> dataSources;
	private TreeSet<Outage> outages;
	private boolean _firstConnect;
	private boolean _shutdown;
	private LinkedList<LrgsDatabaseObject> dbWriteQueue;
	private LinkedList<LrgsDatabaseObject> dbDeleteQueue;
	private LrgsDatabase lrgsDb;
	private long dbConfigChanged;
	private long lastConnectAttempt;
	private int nextOutageId;

	private static LrgsDatabaseThread _instance = null;

	public static LrgsDatabaseThread instance()
	{
		if (_instance == null)
			_instance = new LrgsDatabaseThread();
		return _instance;
	}

	/**
	 * Constructor. 
	 */
	private LrgsDatabaseThread()
	{
		super();
		setDaemon(true);
		setName("LrgsDatabaseThread");
		_firstConnect = true;
		dataSources = new ArrayList<DataSource>();
		dbWriteQueue = new LinkedList<LrgsDatabaseObject>();
		dbDeleteQueue = new LinkedList<LrgsDatabaseObject>();
		outages = new TreeSet<Outage>();
		_shutdown = false;
		lrgsDb = null;
		dbConfigChanged = 0L;
		lastConnectAttempt = 0L;
		nextOutageId = 1;
	}

	/** Called to shut down the thread. */
	public void shutdown()
	{
		_shutdown = true;
	}

	/** The thread run method */
	public void run()
	{
		while(!_shutdown)
		{
			checkConnection();
			checkQueue();
			try { sleep(1000L); } catch(InterruptedException ex) {}
		}
	}

	/**
	 * Checks the configuration and the current database connection.
	 */
	private void checkConnection()
	{
		// Has anything changed in the configuration?
		LrgsConfig cfg = LrgsConfig.instance();
		if (cfg.getLastLoadTime() > dbConfigChanged)
		{
			dbConfigChanged = System.currentTimeMillis();

			if (cfg.dbUrl == null || cfg.dbUrl.trim().length() == 0)
			{
				if (lrgsDb != null)
				{
					info("dbUrl set to null -- closing database connection.");
					lrgsDb.closeConnection();
				}
				lrgsDb = null;
				return;
			}

			// Am I already connected to the correct database?
			if (lrgsDb != null 
			 && cfg.dbUrl.equals(lrgsDb.getDbUrl())
			 && lrgsDb.isConnected())
			{
				return;
			}

			// If I already have a DB, close it & connect to different URL.
			if (lrgsDb != null)
				lrgsDb.closeConnection();
			else // Create database object.
				lrgsDb = new LrgsDatabase();

		}

		// If we're supposed to be connected but aren't try again.
		if (lrgsDb != null && !lrgsDb.isConnected()
		 && System.currentTimeMillis() - lastConnectAttempt > 60000L)
			attemptConnect();
	}

	private synchronized void attemptConnect()
	{
		lastConnectAttempt = System.currentTimeMillis();

		String authFileName = "$LRGSHOME/.lrgsdb.auth";
		try 
		{
			Properties credentials = AuthSourceService.getFromString(authFileName)
										   .getCredentials();

			String username = credentials.getProperty("username");
			info("Attempting connection to db at '"
				+ LrgsConfig.instance().dbUrl + "' as user '" + username + "'");

			lrgsDb.connect(credentials);
			info("Successful database connection");

			if (_firstConnect)
			{
				debug("Reading data sources and outages.");
				dataSources.clear();
				for(DataSource ds : lrgsDb.getDataSources(true))
					dataSources.add(ds);

				Date now = new Date();
				Date weekAgo = new Date(now.getTime() - 7 * 24 * 3600 * 1000L);
				ArrayList<Outage> ol = lrgsDb.getOutages(null, null);
				for(Outage otg : ol)
				{
					if (otg.getBeginTime().compareTo(weekAgo) < 0)
					{
						lrgsDb.deleteOutage(otg);
					}
					else
					{
						// Don't allow open-ended historical outages.
						if (otg.getEndTime() == null)
						{
							otg.setEndTime(now);
						}

						int id = otg.getOutageId();
						if (id >= nextOutageId)
						{
							nextOutageId = id+1;
						}
						outages.add(otg);
					}
				}
				_firstConnect = false;
			}
		}
		catch(AuthException ex)
		{
			String msg = module + " Cannot read DB auth from configuration '"
				+ authFileName;
			throw new RuntimeException(msg,ex);
		}
		catch(LrgsDatabaseException ex)
		{
			alarm(EVT_CANT_CONNECT, " Cannot connect to database: " + ex);
		}
	}

	public DataSource getDataSource(int id)
	{
		for(DataSource ds : dataSources)
			if (ds.getDataSourceId() == id)
				return ds;
		return null;
	}

	/**
	 * Given a type and a name, return the numeric data source ID.
	 * If none previously exists, allocate one.
	 * @param dsType the data source type
	 * @param dsName the name for this instance (e.g. hostname)
	 */
	public synchronized int getDataSourceId(String dsType, String dsName)
	{
		for(int i=0; i<dataSources.size(); i++)
		{
			DataSource ds = dataSources.get(i);
			if (dsType.equalsIgnoreCase(ds.getDataSourceType())
			 && dsName.equalsIgnoreCase(ds.getDataSourceName()))
				return ds.getDataSourceId();
		}
		DataSource ds = new DataSource();
		ds.setDataSourceType(dsType);
		ds.setDataSourceName(dsName);
		ds.setDataSourceId(dataSources.size());
		try
		{
			ds.setLrgsHost(InetAddress.getLocalHost().getHostName());
		}
		catch (UnknownHostException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		dataSources.add(ds);
		info("Queuing data source " + ds.getDataSourceType()
			+ ":" + ds.getDataSourceName());
		doEnqueue(ds);
		return ds.getDataSourceId();
	}

	public synchronized void delete(LrgsDatabaseObject ldo)
	{
debug("Adding object to dbDeleteQueue.");
		dbDeleteQueue.add(ldo);
	}

	/**
	 * Synchronized method to enqueue an object for writing to database.
	 */
	public synchronized void enqueue(LrgsDatabaseObject ldo)
	{
		doEnqueue(ldo);
	}

	/**
	 * Unsynchronized internal method, called from within methods that are
	 * already synchronized.
	 */
	private void doEnqueue(LrgsDatabaseObject ldo)
	{
		if (lrgsDb != null)
		{
			for(LrgsDatabaseObject qldo : dbWriteQueue)
				if (qldo == ldo)
					return;
debug("Adding object to dbWriteQueue.");
			dbWriteQueue.add(ldo);
		}
	}

	/**
	 * Checks the queue and saves any data therein.
	 */
	private synchronized void checkQueue()
	{
		if (lrgsDb == null)
		{
//info("Clearing DB Queues.");
			dbWriteQueue.clear();
			dbDeleteQueue.clear();
			return;
		}
		try
		{
			while (!dbWriteQueue.isEmpty() && lrgsDb.isConnected())
			{
				LrgsDatabaseObject ldo = dbWriteQueue.removeFirst();
				if (ldo instanceof DataSource)
				{
					DataSource ds = (DataSource)ldo;
					debug("Writing data source " + ds.getDataSourceType()
						+ ":" + ds.getDataSourceName());
					lrgsDb.saveDataSource(ds);
				}
				//MJM OpenDCS 6.2 does not support Outage recovery
//				else if (ldo instanceof Outage)
//				{
//					Outage og = (Outage)ldo;
//					debug("Writing Outage: " + og);
//					lrgsDb.saveOutage(og);
//				}
				else if (ldo instanceof DdsConnectionStats)
				{
					DdsConnectionStats dcs = (DdsConnectionStats)ldo;
					debug("Writing DdsConnectionStats");
					lrgsDb.logDdsConn(dcs);
				}
				else if (ldo instanceof DdsPeriodStats)
				{
					DdsPeriodStats dps = (DdsPeriodStats)ldo;
					debug("Writing DdsPeriodStats");
					lrgsDb.logDdsPeriodStats(dps);
				}
			}
			while (!dbDeleteQueue.isEmpty() && lrgsDb.isConnected())
			{
				LrgsDatabaseObject ldo = dbDeleteQueue.removeFirst();
				if (ldo instanceof Outage)
				{
					debug("Deleting Outage: " + (Outage)ldo);
					lrgsDb.deleteOutage((Outage)ldo);
				}
			}
		}
		catch(LrgsDatabaseException ex)
		{
			alarm(EVT_DB_ERROR, " Database error: " + ex);
			lrgsDb.closeConnection();
		}
	}

	public synchronized void deleteDdsStatsBefore(Date t)
	{
		try
		{
			if (lrgsDb != null)
				lrgsDb.deleteDdsStatsBefore(t);
		}
		catch(LrgsDatabaseException ex)
		{
			alarm(EVT_DB_ERROR, " Database error: " + ex);
			lrgsDb.closeConnection();
		}
	}

	/**
	 * Synchronously returns a sorted array of all outages
	 * currently defined in the system. Outages are sorted
	 * by priority and begin time.
	 */
	public synchronized ArrayList<Outage> getOutages() 
	{
		//MJM OpenDCS 6.2 does not support Outage recovery

		ArrayList<Outage> ret = new ArrayList<Outage>();
//		for(Outage otg : outages)
//		{
//			if (otg.getOutageType() == LrgsConstants.damsntOutageType)
//			{
//				int id = otg.getSourceId();
//				DataSource ds = getDataSource(id);
//				if (ds != null)
//					otg.setDataSourceName(ds.getDataSourceName());
//			}
//			ret.add(otg);
//		}
//		Collections.sort(ret);
		return ret;
	}

	/**
	 * Retrieve the outages within a time range.
	 * 
	 * @param ret List of outages
	 * @param startTime specifies the time to start reading Outage records
	 * @param endTime specifies the time to stop reading Outage records
	 * 
	 * @deprecated @since 6.2
	 */
	@Deprecated /*(forRemoval = true, since = "6.2")*/
	public synchronized void 
		getOutages(ArrayList<Outage> ret, Date startTime, Date endTime)
	{
		//MJM OpenDCS 6.2 does not support Outage recovery
	}

	/**
	 * Since changing the outage status will modify its place in the sorted
	 * collection, we need to remove it, modify the status, and then put it
	 * back in.
	 * @param otg Outage definition
	 * @param newstat new status
	 * @deprecated @since 6.2
	 */
	@Deprecated /*(forRemoval = true, since = "6.2")*/
	public synchronized void changeOutageStatus(Outage otg, char newstat)
	{
		//MJM OpenDCS 6.2 does not support Outage recovery
	}

	/**
	 * Asserts an outage.
	 * @param newout the outage	 
	 * @deprecated @since 6.2
	 */
	@Deprecated /*(forRemoval = true, since = "6.2")*/
	public synchronized void assertOutage(Outage newout)
	{
		//MJM OpenDCS 6.2 does not support Outage recovery
	}

	/**
	 * Returns the highest priority outage in the queue, or null if
	 * queue is empty. This method does not remove the item from the queue.
	 * If the item is subsequently modified, you should later call the
	 * assertOutage method once again so that the new value is re-sorted.
	 * @return the highest priority outage in the queue, or null if
	 * queue is empty.
	 * @deprecated @since 6.2
	 */
	@Deprecated /*(forRemoval = true, since = "6.2")*/
	public synchronized Outage highestPriorityOutage()
	{
		//MJM OpenDCS 6.2 does not support Outage recovery
		return null;
	}

	/**
	 * Deletes an outage from the queue.
	 * @param otg the outage
	 * @deprecated @since 6.2
	 */
	@Deprecated /*(forRemoval = true, since = "6.2")*/
	public synchronized void deleteOutage(Outage otg)
	{
		//MJM OpenDCS 6.2 does not support Outage recovery
	}

	/**
	 * Returns an outage by its unique ID number.
	 * @param id the unique ID number
	 * @return an outage by its unique ID number, or null if not found.
	 * @deprecated @since 6.2
	 */
	@Deprecated /*(forRemoval = true, since = "6.2")*/
	public synchronized Outage getOutageById(int id)
	{
		//MJM OpenDCS 6.2 does not support Outage recovery
		return null;
	}

	public synchronized DdsPeriodStats getPeriodStats(Date perStart)
	{
		if (lrgsDb != null && lrgsDb.isConnected())
		{
			try
			{
				List<DdsPeriodStats> ldps = 
					lrgsDb.getPeriodStats(perStart, null); 
				for(DdsPeriodStats dps : ldps)
					if (dps.getStartTime().equals(perStart))
						return dps;
			}
			catch(LrgsDatabaseException ex)
			{
				Logger.instance().warning("Cannot read period stats: " + ex);
				lrgsDb.closeConnection();
			}
		}

		// Either no database or no match for period.
		DdsPeriodStats ret = new DdsPeriodStats();
		ret.setStartTime(perStart);
		return ret;
	}

	public synchronized void terminateConnectionsBefore(long lastRunTime)
	{
		if (lrgsDb != null)
		{
			try { lrgsDb.terminateConnection(new Date(lastRunTime)); }
			catch(LrgsDatabaseException ex)
			{
				Logger.instance().warning("Cannot terminate connections: "
					+ ex);
				lrgsDb.closeConnection();
			}
		}
	}

	private void info(String msg)
	{
		Logger.instance().info(module + " " + msg);
	}

	private void debug(String msg)
	{
		Logger.instance().debug1(module + " " + msg);
	}

	private void alarm(int code, String msg)
	{
		Logger.instance().failure(module + ":" + code + " " + msg);
	}

	private void failure(String msg)
	{
		Logger.instance().failure(module + " " + msg);
	}

	public LrgsDatabase getLrgsDb()
	{
		return lrgsDb;
	}

}
