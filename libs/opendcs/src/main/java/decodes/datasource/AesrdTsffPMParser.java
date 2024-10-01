/*
 * $Id$
 * 
 * $Log$
 * Revision 1.5  2014/01/23 16:16:35  mmaloney
 * dev
 *
 * Revision 1.4  2014/01/22 20:57:03  mmaloney
 * dev
 *
 * Revision 1.2  2014/01/22 20:31:53  mmaloney
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
 * Handles TSFF (Time Series Free Format files created by the 
 * COLLECT TERM Scripts operated by Alberta ESRD
 * @author mmaloney Mike Maloney, Cove Software, LLC
 */
public class AesrdTsffPMParser 
	extends PMParser
{
	private int headerLen = 0;
	public static final String module = "AesrdTsffPMParser";
	
	public AesrdTsffPMParser()
	{
	}
	
	/** Returns the constant string "data-logger". */
	@Override
	public String getHeaderType() { return "AesrdTsff"; }

	/** Returns constant medium type for platform association. */
	@Override
	public String getMediumType() { return Constants.medium_EDL; }

	@Override
	public void parsePerformanceMeasurements(RawMessage msg) throws HeaderParseException
	{
		headerLen = 0;

		// The message delimiter may have gobbled the "raw-fname=" on the
		// first line leaving us with just the station name and trailing nfnum.
		String origLine = getNextLine(msg);
		try
		{
			String line = origLine;
			int idx = line.toLowerCase().indexOf("raw-fname=");
			if (idx > 0)
				line = line.substring(idx + 10).trim();
			for(idx = 0; idx < line.length()
				&& !Character.isWhitespace(line.charAt(idx));
				idx++);
			String mediumId = line.substring(0, idx);
			if (mediumId.length() == 0)
				throw new HeaderParseException("No station name on first line '"
					+ origLine + "'");
			msg.setPM(EdlPMParser.STATION, new Variable(mediumId));
			msg.setPM(GoesPMParser.DCP_ADDRESS, new Variable(mediumId));
			msg.setMediumId(mediumId);
			msg.setPM(GoesPMParser.FAILURE_CODE, new Variable('G'));
			msg.setHeaderLength(headerLen);
			msg.setPM(GoesPMParser.MESSAGE_LENGTH, 
				new Variable((long)(msg.data.length - headerLen)));
			Logger.instance().debug2(module + " after parse, headerLen="
				+ headerLen + ", msgLen=" + msg.getPM(GoesPMParser.MESSAGE_LENGTH));
		}
		catch(Exception ex)
		{
			throw new HeaderParseException("Bad start line '" + origLine
				+ "': " + ex);
		}
	}
	
	/**
	 * Start at headerLen and get the next line of data. Return as a string.
	 * Leave headerLen at the start of the next line.
	 * @return String containing next line of data, or null if this is end of msg.
	 */
	private String getNextLine(RawMessage msg)
	{
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

	@Override
	public int getHeaderLength()
	{
		return headerLen;
	}
	
	@Override
	public boolean containsExplicitLength() { return false; }
}
