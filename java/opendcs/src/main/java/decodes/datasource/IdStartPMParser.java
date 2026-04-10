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
import java.util.Properties;
import java.util.StringTokenizer;

import org.opendcs.utils.logging.OpenDcsLoggerFactory;
import org.slf4j.Logger;

import ilex.util.AsciiUtil;
import ilex.util.PropertiesUtil;
import ilex.var.Variable;
import decodes.db.Constants;

/**
 * A simple PM Parser for various types of files downloaded from the internet.
 * Assume that message starts with space or comma-delimited transport medium ID.
 * Assume that header ends at first LF.
 * The message time is always set to the current time, so the file's data must
 * contain date/time info.
 * Medium type will be "other".
 * <ul>
 *   <li>mediumType: default="other"</li>
 *   <li>delims: escaped string containing possible delimiters default = space or comma</li>
 *   <li>headerEnd: escaped string containing header end string, default = LF</li>
 * </ul>
*/
public class IdStartPMParser extends PMParser
{
	private static final Logger log = OpenDcsLoggerFactory.getLogger();
	public String mediumType = Constants.medium_Other;
	public String delims = " ,";
	public String headerEnd = "\n";
	public int hdrLength = 0;
	
	/** default constructor */
	public IdStartPMParser()
	{
	}

	public void setProperties(Properties props)
	{
		PropertiesUtil.loadFromProps(this, props);
		String s = PropertiesUtil.getIgnoreCase(props, "delims");
		if (s != null)
			delims = new String(AsciiUtil.ascii2bin(s));
		s = PropertiesUtil.getIgnoreCase(props, "headerEnd");
		if (s != null)
			headerEnd = new String(AsciiUtil.ascii2bin(s));
	}

	/**
	  Parses the DOMSAT header.
	  Sets the mediumID to the GOES DCP Address.
	  @param msg the message to parse.
	*/
	public void parsePerformanceMeasurements(RawMessage msg)
		throws HeaderParseException
	{
		String data = new String(msg.getData());
		
		// Get the ID
		StringTokenizer st = new StringTokenizer(data, delims);
		
		String id = st.nextToken().trim();
		if (id == null)
			throw new HeaderParseException("No ID present");
		
		msg.setMediumId(id);
		msg.setPM(GoesPMParser.DCP_ADDRESS, new Variable(id));
		msg.setPM(EdlPMParser.STATION, new Variable(id));

		Date now = new Date();
		msg.setPM(GoesPMParser.MESSAGE_TIME, new Variable(now));
			msg.setTimeStamp(now);
		
		// Set header length by finding the end of header.
		hdrLength = data.indexOf(headerEnd);
		if (hdrLength == -1)
			hdrLength = id.length();
		else
			hdrLength += headerEnd.length();
		msg.setHeaderLength(hdrLength);
		
		msg.setPM(GoesPMParser.MESSAGE_LENGTH, new Variable(data.length()));
		
		msg.setPM(GoesPMParser.FAILURE_CODE, new Variable('G'));
		log.trace("IdStartPMParser: mediumId='{}', hdrLength={}", id, hdrLength);
	}

	/** @return length as determined by the datacol. */
	public int getHeaderLength()
	{
		return hdrLength;
	}

	/** @return "csv" */
	public String getHeaderType()
	{
		return "idstart";
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

