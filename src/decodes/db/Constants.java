/*
*  $Id$
*
*  $Log$
*  Revision 1.4  2015/01/06 16:09:31  mmaloney
*  First cut of Polling Modules
*
*  Revision 1.3  2014/09/25 18:07:11  mmaloney
*  Added Seasons Enum with Editor.
*
*  Revision 1.2  2014/09/15 13:57:41  mmaloney
*  Code cleanup.
*
*  Revision 1.1.1.1  2014/05/19 15:28:59  mmaloney
*  OPENDCS 6.0 Initial Checkin
*
*  Revision 1.18  2013/03/21 18:27:39  mmaloney
*  DbKey Implementation
*
*  Revision 1.17  2012/04/09 15:27:54  mmaloney
*  Added EUMETSAT medium type.
*
*  Revision 1.16  2012/02/16 20:42:21  mmaloney
*  Added support for SutronLoggerCsvPMParser
*
*  Revision 1.15  2011/11/29 16:06:57  mmaloney
*  Added METAR medium type.
*
*  Revision 1.14  2011/09/27 01:23:33  mmaloney
*  Enhancements for SHEF and NOS Decoding.
*
*  Revision 1.13  2011/08/26 19:44:45  mmaloney
*  Added NOS constants for site name type and data type.
*
*  Revision 1.12  2010/06/21 20:38:32  shweta
*  *** empty log message ***
*
*  Revision 1.11  2010/06/21 13:35:30  shweta
*  added operation type
*
*  Revision 1.10  2010/01/29 21:24:06  mjmaloney
*  DCP Communications Classes Prototype
*
*  Revision 1.9  2010/01/07 22:02:09  shweta
*  ICC enhancements
*
*  Revision 1.8  2009/08/12 19:56:14  mjmaloney
*  usgs merge
*
*/
package decodes.db;

import java.util.Calendar;
import java.text.SimpleDateFormat;

import decodes.sql.DbKey;

/**
 * This class contains various numeric and string constants used by
 * the database classes.
 * <p>
 *   Note that in many places, there are constant strings defined here
 *   which must match, ignoring case, the Enum Values defined in the
 *   XML file enum/EnumList.xml of the database (or in a corresponding
 *   table of an SQL database).
 * </p>
 */
public class Constants
{
//	public static final int undefinedId = -1;  // Unassigned numeric ID values
	public static final DbKey undefinedId = DbKey.createDbKey(-1L);
	
	// ModelRunId and ModelId will remain integers for HDB.
	public static final int undefinedIntKey = -1;
	
	public static final double undefinedDouble = Double.MAX_VALUE;

  // Enum "UnitFamily" values

	public static final String unitFamilyEnglish = "English";
	public static final String unitFamilyMetric  = "Metric";


  // Selected values from the Enum "TransportMediumType"

	public static final String medium_Goes   = "GOES";
	public static final String medium_GoesST = "GOES-Self-Timed";
	public static final String medium_GoesRD = "GOES-Random";
	public static final String medium_EDL    = "data-logger";
	public static final String medium_Other  = "other";
	public static final String medium_mbfire = "mbfire";
	public static final String medium_MODEM  = "modem";
	public static final String medium_RADIO  = "radio";
	public static final String medium_IRIDIUM = "iridium";
	public static final String medium_BACKUP = "backup";
	public static final String medium_FILE   = "data-file";
	public static final String medium_ADAPS  = "ADAPS-archived-uvs";
	public static final String medium_OBSERVER = "observer";
	public static final String medium_NETDCP = "netdcp";
	public static final String medium_SHEF   = "shef";
	public static final String medium_METAR  = "metar";
	public static final String medium_SutronCSV = "sutron_logger_csv";
	public static final String medium_Eumetsat = "EUMETSAT";

	// Selected values from the Enum "ContactMedium"
	public static final String contMedium_IRIDIUM = "iridium";

  // These three don't correspond to any enumvalues

	public static final String scriptTypeDecodes = "Decodes";
	public static final String script_ST = "ST";
	public static final String script_RD = "RD";
	public static final String script_EDL = "EDL";


  // Enum "DataOrder".  Note that in the EnumList.xml file, there is no
  // value for 'U', undefined.  Should there be?

	public static final char dataOrderAscending = 'A';
	public static final char dataOrderDescending = 'D';
	public static final char dataOrderUndefined = 'U';

  // Enum "RecordingMode".  Note that in the EnumList.xml file, there is
  // no value for 'U', undefined.

	public static final char recordingModeFixed = 'F';
	public static final char recordingModeVariable = 'V';
	public static final char recordingModeUndefined = 'U';


	public static final String defaultDateFormat_fmt = "MM/dd/yyyy HH:mm:ss";
	public static final SimpleDateFormat defaultDateFormat;

	// Date format for XML file suffixes:
	public static final String suffixDateFormat_fmt = "yyyyMMddHHmmss";
	public static final SimpleDateFormat suffixDateFormat;

