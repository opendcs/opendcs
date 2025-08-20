package decodes.datasource;

/**
 * Use this parser for CSV files where the platform name is contained in one of the 
 * fields in the first few header lines in the file. For example:
 * <pre>
"TOA5","Jackson_Gulch","CR800","25718","CR800.Std.25","CPU:JacksonGulchCR800.CR8","19588","Table60"
"TIMESTAMP","RECORD","BATT_VOLT_Avg","PTemp_Avg","WATER_LVL_Avg","WATER_TEMP_F_Avg","ELEVATION_Avg"
"TS","RN","","","","",""
"","","Avg","Avg","Avg","Avg","Avg"
"2013-05-07 13:01:00",0,13.52,57.45,71.27,44.78,7780.274
"2013-05-07 14:01:00",1,13.57,60.37,71.28,44.66,7780.277
 * </pre>
 * Here, the DCP name, Jackson_Gulch, is found in the second field of the first line.
 * Properties allowed:
 * <ul>
 *   <li>idline - line where the medium ID is found (default=1=first line of message)</li>
 *   <li>idcol - columns within idline where the ID is found (default=1=first line of message)</li>
 *   <li>datetimeline - if a message date/time is present, this is the line number</li>
 *   <li>datetimecol - columns within datetimeline where date/time starts (may span multiple columns</li>
 *   <li>datetimefmt=MMM,dd,yyyy,HH:mm</li>
 *   <li>timezone=UTC</li>
 *   <li>delim - the delimiter in the header lines. Default = comma</li>
 * </ul>
 */
public class CsvHeaderPMParser extends PMParser
{

	@Override
	public String getHeaderType()
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getMediumType()
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void parsePerformanceMeasurements(RawMessage msg) throws HeaderParseException
	{
		// TODO Auto-generated method stub

	}

	@Override
	public int getHeaderLength()
	{
		// TODO Auto-generated method stub
		return 0;
	}

}
