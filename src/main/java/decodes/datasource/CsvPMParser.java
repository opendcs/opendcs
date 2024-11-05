/*
*  $Id$
*/
package decodes.datasource;

import java.util.Date;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.TimeZone;
import java.text.SimpleDateFormat;

import lrgs.common.DcpMsg;
import ilex.util.Logger;
import ilex.util.PropertiesUtil;
import ilex.var.Variable;

import decodes.db.Constants;

/**
 * Use this when parsing CSV (comma-separated-value) files where each
 * line of the file is to be considered a separate 'message'. This PMP
 * gets the medium ID and time from columns and sets the length from
 * the column you specify where the data starts.
 * Properties you can set in the routing spec include:
 * <ul>
 *   <li>idcol - The column (1=first column) where the ID is found.</li>
 *   <li>datetimecol - The column where the date/time value starts
 *       (note that the date/time may span several columns</li>
 *   <li>datetimefmt - SimpleDateFormat spec for parsing the date/time</li>
 *   <li>datacol - The column where the data starts</li>
 *   <li>timezone - The time zone for parsing the date/time field</li>
 *   <li>delim - The string delimiter to use for columns (default = comma)</li>
 * </ul>
 * Example, for the line:
 *    B2-WI,May,12,2010,06:00,-2.8,83,254,7.4,16,0,11.9,-3.5
 * Set these properties:
 *   idcol=1   (B2-WI is the ID)
 *   datetimecol=2
 *   datetimefmt=MMM,dd,yyyy,HH:mm
 *   timezone=UTC (or whatever time zone should be used)
 *   datacol=6    (first data is -2.8 which is column 6)
 *   delim=,
*/
public class CsvPMParser extends PMParser
{
	private SimpleDateFormat sdf = null;
	public int idcol = 1;
	public int datetimecol = 2;
	public String datetimefmt = "MMM,dd,yyyy,HH:mm";
	public String timezone = "UTC";
	public int datacol = 7;
	public String delim = ",";
	public String mediumType = Constants.medium_Other;
	public String headerType = "csv";
	private int hdrLength = 0;
	
	/** default constructor */
	public CsvPMParser()
	{

	}

	public void setProperties(Properties props)
	{
		PropertiesUtil.loadFromProps(this, props);
		sdf = new SimpleDateFormat(datetimefmt);
		sdf.setTimeZone(TimeZone.getTimeZone(timezone));
		Logger.instance().debug1("CsvPMParser delim='" + delim + "'");
	}

	/**
	  Parses the DOMSAT header.
	  Sets the mediumID to the GOES DCP Address.
	  @param msg the message to parse.
	*/
	public void parsePerformanceMeasurements(RawMessage msg)
		throws HeaderParseException
	{
		if (sdf == null)
		{
			Logger.instance().warning("CsvPMParser called without setting props.");
			setProperties(new Properties());
		}

		DcpMsg origMsg = msg.getOrigDcpMsg();
		String data = new String(msg.getData());
		
		// Get the ID
		StringTokenizer st = new StringTokenizer(data, delim);
		for(int col=0; col<idcol-1 && st.hasMoreElements(); st.nextElement());
		if (!st.hasMoreElements())
			throw new HeaderParseException("CsvPMParser no ID found at column" + idcol
				+ " in data '" + data + "'");
		String id = st.nextToken().trim();
		msg.setMediumId(id);
		msg.setPM(GoesPMParser.DCP_ADDRESS, new Variable(id));
		msg.setPM(EdlPMParser.STATION, new Variable(id));

		Date msgTime = null;
		
		// find the start of the date/time
		try
		{
			int dtstart = 0;
			for(int ndelims=0; 
			    dtstart < data.length() && ndelims < datetimecol-1; ndelims++)
				dtstart = data.indexOf(delim, dtstart + delim.length());
			dtstart += delim.length();
			msgTime = sdf.parse(data.substring(dtstart));
			msgTime.setTime(msgTime.getTime() + 1000L); // Add 1 second
			msg.setPM(GoesPMParser.MESSAGE_TIME, new Variable(msgTime));
			msg.setTimeStamp(msgTime);
		}
		catch(Exception ex)
		{
			String err = "CsvPMParser no msg time at column " + datetimecol
				+ ": " + ex + " expected format='" + sdf.toPattern() + "'";
			throw new HeaderParseException(err);
		}
		
		// Set header length by finding the start of the data column.
		try
		{
			hdrLength = 0;
			for(int ndelims=0; 
				hdrLength < data.length() && ndelims < datacol-1; ndelims++)
				hdrLength = data.indexOf(delim, hdrLength + delim.length());
			hdrLength += delim.length();
			msg.setHeaderLength(hdrLength);
			msg.setPM(GoesPMParser.MESSAGE_LENGTH, new Variable(data.length()));
		}
		catch(Exception ex)
		{
			String err = "CsvPMParser no msg time at column " + datetimecol
				+ ": " + ex;
			throw new HeaderParseException(err);
		}
		
		msg.setPM(GoesPMParser.FAILURE_CODE, new Variable('G'));
		Logger.instance().debug3("CsvPMParser: mediumId='" + id + "', msg time=" + msgTime);
	}

	/** @return length as determined by the datacol. */
	public int getHeaderLength()
	{
		return hdrLength;
	}

	/** @return "csv" */
	public String getHeaderType()
	{
		return "csv";
	}

	/** @return medium_OTHER. */
	public String getMediumType()
	{
		return mediumType;
	}
	
	public boolean containsExplicitLength()
	{
		return false;
	}
}

