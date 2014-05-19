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

import oracle.jdbc.OracleResultSet;
import oracle.sql.DATE;

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
	private static final String module = "OracleDateParser";
//	private TimeZone tz;
	private Calendar cal;
	private SimpleDateFormat oracleTimestampZFmt = 
		new SimpleDateFormat("yyyy-MM-dd HH.mm.ss.0 z");
	private SimpleDateFormat oracleTimestampNoZFmt =
		new SimpleDateFormat("yyyy-MM-dd HH.mm.ss.0");

	
	public OracleDateParser(TimeZone tz)
	{
//		Logger.instance().debug1(module + " instantiating with timezone " + tz.getID());
		cal = Calendar.getInstance(tz);
		oracleTimestampNoZFmt.setTimeZone(TimeZone.getTimeZone("UTC"));
	}
	
	private Date parseNoZ(String s)
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
	
	private Date convertDATE(DATE oracleDate)
	{
		byte []db = oracleDate.getBytes();
		if (db.length < 7)
			return null;
		int yearByte = (int)db[1] & 0xff; // avoid sign extension!
		
		cal.clear();
		cal.set(Calendar.YEAR, ((int)db[0] - 100)*100 + (yearByte - 100));
		cal.set(Calendar.MONTH, (int)db[2] - 1);
		cal.set(Calendar.DAY_OF_MONTH, (int)db[3]);
		cal.set(Calendar.HOUR_OF_DAY, (int)db[4] - 1);
		cal.set(Calendar.MINUTE, (int)db[5] - 1);
		cal.set(Calendar.SECOND, (int)db[6] - 1);
		Date ret = cal.getTime();
//		Logger.instance().debug3(module + " Oracle DATE Bytes for "
//			+ cal.getTimeZone().getID() + ": "
//			+ (int)db[0] + " "
//			+ yearByte + " "
//			+ (int)db[2] + " "
//			+ (int)db[3] + " "
//			+ (int)db[4] + " "
//			+ (int)db[5] + " "
//			+ (int)db[6] + " date=" + ret);
		return ret;
	}
	
	public Date getTimeStamp(ResultSet rs, int column)
	{
		String s;
		ResultSetMetaData metaData = null;
		String sqlDataType = null;
		String columnName = null;
		try
		{
			metaData = rs.getMetaData();
			sqlDataType = metaData.getColumnTypeName(column);
			s = rs.getString(column);
			columnName = metaData.getColumnName(column);
//Logger.instance().debug3(module + " column " + columnName + " sqlType='" + sqlDataType
//+ "' string value='" + s + "'");
			if (s == null || rs.wasNull())
				return null;
			
			if (sqlDataType.equals("DATE"))
				return convertDATE(((OracleResultSet)rs).getDATE(column));
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
	
	/**
	 * Convert a Java Date object into an Oracle DATE in the database time zone.
	 * @param d the Java date object
	 * @return the Oracle DATE in the database time zone.
	 */
	public DATE toDATE(Date d)
	{
		byte b[] = new byte[7];
		cal.setTime(d);
		b[0] = (byte)((cal.get(Calendar.YEAR) / 100) + 100);
		b[1] = (byte)((cal.get(Calendar.YEAR) % 100) + 100);
		b[2] = (byte)(cal.get(Calendar.MONTH) + 1);
		b[3] = (byte)cal.get(Calendar.DAY_OF_MONTH);
		b[4] = (byte)(cal.get(Calendar.HOUR_OF_DAY) + 1);
		b[5] = (byte)(cal.get(Calendar.MINUTE) + 1);
		b[6] = (byte)(cal.get(Calendar.SECOND) + 1);
		return new DATE(b);
	}
}
