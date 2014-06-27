/*
*  $Id$
*/
package decodes.consumer;

import java.util.Iterator;
import java.util.Properties;
import java.util.Date;
import java.text.SimpleDateFormat;
import java.text.NumberFormat;

import ilex.var.TimedVariable;
import ilex.var.IFlags;
import ilex.util.Logger;

import decodes.db.*;
import decodes.decoder.DecodedMessage;
import decodes.decoder.TimeSeries;
import decodes.decoder.Sensor;
import decodes.datasource.RawMessage;
import decodes.datasource.UnknownPlatformException;

/**
  This class formats decoded data as required for TVA Transation files.
*/
public class TvaFormatter extends OutputFormatter
{
	private SimpleDateFormat tvaDateFormat;
	public static final String TVA_GAGE_ID = "tva-gage-id";

	private static NumberFormat tvaNumberFormat;
	static
	{
		tvaNumberFormat = NumberFormat.getNumberInstance();
		tvaNumberFormat.setMaximumFractionDigits(2);
		tvaNumberFormat.setMinimumFractionDigits(2);
		tvaNumberFormat.setGroupingUsed(false);
		tvaNumberFormat.setMaximumIntegerDigits(4);
		tvaNumberFormat.setMinimumIntegerDigits(4);
	}

	/** default constructor */
	protected TvaFormatter()
	{
		super();
		tvaDateFormat = new SimpleDateFormat("yyyyMMddHHmm");
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
		tvaDateFormat.setTimeZone(tz);
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

		Platform platform = null;
		String tvaGageId = null;

		try
		{
			platform = rawmsg.getPlatform();
		}
		catch(UnknownPlatformException e)
		{
			throw new OutputFormatterException(e.toString());
		}

		SiteName sn = platform.site.getName(TVA_GAGE_ID);
		if (sn == null)
		{
			String err = "No name of type '" + TVA_GAGE_ID
				+ "' Defined in site "
				+ platform.getSiteName() + " -- skipped.";
			throw new OutputFormatterException(err);
		}
		tvaGageId = sn.getNameValue();
		if (tvaGageId == null)
		{
			String err = "No TVA Gage ID Defined in site "
				+ platform.getSiteName() + " -- skipped.";
			throw new OutputFormatterException(err);
		}
		if (tvaGageId.length() < 4)
		{
			String err = "Invalid TVA Gage ID '" + tvaGageId 
				+ "' Defined in site "
				+ platform.getSiteName() + " -- skipped.";
			throw new OutputFormatterException(err);
		}

		for(Iterator it = msg.getAllTimeSeries(); it.hasNext(); )
		{
			TimeSeries ts = (TimeSeries)it.next();
			Sensor sensor = ts.getSensor();

			DataType origDt = sensor.getDataType();
			if (origDt == null || origDt.getStandard() == null)
			{
				Logger.instance().log(Logger.E_WARNING,
					"Station '" + platform.getSiteName()
					+ "' No datatype defined for sensor '" + sensor.getName() 
					+ "' -- skipped.");
				continue;
			}
			DataType dt = origDt;
			if (!dt.getStandard().equalsIgnoreCase(Constants.datatype_SHEF))
				dt = dt.findEquivalent(Constants.datatype_SHEF);
			if (dt == null || dt.getStandard() == null
			 || dt.getCode() == null || dt.getCode().length() < 2)
			{
				Logger.instance().log(Logger.E_WARNING,
					"Station '" + platform.getSiteName()
					+ "' Cannot find SHEF datatype for sensor '" 
					+ sensor.getName() + "' -- skipped.");
				continue;
			}

			String tvaDataCode = shef2tvaDataCode(dt.getCode(), sensor);
			if (tvaDataCode == null || tvaDataCode.length() < 2)
			{
				Logger.instance().info(
					"Station '" + platform.getSiteName()
					+ "' Cannot find TVA Data Code for sensor '" 
					+ sensor.getName() + "' -- skipped.");
				continue;
			}

			int sz = ts.size();
			for(int i=0; i<sz; i++)
			{
				TimedVariable tv = ts.sampleAt(i);
				if ((tv.getFlags() & (IFlags.IS_MISSING | IFlags.IS_ERROR)) != 0)
					continue;

				sb.setLength(0);
				sb.setLength(30);

				// SHEF Code
				sb.setCharAt(0, dt.getCode().charAt(0));
				sb.setCharAt(1, dt.getCode().charAt(1));

				// 00 means Satellite Telemetry
				sb.setCharAt(2, '0');
				sb.setCharAt(3, '0');

				// 2-digit TVA Data Code
				sb.setCharAt(4, tvaDataCode.charAt(0));
				sb.setCharAt(5, tvaDataCode.charAt(1));

				// 4-digit TVA Gage ID
				sb.setCharAt(6, tvaGageId.charAt(0));
				sb.setCharAt(7, tvaGageId.charAt(1));
				sb.setCharAt(8, tvaGageId.charAt(2));
				sb.setCharAt(9, tvaGageId.charAt(3));

				// date/time in tva format yyyyMMddHHmm
				String dateStr = tvaDateFormat.format(tv.getTime());
				sb.setCharAt(10, dateStr.charAt(0));
				sb.setCharAt(11, dateStr.charAt(1));
				sb.setCharAt(12, dateStr.charAt(2));
				sb.setCharAt(13, dateStr.charAt(3));
				sb.setCharAt(14, dateStr.charAt(4));
				sb.setCharAt(15, dateStr.charAt(5));
				sb.setCharAt(16, dateStr.charAt(6));
				sb.setCharAt(17, dateStr.charAt(7));
				sb.setCharAt(18, dateStr.charAt(8));
				sb.setCharAt(19, dateStr.charAt(9));
				sb.setCharAt(20, dateStr.charAt(10));
				sb.setCharAt(21, dateStr.charAt(11));

				sb.setCharAt(22, ' '); // space separator before number

				StringBuffer num;
				try
				{
					num = new StringBuffer(
						tvaNumberFormat.format(tv.getDoubleValue()));
				}
				catch(ilex.var.NoConversionException ex)
				{
					Logger.instance().log(Logger.E_WARNING,
						"Station '" + platform.getSiteName()
						+ "' Bad sensor value '" + tv.toString()
						+ "' -- skipped.");
					continue;
				}
				for(int j=0; j<3; j++)
					if (num.charAt(j) == '0')
						num.setCharAt(j, ' ');
					else
						break;

				for(int j=0; j<7; j++)
					sb.setCharAt(23+j, num.charAt(j));

				consumer.println(sb.toString());
			}
		}
		consumer.endMessage();
	}

	/**
	  Hard-coded method to convert certain SHEF codes into TVA parameter
	  codes.
	  @param shef the 2-char SHEF PE code
	  @param sensor the sensor object
	  @return String TVA code
	*/
	String shef2tvaDataCode(String shef, Sensor sensor)
	{
		String tvaCode = sensor.getProperty("tvacode");
		if (tvaCode != null)
			return tvaCode;
		if (shef.equalsIgnoreCase("PC") || shef.equalsIgnoreCase("PR"))
			return "01";
		else if (shef.equalsIgnoreCase("HG") || shef.equalsIgnoreCase("SG"))
			return "04";
		else if (shef.equalsIgnoreCase("HP") || shef.equalsIgnoreCase("HE"))
			return "10";
		else if (shef.equalsIgnoreCase("HT") || shef.equalsIgnoreCase("TA"))
			return "11";
		else if (shef.equalsIgnoreCase("GP"))
			return "12";
		else if (shef.equalsIgnoreCase("WO") || shef.equalsIgnoreCase("OX"))
			return "13";
		else if (shef.equalsIgnoreCase("PH"))
			return "14";
		else if (shef.equalsIgnoreCase("TP") || shef.equalsIgnoreCase("TW"))
			return "15";
		else 
			return null;
	} 
}

