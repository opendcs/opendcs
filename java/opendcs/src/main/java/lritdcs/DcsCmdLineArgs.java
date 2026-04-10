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
package lritdcs;

import java.io.IOException;
import java.io.File;
import java.util.TimeZone;

import org.opendcs.utils.logging.OpenDcsLoggerFactory;
import org.slf4j.Logger;

import ilex.cmdline.*;

public class DcsCmdLineArgs extends ApplicationSettings
{
	private static final Logger log = OpenDcsLoggerFactory.getLogger();
    // Public strings set by command line options:
	private IntegerToken debuglevel_arg;
	private StringToken log_arg;
	private String progname;
	private StringToken state_arg;

	public DcsCmdLineArgs(String progname)
	{
		super();
		this.progname = progname;

		debuglevel_arg = new IntegerToken("d", "debug-level", "",
			TokenOptions.optSwitch, 0);
		addToken(debuglevel_arg);

		String logname = LritDcsConfig.instance().getLritDcsHome()
			+ File.separator + "lritdcs.log";

		log_arg = new StringToken(
			"l", "log-file", "", TokenOptions.optSwitch, logname);

		addToken(log_arg);

		state_arg = new StringToken(
				"s", "LRIT-State", "", TokenOptions.optSwitch, "last");
		addToken(state_arg);
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

	public void parseArgs(String args[])
	{
		super.parseArgs(args);

		//checks if the LRIT is started in active or dormant state
		String lritState = getLritState();
		LritDcsMain.instance().setLritStartState(lritState);
		log.info("LRIT Process '{}' Starting.....", progname);
	}

	public String  getLritState() {

		return state_arg.getValue();
	}

}