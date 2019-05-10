/*
 * $Id$
 * 
 * Copyright 2017 Cove Software, LLC. All rights reserved.
 * 
 * $Log$
 * Revision 1.1  2019/03/05 14:53:01  mmaloney
 * Checked in partial implementation of Alarm classes.
 *
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

	// File containing multiple alarm screeninga;
	public static final String AlarmDefinitions = "AlarmDefinitions";

	// Tags for ALARM_SCREENING
	public static final String AlarmScreening = "AlarmScreening";
	// Note Site and DataType are represented as they are in a computation
	public static final String startDateTime = "startDateTime";
	public static final String alarmGroupName = "alarmGroupName";
	public static final String desc = "desc";
	
	// Tags for ALARM_LIMIT_SET
	public static final String AlarmLimitSet = "AlarmLimitSet";
	public static final String screeningName = "screeningName";
	public static final String seasonName = "season";
	public static final String rejectHigh = "rejectHigh";
	public static final String criticalHigh = "criticalHigh";
	public static final String warningHigh = "warningHigh";
	public static final String warningLow = "warningLow";
	public static final String criticalLow = "criticalLow";
	public static final String rejectLow = "rejectLow";
	public static final String stuckDuration = "stuckDuration";
	public static final String stuckTolerance = "stuckTolerance";
	public static final String stuckMinToCheck = "stuckMinToCheck";
	public static final String stuckMaxGap = "stuckMinToCheck";
	public static final String rocInterval = "rocInterval";
	public static final String rejectRocHigh = "rejectRocHigh";
	public static final String criticalRocHigh = "criticalRocHigh";
	public static final String warningRocHigh = "warningRocHigh";
	public static final String warningRocLow = "warningRocLow";
	public static final String criticalRocLow = "criticalRocLow";
	public static final String rejectRocLow = "rejectRocLow";
	public static final String missingPeriod = "missingPeriod";
	public static final String missingInterval = "missingInterval";
	public static final String missingMaxValues = "missingMaxValues";

	
	
}
