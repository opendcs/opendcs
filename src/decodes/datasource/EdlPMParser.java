/*
*  $Id$
*
*  $Log$
*  Revision 1.8  2012/09/06 19:03:25  shweta
*  fixed bug now when Xlinks have seconds embedded in header time.
*
*  Revision 1.7  2012/08/10 18:44:12  shweta
*  Parse Xlink header for GPRSLinks
*
*  Revision 1.6  2012/06/25 19:35:29  shweta
*  reverted back last change
*
*  Revision 1.4  2011/07/14 19:49:35  shweta
*  removed converting station name to uppercase.
*
*  Revision 1.3  2010/04/14 19:58:02  mjmaloney
*  Set GOES ID to Medium ID (kludge)
*
*  Revision 1.2  2009/10/25 15:37:40  mjmaloney
*  SSP Implementation
*
*  Revision 1.1  2008/04/04 18:21:00  cvs
*  Added legacy code to repository
*
*  Revision 1.5  2006/08/31 20:43:12  mmaloney
*  dev
*
*  Revision 1.4  2006/08/31 20:42:23  mmaloney
*  Added debug.
*
*  Revision 1.3  2004/08/24 23:52:43  mjmaloney
*  Added javadocs.
*
*  Revision 1.2  2003/12/12 17:55:32  mjmaloney
*  Working implementation of DirectoryDataSource.
*
*  Revision 1.1  2003/12/07 20:36:47  mjmaloney
*  First working implementation of EDL time stamping.
*
*/
package decodes.datasource;

import java.util.Date;
import java.util.HashMap;
import java.text.SimpleDateFormat;
import java.text.ParseException;

import ilex.var.Variable;
import ilex.util.Logger;

import decodes.datasource.RawMessage;
import decodes.db.Constants;

/**
Performance Measurement Parser for USGS EDL Header Lines.
*/
public class EdlPMParser extends PMParser
{
	// Private time/date stamp parsers:
	private static SimpleDateFormat beginDateTimeSdf = 
		new SimpleDateFormat("yyyyMMddHHmm Z");
	
	// Private time/date stamp parsers:
		private static SimpleDateFormat xlinkDateTimeSdf = 
			new SimpleDateFormat("yyyyMMddHHmmss Z");
	
	private static SimpleDateFormat endTimeSdf =
		new SimpleDateFormat("yyMMdd HHmmss Z");

	// The following properties can be set by this parser.

	/**  The STATION ID parsed out of the neader. */
	public static final String STATION = "Station";

	/** The length of the message that follows the header. */
	public static final String MESSAGE_LENGTH = GoesPMParser.MESSAGE_LENGTH;

	/**
	  The end time of the message. Set from DEVICE END TIME. Possibly 
	  over-ridden by ACTUAL END TIME. Stored as a Date Variable.
	*/
	public static final String MESSAGE_TIME = GoesPMParser.MESSAGE_TIME;

	/** Device End Time, stored as a string. */
	public static final String DEVICE_END_TIME = "DeviceEndTime";

	/** Actual End Time, stored as a string. */
	public static final String ACTUAL_END_TIME = "ActualEndTime";

	/**
	  Optional start time, complete time stamp stored as a date.
	  This is set from the BEGIN TIME and BEGIN DATE header fields.
	*/
	public static final String BEGIN_TIME_STAMP = "BeginTimeStamp";

	public static final String END_TIME_STAMP = "EndTimeStamp";

	/** The DEVICE string contained in the header */
	public static final String DEVICE = "Device";

	/** The SOURCE string contained in the header */
	public static final String SOURCE = "Source";

	/** A string containing the multi-line optional EDL NOTES field. */
	public static final String EDL_NOTES = "EdlNotes";

	/** Flag set when header is parsed, indicating whether begin time had TZ. */
	public boolean beginTimeHasTz;
	
	/** Flag set when header is parsed, indicating whether xlink time had TZ. */
	public boolean XLinkTimeHasTz;
	
	/** Flag set when header is parsed, indicating if time is in format HHmmss */
	public boolean XLinkTimeHasSecs=false;


	/** Use factory method in PMParser, do not construct directly. */
	public EdlPMParser()
	{
		super();
	}

