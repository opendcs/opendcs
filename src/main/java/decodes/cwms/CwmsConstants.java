/*
 * $Id$
 * 
 * $Log$
 * 
 * This software was written by Cove Software, LLC ("COVE") under contract 
 * to the United States Government. 
 * 
 * No warranty is provided or implied other than specific contractual terms
 * between COVE and the U.S. Government
 * 
 * Copyright 2016 U.S. Army Corps of Engineers, Hydrologic Engineering Center.
 * All rights reserved.
 */
package decodes.cwms;

/**
 * This class contains various numeric and string constants used by
 * the Cwms Consumer.
 */
public class CwmsConstants
{
	/** The file path to get the Cwms Configuration file */
	public static final String CONFIG_FILE_NAME = 
									"$DCSTOOL_USERDIR/decodes-cwms.conf";
	/** The Sensor Cwms data type used by CwmsConsumer */
	public static final String CWMS_DATA_TYPE = "CWMS";
	/** The default value for the Time Series descriptor Parameter Type */
	public static final String PARAM_TYPE_INST = "Inst";
	/** The default version value for the Time Series descriptor Version */
	public static final String DEFAULT_VERSION_VALUE = "raw";
	/** Store rule value REPLACE_ALL for Cwms store_ts procedure */
	public static final String REPLACE_ALL = "REPLACE ALL";
	/** Store rule value DO_NOT_REPLACE for Cwms store_ts procedure */
	public static final String DO_NOT_REPLACE = "DO NOT REPLACE";
	/** Store rule value REPLACE_MISSING_VALUES_ONLY for 
		Cwms store_ts procedure */
	public static final String REPLACE_MISSING_VALUES_ONLY  = 
												"REPLACE MISSING VALUES ONLY";
	/** Store rule value REPLACE_WITH_NON_MISSING for 
		Cwms store_ts procedure */
	public static final String REPLACE_WITH_NON_MISSING = 
												"REPLACE WITH NON MISSING";
	/** Store rule value DELETE_INSERT for Cwms store_ts procedure */
	public static final String DELETE_INSERT = "DELETE INSERT";
	/** The Constant to get site name type from properties */
	public static final String SITE_NAME_TYPE = "sitenametype";
	/** The Constant to get Cwms office id from properties */
	public static final String CWMS_OFFICE_ID = "cwmsofficeid";
	/** The Constant to get Cwms param type from properties */
	public static final String CWMS_PARAM_TYPE = "CwmsParamType";
	/** The Constant to get Cwms duration from properties */
	public static final String CWMS_DURATION = "CwmsDuration";
	public static final String CWMS_INTERVAL = "cwmsInterval";
	public static final String CWMS_VERSION = "CwmsVersion";
	public static final String CWMS_STORE_RULE = "storerule";
	public static final String CWMS_OVERRIDE_PROT = "overrideprot";
	public static final String CWMS_VERSION_DATE = "versiondate";
	/** The Constant to get debug flag from properties */
	public static final String DEBUG_FLAG = "debugflag";
	
	/** The following Constants are used to map from SHEF to CWMS code */
	public static final String PC = "PC";
	public static final String PRECIP = "Precip";
	public static final String HG = "HG";
	public static final String STAGE = "Stage";
    public static final String STAGE_POOL = "Stage-Pool";
    public static final String STAGE_TAIL = "Stage-Tail";
	public static final String HP = "HP";
	public static final String HT = "HT";
	public static final String VB = "VB";
	public static final String VOLT = "Volt";
	public static final String BV = "BV";
	public static final String HR = "HR";
	public static final String ELEV = "Elev";
	public static final String LF = "LF";
	public static final String STOR = "Stor";
	public static final String QI = "QI";
	public static final String FLOW_IN = "Flow-In";
	public static final String QR = "QR";
	public static final String FLOW = "Flow";
	public static final String FLOW_OUT = "Flow-Out";
	public static final String TA = "TA";
	public static final String TEMP_AIR = "Temp-Air";
	public static final String TW = "TW";
	public static final String TEMP_WATER = "Temp-Water";
	public static final String US = "US";
	public static final String SPEED_WIND = "Speed-Wind";
	public static final String UP = "UP";
	public static final String UD = "UD";
	public static final String DIR_WIND = "Dir-Wind";
}
