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
package lrgs.lddc;

import org.opendcs.utils.logging.OpenDcsLoggerFactory;
import org.slf4j.Logger;

import ilex.cmdline.*;
import lrgs.ldds.LddsClient;
import lrgs.ldds.LddsParams;

/**
This was originally written as a test client program but it has since found
use as a command-line utility for pulling data from a DDS server.
Command line arguments are used to specify the server paramters, search
criteria, and various formatting options.
*/
public class GetEvents extends Thread
{
	private static final Logger log = OpenDcsLoggerFactory.getLogger();
	private String user;
	private LddsClient lddsClient;

	/**
	 * Constructor.
	 * @param host host name to connect to
	 * @param port port number
	 * @param user user name
	 */
	public GetEvents(String host, int port, String user)
		throws Exception
	{
		this.user = user;
		lddsClient = new LddsClient(host, port);
	}

	/** Thread run method. */
	public void run()
	{
		try
		{
			// Connect & login as specified user.
			lddsClient.connect();
			lddsClient.sendHello(user);
		}
		catch(Exception ex)
		{
			log.atError().setCause(ex).log("Cannot initialize.");
			//e.printStackTrace(System.err);
			return;
		}

		// Continue to receive events
		while (true)
		{
			try
			{
				String[] events = lddsClient.getEvents();
				if (events.length == 0)
				{
					try { Thread.sleep(1000L); } catch(InterruptedException ex) {}
				}
				for(int i=0; events != null && i<events.length; i++)
					System.out.println(events[i]);
			}
			catch(Exception ex)
			{
				log.atError().setCause(ex).log("Error retrieving events.");
			}
		}
	}


	// ========================= main ====================================
	static ApplicationSettings settings = new ApplicationSettings();
	static StringToken hostArg = new StringToken(
		"h", "Host", "", TokenOptions.optSwitch, "localhost");
	static IntegerToken portArg= new IntegerToken(
		"p", "Port number", "", TokenOptions.optSwitch, LddsParams.DefaultPort);
	static StringToken userArg = new StringToken(
		"u", "User", "", TokenOptions.optSwitch | TokenOptions.optRequired, "");
	static IntegerToken debugArg = new IntegerToken(
		"d", "debug level", "", TokenOptions.optSwitch, 0);
	static StringToken logArg = new StringToken(
		"l", "log-file name", "", TokenOptions.optSwitch, "");

	static
	{
		settings.addToken(hostArg);
		settings.addToken(portArg);
		settings.addToken(userArg );
		settings.addToken(debugArg);
		settings.addToken(logArg);
	}

	/**
	  Main method.
	*/
	public static void main(String args[])
	{
		try
		{
			settings.parseArgs(args);

			GetEvents gdm = new GetEvents(
				hostArg.getValue(), portArg.getValue(), userArg.getValue());

			gdm.start();
		}
		catch(Exception ex)
		{
			log.atError().setCause(ex).log("Unable to retrieve events.");
		}
	}
}