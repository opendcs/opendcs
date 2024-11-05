package decodes.sql;

import decodes.util.DecodesVersion;

public class DecodesDatabaseVersion
{
	/** Baseline earliest version number */
	public static final int DECODES_DB_5 = 5;

	/**
	 * VERSION 6 includes the following modifications:
	 * - Added platform properties table
	 * - Added data order to decodes_script
	 * - Added default value and sort-order to enum
	 * - Added last-modify-time to network list
	 * - Added time-adjustment and preamble to transport medium
	 * - Added max decimals to data presentation
	 * - Removed max_decimals from Rounding Rule
	 * - Added elevation and elevation-units to site
	 */
	public static final int DECODES_DB_6 = 6;
	
	/**
	 * VERSION 7 includes the following modifications:
	 *  - Added platform.platform_designator
	 *  - Added USGS_DDNO to platform sensor
	 *  - Added statistics code to config sensor
	 *  - Added timezone to transport medium
	 *  - Added db-num and agency code to site-name
	 */
	public static final int DECODES_DB_7 = 7;
	
	/** Version 8 includes the SITE_PROPERTIES table */
	public static final int DECODES_DB_8 = 8;
	
	/** Version 9 had no changes but it went out in the field. Oops. */
	public static final int DECODES_DB_9 = 9;
	
	/** 
	 * Version 10 contains enhancements for OPENDCS 6.0 release.
	 * - Enum.description
	 * - Datatype.display_name
	 * - Site.active_flag, Site.Location_type, Site.Modify_time, and Site.public_name
	 * - DATAPRESENTATION.MAX_VALUE
	 * - DATAPRESENTATION.MIN_VALUE
	 * - DACQ_EVENT (entire new table)
	 * - PLATFORM_STATUS (entire new table)
	 * - SCHEDULE_ENTRY (entire new table)
	 * - SCHEDULE_ENTRY_STATUS (entire new table)
	 * - SEASON (entire new table)
	 */
	public static final int DECODES_DB_10 = 10;
	
	/**
	 * Version 11 corresponds to OPENDCS 6.1 release
	 * - NetworkListEntry adds two columns: platform_name and description
	 * - Several additions to TransportMedium to support modem and network loggers.
	 * - New tables CP_COMPOSITE_DIAGRAM and CP_COMPOSITE_MEMBER
	 * - Redefinition of table DACQ_EVENT
	 * - New table SERIAL_PORT_STATUS
	 * - Drop table SEASON
	 * - Drop CP_COMPUTATION.SEASON_ID
	 */
	public static final int DECODES_DB_11 = 11;

	/**
	 * Version 12 has minor mods concerning varchar lengths. It corresponds to OpenDCS 6.1 RC09.
	 * This update was included in a script with OpenDCS 6.1 RC11.
	 * - NETWORKLISTENTRY.PLATFORM_NAME increased from 24 to 64.
	 */
	public static final int DECODES_DB_12 = 12;

	/**
	 * Version 13 schema changes corresponds to OpenDCS 6.2 RC01
	 * - For CWMS, don't use CWMS_SEQ for SCHEDULE_ENTRY_STATUS and DACQ_EVENT, instead
	 *   use the CCP-sequences SCHEDULE_ENTRY_STATUSIdSeq and DACQ_EVENTIdSeq
	 * - Addition of CP_ALGO_SCRIPT table
	 * - Removal of unused tables CP_COMPOSITE_DIAGRAM and CP_COMPOSITE_MEMBER.
	 */
	public static final int DECODES_DB_13 = 13;

	/**
	 * Version 14 schema corresponds to OpenDCS 6.3
	 * - For CWMS, add db_office_code to cp_comp_depends, cp_comp_depends_scratchpad,
	 *   and cp_depends_notify
	 * - For CWMS, add cp_depends_notifyseq
	 */
	public static final int DECODES_DB_14 = 14;

	/**
	 * Version 15 schema corresponds to OpenDCS 6.4
	 * - Add column loading_application_id to dacq_event for all versions.
	 * - Added Alarm tables (Azul only).
	 * - Updated HDB 'Archive' tables to have same column lengths as orig tables.
	 */
	public static final int DECODES_DB_15 = 15;
	
	/**
	 * Version 16 schema corresponds to OpenDCS 6.5 - Changes for OpenTSDB
	 * - Add ts_specidseq (affects OpenTSDB Only)
	 * - tsdb_data_source redefined with 3 columns: id, appId, and module.
	 * - Added foreign keys (source_id) from tsdb data tables (both string and num)
	 * - Added foreign key (source_id) from cp_comp_tasklist
	 * - Added data_entry_time to tsdb data tables (both string and num)
	 * - Added separate index on data_entry_time to all data tables.
	 * - Added enabled column for Alarm tables.
	 */
	public static final int DECODES_DB_16 = 16;

	/**
	 * Version 17 schema corresponds to OpenDCS 6.6 - March, 2019
	 * - CWMS VPD to use CWMS_ENV to store office id & code rather than CCP_ENV.
	 * - Alarm Tables Added:
	 * 		ALARM_GROUP
	 * 		EMAIL_ADDR
	 * 		FILE_MONITOR
	 * 		PROCESS_MONITOR
	 * 		ALARM_EVENT
	 * 		ALARM_SCREENING
	 * 		ALARM_LIMIT_SET
	 * 		ALARM_CURRENT
	 * 		ALARM_HISTORY
	 */
	public static final int DECODES_DB_17 = 17;

	/**
	 * Version 67 schema corresponds to OpenDCS 6.7
	 * Keep This number in sync with decodes/tsdb/TsdbDatabaseVersion.
	 * - Defined OpenTSDB Computation Trigger in the sql install files
	 * - Added OpenTSDB Computation Trigger to opendcs.dbupdate.DbUpdate java code.
	 * - Added CP_DEPENDS_NOTIFYIdSeq - This was missing in OpenTSDB.
	 */
	public static final int DECODES_DB_67 = 67;
	

	/**
	 * Version 68 schema corresponds to OpenDCS 6.8
	 * Keep This number in sync with decodes/tsdb/TsdbDatabaseVersion.
	 * - Add LOADING_APPLICATION_ID to ALARM_SCREENING and add it to the 
	 *   CONSTRAINT AS_SDI_START_UNIQUE. Thus There can be multiple site/datatype/start
	 *   tuples as long as they have different loading applications.
	 * - For CWMS, 6.8 also adds separate sequences for each CCP and DECODES table, so
	 *   that it doesn't use the CWMS_SEQ at all any more.
	 */
	public static final int DECODES_DB_68 = 68;
	
}
