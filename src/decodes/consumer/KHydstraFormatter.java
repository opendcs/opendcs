/*
*  $Id$
*
*  Author: Michael Maloney
*  
*  $Log$
*  Revision 1.6  2017/03/20 18:14:28  mmaloney
*  Added new selfIdent property.
*
*  Revision 1.5  2017/02/09 17:22:42  mmaloney
*  Added CVS Header.
*
*/

package decodes.consumer;

import ilex.util.PropertiesUtil;
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
import decodes.util.PropertySpec;

/**
  This class formats decoded data in a format that can be processed
  by the KHydstra system.
  3/20/2017 - Hydstra has a variation called "self identifying" which
  causes additional columns to be included.
 */
public class KHydstraFormatter extends OutputFormatter
{
	private String delimiter;
	private SimpleDateFormat KHydstraDateFormat;
	private boolean selfIdent = false;
	private int qualcode = 1;
	
	protected PropertySpec ofPropSpecs[] = 
	{
		new PropertySpec("delimiter", PropertySpec.STRING, 
			"(default=comma) delimiter between columns."),
		new PropertySpec("selfIdent", PropertySpec.BOOLEAN,
			"(default=false) set to true to add addition columns for Hydstra "
			+ "'self identifying' format."),
		new PropertySpec("qualcode", PropertySpec.INT,
			"(default=1) quality code used in Hydstra output format.")
	};


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
		String d = PropertiesUtil.getIgnoreCase(rsProps, "delimiter");
		if (d != null)
			delimiter = d;
		String s = PropertiesUtil.getIgnoreCase(rsProps, "selfIdent");
		if (s != null)
			selfIdent = TextUtil.str2boolean(s);
		s = PropertiesUtil.getIgnoreCase(rsProps, "qualcode");
		if (s != null)
		{
			try { qualcode = Integer.parseInt(s.trim()); }
			catch(NumberFormatException ex)
			{
				logger.warning("Invalid qualcode propertyp '" + s + "' -- ignored. Usinge default=1");
				qualcode = 1;
			}
		}
		
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
				if(!Character.isLetterOrDigit(stationName.charAt(i)))
					stationName = stationName.replace(stationName.charAt(i), '_');
			if (!selfIdent)
				stationName = TextUtil.setLengthLeftJustify(stationName, 15);
				
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
				if (selfIdent)
					sb.append("#V2" + delimiter);
				
				sb.append(stationName + delimiter);
				
				if (selfIdent)
					sb.append(delimiter); // placeholder for data source

				sb.append(selfIdent ? hydstraCode : TextUtil.setLengthRightJustify(hydstraCode, 7));
				sb.append(delimiter);

				sb.append(KHydstraDateFormat.format(tv.getTime()));
				sb.append(delimiter);		

				NumberFormat nf = NumberFormat.getNumberInstance(new Locale("en_US"));
				nf.setGroupingUsed(false); // don't group by threes
				nf.setMaximumFractionDigits(3);
				nf.setMinimumFractionDigits(3);
				String s = "";
				try
				{
					s = nf.format(tv.getDoubleValue());
				}
				catch (Exception e)
				{
					throw new OutputFormatterException(e.toString());
				}
				sb.append(TextUtil.setLengthRightJustify(s, 10) + delimiter);
				
				String sQualcode = selfIdent ? ("" + qualcode) : ("  " + qualcode);
				sb.append(sQualcode + delimiter);

				String datatransCode = sensor.getProperty("HydstraTransCode");
				if(datatransCode==null)
					datatransCode="1";
				sb.append(TextUtil.setLengthRightJustify(datatransCode, 2) + delimiter);

				String HydstraMaxGap = sensor.getProperty("HydstraMaxGap");
				if(HydstraMaxGap==null)
					HydstraMaxGap="0";
				sb.append(TextUtil.setLengthRightJustify(HydstraMaxGap, 4));

				if (selfIdent)
				{
					boolean decum = TextUtil.str2boolean(sensor.getProperty("HydstraDecum"));
					sb.append(delimiter + (decum ? "DECUM" : ""));
				}
				consumer.println(sb.toString());
			}
		}
		consumer.endMessage();
	}


}

