/**
 * $Id$
 * 
 * Copyright 2019 U.S. Government.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * $Log$
 */
package lrgs.lrgsmain;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.Date;
import java.util.HashMap;

import ilex.util.DirectoryMonitorThread;
import ilex.util.EnvExpander;
import ilex.util.FileUtil;
import ilex.util.Logger;
import lrgs.archive.MsgArchive;
import lrgs.common.DcpMsg;
import lritdcs.HritDcsFileReader;
import lritdcs.HritException;

public class HritFileInterface
	extends DirectoryMonitorThread
	implements LrgsInputInterface, FilenameFilter
{
	private int slot = -1;
	private String module = "HritFile";
	private int statusCode = LrgsInputInterface.DL_DISABLED;
	private String status = "disabled";
	private MsgArchive msgArchive;
	private String fnPrefix = null, fnSuffix = null;
	private File doneDir = null;
	private byte[] defaultSourceCode = null;
	private int timeoutSec = 120;
	private int fileMaxAgeSec = 7200;
	private long lastConfigMsec = 0L;
	private long lastMsgRecvd = 0L;
	private int dataSrcId = -1;
	private boolean enabled = false;
	
	private class RetryEntry
	{
		public RetryEntry(String path)
		{
			super();
			this.path = path;
			this.lastTried = System.currentTimeMillis();
		}
		String path;
		long lastTried;
	};
	HashMap<String, RetryEntry> retryList = new HashMap<String, RetryEntry>();


	public HritFileInterface(LrgsMain lrgsMain, MsgArchive msgArchive)
	{
		super();
		this.msgArchive = msgArchive;
		this.setSleepEveryCycle(true);
		this.setSleepInterval(1000L);
		this.setFilenameFilter(this);
		dataSrcId = lrgsMain.getDbThread().getDataSourceId(DL_LRIT_TYPESTR, "HRIT");
	}

	@Override
	public int getType()
	{
		return DL_LRIT;
	}

	@Override
	public void setSlot(int slot)
	{
		this.slot = slot;
	}

	@Override
	public int getSlot()
	{
		return slot;
	}

	@Override
	public String getInputName()
	{
		return module;
	}

	@Override
	public void initLrgsInput() throws LrgsInputException
	{
		Logger.instance().info(module + " initializing.");
		statusCode = DL_INIT;
		status = "Init";
		
		// Get initial configuration
		getMyConfig();
		
		// Start the DirectoryMonitorThread. It will start scanning.
		start();
	}
	
	private void getMyConfig()
	{
		Logger.instance().debug1(module + " resetting configuration.");
		LrgsConfig cfg = LrgsConfig.instance();
		
		fnPrefix = cfg.hritFilePrefix;
		if (fnPrefix != null && fnPrefix.trim().length() == 0)
			fnPrefix = null;
		fnSuffix = cfg.hritFileSuffix;
		if (fnSuffix != null && fnSuffix.trim().length() == 0)
			fnSuffix = null;
		String s = cfg.hritDoneDir;
		if (s != null && !s.trim().contains("null") && !s.trim().equalsIgnoreCase("none"))
			doneDir = new File(EnvExpander.expand(s.trim()));
		else
			doneDir = null;
		s = cfg.hritSourceCode;
		if (s != null && s.length() >= 2)
		{
			defaultSourceCode = new byte[2];
			defaultSourceCode[0] = (byte)s.charAt(0);
			defaultSourceCode[1] = (byte)s.charAt(1);
		}
		this.timeoutSec = cfg.hritTimeoutSec;
		this.fileMaxAgeSec = cfg.hritFileMaxAgeSec;
		
		this.emptyDirectories();
		s = cfg.hritInputDir;
		if (s == null || s.trim().length() == 0)
		{
			enableLrgsInput(false);
		}
		else
		{
			File idir = new File(EnvExpander.expand(s.trim()));
			if (!idir.isDirectory())
				idir.mkdirs();
			addDirectory(idir);
		}
		enableLrgsInput(cfg.hritFileEnabled);
		
		lastConfigMsec = System.currentTimeMillis();
	}

	@Override
	public void shutdownLrgsInput()
	{
		// Shutdown the directory monitor thread
		shutdown();
		status = "Shutdown";
		statusCode = DL_DISABLED;
	}

	@Override
	public void enableLrgsInput(boolean enabled)
	{
		if (this.enabled == enabled)
			return;
		
		this.enabled = enabled;
		status = enabled ? "Running" : "Disabled";

		if (enabled)
		{
			getMyConfig();
			lastMsgRecvd = System.currentTimeMillis();
		}
	}

	@Override
	public boolean hasBER() { return false; }

	@Override
	public String getBER() { return ""; }

	@Override
	public boolean hasSequenceNums() { return true; }

	@Override
	public int getStatusCode() { return statusCode; }

	@Override
	public String getStatus() { return status; }

	@Override
	public int getDataSourceId() { return dataSrcId; }

	@Override
	public boolean getsAPRMessages() { return true; }

	@Override
	public String getGroup() { return null; }

	@Override
	protected void processFile(File file)
	{
		if (statusCode == DL_TIMEOUT)
		{
			Logger.instance().info(module + " Timeout Recovery. Receiving data again.");
			statusCode = DL_ACTIVE;
			status = "Running";
		}
		if (statusCode == DL_DISABLED)
			// Finished scan will pause until enabled or until shutdown.
			finishedScan();
		
		RetryEntry retryEntry = retryList.get(file.getPath());
		if (retryEntry != null
		 && System.currentTimeMillis() - retryEntry.lastTried < 10000L)
			return;

		if (System.currentTimeMillis() - file.lastModified() > (fileMaxAgeSec*1000L))
		{
			Logger.instance().info(module + " discarding file '" + file.getPath() 
				+ "' because it's too old. Last modified on " + new Date(file.lastModified()));
			finishFile(file);
			return;
		}
	
		Logger.instance().debug1(module + " processing file '" + file.getPath() + "'");
		HritDcsFileReader reader = new HritDcsFileReader(file.getPath(), true);
		try
		{
			reader.load();
			reader.checkHeader();
			DcpMsg msg = null;
			while((msg = reader.getNextMsg()) != null && !isShutdown)
			{
				msg.setDataSourceId(dataSrcId);
				msgArchive.archiveMsg(msg, this);
				lastMsgRecvd = System.currentTimeMillis();
			}
			finishFile(file);
		}
		catch(IOException ex)
		{			
			Logger.instance().warning(module + " I/O Error reading file '" + file.getPath() + "': " + ex);
			finishFile(file);
		}
		catch (HritException ex)
		{
			if (ex.getErrorCode() == HritException.ErrorCode.INCOMPLETE_FILE)
			{
				if (System.currentTimeMillis() - file.lastModified() > 60000L)
				{
					Logger.instance().warning(module + " Incomplete file '" + file.getPath() 
						+ "': " + ex + " -- has not changed in >60 sec.");
					finishFile(file);
				}
				else
				{
					Logger.instance().warning(module + " Incomplete file '" + file.getPath() 
					+ "': " + ex + " -- will ignore for 10 sec.");
					if (retryEntry == null)
						retryList.put(file.getPath(), new RetryEntry(file.getPath()));
				}
			}
			else
			{
				Logger.instance().warning(module + " Invalid file '" + file.getPath() + "': " + ex);
				finishFile(file);
			}
		}
	}
	
	private void finishFile(File file)
	{
		retryList.remove(file.getPath());
		if (doneDir != null)
		{
			try
			{
				FileUtil.moveFile(file, new File(doneDir, file.getName()));
				return;
			}
			catch(IOException ex2)
			{
				Logger.instance().warning(module + " Cannot move file '"
					+ file.getPath() + "' to directory '" + doneDir.getPath() + "': " 
					+ ex2);
			}
		}
		if (!file.delete())
		{
			Logger.instance().warning(module + " Cannot delete file '" + file.getPath() + "'");
			retryList.put(file.getPath(), new RetryEntry(file.getPath()));
		}
	}

	@Override
	protected void finishedScan()
	{
		Logger.instance().debug1(module + " finishedScan");
		if (lastConfigMsec < LrgsConfig.instance().getLastLoadTime())
			getMyConfig();

		if (statusCode == DL_ACTIVE
		 && System.currentTimeMillis() - lastMsgRecvd > (timeoutSec*1000L))
		{
			statusCode = DL_TIMEOUT;
			status = "Timeout";
			Logger.instance().warning(module + " TIMEOUT: No file received in > " + timeoutSec + " sec.");
		}
		
		// Handle periods of being disabled -- this shuts down the file scanning.
		while(!isShutdown && !enabled && !LrgsConfig.instance().hritFileEnabled)
			try { sleep(2000L); }
			catch(InterruptedException ex) {}
	}
	
	public void run()
	{
		// Handle initial period before we are enabled. Don't start file scanning until that happens.
		while(!LrgsConfig.instance().hritFileEnabled)
			try { sleep(2000L); }
			catch(InterruptedException ex) {}
		
		// Start the file scanner
		super.run();
	}

	@Override
	protected void cleanup()
	{
		statusCode = DL_DISABLED;
	}

	@Override
	public boolean accept(File dir, String name)
	{
Logger.instance().info(module + " " + name);
		if (fnPrefix != null && !name.startsWith(fnPrefix))
			return false;
		if (fnSuffix != null && !name.endsWith(fnSuffix))
			return false;
		
		return true;
	}
}
