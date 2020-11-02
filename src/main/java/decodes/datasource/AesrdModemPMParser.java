/*
 * $Id$
 * 
 * $Log$
 * Revision 1.6  2014/03/05 15:56:00  mmaloney
 * Added alternate date/time format for h555.
 *
 * Revision 1.5  2014/01/22 20:57:03  mmaloney
 * dev
 *
 * Revision 1.4  2014/01/22 20:50:17  mmaloney
 * dev
 *
 * Revision 1.3  2014/01/22 20:31:53  mmaloney
 * dev
 *
 * Revision 1.2  2014/01/22 20:18:58  mmaloney
 * dev
 *
 * Revision 1.1  2014/01/22 18:04:02  mmaloney
 * created.
 *
 */
package decodes.datasource;

import ilex.util.Logger;
import ilex.var.Variable;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

import decodes.db.Constants;

/**
 * Handles modem files created by the COLLECT TERM Scripts operated
 * by Alberta ESRD
 * @author mmaloney Mike Maloney, Cove Software, LLC
 */
public class AesrdModemPMParser 
	extends PMParser
{
	public static final String module = "AesrdModemPMParser";
	private int headerLen = 0;
	
	private SimpleDateFormat sdfs[] = 
	{
		new SimpleDateFormat("yy/MM/dd HH:mm:ss"), // amasser, campbell, fts, sutron, vedas
		new SimpleDateFormat("yyyy DDD HH:mm:ss")  // h555
	};
	
	public AesrdModemPMParser()
	{
		// header time stamps are always in MST.
		for(SimpleDateFormat sdf : sdfs)
			sdf.setTimeZone(TimeZone.getTimeZone("GMT-07:00"));
	}
	
	/** Returns the constant string "data-logger". */
	@Override
	public String getHeaderType() { return "AesrdModem"; }

	/** Returns constant medium type for platform association. */
	@Override
	public String getMediumType() { return Constants.medium_EDL; }

	@Override
	public void parsePerformanceMeasurements(RawMessage msg) throws HeaderParseException
	{
		headerLen = 0;

		// The message delimiter may have gobbled the "DACQ TIME STAMP" on the
		// first line leaving us with just the date/time stamp at the end.
		// This may be the best one so save it.
		Date msgTime = parseDateTime(getNextLine(msg));
		
		String line = null;
		while((line = getNextLine(msg)) != null)
		{
			int idx;
			String ucLine = line.toUpperCase(); // for efficient comparisons
			
			if ((idx = ucLine.indexOf("START:")) >= 0)
			{
				String mediumId = line.substring(idx + 6).trim();
				msg.setPM(EdlPMParser.STATION, new Variable(mediumId));
				msg.setPM(GoesPMParser.DCP_ADDRESS, new Variable(mediumId));
				msg.setPM(GoesPMParser.FAILURE_CODE, new Variable('G'));
				msg.setMediumId(mediumId);
				msg.setHeaderLength(headerLen);
				msg.setPM(GoesPMParser.MESSAGE_LENGTH, 
					new Variable((long)(msg.data.length - headerLen)));
				if (msgTime != null)
				{
					msg.setTimeStamp(msgTime);
					msg.setPM(GoesPMParser.MESSAGE_TIME, new Variable(msgTime));
				}
				Logger.instance().debug2(module + " after parse, headerLen="
					+ headerLen + ", msgLen=" + msg.getPM(GoesPMParser.MESSAGE_LENGTH)
					+ " msgTime=" + msgTime);
				return;
			}
			else if ((idx = ucLine.indexOf("DACQ TIME STAMP:")) >= 0)
			{
				Date dt = parseDateTime(line);
				if (dt != null)
					msgTime = dt;
			}
			else if (ucLine.startsWith("CAPTUREFILE")
			  && (idx = line.lastIndexOf("tty")) != -1)
			{
				String device = line.substring(idx);
				msg.setPM(ModemPMParser.DEVICE, new Variable(device));
			}
		}
		throw new HeaderParseException("No 'START:' line found.");
	}
	
	/**
	 * Start at headerLen and get the next line of data. Return as a string.
	 * Leave headerLen at the start of the next line.
	 * @return String containing next line of data, or null if this is end of msg.
	 */
	private String getNextLine(RawMessage msg)
	{
Logger.instance().debug3(module + " getNextLine() headerLen=" + headerLen 
+ ", msgLen=" + msg.data.length);
		if (headerLen >= msg.data.length)
			return null;
		StringBuilder sb = new StringBuilder();
		while(headerLen < msg.data.length)
		{
			char c = (char)msg.data[headerLen++];
			if (c == '\n')
				return sb.toString();
			if (c != '\r')
				sb.append(c);
		}
		if (sb.length() > 0) // unfinished line at end with no linefeed.
			return sb.toString();
		else
			return null;
	}

	private Date parseDateTime(String line)
	{
		// Find the first digit at the start of a word.
		boolean prevWS = true;
		int dateStart = 0;
		for(; dateStart < line.length(); dateStart++)
		{
			char c = line.charAt(dateStart);
			if (Character.isDigit(c) && prevWS)
				break;
			prevWS = Character.isWhitespace(c);
		}
		if (dateStart >= line.length())
		{
			Logger.instance().warning(module + " No date/time stamp on line '"
				+ line + "'");
			return null;
		}
		
		String dateStr = line.substring(dateStart);
		Logger.instance().debug3(module + " parsing date/time from '" + dateStr
				+ "' with '" + sdfs[0].toPattern() + "'");
		
		try { return sdfs[0].parse(dateStr); }
		catch(Exception ex)
		{
			Logger.instance().debug3(module + " parsing date/time from '" + dateStr
				+ "' with '" + sdfs[1].toPattern() + "'");
			try { return sdfs[1].parse(dateStr); }
			catch(ParseException ex2)
			{
				Logger.instance().warning(module + " Bad date/time stamp on line '"
					+ line + "'");
			}
		}
		return null;
	}

	@Override
	public int getHeaderLength()
	{
		return headerLen;
	}

	@Override
	public boolean containsExplicitLength() { return false; }


}
