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

import java.io.*;

import org.opendcs.utils.logging.OpenDcsLoggerFactory;
import org.slf4j.Logger;

import ilex.net.*;
import ilex.cmdline.*;
import lrgs.common.*;
import lrgs.ldds.LddsParams;

public class UsbrDrotFeed extends GetDcpMessages
{
	private static final Logger log = OpenDcsLoggerFactory.getLogger();
	private String drotFeedSvr;
	private int drotFeedPort;
	private BasicClient drotFeed;
	public static final int pauseTime = 10000; // 10 sec.
	private boolean firstConnect = true;

	UsbrDrotFeed(String ddsHost, int ddsPort, String ddsUser, String crit,
		String usbrHost, int usbrPort)
		throws Exception
	{
		// no before/after strings, no verbose, no newline.
		super(ddsHost, ddsPort, false, ddsUser, crit, false, "", "", false, false);
		timeout = 3600;
		setSingleMode(true);
		log.info("Constructing client to {}:{}", usbrHost, usbrPort);
		drotFeed = new BasicClient(usbrHost, usbrPort);
		String passwd = passwordArg.getValue();
		if (passwd != null && passwd.length() > 0)
			setPassword(passwd);
	}

	/**
	  The program should never exit. If the super.run() method exits,
	  it means that the DDS session has failed. We just pause here &
	  retry later.
	*/
	public void run()
	{
		while(true)
		{
			super.run();
			log.warn("DDS session failed, pause before retry...");
			try { sleep(pauseTime); }
			catch(InterruptedException ex) {}
		}
	}

	protected void waitForAllClear()
	{
		while(true)
		{
			checkConnection();
			try
			{
				InputStream is = drotFeed.getInputStream();
				int c;
				while((c = is.read()) != 0);
				return;
			}
			catch(IOException ex)
			{
				log.atWarn()
				   .setCause(ex)
				   .log("Failed to get all-clear from server at {}:{}", drotFeed.getHost(), drotFeed.getPort());
				drotFeed.disconnect();
			}
		}
	}

	protected void checkConnection()
	{
		while(!drotFeed.isConnected())
		{
			if (!firstConnect)
			{
				log.warn("Pausing before attempt to reconnect to USBR server at {}:{}",
						 drotFeed.getHost(), drotFeed.getPort());
				try { sleep(10000L); }
				catch(InterruptedException  ex) {}
			}
			firstConnect = false;
			log.trace("Trying connect()");
			try { drotFeed.connect(); }
			catch(IOException ex)
			{
				log.atWarn()
				   .setCause(ex)
				   .log("Error connecting to server at {}:{}", drotFeed.getHost(), drotFeed.getPort());
			}
		}
	}

	protected void outputMessage(DcpMsg msg)
	{
		log.debug("UsbrDrotFeed.outputMessage");
		waitForAllClear();

		try
		{
			drotFeed.sendData(msg.getData());
			drotFeed.getOutputStream().flush();
		}
		catch(IOException ex)
		{
			log.atWarn()
			   .setCause(ex)
			   .log("Error on server at {}:{}", drotFeed.getHost(), drotFeed.getPort());
			drotFeed.disconnect();
		}
	}

	// ========================= main ====================================
	/**
	  Usage: UsbrDrotFeed -p port -h host -u user -f searchcrit

		... where:
			-p port defaults to 16003.
			-h host defaults to localhost.
			-u user is required.
			-f searchcrit specifies a search criteria file to be downloaded
			   before starting the transfer.
			-t timeout    # seconds to wait for response from server
			-d 0123       set debug level to 0, 1, 2, or 3
			-l logfile    Set name of debug log file (default=stderr)
			-H USBRHost  Host to receive the data.
			-P USBRPoret Port number of USBR service
	*/
	static ApplicationSettings usbrSettings = new ApplicationSettings();
	static StringToken ddsHostArg = new StringToken(
		"h", "DDSHost", "", TokenOptions.optSwitch, "localhost");
	static IntegerToken ddsPortArg= new IntegerToken(
		"p", "DDSPort", "", TokenOptions.optSwitch, LddsParams.DefaultPort);
	static StringToken ddsUserArg = new StringToken(
		"u", "DDSUser", "", TokenOptions.optSwitch | TokenOptions.optRequired, "");
	static StringToken searchcritArg = new StringToken(
		"f", "SearchCritFile", "", TokenOptions.optSwitch, "");
	static IntegerToken timeoutArg = new IntegerToken(
		"t", "timeout seconds", "", TokenOptions.optSwitch, 3600);
	static IntegerToken debugArg = new IntegerToken(
		"d", "debug level", "", TokenOptions.optSwitch, 0);
	static StringToken logArg = new StringToken(
		"l", "log-file name", "", TokenOptions.optSwitch, "");
	static StringToken usbrHostArg = new StringToken(
		"H", "USBR Host", "", TokenOptions.optSwitch | TokenOptions.optRequired, "");
	static IntegerToken usbrPortArg = new IntegerToken(
		"P", "USBR Port", "", TokenOptions.optSwitch | TokenOptions.optRequired, 0);

	static
	{
		usbrSettings.addToken(ddsHostArg);
		usbrSettings.addToken(ddsPortArg);
		usbrSettings.addToken(ddsUserArg );
		usbrSettings.addToken(searchcritArg );
		usbrSettings.addToken(timeoutArg );
		usbrSettings.addToken(debugArg);
		usbrSettings.addToken(logArg);
		usbrSettings.addToken(usbrHostArg);
		usbrSettings.addToken(usbrPortArg);
		passwordArg = new StringToken(
			"w", "Password for auth connect", "", TokenOptions.optSwitch, "");
		usbrSettings.addToken(passwordArg); //Inherited from GetDcpMessages
	}

	public static void main(String args[])
	{
		try
		{
			usbrSettings.parseArgs(args);

			String crit = searchcritArg.getValue();
			if (crit != null && crit.length() == 0)
				crit = null;

			UsbrDrotFeed me = new UsbrDrotFeed(ddsHostArg.getValue(),
				ddsPortArg.getValue(), ddsUserArg.getValue(), crit,
				usbrHostArg.getValue(), usbrPortArg.getValue());

			me.setTimeout(timeoutArg.getValue());
			me.setSingleMode(true);

			me.start();
		}
		catch(Exception ex)
		{
			log.atError().setCause(ex).log("Exception while attempting to start client.");
		}
	}
}
