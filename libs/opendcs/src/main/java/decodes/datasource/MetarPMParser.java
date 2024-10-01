/*
*  $Id$
*
*  $Log$
*  Revision 1.1  2011/11/29 16:05:14  mmaloney
*  Added MetarPMParser
*
*/
package decodes.datasource;

import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Calendar;
import java.util.TimeZone;
import java.text.SimpleDateFormat;

import ilex.util.Logger;
import ilex.var.Variable;

import decodes.db.Constants;

/**
  Concrete subclass of PMParser for parsing performance measurements
  out of GOES DCP messages.
*/
public class MetarPMParser extends PMParser
{
	private static SimpleDateFormat sdf = null;

	/** default constructor */
	public MetarPMParser()
	{
		if (sdf == null)
		{
			sdf = new SimpleDateFormat("yyyyMMdd__HHmm");
			java.util.TimeZone jtz=java.util.TimeZone.getTimeZone("UTC");
			sdf.setCalendar(Calendar.getInstance(jtz));
		}
	}

	/**
	  Parses performance measurements from a METAR message.
	  Look for the filename performance measurement which the data source
	  will place in the message. Try to parse in the following format:
	  	STATION_NAME . DATE_TIME
	  where STATION_NAME is the medium ID.
	  DATE_TIME is the time-stamp in the format shown above for sdf.
	  A Metar message is in the format:
	  MSG :=  \r\n SOH \r\n HEADER_LINE \r\n DATA_LINE \r\n ETX
	  HEADER_LINE := AIRPORT_CODE  STATION_NAME  DDHHMM
	  Set the header length to the start of DATA_LINE
	  @param msg the message to parse.
	*/
	public void parsePerformanceMeasurements(RawMessage msg)
		throws HeaderParseException
	{
		byte data[] = msg.getData();
		
Logger.instance().debug3("MetarPMParser");
		// If filename is present, process it.
		Variable fn = msg.getPM(GoesPMParser.FILE_NAME);
		if (fn != null)
		{
			String filename = fn.getStringValue();
			Logger.instance().debug3("MetarPMParser Processing filename '" + filename + "'");
			int idx = filename.indexOf('.');
			if (idx <= 4)
			{
				Logger.instance().debug1("MetarPMParser - no station-name found before '.'");
				fn = null;
			}
			else
			{
				String mediumId = filename.substring(idx-4,idx);
				msg.setPM(GoesPMParser.DCP_ADDRESS, 
					new Variable(mediumId));
				msg.setMediumId(mediumId);
				try
				{
					Date d = sdf.parse(filename.substring(idx+1));
					msg.setPM(GoesPMParser.MESSAGE_TIME,
						new Variable(d));
				}
				catch(Exception ex)
				{
					Logger.instance().warning(
						"METAR Header Parser -- bad date in filename "
						+ "(requires STATION.YYYYMMDD__HHMM): "
						+ ex);
					fn = null;
				}
			}
		}
		else
			Logger.instance().debug3("MetarPMParser - No FILENAME passed.");
		
		// Find the start of HEADER_LINE
		int nnl = 0;
		int idx = 0;
		for(; idx < data.length && nnl < 2; idx++)
			if (data[idx] == (byte)'\n')
				nnl++;
		if (idx >=  data.length)
			throw new HeaderParseException("Error looking for start of header");
		idx++;
		
		// If no filename provided process HEADER_LINE
		if (fn == null)
		{
			for(; idx < data.length && data[idx] != (byte)' '; idx++);
			if (idx >= data.length)
				throw new HeaderParseException("Error looking for station name in header");
			int stationStart = ++idx;
			for(; idx < data.length && data[idx] != (byte)' '; idx++);
			String mediumId = new String(data,stationStart,idx-stationStart);
			msg.setPM(GoesPMParser.DCP_ADDRESS, new Variable(mediumId));
			msg.setMediumId(mediumId);
			
			try
			{
				int dayStart = ++idx;
Logger.instance().info("MetarPMParser Day starts at char " + dayStart);
				int dayNum = ((int)data[idx++] - 48) * 10 + ((int)data[idx++] - 48);
Logger.instance().info("MetarPMParser dayNum=" + dayNum);
				GregorianCalendar cal = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
				cal.setTime(new Date());
				cal.set(Calendar.DAY_OF_MONTH, dayNum);
				if (dayNum > cal.get(Calendar.DAY_OF_MONTH))
					cal.add(Calendar.MONTH, -1);
				int hour = ((int)data[idx++] - 48) * 10 + ((int)data[idx++] - 48);
Logger.instance().info("MetarPMParser hour=" + hour);
				cal.set(Calendar.HOUR_OF_DAY, hour);
				int min = ((int)data[idx++] - 48) * 10 + ((int)data[idx++] - 48);
Logger.instance().info("MetarPMParser min=" + min);
				cal.set(Calendar.MINUTE, min);
				cal.set(Calendar.SECOND, 0);
				Date d = cal.getTime();
Logger.instance().info("MetarPMParser msgTime=" + d);
				msg.setPM(GoesPMParser.MESSAGE_TIME,
					new Variable(d));
			}
			catch(Exception ex)
			{
				throw new HeaderParseException("Error parsing date in header");
			}
		}
		
		// Find the start of DATA_LINE and set header length.
		for(; idx < data.length && data[idx] != (byte)'\n'; idx++);
		if (++idx >= data.length)
			throw new HeaderParseException("Error looking for data after header");
		
		msg.setHeaderLength(idx);

		// Length should always be total length - header length
		msg.setPM(GoesPMParser.MESSAGE_LENGTH, new Variable(data.length - idx));
	}

	/**
	  @return -1, meaning variable length header
	*/
	public int getHeaderLength()
	{
		return -1;
	}

	/** @return "metar" */
	public String getHeaderType()
	{
		return "metar";
	}

	/** @return expected medium type for transport medium record */
	public String getMediumType()
	{
		return Constants.medium_METAR;
	}

	/** 
	  NOAAPORT msg length derived from the delimited variable length msg.
	  @return false
	*/
	public boolean containsExplicitLength()
	{
		return false;
	}
}