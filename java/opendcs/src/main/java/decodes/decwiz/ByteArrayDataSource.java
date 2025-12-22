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
package decodes.decwiz;

import java.util.Date;
import java.util.Properties;
import java.util.Vector;

import org.opendcs.utils.logging.OpenDcsLoggerFactory;
import org.slf4j.Logger;

import ilex.var.Variable;

import decodes.datasource.*;
import decodes.db.*;

public class ByteArrayDataSource extends DataSourceExec
{
	private static final String CANNOT_PARSE_AS_EITHER_USGS_EDL_OR_GOES_MSG =
						"Cannot parse as either USGS-EDL or GOES msg.";
	private static final String UNEXPECTED_ERROR_PARSING_HEADER = "Unexpected error parsing header.";
	private static final Logger log = OpenDcsLoggerFactory.getLogger();
	byte[] bytes;
	PMParser pmParser = null;
	RawMessage rawMessage = null;

	/**
	 * The byte array DataSource is not used by the general runtime
	 * and does not need the more specific DataSourceExec constructor.
	 * as it never uses the DataSource or Database null is always passed to
	 * super()
	 * @param bytes
	 */
	public ByteArrayDataSource(byte[] bytes)
	{
		super(null,null);
		this.bytes = bytes;
		pmParser = null;
		allowNullPlatform = false;
		allowDapsStatusMessages = false;
	}

	/** @return name of this data source, as defined in DECODES DB. */
	public String getName()
	{
		return "ByteArrayDataSource";
	}

	/** 
	  Return currently active source. Groups should override this to delegate
	  to the active member.
	  @return currently active source.
	*/
	public String getActiveSource()
	{
		return getName();
	}

	/**
	 * Does nothing.
	 */
	public void processDataSource() {}

	/**
	 * Does nothing.
	 */
	public void init(Properties routingSpecProps, String since, 
		String until, Vector networkLists)
	{
	}

	/**
	 * Does nothing.
	*/
	public void close() {}

	/**
	  Reads the next raw message from the data source and returns it.
	  This DataSource will fill in the message data and attempt to 
	  associate it with a TransportMedium object.

	  @return the next RawMessage object from the data source.

	  @throws DataSourceTimeoutException if the data source is still
	  waiting for a message and the timeout (as defined in the properties
	  when init was called) has expired.
	  @throws DataSourceException if some other problem arises.
	*/
	protected RawMessage getSourceRawMessage()
		throws DataSourceException
	{
		rawMessage = new RawMessage(bytes, bytes.length);
		return rawMessage;
	}
	

	public void associatePlatform()
		throws DataSourceException
	{
		Platform plat = null;
		TransportMedium tm = null;
		try
		{ tm = rawMessage.getTransportMedium();}
		catch ( UnknownPlatformException ue ) { tm = null; };
		try 
		{
			if ( tm == null ) { 	
				plat = Database.getDb().platformList.getPlatform(
					pmParser.getMediumType(), rawMessage.getMediumId(), 
					rawMessage.getTimeStamp());
			} else {
				plat = Database.getDb().platformList.getPlatform(
					tm.getMediumType(), tm.getMediumId(), 
					rawMessage.getTimeStamp());
			}
		}
		catch(DatabaseException ex)
		{
			String msg = "Cannot read platform for medium type '"
				+ pmParser.getMediumType() + "' with ID '"
				+ rawMessage.getMediumId();
			throw new DataSourceException(msg,ex);
		}

		if (plat == null)
		{
			String msg = "Cannot determine platform for medium type '"
				+ pmParser.getMediumType() + "' with ID '"
				+ rawMessage.getMediumId() + "'.";
			throw new UnknownPlatformException(msg);
		}

		rawMessage.setPlatform(plat);
		tm = resolveTransportMedium(plat, 
			rawMessage.getMediumId(), -1, false);
		rawMessage.setTransportMedium(tm);
	}
			
	public void parseHeader()
		throws HeaderParseException
	{
		try
		{
			pmParser = PMParser.getPMParser(Constants.medium_EDL);
			pmParser.parsePerformanceMeasurements(rawMessage);
			return;
		}
		catch(HeaderParseException ex)
		{
			log.atWarn().setCause(ex).log("Cannot parse as USGS-EDL.");
		}

		try
		{
			pmParser = PMParser.getPMParser(Constants.medium_Goes);
			pmParser.parsePerformanceMeasurements(rawMessage);
			Variable datevar = rawMessage.getPM(GoesPMParser.MESSAGE_TIME);
			Date msgTime = datevar.getDateValue();
			rawMessage.setTimeStamp(msgTime);
		}
		catch(HeaderParseException ex)
		{
			String msg = CANNOT_PARSE_AS_EITHER_USGS_EDL_OR_GOES_MSG;
			pmParser = null;
			throw new HeaderParseException(msg,ex);
		}
		catch(Exception ex)
		{
			String msg = UNEXPECTED_ERROR_PARSING_HEADER;
			throw new HeaderParseException(msg,ex);
		}
	}
	
	public void parseHeader(String mediumType)
		throws HeaderParseException
	{
		try
		{
			pmParser = PMParser.getPMParser(mediumType);
			pmParser.parsePerformanceMeasurements(rawMessage);
			return;
		}
		catch(HeaderParseException ex)
		{
			log.atWarn().setCause(ex).log("Cannot parse as USGS-EDL types.");
		}

		try
		{
			pmParser = PMParser.getPMParser(Constants.medium_Goes);
			pmParser.parsePerformanceMeasurements(rawMessage);
			Variable datevar = rawMessage.getPM(GoesPMParser.MESSAGE_TIME);
			Date msgTime = datevar.getDateValue();
			rawMessage.setTimeStamp(msgTime);
		}
		catch(HeaderParseException ex)
		{
			String msg = CANNOT_PARSE_AS_EITHER_USGS_EDL_OR_GOES_MSG;
			pmParser = null;
			throw new HeaderParseException(msg,ex);
		}
		catch(Exception ex)
		{
			String msg = UNEXPECTED_ERROR_PARSING_HEADER;
			throw new HeaderParseException(msg,ex);
		}
	}
}
