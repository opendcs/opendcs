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
*  
*  $Log$
*  Revision 1.7  2016/06/27 15:26:37  mmaloney
*  Have to read data types as part of decodes init.
*
*  Revision 1.6  2016/04/22 14:38:40  mmaloney
*  Skip resolving and saving results if the tasklist set is empty.
*
*  Revision 1.5  2016/03/24 19:09:18  mmaloney
*  Added instance() method needed by Python Algorithm.
*
*  Revision 1.4  2015/04/02 18:16:19  mmaloney
*  Added property definitions.
*
*  Revision 1.3  2014/08/22 17:23:04  mmaloney
*  6.1 Schema Mods and Initial DCP Monitor Implementation
*
*  Revision 1.2  2014/07/10 17:07:54  mmaloney
*  Remove startup log from ComputationApp, and add to TsdbAppTemplate.
*
*  Revision 1.1.1.1  2014/05/19 15:28:59  mmaloney
*  OPENDCS 6.0 Initial Checkin
*
*  Revision 1.28  2013/07/12 11:50:53  mmaloney
*  Added tasklist queue stuff.
*
*  Revision 1.27  2013/07/09 19:01:24  mmaloney
*  If database goes away and reconnection is done, also recreate the resolver.
*
*  Revision 1.26  2013/03/28 19:07:24  mmaloney
*  Implement cmd line arg -O OfficeID
*
*  Revision 1.25  2013/03/25 18:15:03  mmaloney
*  Refactor starting event server.
*
*  Revision 1.24  2013/03/25 17:08:43  mmaloney
*  event port fix
*
*  Revision 1.23  2013/03/25 16:58:26  mmaloney
*  Refactor comp lock stale time.
*
*  Revision 1.22  2013/03/21 18:27:39  mmaloney
*  DbKey Implementation
*
*/
package decodes.tsdb;

import java.io.IOException;
import java.util.List;
import java.net.InetAddress;

import opendcs.dai.LoadingAppDAI;
import opendcs.dai.TimeSeriesDAI;
import lrgs.gui.DecodesInterface;
import ilex.cmdline.BooleanToken;
import ilex.cmdline.StringToken;
import ilex.cmdline.TokenOptions;
import ilex.util.Logger;
import ilex.util.TextUtil;
import decodes.sql.DbKey;
import decodes.util.CmdLineArgs;
import decodes.util.DecodesException;
import decodes.util.DecodesSettings;
import decodes.util.PropertySpec;

