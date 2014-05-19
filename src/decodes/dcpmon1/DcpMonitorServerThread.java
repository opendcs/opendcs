/*
*  $Id$
*/
package decodes.dcpmon1;

import ilex.net.BasicServer;
import ilex.net.BasicSvrThread;
import ilex.util.Logger;
import ilex.util.StringPair;
import ilex.xml.XmlOutputStream;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;
import java.util.TimeZone;

import lrgs.common.DcpAddress;
import decodes.db.Constants;
import decodes.db.Database;
import decodes.db.DatabaseException;
import decodes.db.Platform;
import decodes.db.PlatformList;
import decodes.db.TransportMedium;
import decodes.dcpmon1.RecentDataStore;
import decodes.util.ChannelMap;
import decodes.util.Pdt;
import decodes.util.PdtEntry;
import decodes.xml.PlatformParser;
import decodes.xml.XmlDbTags;
import decodes.drgsinfogui.DrgsReceiverIo;

/**
One of these objects is created to service each client that connects.
*/
public class DcpMonitorServerThread extends BasicSvrThread
{
	/** reads input from the client */
	private BufferedReader input;
	/** writes input to the client */
	private BufferedWriter output;

	public static final char SORT_BY_CHANNEL = 'C';
	public static final char SORT_BY_ADDRESS = 'A';
	public static final char SORT_BY_TIME = 'T';
	public static final char SORT_BY_TIMEREV = 'R';

	private SimpleDateFormat dateTimeFmt = null;
	private SimpleDateFormat dateTimeFmtSSS = null;
	private SimpleDateFormat dateFmt = null;
	private SimpleDateFormat longDateFmt = null;
	private SimpleDateFormat timeFmt = null;
	private SimpleDateFormat timeFmtSSS = null;
	private SimpleDateFormat chanReqDateFormat = null;
	private SimpleDateFormat fullDateFmt = null;

	/** Used to format battery voltage */
	private DecimalFormat bvFormat = null;

	private static final String module = "DcpMonitorServerThread ";
	public static final long MSEC_PER_DAY = (24L * 60L * 60L * 1000L);


	/**
	  Constructor called when a new client has connected.
	  @param parent the server object that is doing the listening
	  @param sock the client connection.
	*/
	public DcpMonitorServerThread(BasicServer parent, Socket sock)
		throws IOException
	{
		super(parent, sock);
		input = new BufferedReader(
			new InputStreamReader(sock.getInputStream()));
		output = new BufferedWriter(
			new OutputStreamWriter(sock.getOutputStream()));
		Logger.instance().debug1("New DCP Monitor Client on host " 
			+ sock.getInetAddress().getHostName());
		dateTimeFmt = new SimpleDateFormat("MM/dd/yyyy-HH:mm:ss");
		dateTimeFmtSSS = new SimpleDateFormat("MM/dd/yyyy-HH:mm:ss.S");
		dateFmt = new SimpleDateFormat("MM/dd/yyyy");
		longDateFmt = new SimpleDateFormat("dd MMMMM yyyy");
		timeFmt = new SimpleDateFormat("HH:mm:ss");
		timeFmtSSS = new SimpleDateFormat("HH:mm:ss.S");
		chanReqDateFormat = new SimpleDateFormat("dd MMMM yyyy");
		fullDateFmt = new SimpleDateFormat("EEE MMM dd HH:mm:ss yyyy");
		TimeZone utc = TimeZone.getTimeZone("UTC");
		dateTimeFmt.setTimeZone(utc);
		dateTimeFmtSSS.setTimeZone(utc);
		dateFmt.setTimeZone(utc);
		longDateFmt.setTimeZone(utc);
		timeFmt.setTimeZone(utc);
		timeFmtSSS.setTimeZone(utc);
		fullDateFmt.setTimeZone(utc);
		bvFormat = new DecimalFormat("##.#");
	}

	/**
	  Called when client has closed the socket, or when server wants to
	  hang up on this client.
	*/
	public void disconnect()
	{
		Logger.instance().debug2("DCP Monitor Client disconnected.");
		try
		{
			input.close();
			output.close();
		}
		catch(IOException ex)
		{
			Logger.instance().log(Logger.E_WARNING,
				"Ignored error when closing socket streams: " + ex);
		}
		super.disconnect(); // This removes the thread from server's collection.
	}

	/**
	  Called continually from BasicSvrThread:
	  <ul>
		<li>Block waiting for complete request from client</li>
		<li>Execute request when one is received.</li>
	  </ul>
	*/
	protected void serviceClient()
	{
		Logger.instance().debug2("serviceClient");
		String line = null;
		try { line = input.readLine(); }
		catch(IOException ex)
		{
			Logger.instance().log(Logger.E_FAILURE,
				"Error attempting to read line from socket: " + ex);
			disconnect();
			return;
		}

		if (line == null)
		{
			Logger.instance().debug2("Remote client hung up.");
			disconnect();
			return;
		}

		try { processLine(line); }
		catch(IOException ex)
		{
			Logger.instance().log(Logger.E_FAILURE,
				"IO Error in processing request '" + line + "': " + ex);
			disconnect();
		}
	}

	/**
	  Processes a single line command received from this client.
	  @param line the text line from the client.
	*/
	private void processLine(String line)
		throws IOException
	{
		StringTokenizer tokenizer = new StringTokenizer(line.trim());
		int numTokens = tokenizer.countTokens();
		if (numTokens == 0)
		{
			Logger.instance().debug2(
				"Blank line received and ignored from client.");
			return;
		}

		String keyword = tokenizer.nextToken().toLowerCase();
		if (keyword.equals("lb"))
			listGroups();
		else if (keyword.equals("ld"))
			listDcps();
		else if (keyword.equals("lc"))//list channel
			listChannels();
//		else if (keyword.equals("ms")) // ms group numdays
//		{
//			// Message Status requres 2 args.
//			if (numTokens != 3)
//			{
//				errorResponse(
//					"Improper number of arguments to Message Status request.");
//				return;
//			}
//			
//			String group = tokenizer.nextToken();
//			String s = tokenizer.nextToken();
//			int numDays = 0;
//			try { numDays = Integer.parseInt(s); }
//			catch(NumberFormatException ex)
//			{
//				errorResponse("Bad numDays arg '" + s 
//					+ "' in Message Status request.");
//				return;
//			}
//			sendMessageStatus(group, numDays);
//		}
		else if (keyword.equals("msh")) // ms group numdays
		{
			// Message Status requres 2 args.
			if (numTokens != 3)
			{
				errorHtmlResponse(
					"Improper number of arguments to Message Status HTML request.");
				return;
			}
			
			String group = tokenizer.nextToken();
			String s = tokenizer.nextToken();
			int numDays = 0;
			try { numDays = Integer.parseInt(s); }
			catch(NumberFormatException ex)
			{
				errorHtmlResponse("Bad numDays arg '" + s 
					+ "' in Message Status request.");
				return;
			}
			sendMessageStatusHtml(group, numDays);
		}
		else if (keyword.startsWith("ceh"))
		{
			if (numTokens < 5)
			{
				errorHtmlResponse("Improper number of arguments to ce.");
				return;
			}
			String chanstr = tokenizer.nextToken();
			String datestr = tokenizer.nextToken() + " " + tokenizer.nextToken()
				+ " " + tokenizer.nextToken();
			String groupName = null;
			if (numTokens == 6)
				groupName = tokenizer.nextToken();
			try
			{
				int chan = Integer.parseInt(chanstr);
				Date d = chanReqDateFormat.parse(datestr);
				sendChannelExpandHtml(
					(int)(d.getTime() / RecentDataStore.MSEC_PER_DAY), chan,
					groupName);
			}
			catch(NumberFormatException ex)
			{
				errorHtmlResponse("Bad channel number '" + chanstr + "'");
				return;
			}
			catch(ParseException ex)
			{
				errorHtmlResponse("Bad date '" + datestr + "'");
				return;
			}
		}
//		else if (keyword.equals("wz")) // wza name numdays
//		{
//			//if (numTokens != 3)
//			if (numTokens == 3)
//			{	//old behavior - this can be used from telnet - when testing
//				//and from dcpfull.cgi
//				String nm = tokenizer.nextToken();
//				DcpGroupList dgl = DcpGroupList.instance();
//				DcpAddress dcpAddress = dgl.getDcpAddress(nm);
//			
//				if (dcpAddress == null)
//				{
//					// Old code would throw if nm wasn't a valid address. 
//					// New code will not, we just won't find any data.
//					dcpAddress = new DcpAddress(nm);
//				}
//
//				String s = tokenizer.nextToken();
//				int numDays = 0;
//				try { numDays = Integer.parseInt(s); }
//				catch(NumberFormatException ex)
//				{
//					errorResponse(
//					"Bad numDays arg '" + s + "' in Message Status request.");
//					return;
//				}
//				sendWazzupDcp(nm, dcpAddress, numDays, false);
//			}
//			else if (numTokens == 4)
//			{	//This is used from dcpname link in dcpmsgout.cgi
//				String dcpName = tokenizer.nextToken();
//				DcpAddress dcpAddress = new DcpAddress(tokenizer.nextToken());
//				
//				//JP - Now the cgi will send the dcp address and 
//				//the dcp name in this way we can use the NWS dcp names
//				//without problem
//
//				String s = tokenizer.nextToken();
//				int numDays = 0;
//				try { numDays = Integer.parseInt(s); }
//				catch(NumberFormatException ex)
//				{
//					errorResponse(
//					"Bad numDays arg '" + s + "' in Message Status request.");
//					return;
//				}
//				sendWazzupDcp(dcpName, dcpAddress, numDays, false);
//			}
//			else
//			{
//				errorResponse(
//				"Improper number of arguments to wazzup DCP request.");
//				return;
//			}
//		}
//		else if (keyword.equals("wzd")) // wzd name mm/dd/yyyy
//		{
//			//if (numTokens != 3)
//			if (numTokens == 3)
//			{	//old behavior - this can be used from telnet - when testing
//				String nm = tokenizer.nextToken();
//				DcpGroupList dgl = DcpGroupList.instance();
//				DcpAddress dcpAddress = dgl.getDcpAddress(nm);
//				if (dcpAddress == null)
//					dcpAddress = new DcpAddress(nm);
//
//				String s = tokenizer.nextToken();
//				Date date;
//				try
//				{
//					date = dateFmt.parse(s);
//				}
//				catch(ParseException ex)
//				{
//					errorResponse("Bad date format '" + s + "'");
//					return;
//				}
//				int daynum = (int)(date.getTime() / (24 * 60 * 60 * 1000L));
//				daynum = getCurrentDay() - daynum;
//
//				sendWazzupDcp(nm, dcpAddress, daynum, true);
//			}
//			else if (numTokens == 4)
//			{	
//				String dcpName = tokenizer.nextToken();
//				DcpAddress dcpAddress = new DcpAddress(tokenizer.nextToken());
//				//JP - Now the cgi will send the dcp address and
//				//the dcp name in this way we can use the NWS dcp names
//				//without problem
//
//				String s = tokenizer.nextToken();
//				Date date;
//				try
//				{
//					date = dateFmt.parse(s);
//				}
//				catch(ParseException ex)
//				{
//					errorResponse("Bad date format '" + s + "'");
//					return;
//				}
//				int daynum = (int)(date.getTime() / (24 * 60 * 60 * 1000L));
//				daynum = getCurrentDay() - daynum;
//
//				sendWazzupDcp(dcpName, dcpAddress, daynum, true);
//			}
//			else
//			{
//				errorResponse(
//				"Improper number of arguments to wazzup DCP request.");
//				return;
//			}
//		}

		else if (keyword.equals("wzdh")) // wzd name mm/dd/yyyy dcpaddr
		{
			if (numTokens >= 3)
			{	//old behavior - this can be used from telnet - when testing
				String nm = tokenizer.nextToken();
				String dates = tokenizer.nextToken();
				DcpAddress dcpAddress = null;
				if (numTokens > 3)
				{
					dcpAddress = new DcpAddress(tokenizer.nextToken());
					Logger.instance().info("wzdh dcpAddress='" + dcpAddress + "'");
				}
				else
				{
					DcpNameDescResolver namer = 
						DcpMonitor.instance().getDcpNameDescResolver();
					dcpAddress = namer.name2dcpAddress(nm);
					if (dcpAddress == null)
						dcpAddress = new DcpAddress(nm.toUpperCase());
				}

				boolean oneDayOnly = true;
				Date date;
				// # of days ago to begin retrieval
				int daysAgo = 1;
				try
				{
					date = dateFmt.parse(dates);
					daysAgo = (int)(date.getTime() / (24 * 60 * 60 * 1000L));
					daysAgo = getCurrentDay() - daysAgo;
				}
				catch(ParseException ex)
				{
					try 
					{
						// Alternate form - we are provided an integer # of days.
						daysAgo  = Integer.parseInt(dates);
						if (daysAgo >=0 && daysAgo <= 30)
							oneDayOnly = false;
						else
						{
							errorHtmlResponse("Invalid # days '" + dates + "'");
							return;
						}
					}
					catch(NumberFormatException ex2)
					{
						errorHtmlResponse("Bad date format '" + dates + "'");
						return;
					}
				}

				sendWazzupDcpHtml(nm, dcpAddress, daysAgo, oneDayOnly);
			}
			else
			{
				StringBuilder sb = new StringBuilder();
				while (tokenizer.hasMoreTokens())
					sb.append("'" + tokenizer.nextToken() + "' ");
				errorHtmlResponse("Improper number of arguments to WZDH: "
					+ sb.toString());
			}
		}
		else if (keyword.startsWith("st"))
			sendStatus();
		else if (keyword.startsWith("cm"))
			sendChannelMap();
		else if (keyword.startsWith("dl"))
		{
			sendDrgsList();	
		}
		else if (keyword.startsWith("mh"))
		{
			if (numTokens != 2)
			{
				errorResponse(
					"Improper number of arguments to mh.");
				return;
			}
			String relpath = tokenizer.nextToken();
			if (relpath.endsWith("drgsident.html"))
			{
				sendDrgsList();	
			}
			else
			{	//This is a decode msg request
				//The relpath contains the daynum, dcp addr and goes time
				//Or it may contain missing
				if (relpath.equalsIgnoreCase("missing"))
				{
					output.write("The DCP message for the referenced platform "
					+ "was missing (not received in its proper time slice)." +
					"\n\n");
					output.write("MHEND\n");
					output.flush();
					return;
				}
				StringTokenizer temp = new StringTokenizer(relpath.trim(),
						"_");
				int tempTokens = temp.countTokens();
				if (tempTokens != 3)
				{//mh 13855_9433071A_1197072152000
					output.write("Wrong request. " + line +
					"\nThe mh request is: mh daynum_dcpaddress_goestime"+
					"\ndaynum - is number of days (0 = Jan 1, 1970) " +
					"\ndcpaddress - goes address " + 
					"\ngoestime - goes time in milliseconds " + 
					"\nExample: mh 13855_164012E2_1197114187000 " + 
					"\n\n");
					output.write("MHEND\n");
					output.flush();
					return;
				}
				String dayNStr = temp.nextToken();
				DcpAddress dcpAddress = new DcpAddress(temp.nextToken());
				String goesTime = temp.nextToken();
//System.out.println("day num = " + dayNStr);
//System.out.println("dcp addr = " + dcpAddress);
//System.out.println("goesTime = " + goesTime);
				try 
				{
					int dayNum = new Integer(dayNStr).intValue();
					long ts = new Long(goesTime).longValue();
					Date timestamp = new Date(ts);
					sendDecodedMsgHtml(dayNum, dcpAddress, timestamp);
				}
				catch(NumberFormatException ex)
				{
					output.write("-Error: Wrong request. " + line +
					"\nThe mh request is: mh daynum_dcpaddress_goestime"+
					"\ndaynum - is number of days (0 = Jan 1, 1970) " +
					"\ndcpaddress - goes address " + 
					"\ngoestime - goes time in milliseconds " + 
					"\nExample: mh 13855_164012E2_1197114187000 " + 
					"\n\n");
					output.write("MHEND\n");
					output.flush();
					return;
				}
			}
		}
		else if (keyword.startsWith("md"))
		{
			if (numTokens != 2)
			{
				errorResponse(
					"Improper number of arguments to md.");
				return;
			}
			sendMetaData(tokenizer.nextToken());
		}
//		else if (keyword.startsWith("ce"))
//		{
//			if (numTokens < 5)
//			{
//				errorResponse("Improper number of arguments to ce.");
//				return;
//			}
//			String chanstr = tokenizer.nextToken();
//			String datestr = tokenizer.nextToken() + " " + tokenizer.nextToken()
//				+ " " + tokenizer.nextToken();
//			String groupName = null;
//			if (numTokens == 6)
//				groupName = tokenizer.nextToken();
//			try
//			{
//				int chan = Integer.parseInt(chanstr);
//				Date d = chanReqDateFormat.parse(datestr);
//				sendChannelExpand(
//					(int)(d.getTime() / RecentDataStore.MSEC_PER_DAY), chan,
//					groupName);
//			}
//			catch(NumberFormatException ex)
//			{
//				errorResponse("Bad channel number '" + chanstr + "'");
//				return;
//			}
//			catch(ParseException ex)
//			{
//				errorResponse("Bad date '" + datestr + "'");
//				return;
//			}
//		}
		else
			errorResponse("Unrecognized keyword '" + keyword + "'");
	}

