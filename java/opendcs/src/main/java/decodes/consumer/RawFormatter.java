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

