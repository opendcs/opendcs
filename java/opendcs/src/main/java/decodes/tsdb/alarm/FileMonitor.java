/*
 * $Id$
 * 
 * Copyright 2017 Cove Software, LLC. All rights reserved.
 * 
 * $Log$
 * Revision 1.6  2018/03/23 20:12:20  mmaloney
 * Added 'Enabled' flag for process and file monitors.
 *
 * Revision 1.5  2017/10/04 17:25:07  mmaloney
 * Fix AEP Bugs
 *
 * Revision 1.4  2017/05/17 20:36:26  mmaloney
 * First working version.
 *
 * Revision 1.3  2017/03/30 20:55:20  mmaloney
 * Alarm and Event monitoring capabilities for 6.4 added.
 *
 * Revision 1.2  2017/03/21 12:17:10  mmaloney
 * First working XML and SQL I/O.
 *
 */
package decodes.tsdb.alarm;

import ilex.util.Logger;
import ilex.util.TextUtil;

/**
 * Information about monitoring a file or directory.
 */
public class FileMonitor
{
	private String path = null;

	/** Will generate a DACQ_EVENT with this priority level */
	private int priority = Logger.E_WARNING;

	/** For directories, generate alarm if # of files in the directory exceed this. */
	private int maxFiles = 0;
	
	/** Include this string in the alarm when generating a maxFiles alarm. */
	private String maxFilesHint = null;
	
	/**
	 * Max last-modified time. If file or directory has not been changed in more
	 * than this interval, then generate an alarm. Handles strings like "1 day",
	 * or "4 hours", or "hours*4".
	 */
	private String maxLMT = null;
	
	/** Include this string in the alarm when generating a maxFiles alarm. */
	private String maxLMTHint = null;
	
	/** For files, generate alarm if file size exceed this. */
	private long maxSize = 0L;
	
	/** Include this string in the alarm when generating a maxSize alarm. */
	private String maxSizeHint = null;
	
	/** Generate alarm the named file or directory does not exist. */
	private boolean alarmOnDelete = false;
	
	/** Include this string in the alarm when generating a alarmOnDelete alarm. */
	private String alarmOnDeleteHint = null;
	
	/** Generate alarm the named file or directory exist. */
	private boolean alarmOnExists = false;
	
	/** Include this string in the alarm when generating a alarmOnDelete alarm. */
	private String alarmOnExistsHint = null;
	
	private boolean enabled = true;
	
	/** Means one of the exists/deleted alarms is currently asserted. */
	private transient boolean existsAsserted = false;
	
	/** Means an LMT alarm is asserted. */
	private transient boolean lmtAsserted = false;
	
	/** Means either maxFiles (for directory) or maxSize (for file) is asserted */
	private transient boolean sizeAsserted = false;
	
	private transient boolean changed = true;
	private transient String description = "";

	public FileMonitor(String path)
	{
		super();
		this.path = path;
	}
	
	public FileMonitor copy()
	{
		FileMonitor ret = new FileMonitor(path);
		
		ret.priority = this.priority;
		ret.maxFiles = this.maxFiles;
		ret.maxFilesHint = this.maxFilesHint;
		ret.maxLMT = this.maxLMT;
		ret.maxLMTHint = this.maxLMTHint;
		ret.maxSize = this.maxSize;
		ret.maxSizeHint = this.maxSizeHint;
		ret.alarmOnDelete = this.alarmOnDelete;
		ret.alarmOnDeleteHint = this.alarmOnDeleteHint;
		ret.alarmOnExists = this.alarmOnExists;
		ret.alarmOnExistsHint = this.alarmOnExistsHint;

		return ret;
	}


	public String getPath()
	{
		return path;
	}

	public int getPriority()
	{
		return priority;
	}

	public void setPriority(int priority)
	{
		if (priority != this.priority)
			setChanged(true);
		this.priority = priority;
	}

	public int getMaxFiles()
	{
		return maxFiles;
	}

	public void setMaxFiles(int maxFiles)
	{
		if (maxFiles != this.maxFiles)
			setChanged(true);
		this.maxFiles = maxFiles;
	}

	public String getMaxFilesHint()
	{
		return maxFilesHint;
	}

	public void setMaxFilesHint(String maxFilesHint)
	{
		if (TextUtil.strEqual(maxFilesHint, this.maxFilesHint))
			setChanged(true);

		this.maxFilesHint = maxFilesHint;
	}