	/** Handles the list-groups command */
	private void listGroups()
		throws IOException
	{
		DcpGroupList dgl = DcpGroupList.instance();
		ArrayList<String> grpNames = dgl.getGroupNameList();
		for(String nm : grpNames)
			output.write(nm + "\n");
		output.write("\n");
		output.flush();
	}

	/** Handles the list-channels commad 
	 * @throws IOException */
	private void listChannels() throws IOException
	{
		int[] channels = null;
		DcpMonitorConfig dmc = DcpMonitorConfig.instance();
		if (dmc.allChannels)
		{
			ChannelMap cmap = ChannelMap.instance();
			if (cmap != null)
			{	//channels from https://dcs1.noaa.gov/chans_by_baud.txt
				channels = cmap.getChannelList();
				if (channels != null && channels.length > 0)
				{
					for (int chan : channels)
					{
						output.write("Channel_" + chan + "\n");
					}
					Logger.instance().debug1("DcpMonitorServerThread: " +
						" ListChannels from " +
						"https://dcs1.noaa.gov/chans_by_baud.txt.");
				}	
			}	
		}
		if (channels == null || channels.length == 0)
		{
			//try to get channels from DCP Monitor groups - 
			//using PDT pdtuse = true
			channels = DcpMonitor.instance().getMyChannels();
			if (channels != null && channels.length > 0)
			{
				for (int chan : channels)
				{
					output.write("Channel_" + chan + "\n");
				}
				Logger.instance().debug1("DcpMonitorServerThread: " +
						"ListChannels from Groups");
			}
			else
			{
				Logger.instance().debug1("DcpMonitorServerThread:" +
				" No ListChannels to display.");
				output.write("Channel_1\n");
			}
		}
		output.write("\n");
		output.flush();
	}
	
	/** Handles the list-dcps command */
	private void listDcps()
		throws IOException
	{
Logger.instance().debug1(module + "listDcps starting.");
		DcpGroupList dgl = DcpGroupList.instance();
		ArrayList<String> grpNames = dgl.getGroupNameList();
		for(String grpnm : grpNames)
		{
			DcpGroup grp = dgl.getGroup(grpnm);
			if (grp == null) continue; // shouldn't happen
			ArrayList<String> dcps = new ArrayList<String>();
			for(Iterator<DcpAddress> adit = grp.getDcpAddresses();
				adit.hasNext(); )
			{
				DcpAddress addr = adit.next();
				String dcpnm = grp.getDcpName(addr);
				String ln = addr.toString() + " " + dcpnm + " " + grpnm;
				dcps.add(ln);
			}

			// sort by DCP name, starts on char 9 after the hex address.
			Collections.sort(dcps, 
				new Comparator<String>()
				{
					public int compare(String s1, String s2)
					{
						return s1.substring(9).compareTo(s2.substring(9));
					}
					public boolean equals(Object o1) { return false; }
				});

			for(Iterator<String> dcpit = dcps.iterator(); dcpit.hasNext();)
			{
				String ln = (String)dcpit.next();
				output.write(ln + "\n");
			}
		}
		output.write("\n");
		output.flush();
	}

//	/** Handles the status command */
//	private void sendMessageStatus(String group, int numDays)
//		throws IOException
//	{
//		Logger.instance().debug1(module + ":sendMessageStatus " +
//				"Getting data for  " + group);
//		DcpNameDescResolver namer = 
//			DcpMonitor.instance().getDcpNameDescResolver();
//		DcpGroupList dgl = DcpGroupList.instance();
//
//		int chan = -1;
//		DcpGroup grp = null;
//		if (group != null && group.startsWith("Channel_"))
//		{	//User selected channel radio button in dcpmsg.tmpl
//			//parse the group and get the channel number
//			String temp = group.substring(8);
//			group = temp;
//			chan = Integer.parseInt(temp);
//		}
//		else //User selected group radio button  in dcpmsg.tmpl
//		{	
//			grp = dgl.getGroup(group);
//			if (grp == null)
//			{
//				errorResponse("No such group '" + group + "'");
//				return;
//			}
//		}
//
//		int curday = getCurrentDay();
//		int earliestDay = curday - numDays;
//		Logger.instance().debug3(
//			"SMS curday=" + curday + ", earliestDay=" + earliestDay);
//
//		for(int daynum = curday; daynum >= earliestDay; daynum--)
//		{
//			ArrayList<XmitRecord> results = new ArrayList<XmitRecord>();
//			int n = 0;
//			if (chan == -1) // Retrieve by GROUP
//			{
//				Logger.instance().debug1("SMS Getting data for day " 
//					+ daynum + " from group " + group);
//				n = DcpMonitor.instance().readXmitsByGroup(results, daynum, 
//						grp);
//				Logger.instance().debug1("SMS retrieval done. Read " + n); 
//			}
//			else // Retrieve by CHANNEL
//			{
//				Logger.instance().debug1("SMS Getting data for day " 
//					+ daynum + " from channel " + chan);
//				n = DcpMonitor.instance().readXmitsByChannel(results, daynum, 
//						chan);
//				Logger.instance().debug1("SMS retrieval done. Read " + n); 
//			}
//
//			if (n == 0) // No data for this day
//			{
//				Logger.instance().debug1(module + ":sendMessageStatus " +
//						"No data for day " + daynum + " just skip it.");
//				// Don't skip day, still print an empty day's slots.
//			}
//
//			/*
//			  Sort by address/channel/secondOfDay. This puts all the
//			  records in order for me to compile them into daily MessageStatus
//			  records.
//			*/
//			Logger.instance().debug3(module + ":sendMessageStatus Sorting " +
//					"results by address");
//			sortResults(results, SORT_BY_ADDRESS);
//
//			ArrayList<MsgStatRecord> msVec = new ArrayList<MsgStatRecord>();
//			ArrayList<XmitRecord> unexpectedXRs = new ArrayList<XmitRecord>();
//			DcpAddress lastAddr = null;
//			int lastChan = -1;
//			MsgStatRecord msr = null;
//			for(XmitRecord xr : results)
//			{
//				// If this is for a new DCP:
//				if (lastAddr == null
//				 || !xr.getDcpAddress().equals(lastAddr) 
//				 || xr.getGoesChannel() != lastChan)
//				{
//					msr = new MsgStatRecord(daynum);
//					msr.dcpAddress = lastAddr = xr.getDcpAddress();
//
//					Platform p;
//					try
//					{
//						p = Database.getDb().platformList.getPlatform(
//						    Constants.medium_GoesST, msr.dcpAddress.toString());
//						msr.dcpName = namer.getBestName(msr.dcpAddress, p);
//					}
//					catch (DatabaseException ex)
//					{
//						String err = module + ":dcpMonitorServerThread "
//							+ "SendMessageStatus Exception reading platform: "
//							+ ex;
//						Logger.instance().warning(err);
//						System.err.println(err);
//						ex.printStackTrace();
//					}				
//					msr.firstMsgSecOfDay = xr.firstXmitSecOfDay;
//					msr.goesChannel = lastChan = xr.getGoesChannel();
//					msr.basin = xr.basin;
//					msVec.add(msr);
//				}
//				int hr = xr.getSecOfDay() / 3600;
//				String fc = xr.failureCodes();
//				msr.fcodes[hr] = fc;
//				if (xr.hasFailureCode('U'))
//					unexpectedXRs.add(xr);
//			}
//			
//			// Only if this is by GROUP (not by channel) add empty MSR records
//			// for any group members for which there was no data.
//			if (chan == -1)
//			{
//				Logger.instance().debug3(
//				    "SMS Adding MSRs for empty group members");
//				try
//				{
//					ArrayList<MsgStatRecord> emptyMSRs = 
//						new ArrayList<MsgStatRecord>();
//				  nextDCP:
//					for (Iterator<DcpAddress> dcpit = grp.getDcpAddresses(); 
//						dcpit.hasNext();)
//					{
//						DcpAddress addr = dcpit.next();
//						for (MsgStatRecord msrt : msVec)
//							if (msrt.dcpAddress.equals(addr))
//								continue nextDCP;
//
//						// Fell through -- this DCP isn't in the vec. Add it.
//						msr = new MsgStatRecord(daynum);
//						msr.dcpAddress = addr;
//						msr.basin = grp.getGroupName();
//						try
//						{
//							Platform p = Database.getDb().platformList
//							    .getPlatform(Constants.medium_GoesST, 
//							    addr.toString());
//							msr.dcpName = namer.getBestName(addr, p);
//							if (p != null)
//							{
//								TransportMedium tm = 
//									p.getTransportMedium(Constants.medium_GoesST);
//								if (tm != null)
//								{
//									msr.firstMsgSecOfDay = tm.assignedTime;
//									msr.goesChannel = tm.channelNum;
//								}
//							}
//						}
//						catch (DatabaseException ex)
//						{
//							// Skip if no platform record.
//						}
//						emptyMSRs.add(msr);
//					}
//					for (MsgStatRecord msrt : emptyMSRs)
//						msVec.add(msrt);
//				}
//				catch (Exception ex)
//				{
//					String err = module + ":sendMessageStatus "
//						+ "Exception adding empty records: " + ex;
//					Logger.instance().warning(err);
//					System.out.println(err);
//					ex.printStackTrace();
//				}
//			}
//
//			addUnexpecteds(msVec, unexpectedXRs, daynum);
//			unexpectedXRs = null;
//
//Logger.instance().debug1("SMS printing output.");
//
//			sortMsgStatVec(msVec, SORT_BY_CHANNEL);
//			Pdt pdt = Pdt.instance();
//			for(MsgStatRecord msre : msVec)
//			{
//				PdtEntry pte = pdt.find(msre.dcpAddress);
//				if (pte != null)
//				{
//					msre.firstMsgSecOfDay = pte.st_first_xmit_sod;
//					msre.agency = pte.agency;
//				}
//			}
//
//			output.write("T " + formatDate(daynum) + "\n");
//			lastChan = -1;
//			for(MsgStatRecord msrt : msVec)
//			{
//				if (msrt.goesChannel != lastChan)
//				{
//					lastChan = msrt.goesChannel;
//					output.write("C " + msrt.goesChannel + "\n");
//				}
//				
//				output.write("D:;" 
//					+ msrt.dcpAddress.toString()
//					+ ":;" + msrt.dcpName
//					+ ":;" + msrt.agency
//					+ ":;" + (msrt.isUnexpected ? "UNEXPECT" :
//						formatSecOfDay(msrt.firstMsgSecOfDay)));
//				for(int i=0; i<24; i++)
//					output.write(":;" + msrt.fcodes[i]);
//				output.write("\n");
//			}
//		}
//		output.write("\n");
//		output.flush();
//Logger.instance().debug1("SMS done.");
//	}
	
	
	

