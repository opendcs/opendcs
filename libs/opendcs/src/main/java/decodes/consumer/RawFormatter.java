/*
*  $Id$
*/
package decodes.consumer;

import java.util.Properties;
import ilex.util.PropertiesUtil;
import ilex.util.TextUtil;
import decodes.db.*;
import decodes.decoder.DecodedMessage;
import decodes.datasource.RawMessage;

/**
  This class outputs the raw message data.
*/
public class RawFormatter extends OutputFormatter
{
	/** True if property noStatusMessages is true. */
	private boolean noStatusMessages = false;
	
	/** default constructor */
	public RawFormatter()
	{
		super();
	}

	/**
	  Initializes the Formatter. This method is called from the static
	  makeOutputFormatter method in this class. The RoutingSpec does not
	  need to call it explicitly.
	  @param type the type of this output formatter.
	  @param tz the time zone as specified in the routing spec.
	  @param presGrp The presentation group to handle rounding &amp; EU conversions.
	  @param rsProps the routing-spec properties.
	*/
	protected void initFormatter(String type, java.util.TimeZone tz,
		PresentationGroup presGrp, Properties rsProps)
		throws OutputFormatterException
	{
		String p = PropertiesUtil.getIgnoreCase(rsProps, "noStatusMessages");
		if (p != null)
			noStatusMessages = TextUtil.str2boolean(p);
	}

	/** Prints the file trailer. */
	public void shutdown()
	{
	}

	/**
	  Writes the passed DecodedMessage to the passed consumer, using
	  a concrete format.
	  @param msg The message to output.
	  @param consumer The DataConsumer to output to.
	  @throws OutputFormatterException if there was a problem formatting data.
	  @throws DataConsumerException, passed through from consumer methods.
	*/
	public void formatMessage(DecodedMessage msg, DataConsumer consumer)
		throws DataConsumerException, OutputFormatterException
	{
		consumer.startMessage(msg);
		RawMessage rawmsg = msg.getRawMessage();
		if (rawmsg != null)
		{
			String rm = rawmsg.toString();
			consumer.println(rm);
		}
		consumer.endMessage();
	}

	/** 
	 * Raw output can contain all manner of DCP messages.
	 */
	public boolean acceptRealDcpMessagesOnly() { return noStatusMessages; }

	/**
	 * Raw output doesn't require a successful decode.
	 */
	public boolean requiresDecodedMessage() { return false; }

	/**
	 * Raw output doesn't need or want decode.
	 */
	public boolean attemptDecode() { return false; }
	
	@Override
	public boolean usesTZ() { return false; }

}

