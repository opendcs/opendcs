package decodes.snotel;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.UnknownHostException;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

import decodes.decoder.EndOfDataException;
import decodes.decoder.FieldParseException;
import decodes.decoder.NumberParser;
import decodes.decoder.ScriptFormatException;
import ilex.util.ArrayUtil;
import ilex.util.EnvExpander;
import ilex.util.FileUtil;
import ilex.util.Logger;
import ilex.var.NoConversionException;
import ilex.var.Variable;
import lrgs.common.DcpMsg;
import lrgs.common.LrgsErrorCode;
import lrgs.common.SearchCriteria;
import lrgs.ldds.LddsClient;
import lrgs.ldds.ProtocolError;
import lrgs.ldds.ServerError;

public class RetrievalThread extends Thread
{
	private String module = "RetrievalThread";
	private SnotelDaemon parent = null;
	private SnotelPlatformSpecList specList = null;
	private String since = null;
	private String until = null;
	private boolean _shutdown = false;
	private int sequencNum = 0;
	private byte[] buffer;
	private int pos;
	private PrintWriter curOutput = null;
	private File curOutputFile = null;
	private long lastOutputMsec = 0L;
	private String prefix = "";
	private String lastTimeStamp = "";
	private int seqNum = 1;

	public RetrievalThread(SnotelDaemon parent, SnotelPlatformSpecList specList, 
		String since, String until, int sequenceNum, String prefix)
	{
		super();
		this.module = this.module + "-" + prefix;
		this.parent = parent;
		this.specList = specList;
		this.since = since;
		this.until = until;
		this.sequencNum = sequenceNum;
		this.prefix = prefix;
	}

	@Override
	public void run()
	{
		SnotelConfig conf = parent.getConfig();
		
		Logger.instance().info(module + " Starting. Sequence=" + sequencNum + ", "
			+ specList.getPlatformSpecs().size()
			+ " platforms in list. since=" + since + ", until=" + until);
		
		boolean runRealTime = prefix.equalsIgnoreCase("rt") && conf.retrievalFreq <= 0;
		
		if (specList.getPlatformSpecs().isEmpty())
		{
			Logger.instance().failure(module + " No platforms in list. Cannot run.");
			return;
		}
		
		ArrayList<HostPort> conlist = new ArrayList<HostPort>();
		if (conf.lrgsUser == null || conf.lrgsUser.length() == 0)
		{
			Logger.instance().failure(module + " Configuration missing lrgsUser. Cannot run.");
			return;
		}
		
		addHostPort(conlist, conf.lrgs1, "lrgs1");
		addHostPort(conlist, conf.lrgs2, "lrgs2");
		addHostPort(conlist, conf.lrgs3, "lrgs3");
		addHostPort(conlist, conf.lrgs4, "lrgs4");
		
		try
		{
			for(HostPort con : conlist)
			{
				if (_shutdown)
					break;
				
				LddsClient lddsClient = null;
				int numMsgs = 0;
				try
				{
					lddsClient = new LddsClient(con.host, con.port);
					Logger.instance().debug1(module + " Connecting to " + con.host + ":" + con.port);
					lddsClient.connect();
					Logger.instance().debug1(module + " Logging in to " 
							+ con.host + ":" + con.port + " as user '" + conf.lrgsUser + "'");
					if (conf.lrgsPassword != null)
						lddsClient.sendAuthHello(conf.lrgsUser, conf.lrgsPassword);
					else
						lddsClient.sendHello(conf.lrgsUser);
					
					SearchCriteria crit = new SearchCriteria();
					crit.setLrgsSince(since);
					if (!runRealTime)
						crit.setLrgsUntil(until);
					for(SnotelPlatformSpec plat : specList.getPlatformSpecs())
						crit.addDcpAddress(plat.getDcpAddress());
					
					Logger.instance().debug1(module + " sending search criteria: " 
						+ crit.toString());
					lddsClient.sendSearchCrit(crit);
					
					Logger.instance().debug1(module + " retrieving messages.");
					while(!_shutdown)
					{
						DcpMsg msg = null;
						try
						{
							msg = lddsClient.getDcpMsg(90);
							if (msg != null)
							{
								outputMessage(msg);
								numMsgs++;
							}
						}
						catch (ServerError se)
						{
							if (se.Derrno == LrgsErrorCode.DMSGTIMEOUT && runRealTime)
							{
								Logger.instance().debug1(module + " server caught up. pausing 2 sec.");
								// This means we're running in realtime and server is waiting for
								// the next message. Pause and Stay in the loop.
								flushBuffer();
								try { Thread.sleep(2000L); }
								catch(InterruptedException ie) {}
							}
							else
								throw se;
						}
					}
				}
				catch (UnknownHostException ex)
				{
					Logger.instance().warning("Unknown host '" + con.host + ": " + ex
						+ " -- skipping.");
				}
				catch (IOException ex)
				{
					Logger.instance().warning("IOException on '" + con.host + ": " + ex
						+ " -- skipping.");
				}
				catch (ServerError se)
				{
					if (se.Derrno == LrgsErrorCode.DUNTIL
					 || se.Derrno == LrgsErrorCode.DUNTILDRS)
					{
						Logger.instance().info(module + 
							" Until time reached. Normal termination. " + numMsgs + " processed.");
						return;
					}
					
					Logger.instance().warning("ServerError on connection to " 
						+ con.host + ": " + se + " -- skipping.");				
				}
				catch (ProtocolError ex)
				{
					Logger.instance().warning("DDS Protocol Error on '" + con.host + ": " + ex
						+ " -- skipping.");
				}
				finally
				{
					lddsClient.disconnect();
				}
			}
		}
		finally
		{
			flushBuffer();
			parent.retrievalFinished(this);
		}
	}
	