	/** Add the Unexpecteds to catch 'rogue' DCPs. */
	private void addUnexpecteds(ArrayList<MsgStatRecord> msVec,
		ArrayList<XmitRecord> unexpectedXRs, int daynum)
	{
		DcpNameDescResolver namer = 
			DcpMonitor.instance().getDcpNameDescResolver();
		DcpGroupList dgl = DcpGroupList.instance();
		
		// Sort MSR records first by time, then DCP address. 
		// (all channels will be jumbled together)
		Comparator<MsgStatRecord> comparator =
			new Comparator<MsgStatRecord>()
			{
                public int compare(MsgStatRecord msr1, MsgStatRecord msr2)
                {
             	   int dt = msr1.firstMsgSecOfDay - msr2.firstMsgSecOfDay;
             	   if (dt != 0)
             		   return dt;
             	   return msr1.dcpAddress.compareTo(msr2.dcpAddress);
                }
			};
		Collections.sort(msVec, comparator);

		for(XmitRecord xr : unexpectedXRs)
		{
			// Get time of 1st hour, quantized to 5-second chunks.
			int secOfFirstHour = xr.getSecOfDay() % 3600;
			secOfFirstHour = (secOfFirstHour / 5) * 5;
			int hour = xr.getSecOfDay() / 3600;
			
			// Find insertion point in MSR vector
			MsgStatRecord key = new MsgStatRecord(daynum);
			key.firstMsgSecOfDay = secOfFirstHour;
			key.dcpAddress = xr.getDcpAddress();
			int ip = Collections.binarySearch(msVec, key, comparator);

			MsgStatRecord unexpectedMsr = null;
			if (ip >= 0) // MSR with matching time is found.
			{
				unexpectedMsr = msVec.get(ip);
			}
			else
			{
				// ip negative means no match and IP is the insertion point.
				ip = -(ip + 1);
			}
			if (unexpectedMsr == null)
			{
				unexpectedMsr = new MsgStatRecord(daynum);
				unexpectedMsr.dcpAddress = xr.getDcpAddress();
				unexpectedMsr.firstMsgSecOfDay = secOfFirstHour;
				unexpectedMsr.goesChannel = xr.getGoesChannel();
				unexpectedMsr.isUnexpected = true;
				unexpectedMsr.basin = xr.basin;
				Platform tp = null;
				try
				{
					tp = Database.getDb().platformList.getPlatform(
						Constants.medium_GoesST, 
						xr.getDcpAddress().toString());
				}
				catch(DatabaseException ex) { tp = null; }
				unexpectedMsr.dcpName = namer.getBestName(
					unexpectedMsr.dcpAddress, tp);

				unexpectedMsr.isMine = 
					dgl.getDcpNameIfFound(xr.getDcpAddress()) != null;
				msVec.add(ip, unexpectedMsr);
Logger.instance().info("Added UNEXPEC dcpaddr=" + unexpectedMsr.dcpAddress
+ ", firstMsgSecOfDay=" + unexpectedMsr.firstMsgSecOfDay
+ ", at position " + ip + ", of the " + msVec.size() + " element array.");
			}
			
			StringBuilder sb = 
				new StringBuilder(unexpectedMsr.fcodes[hour]);
			String xrFCs = xr.failureCodes();
			for(int i=0; i < xrFCs.length(); i++)
				if (sb.indexOf(""+xrFCs.charAt(i)) < 0)
				{
					char c = xrFCs.charAt(i);
					if (c == 'G') c = '_';
					sb.append(c);
				}
			unexpectedMsr.setCodes(hour, sb.toString());
		}
	}

