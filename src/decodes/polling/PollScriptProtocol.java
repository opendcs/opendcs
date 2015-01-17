/*
 * $Id$
 * 
 * This software was written by Cove Software, LLC ("COVE") under contract
 * to Alberta Environment and Sustainable Resource Development (Alberta ESRD).
 * No warranty is provided or implied other than specific contractual terms 
 * between COVE and Alberta ESRD.
 *
 * Copyright 2014 Alberta Environment and Sustainable Resource Development.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package decodes.polling;

import ilex.util.AsciiUtil;
import ilex.util.EnvExpander;
import ilex.util.Logger;
import ilex.util.PropertiesUtil;
import ilex.util.TextUtil;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.LineNumberReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.TimeZone;

import decodes.db.Constants;
import lrgs.common.DcpAddress;
import lrgs.common.DcpMsg;
import lrgs.common.DcpMsgFlag;
import decodes.db.Database;
import decodes.db.DbEnum;
import decodes.db.EnumValue;
import decodes.db.TransportMedium;
import decodes.util.DecodesSettings;

public class PollScriptProtocol
	extends LoggerProtocol
	implements StreamReaderOwner
{
	protected String module = "PollScriptProtocol";
	private ArrayList<PollScriptCommand> script = new ArrayList<PollScriptCommand>();
	protected StreamReader streamReader = null;
	protected IOPort ioPort = null;
	protected Exception abnormalShutdown = null;
	protected PollingDataSource dataSource = null;
	protected Properties properties = new Properties();
	protected Date start = null;
	protected TransportMedium transportMedium = null;
	protected boolean _inputClosed = false;
	private int scriptIdx = 0;

	public PollScriptProtocol()
	{
		// No args constructor allows instantiation from Class object
	}
	
	@Override
	public void login(IOPort port, TransportMedium tm)
		throws LoginException
	{
		this.ioPort = port;
		
		dataSource.log(Logger.E_DEBUG3, module + ".login()");
		
		// Use enum associated with tm.loggerType to find Poll script.
		DbEnum lte = Database.getDb().enumList.getEnum(Constants.enum_LoggerType);
		if (lte == null)
			throw new LoginException(module + " No LoggerType enumeration in this database.");
		
		if (tm.getLoggerType() == null || tm.getLoggerType().trim().length() == 0)
			throw new LoginException(module + " No logger type specified in transport medium");
		EnumValue ltev = lte.findEnumValue(tm.getLoggerType().trim());
		if (ltev == null)
			throw new LoginException(module + " No LoggerType enum value matching '" + tm.getLoggerType() + "'.");

		if (ltev.getOptions() == null || ltev.getOptions().trim().length() == 0)
			throw new LoginException(module + " LoggerType enum value '" + tm.getLoggerType() + "' has no script name.");
		
		File scriptDir = new File(EnvExpander.expand(DecodesSettings.instance().pollScriptDir));
		if (!scriptDir.isDirectory())
			throw new LoginException("Script directory '" + scriptDir + "' does not exist. Check "
				+ "decodes setting for 'pollScriptDir'");
		
		File scriptFile = new File(scriptDir, ltev.getOptions().trim());
		if (!scriptFile.canRead())
			throw new LoginException(module + " Cannot read poll script '" + scriptFile.getPath() + "'");

		// Construct a Properties set used for variable substitution in the XMIT directives.
		if (tm.platform.getSite() != null)
			PropertiesUtil.copyProps(properties, tm.platform.getSite().getProperties());
		PropertiesUtil.copyProps(properties, tm.platform.getProperties());
		if (PropertiesUtil.getIgnoreCase(properties, "SITENAME") == null)
			properties.setProperty("SITENAME", tm.platform.getSiteName(false));
		if (tm.getUsername() != null && tm.getUsername().trim().length() > 0)
			properties.setProperty("USERNAME", tm.getUsername());
		if (tm.getPassword() != null && tm.getPassword().trim().length() > 0)
			properties.setProperty("PASSWORD", tm.getPassword());
		properties.setProperty("HOURS", ""+dataSource.getController().getMaxBacklogHours());
		
		// Then read it into memory
		readScript(scriptFile);
		dataSource.log(Logger.E_DEBUG3, module + " Successfully read script file '" + scriptFile + "'");
		
		// Note: No actual login is done here. The script handles it.
	}
	
	void readScript(File scriptFile)
		throws LoginException
	{
		script.clear();
		LineNumberReader lnr = null;
		PollScriptLoopWaitCmd loopWaitCmd = null;
		PollScriptWaitCmd lastWait = null;
		try
		{
			lnr = new LineNumberReader(new FileReader(scriptFile));
			String line;
			while((line = lnr.readLine()) != null)
			{
				line = line.trim();
				if (line.length() == 0 || line.startsWith("#"))
					continue;
				StringTokenizer strtok = new StringTokenizer(line, " ,\t");
				String keyword = strtok.nextToken().toLowerCase();
				if (keyword.equals("xmit"))
				{
					int x = line.indexOf('"');
					int y = line.lastIndexOf('"');
					if (x == -1 || x == y)
						throw new LoginException("Script '" + scriptFile.getPath() + "':"
							+ lnr.getLineNumber() + " XMIT command requires a string surrounded by double quotes.");
					String str = line.substring(x+1, y);
					script.add(new PollScriptXmitCmd(this, str));
				}
				else if (keyword.equals("startformat"))
				{
					int x = line.indexOf('"');
					int y = line.lastIndexOf('"');
					if (x == -1 || x == y)
						throw new LoginException("Script '" + scriptFile.getPath() + "':"
							+ lnr.getLineNumber() + " format for STARTFORMAT for must be surrounded in double quotes.");
					String sdfFmt = line.substring(x+1, y);
					script.add(new PollScriptStartFormatCmd(this, sdfFmt));
				}
				else if (keyword.equals("capture"))
				{
					boolean captureOn = !strtok.hasMoreTokens()
						|| TextUtil.str2boolean(strtok.nextToken());
					script.add(new PollScriptCaptureCmd(this, captureOn));
				}
				else if (keyword.equals("wait") || keyword.equals("waitr") || keyword.equals("waitx"))
				{
					// First arg after keyword should be floating point number of seconds.
					if (!strtok.hasMoreTokens())
						throw new LoginException("Script '" + scriptFile.getPath() + "':"
							+ lnr.getLineNumber() + " expected number of seconds after WAIT.");
					String s = strtok.nextToken();
					double sec = 0.;
					try { sec = Double.parseDouble(s); }
					catch(NumberFormatException ex)
					{
						throw new LoginException("Script '" + scriptFile.getPath() + "':"
							+ lnr.getLineNumber() + " invalid number of seconds '" + s + "' after WAIT.");
					}
					
					PollScriptWaitCmd pswc = new PollScriptWaitCmd(this, sec, keyword.equals("waitr"), line);
					if (keyword.equals("waitx"))
						pswc.setExclude(true);
					
					// String expression to wait for.
					int comma = line.indexOf(',');
					if (comma > 0)
					{
						int startQuote = line.indexOf('"', comma);
						while(startQuote > 0)
						{
							int endQuote = startQuote + 1;
							while(endQuote < line.length() && (line.charAt(endQuote) != '"' || line.charAt(endQuote-1) == '\\'))
								endQuote++;
							String match = line.substring(startQuote+1, endQuote);
							// Expand the string with the platform/site's properties.
							String estr = EnvExpander.expand(match, getProperties());

							pswc.addMatch(AsciiUtil.ascii2bin(estr));
							startQuote = endQuote+1 >= line.length() ? -1 : line.indexOf('"', endQuote+1);
						}
					}

					script.add(lastWait = pswc);
				}
				else if (keyword.equals("flush"))
				{
					script.add(new PollScriptFlushCmd(this));
				}
				else if (keyword.equals("loopwait"))
				{
					// First arg after keyword should be integer # of iterations.
					if (!strtok.hasMoreTokens())
						throw new LoginException("Script '" + scriptFile.getPath() + "':"
							+ lnr.getLineNumber() + " expected number of iterations after LOOPWAIT.");
					String s = strtok.nextToken();
					int iterations = 0;
					try { iterations = Integer.parseInt(s); }
					catch(NumberFormatException ex)
					{
						throw new LoginException("Script '" + scriptFile.getPath() + "':"
							+ lnr.getLineNumber() + " invalid integer number of iterations '" 
							+ s + "' after LOOPWAIT.");
					}
					script.add(loopWaitCmd = new PollScriptLoopWaitCmd(this, iterations));
				}
				else if (keyword.equals("endloop"))
				{
					if (loopWaitCmd == null)
						throw new LoginException("Script '" + scriptFile.getPath() + "':"
							+ lnr.getLineNumber() + " ENDLOOP without prior LOOPWAIT");
					if (lastWait == null)
						throw new LoginException("Script '" + scriptFile.getPath() + "':"
							+ lnr.getLineNumber() + " LOOPWAIT ... ENDLOOP must have a WAIT command in it.");
					
					script.add(new PollScriptEndLoopCmd(this, loopWaitCmd, lastWait));
					loopWaitCmd = null;
					lastWait = null;
				}
			}
		}
		catch (IOException ex)
		{
			throw new LoginException("Error reading script file '" + scriptFile.getPath() + "': " + ex);
		}
		finally
		{
			if (lnr != null)
				try { lnr.close(); } catch(Exception ex) {}
		}
	}
	
	protected void executeScript(IOPort port, Date since)
		throws ProtocolException
	{
		this.ioPort = port;
Logger.instance().info(module + " spawning StreamReader to responses from station.");

		streamReader = new StreamReader(port.getIn(), this);
		streamReader.setPollSessionLogger(pollSessionLogger);
		
		streamReader.start();
		
		try
		{
			abnormalShutdown = null;
			for(scriptIdx = 0; scriptIdx < script.size(); scriptIdx++)
			{
				PollScriptCommand cmd = script.get(scriptIdx);
				if (_inputClosed)
					throw new ProtocolException("Input Stream from Station Closed.");
				cmd.execute();
				if (abnormalShutdown != null)
					break;
			}
			Logger.instance().debug1(module + " Script execution complete."
				+ (abnormalShutdown == null ? "" : " Abnormal Shutdown: " + abnormalShutdown));
		}
		finally { streamReader.shutdown(); }
	}

	@Override
	public DcpMsg getData(IOPort port, TransportMedium tm, Date since)
		throws ProtocolException
	{
		start = since;
		this.transportMedium = tm;
		Date sessionStart = new Date();
		dataSource.log(Logger.E_DEBUG2, module + ".getData() session starting at " + sessionStart);

		executeScript(port, since);
		if (abnormalShutdown != null)
			return null;
		Date sessionEnd = new Date();
		dataSource.log(Logger.E_DEBUG2, module + ".getData() session finished at " + sessionEnd);

		// Build DcpMsg from info in tm and captured data.
		// Set all necessary attributes in DcpMsg.
		// Create an EDL header and prepend it to msgdata.
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		SimpleDateFormat sdf = new SimpleDateFormat("yyMMdd HHmmss Z");
		sdf.setTimeZone(TimeZone.getTimeZone("UTC"));

		try
		{
			Date recvTime = sessionEnd;
			String station = tm.platform.getSiteName(false);
			baos.write(
				 ("//STATION " + station + "\n"
				+ "//SOURCE " + tm.getLoggerType() + "\n"
				+ "//DEVICE END TIME " + sdf.format(sessionStart) + "\n"
				+ "//POLL START " + sdf.format(sessionStart) + "\n"
				+ "//POLL STOP " + sdf.format(recvTime) + "\n"
				).getBytes());
			byte[] capturedData = streamReader.getCapturedData();
			baos.write(capturedData);
			byte[] msgdata = baos.toByteArray();
			DcpMsg ret = new DcpMsg(msgdata, msgdata.length, 0);
			ret.setLocalReceiveTime(recvTime);
			ret.setXmitTime(recvTime);
			ret.setCarrierStart(sessionStart);
			ret.setCarrierStop(recvTime);
			ret.setDcpAddress(new DcpAddress(tm.getMediumId()));
			ret.setFailureCode('G');
			
			ret.setFlagbits(
				DcpMsgFlag.MSG_PRESENT
				| DcpMsgFlag.SRC_NETDCP
	            | DcpMsgFlag.HAS_CARRIER_TIMES
				| DcpMsgFlag.MSG_TYPE_NETDCP
	            | DcpMsgFlag.MSG_NO_SEQNUM);
			ret.setHeaderLength(msgdata.length - capturedData.length);
			dataSource.log(Logger.E_DEBUG2, module + ".getData() returning message.");

			return ret;
		}
		catch (IOException ex)
		{
			// Won't happen for byte array OS
			String msg = module + "U nexpected error in PollScriptProtocol.getData(): " + ex;
			dataSource.log(Logger.E_FAILURE, msg);
			System.err.println(msg);
			ex.printStackTrace(System.err);
			throw new ProtocolException(msg);
		}
		finally
		{
			try {baos.close();} catch(Exception ex){}
		}
	}

	@Override
	public void goodbye(IOPort port, TransportMedium tm)
	{
		// Nothing to do
	}

	/**
	 * Called by a CAPTURE command.
	 * @param captureOn
	 */
	public void setCapture(boolean captureOn)
	{
		streamReader.setCapture(captureOn);
	}
	
	@Override
	public void inputError(IOException ex)
	{
		Logger.instance().warning(module + " input error: " + ex);
		abnormalShutdown = ex;
	}
	
