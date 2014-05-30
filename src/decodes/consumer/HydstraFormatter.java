/*
*  $Id$
*/
package decodes.consumer;

import java.util.Iterator;
import java.util.Properties;
import java.util.Date;
import java.util.Calendar;
import java.text.SimpleDateFormat;

import ilex.var.TimedVariable;
import ilex.var.IFlags;
import ilex.util.TextUtil;

import decodes.db.*;
import decodes.decoder.DecodedMessage;
import decodes.decoder.TimeSeries;
import decodes.decoder.Sensor;
import decodes.datasource.RawMessage;
import decodes.datasource.UnknownPlatformException;

/**
  This class formats decoded data in a format that can be processed
  by the Hydstra system.  The format is a fixed-field format defined
  as follows:
 
     01-15   USGS station number
     16-20   Hydstra Data Type Code
               (Defined by new date type   :  Hydstra-code )
     21-39   Date and time (yyyy/mm/dd hh:mm:ss)
     40-40   Hydstra Translation Code
               (Defined by new sensor property:  HydstraTransCode )
     41-48   Data value
*/
public class HydstraFormatter extends OutputFormatter
{
	private String delimiter;
	private SimpleDateFormat hydstraDateFormat;

	/** default constructor */
	protected HydstraFormatter()
	{
		super();
		delimiter = " ";
		hydstraDateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
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
	protected void initFormatter(String type, java.util.TimeZone tz,
		PresentationGroup presGrp, Properties rsProps)
		throws OutputFormatterException
	{
		String d = (String)rsProps.get("delimiter");
		if (d != null)
			delimiter = d;
		Calendar cal = Calendar.getInstance(tz);
		hydstraDateFormat.setCalendar(cal);
	}

	/** Does nothing. */
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

		StringBuffer sb = new StringBuffer();
		RawMessage rawmsg = msg.getRawMessage();

		TransportMedium tm;
		Platform platform;
		String hydstraTranslationCode;

		try
		{
			tm = rawmsg.getTransportMedium();
			platform = rawmsg.getPlatform();
		}
		catch(UnknownPlatformException e)
		{
			throw new OutputFormatterException(e.toString());
		}

		SiteName sn = platform.site.getName(Constants.snt_USGS);
		if (sn == null)
			sn = platform.site.getPreferredName();
        String stationNum = sn.getNameValue();

		for(Iterator it = msg.getAllTimeSeries(); it.hasNext(); )
		{
			TimeSeries ts = (TimeSeries)it.next();
                        if ( ! ts.isAscending() )
                          ts.sort();
			Sensor sensor = ts.getSensor();

			EngineeringUnit eu = ts.getEU();
			
			DataType dt = sensor.getDataType(Constants.datatype_Hydstra);
			if (dt == null)
			{
				dt = sensor.getDataType();
				dt = dt.findEquivalent(Constants.datatype_Hydstra);
			}
			String hydstraCode = dt != null ? dt.getCode() : "0";
                        hydstraTranslationCode =
                              sensor.getProperty("HydstraTransCode");
                        if ( hydstraTranslationCode == null ) {
			  if ( hydstraCode.equals("10") )	
			    hydstraTranslationCode="5";
                          else if ( hydstraCode.equals("300") )
			    hydstraTranslationCode="8";
			  else
                            hydstraTranslationCode = "1";
			}
                        int sz = ts.size();
			for(int i=0; i<sz; i++)
			{
				TimedVariable tv = ts.sampleAt(i);
				if ((tv.getFlags() & (IFlags.IS_MISSING | IFlags.IS_ERROR)) != 0)
					continue;

				sb.setLength(0);
				sb.append(TextUtil.setLengthLeftJustify(stationNum, 15));

				sb.append(delimiter);
				sb.append(TextUtil.setLengthRightJustify(hydstraCode, 5));
			
				sb.append(delimiter);
				sb.append(hydstraDateFormat.format(tv.getTime()));

				sb.append(delimiter);
				sb.append(hydstraTranslationCode);

				sb.append(delimiter);
				String s = ts.formattedSampleAt(i).trim();
				sb.append(TextUtil.setLengthLeftJustify(s, 8));
				consumer.println(sb.toString());
			}
		}
		consumer.endMessage();
	}
}

