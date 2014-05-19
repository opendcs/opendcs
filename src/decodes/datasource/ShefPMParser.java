/*
*  $Id$
*
*  $Log$
*  Revision 1.1  2011/09/27 01:23:08  mmaloney
*  Enhancements to StreamDataSource for SHEF and NOS Decoding.
*
*  Revision 1.1  2008/04/04 18:21:00  cvs
*  Added legacy code to repository
*
*  Revision 1.8  2004/08/24 23:52:45  mjmaloney
*  Added javadocs.
*
*  Revision 1.7  2003/12/07 20:36:48  mjmaloney
*  First working implementation of EDL time stamping.
*
*  Revision 1.6  2003/06/17 00:34:00  mjmaloney
*  StreamDataSource implemented.
*  FileDataSource re-implemented as a subclass of StreamDataSource.
*
*  Revision 1.5  2002/10/25 19:49:04  mjmaloney
*  Fixed problems in NOAAPORT PM Parser & Socket Stream
*
*  Revision 1.4  2002/10/11 18:12:27  mjmaloney
*  Fixed NoaaportPMParser
*
*  Revision 1.3  2002/10/11 01:27:01  mjmaloney
*  Added SocketStreamDataSource and NoaaportPMParser stuff.
*
*  Revision 1.2  2002/09/30 21:25:35  mjmaloney
*  dev.
*
*  Revision 1.1  2002/09/30 19:08:55  mjmaloney
*  Created.
*
*/
package decodes.datasource;

import java.util.Date;
import java.util.TimeZone;
import java.text.SimpleDateFormat;

import ilex.util.Logger;
import ilex.var.Variable;

import decodes.db.Constants;

/**
  Concrete subclass of PMParser for parsing performance measurements
  out of GOES DCP messages.
*/
public class ShefPMParser extends PMParser
{
	private static SimpleDateFormat mmdd = new SimpleDateFormat("MMdd");
	private static SimpleDateFormat yymmdd = new SimpleDateFormat("yyMMdd");
	private static SimpleDateFormat ccyymmdd = new SimpleDateFormat("yyyyMMdd");
	
	public static final String PM_TIMEZONE = "TIME_ZONE";
	public static final String PM_MESSAGE_TYPE = "SHEF_MESSAGE_TYPE";
	
	/** default constructor */
	public ShefPMParser()
	{
	}

	/**
	  Parses performance measurements from a SHEF message:
	  locid  date  [tz]
	  @param msg the message to parse.
	*/
	public void parsePerformanceMeasurements(RawMessage msg)
		throws HeaderParseException
	{
		byte data[] = msg.getData();
		StringBuilder sb = new StringBuilder();
		int idx = 0;

		// In the editor, we may be passed a full message starting with .E or .A
		while(idx < data.length
		 && Character.isWhitespace((char)data[idx]))
			idx++;
		if (idx == data.length)
			throw new HeaderParseException("Invalid SHEF header, length=" + data.length
				+ ", '" + new String(data) + "'");
		if ((char)data[idx] == '.')
		{
			idx++;
			msg.setPM(PM_MESSAGE_TYPE, new Variable((char)data[idx]));
			Logger.instance().debug3("Message Type set to '" + (char)data[idx] + "'");
			while(idx < data.length && !Character.isWhitespace((char)data[idx]))
				idx++;
		}

		// Skip white space then get next word - this is station name
		while(idx < data.length
		 && Character.isWhitespace((char)data[idx]))
			idx++;
		if (idx == data.length)
			throw new HeaderParseException("Invalid SHEF header, length=" + data.length
				+ ", '" + new String(data) + "'");
		while(idx < data.length
		 && !Character.isWhitespace((char)data[idx]))
			sb.append((char)data[idx++]);
		if (idx == data.length)
			throw new HeaderParseException("Invalid SHEF header, length=" + data.length
				+ ", '" + new String(data) + "'");
		String stationName = sb.toString();
		Logger.instance().debug3("SHEF Station name='" + stationName + "'");
		
		sb.setLength(0);
		
		// Skip white space then get next word - this is date/time field
		while(idx < data.length
		 && Character.isWhitespace((char)data[idx]))
			idx++;
		if (idx == data.length)
			throw new HeaderParseException("Invalid SHEF header, length=" + data.length
				+ ", '" + new String(data) + "'");
		while(idx < data.length
		 && !Character.isWhitespace((char)data[idx]))
			sb.append((char)data[idx++]);
		if (idx == data.length)
			throw new HeaderParseException("Invalid SHEF header, length=" + data.length
				+ ", '" + new String(data) + "'");
		String dateField = sb.toString();
		Logger.instance().debug3("SHEF Station Time='" + dateField + "'");
		sb.setLength(0);

		// Optional third field for timezone.
		while(idx < data.length
		 && Character.isWhitespace((char)data[idx]))
			idx++;
		if (idx == data.length)
			throw new HeaderParseException("Invalid SHEF header, length=" + data.length
				+ ", '" + new String(data) + "'");
		int headerEnd = idx;
		while(idx < data.length
		 && !Character.isWhitespace((char)data[idx]) && (char)data[idx] != '/')
			sb.append((char)data[idx++]);
		if (idx == data.length)
			throw new HeaderParseException("Invalid SHEF header, length=" + data.length
				+ ", '" + new String(data) + "'");
		String tzField = null;
		TimeZone tz = TimeZone.getTimeZone("UTC");
		if (sb.charAt(0) != 'D' && sb.charAt(0) != 'd')
		{
			tzField = sb.toString();
			headerEnd = idx;
			tz = shefTZ2javaTZ(tzField);
			if (tz == null)
			{
				Logger.instance().warning("Invalid time-zone '" + tzField
					+ "' in SHEF message ignored, using UTC.");
				tz = TimeZone.getTimeZone("UTC");
			}
		}
		Logger.instance().debug3("SHEF Time Zone = " + tz.getID());
		
		msg.setMediumId(stationName);
		msg.setPM(GoesPMParser.DCP_ADDRESS, new Variable(stationName));
		msg.setHeaderLength(headerEnd);
		Logger.instance().debug3("SHEF Header Length set to " + headerEnd);
		
		Date msgTime = null;
		try
		{
			if (dateField.length() == 4)
			{
				mmdd.setTimeZone(tz);
				msgTime = mmdd.parse(dateField);
			}
			else if (dateField.length() == 6)
			{
				yymmdd.setTimeZone(tz);
				msgTime = yymmdd.parse(dateField);
			}
			else if (dateField.length() == 8)
			{
				ccyymmdd.setTimeZone(tz);
				msgTime = ccyymmdd.parse(dateField);
			}
			else
				throw new Exception();
		}
		catch(Exception ex)
		{
			throw new HeaderParseException("Invalid date field in SHEF message '"
				+ dateField + "'");
		}
		msg.setTimeStamp(msgTime);
		msg.setPM(GoesPMParser.MESSAGE_TIME, new Variable(msgTime));
		Logger.instance().debug3("SHEF Message Date=" + ccyymmdd.format(msgTime));
		msg.setPM(PM_TIMEZONE, new Variable(tz.getID()));
		msg.setPM(GoesPMParser.MESSAGE_LENGTH, new Variable(data.length - headerEnd));
		msg.setPM(GoesPMParser.FAILURE_CODE, new Variable('G'));
		msg.setPM(GoesPMParser.SITE_NAME, new Variable(stationName));
	}

