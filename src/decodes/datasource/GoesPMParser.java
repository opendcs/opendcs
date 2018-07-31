/*
*  $Id$
*/
package decodes.datasource;

import java.util.Date;
import java.util.Calendar;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;

import ilex.util.ByteUtil;
import ilex.var.Variable;

import decodes.db.Constants;

/**
  Concrete subclass of PMParser for parsing performance measurements
  out of GOES DCP messages.
*/
public class GoesPMParser extends PMParser
{
	public static final String DCP_ADDRESS = "DcpAddress";
	public static final String MESSAGE_TIME = "Time";
	public static final String MESSAGE_LENGTH = "Length";
	public static final String FAILURE_CODE = "FailureCode";
	public static final String SIGNAL_STRENGTH = "SignalStrength";
	public static final String FREQ_OFFSET = "FrequencyOffset";
	public static final String MOD_INDEX = "ModulationIndex";
	public static final String QUALITY = "Quality";
	public static final String CHANNEL = "Channel";
	public static final String SPACECRAFT = "Spacecraft";
	public static final String UPLINK_CARRIER = "UplinkCarrier";
	public static final String BAUD = "Baud";
	public static final String CARRIER_START = "CarrierStart";
	public static final String CARRIER_STOP = "CarrierStop";
	public static final String DOMSAT_TIME = "DomsatTime";
	public static final String DCP_MSG_FLAGS = "DcpMsgFlags";
	public static final String GPS_SYNC = "GPS";
	
	public static final String SITE_NAME = "SiteName";
	public static final String SITE_DESC = "SiteDesc";
	public static final String FILE_NAME = "filename";

	private SimpleDateFormat goesDateFormat = null;

	/** default constructor */
	public GoesPMParser()
	{
		goesDateFormat = new SimpleDateFormat("yyDDDHHmmss");
		java.util.TimeZone jtz=java.util.TimeZone.getTimeZone("UTC");
		goesDateFormat.setCalendar(Calendar.getInstance(jtz));
	}

	/**
	  Parses the DOMSAT header.
	  Sets the mediumID to the GOES DCP Address.
	  @param msg the message to parse.
	*/
	public void parsePerformanceMeasurements(RawMessage msg)
		throws HeaderParseException
	{
		byte data[] = msg.getData();
		msg.setHeaderLength(37);

		if (data == null || data.length < 37)
			throw new HeaderParseException(
				"Header to short, 37 bytes is required");

		/*
		 * Parse necessary parameters: DCP addr, length, channel, timestamp
		 */

		// Make sure first 8 bytes are hex digits
		for(int i=0; i<8; i++)
			if (ByteUtil.fromHexChar((char)data[i]) == -1)
				throw new HeaderParseException("Invalid GOES DCP Address");
		String dcpAddr = new String(data, 0, 8);
		dcpAddr = dcpAddr.toUpperCase();
		msg.setPM(DCP_ADDRESS, new Variable(dcpAddr));

		String lenfield = new String(data, 32, 5);
		try 
		{
			msg.setPM(MESSAGE_LENGTH, new Variable(Long.parseLong(lenfield)));
		}
		catch(NumberFormatException e)
		{
			throw new HeaderParseException("Invalid length field '"
				+ lenfield + '"');
		}

		String chanfield = new String(data, 26, 3);
		try { msg.setPM(CHANNEL, new Variable(Long.parseLong(chanfield))); }
		catch(NumberFormatException e)
		{
			throw new HeaderParseException("Invalid channel field '"
				+ chanfield + "'");
		}

		String datefield = new String(data, 8, 11);
		try
		{
			Date d = goesDateFormat.parse(datefield, new ParsePosition(0));
			if (d == null)
			{
				String emsg = "Invalid timestamp field '" + datefield + "'";
				System.err.println(emsg);
				System.err.println("SDF Pattern '" + goesDateFormat.toPattern() + "'");
				throw new HeaderParseException(emsg);
			}
			msg.setPM(MESSAGE_TIME, new Variable(d));
			msg.setTimeStamp(d);
		}
		catch(NumberFormatException ex)
		{
			throw new HeaderParseException("Invalid timestamp field '"
				+ datefield + "': " + ex);
		}

		/*
		 * The rest of the PMs are optional. Don't fail if they can't be
		 * parsed.
		 */

		try
		{
			msg.setPM(FAILURE_CODE, new Variable((char)data[19]));
		}
		catch(Exception e)
		{
			// Silently allow failures for above.
		}
		try
		{
			String ss = new String(data, 20, 2);
			long li = Long.parseLong(ss);
			msg.setPM(SIGNAL_STRENGTH, new Variable(li));
		}
		catch(Exception e)
		{
			// Silently allow failures for above.
		}
		try
		{
			String fos = new String(data, 22, 2);
			long li = 0;
			if ( fos.charAt(1) == 'A' ) {
				li = 10;
				if ( fos.charAt(0) == '-')
					li *= -1;			
			} else {
				if (fos.charAt(0) == '+')
					fos = fos.substring(1);
				li = Long.parseLong(fos);
			}
			msg.setPM(FREQ_OFFSET, new Variable(li));
		}
		catch(Exception e)
		{
			// Silently allow failures for above.
		}
		try
		{
			msg.setPM(MOD_INDEX, new Variable((char)data[24]));
		}
		catch(Exception e)
		{
			// Silently allow failures for above.
		}
		try
		{
			msg.setPM(QUALITY, new Variable((char)data[25]));
		}
		catch(Exception e)
		{
			// Silently allow failures for above.
		}
		try
		{
			msg.setPM(SPACECRAFT, new Variable((char)data[29]));
		}
		catch(Exception e)
		{
			// Silently allow failures for above.
		}
		try
		{
			msg.setPM(UPLINK_CARRIER, new Variable(new String(data, 30, 2)));
		}
		catch(Exception e)
		{
			// Silently allow failures for above.
		}
		if (data.length > 37)
		{
			int f = (int)data[37];
			msg.setPM(GPS_SYNC, new Variable((f & 0x02) == 0 ? 0 : 1));
		}
		msg.setMediumId(dcpAddr);
	}

	/** @return 37, the length of a DOMSAT header. */
	public int getHeaderLength()
	{
		return 37;
	}

	/** @return "goes" */
	public String getHeaderType()
	{
		return "goes";
	}

	/** @return the medium type constant for GOES DCP messages. */
	public String getMediumType()
	{
		return Constants.medium_Goes;
	}
}

