package lrgs.db;

/**
 * This class contains various numeric and string constants used by
 * the LRGS database classes.
 */
public class LrgsConstants
{
	/** Unassigned numeric ID values */
	public static final int undefinedId = -1;
	
	/** variable used for the outage table sequence number */
	public static final String outageTable = "Outage";
	/** variable used for the dds_connection table sequence number */
	public static final String ddsConnectionTable = "Connection";
	/** variable used for the data_source table sequence number */
	public static final String dataSourceTable = "Data_source";

	/** varibles used for the outage types */
	public static final char systemOutageType = 'S';
	public static final char domsatGapOutageType = 'G';
	public static final char damsntOutageType = 'C';
	public static final char realTimeOutageType = 'R';
	public static final char missingDCPMsgOutageType = 'M';

	// used for outageStatus:
	public static final char outageStatusActive = 'A';
	public static final char outageStatusFailed = 'F';
	public static final char outageStatusPartial = 'P';
	public static final char outageStatusRecovered = 'R';
	public static final char outageStatusDeleted = 'D';

	// used for successCode:
	public static final char successfulAuthConn = 'A';
	public static final char successfulUnAuthConn = 'U';
	public static final char badPasword = 'P';
	public static final char badUserName = 'N';
	public static final char maxClientExceeded = 'M';
	public static final char badIpAddress = 'I';
	
	public static String outageTypeName(char code)
	{
		switch(code)
		{
		case systemOutageType: return "System";
		case domsatGapOutageType: return "DOMSAT Gap";
		case damsntOutageType: return "DAMS-NT Chan";
		case realTimeOutageType: return "Real-Time";
		case missingDCPMsgOutageType: return "Missing Msg";
		default: return "Other";
		}
	}

	public static String outageStatusName(char code)
	{
		switch(code)
		{
		case outageStatusActive: return "Active";
		case outageStatusFailed: return "Failed";
		case outageStatusRecovered: return "Recovered";
		case outageStatusPartial: return "Partial";
		case outageStatusDeleted: return "Deleted";
		default: return "Unknown";
		}
	}
	
	public static String successCodeName(char code)
	{
		switch(code)
		{
		case successfulAuthConn: return "Success Auth";
		case successfulUnAuthConn: return "Success UnAuth";
		case badPasword: return "Bad Password";
		case badUserName: return "Bad User Name";
		case maxClientExceeded: return "Max Client Exceeded";
		case badIpAddress: return "Bad Ip Addr";
		default: return "Unknown";
		}
	}
}
