package decodes.hdb;

import ilex.util.Logger;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.text.ParseException;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

import decodes.sql.OracleDateParser;
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
public class HdbOracleDateParser
	extends OracleDateParser
{
	
	public HdbOracleDateParser(TimeZone tz)
	{
		super(tz);
		module = "HdbOracleDateParser";
Logger.instance().info("Constructing " + module);
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
			if (s == null || rs.wasNull())
				return null;
			
			if (sqlDataType.equals("DATE"))
				return convertDATE(((OracleResultSet)rs).getDATE(column));
		}
		catch (SQLException ex)
		{
			try
			{
				Timestamp ts = rs.getTimestamp(column);
				Date ret = new Date(ts.getTime());
				return ret;
			}
			catch (SQLException e2)
			{
				Logger.instance().warning(module + " Error Attempt to get date as String and Timestamp both failed: " + e2);
				return null;
			}
		}
		try 
		{
			Date d = oracleTimestampZFmt.parse(s);
			return d;
		}
		catch(Exception ex)
		{
			try
			{
				Date d = parseNoZ(s);
				return d;
			}
			catch(ParseException ex2)
			{
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
