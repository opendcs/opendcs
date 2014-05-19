/*
*  $Id$
*
*  $Log$
*  Revision 1.1  2008/04/04 18:20:59  cvs
*  Added legacy code to repository
*
*  Revision 1.3  2004/08/24 21:01:37  mjmaloney
*  added javadocs
*
*  Revision 1.2  2004/02/29 20:48:52  mjmaloney
*  Working implementation of DCP Monitor Server
*
*  Revision 1.1  2004/02/19 01:46:43  mjmaloney
*  Added NullFormatter to this package. Used by DCP Monitor app.
*
*/
package decodes.consumer;

import java.util.Iterator;
import java.util.Properties;
import java.util.Date;
import java.util.Calendar;
import java.text.SimpleDateFormat;

import ilex.var.TimedVariable;
import ilex.var.IFlags;

import decodes.db.*;
import decodes.decoder.DecodedMessage;
import decodes.decoder.TimeSeries;
import decodes.decoder.Sensor;
import decodes.datasource.RawMessage;
import decodes.datasource.UnknownPlatformException;

/**
  NullFormatter is used for consumers that don't want formatted
  data. Rather, they take the DecodedMessage structure in the
  startMessage call directly. The formatter essentially does nothing.
*/
public class NullFormatter extends OutputFormatter
{
	/** default constructor */
	protected NullFormatter()
	{
		super();
	}

	/** The NullFormatter requires no initialization and ignores all args.  */
	protected void init(String type, java.util.TimeZone tz,
		PresentationGroup presGrp, Properties rsProps)
		throws OutputFormatterException
	{
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
	public void writeMessage(DecodedMessage msg, DataConsumer consumer)
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
	public boolean usesTimeZone() { return false; }


}

