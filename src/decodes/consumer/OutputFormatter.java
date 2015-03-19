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
*  Revision 1.3  2013/06/17 19:50:59  mmaloney
*  Created
*
*  Revision 1.2  2009/01/22 00:31:32  mjmaloney
*  DB Caching improvements to make msgaccess start quicker.
*  Remove the need to cache the entire database.
*
*  Revision 1.1  2008/04/04 18:20:59  cvs
*  Added legacy code to repository
*
*  Revision 1.11  2006/07/18 00:12:16  mmaloney
*  dev
*
*  Revision 1.10  2005/03/15 16:52:00  mjmaloney
*  Rename Enum to DbEnum for Java 5 compatibility
*
*  Revision 1.9  2005/03/15 16:11:25  mjmaloney
*  Modify 'Enum' for Java 5 compat.
*
*  Revision 1.8  2004/08/24 21:01:37  mjmaloney
*  added javadocs
*
*  Revision 1.7  2004/02/29 20:48:52  mjmaloney
*  Working implementation of DCP Monitor Server
*
*  Revision 1.6  2004/02/19 01:52:57  mjmaloney
*  Hard coded mapping for NullFormatter.
*
*  Revision 1.5  2002/05/19 00:22:18  mjmaloney
*  Deprecated decodes.db.TimeZone and decodes.db.TimeZoneList.
*  These are now replaced by the java.util.TimeZone class.
*
*  Revision 1.4  2002/03/31 21:09:34  mike
*  bug fixes
*
*  Revision 1.3  2001/12/01 20:48:15  mike
*  error message bug fix.
*
*  Revision 1.2  2001/09/14 21:16:42  mike
*  dev
*
*  Revision 1.1  2001/09/09 17:39:42  mike
*  Created.
*
*/
package decodes.consumer;

import ilex.util.Logger;

import java.util.Properties;

import decodes.db.*;
import decodes.decoder.DecodedMessage;
import decodes.util.PropertiesOwner;
import decodes.util.PropertySpec;

/**
  This is the base-class for all DECODES output formatters.
*/
public abstract class OutputFormatter
	implements PropertiesOwner
{
	protected Logger logger = Logger.instance();
	
	/**
	* All subclasses must implement a no-arguments constructor because
	* the makeOutputFormatter factory method instantiates the object
	* dynamically from the named Class.
	*/
	protected OutputFormatter() {}

	/**
	  Initializes the Formatter. This method is called from the static
	  makeOutputFormatter method in this class. The RoutingSpec does not
	  need to call it explicitely.
	  @param type the type of this output formatter.
	  @param tz the time zone as specified in the routing spec.
	  @param presGrp The presentation group to handle rounding & EU conversions.
	  @param rsProps the routing-spec properties.
	*/
	protected abstract void initFormatter(String type, java.util.TimeZone tz,
		PresentationGroup presGrp, Properties rsProps)
		throws OutputFormatterException;

	/**
	* Closes any resources used by this formatter.
	*/
	public abstract void shutdown();

	/**
	  Writes the passed DecodedMessage to the passed consumer, using
	  a concrete format.
	  @param msg The message to output.
	  @param consumer The DataConsumer to output to.
	  @throws OutputFormatterException if there was a problem formatting data.
	  @throws DataConsumerException, passed through from consumer methods.
	*/
	public abstract void formatMessage(
		DecodedMessage msg, DataConsumer consumer)
		throws DataConsumerException, OutputFormatterException;

	/**
	 * Base class does nothing. This is called by the RoutingSpecThread when
	 * a data source either returns null (meaning try again later) or throws
	 * a DataSourceEndException (which terminates the routing spec). This
	 * method allows an output formatter to do any special processing like
	 * flushing buffers, etc.
	 * @param dataSourceEnd true if this is the result of DataSourceEndException
	 */
	public void dataSourceCaughtUp(boolean dataSourceEnd)
	{
	}

	/**
	  Factory method to make a concrete OutputFormatter object.
	  @param type Enum value specifying type of formatter.
	  @param tz the time zone to use.
	  @param presGrp the presentation group for rounding and EU conversions.
	  @param rsProps routing spec properties
	  @throws OutputFormatterException if unrecognized type or if thrown
	     by concrete class constructor.
	*/
	public static OutputFormatter makeOutputFormatter(
		String type, java.util.TimeZone tz, PresentationGroup presGrp, 
		Properties rsProps)
		throws OutputFormatterException
	{
		if (tz == null)
			tz = java.util.TimeZone.getTimeZone("UTC");

		// Get the Data Consumer Type Enumeration from the database
		Database db = Database.getDb();
		decodes.db.DbEnum types = db.getDbEnum("OutputFormat");
		if (types == null)
			throw new OutputFormatterException(
				"Cannot prepare output formatter '" + type+
				"' No Enumeration for OutputFormat");

		// Lookup the value corresponding to this object's type.
		OutputFormatter ret = null;
		if (!type.equalsIgnoreCase("null"))
		{
			EnumValue myType = types.findEnumValue(type);
			if (myType == null)
				throw new OutputFormatterException(
					"Cannot prepare output formatter: "
					+ "No Output Formatter Enumeration Value for '" + type+"'");

			// Instantiate a concrete data source to delegate to.
			try
			{
				Class myClass = myType.getExecClass();
				ret = (OutputFormatter)myClass.newInstance();
			}
			catch(Exception e)
			{
				throw new OutputFormatterException(
					"Cannot prepare output formatter :"
					+ "Cannot instantiate an output formatter of type '" +type
					+ "': " + e.toString());
			}
		}
		else // use the NullFormatter
			ret = new NullFormatter();

		if (rsProps != null)
			ret.initFormatter(type, tz, presGrp, rsProps);
		return ret;
	}

	/**
	  Returns true if this formatter requires a decoded message.
	  Most formatters do, so they don't override this method in the
	  base class, which just returns true.
	  Some formatters, like NullFormatter, or DumpFormatter, can do
	  something with a raw-message only. In such cases, the concrete
	  subclass should override this method and return false.
	  @return true
	*/
	public boolean requiresDecodedMessage() { return true; }

	/**
	  Returns true if this formatter can only accept real DCP messages.
	  That is, it disallows DAPS status messages with failure codes other
	  than G or ?. The base class returns true, which is correct for most
	  formatters. Some formatters, like NullFormatter or DumpFormatter
	  can accept DAPS status messages, and should override this method
	  to return false.
	  @return true
	*/
	public boolean acceptRealDcpMessagesOnly() { return true; }

	/**
	 * Returns true if routing spec should attempt to decode the data.
	 * Default implementation here returns true.
	 */
	public boolean attemptDecode() { return true; }

	public void setLogger(Logger logger)
	{
		this.logger = logger;
	}
	
	/**
	 * @return true if this formatter uses the timezone setting, false if not.
	 */
	public boolean usesTZ() { return true; }
	
	protected PropertySpec ofPropSpecs[] = 
	{
	};
	
	/**
	 * @return specifications of supported properties.
	 */
	public PropertySpec[] getSupportedProps()
	{
		return ofPropSpecs;
	}
	
	/**
	 * @return true if additional unnamed props are allowed, falis if only the
	 * ones returned by getSupportedProps are allowed.
	 */
	public boolean additionalPropsAllowed()
	{
		return true;
	}

}

