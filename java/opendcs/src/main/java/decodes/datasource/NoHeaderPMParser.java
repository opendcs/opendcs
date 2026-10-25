/*
*  $Id$
*/
package decodes.datasource;

import java.util.Date;

import com.google.auto.service.AutoService;

import ilex.var.Variable;

import decodes.db.Constants;

/**
  Concrete subclass of PMParser for parsing performance measurements
  out of a file with no header. Sets header length to 0 and message length
  to the entire raw message.
  Used when parsing files and medium ID has been given via some other means.
  Also sets the MESSAGE_TIME to current time.
*/
public class NoHeaderPMParser implements PMParser
{
	public static final String HEADER_TYPE = "noHeader";
	public static final String HEADER_TYPE_OTHER = "other";

	/** default constructor */
	public NoHeaderPMParser()
	{
	}

	/**
	  Parses the DOMSAT header.
	  Sets the mediumID to the GOES DCP Address.
	  @param msg the message to parse.
	*/
	public void parsePerformanceMeasurements(RawMessage msg)
		throws HeaderParseException
	{
		byte data[] = msg.getData();
		msg.setHeaderLength(0);

		msg.setPM(GoesPMParser.MESSAGE_LENGTH, new Variable(data.length));
		msg.setPM(GoesPMParser.MESSAGE_TIME, new Variable(new Date()));
	}

	/** @return 0. */
	public int getHeaderLength()
	{
		return 0;
	}

	/** @return "NoHeader" */
	public String getHeaderType()
	{
		return HEADER_TYPE;
	}

	/** @return the medium type constant for 'other' messages. */
	public String getMediumType()
	{
		return Constants.medium_Other;
	}

	@AutoService(PMParser.Provider.class)
	public static final class Provider implements PMParser.Provider
	{
		@Override
		public PMParser create(String headerType)
		{
			return HEADER_TYPE.equalsIgnoreCase(headerType) || HEADER_TYPE_OTHER.equalsIgnoreCase(headerType)
				 ? new NoHeaderPMParser()
				 : null;
		}
	}
}