	/** Handles the status command */
	private void sendMessageStatusHtml(String group, int numDays)
		throws IOException
	{
		String method = module + ":sendMessageStatusHtml ";
		Logger.instance().debug1(method + "Getting data for  " + group);
		DcpNameDescResolver namer = 
			DcpMonitor.instance().getDcpNameDescResolver();
		Pdt pdt = Pdt.instance();

		int chan = -1;
		DcpGroup grp = null;
		if (group != null && group.startsWith("Channel_"))
		{	//User selected channel radio button in dcpmsg.tmpl
			//parse the group and get the channel number
			String temp = group.substring(8);
			group = temp;
			chan = Integer.parseInt(temp);
		}
		else //User selected group radio button  in dcpmsg.tmpl
		{	
			grp = DcpGroupList.instance().getGroup(group);
			if (grp == null)
			{
				errorResponse("No such group '" + group + "'");
				return;
			}
		}

		int curday = getCurrentDay();
		int earliestDay = curday - numDays;
		Logger.instance().debug3(
			"SMS curday=" + curday + ", earliestDay=" + earliestDay);

		ByteArrayOutputStream baos = startByteArrayOutputStream();
		XmlOutputStream xos = startXmlOutputStream(baos, "DCP Message Status");
		xos.startElement("body");
		xos.writeElement("h1", "class", "msg_status", "DCP Message Status");
		xos.writeElement("div", "style", "text-align:center;",
			"UTC: " + fullDateFmt.format(new Date()));
		xos.writeElement("br", null);

		for(int daynum = curday; daynum >= earliestDay; daynum--)
		{
			ArrayList<XmitRecord> results = new ArrayList<XmitRecord>();
			int n = 0;
			if (chan == -1) // Retrieve by GROUP
			{
				Logger.instance().debug1("SMS Getting data for day " 
					+ daynum + " from group " + group);
				n = DcpMonitor.instance().readXmitsByGroup(results, daynum, 
						grp);
				Logger.instance().debug1("SMS retrieval done. Read " + n); 
			}
			else // Retrieve by CHANNEL
			{
				Logger.instance().debug1("SMS Getting data for day " 
					+ daynum + " from channel " + chan);
				n = DcpMonitor.instance().readXmitsByChannel(results, daynum, 
						chan);
				Logger.instance().debug1("SMS retrieval done. Read " + n); 
			}

			if (n == 0) // No data for this day
			{
				Logger.instance().debug1(method +
						"No data for day " + daynum + " just skip it.");
				// Don't skip day, still print an empty day's slots.
			}

			/*
			  Sort by address/channel/secondOfDay. This puts all the
			  records in order for me to compile them into daily MessageStatus
			  records.
			*/
			Logger.instance().debug3(method + "results by address");
			sortResults(results, SORT_BY_ADDRESS);

			ArrayList<MsgStatRecord> msVec = new ArrayList<MsgStatRecord>();
			ArrayList<XmitRecord> unexpectedXRs = new ArrayList<XmitRecord>();
			DcpAddress lastAddr = null;
			int lastChan = -1;
			MsgStatRecord msr = null;
			for(XmitRecord xr : results)
			{
				// If this is for a new DCP:
				if (lastAddr == null
				 || !xr.getDcpAddress().equals(lastAddr) 
				 || xr.getGoesChannel() != lastChan)
				{
					msr = new MsgStatRecord(daynum);
					msr.dcpAddress = lastAddr = xr.getDcpAddress();

					Platform p;
					try
					{
						p = Database.getDb().platformList.getPlatform(
						    Constants.medium_GoesST, msr.dcpAddress.toString());
						msr.dcpName = namer.getBestName(msr.dcpAddress, p);
					}
					catch (DatabaseException ex)
					{
						String err = module + ":dcpMonitorServerThread "
							+ "SendMessageStatus Exception reading platform: "
							+ ex;
						Logger.instance().warning(err);
						System.err.println(err);
						ex.printStackTrace();
					}				
					msr.firstMsgSecOfDay = xr.firstXmitSecOfDay;
					msr.goesChannel = lastChan = xr.getGoesChannel();
					// Xmits on wrong channel, MSR should display on expected
					// channel.
					if (xr.hasFailureCode('W'))
					{
						PlatformInfo platInfo = 
							PlatformInfo.getPlatformInfo(null, 
								msr.dcpAddress, 0);
						if (platInfo != null)
							msr.goesChannel = platInfo.stchan;
					}

					msr.basin = xr.basin;
					msVec.add(msr);
				}
				int hr = xr.getSecOfDay() / 3600;
				String fc = xr.failureCodes();
				msr.fcodes[hr] = fc;
				if (xr.hasFailureCode('U'))
					unexpectedXRs.add(xr);
			}
			
			// Only if this is by GROUP (not by channel) add empty MSR records
			// for any group members for which there was no data.
			if (chan == -1)
			{
				Logger.instance().debug3(
				    "SMS Adding MSRs for empty group members");
				try
				{
					ArrayList<MsgStatRecord> emptyMSRs = 
						new ArrayList<MsgStatRecord>();
				  nextDCP:
					for (Iterator<DcpAddress> dcpit = grp.getDcpAddresses(); 
						dcpit.hasNext();)
					{
						DcpAddress addr = dcpit.next();
						for (MsgStatRecord msrt : msVec)
							if (msrt.dcpAddress.equals(addr))
								continue nextDCP;

						// Fell through -- this DCP isn't in the vec. Add it.
						msr = new MsgStatRecord(daynum);
						msr.dcpAddress = addr;
						msr.basin = grp.getGroupName();
						
						try
						{
							Platform p = Database.getDb().platformList
							    .getPlatform(Constants.medium_GoesST, 
							    addr.toString());
							msr.dcpName = namer.getBestName(addr, p);
							if (p != null)
							{
								TransportMedium tm = 
									p.getTransportMedium(Constants.medium_GoesST);
								if (tm != null)
								{
									msr.firstMsgSecOfDay = tm.assignedTime;
									msr.goesChannel = tm.channelNum;
								}
							}
						}
						catch (DatabaseException ex)
						{
							// Skip if no platform record.
						}
						emptyMSRs.add(msr);
					}
					for (MsgStatRecord msrt : emptyMSRs)
						msVec.add(msrt);
				}
				catch (Exception ex)
				{
					String err = module + ":sendMessageStatus "
						+ "Exception adding empty records: " + ex;
					Logger.instance().warning(err);
					System.out.println(err);
					ex.printStackTrace();
				}
			}

			for(MsgStatRecord msre : msVec)
			{
				PdtEntry pte = pdt.find(msre.dcpAddress);
				if (pte != null)
				{
					msre.firstMsgSecOfDay = pte.st_first_xmit_sod;
					msre.agency = pte.agency;
					if (msre.goesChannel == -1)
						msre.goesChannel = pte.st_channel;
				}
			}

			addUnexpecteds(msVec, unexpectedXRs, daynum);
			unexpectedXRs = null;

			// Sort the records by channel, 1st-shoot-time, name
			Comparator<MsgStatRecord> comparator =
				new Comparator<MsgStatRecord>()
				{
	                public int compare(MsgStatRecord msr1, MsgStatRecord msr2)
	                {
	                	int c = msr1.goesChannel - msr2.goesChannel;
	                	if (c != 0)
	                		return c;

	            		c = msr1.firstMsgSecOfDay - msr2.firstMsgSecOfDay;
	            		if (c != 0)
	            			return c;

	            		return msr1.dcpName.compareTo(msr2.dcpName);
	                }
				};
			Collections.sort(msVec, comparator);
			Logger.instance().debug1("SMSH printing output.");
			
			xos.startElement("table");
			String longDate = longDateFmt.format(daynumToDate(daynum));
			addMsgStatDayHeading(xos, group, longDate);

			lastChan = -1;
			for(MsgStatRecord msrt : msVec)
			{
				if (msrt.goesChannel != lastChan)
				{
					lastChan = msrt.goesChannel;
					xos.startElement("tr");
					xos.startElement("td", "class", "rowgroupHeader",
						"colspan", "28");
					xos.writeElement("a", "href",
						"chan-expandHtml.cgi?channel=Channel " 
						+ msrt.goesChannel
						+ "&date=" + longDate
						+ "&group=" + group,
						"Channel " + msrt.goesChannel);
					xos.endElement("td");
					xos.endElement("tr");
				}

				addMsgStatLine(xos, msrt, daynum);
			}
		}
		xos.endElement("table");
		writeLegend(xos, true);
		writeFooter(xos);
		xos.endElement("body");
		xos.endElement("html");
		output.write(baos.toString());
		output.write("HTML END\n");
		output.flush();
Logger.instance().debug1("SMSH done.");
	}

	private void writeLegend(XmlOutputStream xos, boolean subG)
		throws IOException
	{
		xos.startElement("table", "class", "failureCodeLegend");
		xos.writeElement("caption", "Failure code legend");
		xos.startElement("tr");
		xos.writeElement("td", subG ? "_" : "G");
		xos.writeElement("td","Good DCP Message");
		xos.endElement("tr");

		xos.startElement("tr");
		xos.writeElement("td","?");
		xos.writeElement("td","DCP Message with Parity Error");
		xos.endElement("tr");
		
		xos.startElement("tr");
		xos.writeElement("td","A");
		xos.writeElement("td","DCP message contained a correctable address error");
		xos.endElement("tr");
		
		xos.startElement("tr");
		xos.writeElement("td","B");
		xos.writeElement("td","DCP message contained a bad (unknown) address");
		xos.endElement("tr");
		
		xos.startElement("tr");
		xos.writeElement("td","D");
		xos.writeElement("td","DCP message was duplicated (i.e. received on multiple channels)");
		xos.endElement("tr");
		
		xos.startElement("tr");
		xos.writeElement("td","I");
		xos.writeElement("td","DCP message had an invalid address");
		xos.endElement("tr");
		
		xos.startElement("tr");
		xos.writeElement("td","M");
		xos.writeElement("td","The DCP message for the referenced platform was missing (not received in its proper time slice)");
		xos.endElement("tr");
		
		xos.startElement("tr");
		xos.writeElement("td","N");
		xos.writeElement("td","The referenced platform has a non-complete entry in the DAPS Platform Description Table (PDT)");
		xos.endElement("tr");
		
		xos.startElement("tr");
		xos.writeElement("td","Q");
		xos.writeElement("td","DCP message had bad quality measurements");
		xos.endElement("tr");
		
		xos.startElement("tr");
		xos.writeElement("td","T");
		xos.writeElement("td","DCP message was received outside its proper time slice (early/late)");
		xos.endElement("tr");
		
		xos.startElement("tr");
		xos.writeElement("td","U");
		xos.writeElement("td","DCP message was unexpected");
		xos.endElement("tr");
		
		xos.startElement("tr");
		xos.writeElement("td","W");
		xos.writeElement("td","DCP message was received on the wrong channel");
		xos.endElement("tr");
		
		xos.startElement("tr");
		xos.writeElement("td","C");
		xos.writeElement("td","Excessive carrier before start of message");
		xos.endElement("tr");
		
		xos.startElement("tr");
		xos.writeElement("td","S");
		xos.writeElement("td","Low signal strength");
		xos.endElement("tr");
		
		xos.startElement("tr");
		xos.writeElement("td","F");
		xos.writeElement("td","Excessive frequency offset");
		xos.endElement("tr");
		
		xos.startElement("tr");
		xos.writeElement("td","X");
		xos.writeElement("td","Bad modulation index");
		xos.endElement("tr");
		
		xos.startElement("tr");
		xos.writeElement("td","V");
		xos.writeElement("td","Low battery voltage");
		xos.endElement("tr");
		
		xos.endElement("table");
	}


	private void writeFooter(XmlOutputStream xos)
		throws IOException
	{
		xos.writeElement("hr", null);
		xos.startElement("p", "class", "footer");
		xos.writeElement("a", "href", "/dcpmon/dcpmsghlp.html", "Help");
		DcpMonitorConfig dmc = DcpMonitorConfig.instance();
		if (dmc.agencyHomeDisplay != null && dmc.agencyHomeDisplay.length() > 0)
			xos.writeElement("a", "href", dmc.agencyHomeUrl, dmc.agencyHomeDisplay);
		xos.writeElement("br", 
			"Output generated: " + fullDateFmt.format(new Date()) + " (UTC)");
		xos.endElement("p");
	}
	
	
//	/** 
//	  Handles the detailed-performance-measurements 
//	  @param name the DCP name
//	  @param dcpAddress the DCP address
//	  @param numDays the number of days to look back
//	  @param oneDayOnly True if only want a single day's worth of data
//	*/
//	private void sendWazzupDcp(String name, DcpAddress dcpAddress, int numDays, 
//		boolean oneDayOnly)
//		throws IOException
//	{
//		Logger.instance().debug2(module + ":sendWazzupDcp starting.");
//		DcpMonitorConfig cfg = DcpMonitorConfig.instance();
//
//		int curday = getCurrentDay();
//		int startDay = curday - numDays;
//		int endDay = (oneDayOnly ? startDay : curday);
//
//		PlatformInfo platInfo = 
//			PlatformInfo.getPlatformInfo(null, dcpAddress, 0);
//		
//		if (platInfo != null)
//			output.write(
//				name + ":;" 
//				+ platInfo.platformDescription + ":;" //JP
//				+ dcpAddress.toString() + ":;"
//				+ formatSecOfDay(platInfo.firstXmitSecOfDay) + ":;"
//				+ platInfo.windowLength + ":;"
//				+ formatSecOfDay(platInfo.xmitInterval) + ":;"
//				+ platInfo.baud + ":;"
//				+ platInfo.preamble + "\n");
//		else
//		{
//			Logger.instance().debug1("No platform info for address "
//				+ dcpAddress.toString() + " -- mocking up dummy info.");
//			output.write(
//				name + ":;" 
//				+ "_" + ":;" //JP just put _ do not put addr as description
//				+ dcpAddress.toString() + ":;"
//				+ formatSecOfDay(0) + ":;"
//				+ "0" + ":;"
//				+ formatSecOfDay(0) + ":;"
//				+ 100 + ":;"
//				+ 'S' + "\n");
//		}
//
//		Logger.instance().debug1("wzd curday=" + curday + ", startDay=" + 
//				startDay + ", endDay=" + endDay);
//		for(int daynum = endDay; daynum >= startDay; daynum--)
//		{
//			Logger.instance().debug1(module + ":sendWazzupDcp getting data for " +
//					"day " + daynum);
//			ArrayList<XmitRecord> results = new ArrayList<XmitRecord>();
//			int n =	DcpMonitor.instance().readXmitsByDcpAddress(results, 
//					daynum, dcpAddress);
//			
//			//MJM-MISSING: Added the following line
//			n += addMissingXmitRecords(results, dcpAddress, daynum);
//			
//			if (n == 0)
//			{
//				Logger.instance().debug1(module + ":sendWazzupDcp no data for " +
//						"day " + daynum + " just skip it.");
//				//continue;
//			}
//			Logger.instance().debug3("SWZ sorting by time reverse order");
//			sortResults(results, SORT_BY_TIMEREV);
//
//			Logger.instance().debug3("SWZ iterating results");
//			for(XmitRecord xr : results)
//			{
//				String fc = xr.failureCodes();
//				if (fc.length() > 0 && fc.charAt(0) == 'M')
//				{
//					String ts = formatDateTime(daynum, xr.getSecOfDay(),
//							xr, xr.getCarrierStart());
//					output.write(
//						"R:" + ts + ":;" 
//						+ "R:" + fc + ":;"
//						+ "R:" + xr.getSignalStrength() + ":;"
//						+ checkMessageLen(xr.getMsgLength()) + ":;"
//						+ xr.getGoesChannel() + ":;"
//						+ "R:" + xr.getFreqOffset() + ":;"
//						+ checkModulationIndex(xr.getModIndex()) + ":;"
//						+ xr.getUpLinkCarrier() + ":;"
//						+ "N/A" + ":;"
//						+ formatSecOfDay(xr.getWindowStartSec()) + ":;"
//						+ xr.getWindowLength() + ":;"
//						+ "R:--:--:--" + ":;"
//						+ "missing\n");
//				}
//				else
//				{
//					boolean isRandom = 
//					XmitRecordFlags.testFlag(xr, XmitRecordFlags.IS_RANDOM);
//					String link = "" + daynum + "_" + dcpAddress.toString() + "_" + 
//													xr.getGoesTimeMsec();
//					output.write(
//					checkStartTime(daynum, xr, cfg, isRandom) + ":;"
//					+ checkFailureCodes(fc, cfg) + ":;"
//					+ checkSignalStrength(xr.getSignalStrength(), cfg) 
//																	+ ":;"
//					+ checkMessageLen(xr.getMsgLength()) + ":;"
//					+ xr.getGoesChannel() + ":;"
//					+ checkFreqOffset(xr.getFreqOffset(), cfg) + ":;"
//					+ checkModulationIndex(xr.getModIndex()) + ":;"
//					+ xr.getUpLinkCarrier() + ":;"
//					+ checkBattery(xr.getBattVolt(), cfg) + ":;"
//					+ (isRandom ? "N/A" : 
//								formatSecOfDay(xr.getWindowStartSec())) + ":;"
//					+ xr.getWindowLength() + ":;"
//					+ checkEndTime(xr, cfg, isRandom) + ":;"
//					+ link + "\n");
//				}
//			}
//		}
//		output.write("\n");
//		output.flush();
//		Logger.instance().debug1("SWZ done.");
//	}

