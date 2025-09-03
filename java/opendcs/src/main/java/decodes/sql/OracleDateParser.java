/*
* Where Applicable, Copyright 2025 OpenDCS Consortium and/or its contributors
*
* Licensed under the Apache License, Version 2.0 (the "License"); you may not
* use this file except in compliance with the License. You may obtain a copy
* of the License at
*
*   http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
* WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
* License for the specific language governing permissions and limitations
* under the License.
*/
package decodes.sql;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

import org.opendcs.utils.logging.OpenDcsLoggerFactory;
import org.slf4j.Logger;


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
	private static final Logger log = OpenDcsLoggerFactory.getLogger();
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
			if (s == null || rs.wasNull())
				return null;
		}
		catch (SQLException ex)
		{
			log.atWarn().setCause(ex).log("Error getting date as string.");
			return null;
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
				/* fall through to next attempt */
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
			log.atWarn()
			   .setCause(ex)
			   .log(" Cannot parse {} string '{}' and cannot get as Timestamp: ", columnName, s);
			return null;
		}
	}

}
