/*
*  $Id$
*/
package decodes.datasource;

import java.util.Date;
import ilex.var.Variable;

import decodes.db.Constants;

/**
  Concrete subclass of PMParser for parsing performance measurements
  out of a file with no header. Sets header length to 0 and message length
  to the endire raw message.
  Used when parsing files and medium ID has been given via some other means.
  Also sets the MESSAGE_TIME to current time.
*/
public class NoHeaderPMParser extends PMParser
{
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
		return "NoHeader";
	}

	/** @return the medium type constant for 'other' messages. */
	public String getMediumType()
	{
		return Constants.medium_Other;
	}
}

