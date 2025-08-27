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

import java.util.GregorianCalendar;
import java.util.Calendar;
import java.util.TimeZone;
import java.text.SimpleDateFormat;

import org.opendcs.utils.logging.OpenDcsLoggerFactory;
import org.slf4j.Logger;

import ilex.util.ByteUtil;
import ilex.var.Variable;

import decodes.db.Constants;

/**
  Concrete subclass of PMParser for parsing performance measurements
  out of GOES DCP messages.
*/
public class NoaaportPMParser extends PMParser
{
	private static final Logger log = OpenDcsLoggerFactory.getLogger();
	private static SimpleDateFormat goesDateFormat = null;

	/** default constructor */
	public NoaaportPMParser()
	{
		if (goesDateFormat == null)
		{
			goesDateFormat = new SimpleDateFormat("yyDDDHHmmss");
			java.util.TimeZone jtz=java.util.TimeZone.getTimeZone("UTC");
			goesDateFormat.setCalendar(Calendar.getInstance(jtz));
		}
	}

	/**
	  Parses performance measurements from a NOAAPORT message.
	  NOAAPORT messages are formatted as follows:
	  <ul>
		<li>
		[SOH]\r\r\nQQQ\r\r\nHHH\r\r\nDDD\r\r\n[ETX]
		</li>
	  </ul>
	  where QQQ is a 3-digit sequence number,
	  <ul>
		<li>
		HHH is a header consisting of product ID, office ID and day/time stamp.
		</li>
		<li>DDD is the DCP message.</li>
	  </ul>
	  The DDD DCP message field is formatted as follows:
	  <ul>
		<li>[0x1e]AAAAAAAA DDDHHMMSSddddd....SSFFNN CCCs</li>
	  </ul>
	  where
	  <ul>
		<li>0x1e is a flag that this is the start of the real message.</li>
		<li>AAAAAAAA is the 8-hex digit DCP address</li>
		<li>DDDHHMMSS is the date/time stamp with no year.</li>
		<li>dddd.... is the message data with no length indication.</li>
		<li>SS is signal strength</li>
		<li>FF is frequency offset, first char will be + or -</li>
		<li>NN is a placeholder for IFPD, always set to NN</li>
		<li>CCC is GOES channel number, padded left with blanks, not zeros.</li>
		<li>s is spacecraft (E or W)</li>
	  </ul>
	  This parser assumes that you use 0x1e to \r\r\nETX as the socket stream
	  framing sequences. This means that the NOAAPORT sequence number and
	  HHH header are ignored. Hence only the DDD data field, as described above
	  is parsed, starting with the AAAAAAAA DCP address field.
	  <p>
	  Sets the medium Id to the 8-hex-digit DCP address.
	  @param msg the message to parse.
	*/
	public void parsePerformanceMeasurements(RawMessage msg)
		throws HeaderParseException
	{
		byte data[] = msg.getData();

		// header=18 will cause decoding to start at the right place.
		// In NOAAPORT, some of the header fields are at the end of the 
		// message. This is OK, they'll just be ignored by the decoder.
		msg.setHeaderLength(18);

		// The required stuff before and after message totals 29 bytes.
		// This is the absolute minimum message length.
		if (data == null || data.length < 29)
			throw new HeaderParseException(
				"NOAAPORT Header to short, 29 bytes is required");

		// Parse DCP Address. Make sure first 8 bytes are hex digits.
		for(int i=0; i<8; i++)
			if (ByteUtil.fromHexChar((char)data[i]) == -1)
				throw new HeaderParseException(
					"Invalid DCP Address '" + new String(data, 0, 8) + "'");
		String dcpAddr = new String(data, 0, 8);
		dcpAddr = dcpAddr.toUpperCase();
		msg.setPM(GoesPMParser.DCP_ADDRESS, new Variable(dcpAddr));

		msg.setPM(GoesPMParser.FAILURE_CODE, 
			new Variable((char)data[8] == ' ' ? 'G' : '?'));


		// time stamp
		int day, hour, min, sec;
		try
		{
			day = Integer.parseInt(new String(data, 9, 3));
			hour = Integer.parseInt(new String(data, 12, 2));
			min = Integer.parseInt(new String(data, 14, 2));
			sec = Integer.parseInt(new String(data, 16, 2));
		}
		catch(NumberFormatException ex)
		{
			throw new HeaderParseException("Invalid NOAAPORT time-stamp '"
				+ new String(data, 9, 9) + "'",ex);
		}

		GregorianCalendar cal = new GregorianCalendar();
		cal.setTimeZone(TimeZone.getTimeZone("UTC"));
		int year = cal.get(Calendar.YEAR);
		if (day > cal.get(Calendar.DAY_OF_YEAR))
			--year;
		cal.set(Calendar.YEAR, year);
		cal.set(Calendar.DAY_OF_YEAR, day);
		cal.set(Calendar.HOUR_OF_DAY, hour);
		cal.set(Calendar.MINUTE, min);
		cal.set(Calendar.SECOND, sec);

		Variable tv = new Variable(cal.getTime());
		msg.setPM(GoesPMParser.MESSAGE_TIME, tv);

		// Length should always be total length - 29
		msg.setPM(GoesPMParser.MESSAGE_LENGTH, new Variable(data.length - 29));

		// Goes Spacecraft (E or W)
		int iEnd = data.length - 1;
		while (iEnd > 4 && Character.isWhitespace((char)data[iEnd])) 
			--iEnd;
		msg.setPM(GoesPMParser.SPACECRAFT, new Variable((char)data[iEnd]));

		// channel
		iEnd -= 3;
		for (int i = 0; i < 2; ++i)
			if (data[iEnd + i] == ' ')
				data[iEnd + i] = (byte)'0';

		String sChan = new String(data, iEnd, 3);
		try
		{
			int chan = Integer.parseInt(sChan);
			msg.setPM(GoesPMParser.CHANNEL, new Variable(chan));
		}
		catch(NumberFormatException ex)
		{
			throw new HeaderParseException(
				"Invalid channel field in NOAAPORT trailer '" + sChan + "'",ex);
		}
		
		// signal & frequency quality, fake in the IFPD //
		iEnd -= 7;
		String sss = new String(data, iEnd, 2);
		try
		{
			msg.setPM(GoesPMParser.SIGNAL_STRENGTH,
				new Variable(Integer.parseInt(sss)));
		}
		catch(NumberFormatException ex)
		{
			log.atWarn().setCause(ex).log("Invalid signal strength field in NOAAPORT trailer '{}' -- ignored", sss);
		}

		String fos = new String(data, iEnd+2, 2);
		if (fos.charAt(0) == '+')
			fos = fos.substring(1);
		try
		{
			msg.setPM(GoesPMParser.FREQ_OFFSET, 
				new Variable(Integer.parseInt(fos)));
		}
		catch(NumberFormatException ex)
		{
			log.atWarn()
			   .setCause(ex)
			   .log("Invalid Frequency Offset field in NOAAPORT trailer '{}' -- ignored.", fos);
		}

		msg.setMediumId(dcpAddr);
	}

	/**
	  @return 18, the length of a NOAAPORT message header
	*/
	public int getHeaderLength()
	{
		return 18;
	}

	/** @return "noaaport" */
	public String getHeaderType()
	{
		return "noaaport";
	}

	/** @return data type constant for NOAAPORT message */
	public String getMediumType()
	{
		return Constants.medium_Goes; // Noaaport msgs are still from GOES!
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