	/** 
	  Handles the detailed-performance-measurements 
	  @param name the DCP name
	  @param dcpAddress the DCP address
	  @param daysAgo number of days ago to being retrieval (0=today, 1=yesterday) 
	  @param oneDayOnly True if only want a single day's worth of data
	*/
	private void sendWazzupDcpHtml(String name, DcpAddress dcpAddress, 
		int daysAgo, boolean oneDayOnly)
		throws IOException
	{
		String method = module + ":sendWazzupDcpHtml ";
		Logger.instance().debug2(method + "starting.");
		DcpMonitorConfig cfg = DcpMonitorConfig.instance();

		int curday = getCurrentDay();
		int startDay = curday - daysAgo;
		int endDay = (oneDayOnly ? startDay : curday);

		PlatformInfo platInfo = 
			PlatformInfo.getPlatformInfo(null, dcpAddress, 0);
		Logger.instance().info("WZDH DCP Address='" + dcpAddress
			+ "' desc=" + platInfo.platformDescription + "'");

		ByteArrayOutputStream baos = startByteArrayOutputStream();
		XmlOutputStream xos = startXmlOutputStream(baos, "DCP Full Status");
		xos.startElement("body");
		xos.writeElement("h1", "DCP Full Performance Parameters");
		xos.writeElement("div", "style", "text-align:center;",
			"UTC: " + fullDateFmt.format(new Date()));
		xos.writeElement("br", null);

		String desc = platInfo != null ? 
			(" ("+platInfo.platformDescription+")") : "";
		xos.writeElement("h2", name + desc);
		
		xos.startElement("table", "class", "headerTable");
		xos.startElement("tr");
		xos.writeElement("td", "DCP Address:");
		xos.writeElement("td", dcpAddress.toString());
		xos.endElement("tr");
		
		int X = platInfo != null ? platInfo.firstXmitSecOfDay : 0;
		xos.startElement("tr");
		xos.writeElement("td", "First transmission time:");
		xos.writeElement("td", formatSecOfDay(X));
		xos.endElement("tr");
		
		xos.startElement("tr");
		xos.writeElement("td", "Self-timed channel:");
		xos.writeElement("td", "" + platInfo.stchan);
		xos.endElement("tr");
		
		X = platInfo != null ? platInfo.xmitInterval : 0;
		xos.startElement("tr");
		xos.writeElement("td", "Transmission interval:");
		xos.writeElement("td", formatSecOfDay(X));
		xos.endElement("tr");

		X = platInfo != null ? platInfo.windowLength : 0;
		xos.startElement("tr");
		xos.writeElement("td", "Transmission window:");
		xos.writeElement("td", ""+X);
		xos.endElement("tr");
		
		char pre = platInfo != null ? platInfo.preamble : 'S';
		xos.startElement("tr");
		xos.writeElement("td", "Preamble:");
		xos.writeElement("td", ""+pre);
		xos.endElement("tr");

		X = platInfo != null ? platInfo.baud : 100;
		xos.startElement("tr");
		xos.writeElement("td", "Baud rate:");
		xos.writeElement("td", ""+X);
		xos.endElement("tr");
		xos.endElement("table");
		
		xos.startElement("table");
		xos.startElement("thead");
		xos.startElement("tr");
		xos.writeElement("th", "GOES channel");
		xos.writeElement("th", "Date");
		xos.startElement("th");
		xos.writeLiteral("Transmit<br>start");
		xos.endElement("th");
		xos.startElement("th");
		xos.writeLiteral("Transmit<br>end");
		xos.endElement("th");
		xos.writeElement("th", "Window start");
		xos.writeElement("th", "Window end");
		xos.writeElement("th", "Failure code");
		xos.writeElement("th", "Signal strength");
		xos.writeElement("th", "Message length");
		xos.writeElement("th", "Frequency offset");
		xos.writeElement("th", "Modulation index");
		xos.writeElement("th", "DRGS code");
		xos.writeElement("th", "Battery voltage");
		xos.endElement("tr");
		xos.endElement("thead");

		Logger.instance().debug1("wzd curday=" + curday + ", startDay=" + 
				startDay + ", endDay=" + endDay);
		for(int daynum = endDay; daynum >= startDay; daynum--)
		{
			Logger.instance().debug1(module + ":sendWazzupDcp getting data for " +
					"day " + daynum);
			ArrayList<XmitRecord> results = new ArrayList<XmitRecord>();
			int n =	DcpMonitor.instance().readXmitsByDcpAddress(results, 
					daynum, dcpAddress);

			//MJM-MISSING: Added the following line
			n += addMissingXmitRecords(results, dcpAddress, daynum);

			if (n == 0)
			{
				Logger.instance().debug1(module + ":sendWazzupDcp no data for " +
						"day " + daynum + " just skip it.");
				//continue;
			}
			Logger.instance().debug3("SWZ sorting by time reverse order");
			sortResults(results, SORT_BY_TIMEREV);

			Logger.instance().debug3("SWZ iterating results");
			for(XmitRecord xr : results)
			{
				String fc = xr.failureCodes();

				xos.startElement("tr", "class", "full_perf_report");
				if (fc.contains("W"))
				{
					xos.startElement("td");
					xos.writeElement("span", "class", "alarm",
						""+xr.getGoesChannel());
					xos.endElement("td");
				}
				else
					xos.writeElement("td", ""+xr.getGoesChannel());

				xos.writeElement("td", "class", "date", 
					dateFmt.format(xr.getCarrierStart()));
				
				if (fc.length() > 0 && fc.charAt(0) == 'M')
				{
					xos.startElement("td", "class", "time");
					xos.writeElement("span", "class", "alarm", "--:--:--");
//						timeFmt.format(new Date(
//							(daynum*24*60*60 + xr.getSecOfDay()) * 1000L)));
					xos.endElement("td");
					xos.startElement("td", "class", "time");
					xos.writeElement("span", "class", "alarm", "--:--:--");
					xos.endElement("td");
					xos.writeElement("td", "class", "time",
						formatSecOfDay(xr.getWindowStartSec()));
					xos.writeElement("td", "class", "time",
						formatSecOfDay(xr.getWindowStartSec()
							+ xr.getWindowLength()));
					xos.startElement("td");
					xos.writeElement("span", "class", "alarm", "M");
					xos.endElement("td");
					xos.startElement("td");
					xos.writeElement("span", "class", "alarm", "0"); //sigstrength
					xos.endElement("td");
					xos.writeElement("td", "0");   // length
					xos.writeElement("td", "N/A"); // freq offset
					xos.writeElement("td", "N/A"); // mod index
					xos.writeElement("td", "--");  // drgs code
					xos.writeElement("td", "N/A"); // battery
				}
				else
				{
					boolean isRandom = 
						XmitRecordFlags.testFlag(xr, XmitRecordFlags.IS_RANDOM);
					String link = "msg-html.cgi?msgfilename=" 
						+ daynum + "_" + dcpAddress.toString() + "_" + 
						xr.getGoesTimeMsec();
					
					int sod =
						(int)((xr.getCarrierStart() % RecentDataStore.MSEC_PER_DAY) / 1000L);
					int window = xr.getWindowStartSec();

					String dt = "";
					if ((xr.getFlags() & XmitRecordFlags.CARRIER_TIME_MSEC) != 0)
					{
						dt = timeFmtSSS.format(new Date(xr.getCarrierStart()));
						dt = extraResolution(xr.getCarrierStart(), dt);
					}
					else
						dt = timeFmt.format(new Date(xr.getCarrierStart()));

					// No window checking on random messages
					String cls = null;
					if (!isRandom)
					{
						int offset = sod - window;
						if (offset < cfg.redMsgTime)
							cls="alarm";
						else if (offset < cfg.yellowMsgTime)
							cls="warning";
					}
					xos.startElement("td", "class", "time");
					xos.startElement("a", "href", link);
					if (cls != null)
						xos.writeElement("span", "class", cls, dt);
					else
						xos.writeElement("span", dt);
					xos.endElement("a");
					xos.endElement("td");
					
					int endSOD = 
						(int)((xr.getCarrierEnd() % RecentDataStore.MSEC_PER_DAY) / 1000L);
					dt = "";
					if ((xr.getFlags() & XmitRecordFlags.CARRIER_TIME_MSEC) != 0)
					{
						dt = timeFmtSSS.format(new Date(xr.getCarrierEnd()));
						dt = extraResolution(xr.getCarrierEnd(), dt);
					}
					else
						dt = timeFmt.format(new Date(xr.getCarrierEnd()));

					cls = null;
					if (!isRandom)
					{
						window = xr.getWindowStartSec();
						int len = xr.getWindowLength();

						int offset = (window+len) - endSOD;
						if (offset < cfg.redMsgTime)
							cls = "alarm";
						else if (offset < cfg.yellowMsgTime)
							cls = "warning";
					}
					xos.startElement("td", "class", "time");
					if (cls != null)
						xos.writeElement("span", "class", cls, dt);
					else
						xos.writeElement("span", dt);
					xos.endElement("td");
					
					xos.writeElement("td", 
						(isRandom ? "N/A" : 
						formatSecOfDay(xr.getWindowStartSec())));
					
					xos.writeElement("td", 
						(isRandom ? "N/A" : 
						formatSecOfDay(
							xr.getWindowStartSec()+xr.getWindowLength())));

					cls = null;
					String xx = checkFailureCodes(fc, cfg);
					if (xx.startsWith("R:"))
					{
						cls="alarm";
						xx = xx.substring(2);
					}
					else if (xx.startsWith("Y:"))
					{
						cls="warning";
						xx = xx.substring(2);
					}
					if (cls != null)
					{
						xos.startElement("td");
						xos.writeElement("span", "class", cls, xx);
						xos.endElement("td");
					}
					else
						xos.writeElement("td", xx);
					
					xx = checkSignalStrength(xr.getSignalStrength(), cfg);
					cls = null;
					if (xx.startsWith("R:"))
					{
						cls="alarm";
						xx = xx.substring(2);
					}
					else if (xx.startsWith("Y:"))
					{
						cls="warning";
						xx = xx.substring(2);
					}
					if (cls != null)
					{
						xos.startElement("td");
						xos.writeElement("span", "class", cls, xx);
						xos.endElement("td");
					}
					else
						xos.writeElement("td", xx);
					
					if (xr.getMsgLength() == 0)
					{
						xos.startElement("td");
						xos.writeElement("span", "class", "alarm", "0");
						xos.endElement("td");
					}
					else
						xos.writeElement("td", ""+ xr.getMsgLength());
					
					xx = checkFreqOffset(xr.getFreqOffset(), cfg);
					cls = null;
					if (xx.startsWith("R:"))
					{
						cls="alarm";
						xx = xx.substring(2);
					}
					else if (xx.startsWith("Y:"))
					{
						cls="warning";
						xx = xx.substring(2);
					}
					if (cls != null)
					{
						xos.startElement("td");
						xos.writeElement("span", "class", cls, xx);
						xos.endElement("td");
					}
					else
						xos.writeElement("td", xx);

					xx = checkModulationIndex(xr.getModIndex());
					cls = null;
					if (xx.startsWith("R:"))
					{
						cls="alarm";
						xx = xx.substring(2);
					}
					else if (xx.startsWith("Y:"))
					{
						cls="warning";
						xx = xx.substring(2);
					}
					if (cls != null)
					{
						xos.startElement("td");
						xos.writeElement("span", "class", cls, xx);
						xos.endElement("td");
					}
					else
						xos.writeElement("td", xx);

					xos.writeElement("td", xr.getUpLinkCarrier());
					
					xx = checkBattery(xr.getBattVolt(), cfg);
					cls = null;
					if (xx.startsWith("R:"))
					{
						cls="alarm";
						xx = xx.substring(2);
					}
					else if (xx.startsWith("Y:"))
					{
						cls="warning";
						xx = xx.substring(2);
					}
					if (cls != null)
					{
						xos.startElement("td");
						xos.writeElement("span", "class", cls, xx);
						xos.endElement("td");
					}
					else
						xos.writeElement("td", xx);
				}
				xos.endElement("tr");
			}
		}
		xos.endElement("table");
		writeLegend(xos, false);
		writeFooter(xos);
		xos.endElement("body");
		xos.endElement("html");
		output.write(baos.toString());
		output.write("HTML END\n");
		output.flush();
Logger.instance().debug1("SWZH done.");
	}
	
	
	/**
	  Formats & returns an error response to the client.
	  @param msg to be included in the error response
	*/
	private void errorResponse(String msg)
		throws IOException
	{
		output.write("-Error: " + msg + "\n\n");
		output.flush();
	}

