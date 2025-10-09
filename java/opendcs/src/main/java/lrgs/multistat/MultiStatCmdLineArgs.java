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
package lrgs.multistat;

import java.text.SimpleDateFormat;
import java.util.TimeZone;

import ilex.cmdline.*;

public class MultiStatCmdLineArgs extends StdAppSettings
{
	private StringToken configFileArg = new StringToken(
		"f", "config-file", "", TokenOptions.optSwitch, "$DCSTOOL_HOME/multistat.conf");

    public MultiStatCmdLineArgs()
	{
		super(false);
		addToken(configFileArg);
    }

	/**
	  Parses command line arguments.
	*/
	public void parseArgs(String args[])
	{
		try { super.parseArgs(args); }
		catch (IllegalArgumentException ex)
		{
			System.err.println("Illegal arguments ... program exiting.");
			System.exit(1);
		}
	}

	public String getConfigFileName()
	{
		return configFileArg.getValue();
	}
}
