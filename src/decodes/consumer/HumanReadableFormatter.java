/*
*  $Id$
*/
package decodes.consumer;

import java.util.Iterator;
import java.util.Properties;
import java.util.Date;
import java.util.Calendar;
import java.util.ArrayList;
import java.text.SimpleDateFormat;

import ilex.var.TimedVariable;
import ilex.util.TextUtil;
import ilex.util.Logger;
import ilex.util.PropertiesUtil;

import decodes.cwms.CwmsFlags;
import decodes.db.*;
import decodes.decoder.DecodedMessage;
import decodes.decoder.TimeSeries;
import decodes.decoder.Sensor;
import decodes.datasource.RawMessage;
import decodes.datasource.UnknownPlatformException;
import decodes.util.DecodesSettings;

/**
  This class formats decoded data in an easy-to-read row/column format.
  Properties honored:
  <ul>
    <li>delimiter - Delimits each column, default=" | "</li>
    <li>dateformat> - A template handled by java.util.SimpleDateFormat</li>
    <li>datatype - the preferred data type standard (e.g. SHEF-PE) </li>
    <li>displayempty - display empty colums (default is to omit) </li>
    <li>cwmsflags - default=false, if true, display cwms flag values </li>
  </ul>
*/
public class HumanReadableFormatter extends OutputFormatter
{
	private String delimiter;
	private SimpleDateFormat dateFormat;
	private SimpleDateFormat tzFormat;
	private String dateFormatString = Constants.defaultDateFormat_fmt;
	private String preferredDataType = 
		DecodesSettings.instance().dataTypeStdPreference;
//		Constants.datatype_SHEF;
	private boolean displayEmpty;
	private ArrayList<Column> columns;
	boolean cwmsflags = false;
	boolean includeSensorNum = false;

	/** default constructor */
	protected HumanReadableFormatter()
	{
		super();
		delimiter = " | ";
		displayEmpty = false;
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
		String s = PropertiesUtil.getIgnoreCase(rsProps, "delimiter");
		if (s != null)
			delimiter = s;

		s = PropertiesUtil.getIgnoreCase(rsProps, "dateformat");
		if (s != null)
			dateFormatString = s;
		dateFormat = new SimpleDateFormat(dateFormatString);
		tzFormat = new SimpleDateFormat("z");

		dateFormat.setTimeZone(tz);
		tzFormat.setTimeZone(tz);

		s = PropertiesUtil.getIgnoreCase(rsProps,"datatype");
		if (s != null)
			preferredDataType = s;

		s = PropertiesUtil.getIgnoreCase(rsProps, "displayempty");
		if (s != null)
			displayEmpty = TextUtil.str2boolean(s);
		
		s = PropertiesUtil.getIgnoreCase(rsProps, "cwmsflags");
		if (s != null)
			cwmsflags = TextUtil.str2boolean(s);

		s = PropertiesUtil.getIgnoreCase(rsProps, "includeSensorNum");
		if (s != null)
			includeSensorNum = TextUtil.str2boolean(s);
	}

	/** Does nothing.  */
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

		try 
		{
			Platform p = rawmsg.getPlatform();
			consumer.printLine("");
			String name = rawmsg.getPlatform().makeFileName();
			if ( ! name.equals("dummy") )
				consumer.printLine("Message for Platform " 
					+ rawmsg.getPlatform().makeFileName());
		}
		catch(Exception e) {}


		// Construct column array.
		int numColumns = 0;
		for(Iterator it = msg.getAllTimeSeries(); it.hasNext(); it.next())
			numColumns++;
		columns = new ArrayList<Column>();

		int i=0;
		for(Iterator it = msg.getAllTimeSeries(); it.hasNext(); )
		{
			TimeSeries ts = (TimeSeries)it.next();
			if (!displayEmpty && ts.size() == 0)
				continue;
			columns.add(new Column(ts));
		}

		// First header line is sensor names
		sb.setLength(0);
		for(i=0; i<dateFormatString.length(); i++)
			sb.append(' ');
		sb.append(delimiter);
		for(Column col : columns)
		{
			sb.append(TextUtil.strcenter(col.sensorName, 
				col.colWidth));
			sb.append(delimiter);
		}
		consumer.printLine(sb.toString());

		// Second header line is sensor data type code
		sb.setLength(0);
		for(i=0; i<dateFormatString.length(); i++)
			sb.append(' ');
		sb.append(delimiter);
		for(Column col : columns)
		{
			sb.append(TextUtil.strcenter(col.dataType,
				col.colWidth));
			sb.append(delimiter);
		}
		consumer.printLine(sb.toString());

		// Third header line is EU abbreviation
		sb.setLength(0);
		Date msgTime = msg.getRawMessage().getTimeStamp();
		if (msgTime != null)
			sb.append(TextUtil.strcenter(
				tzFormat.format(msg.getRawMessage().getTimeStamp()),
				dateFormatString.length()));
		else
			sb.append("                   ");
		sb.append(delimiter);
		for(Column col : columns)
		{
			sb.append(TextUtil.strcenter(col.euAbbr, col.colWidth));
			sb.append(delimiter);
		}
		consumer.printLine(sb.toString());