	/**
	  Sends a status report back to the client.
	*/
	private void sendStatus()
		throws IOException
	{
		ArrayList<StringPair> status = new ArrayList<StringPair>();
//		RecentDataStore.instance().setStatus(status);
		((DcpMonitorServer)parent).setStatus(status);
		for(StringPair sp : status)
			output.write(sp.first + "=" + sp.second + "\n");
		output.write("\n");
		output.flush();
	}

	/** Sends the channel map to the client. */
	private void sendChannelMap()
		throws IOException
	{
		ChannelMap cmap = ChannelMap.instance();
		cmap.dumpMap(output);
		output.write("\n");
		output.flush();
	}

	/**
	  Sorts a results vector according to user parameters:
	  <ul>
		<li>sortOrder==B: sort by [basin, name, channel]</li>
		<li>sortOrder==A: sort by [addr, chan]</li>
		<li>sortOrder==C: sort by [chan, name]</li>
		<li>sortOrder==T: sort by [firstSecOfDay]</li>
	  </ul>
	*/
	private void sortResults(List<XmitRecord> resultsVec, char sortOrder)
	{
		Comparator cmp;
//		if (sortOrder == SORT_BY_BASIN)
//			cmp = new BasinComparator();
//		if (sortOrder == SORT_BY_CHANNEL)
//			cmp = new ChannelComparator();
		if (sortOrder == SORT_BY_ADDRESS)
			cmp = new AddressComparator();
//		else if (sortOrder == SORT_BY_NAME)
//			cmp = new NameComparator();
		else if (sortOrder == SORT_BY_TIME)
			cmp = new TimeComparator(true);
		else // (sortOrder == SORT_BY_TIMEREV)
			cmp = new TimeComparator(false);

		Collections.sort(resultsVec, cmp);
	}


	/**
	  Formats day number and secOfDay into printable date/time.
	  @param daynum the day number, Jan 1, 1970 = day 0.
	  @param secOfDay 0 = midnight
	  @return formated date/time string.
	*/
//	private String formatDateTime(int daynum, int secOfDay, XmitRecord xr,
//			long time)
//	{
//		//Flags will be used when displaying Transmit Start and Transmit End
//		//If we have millisecond resolution display it
//		if ((xr.getFlags() & XmitRecordFlags.CARRIER_TIME_MSEC) != 0)
//		{
//			String dt = dateTimeFmtSSS.format(new Date(time));
//			dt = extraResolution(time, dt);
//			return dt;
//		}
//		else
//		{
//			return dateTimeFmt.format(
//				new Date(((daynum*24*60*60) + secOfDay) * 1000L));				
//		}
//	}

	/**
	 * Round the extra resolution to the nearest tenth. 
	 * HH:mm:ss.x So 14:57:08.761 ==> 14:57:08.8
	 * Now 14:57:08.999 ==> 14:57:08.9
	 * @param time
	 * @param dt
	 * @return
	 */
	public static String extraResolution(long time, String dt)
	{
		String newTime = dt;
		try 
		{
			int milliseconds = (int)(time % 1000);
			String decMilli = "." + milliseconds;
			DecimalFormat decF = new DecimalFormat(".#");
			String nearestTenth = 
						decF.format(new Double(decMilli).doubleValue());
			
			if (new Double(nearestTenth).doubleValue() >= 1.0)
			{	//Just get the first digit of the milliseconds
				if (dt.lastIndexOf(".") != -1)
					newTime = dt.substring(0, dt.lastIndexOf(".")+2);
			}
			else
			{
				if (dt.lastIndexOf(".") != -1)
				{
					newTime = 
						(dt.substring(0, dt.lastIndexOf("."))) + nearestTenth;
				}
			}
			//System.out.println("decMilli = " + decMilli);
			//System.out.println("dec milli format = " + nearestTenth);
		} catch (Exception ex)
		{
			Logger.instance().warning(module + " extraResolution error on" +
					" date/time :" + dt);
			newTime = dt;
		}
		
		return newTime;
	}
	
	/**
	  Format date from day number.
	  @param daynum the day number, Jan 1, 1970 = day 0.
	  @return formated date string.
	*/
	private String formatDate(int daynum)
	{
		return dateFmt.format(new Date(((daynum*24*60*60)) * 1000L));
	}

	/**
	  Format time from secOfDay.
	  @param sod (second of day) 0 = midnight
	  @return formated time string.
	*/
	private String formatSecOfDay(int sod)
	{
		if (sod == -1)
			sod = 0;
		return timeFmt.format(new Date(sod * 1000L));
	}

	/**
	  Sorts Message Status Vector.
	*/
	private void sortMsgStatVec(List<MsgStatRecord> msgStatVec, char sortOrder)
	{
		Comparator cmp;
//		if (sortOrder == SORT_BY_BASIN)
//			cmp = new MsgStatBasinComparator();
		if (sortOrder == SORT_BY_CHANNEL)
			cmp = new MsgStatChannelComparator();
		else if (sortOrder == SORT_BY_ADDRESS)
			cmp = new MsgStatAddressComparator();
//		else if (sortOrder == SORT_BY_NAME)
//			cmp = new MsgStatNameComparator();
		else // SORT_BY_TIME
			cmp = new MsgStatTimeComparator();

		Collections.sort(msgStatVec, cmp);
	}

	/**
	  Checks start time against red/yellow limits.
	*/
//	private String checkStartTime(int daynum, XmitRecord xr, 
//		DcpMonitorConfig cfg, boolean isRandom)
//	{
//		int sod =
//			(int)((xr.getCarrierStart() % RecentDataStore.MSEC_PER_DAY) / 1000L);
//		int window = xr.getWindowStartSec();
//
//		String dt = "";
//		if ((xr.getFlags() & XmitRecordFlags.CARRIER_TIME_MSEC) != 0)
//		{
//			dt = dateTimeFmtSSS.format(new Date(xr.getCarrierStart()));
//			dt = extraResolution(xr.getCarrierStart(), dt);
//		}
//		else
//		{
//			dt = dateTimeFmt.format(new Date(xr.getCarrierStart()));
//		}
//
//		// No window checking on random messages
//		if (isRandom)
//			return dt;
//
//		//int offset = (int)(xr.getCarrierStart() - (window * 1000L));
//		int offset = sod - window;
//		if (offset < cfg.redMsgTime)
//			return "R:" + dt;
//		else if (offset < cfg.yellowMsgTime)
//			return "Y:" + dt;
//		else
//			return dt;
//	}

	/**
	  Checks failure code against red/yellow limits.
	*/
	private String checkFailureCodes(String codes, DcpMonitorConfig cfg)
	{
		for(int i=0; i<cfg.redFailureCodes.length(); i++)
			if (codes.indexOf(cfg.redFailureCodes.charAt(i)) != -1)
				return "R:" + codes;
		for(int i=0; i<cfg.yellowFailureCodes.length(); i++)
			if (codes.indexOf(cfg.yellowFailureCodes.charAt(i)) != -1)
				return "Y:" + codes;
		return codes;
	}
	
	

	/**
	  Checks message Length against red limits.
	*/
//	private String checkMessageLen(int msgLen)
//	{
//		if (msgLen == 0)
//			return "R:" + msgLen;
//		else
//			return "" + msgLen;
//	}
	
	/**
	  Checks modulation index against red limits.
	*/
	private String checkModulationIndex(char modIndex)
	{
		if (modIndex == '?')
			return "R:" + modIndex;
		else
			return "" + modIndex;
	}
	
	/**
	  Checks signal strength against red/yellow limits.
	*/
	private String checkSignalStrength(int ss, DcpMonitorConfig cfg)
	{
		if (ss < cfg.redSignalStrength)
			return "R:" + ss;
		else if (ss < cfg.yellowSignalStrength)
			return "Y:" + ss;
		else
			return "" + ss;
	}

	/**
	  Checks frequencey offset against red/yellow limits.
	*/
	private String checkFreqOffset(int fo, DcpMonitorConfig cfg)
	{
		int afo = Math.abs(fo);
		if (afo > cfg.redFreqOffset)
			return "R:" + fo;
		else if (afo > cfg.yellowFreqOffset)
			return "Y:" + fo;
		else
			return "" + fo;
	}

	/**
	  Checks battery voltage against red/yellow limits.
	*/
	private String checkBattery(double bv, DcpMonitorConfig cfg)
	{
		if (bv < 0.1 && bv > -0.1)
			return "N/A";
		String bvs = bvFormat.format(bv);
		if (bv < cfg.redBattery)
			return "R:" + bvs;
		else if (bv < cfg.yellowBattery)
			return "Y:" + bvs;
		else
			return "" + bvs;
	}

	/**
	  Checks end time against red/yellow limits.
	*/
//	private String checkEndTime(XmitRecord xr, DcpMonitorConfig cfg, 
//		boolean isRandom)
//	{
//		int endSOD = 
//			(int)((xr.getCarrierEnd() % RecentDataStore.MSEC_PER_DAY) / 1000L);
//		String tm = "";
//		if ((xr.getFlags() & XmitRecordFlags.CARRIER_TIME_MSEC) != 0)
//		{
//			tm = timeFmtSSS.format(new Date(xr.getCarrierEnd()));
//			tm = extraResolution(xr.getCarrierEnd(), tm);
//		}
//		else
//		{
//			tm = timeFmt.format(new Date(xr.getCarrierEnd()));
//		}
//
//		if (isRandom)
//		{
//			return tm;
//		}
//
//		int window = xr.getWindowStartSec();
//		int len = xr.getWindowLength();
//
//		//int offset = (int)(((window+len)*1000L) - xr.getCarrierEnd());
//		int offset = (window+len) - endSOD;
//		if (offset < cfg.redMsgTime)
//		{
//			return "R:" + tm;
//		}
//		else if (offset < cfg.yellowMsgTime)
//		{
//			return "Y:" + tm;
//		}
//		else
//		{
//			return tm;
//		}
//	}