	/** Returns the constant string "data-logger". */
	public String getHeaderType() { return "data-logger"; }

	/** Returns constant medium type for platform association. */
	public String getMediumType() { return Constants.medium_EDL; }

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
		beginTimeHasTz = false;
		byte data[] = msg.getData();
		int len = data.length;

		boolean inNotes = false;
		String beginDate = null;
		String beginTime = null;
		String endTime = null;
		String station = null;
		String device = null;
		String xLinkDate=null;
		String xLinkTime=null;
		StringBuffer notes = new StringBuffer();
		int e=0;		
		for(int p=0; p<len-3; p = e)
		{
			// Find the beginning of the next line.
			for(e = p; e < len && data[e] != (byte)'\n'; e++);
			e++;
			


			// Check for start of new tag.
			if (data[p] == (byte)'/' && data[p+1] == (byte)'/')
			{
				p += 2;

				String s = new String(data, p, e-p);
				
				//Removed uppercase as in case of dataloggers if transport medium is defined in lowercase, 
				//decodes fails to find matching platform
				
				//s = s.toUpperCase().trim();
				
				s = s.trim();
				if (s.length() == 0)
					continue;	// Skip comment line with just '//'
				if (s.startsWith("STATION"))
				{
					String val = s.substring(7).trim();
					msg.setPM(STATION, new Variable(val));
				}
				else if (s.startsWith("DEVICE END TIME")) // do before DEVICE !!
				{
					if (endTime == null)
						endTime = s.substring(15).trim();
				}
				else if (s.startsWith("DEVICE"))
				{
					String val = s.substring(6).trim();
					int hyphen = val.indexOf('-');
					int space = val.indexOf(' ');
					if (hyphen >= 0)
					{
						if (space > 0 && hyphen < space)
							val = val.substring(0, space);
						else if (space > 0 && space < hyphen)
							val = val.substring(0, space) + "-"
								+ val.substring(space+1);
					}
					else // no hyphen
					{
						if (space >= 0)
							val = val.substring(0,space) + "-"
								+ val.substring(space+1);
					}
					space = val.indexOf(' ');
					if (space > 0)
						val = val.substring(0,space);
					msg.setPM(DEVICE, new Variable(val));
				}
				else if (s.startsWith("SOURCE"))
				{
					String val = s.substring(6).trim();
					msg.setPM(SOURCE, new Variable(val));
				}
				else if (s.startsWith("BEGIN DATE"))
				{
					beginDate = s.substring(10).trim();
				}
				else if (s.startsWith("BEGIN TIME"))
				{
					beginTime = s.substring(10).trim();
				}
				else if (s.startsWith("XLINK DATE"))
				{
					xLinkDate = s.substring(10).trim();
				}
				else if (s.startsWith("XLINK TIME"))
				{
					xLinkTime = s.substring(10).trim();					
				}
				else if (s.startsWith("ACTUAL END TIME"))
				{
					endTime = s.substring(15).trim();
				}
				else if (s.startsWith("EDL NOTES")
				      || s.startsWith("PFC NOTES")
				      || s.startsWith("DEVICE NOTES"))
				{
					inNotes = true;
				}
				else if (s.startsWith("DATA"))
				{
					inNotes = false;
				}
			}
			else if (inNotes)
				notes.append(new String(data, p, e-p));
			else // this is the end of the header!
			{
				msg.setHeaderLength(p);
				msg.setPM(MESSAGE_LENGTH, new Variable((long)(len - p)));
				break;
			}
		}

		if (beginDate != null)
		{
			if (beginTime != null)
			{
				// begin time can optionally contain time zone.
				int idx = beginTime.lastIndexOf('S');
				if (idx != -1)
				{
					beginTimeHasTz = true;
					beginTime = beginTime.substring(0, idx) + "00";
				}
				else // Add dummy offset to UTC
				{
					beginTimeHasTz = false;
					beginTime += " +0000";
				}
				beginDate += beginTime;
			}
			else
				beginDate += "0000 +0000"; // HHMM & TZ
			try
			{
				Logger.instance().debug1("Parsing begin date/time '"
					+ beginDate + "'");
				Date d = beginDateTimeSdf.parse(beginDate);
				msg.setPM(BEGIN_TIME_STAMP, new Variable(d));
			}
			catch(ParseException ex)
			{
				Logger.instance().log(Logger.E_FAILURE, 
					"Unparsable begin time '" + beginTime + "': Ignored.");
			}
		}

