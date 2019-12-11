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
*  Revision 1.2  2019/10/13 19:29:57  mmaloney
*  dev
*
*  Revision 1.1.1.1  2014/05/19 15:28:59  mmaloney
*  OPENDCS 6.0 Initial Checkin
*
*  Revision 1.5  2013/03/21 18:27:40  mmaloney
*  DbKey Implementation
*
*  Revision 1.4  2011/01/13 13:22:14  mmaloney
*  downgrade warning message to debug1
*
*  Revision 1.3  2008/08/06 19:40:55  mjmaloney
*  dev
*
*  Revision 1.2  2008/06/10 21:39:52  cvs
*  dev
*
*  Revision 1.1  2008/04/04 18:21:07  cvs
*  Added legacy code to repository
*
*  Revision 1.5  2007/06/27 20:57:39  mmaloney
*  dev
*
*  Revision 1.4  2006/05/11 13:32:35  mmaloney
*  DataTypes are now immutable! Modified all references. Modified SQL IO code.
*
*  Revision 1.3  2006/03/28 15:37:16  mmaloney
*  dev
*
*  Revision 1.2  2006/03/17 16:38:43  mmaloney
*  dev
*
*  Revision 1.1  2006/03/17 01:54:48  mmaloney
*  dev
*
*/
package decodes.tsdb.test;

import java.io.IOException;
import java.util.Properties;

import lrgs.gui.DecodesInterface;

import ilex.cmdline.*;
import ilex.util.EnvExpander;
import ilex.util.Logger;
import ilex.util.StderrLogger;
import ilex.util.UserAuthFile;

import decodes.util.CmdLineArgs;
import decodes.util.DecodesSettings;
import decodes.tsdb.*;
import decodes.sql.DbKey;
import decodes.sql.SqlDatabaseIO;
import decodes.db.Database;

public abstract class TestProg
{
	// Static command line arguments and initialization for main method.
	private CmdLineArgs cmdLineArgs;
	protected StringToken cfgFileArg;
	protected BooleanToken testModeArg;
	protected IntegerToken modelRunArg;
	protected StringToken appNameArg;

	public TimeSeriesDb theDb;
	protected DbKey appId;

	public TestProg(String logname)
	{
		if (logname == null)
			logname = "test.log";
		cmdLineArgs = new CmdLineArgs(false, logname);
		cfgFileArg = new StringToken("c", "config-file",
			"", TokenOptions.optSwitch, "$DECODES_INSTALL_DIR/comp.conf"); 
		testModeArg = new BooleanToken("t", "test-mode",
			"", TokenOptions.optSwitch, false);
		modelRunArg = new IntegerToken("m", 
			"output-model-run-ID", "", TokenOptions.optSwitch, -1); 
		appNameArg = new StringToken("a", "App-Name", "",
			TokenOptions.optSwitch, "");
		cmdLineArgs.addToken(cfgFileArg);
		cmdLineArgs.addToken(testModeArg);
		cmdLineArgs.addToken(modelRunArg);
		cmdLineArgs.addToken(appNameArg);
	}

	/**
	 * The sub-class main method should call this.
	 */
	public void execute(String args[])
		throws Exception
	{
		addCustomArgs(cmdLineArgs);
		parseArgs(args);
		initConfig();
		createDatabase();
		initDecodes();
		runTest();
	}

	/**
	 * Override this and add any test-specific arguments.
	 */
	protected void addCustomArgs(CmdLineArgs cmdLineArgs)
	{
		// Default impl does nothing.
	}

	/**
	 * Override this with the guts of your test program.
	 */
	protected abstract void runTest()
		throws Exception;

	protected void parseArgs(String args[])
		throws Exception
	{
//		Logger.setLogger(new StderrLogger(appNameArg.getValue()));

		// Parse command line arguments.
		try { cmdLineArgs.parseArgs(args); }
		catch(IllegalArgumentException ex)
		{
			System.exit(1);
		}
	}

	// Call this method second.
	public void initConfig()
		throws Exception
	{
	}

	// Call this method third.
	public void createDatabase()
		throws Exception
	{
//		String className = DecodesSettings.instance().dbClassName;
		String className = DecodesSettings.instance().getTsdbClassName();
		String authFileName = EnvExpander.expand(DecodesSettings.instance().DbAuthFile);
		try
		{
			ClassLoader cl = Thread.currentThread().getContextClassLoader();
			Class dbClass = cl.loadClass(className);
			theDb = (TimeSeriesDb)dbClass.newInstance();
		}
		catch(Exception ex)
		{
			String msg = "Cannot create database interface for class '"
				+ className + "': " + ex;
			System.err.println(msg);
			Logger.instance().fatal(msg);
			throw ex;
		}

		// Get authorization parameters.
		UserAuthFile authFile = new UserAuthFile(authFileName);
		try { authFile.read(); }
		catch(Exception ex)
		{
			String msg = "Cannot read DB auth info from '" + authFileName + "': " + ex;
			System.err.println(msg);
			Logger.instance().fatal(msg);
			throw ex;
		}

		// Set test-mode flag & model run ID in the database interface.
		theDb.setTestMode(testModeArg.getValue());
		int modelRunId = modelRunArg.getValue();
		if (modelRunId != -1)
			theDb.setWriteModelRunId(modelRunId);

		// Connect to the database!
		Properties props = new Properties();
		props.setProperty("username", authFile.getUsername());
		props.setProperty("password", authFile.getPassword());

		TestProg app;
		try
		{
			appId = theDb.connect(appNameArg.getValue(), props);
			Logger.instance().info("Connected with appId=" + appId);
		}
		catch(BadConnectException ex)
		{
			String msg = "Cannot connect to DB: " + ex.getMessage();
			System.err.println(msg);
			Logger.instance().fatal(msg);
			throw ex;
		}
	}

	public void initDecodes()
		throws Exception
	{
		DecodesInterface.initDecodes(cmdLineArgs.getPropertiesFile());
//		DecodesInterface.initializeForEditing();

//		System.out.print("Init DECODES DB: "); System.out.flush();
//		Database decodesDb = new Database();
//		Database.setDb(decodesDb);
//		SqlDatabaseIO dbio = new SqlDatabaseIO();
//		dbio.useExternalConnection(theDb.getConnection(), 
//			theDb.getKeyGenerator(), "TSDB");
//		decodesDb.setDbIo(dbio);
//		System.out.print("Enum, "); System.out.flush();
//		decodesDb.enumList.read();
//		System.out.print("DataType, "); System.out.flush();
//		decodesDb.dataTypeSet.read();
//		System.out.print("EU, "); System.out.flush();
//		decodesDb.engineeringUnitList.read();
//		//Site.explicitList = true;
//		System.out.print("Site, "); System.out.flush();
//		decodesDb.siteList.read();
//		System.out.println();
	}

	public CmdLineArgs getCmdLineArgs()
	{
		return cmdLineArgs;
	}
}
