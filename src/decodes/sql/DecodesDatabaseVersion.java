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

	// Future schema changes should define DECODES_DB_15
	
}