	private void outputMessage(DcpMsg dcpMsg)
	{
		Logger.instance().info(module + ".outputMessage: " + new String(dcpMsg.getData()));
		
		long curOutputTime = System.currentTimeMillis();
		
		SnotelPlatformSpec spec = specList.getPlatformSpec(dcpMsg.getDcpAddress());
		if (spec == null)
		{
			Logger.instance().failure(module + " received message for " + dcpMsg.getDcpAddress()
				+ " but address is not in the spec list -- skipped.");
			return;
		}
		Logger.instance().debug1(module + " decoding with spec: " + spec);

		initDecoder(dcpMsg);
		
		// Initialize calendar with the message's transmit time and routing spec's TZ.
		Date xmitTime = dcpMsg.getXmitTime();
		Calendar cal = Calendar.getInstance();
		TimeZone outTz = TimeZone.getTimeZone(parent.getConfig().outputTZ);
		cal.setTimeZone(outTz);
		cal.setTime(xmitTime);
		StringBuilder sb = new StringBuilder();
		SimpleDateFormat dateSdf = new SimpleDateFormat("MM/dd/yyyy");
		dateSdf.setTimeZone(outTz);
		SimpleDateFormat timeSdf = new SimpleDateFormat("HH:mm:ss");
		timeSdf.setTimeZone(outTz);
		SimpleDateFormat hhmmSdf = new SimpleDateFormat("HHmm");
		hhmmSdf.setTimeZone(outTz);
		
		boolean msgJan1 = cal.get(Calendar.DAY_OF_YEAR) == 1;
		NumberFormat numFmt = NumberFormat.getNumberInstance();
		numFmt.setGroupingUsed(false);
		numFmt.setMaximumFractionDigits(3);

		if (curOutput != null 
		 && (curOutputTime - lastOutputMsec) >= (parent.getConfig().fileBufferTime*1000L))
			flushBuffer();
		
		if (curOutput == null)
		{
			SimpleDateFormat fileSdf = new SimpleDateFormat("yyyyMMdd-HHmmss");
			
			// Use sequence num to guard against 2 files within same second overwriting.
			Date now = new Date();
			String ts = fileSdf.format(now);
			if (lastTimeStamp != null && lastTimeStamp.equals(ts))
				seqNum++;
			else
				seqNum = 1;
			lastTimeStamp = ts;
			
			String fname = prefix + "-" + lastTimeStamp + "-" + seqNum + ".csv";
			
			curOutputFile = new File(EnvExpander.expand(parent.getConfig().outputTmp), fname);
			try
			{
				curOutput = new PrintWriter(curOutputFile);
				Logger.instance().info(module + " opened output file '" 
					+ curOutputFile.getPath() + "'");
				lastOutputMsec = System.currentTimeMillis();
			}
			catch (FileNotFoundException ex)
			{
				Logger.instance().failure(module + " cannot create output file '"
					+ curOutputFile.getPath() + "': " + ex + " -- message '" + dcpMsg.getHeader()
					+ "' skipped.");
				curOutput = null;
			}
		}
		
		boolean ascii = isAscii();
		NumberParser numParser = new NumberParser();
		try { numParser.setDataType(
			ascii ? NumberParser.ASCII_FMT : NumberParser.CAMPBELL_BINARY_FMT); }
		catch(ScriptFormatException ex) {} // won't happen
		
		if (ascii && spec.getDataFormat() == 'B')
		{
			Logger.instance().failure(module 
				+ " invalid msg '" + new String(dcpMsg.getHeader()) + "' -- "
				+ "spec says new B format but message contains ASCII chars. Msg skipped.");
			return;
		}
		
		try
		{
			forwardspace(); // Skip initial DADDS status char in msg.
			
			// for format A, numHours is always 1 and set numChans to big number.
			int numHours = 1, numChans = 1000;
			if (spec.getDataFormat() == 'B')
			{
				numHours = numParser.parseIntValue(getField(3,  null));
				numChans = numParser.parseIntValue(getField(3,  null));
				Logger.instance().debug2(module + " B format numHours=" + numHours 
					+ ", numChans=" + numChans);
				if (numHours < 1 || numHours > 5
				 || numChans < 1 || numHours*numChans > 375)
				{
					Logger.instance().warning(module + " message '" + dcpMsg.getHeader()
						+ "': Spec says format B but numHours=" + numHours
						+ " and numChans=" + numChans + " which are impossible values"
						+ " -- will attempt format A.");
					numHours = 1;
					numChans = 1000;
					pos = 37; // GOES messages always have 37 byte header
					forwardspace();
				}
			}

			for(int hr = 0; hr < numHours; hr++)
			{
				sb.setLength(0);
				
				if (!ascii)
				{
					// Pseudobinary includes sample time in first 2 PB fields.
					byte[] doyField = getField(3, null);
					byte[] hhmmField = getField(3, null);
					
					// Decode the time into a fresh calendar object
					try
					{
						int doy = numParser.parseIntValue(doyField);
						cal.set(Calendar.DAY_OF_YEAR, doy);
						int hhmm = numParser.parseIntValue(hhmmField);
						cal.set(Calendar.HOUR_OF_DAY, hhmm/100);
						cal.set(Calendar.MINUTE, hhmm % 100);
						cal.set(Calendar.SECOND, 0);
						Logger.instance().debug1(module + " parsed doy=" + doy + ", hhmm=" + hhmm
							+ ", resulting time=" + cal.getTime());
						
						// Handle case where msg received on Jan 1 and the 
						// day-of-year parsed is Dec 31. Subtract 1 from year.
						if (msgJan1
						 && cal.get(Calendar.MONTH) == Calendar.DECEMBER
						 && cal.get(Calendar.DAY_OF_MONTH) == 31)
							cal.add(Calendar.YEAR, -1);
					}
					catch(FieldParseException ex)
					{
						String emsg = module + " invalid time fields doy='" 
							+ new String(doyField) + "', hhmm='" + new String(hhmmField);
						Logger.instance().warning(emsg);
						return;
					}
				}
				else // ASCII does not include time. Truncate seconds and minutes in xmit time.
				{
					cal.set(Calendar.SECOND, 0);
					cal.set(Calendar.MINUTE, 0);
					skipWhiteSpace(); // ASCII msg begins with CR LF.
				}
				
				sb.append(
					        dateSdf.format(dcpMsg.getXmitTime())
					+ "," + timeSdf.format(dcpMsg.getXmitTime())
					+ "," + spec.getStationId()
					+ "," + dateSdf.format(cal.getTime())
					+ "," + hhmmSdf.format(cal.getTime()));
		
				int chan = 0;
				for (chan = 0; chan < numChans; chan++)
				{
					// Decode a data value.
					byte[] dataField = new byte[0];
					try
					{
						if (!ascii)
							dataField = getField(3, null);
						else
						{
							dataField = getField(10, ",", false, false);
							forwardspace(); // gobble the comma delimiter
						}
						Variable v = numParser.parseDataValue(dataField);
						
						sb.append("," + numFmt.format(v.getDoubleValue()));
					}
					catch (FieldParseException e)
					{
						Logger.instance().warning(module + " hour " + hr + " chan " + chan
							+ " bad data field '" + new String(dataField) + "' -- skipped.");
					}
					catch(EndOfDataException ex)
					{
						// ran out of message data. Break and print what we have.
						break;
					}
					catch (NoConversionException e)
					{
						Logger.instance().warning(module + " cannot represent value as a number: " + e
							+ " field='" + new String(dataField) + "' -- skipped.");
					}
				}
				String line = sb.toString();
Logger.instance().debug2(module + " msgTime=" + dcpMsg.getXmitTime() + ", tz=" 
+ outTz.getID() + ", #chans=" + chan + ", line='" + line + "'");

				curOutput.println(line);
			}
		}
		catch(FieldParseException ex)
		{
			Logger.instance().warning(module + " bad msg field: " + ex);
		}
		catch(EndOfDataException ex)
		{
			Logger.instance().warning(module + " bad message hour - missing time fields: '" 
				+ new String(dcpMsg.getData()) + "' -- skipped.");
		}

		
		
		
	}
	
	
	private void initDecoder(DcpMsg dcpMsg)
	{
		buffer = dcpMsg.getData();
		if (buffer[buffer.length - 1] == (byte)' ')
			buffer = ArrayUtil.resize(buffer, buffer.length - 1);
		
		pos = 37; // GOES messages always have 37 byte header
		
	}
	
