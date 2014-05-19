package lrgs.common;

/**
Constant error codes, names, and descriptions.
*/
public class LrgsErrorCode
{
	/* IMPORTANT: These values must stay in sync with the native #define
	   values defined in g_error.h.
	*/
	public static final int DSUCCESS     =  0;
	public static final int DNOFLAG      =  1;
	public static final int DDUMMY       =  2;
	public static final int DLONGLIST    =  3;
	public static final int DARCERROR    =  4;
	public static final int DNOCONFIG    =  5;
	public static final int DNOSRCHSHM   =  6;
	public static final int DNODIRLOCK   =  7;
	public static final int DNODIRFILE   =  8;
	public static final int DNOMSGFILE   =  9;
	public static final int DDIRSEMERR   = 10;
	public static final int DMSGTIMEOUT  = 11;
	public static final int DNONETLIST   = 12;
	public static final int DNOSRCHCRIT  = 13;
	public static final int DBADSINCE    = 14;
	public static final int DBADUNTIL    = 15;
	public static final int DBADNLIST    = 16;
	public static final int DBADADDR     = 17;
	public static final int DBADEMAIL    = 18;
	public static final int DBADRTRAN    = 19;
	public static final int DNLISTXCD    = 20;
	public static final int DADDRXCD     = 21;
	public static final int DNOLRGSLAST  = 22;
	public static final int DWRONGMSG    = 23;
	public static final int DNOMOREPROC  = 24;
	public static final int DBADDAPSSTAT = 25;
	public static final int DBADTIMEOUT  = 26;
	public static final int DCANTIOCTL   = 27;
	public static final int DUNTILDRS    = 28;
	public static final int DBADCHANNEL  = 29;
	public static final int DCANTOPENSER = 30;
	public static final int DBADDCPNAME  = 31;
	public static final int DNONAMELIST  = 32;
/* This is the end of the legacy DRS compatible codes. */
	public static final int DIDXFILEIO   = 33;
	public static final int DNOSRCHSEM   = 34;

// Reuse 34 for LRGS 6.
	public static final int DBADSEARCHCRIT = 34;

	public static final int DUNTIL       = 35;
	public static final int DJAVAIF      = 36;
	public static final int DNOTATTACHED = 37;
	public static final int DBADKEYWORD  = 38;
	public static final int DPARSEERROR  = 39;
	public static final int DNONAMELISTSEM = 40;
	public static final int DBADINPUTFILE= 41;
	public static final int DARCFILEIO   = 42;
	public static final int DNOARCFILE   = 43;
	public static final int DICPIOCTL    = 44;
	public static final int DICPIOERR    = 45;
	public static final int DINVALIDUSER = 46;
	public static final int DDDSAUTHFAILED = 47;
	public static final int DDDSINTERNAL = 48;
	public static final int DDDSFATAL = 49;
	public static final int DNOSUCHSOURCE = 50;
	public static final int DALREADYATTACHED = 51;
	public static final int DNOSUCHFILE = 52;
	public static final int DTOOMANYDCPS = 53;

	public static final int DMAXERROR    = 53;

	/**
	  Returns the name corresponding to an LRGS error code.
	*/
	public static String code2string(int code)
	{
		switch(code)
		{
		case DSUCCESS: return "DSUCCESS";
		case DNOFLAG: return "DNOFLAG";
		case DDUMMY: return "DDUMMY";
		case DLONGLIST: return "DLONGLIST";
		case DARCERROR: return "DARCERROR";
		case DNOCONFIG: return "DNOCONFIG";
		case DNOSRCHSHM: return "DNOSRCHSHM";
		case DNODIRLOCK: return "DNODIRLOCK";
		case DNODIRFILE: return "DNODIRFILE";
		case DNOMSGFILE: return "DNOMSGFILE";
		case DDIRSEMERR: return "DDIRSEMERR";
		case DMSGTIMEOUT: return "DMSGTIMEOUT";
		case DNONETLIST: return "DNONETLIST";         /* used */
		case DNOSRCHCRIT: return "DNOSRCHCRIT";
		case DBADSINCE: return "DBADSINCE";
		case DBADUNTIL: return "DBADUNTIL";
		case DBADNLIST: return "DBADNLIST";
		case DBADADDR: return "DBADADDR";
		case DBADEMAIL: return "DBADEMAIL";
		case DBADRTRAN: return "DBADRTRAN";
		case DNLISTXCD: return "DNLISTXCD";
		case DADDRXCD: return "DADDRXCD";
		case DNOLRGSLAST: return "DNOLRGSLAST";
		case DWRONGMSG: return "DWRONGMSG";
		case DNOMOREPROC: return "NOMOREPROC";
		case DBADDAPSSTAT: return "DBADDAPSSTAT";
		case DBADTIMEOUT: return "DBADTIMEOUT";
		case DCANTIOCTL: return "DCANTIOCTL";
		case DUNTILDRS: return "DUNTILDRS";
		case DBADCHANNEL: return "DBADCHANNEL";
		case DCANTOPENSER: return "DCANTOPENSER";
		case DBADDCPNAME: return "DBADDCPNAME";
		case DNONAMELIST: return "DNONAMELIST";

		case DIDXFILEIO: return "DIDXFILEIO";
		case DNOSRCHSEM: return "DBADSEARCHCRIT";
		case DUNTIL: return "DUNTIL";
		case DJAVAIF: return "DJAVAIF";
		case DNOTATTACHED: return "DNOTATTACHED";       /* Used */
		case DBADKEYWORD: return "DBADKEYWORD";         /* Used */
		case DPARSEERROR: return "DPARSEERROR";
		case DNONAMELISTSEM: return "DNONAMELISTSEM";
		case DBADINPUTFILE: return "DBADINPUTFILE";
		case DARCFILEIO: return "DARCFILEIO";
		case DNOARCFILE: return "DNOARCFILE";
		case DICPIOCTL: return "DICPIOCTL";
		case DICPIOERR: return "DICPIOERR";
		case DINVALIDUSER: return "DINVALIDUSER";       /* Used */
		case DDDSAUTHFAILED: return "DDDSAUTHFAILED";   /* Used */
		case DDDSINTERNAL: return "DDDSINTERNAL";
		case DDDSFATAL: return "DDSFATAL"; 
		case DNOSUCHSOURCE: return "DNOSUCHSOURCE";
		case DALREADYATTACHED: return "DALREADYATTACHED"; /* Used */
		case DNOSUCHFILE: return "DNOSUCHFILE";           /* Used */
		case DTOOMANYDCPS: return "DTOOMANYDCPS";         /* Used */

		default:
			return "UNKNOWN";
		}
	}

