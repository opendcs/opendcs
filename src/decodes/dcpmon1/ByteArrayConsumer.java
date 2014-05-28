package decodes.dcpmon1;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.util.Properties;

import decodes.consumer.DataConsumer;
import decodes.consumer.DataConsumerException;
import decodes.decoder.DecodedMessage;

/**
 Used by the Dcp Monitor to decode data.
 Stores results in a ByteArrayOutputStream internally.
*/
public class ByteArrayConsumer extends DataConsumer
{
	ByteArrayOutputStream os;

	/**
	  Constructs new object with a specified ByteArrayOutputStream.
	*/
	public ByteArrayConsumer()
	{
		super();
		os = null;
	}

	/**
	  Opens and initializes the consumer.
	  @param consumerArg ignored
	  @param props ignored
	  @throws DataConsumerException if the consumer could not be initialized.
	*/
	public void open(String consumerArg, Properties props)
		throws DataConsumerException
	{
	}

	/** Does nothing. */
	public void close()
	{
	}

	/** 
	  Initialize the OutputStream
	  @param msg ignored.
	*/
	public void startMessage(DecodedMessage msg)
	{
		os = new ByteArrayOutputStream();
	}

	/**
	  @param line the line
	*/
	public void printLine(String line)
	{

	}

	/** Does nothing. */
	public void endMessage()
	{
		// does not need to delimit the end of a message.
	}

	public OutputStream getOutputStream()
	{
		return os;
	}

	/** @return "StringBuffer" */
	public String getActiveOutput() { return "OSConsumer"; }
}
