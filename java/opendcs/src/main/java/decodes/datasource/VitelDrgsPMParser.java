/*
*  $Id$
*
*  $Log$
*  Revision 1.1  2008/04/04 18:21:00  cvs
*  Added legacy code to repository
*
*  Revision 1.5  2004/08/24 23:52:46  mjmaloney
*  Added javadocs.
*
*  Revision 1.4  2003/12/07 20:36:49  mjmaloney
*  First working implementation of EDL time stamping.
*
*  Revision 1.3  2002/06/03 15:39:00  mjmaloney
*  DR fixes.
*
*  Revision 1.2  2002/06/03 00:54:43  mjmaloney
*  dev
*
*  Revision 1.1  2002/05/21 19:50:19  mjmaloney
*  Created to handle slightly different header coming from the Vitel DRGS.
*
*
*	5/21/2002 - created based on GoesPMParser
*/
package decodes.datasource;

import java.util.Date;
import java.util.Calendar;
import java.text.ParsePosition;
import java.text.ParseException;
import java.text.SimpleDateFormat;

import ilex.util.ArrayUtil;
import ilex.util.ByteUtil;
import ilex.var.Variable;

import decodes.db.Constants;

/**
  Concrete subclass of PMParser for parsing performance measurements
  from socket stream messages from a Vitel DRGS.
  <p>
  The vitel DRGS header differs from DOMSAT in that it does not
  contain the FAILURE_CODE field.
*/
public class VitelDrgsPMParser extends PMParser
{
	/* Note: This class will use the label strings defined in
	   GoesPMParser for DCP_ADDRESS, MESSAGE_TIME, MESSAGE_LENGTH,
	   SIGNAL_STRENGTH, FREQ_OFFSET, MOD_INDEX, QUALITY, CHANNEL,
	   SPACECRAFT, and UPLINK_CARRIER.
	*/

	private static SimpleDateFormat goesDateFormat = null;

	/** default constructor */
	public VitelDrgsPMParser()
	{
		if (goesDateFormat == null)
		{
			goesDateFormat = new SimpleDateFormat("yyDDDHHmmss");
			java.util.TimeZone jtz=java.util.TimeZone.getTimeZone("UTC");
			goesDateFormat.setCalendar(Calendar.getInstance(jtz));
		}
	}

	/**
	  Parse performance measurements.
	  @param msg the message to parse.
	*/
	public void parsePerformanceMeasurements(RawMessage msg)
		throws HeaderParseException
	{
		byte data[] = msg.getData();
		msg.setHeaderLength(36);

		if (data == null || data.length < 36)
			throw new HeaderParseException(
				"Header to short, 36 bytes is required");

		/*
		 * Parse necessary parameters: DCP addr, length, channel, timestamp
		 */

		// Make sure first 8 bytes are hex digits
		for(int i=0; i<8; i++)
			if (ByteUtil.fromHexChar((char)data[i]) == -1)
				throw new HeaderParseException("Invalid DCP Address");
		String dcpAddr = new String(data, 0, 8);
		dcpAddr = dcpAddr.toUpperCase();
		msg.setPM(GoesPMParser.DCP_ADDRESS, new Variable(dcpAddr));
		msg.setMediumId(dcpAddr);

		String lenfield = new String(data, 31, 5);
		try 
		{
			msg.setPM(GoesPMParser.MESSAGE_LENGTH, 
				new Variable(Long.parseLong(lenfield)));
		}
		catch(NumberFormatException e)
		{
			throw new HeaderParseException("Invalid length field '"
				+ lenfield + '"');
		}

		String chanfield = new String(data, 25, 3);
		try 
		{
			msg.setPM(GoesPMParser.CHANNEL, 
				new Variable(Long.parseLong(chanfield))); 
		}
		catch(NumberFormatException e)
		{
			throw new HeaderParseException("Invalid channel field '"
				+ chanfield + "'");
		}

		String datefield = new String(data, 8, 11);
		Date d = goesDateFormat.parse(datefield, new ParsePosition(0));
		if (d == null)
			throw new HeaderParseException("Invalid timestamp field '"
				+ datefield + "'");
		msg.setPM(GoesPMParser.MESSAGE_TIME, new Variable(d));

		/*
		 * The rest of the PMs are optional. Don't fail if they can't be
		 * parsed.
		 */

		try
		{
			// No failure code from Vitel DRGS
			//msg.setPM(GoesPMParser.FAILURE_CODE,new Variable((char)data[19]));

			msg.setPM(GoesPMParser.SIGNAL_STRENGTH,
				new Variable(Long.parseLong(new String(data, 19, 2))));

			msg.setPM(GoesPMParser.FREQ_OFFSET, 
				new Variable(new String(data, 21, 2)));

			msg.setPM(GoesPMParser.MOD_INDEX, new Variable((char)data[23]));

			msg.setPM(GoesPMParser.QUALITY, new Variable((char)data[24]));

			msg.setPM(GoesPMParser.SPACECRAFT, new Variable((char)data[28]));

			msg.setPM(GoesPMParser.UPLINK_CARRIER, 
				new Variable(new String(data, 29, 2)));
		}
		catch(Exception e)
		{
			// Silently allow failures for above.
		}
	}

	/** @return 36, the length of the vitel header */
	public int getHeaderLength()
	{
		return 36;
	}

	/** @return "vitel" */
	public String getHeaderType()
	{
		return "vitel";
	}

	/** @return medium type constant for this kind of message. */
	public String getMediumType()
	{
		return Constants.medium_Goes;
	}

}