	private void flushBuffer()
	{
		if (curOutput != null)
		{
			try
			{
				curOutput.close();
				if (curOutputFile.length() > 0)
				{
					File outFile = new File(EnvExpander.expand(parent.getConfig().outputDir),
							curOutputFile.getName());
					Logger.instance().info("Moving '" + curOutputFile.getPath() 
						+ "' to '" + outFile.getPath() + "'");
					FileUtil.moveFile(curOutputFile, outFile);
				}
				else
				{
					Logger.instance().debug1("Deleting empty output file '" 
						+ curOutputFile.getPath() + "'");
					curOutputFile.delete();
				}
			} 
			catch(Exception ex)
			{
				Logger.instance().warning("Error closing file: " + ex);
			}
			curOutput = null;
		}
	}
	
	private void forwardspace() { pos++; }

	public void shutdown() { _shutdown = true; }
	
	private void addHostPort(ArrayList<HostPort> conlist, String lrgs, String name)
	{
		if (lrgs != null && lrgs.length() > 0)
		{
			String host = lrgs;
			int port = 16003;
			int colon = lrgs.indexOf(':');
			if (colon > 0)
			{
				host = lrgs.substring(0,colon);
				try { port = Integer.parseInt(lrgs.substring(colon+1)); }
				catch(NumberFormatException ex)
				{
					Logger.instance().warning(module + " Bad port in " + name + " spec '"
						+ lrgs + "' -- will use 16003.");
					port = 16003;
				}
			}
			conlist.add(new HostPort(host, port));
			Logger.instance().debug1(module + "LRGS #" + conlist.size() + ": " + host + ":" + port);
		}
	}