	/**
	 * This method is now used to display the DRGS Receiver Info
	 * @param relpath
	 */
	private void sendDrgsList()
	{
		BufferedReader br = null;

		File htmlFile = null;
		
		// This is $DECODES_INSTALL_DIR/drgsident/drgsident.html
		Logger.instance().debug1("Sending DRGS Ident File '" + 
			DrgsReceiverIo.drgsRecvHtml + "'");

		htmlFile = new File(DrgsReceiverIo.drgsRecvHtml);
		try
		{
			if (!htmlFile.canRead())
				output.write("No such file '" + htmlFile.getPath() + "'\n");
			else
			{
				br = new BufferedReader(new FileReader(htmlFile));
				String s;
				while((s = br.readLine()) != null)
					output.write(s + "\n");
			}
			output.write("MHEND\n");
			output.flush();
		}
		catch(IOException ex)
		{
			try { errorResponse("IOError reading '" 
				+ htmlFile.getPath() + "': " + ex); }
			catch(IOException ex2) {}
		}
		finally
		{
			if (br != null)
				try { br.close(); } catch(IOException ex) {}
		}
	}

	/**
	 * Decodes a raw message on real time.
	 * 
	 * @param dayNum
	 * @param addr
	 * @param timestamp
	 * @param dcpAddr
	 */
	private void sendDecodedMsgHtml(int dayNum, DcpAddress dcpAddress, 
		Date timestamp)
	{
		try
		{
			//Find the raw msg for this transmission
			XmitRecord xr = 
				DcpMonitor.instance().readXmitRawMsg(dayNum, dcpAddress,
					timestamp);

			if (xr != null)
			{
				String htmlOutput = DcpDecodeMsg.decodeData(xr,
						timestamp, dcpAddress);
				output.write(htmlOutput + "\n");
			}
			else
			{
				output.write("Did not find raw message for dcp address: " +
					dcpAddress + " with timestamp: " + 
						dateTimeFmt.format(timestamp) + "\n");
			}
			output.write("MHEND\n");
			output.flush();
		} catch (IOException ex)
		{
			try { errorResponse("IOError decoding '" + dcpAddress + "': " + ex); }
			catch(IOException ex2) {}
		}	
	}

	private void sendMetaData(String addrs)
	{
		try
		{
			PlatformList pl = Database.getDb().platformList;
			Platform p = pl.getPlatform(Constants.medium_Goes, addrs, null);
			if (p == null)
			{
				errorResponse("No platform with DCP address '" + addrs + "'");
				return;
			}

			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			XmlOutputStream xos = new XmlOutputStream(baos,
				XmlDbTags.Platform_el);
			xos.writeXmlHeader();
			PlatformParser pp = new PlatformParser(p);
			pp.writeXml(xos);
			output.write(baos.toString());
			output.write("\nMDEND\n");
			output.flush();
		}
		catch(DatabaseException dbex)
		{
			try 
			{
				errorResponse("No platform with DCP address '" + addrs + "': "
					+ dbex);
			}
			catch(IOException ex2)
			{
				Logger.instance().warning("IOError in sendMetaData: " + ex2);
			}
			return;
		}
		catch(IOException ex)
		{
			Logger.instance().warning("IOError in sendMetaData: " + ex);
		}
	}

//	private void sendChannelExpand(int daynum, int chan, String groupName)
//	{
//		Logger.instance().debug1(module + ":sendChannelExpand starting.");
//		DcpNameDescResolver namer = 
//			DcpMonitor.instance().getDcpNameDescResolver();
//		DcpGroupList dgl = DcpGroupList.instance();
//
//		try
//		{
//			ArrayList<XmitRecord> channelExpandResults
//				= new ArrayList<XmitRecord>();
//			int n = DcpMonitor.instance().readXmitsByChannel(
//				channelExpandResults, daynum, chan);
//			if (n == 0)
//			{
//				Logger.instance().debug1("No records for day number "
//						+ daynum + " in storage.");
//			}
//			Logger.instance().debug1(module + ":sendChannelExpand Sorting " + 
//				n + " results by address");
//			sortResults(channelExpandResults, SORT_BY_ADDRESS);
//			ArrayList<MsgStatRecord> msVec = new ArrayList<MsgStatRecord>();
//			DcpAddress lastAddr = null;
//			MsgStatRecord msr = null;
//// group stuff is different from MsgStat
//			DcpGroup grp = groupName == null ? null : dgl.getGroup(groupName);
//			Logger.instance().debug1(module + ":sendChannelExpand iterating " +
//				"through resultsVec, grp="
//				+ (grp==null ? "null" : grp.groupName));
////
//			ArrayList<XmitRecord> unexpectedXRs = new ArrayList<XmitRecord>();
//			for(XmitRecord xr : channelExpandResults)
//			{
//				if (lastAddr == null
//				 || !xr.getDcpAddress().equals(lastAddr))
//				{
//					msr = new MsgStatRecord(daynum);
//					msr.dcpAddress = lastAddr = xr.getDcpAddress();
//					if (grp != null)
//					{
//						msr.dcpName = grp.getDcpName(msr.dcpAddress);
//						msr.isMine = (msr.dcpName != null);
//					}
//					//DCP Monitor Enhacement Problem # 2
//					//Set the dcpname based on the rules
//					Platform p;
//					try
//					{
//						p = Database.getDb().platformList.getPlatform(
//							Constants.medium_GoesST, msr.dcpAddress.toString());
//
//						msr.dcpName = namer.getBestName(msr.dcpAddress, p);
//					} catch (DatabaseException ex)
//					{
//						Logger.instance().warning(
//						"dcpMonitorServerThread:" +
//						"SendMessageStatus Exception reading platform: " + ex);
//						ex.printStackTrace();
//					}
//					msr.firstMsgSecOfDay = xr.firstXmitSecOfDay;
//					msr.goesChannel = xr.getGoesChannel();
//					msr.basin = xr.basin;
//
//					msVec.add(msr);
//				}
//				int hr = xr.getSecOfDay() / 3600;
//				String fc = xr.failureCodes();
//				msr.fcodes[hr] = fc;
//				if (xr.hasFailureCode('U'))
//					unexpectedXRs.add(xr);
//			}
//			DcpMonitorConfig cfg = DcpMonitorConfig.instance();
//			Pdt pdt = Pdt.instance();
//			for(MsgStatRecord msre : msVec)
//			{
//				PdtEntry pte = pdt.find(msre.dcpAddress);
//				//JP July 26,07 for testing how to add NESDIS user name
//				if (pte != null)
//				{
//					msre.firstMsgSecOfDay = pte.st_first_xmit_sod;
//					msre.agency = pte.agency;//JP - July 26,07
//				}
//			}
//			
//			addUnexpecteds(msVec, unexpectedXRs, daynum);
//			sortMsgStatVec(msVec, SORT_BY_CHANNEL);
//StringBuffer mytest = new StringBuffer();
//			output.write("T " + formatDate(daynum) + "\n");
//mytest.append("T " + formatDate(daynum) + "\n");
//			output.write("C " + chan + "\n");
//mytest.append("C " + chan + "\n");
//			//output.write("B " + "Channel " + chan + "\n");
//
//			for(MsgStatRecord msre : msVec)
//			{
//				String str = "D:;" + (msre.isMine ? "G:" : "")
//					+ msre.dcpAddress.toString()
//					+ ":;" + msre.dcpName 
//					+ ":;" + msre.agency
//					+ ":;" + (msre.isUnexpected ? "UNEXPECT" :
//						formatSecOfDay(msre.firstMsgSecOfDay));
//
//				output.write(str);
//mytest.append(str);
//				for(int i=0; i<24; i++)
//				{
//					output.write(":;" + msre.fcodes[i]);
//mytest.append(":;" + msre.fcodes[i]);
//				}
//				output.write("\n");
//mytest.append("\n");
//			}
//			output.write("\n");
//mytest.append("\n");
//Logger.instance().debug1("str = ***" + mytest.toString() + "***");
//			output.flush();
//Logger.instance().debug1("CE done.");
//		}
//		catch(IOException ex)
//		{
//			Logger.instance().warning("IOError in sendChannelExpand: " + ex);
//			return;
//		}
//	}

	private ByteArrayOutputStream startByteArrayOutputStream()
		throws IOException
	{
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
        String dtdHeader =
"<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 4.01//EN\" \"http://www.w3.org/TR/html4/strict.dtd\">\n";
        baos.write(dtdHeader.getBytes());
		return baos;
	}

	private XmlOutputStream startXmlOutputStream(ByteArrayOutputStream baos,
		String title)
		throws IOException
	{
		XmlOutputStream xos = new XmlOutputStream(baos, null);
		xos.startElement("html");
		xos.startElement("head");
		xos.writeElement("meta", "http-equiv", "Content-Type",
			"content", "text/html; charset=iso-8859-1", null);
		xos.writeElement("title", title);
		StringPair attrs[] = new StringPair[3];
		attrs[0] = new StringPair("rel", "stylesheet");
		attrs[1] = new StringPair("href", "/dcpmon/dcpmon.css");
		attrs[2] = new StringPair("type", "text/css");
		xos.writeElement("LINK", attrs, null);
		xos.endElement("head");
		return xos;
	}
	
	private void addMsgStatDayHeading(XmlOutputStream xos, String group, 
		String longDate)
		throws IOException
	{
		xos.writeElement("caption", group + " for " + longDate);
		xos.writeElement("colgroup",null);
		xos.writeElement("colgroup",null);
		xos.writeElement("colgroup",null);
		xos.writeElement("colgroup",null);
		xos.writeElement("colgroup", "span", "24", null);

		xos.startElement("thead");
		xos.startElement("tr");
		xos.startElement("th", "rowspan", "2");
		xos.writeLiteral("DCP<br>address");
		xos.endElement("th");
		xos.startElement("th", "rowspan", "2");
		xos.writeLiteral("DCP<br>name");
		xos.endElement("th");
		xos.writeElement("th", "rowspan", "2", "Agency");
		xos.startElement("th", "rowspan", "2");
		xos.writeLiteral("First xmit<br>time");
		xos.endElement("th");
		xos.writeElement("th", "colspan", "24", 
			"Failure codes by hour of transmission");
		xos.endElement("tr");
		xos.startElement("tr");
		for(int x=0; x<24; x++)
			xos.writeElement("th", ""+x);
		xos.endElement("tr");
		xos.endElement("thead");
	}

	private void addMsgStatLine(XmlOutputStream xos, MsgStatRecord msrt,
		int daynum)
		throws IOException
	{
		//MJM-MISSING:
		addMissingIndicators(msrt);

		xos.startElement("tr", "class", "msg_status_report");
		xos.writeElement("td", msrt.dcpAddress.toString());
		xos.startElement("td");
		xos.writeElement("a", "href",
			"dcpfullouthtml.cgi?select_option=dcp_text&dcp_text=" + msrt.dcpName
			+ "&dcpaddr=" + msrt.dcpAddress.toString()
			+ "&date_range=" + dateFmt.format(daynumToDate(daynum)),
			msrt.dcpName);
		xos.endElement("td");
		xos.writeElement("td", msrt.agency);
		xos.writeElement("td", (msrt.isUnexpected ? "UNEXPECT" :
				formatSecOfDay(msrt.firstMsgSecOfDay)));
		
		for(int i=0; i<24; i++)
		{
			StringBuilder fc = new StringBuilder(msrt.fcodes[i]);
			if (fc.length() == 0 || fc.charAt(0) == '.')
				fc.setCharAt(0, ' ');
			else for(int x=0; x<fc.length(); x++)
				if (fc.charAt(x) == 'G')
					fc.setCharAt(x, '_');
			xos.writeElement("td", "class", "failureCode", fc.toString());
		}
		xos.endElement("tr");
	}
	
