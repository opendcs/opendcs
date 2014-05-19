/*
*  $Id$
*
*  $State$
*
*  $Log$
*  Revision 1.5  2012/10/08 18:23:25  mmaloney
*  Hold routingSpecThread
*
*  Revision 1.4  2009/08/12 19:46:13  mjmaloney
*  usgs merge
*
*  Revision 1.1  2009/02/09 14:36:43  ddschwit
*  First Entry
*
*  Revision 1.11  2009/01/22 17:35:50  satin
*  Added the capability to the Directory consumer to append messages  to a
*  file instead of creating a new file.  ( This capability was already
*  in the FileAppend consumer. )  This is to handle messages that have data
*  for multiple sites.
*
*  Revision 1.10  2008/10/05 12:18:54  satin
*  Added method startMessageAppend to allow some formatters to append
*  messages.  The default is to invoke startMessage so consumers that
*  allow this must override this method.
*
*  Revision 1.9  2008/08/14 22:35:05  satin
*  Added method endmessage(String dbno) to allow formatters to specify
*  a database number to be used in constructing database pathnames.
*  (USGS specific).
*
*  Revision 1.8  2007/10/10 19:38:27  mmaloney
*  added timezone - needed for the ExcelConsumer class
*
*  Revision 1.7  2005/04/25 21:38:07  mjmaloney
*  dev
*
*  Revision 1.6  2005/03/15 16:51:59  mjmaloney
*  Rename Enum to DbEnum for Java 5 compatibility
*
*  Revision 1.5  2005/03/15 16:11:25  mjmaloney
*  Modify 'Enum' for Java 5 compat.
*
*  Revision 1.4  2004/08/24 21:01:35  mjmaloney
*  added javadocs
*
*  Revision 1.3  2004/04/15 19:47:47  mjmaloney
*  Added status methods to support the routng status monitor web app.
*
*  Revision 1.2  2001/09/14 21:16:42  mike
*  dev
*
*  Revision 1.1  2001/09/09 17:39:42  mike
*  Created.
*
*/
package decodes.consumer;

import java.util.Properties;
import java.util.TimeZone;
import java.io.OutputStream;
import decodes.datasource.RawMessage;
import decodes.decoder.DecodedMessage;
import decodes.db.*;
import decodes.routing.RoutingSpecThread;

/**
  This abstract class defines the interface for all DECODES data consumers.
*/
public abstract class DataConsumer
{
	protected TimeZone tz;
	
	protected RoutingSpecThread routingSpecThread = null;
	
	/**
	* All subclasses must implement a no-arguments constructor because
	* the makeOutputFormatter factory method instantiates the object
	* dynamically from the named Class.
	*/
	protected DataConsumer() 
	{
		tz = null;
	}

	public void setTimeZone(TimeZone tz)
	{
		this.tz = tz;
	}
	
	/** 
	  Opens and initializes the consumer.
	  @param consumerArg argument to the concrete consumer.
	  @param props Properties to be used by the consumer.
	  @throws DataConsumerException if the consumer could not be initialized.
	*/
	public abstract void open(String consumerArg, Properties props)
		throws DataConsumerException;

	/**
	  Closes the data consumer.
	  This method is called by the routing specification when the data
	  consumer is no longer needed.
	*/
	public abstract void close();

	/**
	  @return a descriptive name of current output, suitable for use in
	  a status display.
	*/
	public String getActiveOutput()
	{
		return "";
	}

	/**
	  Called when a new message is ready for output, but before any formatting
	  has been done.

	  @param msg the message to be output.
	  @throws DataConsumerException if an error occurs.
	*/
	public abstract void startMessage(DecodedMessage msg)
		throws DataConsumerException;

	public void startMessageAppend(DecodedMessage msg)
		throws DataConsumerException
	{
			try {
				startMessage(msg);
			} catch ( DataConsumerException dce ) {
				throw ( dce );
			}
	}		
	/*
	  Called from the formatter to output a single line to the consumer.
	  @param line the line of text to be output, not including any line
	  terminator.
	*/
	public abstract void println(String line);

	/** Called after all lines have been output by the formatter. */
	public abstract void endMessage();

	/** Called after all lines have been output by the formatter -stdmsg. */
	public void endMessage(String dbno)
	{
			endMessage();
			return;
	}
	/** 
	  Some formatters need to do their own formatting via an output stream.
	  Default implementation throws DataConsumerException.
	*/
	public OutputStream getOutputStream()
		throws DataConsumerException
	{
		throw new DataConsumerException(
			"OutputStream not supported for this consumer type.");
	}

	/**
	  Factory method to make a concrete DataConsumer object.
	  @param type the enumeration value specifying type of consumer.
	  @return a concrete DataConsumer object of the specified type.
	*/
	public static DataConsumer makeDataConsumer(String type)
		throws DataConsumerException
	{
		// Get the Data Consumer Type Enumeration from the database
		Database db = Database.getDb();
		decodes.db.DbEnum types = db.getDbEnum("DataConsumer");
		if (types == null)
			throw new DataConsumerException(
				"No DataConsumer ENUM found in this database!");

		// Lookup the value corresponding to this object's type.
		EnumValue myType = types.findEnumValue(type);
		if (myType == null)
			throw new DataConsumerException(
				"No consumer type found matching '" + type+"'");

		// Instantiate a concrete data source to delegate to.
		DataConsumer ret;
		try
		{
			Class myClass = myType.getExecClass();
			ret = (DataConsumer)myClass.newInstance();
		}
		catch(Exception e)
		{
			throw new DataConsumerException(
				"Cannot instantiate DataConsumer of type '" + type
				+ "': " + e.toString());
		}
		return ret;
	}

	public void setRoutingSpecThread(RoutingSpecThread routingSpecThread)
	{
		this.routingSpecThread = routingSpecThread;
	}
	
	/**
	 * Return a string describing how the 'argument' string is used. For example,
	 * a FileConsumer would return "Filename". Return the generic string "Argument"
	 * here. This is used in the database editor.
	 * @return label describing how the argument is used
	 */
	public String getArgLabel()
	{
		return "Argument";
	}


}

