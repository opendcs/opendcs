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
package decodes.tsdb.locks;

import java.util.Properties;

import org.opendcs.authentication.AuthSourceService;
import org.opendcs.utils.logging.OpenDcsLoggerFactory;
import org.slf4j.Logger;

import lrgs.gui.DecodesInterface;

import ilex.cmdline.*;
import ilex.util.EnvExpander;

import decodes.util.CmdLineArgs;
import decodes.util.DecodesSettings;
import decodes.tsdb.*;
import decodes.sql.DbKey;

/**
 * @deprecated We have an integration test system setup. Such usages should be moved to there.
 */
@Deprecated
public abstract class TestProg
{
	private static final Logger log = OpenDcsLoggerFactory.getLogger();
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

		// Parse command line arguments.
		try { cmdLineArgs.parseArgs(args); }
		catch(IllegalArgumentException ex)
		{
			log.atError().setCause(ex).log("Unable to parse arguments.");
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
			throw new Exception("Cannot create database interface for class '" + className + "'", ex);
		}

		// Get authorization parameters.
		Properties props = AuthSourceService.getFromString(authFileName)
											.getCredentials();
		// Set test-mode flag & model run ID in the database interface.
		theDb.setTestMode(testModeArg.getValue());
		int modelRunId = modelRunArg.getValue();
		if (modelRunId != -1)
			theDb.setWriteModelRunId(modelRunId);

		// Connect to the database!
		appId =theDb.getAppId();
		log.info("Connected with appId={}", appId);
	}

	public void initDecodes()
		throws Exception
	{
		DecodesInterface.initDecodes(cmdLineArgs.getPropertiesFile());
	}

	public CmdLineArgs getCmdLineArgs()
	{
		return cmdLineArgs;
	}
}