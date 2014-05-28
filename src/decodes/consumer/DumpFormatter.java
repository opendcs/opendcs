/*
*  $Id$
*
*  $State$
*
*  $Log$
*  Revision 1.1.1.1  2014/05/19 15:28:59  mmaloney
*  OPENDCS 6.0 Initial Checkin
*
*  Revision 1.3  2012/09/11 00:07:43  mmaloney
*  dev
*
*  Revision 1.2  2012/09/10 23:55:39  mmaloney
*  Allow operation without platform record for OutputTs.
*
*  Revision 1.1  2008/04/04 18:20:59  cvs
*  Added legacy code to repository
*
*  Revision 1.11  2008/03/25 17:35:16  mmaloney
*  Allow un-decoded messages for debugging purposes.
*
*  Revision 1.10  2007/05/31 13:28:24  mmaloney
*  dev
*
*  Revision 1.9  2004/08/24 21:01:35  mjmaloney
*  added javadocs
*
*  Revision 1.8  2004/06/30 20:02:44  mjmaloney
*  dev
*
*  Revision 1.7  2002/05/19 13:02:44  mjmaloney
*  Final TimeZone mods
*
*  Revision 1.6  2002/05/19 00:22:18  mjmaloney
*  Deprecated decodes.db.TimeZone and decodes.db.TimeZoneList.
*  These are now replaced by the java.util.TimeZone class.
*
*  Revision 1.5  2002/04/05 21:25:14  mike
*  Implement sensor-site names in all formatters.
*  Fix time-ordering.
*  Fix DirectoryConsumer move functions.
*
*  Revision 1.4  2001/12/02 13:57:17  mike
*  dev
*
*  Revision 1.3  2001/09/27 00:57:14  mike
*  Add units & data types.
*
*  Revision 1.2  2001/09/24 01:17:37  mike
*  Got EUs working.
*
*  Revision 1.1  2001/09/14 21:16:42  mike
*  dev
*
*/
package decodes.consumer;

import java.util.Iterator;
import java.util.Properties;
import java.util.Date;
import java.util.Calendar;
import java.text.SimpleDateFormat;

import ilex.var.Variable;
import ilex.var.TimedVariable;

import decodes.db.*;
import decodes.decoder.DecodedMessage;
import decodes.decoder.TimeSeries;
import decodes.decoder.Sensor;
import decodes.datasource.RawMessage;
import decodes.datasource.UnknownPlatformException;

/**
  This class dumps the raw message, performance measurements and decoded
  data to the consumer. It is primarily used for debugging and trouble-
  shooting.
*/
public class DumpFormatter extends OutputFormatter
{
	private SimpleDateFormat myDateFormat;

	/** default constructor */
	protected DumpFormatter()
	{
		super();
		myDateFormat = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss z");
	}

	/**
	 * Initializes the Formatter. This method is called from the static
	 * makeOutputFormatter method in this class. The RoutingSpec does not
	 * need to call it explicitely.
	  @param type the type of this output formatter.
	  @param tz the time zone as specified in the routing spec.
	  @param presGrp The presentation group to handle rounding & EU conversions.
	  @param rsProps the routing-spec properties.
	 */
	protected void initFormatter(String type, java.util.TimeZone tz,
		PresentationGroup presGrp, Properties rsProps)
		throws OutputFormatterException
	{
		Calendar cal = Calendar.getInstance(tz);
		myDateFormat.setCalendar(cal);
	}

	/**
	 * Does nothing.
	*/
	public void shutdown()
	{
	}

	/**
	* Writes the passed DecodedMessage to the passed consumer, using
	* a concrete format.
	  @param msg The message to output.
	  @param consumer The DataConsumer to output to.
	* @throws OutputFormatterException if there was a problem formatting data.
	* @throws DataConsumerException, passed through from consumer methods.
	*/
	public void formatMessage(DecodedMessage msg, DataConsumer consumer)
		throws DataConsumerException, OutputFormatterException
	{
		consumer.startMessage(msg);

		RawMessage rawmsg = msg.getRawMessage();
		consumer.printLine("=================================");
		consumer.printLine("Start of message");
		consumer.printLine("Time Stamp: "
			+ myDateFormat.format(rawmsg.getTimeStamp()));
		consumer.printLine("Raw Data:");
		consumer.printLine(new String(rawmsg.getData()));
		consumer.printLine("");
		consumer.printLine("Performance Measurements:");
		for(Iterator it = rawmsg.getPMNames(); it.hasNext(); )
		{
			String nm = (String)it.next();
			Variable v = rawmsg.getPM(nm);
			consumer.printLine(nm + "=" + v);
		}

		Platform platform;
		try { platform = rawmsg.getPlatform(); }
		catch(UnknownPlatformException e)
		{
			consumer.printLine(
				"Cannot get Platform to format output: " + e);
			platform = null;
		}
		consumer.printLine("");
		if (platform != null)
			consumer.printLine("Message is for platform " + platform.makeFileName());
		consumer.printLine("Decoded Data:");
		consumer.printLine("");
		for(Iterator it = msg.getAllTimeSeries(); it.hasNext(); )
		{
			TimeSeries ts = (TimeSeries)it.next();
			Sensor sensor = ts.getSensor();
			String platformName = sensor.getSensorSiteName();
			EngineeringUnit eu = ts.getEU();
			consumer.printLine("Sensor " + ts.getSensorNumber()
				+ ": " + sensor.getName()
				+ ", EU=" + (eu == null ? "unknown" : eu.toString())
				+ ", DataType=" + sensor.getDataType().toString());
			if (platformName != null)
				consumer.printLine("Site Name Override: " + platformName);
			int sz = ts.size();
			consumer.printLine("Number of Samples=" + sz);
			for(int i=0; i<sz; i++)
			{
				TimedVariable tv = ts.sampleAt(i);
				consumer.printLine("Sample[" + i + "]=" + 
					myDateFormat.format(tv.getTime()) + ": " + tv.valueString()
					+ "  '" + ts.formattedSampleAt(i) + "'");
			}
		}

		consumer.endMessage();
	}

	/**
	  DumpFormatter allows un-decoded, raw messages to pass.
	  @return false to allow un-decoded messages to pass.
	*/
	public boolean requiresDecodedMessage() { return false; }

}

