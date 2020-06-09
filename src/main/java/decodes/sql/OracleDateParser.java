package decodes.sql;

import ilex.util.Logger;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;


/**
 * Oracle DATE objects are commonly used to store timestamp info.
 * Oracle DATE objects store time-stamps but with out any timezone info.
 * A DATE is stored as seven bytes with the following values:
 *   century+100
 *   year in century + 100
 *   month
 *   day
 *   hour + 1
 *   min + 1
 *   sec + 1
 * Example: July 26, 2012 9:53:17 is stored as:
 * 	120 112 7 12 10 54 18
 * Also, we have a mix of DATE and TIMESTAMP WITH TIMEZONE so we
 * sometimes don't know the underlying SQL data type.
 * 
 * The only reliable way to retrieve the date/time correctly is to
 * set session timezone on startup to whatever timezone the DATEs are stored in.
 * Then try to parse from a string in a variety of formats.
 * 
 * Experimentally, I have determined that ResultSet.getTimestamp does not
 * produce correct results in Oracle for either DATE or TIMESTAMP WITH TIME ZONE.
 * 
 * @author mmaloney Mike Maloney, Cove Software, LLC
 */
public class OracleDateParser
{
	protected static String module = "OracleDateParser";
	protected Calendar cal;
	protected SimpleDateFormat oracleTimestampZFmt = new SimpleDateFormat("yyyy-MM-dd HH.mm.ss.0 z");
	protected SimpleDateFormat oracleTimestampNoZFmt = new SimpleDateFormat("yyyy-MM-dd HH.mm.ss.0");

	public OracleDateParser(TimeZone tz)
	{
		cal = Calendar.getInstance(tz);
		oracleTimestampNoZFmt.setTimeZone(TimeZone.getTimeZone("UTC"));
	}
	
	protected Date parseNoZ(String s)
		throws ParseException
	{
		// Count number of spaces in str and remember position of last one.
		s = s.trim();
		int nSpaces = 0;
		int lastSpace = -1;
		for(int i=0; i<s.length(); i++)
			if (s.charAt(i) == ' ')
			{
				nSpaces++;
				lastSpace = i;
			}
		if (nSpaces != 2)
			throw new ParseException("Not in No-Z Format 'yyyy-MM-dd HH.mm.ss.0 -H:m", 0);
		Date d = oracleTimestampNoZFmt.parse(s.substring(0, lastSpace));
		String hs = s.substring(lastSpace+1);
		int colon = hs.indexOf(':');
		if (colon > 0)
		{
			try
			{
				int hr = Integer.parseInt(hs.substring(0, colon));
				return new Date(d.getTime() - (hr * 3600000L));
			}
			catch(Exception ex)
			{
				throw new ParseException(ex.getMessage(), lastSpace+1);
			}
		}
		else
			return d;
	}
	
	public Date getTimeStamp(ResultSet rs, int column)
	{
		String s;
		ResultSetMetaData metaData = null;
		String columnName = null;
		try
		{
			metaData = rs.getMetaData();
			s = rs.getString(column);
			columnName = metaData.getColumnName(column);
//Logger.instance().debug3(module + " column " + columnName + " sqlType='" + sqlDataType
//+ "' string value='" + s + "'");
			if (s == null || rs.wasNull())
				return null;
		}
		catch (SQLException ex)
		{
			Logger.instance().warning(module + " Error getting date as string: " + ex);
			return null;
		}
		try 
		{
			Date d = oracleTimestampZFmt.parse(s);
//			Logger.instance().debug3(module + " Parsed Oracle Timestamp. String='" + s + "', date=" + d);
			return d;
		}
		catch(Exception ex)
		{
//			Logger.instance().debug3(module + " Cannot parse oracle date/time with Z format.");
			try
			{
				Date d = parseNoZ(s);
//				Logger.instance().debug3(module + " Parsed Oracle Timestamp NoZ. String='" + s + "', date=" + d);
				return d;
			}
			catch(ParseException ex2)
			{
//				Logger.instance().debug3(module + " Still can't parse with NoZ format.");
			}
		}
		try
		{
			java.sql.Timestamp ts = rs.getTimestamp(column, cal);
			if (ts == null || rs.wasNull())
				return null;
			else
			{
				Date d = new Date(ts.getTime());
//Logger.instance().debug3(module + " read as sql.Timestamp: ts='" + ts + "', msec=" + 
//ts.getTime() + ", date=" + d);
				return d;
			}
		}
		catch(SQLException ex)
		{
			Logger.instance().warning(module + 
				" Cannot parse " + columnName 
				+ " string '" + s + "' and cannot get as Timestamp: " + ex);
			return null;
		}
	}
	
}