	static  // Set both default date formatters to UTC!
	{
		defaultDateFormat = new SimpleDateFormat(defaultDateFormat_fmt);
		suffixDateFormat = new SimpleDateFormat(suffixDateFormat_fmt);
		Calendar cal = Calendar.getInstance(
			java.util.TimeZone.getTimeZone("UTC"));
		defaultDateFormat.setCalendar(cal);
		suffixDateFormat.setCalendar(cal);
	}


  // Enum "SiteNameType"

	public static final String snt_USGS      = "USGS";
	public static final String snt_USGS_DRGS = "USGS-DRGS";
	public static final String snt_NWSHB5    = "NWSHB5";
	public static final String snt_local     = "Local";
	public static final String snt_CWMS      = "CWMS";
	public static final String snt_NOS       = "nos";


  // Enum "UnitConversionAlgorithm"

	public static final String eucvt_enumName = "UnitConversionAlgorithm";

	public static final String eucvt_none    = "none";   // out=in (no convert)
	public static final String eucvt_usgsstd = "USGS-Standard"; //Y=A*(B+x)^C+D
	public static final String eucvt_linear  = "linear"; // Y = Mx + B
	public static final String eucvt_poly5   = "Poly-5"; // 5th order polynomial


	// Standard USGS Platform Sensor Property Names:
	public static final String usgsprop_AlertNum = "AlertNum";
	public static final String usgsprop_AlertUser = "AlertUser";


  // Enum "EquationScope"

	public static final String eqSpecScopeDCP = "DCP";
	public static final String eqSpecScopeDCF = "DCF";
	public static final String eqSpecScopeNL  = "NL";
	public static final String eqSpecScopeSITE = "SITE";
	public static final String eqSpecScopeALL = "ALL";


  // Enum "DataTypeStandard"

	public static final String datatype_SHEF       = "SHEF-PE";
	public static final String datatype_EPA        = "EPA-Code";
	public static final String datatype_NOS        = "NOS";
	public static final String datatype_Hydstra    = "Hydstra-Code";
	public static final String datatype_HDB        = "HDB";
	public static final String datatype_USGS       = "USGS-Parm-Code";
	public static final String datatype_CWMS       = "CWMS";
	public static final String datatype_SHEFCODE   = "SHEFCODE";
	public static final String datatype_LABEL      = "LABEL";
	


  // Enum LookupAlgorithm"

	public static final String lookup_linear       = "linear";
	public static final String lookup_exponential  = "exponential";
	public static final String lookup_logarithmic  = "logarithmic";
	public static final String lookup_truncating   = "truncating";
	public static final String lookup_rounding     = "rounding";
	public static final String lookup_exact        = "exact-match";


  // Enum "EquipmentType"

	public static final String eqType_dcp          = "DCP";
	public static final String eqType_sensor       = "Sensor";
	public static final String eqType_transport    = "TransportMedium";

	// Enum Names
    public static final String enum_DataConsumer = "DataConsumer";
    public static final String enum_DataOrder = "DataOrder";
    public static final String enum_DataSourceType = "DataSourceType";
    public static final String enum_DataTypeStd = "DataTypeStandard";
    public static final String enum_EquationScope = "EquationScope";
	public static final String enum_Equipment =  "EquipmentType";
	public static final String enum_EUAlgorithm = "UnitConversionAlgorithm";
    public static final String enum_LookupAlgorithm = "LookupAlgorithm";
	public static final String enum_Measures = "Measures";
	public static final String enum_OutputFormat = "OutputFormat";
    public static final String enum_RecordingMode = "RecordingMode";
    public static final String enum_SiteName   = "SiteNameType";
    public static final String enum_TMType = "TransportMediumType";
    public static final String enum_UnitFamily = "UnitFamily";
    public static final String enum_DeviceType = "DeviceType";
    public static final String enum_ConnectType = "ConnectType";
    public static final String enum_AuthenticationType = "AuthenticationType";
    public static final String enum_SessionProtocol = "SessionProtocol";
    public static final String enum_contMedium = "ContactMedium";
    public static final String enum_OpType = "OperationType";
    public static final String enum_Season = "Season";
	public static final String enum_LoggerType = "LoggerType";
	public static final String enum_ApplicationType = "ApplicationType";



	/// Code for short preamble in a transport medium.
	public static final char preambleShort = 'S';

	/// Code for long preamble in a transport medium.
	public static final char preambleLong = 'L';

	/// Code for undefined preamble
	public static final char preambleUndefined = 'U';

	public static final String siteCountyNameProp = "CountyName";
	public static final String usgsSiteDbNumProp = "UsgsDbNum";
	public static final String usgsSiteAgencyProp = "UsgsAgency";
	public static final String siteRegionProp = "SiteRegion";
	public static final String siteCountryProp = "SiteCountry";
	public static final String siteCityProp = "SiteNearestCity";

}
