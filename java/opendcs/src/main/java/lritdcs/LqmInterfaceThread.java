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

import java.util.*;

import org.opendcs.utils.logging.OpenDcsLoggerFactory;
import org.slf4j.Logger;

import java.io.*;
import java.net.Socket;

import ilex.net.*;

public class LqmInterfaceThread extends BasicSvrThread
{
	private static final Logger log = OpenDcsLoggerFactory.getLogger();
	BufferedReader input;

	LqmInterfaceThread(BasicServer parent, Socket socket)
		throws IOException
	{
		super(parent, socket);
		input =
			new BufferedReader(new InputStreamReader(socket.getInputStream()));
		LritDcsStatus stat = LritDcsMain.instance().getStatus();
		stat.lqmStatus = "Active: " + socket.getInetAddress().toString();
		stat.lastLqmContact = System.currentTimeMillis();
	}

	public void disconnect()
	{
		LritDcsStatus stat = LritDcsMain.instance().getStatus();
		stat.lqmStatus = "Not Connected";
		super.disconnect();
	}

	protected void serviceClient()
	{
		// Blocking Read for a line of input
		String line;
		LritDcsStatus myStatus = LritDcsMain.instance().getStatus();
		try
		{
			line = input.readLine();
			if (line == null)
			{
				log.warn("LRIT:{} LQM Interface Socket from {} has disconnected.",
						 Constants.EVT_NO_LQM, getClientName());
				disconnect();
				return;
			}
		}
		catch(IOException ex)
		{
			log.atWarn()
			   .setCause(ex)
			   .log("LRIT:{} IO Error on LQM Interface Socket from {} -- disconnecting",
			   		Constants.EVT_NO_LQM, getClientName());
			disconnect();
			return;
		}

		// Process the line.
		line = line.trim();
		log.debug("Received LQM '{}'", line);
		StringTokenizer st = new StringTokenizer(line);
		int n = st.countTokens();
		if (n == 0)
			return;

		String keyword = st.nextToken();
		if (keyword.equalsIgnoreCase("STATUS"))
		{
			myStatus.lastLqmContact = System.currentTimeMillis();
		}
		else if (keyword.equalsIgnoreCase("FILE") && n == 3)
		{
			myStatus.lastLqmContact = System.currentTimeMillis();
			String fn = st.nextToken();
			String stat = st.nextToken();
			log.trace("LQM/IF: Got FILE report from LQM, name='{}', status='{}'", fn, stat);
			LritDcsMain ldm = LritDcsMain.instance();
			LinkedList pendingList = ldm.getFileNamesPending();
			FileQueue autoRetransQ = ldm.getFileQueueAutoRetrans();
			boolean wasInList = false;
			synchronized(pendingList)
			{
				for(Iterator it = pendingList.iterator(); it.hasNext(); )
				{
					SentFile sf = (SentFile)it.next();
					if (sf.filename.equals(fn))
					{
						it.remove();
						wasInList = true;
						break;
					}
				}
			}

			if (wasInList && stat.equalsIgnoreCase("B"))
			{
				// This file should be found in the priority's sent-directory:
				char pri = fn.charAt(1);
				File dir = new File(
					LritDcsConfig.instance().getLritDcsHome()
					+ File.separator
					+ (pri == Constants.HighPri ? "high.sent" :
					 pri == Constants.MediumPri ? "medium.sent" : "low.sent"));

				autoRetransQ.enqueue(new File(dir, fn));
				log.info("LQM reports failed transmission of '{}' -- scheduled for retransmission.", fn);
			}
			else if (wasInList)
			{
				log.info("LQM/IF reports successful transmission of '{}'.", fn);
			}
			else
			{
				log.info("Unexpected LQM transmit report: '{}' -- ignored.", line);
			}
		}
		else
		{
			log.info("Unrecognized Request from LQM '{}' -- ignored.", line);
		}
	}
}