/**
ComputationApp is the main module for the background comp processor.
*/
public class ComputationApp
	extends TsdbAppTemplate
{
	/** Holds app name, id, & description. */
	CompAppInfo appInfo;

	/** My lock */
	private TsdbCompLock myLock;
	
	/** My resolver */
	private DbCompResolver resolver;
	
	private boolean shutdownFlag;

	private String hostname;
	private int compsTried = 0;
	private int compErrors = 0;
	private int evtPort = -1;
	
	private BooleanToken regressionTestModeArg = new BooleanToken("T", "Regression Test Mode",
		"", TokenOptions.optSwitch, false);
	private StringToken officeIdArg = new StringToken(
		"O", "OfficeID", "", TokenOptions.optSwitch, "");
	private CompEventSvr compEventSvr = null;
	
	private static ComputationApp _instance = null;
	public static ComputationApp instance() { return _instance; }
	
	private PropertySpec[] myProps =
	{
		new PropertySpec("monitor", PropertySpec.BOOLEAN,
			"Set to true to allow monitoring from the GUI."),
		new PropertySpec("EventPort", PropertySpec.INT,
			"Open listening socket on this port to serve out app events.")
	};

	
	/**
	 * Constructor called from main method after parsing arguments.
	 */
	public ComputationApp()
	{
		super("compproc.log");
		myLock = null;
		resolver = null;
		shutdownFlag = false;
	}

	/** @return the application ID. */
	public DbKey getAppId() { return appId; }

	protected void addCustomArgs(CmdLineArgs cmdLineArgs)
	{
		cmdLineArgs.addToken(regressionTestModeArg);
		appNameArg.setDefaultValue("compproc");
		cmdLineArgs.addToken(officeIdArg);
	}
	
	@Override
	protected void oneTimeInit()
	{
		// Comp Proc can survive DB going down.
		surviveDatabaseBounce = true;
		
		try { hostname = InetAddress.getLocalHost().getHostName(); }
		catch(Exception e) { hostname = "unknown"; }

		if (officeIdArg.getValue() != null && officeIdArg.getValue().length() > 0)
			DecodesSettings.instance().CwmsOfficeId = officeIdArg.getValue();
	}


	/**
	 * Sets the application ID. 
	 * @param id the ID.
	 */
	public void setAppId(DbKey id) { this.appId = id; }

	/** @return the application name. */
	public String getAppName() 
	{
		return appInfo.getAppName(); 
	}

	/** @return the application comment. */
	public String getAppComment() 
	{
		return appInfo.getComment(); 
	}

	/**
	 * The application run method. Called after all initialization methods
	 * by the base class.
	 * @throws LockBusyException if another process has the lock
	 * @throws DbIoException on failure to access the database
	 * @throws NoSuchObjectException if the application is invalid.
	 */
	public void runApp( )
		throws LockBusyException, DbIoException, NoSuchObjectException
	{
		Logger.instance().debug1("runApp starting");
		_instance = this;
		shutdownFlag = false;
		runAppInit();
		Logger.instance().debug1("runAppInit done, shutdownFlag=" + shutdownFlag 
			+ ", surviveDatabaseBounce=" + surviveDatabaseBounce);

		long lastDataTime = System.currentTimeMillis();
		long lastLockCheck = 0L;

		while(!shutdownFlag)
		{
			String action="";
			TimeSeriesDAI timeSeriesDAO = theDb.makeTimeSeriesDAO();
			LoadingAppDAI loadingAppDAO = theDb.makeLoadingAppDAO();
			try
			{
				long now = System.currentTimeMillis();

				action = "Checking lock";
				if (myLock != null && now - lastLockCheck > 5000L)
				{
					setAppStatus("Cmps: " + compsTried + "/" + compErrors);
					loadingAppDAO.checkCompProcLock(myLock);
					lastLockCheck = now;
				}
				
				action = "Getting new data";
				DataCollection data = theDb.getNewData(appId);
				
				// In Regression Test Mode, exit after 5 sec of idle
				if (!data.isEmpty())
					lastDataTime = System.currentTimeMillis();
				else if (regressionTestModeArg.getValue()
				 && System.currentTimeMillis() - lastDataTime > 10000L)
				{
					Logger.instance().info("Regression Test Mode - Exiting after 10 sec idle.");
					shutdownFlag = true;
					loadingAppDAO.releaseCompProcLock(myLock);
				}

				if (!data.isEmpty())
				{
					action = "Resolving computations";
					DbComputation comps[] = resolver.resolve(data);
	
					action = "Applying computations";
					for(DbComputation comp : comps)
					{
						Logger.instance().debug1("Trying computation '" 
							+ comp.getName() + "' #trigs=" + comp.getTriggeringRecNums().size());
						compsTried++;
						try
						{
							comp.prepareForExec(theDb);
							comp.apply(data, theDb);
						}
						catch(NoSuchObjectException ex)
						{
							compErrors++;
							warning("Computation '" + comp.getName()
								+ "removed from DB: " + ex);
						}
						catch(DbCompException ex)
						{
							String msg = "Computation '" + comp.getName() 
								+ "' DbCompException: " + ex;
							warning(msg);
							compErrors++;
							for(Integer rn : comp.getTriggeringRecNums())
								 data.getTasklistHandle().markComputationFailed(rn);
						}
						catch(Exception ex)
						{
							compErrors++;
							String msg = "Computation '" + comp.getName() 
								+ "' Exception: " + ex;
							warning(msg);
							System.err.println(msg);
							ex.printStackTrace(System.err);
							for(Integer rn : comp.getTriggeringRecNums())
								 data.getTasklistHandle().markComputationFailed(rn);
						}
						comp.getTriggeringRecNums().clear();
						Logger.instance().debug1("End of computation '" 
							+ comp.getName() + "'");
					}
	
					action = "Saving results";
					List<CTimeSeries> tsList = data.getAllTimeSeries();
	Logger.instance().debug3(action + " " + tsList.size() +" time series in data.");
					for(CTimeSeries ts : tsList)
					{
						try { timeSeriesDAO.saveTimeSeries(ts); }
						catch(BadTimeSeriesException ex)
						{
							warning("Cannot save time series " + ts.getNameString()
								+ ": " + ex);
						}
					}
	
					action = "Releasing new data";
					theDb.releaseNewData(data);
					lastDataTime = System.currentTimeMillis();
				}
			}
			catch(LockBusyException ex)
			{
				Logger.instance().fatal("No Lock - Application exiting: " + ex);
				shutdownFlag = true;
			}
			catch(DbIoException ex)
			{
				warning("Database Error while " + action + ": " + ex);
				shutdownFlag = true;
				databaseFailed = true;
			}
			catch(Exception ex)
			{
				String msg = "Unexpected exception while " + action + ": " + ex;
				warning(msg);
				System.err.println(msg);
				ex.printStackTrace(System.err);
				shutdownFlag = true;
			}
			finally
			{
				timeSeriesDAO.close();
				loadingAppDAO.close();
			}
			try { Thread.sleep(1000L); }
			catch(InterruptedException ex) {}
		}
		resolver = null;
		Logger.instance().debug1("runApp() exiting.");
	}

	/**
	 * Called at the start of the runApp() method, which is called by the base
	 * class after connecting to the database.
	 * @throws DbIoException
	 * @throws NoSuchObjectException
	 */
	private void runAppInit()
	{
		LoadingAppDAI loadingAppDao = theDb.makeLoadingAppDAO();
		try
		{
			appInfo = loadingAppDao.getComputationApp(appId);

			// Construct the resolver & load it.
			resolver = new DbCompResolver(theDb);
			
			// If this process can be monitored, start an Event Server.
			if (TextUtil.str2boolean(appInfo.getProperty("monitor")) && compEventSvr == null)
			{
				try 
				{
					compEventSvr = new CompEventSvr(determineEventPort(appInfo));
					compEventSvr.startup();
				}
				catch(IOException ex)
				{
					failure("Cannot create Event server: " + ex
						+ " -- no events available to external clients.");
				}
			}

			try { myLock = loadingAppDao.obtainCompProcLock(appInfo, getPID(), hostname); }
			catch(LockBusyException ex)
			{
				shutdownFlag = true;
				Logger.instance().fatal(getAppName() + " runAppInit: lock busy: " + ex);
			}
		}
		catch(NoSuchObjectException ex)
		{
			// This means a bad app name was given on the command line. Exit.
			Logger.instance().fatal(getAppName() + " runAppInit: " + ex);
			shutdownFlag = true;
			return;
		}
		catch(DbIoException ex)
		{
			Logger.instance().fatal("App Name " + getAppName() + " error in runAppInit(): " + ex);
			shutdownFlag = true;
			databaseFailed = true;
		}
		finally
		{
			loadingAppDao.close();
		}
	}
	
	public void initDecodes()
		throws DecodesException
	{
		DecodesInterface.silent = true;
		if (DecodesInterface.isInitialized())
			return;
		DecodesInterface.initDecodesMinimal(cmdLineArgs.getPropertiesFile());
		decodes.db.Database.getDb().dataTypeSet.read();
	}

	/**
	 * The main method.
	 * @param args command line arguments.
	 */
	public static void main( String[] args )
		throws Exception
	{
		ComputationApp compApp = new ComputationApp();
		compApp.execute(args);
	}

	/**
	 * Sets the application's status string in its database lock.
	 */
	public void setAppStatus(String status)
	{
		if (myLock != null)
			myLock.setStatus(status);
	}
	
	@Override
	public PropertySpec[] getSupportedProps()
	{
		return myProps;
	}

	public DbCompResolver getResolver()
	{
		return resolver;
	}

}

