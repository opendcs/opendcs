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

import decodes.db.*;
import decodes.decoder.DecodedMessage;
import decodes.routing.RoutingSpecThread;
import decodes.util.PropertiesOwner;
import decodes.util.PropertySpec;

/**
  This is the base-class for all DECODES output formatters.
*/
public abstract class OutputFormatter implements PropertiesOwner
{
	protected RoutingSpecThread rsThread = null;

	/**
	* All subclasses must implement a no-arguments constructor because
	* the makeOutputFormatter factory method instantiates the object
	* dynamically from the named Class.
	*/
	protected OutputFormatter() {}

	/**
	  Initializes the Formatter. This method is called from the static
	  makeOutputFormatter method in this class. The RoutingSpec does not
	  need to call it explicitly.
	  @param type the type of this output formatter.
	  @param tz the time zone as specified in the routing spec.
	  @param presGrp The presentation group to handle rounding &amp; EU conversions.
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
	 * @param tz the time zone to use.
	 * @param presGrp the presentation group for rounding and EU conversions.
	 * @param rsProps routing spec properties
	 * @param rsThread TODO
	  @throws OutputFormatterException if unrecognized type or if thrown
	     by concrete class constructor.
	*/
	public static OutputFormatter makeOutputFormatter(
		String type, java.util.TimeZone tz, PresentationGroup presGrp,
		Properties rsProps, RoutingSpecThread rsThread)
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
			{
				if (type.equalsIgnoreCase("tsdb"))
					ret =  new TsdbFormatter();
				else
					throw new OutputFormatterException(
						"Cannot prepare output formatter: "
						+ "No Output Formatter Enumeration Value for '" + type+"'");
			}

			// Instantiate a concrete data source to delegate to.
			if (ret == null)
			{
				try
				{
					Class<?> myClass = myType.getExecClass();
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
		}
		else // use the NullFormatter
			ret = new NullFormatter();

		ret.rsThread = rsThread;
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
	 * @return true if additional unnamed props are allowed, fail if only the
	 * ones returned by getSupportedProps are allowed.
	 */
	public boolean additionalPropsAllowed()
	{
		return true;
	}
}
