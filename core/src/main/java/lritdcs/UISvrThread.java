/*
*  $Id$
*/
package lritdcs;

import java.util.Date;
import java.util.StringTokenizer;
import java.io.*;
import java.net.Socket;
import java.util.Vector;

import ilex.net.*;
import ilex.util.*;
import lrgs.common.SearchCriteria;
import lrgs.common.SearchSyntaxException;

public class UISvrThread
	extends BasicSvrThread
{
	BufferedReader input;
	PrintWriter output;
	SearchCriteria highCriteria;
	SearchCriteria lowCriteria;
	SearchCriteria mediumCriteria;
	int eventIndex;

	UISvrThread(BasicServer parent, Socket socket)
		throws IOException
	{
		super(parent, socket);
		input = 
			new BufferedReader(new InputStreamReader(socket.getInputStream()));
		output = new PrintWriter(socket.getOutputStream());
		highCriteria = new SearchCriteria();
		lowCriteria = new SearchCriteria();
		mediumCriteria = new SearchCriteria();
		eventIndex = LritDcsMain.instance().getLogQueue().getNextIdx();
		if (eventIndex < 0)
			eventIndex = 0;
	}

	protected void serviceClient()
	{
		// Blocking Read for a line of input
		String line;
		try 
		{
			line = input.readLine(); 
			if (line == null)
			{
				Logger.instance().info(
					"User Interface Socket from " + getClientName() 
					+ " has disconnected.");
				disconnect();
				return;
			}
			processLine(line);
		}
		catch(IOException ex)
		{
			Logger.instance().info(
				"IO Error on User Interface Socket from " + getClientName() 
				+ " -- disconnecting");
			disconnect();
			return;
		}
	}

	private void processLine(String line)
		throws IOException
	{
		// Process the line.
		line = line.trim();
		StringTokenizer st = new StringTokenizer(line);
		if (st.countTokens() == 0)
			return;

		String keyword = st.nextToken();
		if (keyword.equalsIgnoreCase("status"))
		{
			LritDcsStatus stat = LritDcsMain.instance().getStatus();
			output.print(stat.toString());
			output.println();
		}
		else if (keyword.equalsIgnoreCase("events"))
		{
			QueueLogger lq = LritDcsMain.instance().getLogQueue();
			try { sendQueuedEvents(lq); }
			catch(IndexRangeException ex)
			{
				eventIndex = lq.getStartIdx();
				try { sendQueuedEvents(lq); }
				catch(IndexRangeException ex2) {}
			}
			output.println();
		}
		else if (keyword.equalsIgnoreCase("fileretrans"))
		{
			if (!st.hasMoreTokens())
				Logger.instance().warning(
					"fileretrans not implemented");
		}
		else if (keyword.equalsIgnoreCase("getcrit"))
		{
			if (!st.hasMoreTokens())
				Logger.instance().warning(
					"Error: missing argument to getcrit request");
			else
			{
				String t = st.nextToken();
				SearchCriteria crit = null;
				String filename = null;
				if (t.equalsIgnoreCase("high"))
				{
					crit = highCriteria;
					filename = "searchcrit.H";
				}
				else if (t.equalsIgnoreCase("medium"))
				{
					crit = mediumCriteria;
					filename = "searchcrit.M";
				}
				else if (t.equalsIgnoreCase("low"))
				{
					crit = lowCriteria;
					filename = "searchcrit.L";
				}
				else if (t.equalsIgnoreCase("manual"))
				{
					crit = lowCriteria;
					filename = "searchcrit.manual";
				}
				else
					crit = null;
				if (crit != null && filename != null)
				{
					File f = new File(
						LritDcsConfig.instance().getLritDcsHome()
						+ File.separator + filename);
					try 
					{
						crit.parseFile(f); 
						output.print(crit.toString());
					}
					catch(SearchSyntaxException ssex)
					{
						Logger.instance().warning("LRIT:"
							+ Constants.EVT_SEARCHCRIT
							+ "- Invalid Searchcrit '"
							+ f.getPath() + "': " + ssex);
					}
					catch(IOException ioex)
					{
						Logger.instance().warning("LRIT:"
							+ Constants.EVT_SEARCHCRIT
							+ "- " + f.getPath() + "': " + ioex);
					}
				}
			}
			output.println("ENDSC");
		}
		else if (keyword.equalsIgnoreCase("putcrit"))
		{
			if (!st.hasMoreTokens())
			{
				Logger.instance().warning(
					"Error: missing argument to set request");
				captureToBlankLine();   // discard sent criteria.
			}
			else
			{
				String t = st.nextToken();
				SearchCriteria crit = null;
				char pri = Constants.LowPri;
				if (t.equalsIgnoreCase("high"))
				{
					crit = highCriteria;
					pri = Constants.HighPri;
				}
				else if (t.equalsIgnoreCase("medium"))
				{
					crit = mediumCriteria;
					pri = Constants.MediumPri;
				}
				else if (t.equalsIgnoreCase("low"))
				{
					crit = lowCriteria;
					pri = Constants.LowPri;
				}
				else if (t.equalsIgnoreCase("manual"))
				{
					crit = new SearchCriteria();
					pri = Constants.ManualRetransPri;
				}

				setCriteria(crit, pri);

				if (pri == Constants.ManualRetransPri)
				{
					// Sending manual search crit also does the request.
					LritDcsMain.instance().getManualRetransThread().queueSC(
						crit);
				}
				else
				{   
					try 
					{
						File scFile = new File(
							LritDcsConfig.instance().getLritDcsHome()
							+ File.separator + "searchcrit."  + pri);
						crit.saveFile(scFile);
					}
					catch(IOException ex)
					{
						Logger.instance().warning("Error saving " + pri
							+ " priority search critiera: " + ex);
					}
				}
			}
		}
		else if (keyword.equalsIgnoreCase("flush"))
		{
			if (!st.hasMoreTokens())
				output.println("Error: missing argument to flush request");
			else
			{
				String t = st.nextToken();
				if (t.equalsIgnoreCase("all"))
				{
					flushHigh();
					flushMedium();
					flushLow();
					flushAuto();
					flushManual();
					flushPending();
				}
				else if (t.equalsIgnoreCase("high"))
					flushHigh();
				else if (t.equalsIgnoreCase("medium"))
					flushMedium();
				else if (t.equalsIgnoreCase("low"))
					flushLow();
				else if (t.equalsIgnoreCase("auto"))
					flushAuto();
				else if (t.equalsIgnoreCase("manual"))
					flushManual();
				else if (t.equalsIgnoreCase("pending"))
					flushPending();
				else
				{
					Logger.instance().warning("Unrecognized queue '" + t 
						+ "' -- command ignored.");
				}
			}
		}
		else if (keyword.equalsIgnoreCase("getconfig"))
		{
			LritDcsConfig.instance().save(output);
		}
		else if (keyword.equalsIgnoreCase("putconfig"))
		{
			putConfig();
		}
		else if (keyword.equalsIgnoreCase("lastretrieval"))
		{
			
			String  t = captureStringToBlankLine();
			setLastRetrievalTime(t);
			
		}
		else
		{
			Logger.instance().warning("Unrecognized keyword '" + keyword 
				+ "' -- command ignored.");
		}
		output.flush();
	}

	private void flushHigh()
	{
		LritDcsMain ldm = LritDcsMain.instance();
		int sz = ldm.getFileQueueHigh().size();
		info("Flushing " + sz + " files from High-priority file queue.");
		LritDcsFileStats stats = null;
		while((stats = ldm.getFileQueueHigh().dequeue()) != null)
		{
			stats.getFile().delete();
		}
		//ldm.getFileQueueHigh().clear();
	}

	private void flushMedium()
	{
		LritDcsMain ldm = LritDcsMain.instance();
		int sz = ldm.getFileQueueMedium().size();
		info("Flushing " + sz + " files from medium-priority file queue.");
		LritDcsFileStats stats = null;
		while((stats = ldm.getFileQueueMedium().dequeue()) != null)
		{
			stats.getFile().delete();
		}
		//ldm.getFileQueueMedium().clear();
	}

	private void flushLow()
	{
		LritDcsMain ldm = LritDcsMain.instance();
		int sz = ldm.getFileQueueLow().size();
		info("Flushing " + sz + " files from Low-priority file queue.");
		LritDcsFileStats stats = null;
		while((stats = ldm.getFileQueueLow().dequeue()) != null)
		{
			stats.getFile().delete();
		}
		//ldm.getFileQueueLow().clear();
	}

	private void flushAuto()
	{
		LritDcsMain ldm = LritDcsMain.instance();
		int sz = ldm.getFileQueueAutoRetrans().size();
		info("Flushing " + sz + " files from auto-retransmit file queue.");
		ldm.getFileQueueAutoRetrans().clear();
	}

	private void flushManual()
	{
		LritDcsMain ldm = LritDcsMain.instance();
		ldm.getManualRetransThread().cancelled = true;
		int sz = ldm.getFileQueueManualRetrans().size();
		info("Flushing " + sz + " files from Manual-Retransmit file queue.");
		LritDcsFileStats stats = null;
		while((stats = ldm.getFileQueueManualRetrans().dequeue()) != null)
		{
			stats.getFile().delete();
		}
		//ldm.getFileQueueManualRetrans().clear();
	}

	private void flushPending()
	{
		LritDcsMain ldm = LritDcsMain.instance();
		int sz = ldm.getFileNamesPending().size();
		info("Flushing " + sz + " files from pending-queue.");
		ldm.getFileNamesPending().clear();
	}

	private void sendQueuedEvents(QueueLogger lq)
		throws IndexRangeException
	{
		String msg;
		while( (msg = lq.getMsg(eventIndex)) != null)
		{
			output.println(msg);
			eventIndex++;
		}
	}

	/**
	  Retrieves config from UI client & saves it to my config file.
	  @returns response for the client.
	*/
	private void putConfig()
	{
		// Accumulate config in vector up to blank line.
		Vector v = new Vector();
		String cline;
		try
		{
			while((cline = input.readLine()) != null)
			{
				cline = cline.trim();
				if (cline.length() == 0)
					break;
				v.add(cline);
			}
		}
		catch(IOException ex)
		{
			Logger.instance().warning(
				"IOException reading config from UI: " + ex);
			return;
		}

		if (cline == null)
			return; // hangup from client!

		try
		{
			File cfgFile = LritDcsConfig.instance().getConfigFile();
			File tf = new File(cfgFile.getPath() + ".tmp");
			PrintWriter tw = new PrintWriter(new FileOutputStream(tf));
			for(int i=0; i<v.size(); i++)
				tw.println((String)v.get(i));
			tw.close();
			FileUtil.moveFile(tf, cfgFile);
			Logger.instance().info("Saved new configuration to "
				+ cfgFile.getPath());
		}
		catch(Exception ex)
		{
			Logger.instance().warning("Error saving config: " + ex);
			return;
		}
	}

	private void setCriteria(SearchCriteria crit, char pri)
		throws IOException
	{
		byte critbuf[] = captureToBlankLine();
		InputStreamReader isr = new InputStreamReader(
			new ByteArrayInputStream(critbuf));
		try 
		{
			crit.clear();
			crit.parseFile(isr); 
			isr.close();
			String ps = pri == 'H' ? "High Priority" 
					  : pri == 'M' ? "Medium Priority"
					  : pri == 'L' ? "Low Priority" : "Manual Retrans";
			Logger.instance().info("Saved " + ps + " criteria.");
		}
		catch(SearchSyntaxException ssex)
		{
			Logger.instance().warning("Error parsing " + pri
				+ " priority search critiera: " + ssex);
		}
		catch(IOException ioex)
		{
			Logger.instance().warning("Error reading " + pri
				+ " priority search critiera: " + ioex);
		}
	}

	private byte[] captureToBlankLine()
		throws IOException
	{
		ByteArrayOutputStream configBufOS = new ByteArrayOutputStream();
		byte nl[] = new byte[] { (byte)'\n' };
		while(true)
		{
			String line = input.readLine();
			if (line == null)
				throw new IOException("Socket closed by UI Client.");

			line = line.trim();
			if (line.length() == 0)
				break;
			configBufOS.write(line.getBytes());
			configBufOS.write(nl);
		}
		configBufOS.close();
		return configBufOS.toByteArray();
	}

	
	private String captureStringToBlankLine()
	throws IOException
{
		String line = input.readLine();		
		if (line == null)
			throw new IOException("Socket closed by UI Client.");
		line = line.trim();
			
	return line;
}
	
	
	
	private void info(String msg)
	{
		Logger.instance().info(msg);
	}
	
	/**
	 * 	 
	 * When LRIT is made active from dormant mode it should start picking messages from where last 'active' LRIT left.
	 * This method sets the lastRetrieval time for the current LRIT from last 'active' LRIT.
	 */
	private void setLastRetrievalTime(String cline)
	{
		LritDcsStatus stat = LritDcsMain.instance().getStatus();		
		if(cline!=null )
		stat.lastRetrieval = Long.valueOf(cline).longValue();	
		else 
		stat.lastRetrieval = 0L;	
		
		stat.writeToFile();		
	}
}