//	/**
//	 * Test main opens a mock session with stdin/stdout.
//	 * @param args two args are expected: station-name and script-file-name
//	 */
//	public static void main(String[] args)
//		throws Exception
//	{
//		Logger.instance().setMinLogPriority(Logger.E_DEBUG3);
//		IOPort ioPort = new IOPort(null, 1, (Dialer)null);
//		ioPort.setIn(System.in);
//		ioPort.setOut(System.out);
//		
//		TransportMedium tm = new TransportMedium(null, Constants.medium_EDL, args[0]);
//		PollScriptProtocol tlp = new PollScriptProtocol();
//		tlp.readScript(new File(args[1]));
//		System.out.println("After reading " + args[1] + ", there are " + tlp.script.size() + " commands.");
//		
//		DcpMsg msg = tlp.getData(ioPort, tm, new Date());
//		
//		System.out.println("=========== Results ===========");
//		System.out.println("Complete Session Log:");
//		System.out.println(new String(tlp.streamReader.getEntireSession()));
//		System.out.println("------ Captured Data ------");
//		System.out.println(new String(tlp.streamReader.getCapturedData()));
//		System.out.println("------ DCP Message ------");
//		System.out.println(msg.toString());
//	}

	public IOPort getIoPort()
	{
		return ioPort;
	}

	public StreamReader getStreamReader()
	{
		return streamReader;
	}

	@Override
	public void setDataSource(PollingDataSource dataSource)
	{
		this.dataSource = dataSource;
	}
	
	PollSessionLogger getPollSessionLogger() { return pollSessionLogger; }

	public Properties getProperties()
	{
		return properties;
	}
	
	/** Called from StartFormat Command */
	public void startFormat(SimpleDateFormat sdf)
	{
		if (transportMedium.getTimeZone() != null && transportMedium.getTimeZone().trim().length() > 0)
			sdf.setTimeZone(TimeZone.getTimeZone(transportMedium.getTimeZone().trim()));
		properties.setProperty("START", sdf.format(start));
	}

	@Override
	public void inputClosed()
	{
		_inputClosed = true;
	}
	
	@Override
	public String getModule() { return module; }

	/**
	 * Called from PollScriptEndLoop if a match was not found.
	 * @param rewindTo
	 */
	public void rewind(PollScriptLoopWaitCmd rewindTo)
		throws ProtocolException
	{
		while(scriptIdx >= 0 && script.get(scriptIdx) != rewindTo)
			scriptIdx--;
		if (scriptIdx < 0) // shouldn't happen
			throw new ProtocolException("ENDLOOP with no matching LOOPWAIT");
		// decrement once more, because execute's loop will increment at end of for loop.
		scriptIdx--;
	}

}
