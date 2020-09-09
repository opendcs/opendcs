/*
*  $Id$
*/
package decodes.datasource;

import java.util.HashMap;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Calendar;
import java.util.Properties;
import java.util.TimeZone;
import java.text.ParsePosition;
import java.text.ParseException;
import java.text.SimpleDateFormat;

import ilex.util.ArrayUtil;
import ilex.util.ByteUtil;
import ilex.util.Logger;
import ilex.var.Variable;
import decodes.db.Constants;

/**
  Concrete subclass of PMParser for parsing performance measurements
  out of GOES DCP messages.
*/
public class MBFirePMParser extends PMParser
{
	// Do not define these - re-use the definitions in GoesPMParser:
	//public static final String DCP_ADDRESS = "DcpAddress";
	//public static final String MESSAGE_TIME = "Time";
	//public static final String MESSAGE_LENGTH = "Length";
	//public static final String FAILURE_CODE = "FailureCode";
	//public static final String SIGNAL_STRENGTH = "SignalStrength";
	//public static final String FREQ_OFFSET = "FrequencyOffset";
	//public static final String MOD_INDEX = "ModulationIndex";
	//public static final String QUALITY = "Quality";
	//public static final String CHANNEL = "Channel";
	//public static final String SPACECRAFT = "Spacecraft";
	//public static final String UPLINK_CARRIER = "UplinkCarrier";

	private SimpleDateFormat dateFormat = null;
	private static final String dateFmtStr = "MMM dd, yyyy      HH";

	/** default constructor */
	public MBFirePMParser()
	{
//		Logger.instance().debug3("MBFirePMParser ctor");
		dateFormat = new SimpleDateFormat(dateFmtStr);
		java.util.TimeZone jtz=java.util.TimeZone.getTimeZone("GMT-06:00");
		dateFormat.setCalendar(Calendar.getInstance(jtz));
	}

	/**
	*/
	public void parsePerformanceMeasurements(RawMessage msg)
		throws HeaderParseException
	{
//Logger.instance().info("MBFirePMP: parsePMs");;
		byte data[] = msg.getData();
		StringBuilder idbuf = new StringBuilder();
		int idx = 0;
		for(; idx < data.length && (char)data[idx] == ' '; idx++);
		if (idx >= data.length)
			throw new HeaderParseException("Header completely blank");
		for(; idx<data.length && (char)data[idx] != ' '; idx++)
			idbuf.append((char)data[idx]);
		String ids = idbuf.toString();
		if (idx >= data.length || ids.length() == 0)
				throw new HeaderParseException("No Medium ID");

		if (ids.startsWith("-") || ids.equals("Station") || ids.startsWith("("))
			throw new HeaderParseException("Blank or comment line");

		msg.setMediumId(ids);
Logger.instance().debug3("MBFirePMP: mediumId = '" + ids + "'");
		while(idx<data.length && (char)data[idx] == ' ')
			idx++;
		if (data.length < idx + dateFmtStr.length())
			throw new HeaderParseException("No date in line");
		String datestr = new String(data, idx, dateFmtStr.length());

		try
		{
Logger.instance().debug3("MBFirePMP: parsing date string = '" + datestr+ "'");
			Variable tv = new Variable(dateFormat.parse(datestr));
			msg.setPM(GoesPMParser.MESSAGE_TIME, tv);
		}
		catch(ParseException ex)
		{
			Logger.instance().warning("Bad date format '" + datestr + "'");
		}

		int headerlen = idx + dateFmtStr.length();
		msg.setHeaderLength(headerlen);
Logger.instance().debug3("MBFirePMP: header len = " + headerlen);

		// Length should always be total length - 29
		int datalen = data.length - headerlen;
		msg.setPM(GoesPMParser.MESSAGE_LENGTH, new Variable(datalen));
Logger.instance().debug3("MBFirePMP: msg len = " + datalen);
	}

	/**
	  @return 18, the length of a NOAAPORT message header
	*/
	public int getHeaderLength()
	{
		return 28;
	}

	/** @return "mbfire" */
	public String getHeaderType()
	{
		return "mbfire";
	}

	/** @return "mbfire" */
	public String getMediumType()
	{
		return Constants.medium_mbfire;
	}

	/** 
	  We want to process a whole line ending with \r, so return false.
	  @return false
	*/
	public boolean containsExplicitLength()
	{
		return false;
	}
	
	@Override
	public void setProperties(Properties rsProps)
	{
		String s = rsProps.getProperty("webtz");
		if (s != null)
		{
			TimeZone tz = TimeZone.getTimeZone(s);
			if (tz == null)
				Logger.instance().warning("MBFirePMParser invalid webtz property '" + s + "' -- using GMT-06:00.");
			else
				dateFormat.setTimeZone(tz);
		}
	}
}

