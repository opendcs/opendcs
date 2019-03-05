/*
 * $Id$
 * 
 * Copyright 2017 Cove Software, LLC. All rights reserved.
 * 
 * $Log$
 * Revision 1.3  2018/03/23 20:12:20  mmaloney
 * Added 'Enabled' flag for process and file monitors.
 *
 * Revision 1.2  2017/03/21 12:17:10  mmaloney
 * First working XML and SQL I/O.
 *
 */
package decodes.tsdb.alarm.xml;

/**
Constant tags for storing Comp Meta Data in XML Files.
*/
public class AlarmXioTags
{
	public static final String AlarmGroup = "AlarmGroup";
	public static final String CheckPeriodSec = "CheckPeriodSec";
	public static final String ProcessMonitor = "ProcessMonitor";
	public static final String name = "name";
	public static final String AlarmDef = "AlarmDef";
	public static final String priority = "priority";
	public static final String Pattern = "Pattern";
	public static final String FileMonitor = "FileMonitor";
	public static final String path = "path";
	public static final String MaxFiles = "MaxFiles";
	public static final String hint = "hint";
	public static final String MaxLMT = "MaxLMT";
	public static final String Email = "Email";
	public static final String OnDelete = "OnDelete";
	public static final String OnExists = "OnExists";
	public static final String MaxSize = "MaxSize";
	public static final String lastModified = "LastModified";
	public static final String enabled = "Enabled";
}