		// Forth line is actual-site-name. Only use if a sensor uses it.
		sb.setLength(0);
		for(i=0; i<dateFormatString.length(); i++)
			sb.append(' ');
		sb.append(delimiter);
		boolean doSensorSite = false;
		for(Column col : columns)
		{
			if (col.siteName != null)
			{
				doSensorSite = true;
				sb.append(TextUtil.strcenter(col.siteName, col.colWidth));
			}
			else
				sb.append(TextUtil.strcenter(" ", col.colWidth));
			sb.append(delimiter);
		}
		if (doSensorSite)
			consumer.printLine(sb.toString());

		Date d;
		while((d = findNextDate()) != null)
		{
			sb.setLength(0);
			sb.append(dateFormat.format(d));
			sb.append(delimiter);
			for(Column col : columns)
			{
				String s;
				if (d.equals(col.nextSampleTime()))
					s = TextUtil.setLengthLeftJustify(col.nextSample(),
						col.colWidth);
				else
					s = col.getBlankSample();

				sb.append(s);
				sb.append(delimiter);
			}
			consumer.printLine(sb.toString());
		}
		consumer.endMessage();
	}

	/** @return next date in any time series */
	private Date findNextDate()
	{
		Date ret = null;

		for(Column col : columns)
		{
			Date d = col.nextSampleTime();
			if (d != null
			 && (ret == null || d.compareTo(ret) < 0))
				ret = d;
		}
		return ret;
	}

	class Column
	{
		TimeSeries timeSeries;
		int colWidth;
		int curSampleNum;
		String blankSample;
		String sensorName;
		String dataType;
		String euAbbr;
		int dotPos;
		String siteName;

		Column(TimeSeries ts)
		{
			timeSeries = ts;
			curSampleNum = 0;
			colWidth = 0;
			siteName = ts.getSensor().getSensorSiteName();

			sensorName = ts.getSensor().getName();
			if (sensorName == null)
				sensorName = "unknown";
			if (includeSensorNum)
				sensorName = ts.getSensor().getNumber() + ": " + sensorName;

			euAbbr = ts.getEU().abbr;

			DataType dt = ts.getSensor().getDataType(preferredDataType);
			if (dt == null)
			{
				// Doesn't have preferred, get whatever is defined.
				dt = ts.getSensor().getDataType();
				if (dt == null)
				{
					Logger.instance().log(Logger.E_WARNING,
						"Site '" + siteName +"' Sensor "
						+ ts.getSensor().getNumber() 
						+ " has unknown data type!");
					dt = DataType.getDataType("UNKNOWN", "UNKNOWN");
				}
			}
			dataType = dt.getCode();

			colWidth = sensorName.length();
			if (dataType.length() > colWidth)
				colWidth = dataType.length();
			if (euAbbr.length() > colWidth)
				colWidth = euAbbr.length();
			if (siteName != null && siteName.length() > colWidth)
				colWidth = siteName.length();

			dotPos = -1;
			for(int i=0; i<timeSeries.size(); i++)
//			for(Iterator it = timeSeries.formattedSamplesIterator(); 
//				it.hasNext(); )
			{
				String s = timeSeries.formattedSampleAt(i);
//				String s = (String)it.next();
				if (s.length() > colWidth)
					colWidth = s.length();
				int dp = s.indexOf('.');
				if (dp == -1)
				{
					String trimmed = s.trim();
					if (trimmed.length() > 0 && !Character.isDigit(trimmed.charAt(0)))
						dp = 0;
					else
						dp = s.length();
				}
				if (dp > dotPos)
					dotPos = dp;
			}
			StringBuffer sb = new StringBuffer();
			for(int i=0; i<colWidth; i++)
				sb.append(' ');
			blankSample = sb.toString();

			// sort time series into ascending order.
			timeSeries.setDataOrder(Constants.dataOrderAscending);
			timeSeries.sort();
		}

		String getBlankSample()
		{
			return blankSample;
		}
	
		// Return date of next sample in series or null if at end of series.
		Date nextSampleTime()
		{
			if (curSampleNum < timeSeries.size())
				return timeSeries.timeAt(curSampleNum);
			return null;
		}

		String nextSample()
		{
			if (curSampleNum >= timeSeries.size())
				return blankSample;
			TimedVariable tv = timeSeries.sampleAt(curSampleNum);
			String s = timeSeries.formattedSampleAt(curSampleNum++);
			int dp = s.indexOf('.');
			if (dp == -1)
				dp = s.length();
			StringBuffer sb = new StringBuffer(s);
			if (dp < dotPos)
			{
				while(dp++ < dotPos)
				{
					sb.insert(0, ' ');
					if (sb.charAt(sb.length() - 1) == ' ')
						sb.deleteCharAt(sb.length() - 1);
				}
			}
			if (cwmsflags)
			{
				sb.append(' ');
				sb.append(CwmsFlags.flags2Display(tv.getFlags()));
			}
			while(sb.length() < colWidth-2)
				sb.append(' ');
			
			s = sb.toString();
			return s;
		}
	}
}


