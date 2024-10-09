/*
 *  $Id$
 */
package covesw.azul.consumer;

import ilex.util.AsciiUtil;
import ilex.util.Logger;
import ilex.util.PropertiesUtil;
import ilex.util.TextUtil;
import ilex.var.IFlags;
import ilex.var.TimedVariable;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Properties;
import java.util.Date;
import java.util.StringTokenizer;
import java.text.SimpleDateFormat;

import decodes.consumer.DataConsumer;
import decodes.consumer.DataConsumerException;
import decodes.consumer.OutputFormatter;
import decodes.consumer.OutputFormatterException;
import decodes.datasource.RawMessage;
import decodes.datasource.UnknownPlatformException;
import decodes.db.Constants;
import decodes.db.Platform;
import decodes.db.PresentationGroup;
import decodes.decoder.DecodedMessage;
import decodes.decoder.Sensor;
import decodes.decoder.TimeSeries;
import decodes.util.PropertySpec;

public class CsvFormatter extends OutputFormatter
{
	private String mod = "CsvFormatter";
	private String timeFormat = "MM/dd/yyyy,HH:mm:ss";
	private SimpleDateFormat sdf = new SimpleDateFormat(timeFormat);
	private ArrayList<CsvCol> csvCols = new ArrayList<CsvCol>();
	private String delimiter = ",";
	private boolean inclSiteNameCol = true;
	private String dataTypes = null;
	private String missingValue = "";
	private boolean noHeader = false;
	private boolean headerEveryMessage = true;
	private boolean firstMsg = true;
	private boolean omitTrailingBlanks = false;
	
	PropertySpec propSpecs[] = 
	{
		new PropertySpec("delimiter", PropertySpec.STRING, "(default=comma) Use to delimit columns"),
		new PropertySpec("dataTypes", PropertySpec.STRING,
			"Default is to display all time series in the message. Set this to a "
			+ "comma-separated list of data types to only print specific sensors, and to"
			+ " control the order of columns."),
		new PropertySpec("timeFormat", PropertySpec.STRING, "(default=MM/dd/yyyy,HH:mm:ss)"
			+ " See API doc for java.text.SimpleDateFormat."),
		new PropertySpec("missingValue", PropertySpec.STRING, "(default=blank) Set this if"
			+ " you require a special value to designate a missing value in a column."),
		new PropertySpec("noHeader", PropertySpec.BOOLEAN, "(default=false) "
			+ "Set to true to never print a header."),
		new PropertySpec("headerEveryMessage", PropertySpec.BOOLEAN, "(default=true) "
			+ "True means to print a header at the start of every message."),
		new PropertySpec("omitTrailingBlanks", PropertySpec.BOOLEAN, "(default=false) "
			+ "If true, omit trailing columns if they have no data."),
	};

	public CsvFormatter()
	{
		super();
	}
	
	class CsvCol
	{
		TimeSeries timeSeries;
		int sampleNum;
		String colName;

		CsvCol(TimeSeries ts, String colName)
		{
			timeSeries = ts;
			sampleNum = 0;
			this.colName = colName;

			// sort time series into ascending order.
			if (timeSeries != null)
			{
				timeSeries.setDataOrder(Constants.dataOrderAscending);
				timeSeries.sort();
			}
		}

		String nextSamp()
		{
			if (timeSeries == null)
				return "";
			if (sampleNum >= timeSeries.size())
				return "";
			int idx = sampleNum++;
			TimedVariable tv = timeSeries.sampleAt(idx);
			if ((tv.getFlags() & IFlags.IS_MISSING) != 0 || (tv.getFlags() & IFlags.IS_ERROR) != 0)
			{
				return missingValue;
			}
			return timeSeries.formattedSampleAt(idx).trim();
		}
		
		Date nextSampTime()
		{
			if (timeSeries == null)
				return null;
			if (sampleNum < timeSeries.size())
				return timeSeries.timeAt(sampleNum);
			return null;
		}

	}


