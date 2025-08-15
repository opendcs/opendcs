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

import ilex.var.Variable;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

import org.opendcs.utils.logging.OpenDcsLoggerFactory;
import org.slf4j.Logger;

import decodes.db.Constants;

/**
 * Handles modem files created by the COLLECT TERM Scripts operated
 * by Alberta ESRD
 * @author mmaloney Mike Maloney, Cove Software, LLC
 */
public class AesrdModemPMParser extends PMParser
{
	private static final Logger log = OpenDcsLoggerFactory.getLogger();
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
				log.debug("After parse, headerLen={}, msgLen={}, msgTime={}",
				          headerLen, msg.getPM(GoesPMParser.MESSAGE_LENGTH), msgTime);
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
		log.trace(" getNextLine() headerLen={}, msgLen={}", headerLen, msg.data.length);
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
			log.warn(" No date/time stamp on line '{}'", line);
			return null;
		}

		String dateStr = line.substring(dateStart);
		log.trace("parsing date/time from '{}', with '{}'.", dateStr, sdfs[0].toPattern());

		try { return sdfs[0].parse(dateStr); }
		catch(Exception ex)
		{
			log.atTrace().setCause(ex).log("parsing date/time from '{}' with '{}'.", dateStr, sdfs[1].toPattern());
			try { return sdfs[1].parse(dateStr); }
			catch(ParseException ex2)
			{
				log.atWarn().setCause(ex2).log(" Bad date/time stamp on line '{}'", line);
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
