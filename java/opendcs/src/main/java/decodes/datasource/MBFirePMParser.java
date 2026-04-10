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

import java.util.Calendar;
import java.util.Properties;
import java.util.TimeZone;
import java.text.ParseException;
import java.text.SimpleDateFormat;

import org.opendcs.utils.logging.OpenDcsLoggerFactory;
import org.slf4j.Logger;

import ilex.var.Variable;
import decodes.db.Constants;

/**
  Concrete subclass of PMParser for parsing performance measurements
  out of GOES DCP messages.
*/
public class MBFirePMParser extends PMParser
{
	private static final Logger log = OpenDcsLoggerFactory.getLogger();
	private SimpleDateFormat dateFormat = null;
	private static final String dateFmtStr = "MMM dd, yyyy      HH";

	/** default constructor */
	public MBFirePMParser()
	{
		dateFormat = new SimpleDateFormat(dateFmtStr);
		java.util.TimeZone jtz=java.util.TimeZone.getTimeZone("GMT-06:00");
		dateFormat.setCalendar(Calendar.getInstance(jtz));
	}

	/**
	*/
	public void parsePerformanceMeasurements(RawMessage msg)
		throws HeaderParseException
	{
		byte data[] = msg.getData();
		StringBuilder idbuf = new StringBuilder();
		int idx = 0;
		for(; idx < data.length && (char)data[idx] == ' '; idx++);
		if (idx >= data.length)
			throw new HeaderParseException("Header completely blank");
		for(; idx<data.length && (char)data[idx] != ' '; idx++)
			idbuf.append((char)data[idx]);
		String ids = idbuf.toString();
		if (idx >= data.length || ids.length() == 0)
				throw new HeaderParseException("No Medium ID");

		if (ids.startsWith("-") || ids.equals("Station") || ids.startsWith("("))
			throw new HeaderParseException("Blank or comment line");

		msg.setMediumId(ids);
		log.trace("MBFirePMP: mediumId = '{}'",ids);
		while(idx<data.length && (char)data[idx] == ' ')
			idx++;
		if (data.length < idx + dateFmtStr.length())
			throw new HeaderParseException("No date in line");
		String datestr = new String(data, idx, dateFmtStr.length());

		try
		{
			log.trace("MBFirePMP: parsing date string = '{}'", datestr);
			Variable tv = new Variable(dateFormat.parse(datestr));
			msg.setPM(GoesPMParser.MESSAGE_TIME, tv);
		}
		catch(ParseException ex)
		{
			log.atWarn().setCause(ex).log("Bad date format '{}'", datestr);
		}

		int headerlen = idx + dateFmtStr.length();
		msg.setHeaderLength(headerlen);
		log.trace("MBFirePMP: header len = {}", headerlen);

		// Length should always be total length - 29
		int datalen = data.length - headerlen;
		msg.setPM(GoesPMParser.MESSAGE_LENGTH, new Variable(datalen));
		log.trace("MBFirePMP: msg len = {}", datalen);
	}

	/**
	  @return 18, the length of a NOAAPORT message header
	*/
	public int getHeaderLength()
	{
		return 28;
	}

	/** @return "mbfire" */
	public String getHeaderType()
	{
		return "mbfire";
	}

	/** @return "mbfire" */
	public String getMediumType()
	{
		return Constants.medium_mbfire;
	}

	/** 
	  We want to process a whole line ending with \r, so return false.
	  @return false
	*/
	public boolean containsExplicitLength()
	{
		return false;
	}
	
	@Override
	public void setProperties(Properties rsProps)
	{
		String s = rsProps.getProperty("webtz");
		if (s != null)
		{
			TimeZone tz = TimeZone.getTimeZone(s);
			if (tz == null)
				log.warn("MBFirePMParser invalid webtz property '{}' -- using GMT-06:00.", s);
			else
				dateFormat.setTimeZone(tz);
		}
	}
}