	/**
	 * MJM-MISSING: This method is called just before display of a 
	 * msg-stat record. It adds any 'M' indicators based on the current
	 * PDT.
	 */
	private void addMissingIndicators(MsgStatRecord msr)
	{
		Pdt pdt = Pdt.instance();
		PdtEntry pte = pdt.find(msr.dcpAddress);
		if (pte == null)
			return;
		if (msr.goesChannel != pte.st_channel
		 || pte.st_xmit_interval <= 0)
			return;
		
		long now = System.currentTimeMillis();
		for(int sod = pte.st_first_xmit_sod; sod < 24*3600; 
			sod += pte.st_xmit_interval)
		{
			long t = msr.daynum * MSEC_PER_DAY 
				+ (sod+pte.st_xmit_window+30)*1000L;
			if (t > now)
				break;
			int hr = sod/3600;
			String fcodes = msr.fcodes[hr];
			if (fcodes == null || fcodes.length() == 0 
				|| fcodes.equals("."))
				msr.setCodes(hr, "M");
		}
	}
	
	/**
	 * MJM-MISSING: Called just prior to displaying the xmit records
	 * for the Wazzup display. It addes 'M' xmit records based on the
	 * current PDT.
	 * @return
	 */
	private int addMissingXmitRecords(ArrayList<XmitRecord> results, 
		DcpAddress dcpAddress, int dayNum)
	{
		Pdt pdt = Pdt.instance();
		PdtEntry pte = pdt.find(dcpAddress);
		if (pte == null)
			return 0;
		if (pte.st_xmit_interval <= 0)
			return 0;
		
		int numAdded = 0;
		long now = System.currentTimeMillis();

		for(int sched_sod = pte.st_first_xmit_sod; sched_sod < 24*3600; 
			sched_sod += pte.st_xmit_interval)
		{
			long t = dayNum * MSEC_PER_DAY 
				+ (sched_sod+pte.st_xmit_window+30)*1000L;
			if (t > now)
				break;

			boolean found = false;
			for(XmitRecord xr : results)
			{
				if (xr.hasFailureCode('M'))
					break;
				int sod = xr.getSecOfDay();
				int dt = sod - sched_sod;
				if (dt > -180 && dt < 180)
				{
					found = true;
					break;
				}
			}
			if (found) // no msg within 3 min of schedule.
				continue;
			XmitRecord xr = new XmitRecord(dcpAddress, sched_sod, dayNum);
			xr.addCode('M');
			xr.setGoesChannel(pte.st_channel);
			xr.setWindowStartSec(sched_sod);
			xr.setWindowLength(pte.st_xmit_window);
			xr.setCarrierStart(dayNum*MSEC_PER_DAY + sched_sod*1000L);
			numAdded++;
			results.add(xr);
		}
		return numAdded;
	}

	private void sendChannelExpandHtml(int daynum, int chan, String groupName)
		throws IOException
	{
		String method = module + ":sendChannelExpand ";
		Logger.instance().debug1(method + "starting.");
		DcpNameDescResolver namer = 
			DcpMonitor.instance().getDcpNameDescResolver();
		DcpGroupList dgl = DcpGroupList.instance();

		ArrayList<XmitRecord> results = new ArrayList<XmitRecord>();
		int n = DcpMonitor.instance().readXmitsByChannel(results, daynum, chan);
		if (n == 0)
		{
			Logger.instance().debug1("No records for day number "
					+ daynum + " in storage.");
		}
		sortResults(results, SORT_BY_ADDRESS);
		ArrayList<MsgStatRecord> msVec = new ArrayList<MsgStatRecord>();
		DcpAddress lastAddr = null;
		MsgStatRecord msr = null;

		ByteArrayOutputStream baos = startByteArrayOutputStream();
		XmlOutputStream xos = startXmlOutputStream(baos, "DCP Message Status");
		xos.startElement("body");
		xos.writeElement("h1", "class", "msg_status", "DCP Message Status");
		xos.writeElement("div", "style", "text-align:center;",
			"UTC: " + fullDateFmt.format(new Date()));
		xos.writeElement("br", null);

		DcpGroup grp = groupName == null ? null : dgl.getGroup(groupName);
		Logger.instance().debug1(module + ":sendChannelExpand iterating " +
			"through resultsVec, grp="
			+ (grp==null ? "null" : grp.groupName));

		ArrayList<XmitRecord> unexpectedXRs = new ArrayList<XmitRecord>();
		for(XmitRecord xr : results)
		{
			if (lastAddr == null
			 || !xr.getDcpAddress().equals(lastAddr))
			{
				msr = new MsgStatRecord(daynum);
				msr.dcpAddress = lastAddr = xr.getDcpAddress();
				if (grp != null)
				{
					msr.dcpName = grp.getDcpName(msr.dcpAddress);
					msr.isMine = (msr.dcpName != null);
				}
				//DCP Monitor Enhacement Problem # 2
				//Set the dcpname based on the rules
				Platform p;
				try
				{
					p = Database.getDb().platformList.getPlatform(
						Constants.medium_GoesST, msr.dcpAddress.toString());

					msr.dcpName = namer.getBestName(msr.dcpAddress, p);
				} catch (DatabaseException ex)
				{
					Logger.instance().warning(
					"dcpMonitorServerThread:" +
					"SendMessageStatus Exception reading platform: " + ex);
					ex.printStackTrace();
				}
				msr.firstMsgSecOfDay = xr.firstXmitSecOfDay;
				msr.goesChannel = xr.getGoesChannel();
				msr.basin = xr.basin;

				msVec.add(msr);
			}
			int hr = xr.getSecOfDay() / 3600;
			String fc = xr.failureCodes();
			msr.fcodes[hr] = fc;
			if (xr.hasFailureCode('U'))
				unexpectedXRs.add(xr);
		}
//		DcpMonitorConfig cfg = DcpMonitorConfig.instance();
		Pdt pdt = Pdt.instance();
		for(MsgStatRecord msre : msVec)
		{
			PdtEntry pte = pdt.find(msre.dcpAddress);
			if (pte != null)
			{
				msre.firstMsgSecOfDay = pte.st_first_xmit_sod;
				msre.agency = pte.agency;
			}
		}
		
		addUnexpecteds(msVec, unexpectedXRs, daynum);
		sortMsgStatVec(msVec, SORT_BY_CHANNEL);


		Logger.instance().debug1("SMSH printing output.");
		xos.startElement("table");
		String longDate = longDateFmt.format(daynumToDate(daynum));
		addMsgStatDayHeading(xos, "Channel " + chan, longDate);

		for(MsgStatRecord msre : msVec)
			addMsgStatLine(xos, msre, daynum);
		xos.endElement("table");
		writeLegend(xos, true);
		writeFooter(xos);
		xos.endElement("body");
		xos.endElement("html");
		output.write(baos.toString());
		output.write("HTML END\n");
		output.flush();
Logger.instance().debug1("CEH done.");
	}
	
	public static int getCurrentDay()
	{
		return msecToDay(System.currentTimeMillis());
	}

	/** 
	  Convenience method to convert msec time value to day number. 
	  @param msec the msec value
	  @return day number (0 = Jan 1, 1970)
	*/
	public static int msecToDay(long msec)
	{
		return (int)(msec / MSEC_PER_DAY);
	}

	/** 
	  Convenience method to conver msec time value to second of day.
	  @param msec the Java time value.
	  @return second-of-day
	*/
	public static int msecToSecondOfDay(long msec)
	{
		return (int)((msec % MSEC_PER_DAY)/1000L);
	}
	
	public static Date daynumToDate(int daynum)
	{
		return new Date(daynum * MSEC_PER_DAY);
	}

	private void errorHtmlResponse(String msg)
	{
		Logger.instance().warning("Request error: " + msg);
		try
		{
			ByteArrayOutputStream baos = startByteArrayOutputStream();
			XmlOutputStream xos = startXmlOutputStream(baos, "Request Error");
			xos.startElement("body");
			xos.writeElement("h1", "class", "msg_status", "Request Error");
			xos.writeElement("br", null);
			xos.writeElement("p", msg);
			xos.endElement("body");
			xos.endElement("html");
			output.write(baos.toString());
			output.write("HTML END\n");
			output.flush();
		}
		catch(Exception ex) {}
	}
}



/** Sorts XmitRecords by Address, then Channel, then second-of-day. */
class AddressComparator implements Comparator
{
	public int compare(Object o1, Object o2)
	{
		XmitRecord xr1 = (XmitRecord)o1;
		XmitRecord xr2 = (XmitRecord)o2;
		DcpAddress ad1 = xr1.getDcpAddress();
		DcpAddress ad2 = xr2.getDcpAddress();
		if (ad1 == null && ad2 != null)
			return 1;
		else if (ad1 != null && ad2 == null)
			return -1;
		else if (ad1 != null)
		{
			int r = ad1.compareTo(ad2);
			if (r != 0)
				return r;
		}
		
		int c = xr1.getGoesChannel() - xr2.getGoesChannel();
		if (c != 0)
			return c;

		return xr1.getSecOfDay() - xr2.getSecOfDay();
	}

	public boolean equals(Object obj) { return false; }
}


/** Used internally to sort results by Time. */
class TimeComparator implements Comparator<XmitRecord>
{
	boolean ascending;

	public TimeComparator(boolean ascending)
	{
		this.ascending = ascending;
	}

	public int compare(XmitRecord xr1, XmitRecord xr2)
	{
		int c = xr1.getSecOfDay() - xr2.getSecOfDay();
		if (c != 0)
			return ascending ? c : -c;

		return xr1.getDcpAddress().compareTo(xr2.getDcpAddress());
	}

	public boolean equals(Object obj) { return false; }
}


/** Used internally to sort results by basin. */
class MsgStatBasinComparator implements Comparator
{
	public int compare(Object o1, Object o2)
	{
		MsgStatRecord xr1 = (MsgStatRecord)o1;
		MsgStatRecord xr2 = (MsgStatRecord)o2;
		int c = xr1.basin.compareTo(xr2.basin);
		if (c != 0)
			return c;
		c = xr1.dcpName.compareTo(xr2.dcpName);
		if (c != 0)
			return c;
		return xr1.goesChannel - xr2.goesChannel;
	}

	public boolean equals(Object obj) { return false; }
}

/** Used internally to sort results by address. */
class MsgStatAddressComparator implements Comparator<MsgStatRecord>
{
	public int compare(MsgStatRecord xr1, MsgStatRecord xr2)
	{
		int r = xr1.dcpAddress.compareTo(xr2.dcpAddress);
		if (r != 0)
			return r < 0 ? -1 : 1;

		return xr1.goesChannel - xr2.goesChannel;
	}

	public boolean equals(Object obj) { return false; }
}

/** Sorts MsgStatRecord by channel, then first sec-of-day, then dcp name. */
class MsgStatChannelComparator implements Comparator
{
	public int compare(Object o1, Object o2)
	{
		MsgStatRecord xr1 = (MsgStatRecord)o1;
		MsgStatRecord xr2 = (MsgStatRecord)o2;
		int c = xr1.goesChannel - xr2.goesChannel;
		if (c != 0)
			return c;

		c = xr1.firstMsgSecOfDay - xr2.firstMsgSecOfDay;
		if (c != 0)
			return c;

		return xr1.dcpName.compareTo(xr2.dcpName);
	}

	public boolean equals(Object obj) { return false; }
}

/** Used internally to sort results by Time. */
class MsgStatTimeComparator implements Comparator
{
	public int compare(Object o1, Object o2)
	{
		MsgStatRecord xr1 = (MsgStatRecord)o1;
		MsgStatRecord xr2 = (MsgStatRecord)o2;
		int c = xr1.firstMsgSecOfDay - xr2.firstMsgSecOfDay;
		if (c != 0)
			return c;

		return xr1.dcpName.compareTo(xr2.dcpName);
	}

	public boolean equals(Object obj) { return false; }
}

/** Used internally to sort results by Name. */
class MsgStatNameComparator implements Comparator
{
	public int compare(Object o1, Object o2)
	{
		MsgStatRecord xr1 = (MsgStatRecord)o1;
		MsgStatRecord xr2 = (MsgStatRecord)o2;

		int c = xr1.dcpName.compareTo(xr2.dcpName);
		if (c != 0)
			return c;

		return xr1.goesChannel - xr2.goesChannel;
	}

	public boolean equals(Object obj) { return false; }
}
