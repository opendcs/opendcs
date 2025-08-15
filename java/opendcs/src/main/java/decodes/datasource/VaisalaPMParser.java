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


import org.opendcs.utils.logging.OpenDcsLoggerFactory;
import org.slf4j.Logger;

import decodes.db.Constants;

/**
  Concrete subclass of PMParser for parsing performance measurements
  out of GOES DCP messages.
*/
public class VaisalaPMParser extends GoesPMParser
{
	private static final Logger log = OpenDcsLoggerFactory.getLogger();
	/** default constructor */
	public VaisalaPMParser()
	{
		super();
	}

	/**
	  Vaisala DRGS puts a platform description on the header line,
	  immediately following the 37-byte DOMSAT header.
	  We need to set the headerLength variable in 'msg' to point to
	  just past this added description.

	  @param msg The message to parse.
	*/
	public void parsePerformanceMeasurements(RawMessage msg)
		throws HeaderParseException
	{
		// Super class handles DOMSAT header, sets medium ID & length.
		super.parsePerformanceMeasurements(msg);

		byte data[] = msg.getData();
		int i = 0;
		for(; i<data.length && data[i] != (byte)'\n'; i++);

		if (i < data.length && data[i] == '\n')
		{
			log.trace("Setting header length to {}", i);
			msg.setHeaderLength(i);
		}
	}

	/** @return "vaisala" */
	public String getHeaderType()
	{
		return "vaisala";
	}

	/** @return constant medium type for this type of header. */
	public String getMediumType()
	{
		return Constants.medium_Goes;
	}
}
