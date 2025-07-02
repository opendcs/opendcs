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

import org.slf4j.LoggerFactory;

import java.util.Date;
import java.text.SimpleDateFormat;

import lrgs.common.DcpMsg;

import ilex.var.Variable;

import decodes.db.Constants;

/**
  Concrete subclass of PMParser for parsing performance measurements
  out of IRIDIUM SBD messages.
*/
public class IridiumPMParser extends PMParser
{
	private static final org.slf4j.Logger logger = LoggerFactory.getLogger(IridiumPMParser.class);
	// Performance Measurement tags:
	public static final String LATITUDE = "latitude";
	public static final String LONGITUDE = "longitude";
	public static final String CEP_RADIUS = "radius";
	public static final String PAYLOAD_LEN = "PayloadSize";

	private SimpleDateFormat goesDateFormat = null;
	private int headerLength = -1;

	/** default constructor */
	public IridiumPMParser()
	{
		goesDateFormat = new SimpleDateFormat("yyDDDHHmmss");
		goesDateFormat.setTimeZone(java.util.TimeZone.getTimeZone("UTC"));
	}

	/**
	  Parses the DOMSAT header.
	  Sets the mediumID to the DCP's IMEI.
	  @param msg the message to parse.
	*/
	public void parsePerformanceMeasurements(RawMessage msg) throws HeaderParseException
	{
		String hdr = null;
		try
		{
			DcpMsg origMsg = msg.getOrigDcpMsg();
			byte data[] = msg.getData();
			if (data == null || data.length < 75)
			{
				throw new HeaderParseException("Header too short, 75 bytes is required");
			}
			msg.setPM(GoesPMParser.MESSAGE_LENGTH, new Variable(data.length));
			hdr = new String(data);

			if (origMsg != null)
			{
				String emei = origMsg.getDcpAddress().toString();
				msg.setMediumId(emei);
				msg.setPM(GoesPMParser.DCP_ADDRESS, new Variable(emei));
				Date d = origMsg.getXmitTime();
				msg.setPM(GoesPMParser.MESSAGE_TIME, new Variable(d));
				msg.setTimeStamp(d);
				int sessionStat = origMsg.getSessionStatus();
				if (sessionStat == 0 || sessionStat == 1 || sessionStat == 2)
				{
					msg.setPM(GoesPMParser.FAILURE_CODE, new Variable('G'));
				}
				else
				{
					msg.setPM(GoesPMParser.FAILURE_CODE, new Variable('?'));
				}
				msg.setPM(GoesPMParser.SPACECRAFT, new Variable('I'));
				msg.setPM(GoesPMParser.UPLINK_CARRIER, new Variable("IR"));
				msg.setPM(GoesPMParser.DCP_MSG_FLAGS,new Variable(origMsg.flagbits));
			}
			else
			{
				// Parse information out of the ASCII-ized msg header. Example:
				// ID=300034012724030,TIME=08229165743,STAT=00,MO=00551,MT=00000,CDR=01D3997C
				int idx = hdr.indexOf("ID=");
				if (idx == -1)
				{
					throw new HeaderParseException("No ID (EMEI) in Iridium header");
				}
				String s = hdr.substring(idx+3, idx+3+15);
				msg.setMediumId(s);
				msg.setPM(GoesPMParser.DCP_ADDRESS, new Variable(s));

				// The TIME in the header is the time the message was sent from the
				// SBD gateway (over the internet). It is not the DCP's original
				// transmission time. The difference between the two times can vary
				// from a few seconds to several minutes. Users decoding these
				// messages and, in particular, use F(MOFF) must be aware of this.
				// DECODES F(MHD) and F(MHT) operations can be used to change this
				// time in the decoded message (the raw value doesn't change).
				idx = hdr.indexOf("TIME=");
				if (idx == -1)
				{
					throw new HeaderParseException("No TIME in Iridium header");
				}
				s = hdr.substring(idx+5, idx+5+11);
				try
				{
					Date d = goesDateFormat.parse(s);
					msg.setPM(GoesPMParser.MESSAGE_TIME, new Variable(d));
				}
				catch (Exception ex)
				{
					throw new HeaderParseException("Cannot parse time from Iridium header '" + s + "'", ex);
				}

				idx = hdr.indexOf("STAT=");
				if (idx != -1)
				{
					int comma = hdr.indexOf(',', idx);
					try
					{
						int sessionStat = Integer.parseInt(hdr.substring(idx+5, comma));
						if (sessionStat == 0 || sessionStat == 1 || sessionStat == 2)
						{
							msg.setPM(GoesPMParser.FAILURE_CODE, new Variable('G'));
						}
					}
					catch (Exception ex)
					{
						logger.atWarn()
						  	  .setCause(ex)
						  	  .log("Bad STAT field in Iridium header '{}'",hdr);
					}
				}
			}

			// Process Location info if it is present.
			int idx = hdr.indexOf("LAT=");
			if (idx != -1)
			{
				int comma = hdr.indexOf(',', idx);
				try
				{
					msg.setPM(LATITUDE, new Variable(Double.parseDouble(hdr.substring(idx+4, comma))));
				}
				catch (Exception ex)
				{
					logger.atWarn()
						  .setCause(ex)
						  .log("Bad LAT field in Iridium header '{}'",hdr);
				}
			}
			idx = hdr.indexOf("LON=");
			if (idx != -1)
			{
				int comma = hdr.indexOf(',', idx);
				try
				{
					msg.setPM(LONGITUDE, new Variable(Double.parseDouble(hdr.substring(idx+4, comma))));
				}
				catch (Exception ex)
				{
					logger.atWarn()
						  .setCause(ex)
						  .log("Bad LON field in Iridium header '{}'", hdr);
				}
			}
			idx = hdr.indexOf("RAD=");
			if (idx != -1)
			{
				int comma = hdr.indexOf(',', idx);
				try
				{
					msg.setPM(CEP_RADIUS, new Variable(Double.parseDouble(hdr.substring(idx+4, comma))));
				}
				catch(Exception ex)
				{
					logger.atWarn()
						  .setCause(ex)
						  .log("Bad RAD field in Iridium header '{}'", hdr);
				}
			}

			idx = hdr.indexOf("PLEN=");
			if (idx != -1)
			{
				// Currently this is the last header so there's no comma after, but there is a space
				int comma = hdr.indexOf(' ', idx);
				try
				{
					msg.setPM(PAYLOAD_LEN, new Variable(Integer.parseInt(hdr.substring(idx+5, comma))));
				}
				catch (Exception ex)
				{
					logger.atWarn()
						.setCause(ex)
						.log("Bad PLEN field in Iridium header '{}'", hdr);
				}
			}

			// If iridiumIEInPayload is true, this length does not include the IE:02 portion;
			// if false, the IE:02 part is removed, and this really is the length of the header.
			idx = hdr.indexOf(" ");
			if (!hdr.substring(idx+1, idx+6).equals("IE:02"))
			{
				idx++;	// include the ending space in the header portion
			}

			if (idx < 75) idx = 75;
			msg.setHeaderLength(idx);
			headerLength = idx;
		}
		catch (Throwable ex)
		{
			throw new HeaderParseException("Error parsing header '" + hdr + "'", ex);
		}
	}

	public boolean containsExplicitLength()
	{
		return false;
	}

	/** @return 75, the length of a ASCII-ized Iridium header. */
	public int getHeaderLength()
	{
		return headerLength;
	}

	/** @return "iridium" */
	public String getHeaderType()
	{
		return "iridium";
	}

	/** @return the medium type constant for GOES DCP messages. */
	public String getMediumType()
	{
		return Constants.medium_IRIDIUM;
	}

	public SimpleDateFormat getGoesDateFormat()
	{
		return goesDateFormat;
	}
}
