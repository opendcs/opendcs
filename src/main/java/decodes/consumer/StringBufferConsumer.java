/*
*  $Id$
*
*  $State$
*
*  $Log$
*  Revision 1.2  2014/05/28 13:09:27  mmaloney
*  dev
*
*  Revision 1.1.1.1  2014/05/19 15:28:59  mmaloney
*  OPENDCS 6.0 Initial Checkin
*
*  Revision 1.2  2008/11/20 18:49:17  mjmaloney
*  merge from usgs mods
*
*  Revision 1.1  2008/04/04 18:20:59  cvs
*  Added legacy code to repository
*
*  Revision 1.4  2004/08/24 21:01:38  mjmaloney
*  added javadocs
*
*  Revision 1.3  2004/04/15 19:47:48  mjmaloney
*  Added status methods to support the routng status monitor web app.
*
*  Revision 1.2  2001/10/26 00:17:22  mike
*  Added getBuffer method.
*
*  Revision 1.1  2001/10/26 00:14:52  mike
*  Added StringBufferConsumer
*
*/
package decodes.consumer;

import java.io.PrintStream;

import java.util.Properties;
import decodes.datasource.RawMessage;
import decodes.decoder.DecodedMessage;
import decodes.db.*;

/**
  StringBufferConsumer writes data to a StringBuffer. It is used by GUI
  applications that want to display data in GUI components.
*/
public class StringBufferConsumer extends DataConsumer
{
	/** The string buffer */
	StringBuffer sb;
	/** The line separator to use */
	String lineSep;

	/**
	  Constructs new object with a specified StringBuffer.
	*/
	public StringBufferConsumer(StringBuffer sb)
	{
		super();
		this.sb = sb;
		lineSep = System.getProperty("line.separator");
		if (lineSep == null)
			lineSep = "\n";
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
		sb.delete(0, sb.length());
	}

	/** Does nothing. */
	public void close()
	{
	}

	/** 
	  Zeros the string buffer.
	  @param msg ignored.
	*/
	public void startMessage(DecodedMessage msg)
	{
		sb.delete(0, sb.length());
	}

	/**
	  Appends a line to the buffer.
	  @param line the line
	*/
	public void println(String line)
	{
		sb.append(line);
		sb.append(lineSep);
	}

	/** Does nothing. */
	public void endMessage()
	{
		// does not need to delimit the end of a message.
	}

	/**
	  @return the StringBuffer.
	*/
	public StringBuffer getBuffer() { return sb; }

	/** @return "StringBuffer" */
	public String getActiveOutput() { return "StringBuffer"; }
	
	@Override
	public String getArgLabel()
	{
		return null;
	}

}