	@Override
	protected void initFormatter(String type, java.util.TimeZone tz, PresentationGroup presGrp,
		Properties rsProps) throws OutputFormatterException
	{
		logger.debug1("initFormatter " + mod 
			+ ", props='" + PropertiesUtil.props2string(rsProps) + "'");
		
		String pval = PropertiesUtil.getIgnoreCase(rsProps, "headerEveryMessage");
		if (pval != null)
			headerEveryMessage = TextUtil.str2boolean(pval);

		pval = PropertiesUtil.getIgnoreCase(rsProps, "noHeader");
		if (pval != null)
			noHeader = TextUtil.str2boolean(pval);
		
		pval = PropertiesUtil.getIgnoreCase(rsProps, "timeFormat");
		if (pval != null)
			sdf = new SimpleDateFormat(timeFormat = pval);

		pval = PropertiesUtil.getIgnoreCase(rsProps, "IncludeSiteName");
		if (pval != null)
			inclSiteNameCol = TextUtil.str2boolean(pval);

		sdf.setTimeZone(tz);

		dataTypes = PropertiesUtil.getIgnoreCase(rsProps, "dataTypes");
		if (dataTypes != null)
			dataTypes = dataTypes.trim();

		pval = PropertiesUtil.getIgnoreCase(rsProps, "missing");
		if (pval != null)
			missingValue = pval;

		pval = PropertiesUtil.getIgnoreCase(rsProps, "delimiter");
		if (pval != null)
			delimiter = new String(AsciiUtil.ascii2bin(pval));
		
		pval = PropertiesUtil.getIgnoreCase(rsProps, "omitTrailingBlanks");
		if (pval != null)
			omitTrailingBlanks = TextUtil.str2boolean(pval);
		
		firstMsg = true;
	}

	/**
	 * Closes any resources used by this formatter.
	 */
	public void shutdown()
	{
	}

	@Override
	public void formatMessage(DecodedMessage msg, DataConsumer consumer) throws DataConsumerException,
		OutputFormatterException
	{
		consumer.startMessage(msg);

		StringBuilder sb = new StringBuilder();
		RawMessage rawmsg = msg.getRawMessage();
		csvCols.clear();

		Platform platform;
		try { platform = rawmsg.getPlatform(); }
		catch (UnknownPlatformException ex)
		{
			logger.warning(mod + " requires platform association. Cannot format '"
				+ (msg.getRawMessage() != null ? new String(msg.getRawMessage().getHeader()) : "unknown") + "'");
			throw new OutputFormatterException(ex.toString());
		}

		// Has user specified specific columns?
		if (dataTypes != null && dataTypes.length() > 0)
		{
			StringTokenizer st = new StringTokenizer(dataTypes, ", ");
			while (st.hasMoreTokens())
			{
				String dt = st.nextToken();
				// ts may be null -- that's ok. Leave a placeholder.
				csvCols.add(new CsvCol(msg.getTimeSeries(dt), dt));
			}
		}
		else
			for(TimeSeries ts : msg.getTimeSeriesArray())
				csvCols.add(new CsvCol(ts, ts.getDataTypeCode()));
		
		if (csvCols.size() == 0)
			throw new OutputFormatterException("No columns -- nothing to format.");
		
		if (firstMsg || headerEveryMessage)
		{
			if (!noHeader)
			{
				sb.setLength(0);
				if (inclSiteNameCol)
					sb.append("SiteName" + delimiter);
				// If time format contains the delimeter, assume separate columns for date and time
				if (timeFormat.contains(delimiter))
					sb.append("Date" + delimiter + "Time");
				else
					sb.append("Date/Time");
				for(CsvCol col : csvCols)
				{
					sb.append(delimiter);
					sb.append(col.colName);
				}
				consumer.println(sb.toString());
			}
			firstMsg = false;
		}

		// First column will be ID attached to sensor[0] or site name.
		String id = platform.getSiteName(false);

		Date d;
		while ((d = nextDate()) != null)
		{
			sb.setLength(0);
			if (inclSiteNameCol)
			{
				sb.append(id);
				sb.append(delimiter);
			}
			
			sb.append(sdf.format(d));

			for(CsvCol col : csvCols)
			{
				sb.append(delimiter);
				if (d.equals(col.nextSampTime()))
					sb.append(col.nextSamp());
			}
			if (omitTrailingBlanks)
			{
				int newlen = sb.length();
				while(newlen > 0 && sb.charAt(newlen-1) == ',')
					newlen--;
				String s= sb.toString();
				sb.setLength(newlen);
				Logger.instance().debug1(
					"Truncate '" + s + "' to len="+newlen + ", result='" + sb.toString() + "'");
			}
			consumer.println(sb.toString());
		}
		consumer.endMessage();
	}

	private Date nextDate()
	{
		Date ret = null;

		for (int idx = 0; idx < csvCols.size(); idx++)
		{
			Date d = csvCols.get(idx).nextSampTime();
			if (d != null && (ret == null || d.compareTo(ret) < 0))
				ret = d;
		}
		return ret;
	}


	/**
	 * @return specifications of supported properties.
	 */
	public PropertySpec[] getSupportedProps()
	{
		return propSpecs;
	}

	/**
	 * @return true if additional unnamed props are allowed, falis if only the
	 *         ones returned by getSupportedProps are allowed.
	 */
	public boolean additionalPropsAllowed()
	{
		return true;
	}

}
