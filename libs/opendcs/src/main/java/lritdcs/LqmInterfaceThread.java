/*
*  $Id$
*
*  $Log$
*  Revision 1.1  2008/04/04 18:21:16  cvs
*  Added legacy code to repository
*
*  Revision 1.8  2007/11/01 20:07:51  mmaloney
*  dev
*
*  Revision 1.7  2005/12/30 19:40:59  mmaloney
*  dev
*
*  Revision 1.6  2004/05/25 15:06:52  mjmaloney
*  Propegate debug level to all loggers.
*
*  Revision 1.5  2004/05/24 17:11:08  mjmaloney
*  release prep
*
*  Revision 1.4  2004/05/21 18:27:44  mjmaloney
*  Release prep.
*
*  Revision 1.3  2004/05/18 22:52:40  mjmaloney
*  dev
*
*  Revision 1.2  2004/05/18 18:02:09  mjmaloney
*  dev
*
*  Revision 1.1  2004/05/06 21:48:13  mjmaloney
*  Implemented Lqm Server. Modified GUI to read events over the net.
*
*/
package lritdcs;

import java.util.*;
import java.io.*;
import java.net.Socket;

import ilex.net.*;
import ilex.util.Logger;
import lrgs.common.SearchCriteria;

public class LqmInterfaceThread
	extends BasicSvrThread
{
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
				Logger.instance().warning("LRIT:" + Constants.EVT_NO_LQM
					+ " LQM Interface Socket from " + getClientName() 
					+ " has disconnected.");
				disconnect();
				return;
			}
		}
		catch(IOException ex)
		{
			Logger.instance().warning("LRIT:" + Constants.EVT_NO_LQM
				+ " IO Error on LQM Interface Socket from " + getClientName() 
				+ " -- disconnecting");
			disconnect();
			return;
		}

		// Process the line.
		line = line.trim();
		Logger.instance().debug1(
			"Received LQM '" + line + "'");
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
			Logger.instance().log(Logger.E_DEBUG2, 
				"LQM/IF: Got FILE report from LQM, name='"
				+ fn + "', status=" + stat);
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
				Logger.instance().info(
					"LQM reports failed transmission of '" + fn 
					+ "' -- scheduled for retransmission.");
			}
			else if (wasInList)
				Logger.instance().info(
					"LQM/IF reports successful transmission of '" + fn 
					+ "'.");
			else
				Logger.instance().info(
					"Unexpected LQM transmit report: '" + line 
					+ "' -- ignored.");
		}
		else
			Logger.instance().info(
				"Unrecognized Request from LQM '" + line + "' -- ignored.");
	}
}
