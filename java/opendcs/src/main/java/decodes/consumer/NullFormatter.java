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

import org.opendcs.utils.logging.OpenDcsLoggerFactory;
import org.slf4j.Logger;

import decodes.db.*;
import decodes.decoder.DecodedMessage;

/**
  NullFormatter is used for consumers that don't want formatted
  data. Rather, they take the DecodedMessage structure in the
  startMessage call directly. The formatter essentially does nothing.
*/
public class NullFormatter extends OutputFormatter
{
	private static final Logger log = OpenDcsLoggerFactory.getLogger();
	/** default constructor */
	public NullFormatter()
	{
		super();
	}

	/** The NullFormatter requires no initialization and ignores all args.  */
	protected void initFormatter(String type, java.util.TimeZone tz,
		PresentationGroup presGrp, Properties rsProps)
		throws OutputFormatterException
	{
		log.info("NullFormatter initializing.");
	}

	/** Does nothing. */
	public void shutdown()
	{
	}

	/**
	  Passes the DecodedMessage to the passed consumer via
	  the startMessage call. No formatting is done.

	  @param msg The message to output.
	  @param consumer The DataConsumer to output to.
	  @throws DataConsumerException, passed through from consumer methods.
	*/
	public void formatMessage(DecodedMessage msg, DataConsumer consumer)
		throws DataConsumerException, OutputFormatterException
	{
		consumer.startMessage(msg);
		consumer.endMessage();
	}

	/**
	  NullFormatter makes no assumptions about what kind of message is
	  requred.
	  @return false to allow any type of message to be processed.
	*/
	public boolean acceptRealDcpMessagesOnly() { return false; }

	/**
	  NullFormatter allows un-decoded, raw messages to pass.
	  @return false to allow un-decoded messages to pass.
	*/
	public boolean requiresDecodedMessage() { return false; }

	@Override
	public boolean usesTZ() { return false; }


}
