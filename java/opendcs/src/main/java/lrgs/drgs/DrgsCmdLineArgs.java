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
package lrgs.drgs;

import java.io.IOException;
import ilex.cmdline.*;
import ilex.cmdline.StdAppSettings;
import ilex.util.ShellExpander;

/**
  This object parses command line arguments to the DRGS daemon.
*/
public class DrgsCmdLineArgs extends StdAppSettings
{
	private StringToken log_arg;
	private StringToken cfg_arg;
	private StringToken lock_arg;
	private StringToken procname_arg;

	/**
	  Constructor: Adds the DRGS-specific options to the StdAppSettings
	  base clase. See parseArgs method for usage.
	*/
    public DrgsCmdLineArgs()
	{
		super(false);

		// -n <procname>
		procname_arg = new StringToken(
			"n", "process-name", "", TokenOptions.optSwitch, "DrgsInput");
		addToken(procname_arg);

		// -l <logfile>
		log_arg = new StringToken(
			"l", "log-file", "", TokenOptions.optSwitch, "");
		addToken(log_arg);

		// -f <cfgfile>
		cfg_arg = new StringToken(
			"f", "cfg-file", "", TokenOptions.optSwitch, "drgsconf.xml");
		addToken(cfg_arg);

		// -k <lockfile>
		String lockfile = ShellExpander.expand("~/drgs.lock");
		lock_arg = new StringToken(
			"k", "lock-file", "", TokenOptions.optSwitch, lockfile);
		addToken(lock_arg);
    }

	/** @return the log file name */
	public String getLogFile()
	{
		String s = log_arg.getValue();
		if (s == null || s.length() == 0)
			return null;
		return s;
	}

	/** @return the config file name */
	public String getCfgFile()
	{
		return cfg_arg.getValue();
	}

	/** @return the lock file name */
	public String getLockFile()
	{
		return lock_arg.getValue();
	}

	/** @return the process name */
	public String getProcessName()
	{
		return procname_arg.getValue();
	}

	/**
	  Parses the command line arguments.
	  The super implementation does the parsing. This method then extracts
	  and validates DRGS-specific arguments.
	  <ul>
	    <li>-n procname    (Process name to use in log and LRGS connection)</li>
		<li>-l logfile     (DRGS Log file to write to)</li>
		<li>-f cfgfile     (DRGS configuration file to read)</li>
		<li>-k lockfile    (Lock file ensures only one instance)</li>
	  </ul>
	*/
	public void parseArgs(String args[])
	{
		super.parseArgs(args);
	}
}