	/**
	 * Converts a SHEF timezone name to a valid Java timezone.
	 * @param shefTZ the shef time zone
	 * @return the java TimeZone object, or null if not found.
	 */
	public static TimeZone shefTZ2javaTZ(String shefTZ)
	{
		shefTZ = shefTZ.toUpperCase();
		if (shefTZ.equals("Z"))
			return TimeZone.getTimeZone("UTC");

		if (shefTZ.equals("N"))
			return TimeZone.getTimeZone("Canada/Newfoundland");
		if (shefTZ.equals("NS"))
			return TimeZone.getTimeZone("GMT-0230");

		if (shefTZ.equals("A"))
			return TimeZone.getTimeZone("Atlantic/Bermuda");
		if (shefTZ.equals("AD"))
			return TimeZone.getTimeZone("GMT-0300");
		if (shefTZ.equals("AS"))
			return TimeZone.getTimeZone("GMT-0400");
		
		if (shefTZ.equals("E"))
			return TimeZone.getTimeZone("America/New_York");
		if (shefTZ.equals("ED"))
			return TimeZone.getTimeZone("GMT-0400");
		if (shefTZ.equals("ES"))
			return TimeZone.getTimeZone("GMT-0500");

		if (shefTZ.equals("C"))
			return TimeZone.getTimeZone("America/Chicago");
		if (shefTZ.equals("CD"))
			return TimeZone.getTimeZone("GMT-0500");
		if (shefTZ.equals("CS"))
			return TimeZone.getTimeZone("GMT-0600");
		
		if (shefTZ.equals("J"))
			return TimeZone.getTimeZone("GMT+0800");
		
		if (shefTZ.equals("M"))
			return TimeZone.getTimeZone("America/Denver");
		if (shefTZ.equals("MD"))
			return TimeZone.getTimeZone("GMT-0600");
		if (shefTZ.equals("MS"))
			return TimeZone.getTimeZone("GMT-0700");

		if (shefTZ.equals("P"))
			return TimeZone.getTimeZone("America/Los_Angeles");
		if (shefTZ.equals("PD"))
			return TimeZone.getTimeZone("GMT-0700");
		if (shefTZ.equals("PS"))
			return TimeZone.getTimeZone("GMT-0800");
		
		if (shefTZ.equals("Y"))
			return TimeZone.getTimeZone("Canada/Yukon");
		if (shefTZ.equals("YD"))
			return TimeZone.getTimeZone("GMT-0700");
		if (shefTZ.equals("YS"))
			return TimeZone.getTimeZone("GMT-0800");

		if (shefTZ.equals("H"))
			return TimeZone.getTimeZone("US/Hawaii");
		if (shefTZ.equals("HS"))
			return TimeZone.getTimeZone("GMT-1000");
		
		if (shefTZ.equals("L"))
			return TimeZone.getTimeZone("US/Alaska");
		if (shefTZ.equals("LD"))
			return TimeZone.getTimeZone("GMT-0800");
		if (shefTZ.equals("LS"))
			return TimeZone.getTimeZone("GMT-0900");

		if (shefTZ.equals("B"))
			return TimeZone.getTimeZone("Canada/Yukon");
		if (shefTZ.equals("BD"))
			return TimeZone.getTimeZone("GMT-0700");
		if (shefTZ.equals("BS"))
			return TimeZone.getTimeZone("GMT-0800");
		
		return null;
	}
	/**
	  @return -1 because the header is variable length.
	*/
	public int getHeaderLength()
	{
		return -1;
	}

	/** @return "shef" */
	public String getHeaderType()
	{
		return "shef";
	}

	/** @return data type constant for SHEF message */
	public String getMediumType()
	{
		return Constants.medium_SHEF;
	}

	/** 
	  SHEF msg length derived from the delimited variable length msg.
	  @return false
	*/
	public boolean containsExplicitLength()
	{
		return false;
	}
}

