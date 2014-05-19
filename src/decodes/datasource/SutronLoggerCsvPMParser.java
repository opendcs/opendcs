/*
*  $Id$
*
*  $Log$
*  Revision 1.1  2012/02/16 20:41:47  mmaloney
*  Created to handle logger CSV files from sutron loggers.
*
*/
package decodes.datasource;

import java.util.Date;
import java.text.SimpleDateFormat;

import ilex.util.Logger;
import ilex.var.Variable;

import decodes.datasource.RawMessage;
import decodes.db.Constants;

/**
Performance Measurement Parser for USGS EDL Header Lines.
*/
public class SutronLoggerCsvPMParser extends PMParser
{
	// Private time/date stamp parsers:
	private static SimpleDateFormat sdf = 
		new SimpleDateFormat("MM/dd/yyyy,HH:mm:ss");

	/**  The STATION ID parsed out of the header. */
	public static final String STATION = EdlPMParser.STATION;

	/** The length of the message that follows the header. */
	public static final String MESSAGE_LENGTH = GoesPMParser.MESSAGE_LENGTH;

	/**
	  The end time of the message. Set from DEVICE END TIME. Possibly 
	  over-ridden by ACTUAL END TIME. Stored as a Date Variable.
	*/
	public static final String MESSAGE_TIME = GoesPMParser.MESSAGE_TIME;

	/**
	  Optional start time, complete time stamp stored as a date.
	  This is set from the BEGIN TIME and BEGIN DATE header fields.
	*/
	public static final String BEGIN_TIME_STAMP = "BeginTimeStamp";

	public static final String END_TIME_STAMP = "EndTimeStamp";

	/** Use factory method in PMParser, do not construct directly. */
	public SutronLoggerCsvPMParser()
	{
		super();
	}

	/** Returns the constant string "data-logger". */
	public String getHeaderType() { return "sutron_logger_csv"; }

	/** Returns constant medium type for platform association. */
	public String getMediumType() { return Constants.medium_SutronCSV; }

	/**
	  Parses performance measurements from raw message and populates
	  a hashmap (string - Variable) table of results.
	  Also sets the boolean 'beginTimeHasTz' indicating whether or not the
	  BEGIN TIME stamp (if one was present) had a time zone indicator. If
	  it does NOT, then you should adjust the begin time by the site's TZ.
	  @param msg the raw message to parse
	*/
	public void parsePerformanceMeasurements(RawMessage msg)
		throws HeaderParseException
	{
		byte data[] = msg.getData();
		int len = data.length;

		int p=0;
		
		Logger.instance().debug1("Parsing " + getMediumType() + ", len=" + len);
		
		// Look for the string "Station Name" on the first line
		StringBuilder sb = new StringBuilder();
		for(; p<len && data[p] != '\n' && data[p] != '\r'; p++)
			sb.append((char)data[p]);
		if (!sb.toString().equalsIgnoreCase("Station Name"))
			throw new HeaderParseException("First line must be 'Station Name'");
		while(p < len && Character.isWhitespace(data[p]))
			p++;
		
		// The second line contains the actual station name
		sb.setLength(0);
		for(; p<len && data[p] != '\n' && data[p] != '\r'; p++)
			sb.append((char)data[p]);
		String station = sb.toString().trim();
		if (station.length() == 0)
			throw new HeaderParseException(
				"2nd line is blank. It must contain station name.");
		msg.setPM(STATION, new Variable(station));
		msg.setMediumId(station);
		msg.setPM(GoesPMParser.DCP_ADDRESS, new Variable(station));

		// Set the header length to the beginning of the 3rd line
		while(p < len && Character.isWhitespace(data[p]))
			p++;
		msg.setHeaderLength(p);
		msg.setPM(MESSAGE_LENGTH, new Variable((long)(len - p)));

		// File is in ascending time order. Parse first date/time. That will
		// be the message start-time.
		if (len < p + 19)
			throw new HeaderParseException("No last data line");
		String s = new String(data, p, 19);
		try 
		{
			Date d = sdf.parse(s);
			msg.setPM(BEGIN_TIME_STAMP, new Variable(d));
		}
		catch(Exception ex)
		{
			throw new HeaderParseException("Invalid date/time '"
				+ s + "' on 1st data line.");
		}

		// Get the end time from the last value in the message
		for(p = len - 3; p>19 && data[p] != (byte)'\n'; p--);
		if (data[p] != (byte)'\n')
			throw new HeaderParseException(
				"Cannot find end time, p=" + p + ".");
		p++;
		if (len-p < 19)
			throw new HeaderParseException("No last data line.");
		s = new String(data, p, 19);
		try 
		{
			Date d = sdf.parse(s);
			msg.setPM(END_TIME_STAMP, new Variable(d));
			msg.setPM(MESSAGE_TIME, new Variable(d));
		}
		catch(Exception ex)
		{
			throw new HeaderParseException("Invalid date/time '"
				+ s + "' on last data line.");
		}
	}

	/**
	  @return -1 because Header length is variable, not fixed.
	*/
	public int getHeaderLength()
	{
		return -1;
	}


	/**
	  @return false, the header contains no indication of the message length.
	*/
	public boolean containsExplicitLength()
	{
		return false;
	}
}

