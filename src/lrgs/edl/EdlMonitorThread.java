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
package lrgs.edl;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.Date;

import decodes.datasource.EdlPMParser;
import decodes.datasource.GoesPMParser;
import decodes.datasource.HeaderParseException;
import decodes.datasource.RawMessage;
import lrgs.common.DcpAddress;
import lrgs.common.DcpMsg;
import lrgs.common.DcpMsgFlag;
import lrgs.lrgsmain.LrgsConfig;
import ilex.util.DirectoryMonitorThread;
import ilex.util.EnvExpander;
import ilex.util.FileUtil;
import ilex.util.Logger;
import ilex.var.Variable;


public class EdlMonitorThread
	extends DirectoryMonitorThread
{
	private File doneDir = null;
	private EdlInputInterface parent = null;
	private EdlPMParser pmParser = new EdlPMParser();
	private static String module = "EdlMonitor";
	private long startTime;
	
	public EdlMonitorThread(EdlInputInterface parent)
	{
		this.parent = parent;
		configure();
		startTime = System.currentTimeMillis();
	}
	
	/**
	 * Configure the monitor once upon starting up.
	 */
	private void configure()
	{
		this.setSleepEveryCycle(true);
		this.setSleepInterval(1000L);
		
		this.myDirs.clear();
		File topdir = new File(EnvExpander.expand(LrgsConfig.instance().edlIngestDirectory));
		if (!topdir.isDirectory())
			topdir.mkdirs();
		this.addDirectory(topdir);
		if (LrgsConfig.instance().edlIngestRecursive)
			expand(myDirs.get(0));
		
		doneDir = null;
		if (LrgsConfig.instance().edlDoneDirectory != null
		 && LrgsConfig.instance().edlDoneDirectory.trim().length() > 0)
		{
			String exp = EnvExpander.expand(LrgsConfig.instance().edlDoneDirectory.trim());
			Logger.instance().info(module + " configuring with edlDoneDirecgtory='" +
				LrgsConfig.instance().edlDoneDirectory + "' expanded='" + exp + "'");
			doneDir = new File(exp);
			if (!doneDir.isDirectory())
				doneDir.mkdirs();
		}
		
		this.setFilenameFilter(null);
		if (LrgsConfig.instance().edlFilenameSuffix != null
		 && LrgsConfig.instance().edlFilenameSuffix.trim().length() > 0)
		{
			this.setFilenameFilter(
				new FilenameFilter()
				{
					@Override
					public boolean accept(File dir, String name)
					{
						return name.endsWith(LrgsConfig.instance().edlFilenameSuffix);
					}
				});
		}
		Logger.instance().debug1(module + " After configuration, there are " + myDirs.size()
			+ " directories in the list.");

	}
	
	/**
	 * Recursively expand the top dir.
	 * @param dir
	 */
	private void expand(File dir)
	{
		File[] files = dir.listFiles();
		for(int i=0; i<files.length; i++)
			if (files[i].isDirectory())
			{
				this.addDirectory(files[i]);
				expand(files[i]);
			}
	}


	@Override
	protected void processFile(File file)
	{
		Logger.instance().debug1(module + " Processing file '" + file.getPath() + "'");
		try
		{
			byte[] fileBytes = FileUtil.getfileBytes(file);
			
			// Wrap with RawMessage and use the Decodes EdlPMParser to parse the header.
			RawMessage rawMsg = new RawMessage(fileBytes, fileBytes.length);
			pmParser.parsePerformanceMeasurements(rawMsg);
			
			DcpMsg msg = new DcpMsg(fileBytes, fileBytes.length, 0);
			Date now = new Date();
			msg.setLocalReceiveTime(now);
			
			Variable dv = rawMsg.getPM(EdlPMParser.POLL_START);
			if (dv != null)
				try { msg.setCarrierStart(dv.getDateValue()); } catch(Exception ex) {}
			
			dv = rawMsg.getPM(EdlPMParser.POLL_STOP);
			if (dv == null)
				dv = rawMsg.getPM(EdlPMParser.END_TIME_STAMP);
			if (dv == null)
				dv = rawMsg.getPM(GoesPMParser.MESSAGE_TIME);
			if (dv != null)
				try 
				{
					msg.setCarrierStop(dv.getDateValue());
					msg.setXmitTime(dv.getDateValue());
				} catch(Exception ex) {}
			else
				msg.setXmitTime(now);
			
			msg.setDataSourceId(parent.getDataSourceId());
			Variable stationVar = rawMsg.getPM(EdlPMParser.STATION);
			if (stationVar == null)
				throw new HeaderParseException("Message missing required '" + EdlPMParser.STATION
					+ "' setting in header.");
			msg.setDcpAddress(new DcpAddress(stationVar.toString()));
			msg.setFailureCode('G'); // should there be anything else?
			msg.setFlagbits(
				DcpMsgFlag.MSG_PRESENT
				| DcpMsgFlag.SRC_NETDCP
	            | DcpMsgFlag.HAS_CARRIER_TIMES
				| DcpMsgFlag.MSG_TYPE_NETDCP
	            | DcpMsgFlag.MSG_NO_SEQNUM);
;
			msg.setHeaderLength(pmParser.getHeaderLength());
			parent.saveMessage(msg);
		}
		catch (IOException ex)
		{
			Logger.instance().warning(module + " Error reading file '" + file.getPath() + "': " + ex);
		}
		catch (HeaderParseException ex)
		{
			Logger.instance().warning(module + " Error parsing header for '" + file.getPath() + "': " + ex);
		}
		
		if (doneDir != null)
			try
			{
				FileUtil.moveFile(file, new File(doneDir, file.getName()));
			}
			catch (IOException e)
			{
				Logger.instance().warning("Cannot move '" + file.getPath() + "' to " + doneDir.getPath());
				file.delete();
			}
		else
			file.delete();
	}

	@Override
	protected void finishedScan()
	{
		Logger.instance().debug3(module + " finished scan.");
	}

	@Override
	protected void cleanup()
	{
	}

	public long getStartTime()
	{
		return startTime;
	}

}
