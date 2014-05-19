
/*
*  $Id$
*/
package decodes.consumer;

import ilex.util.PropertiesUtil;
import ilex.util.TextUtil;

import java.util.Properties;

import decodes.datasource.RawMessage;
import decodes.db.PresentationGroup;
import decodes.decoder.DecodedMessage;
import decodes.util.PropertySpec;

/**
  This class outputs only the header of the message
*/
public class HeaderFormatter extends OutputFormatter
{
	/** True if property noStatusMessages is true. */
	private boolean noStatusMessages = false;
	
	private PropertySpec propSpecs[] = 
	{		
		new PropertySpec("noStatusMessages", PropertySpec.BOOLEAN,
				"True to ignore DAPS status messages (default=false)")
	};

	/** default constructor */
	public HeaderFormatter()
	{
		super();
	}

	/**
	  Initializes the Formatter. This method is called from the static
	  makeOutputFormatter method in this class. The RoutingSpec does not
	  need to call it explicitely.
	  @param type the type of this output formatter.
	  @param tz the time zone as specified in the routing spec.
	  @param presGrp The presentation group to handle rounding & EU conversions.
	  @param rsProps the routing-spec properties.
	*/
	protected void init(String type, java.util.TimeZone tz,
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
	public void writeMessage(DecodedMessage msg, DataConsumer consumer)
		throws DataConsumerException, OutputFormatterException
	{
		consumer.startMessage(msg);
		RawMessage rawmsg = msg.getRawMessage();
		if (rawmsg != null)
		{			
			byte[] rm = rawmsg.getHeader();			
			String rr = new String(rm);		
			consumer.println(rr);
			
			
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
	public boolean usesTimeZone() { return false; }

	@Override
	public PropertySpec[] getSupportedProps()
	{
		return propSpecs;
	}

}