	/**
	  Returns an explanatory message for an LRGS error code.
	*/
	public static String code2message(int code)
	{
		switch(code)
		{
		case DSUCCESS: return "Success.";
		case DNOFLAG: return "Could not find start of message flag.";
		case DDUMMY : return "Message found (and loaded) but it's a dummy.";
		case DLONGLIST: return "Network list was too long to upload.";
		case DARCERROR: return "Error reading archive file.";
		case DNOCONFIG: return "Cannot attach to configuration shared memory";
		case DNOSRCHSHM: return "Cannot attach to search shared memory";
		case DNODIRLOCK: return "Could not get ID of directory lock semephore";
		case DNODIRFILE: return "Could not open message directory file";
		case DNOMSGFILE: return "Could not open message storage file";
		case DDIRSEMERR: return "Error on directory lock semephore";
		case DMSGTIMEOUT: return "Timeout waiting for new messages";
		case DNONETLIST: return "Could not open network list file";
		case DNOSRCHCRIT: return "Could not open search criteria file";
		case DBADSINCE: return "Bad since time in search criteria file";
		case DBADUNTIL: return "Bad until time in search criteria file";
		case DBADNLIST: return "Bad network list in search criteria file";
		case DBADADDR: return "Bad DCP address in search criteria file";
		case DBADEMAIL: return "Bad electronic mail value in search criteria file";
		case DBADRTRAN: return "Bad retransmitted value in search criteria file";
		case DNLISTXCD: return "Number of network lists exceeded";
		case DADDRXCD: return "Number of DCP addresses exceeded";
		case DNOLRGSLAST: return "Could not open last read access file";
		case DWRONGMSG: return "Message doesn't correspond with directory entry";
		case DNOMOREPROC: return "Can't attach: No more proccesses allowed";
		case DBADDAPSSTAT: return "Bad DAPS status specified in search criteria.";
		case DBADTIMEOUT: return "Bad TIMEOUT value in search crit file.";
		case DCANTIOCTL: return "Cannot ioctl() the open serial port.";
		case DUNTILDRS: return "Specified 'until' time reached";
		case DBADCHANNEL: return "Bad GOES channel number specified in search crit";
		case DCANTOPENSER: return "Can't open specified serial port.";
		case DBADDCPNAME: return "Unrecognized DCP name in search criteria";
		case DNONAMELIST: return "Cannot attach to name list shared memory.";
		case DIDXFILEIO: return "Index file I/O error";
		case DNOSRCHSEM : return "Bad search-criteria data";
		case DUNTIL: return "Specified 'until' time reached";
		case DJAVAIF    : return "Error in Java - Native Interface";
		case DNOTATTACHED: return "Not attached to LRGS native interface";
		case DBADKEYWORD: return "Bad keyword";
		case DPARSEERROR: return "Error parsing input file";
		case DNONAMELISTSEM: return "Cannot attach to name list semaphore.";
		case DBADINPUTFILE: return "Cannot open or read specified input file";
		case DARCFILEIO: return "Archive file I/O error";
		case DNOARCFILE: return "Archive file not opened";
		case DICPIOCTL: return "Error on ICP188 ioctl call";
		case DICPIOERR: return "Error on ICP188 I/O call";
		case DINVALIDUSER: return "Invalid DDS User";
		case DDDSAUTHFAILED: return "DDS Authentication failed";
		case DDDSINTERNAL: return "DDS Internal Error (connection will close)";
		case DDDSFATAL: return "DDS Fatal Server Error (retry later)"; 
		case DNOSUCHSOURCE: return "No such data source";
		case DALREADYATTACHED: return "User already attached (mult disallowed)";
		case DNOSUCHFILE: return "No such file";
		case DTOOMANYDCPS: return "Too many DCPs for real-time stream";

		default:
			return "UNKNOWN";
		}
	}
}
