/*
*  $Id$
*
*  Author: Michael Maloney
*  
*  $Log$
*/

package decodes.consumer;

import ilex.util.TextUtil;
import ilex.var.IFlags;
import ilex.var.TimedVariable;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Iterator;
import java.util.Locale;
import java.util.Properties;

import decodes.datasource.RawMessage;
import decodes.datasource.UnknownPlatformException;
import decodes.db.Constants;
import decodes.db.DataType;
import decodes.db.EngineeringUnit;
import decodes.db.Platform;
import decodes.db.PresentationGroup;
import decodes.db.SiteName;
import decodes.db.TransportMedium;
import decodes.decoder.DecodedMessage;
import decodes.decoder.Sensor;
import decodes.decoder.TimeSeries;

/**
  This class formats decoded data in a format that can be processed
  by the KHydstra system.  The format is a fixed-field format defined
  as follows:

     01-15   USGS station number
     16-22   Hydstra Data Type Code
               (Defined by new date type   :  Hydstra-code )
     23-37   Date and time (yyyyMMddHHmmss)
     38-40   Hydstra Quality Code
               (default -1 )
     41-42   Data Trans Code
       		(Defined by  sensor property:  HydstraTransCode )
     43-46	Maximum accepatable gap between points
     		(Default - 0)
 */
public class KHydstraFormatter extends OutputFormatter
{
	private String delimiter;
	private SimpleDateFormat KHydstraDateFormat;

	/** default constructor */
	protected KHydstraFormatter()
	{
		super();
		delimiter = ", ";
		KHydstraDateFormat = new SimpleDateFormat("yyyyMMddHHmmss");

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
		KHydstraDateFormat.setCalendar(cal);
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

		SiteName sn = platform.getSite().getName(Constants.snt_USGS);
		if (sn == null)
			sn = platform.getSite().getPreferredName();
	

		for(Iterator it = msg.getAllTimeSeries(); it.hasNext(); )
		{
			TimeSeries ts = (TimeSeries)it.next();
			if ( ! ts.isAscending() )
				ts.sort();
			Sensor sensor = ts.getSensor();

			String stationName = sn.getNameValue().toUpperCase();
			String siteName =sensor.getSensorSiteName();
							
				if (siteName != null)
					stationName = siteName;
			
				//site id may contain only upper case letters, digits and underscore characters.
				// replace any other occurrence with '_'
				for(int i = 0;i<stationName.length();i++)
				{

					if(!Character.isLetterOrDigit(stationName.charAt(i)))
					{
						stationName = stationName.replace(stationName.charAt(i), '_');
					}
				}
			
				
			EngineeringUnit eu = ts.getEU();


			DataType dt = sensor.getDataType(Constants.datatype_Hydstra);
			if (dt == null)
			{
				dt = sensor.getDataType();
				dt = dt.findEquivalent(Constants.datatype_Hydstra);
			}
			String hydstraCode = dt != null ? dt.getCode() : "0";                       
			int sz = ts.size();
			for(int i=0; i<sz; i++)
			{
				TimedVariable tv = ts.sampleAt(i);
				if ((tv.getFlags() & (IFlags.IS_MISSING | IFlags.IS_ERROR)) != 0)
					continue;

				sb.setLength(0);
				sb.append(TextUtil.setLengthLeftJustify(stationName, 15));

				sb.append(delimiter);
				sb.append(TextUtil.setLengthRightJustify(hydstraCode, 7));

				sb.append(delimiter);
				sb.append(KHydstraDateFormat.format(tv.getTime()));

				sb.append(delimiter);		
				 NumberFormat nf = NumberFormat.getNumberInstance(new Locale("en_US")) ;				
			     nf.setGroupingUsed(false) ;     // don't group by threes
			     nf.setMaximumFractionDigits(3) ;
			     nf.setMinimumFractionDigits(3) ;			  
			     String s="";
				try {
					s = nf.format(tv.getDoubleValue());
				} catch (Exception e) {
					throw new OutputFormatterException(e.toString());
				} 
				sb.append(TextUtil.setLengthRightJustify(s, 10));

				sb.append(delimiter);
				sb.append(TextUtil.setLengthRightJustify("1", 3));

				String datatransCode = sensor.getProperty("HydstraTransCode");
				if(datatransCode==null)
					datatransCode="1";
				sb.append(delimiter);
				sb.append(TextUtil.setLengthRightJustify(datatransCode, 2));

				String HydstraMaxGap = sensor.getProperty("HydstraMaxGap");
				if(HydstraMaxGap==null)
					HydstraMaxGap="0";
				
				sb.append(delimiter);
				sb.append(TextUtil.setLengthRightJustify(HydstraMaxGap, 4));
				consumer.println(sb.toString());
			}
		}
		consumer.endMessage();
	}
}

