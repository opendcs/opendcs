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
package lrgs.lrgsmain;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.TimeZone;

import org.opendcs.utils.logging.OpenDcsLoggerFactory;
import org.slf4j.Logger;

import ilex.cmdline.*;
import ilex.util.EnvExpander;

public class LrgsCmdLineArgs extends ApplicationSettings
{
	private static final Logger log = OpenDcsLoggerFactory.getLogger();
    // Public strings set by command line options:
	private IntegerToken debuglevel_arg;
	private StringToken log_arg;
	private StringToken configFile_arg;
	private IntegerToken maxLogSize_arg;
	private IntegerToken numOldLogs_arg;
	private final StringToken lockArg = new StringToken("k", "lock-file",
	"",TokenOptions.optSwitch, "$LRGSHOME/lrgs.lock");
	public final BooleanToken windowsSvcArg = new BooleanToken("w",
	"Run as Windows Service", "", TokenOptions.optSwitch, false);
	private final BooleanToken foreground = new BooleanToken("F",
		"Run in forground. Captures SigTerm Directly","",
		TokenOptions.optSwitch,false);

	public static final String progname = "lrgs";

	public LrgsCmdLineArgs()
	{
		super();

		debuglevel_arg = new IntegerToken("d", "debug-level", "",
			TokenOptions.optSwitch, 0);
		addToken(debuglevel_arg);

		log_arg = new StringToken(
			"l", "log-file", "", TokenOptions.optSwitch,
			EnvExpander.expand("$LRGSHOME/lrgslog"));
		addToken(log_arg);

		configFile_arg = new StringToken(
			"f", "config-file", "", TokenOptions.optSwitch,
			"$LRGSHOME/lrgs.conf");
		addToken(configFile_arg);

		maxLogSize_arg = new IntegerToken("S", "MaxLogSize", "",
			TokenOptions.optSwitch, 20000000);
		addToken(maxLogSize_arg);

		numOldLogs_arg = new IntegerToken("N", "NumOldLogs", "",
			TokenOptions.optSwitch, 5);
		addToken(numOldLogs_arg);
		addToken(lockArg);
		addToken(windowsSvcArg);
		addToken(foreground);

	}

    /**
     * Returns the numeric debug-level specified on the command line, or
     * 0 if none was specified.
     */
	public int getDebugLevel()
	{
		return debuglevel_arg.getValue();
	}

	public String getLogFile()
	{
		return log_arg.getValue();
	}

	/**
	  Parses command line arguments, sets internal variables, & sets up logging.
	  We will use a TeeLogger to fork log messages to a file and to an internal
	  QueueLogger. The Queue will be used by DDS clients who want to retrieve
	  log messages.
	  @param args command line arguments from main().
	*/
	public void parseArgs(String args[])
	{
		super.parseArgs(args);

		log.info("========== Process '{}' Starting ==========", progname);
	}

	public String getConfigFile()
	{
		return configFile_arg.getValue();
	}

	public String getLockFile()
	{
		return lockArg.getValue();
	}

	public boolean runInForGround() {
		return this.foreground.getValue();
	}
}
