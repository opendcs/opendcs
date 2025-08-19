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
import java.util.TimeZone;
import java.text.SimpleDateFormat;

import org.opendcs.utils.logging.OpenDcsLoggerFactory;
import org.slf4j.Logger;

import ilex.util.ByteUtil;
import ilex.var.Variable;

import decodes.db.Constants;

/**
  Concrete subclass of PMParser for parsing performance measurements
  out of a message in an EUMETSAT downloaded file.
  The format is (supposed to be) documented at:
  http://www.eumetsat.int/Home/Main/News/ProductServiceNews/813504?l=en
  But there are differences between the actual data and that described
  in the documents.
  
  In parsing the file, the 8-byte operator ID 
  must be used for the start-delimiter. That means we see the ASCII header
  with these eight bytes already removed.
  
  The ascii header is 86 bytes minus 8 bytes because the 8-byte OPERATOR_ID
  is gobbled by the FileDataSource as the start delimiter.
  It mostly matches the documentation except that the final 2 bytes we see
  are "/n", documentation says it should be <cr><lf>
  
  After this the 31-byte binary header is very different from the documentation.

  After this, according to the documentation, is a 2-byte binary message
  length and a 4-byte binary dcp-address. The data we see does not agree
  with these same fields in the ascii header so we skip them.
  
  Also after the DCP address we always see a single null byte which we discard.
  
  Thus the total header length is:
     78 ASCII Header (86 - 8)
     31 Binary Header
      6 Bogus length and address fields
      1 Null byte
    ---
    116 Bytes
  
*/
public class EumetsatPMParser extends PMParser
{
	private static final Logger log = OpenDcsLoggerFactory.getLogger();
	public static final String module = "EumetsatPMParser";
	private static SimpleDateFormat eumetsatDateFormat = null;

	/** default constructor */
	public EumetsatPMParser()
	{
		if (eumetsatDateFormat == null)
		{
			eumetsatDateFormat = new SimpleDateFormat("dd/MM/yy HH:mm:ss");
			eumetsatDateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
		}
	}

	/**
	 *  Parses the EUMETSAT headers. See notes in class description above.
	 *  @param msg the message to parse.
	*/
	public void parsePerformanceMeasurements(RawMessage msg)
		throws HeaderParseException
	{
		byte data[] = msg.getData();
		msg.setHeaderLength(116); // See above

		log.trace("starting, data.length={}", data.length);
		
		if (data == null || data.length < 116)
			throw new HeaderParseException(
				"Header to short, 116 bytes is required");

		String sn = new String(data, 1, 16).trim();
		msg.setPM(GoesPMParser.SITE_NAME, new Variable(sn));
		log.trace("Found SITE_NAME='{}'", sn);

		String dcpaddr = new String(data, 19, 8);
		for(int i=0; i<8; i++)
			if (ByteUtil.fromHexChar(dcpaddr.charAt(i)) == -1)
				throw new HeaderParseException("Invalid EUMETSAT DCP Address");
		msg.setPM(GoesPMParser.DCP_ADDRESS, new Variable(dcpaddr));
		msg.setMediumId(dcpaddr);
		log.trace("Found DCPADDR='{}'", dcpaddr);

		String datefield = new String(data, 47, 17);
		try
		{
			Date d = eumetsatDateFormat.parse(datefield);
			if (d == null)
				throw new HeaderParseException("Invalid timestamp field '"
					+ datefield + "'");
			msg.setPM(GoesPMParser.MESSAGE_TIME, new Variable(d));
			msg.setTimeStamp(d);
			log.trace("MSG TIME='{}' datefield='{}'", d, datefield);
		}
		catch(Exception ex)
		{
			throw new HeaderParseException("Invalid timestamp field '"
				+ datefield + "'");
		}

		String lenfield = new String(data, 69, 3);
		try 
		{
			long len = Long.parseLong(lenfield);
			// the length in the header contains the 6-byte bogus addr & len field in
			// the binary header (see above). So subtract this.
			len -= 6;
			msg.setPM(GoesPMParser.MESSAGE_LENGTH, new Variable(len));
		}
		catch(NumberFormatException ex)
		{
			throw new HeaderParseException("Invalid length field '" + lenfield + '"',ex);
		}

		/* Now parse the binary header. */
		msg.setPM("SubSystemId", new Variable((long)data[78]));
		msg.setPM("ModuleId", new Variable((long)data[79]));
		msg.setPM("ReceiverId", new Variable((long)data[80]));
		long chanType = (long)data[81];
		msg.setPM("ChannelType", new Variable(chanType));
		if (chanType == 0 || chanType == 1 || chanType == 2)
			msg.setPM(GoesPMParser.BAUD, new Variable(100));
		
		log.trace("chanType={}", chanType);

		// Doc says this is channel frequency. But we see small integers
//		long chan = (long)ByteUtil.getInt4_BigEndian(data, 82);
//		msg.setPM(GoesPMParser.CHANNEL, new Variable(chan));
		// NO! Don't put channel in otherwise DataSourceExec.resolveTransportMedium
		// will think this is a GOES message and fail to find the TM.

		// The rest of the info in the binary header is unreliable and/or
		// does not match the documentation.

		msg.setPM(GoesPMParser.FAILURE_CODE, new Variable('G'));

		msg.setPM(GoesPMParser.SPACECRAFT, new Variable('M')); // M=meteosat
		
		log.trace("done");
	}

	/** @return 116. See notes above in class description. */
	public int getHeaderLength()
	{
		return 116;
	}

	/** @return "eumetsat" */
	public String getHeaderType()
	{
		return "eumetsat";
	}

	/** @return the medium type constant for EUMETSAT DCP messages. */
	public String getMediumType()
	{
		return Constants.medium_Eumetsat;
	}

	/**
	 * After parsing the header we know the exact length of the
	 * message.
	 * @return true
	 */
	public boolean containsExplicitLength()
	{
		return true;
	}
}

