/*
*  $Id$
*/
package lritdcs;

/**
 Symbolic constants used in several places of LRIT DCS.
*/
public class Constants
{
	public static final String version = "5.2";
	public static final String releaseDate = "Dec 12, 2012";
	public static final int MaxDdsServers = 3;

	public static final char HighPri = 'H';
	public static final char MediumPri  = 'M';
	public static final char LowPri  = 'L';
	public static final char ManualRetransPri  = 'N';

	public static final char SC_East = 'e';
	public static final char SC_West = 'w';
	public static final char SC_Both = '\0';

	public static final int EVT_NO_DATA_SOURCE = 1;
	public static final int EVT_DATA_SOURCE_ERR = 2;
	public static final int EVT_MSG_DISCARDED = 3;
	public static final int EVT_INTERNAL_ERR = 4;
	public static final int EVT_LQM_CON_FAILED = 5;
	public static final int EVT_NO_LQM = 6;
	public static final int EVT_LQM_INVALID_HOST = 7;
	public static final int EVT_LQM_BAD_CONFIG = 8;
	public static final int EVT_LQM_LISTEN_ERR = 9;
	public static final int EVT_BAD_CONFIG = 10;
	public static final int EVT_CONFIG_PROPERTY = 11;
	public static final int EVT_INIT_FAILED = 12;
	public static final int EVT_SEARCHCRIT = 13;
	public static final int EVT_FILE_SAVE_ERR = 14;
	public static final int EVT_UI_LISTEN_ERR = 15;
	public static final int EVT_FILE_DELETE_ERR = 16;
	public static final int EVT_FILE_MOVE_ERR = 17;
	public static final int EVT_SSH_EXEC_ERR = 18;
	public static final int EVT_PENDING_TIMEOUT = 19;
	public static final int EVT_UI_INVALID_HOST = 20;
	public static final int EVT_LQM_CON_LOST = 21;

}
