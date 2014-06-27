package decodes.tsdb;

public class TsdbDatabaseVersion
{
	// V2: MY_DCP_TRANS_nn - add uplink carrier column
	//	uplinkcarrier varchar(2),
	// V2: Drop OTHER_DCP_TRANS_nn tables
	// Added column to my_dcp_trans_<suffix>
	// But we don't need to do this because we're not support dcpmon
	// for anybody using version 1.
	public static final int VERSION_2 = 2;
	
	// Version 3:
	// Add SOURCE_ID column to CP_COMP_TASKLIST
	public static final int VERSION_3 = 3;
	
	// Version 4:
	// Add FAIL_TIME column to CP_COMP_TASKLIST
	public static final int VERSION_4 = 4;

	// Version 5:
	// - Supports groups in tempest only. Several new tables.
	public static final int VERSION_5 = 5;
	
	/**
	 * Version 6:
	 * - refactor tsdb_group for inclusion with CWMS (several changes)
	 * - tsdb_group_member_other is used for version and statcode
	 * - tsdb_group_member_group has new 'include_group' attribute
	 * CP_COMPUTATION now has group_id
	 */ 
	public static final int VERSION_6 = 6;
	public static final String VERSION_6_DTK = "DCSTOOL 5.0 Dec 31, 2010";
	
	/** 7 is DCSTOOL 5.1 corresponding to CWMS 2.1
	 * - DB_OFFICE_CODE is added to the following tables:
	 *     o  CP_ALGORITHM
	 *     o  CP_COMPUTATION
	 * - It also has the additions to my_dcp_trans_xxx tables
	 */
	public static final int VERSION_7 = 7;
	public static final String VERSION_7_DTK = "DCSTOOL 5.1";
	
	/**
	 * VERSION 8 A few Modifications, mainly to support groups in HDB,
	 * also miscellaneous cleanup.
	 * - CP_COMP_TS_PARM no longer has GROUP_ID column.
	 * - CP_COMPUTATION added column GROUP_ID
	 * - CP_COMP_DEPENDS first column is called "TS_ID" only for HDB.
	 * - New table CP_COMP_DEPENDS_SCRATCHPAD for use by the new Java
	 *   daemon to keep CP_COMP_DEPENDS up to date.
	 * - For HDB DATATYPE_ID and DELTA_T_UNITS are added to CP_COMP_TS_PARM
	 *   (tempest already had this update at an earlier version)
	 * - New table CP_DEPENDS_NOTIFY for the new daemon
	 * - New HDB Table CP_TS_ID
	 * - CP_COMP_TS_PARM now has SITE_ID for future development.
	 */
	public static final int VERSION_8 = 8;
	public static final String VERSION_8_DTK = "DCSTOOL 5.2";
	
	
	/**
	 * Version 9 enhancements:
	 * - Added CP_COMP_TS_PARM.site_id
	 * - Tables my_dcp_trans_xx changed to dcp_trans_xx
	 * - All time stamps in the xmit tables are now long integer.
	 * 
	 * - CP_COMPUTATION.SEASON_ID
	 * - CP_COMP_TS_PARM.SITE_ID
	 * - INTERVAL_CODE (entire table - OpenTSDB only)
	 * - for CWMS, uses new set_ccp_session_ctx method rather than the 2.2 set_session_office_id
	 */
	public static final int VERSION_9 = 9;
	public static final String VERSION_9_DTK = "OPENDCS 6.0";
	
	/**
	 * Version 10 = OpenDCS 6.1
	 * 
	 */
	public static final int VERSION_10 = 10;
	public static final String VERSION_10_DTK = "OPENDCS 6.1";

}