	public String getMaxLMT()
	{
		return maxLMT;
	}

	public void setMaxLMT(String maxLMT)
	{
		if (TextUtil.strEqual(maxLMT, this.maxLMT))
			setChanged(true);
		this.maxLMT = maxLMT;
	}

	public String getMaxLMTHint()
	{
		return maxLMTHint;
	}

	public void setMaxLMTHint(String maxLMTHint)
	{
		if (TextUtil.strEqual(maxLMTHint, this.maxLMTHint))
			setChanged(true);
		this.maxLMTHint = maxLMTHint;
	}

	public long getMaxSize()
	{
		return maxSize;
	}

	public void setMaxSize(long maxSize)
	{
		if (maxSize != this.maxSize)
			setChanged(true);
		this.maxSize = maxSize;
	}

	public String getMaxSizeHint()
	{
		return maxSizeHint;
	}

	public void setMaxSizeHint(String maxSizeHint)
	{
		if (TextUtil.strEqual(maxSizeHint, this.maxSizeHint))
			setChanged(true);
		this.maxSizeHint = maxSizeHint;
	}

	public boolean isAlarmOnDelete()
	{
		return alarmOnDelete;
	}

	public void setAlarmOnDelete(boolean alarmOnDelete)
	{
		if (alarmOnDelete != this.alarmOnDelete)
			setChanged(true);
		this.alarmOnDelete = alarmOnDelete;
	}

	public String getAlarmOnDeleteHint()
	{
		return alarmOnDeleteHint;
	}

	public void setAlarmOnDeleteHint(String alarmOnDeleteHint)
	{
		if (TextUtil.strEqual(alarmOnDeleteHint, this.alarmOnDeleteHint))
			setChanged(true);
		this.alarmOnDeleteHint = alarmOnDeleteHint;
	}

	public boolean isAlarmOnExists()
	{
		return alarmOnExists;
	}

	public void setAlarmOnExists(boolean alarmOnExists)
	{
		if (alarmOnExists != this.alarmOnExists)
			setChanged(true);
		this.alarmOnExists = alarmOnExists;
	}

	public String getAlarmOnExistsHint()
	{
		return alarmOnExistsHint;
	}

	public void setAlarmOnExistsHint(String alarmOnExistsHint)
	{
		if (TextUtil.strEqual(alarmOnExistsHint, this.alarmOnExistsHint))
			setChanged(true);
		this.alarmOnExistsHint = alarmOnExistsHint;
	}

	public boolean isExistsAsserted()
	{
		return existsAsserted;
	}

	public void setExistsAsserted(boolean alarmAsserted)
	{
		this.existsAsserted = alarmAsserted;
	}

	public boolean isLmtAsserted()
	{
		return lmtAsserted;
	}

	public void setLmtAsserted(boolean lmtAsserted)
	{
		this.lmtAsserted = lmtAsserted;
	}

	public boolean isSizeAsserted()
	{
		return sizeAsserted;
	}

	public void setSizeAsserted(boolean sizeAsserted)
	{
		this.sizeAsserted = sizeAsserted;
	}

	public void makeDescription()
	{
		StringBuilder sb = new StringBuilder();
		if (maxFiles > 0)
			sb.append("maxFile=" + maxFiles);
		
		if (maxLMT != null)
			sb.append((sb.length() > 0 ? ", " : "") + "maxAge=" + maxLMT);
		
		if (maxSize > 0)
			sb.append((sb.length() > 0 ? ", " : "") + "maxSize=" + maxSize);
		
		if (alarmOnDelete)
			sb.append((sb.length() > 0 ? ", " : "") + "alarmOnDelete");
		
		if (alarmOnExists)
			sb.append((sb.length() > 0 ? ", " : "") + "alarmOnExists");

		description = sb.toString();
	}

	public boolean isChanged()
	{
		return changed;
	}

	public void setChanged(boolean changed)
	{
		this.changed = changed;
		makeDescription();
	}

	public String getDescription()
	{
		return description;
	}

	public void setPath(String path)
	{
		this.path = path;
	}

	public boolean isEnabled()
	{
		return enabled;
	}

	public void setEnabled(boolean enabled)
	{
		this.enabled = enabled;
		setChanged(true);
	}

	
	

}