		if (endTime != null)
		{
			// Check for start of timezone.
			int idx = endTime.indexOf('-');
			if (idx == -1)
				idx = endTime.indexOf('+');

			if (idx == -1) // No time zone at all, add one.
				endTime += " +0000";
			else
			{
				int i = ++idx;  // idx points to first digit after sign.

				for(; i < endTime.length() 
					&& i-idx <= 4
					&& Character.isDigit(endTime.charAt(i)); i++);
				// i now points to first non-digit after TZ

				switch(i-idx)  // i-idx is # of digits after sign.
				{
				case 0: 
					endTime = endTime.substring(0,idx) + "0000"; 
					break;
				case 1: // 1 digit hour? move to position 2 in HHMM:
					endTime = endTime.substring(0,idx) + "0" 
						+ endTime.charAt(idx) + "00";
					break;
				case 2: // HH only, add MM
					endTime = endTime.substring(0,i) + "00";
					break;
				case 3: // HHM, ad lcd
					endTime = endTime.substring(0,i) + "0";
					break;
				default: // complete. Just truncate at 4 digits.
					endTime = endTime.substring(0, idx+4);
				}
			}
				
			try
			{
				msg.setPM(END_TIME_STAMP, new Variable(
					endTimeSdf.parse(endTime)));
			}
			catch(ParseException ex)
			{
				Logger.instance().log(Logger.E_FAILURE, "Unparsable end time '"
					+ endTime + "': Ignored.");
			}
		}
		
		if (xLinkDate != null)
		{
			if (xLinkTime != null)								
			{			
				
				if(xLinkTime.length()==10)
					XLinkTimeHasSecs=true;
				else
					XLinkTimeHasSecs=false;
				
				// begin time can optionally contain time zone.
				int idx = xLinkTime.lastIndexOf('S');
				if (idx != -1)
				{
					XLinkTimeHasTz = true;
					xLinkTime = beginTime.substring(0, idx) + "00";
				}
				else // Add dummy offset to UTC
				{
					XLinkTimeHasTz = false;
					xLinkTime += " +0000";
				}
				xLinkDate += xLinkTime;				
			}
			else			
				xLinkDate += "0000 +0000"; // HHMM & TZ
			try
			{
			Logger.instance().debug1("Parsing xLink date/time '"
					+ xLinkDate + "'");
			
			if(XLinkTimeHasSecs)
			{			
			Date dd = xlinkDateTimeSdf.parse(xLinkDate);
			msg.setPM(GoesPMParser.MESSAGE_TIME, new Variable(dd));
			msg.setTimeStamp(dd);
			}
			else
			{
				Date d = beginDateTimeSdf.parse(xLinkDate);
				msg.setPM(GoesPMParser.MESSAGE_TIME, new Variable(d));
				msg.setTimeStamp(d);
			}
			}
			catch(ParseException ex)
			{
				Logger.instance().log(Logger.E_FAILURE, 
					"Unparsable xlink time '" + xLinkTime + "': Ignored.");
			}
		}

		if (notes.length() > 0)
		{
			msg.setPM(EDL_NOTES, new Variable(notes.toString()));
		}

		// Construct medium ID by concatenating station to device.
		if (msg.getMediumId() == null)
		{
			String mid = System.getProperty("MEDIUMID");
			if (mid == null)
			{
				Variable v = msg.getPM(STATION);
				if (v == null)
					throw new HeaderParseException("No STATION in EDL file.");
				mid = v.getStringValue();
				v = msg.getPM(DEVICE);
				if (v != null)
					mid = mid + "-" + v.getStringValue();
			}
			Logger.instance().log(Logger.E_DEBUG3,
				"Setting EDL File Medium ID to '" + mid + "'");
			msg.setMediumId(mid);
			msg.setPM(GoesPMParser.DCP_ADDRESS, new Variable(mid));
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

