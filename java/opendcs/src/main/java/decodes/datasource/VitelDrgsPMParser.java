/*
* Where Applicable, Copyright 2025 OpenDCS Consortium and/or its contributors
* 
* Licensed under the Apache License, Version 2.0 (the "License"); you may not
* use this file except in compliance with the License. You may obtain a copy
* of the License at
* 
*   http://www.apache.org/licenses/LICENSE-2.0
* 
* Unless required by applicable law or agreed to in writing, software 
* distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
* WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
* License for the specific language governing permissions and limitations 
* under the License.
*/
package decodes.datasource;

import java.util.Date;

import org.opendcs.utils.logging.OpenDcsLoggerFactory;
import org.slf4j.Logger;

import java.util.Calendar;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;

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
	private static final Logger log = OpenDcsLoggerFactory.getLogger();
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
		catch(NumberFormatException ex)
		{
			throw new HeaderParseException("Invalid length field '" + lenfield + '"',ex);
		}

		String chanfield = new String(data, 25, 3);
		try 
		{
			msg.setPM(GoesPMParser.CHANNEL, 
				new Variable(Long.parseLong(chanfield))); 
		}
		catch(NumberFormatException ex)
		{
			throw new HeaderParseException("Invalid channel field '"+ chanfield + "'", ex);
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
		catch(Exception ex)
		{
			log.atTrace().setCause(ex).log("Unable to set an optional performance measurement.");
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