	public int getSequencNum()
	{
		return sequencNum;
	}
	
	// Scan message for anything out of the pseudobinary range.
	private boolean isAscii()
	{
		for(int i=38; i<buffer.length; i++) // skip first demod status char
			if (buffer[i] < 63 || buffer[i] > 127)
			{
Logger.instance().debug1(module + " ASCII MSG: non PB char '" + (char)buffer[i] 
+ "' (" + (int)buffer[i] + ") at position " + i);
				return true;
			}
			
		return false;
	}

	/**
	 * Provides default isBinary = false.
	 * @see getField(int length, String delim, boolean isBinary)
	 */
	public byte[] getField(int length, String delim)
	throws EndOfDataException
	{
		return getField(length, delim, false, false);
	}
	
	/**
	  Returns a 'field' of data. The field starts at the current byte
	  and continues for specified number of characters or until one of
	  a set of specified delimiter characters is found. Pass null as
	  'delimiter' to mean un-delimited.
	  <p>
	  Fields always implicitely end when the end-of-line is encountered.
	  @param length number of bytes in field length
	  @param delim String containing delimiter characters or null
	  @return byte array containing the field data
	*/
	public byte[] getField(int length, String delim, boolean isBinary,
		boolean isString)
		throws EndOfDataException
	{
		if (pos >= buffer.length)
			throw new EndOfDataException("pos=" + pos + ", len=" + buffer.length);
		
		byte b[] = new byte[length];
		int i;

		for(i=0; i < length; i++ ) 
		{
			if ( pos < buffer.length ) 
			{
 				byte c = buffer[pos];

 				if (isBinary)  // For binary, no EOL check and no delims.
 					;
 				else if (c == (byte)'\r' || c == (byte)'\n'                  // EOL
				 || ((isString||i>0)                         // Allow empty strings only
				     && delim != null                        // Delimiter is specified.
				     && (delim.indexOf(c) != -1)))           // This is a delimiter.
				{
					break;
				}
				else if (i>0 
				 && delim != null
				 && delim.indexOf('!') >= 0 
				 && !isNumberChar(c))
				{
					break;
				}
				
				b[i] = c;
				forwardspace();
			}
			else
				break;
		}
		return i==length ? b : ArrayUtil.getField(b, 0, i);
	}

	private boolean isNumberChar(byte b)
	{
		char c = (char)b;
		return Character.isDigit(c) || c == '.' 
		 || c == '+' || c == '-'
		 || c == 'e' || c == 'E';
	}

	public void skipWhiteSpace()
	{
		while( pos < buffer.length 
		 &&  (  buffer[pos] == (byte)' ' || buffer[pos] == (byte)'\t'
		     || buffer[pos] == (byte)'\r' || buffer[pos] == (byte)'\n'
			 || buffer[pos] == (byte)0x00AE))
			pos++;
	}


}

class HostPort
{
	String host = null;
	int port = 16003;
	
	public HostPort(String host, int port)
	{
		super();
		this.host = host;
		this.port = port;
	}
}
