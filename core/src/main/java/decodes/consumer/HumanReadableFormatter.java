/*
*  $Id$
*/
package decodes.consumer;

import java.util.Iterator;
import java.util.Properties;
import java.util.Date;
import java.util.ArrayList;
import java.text.SimpleDateFormat;

import ilex.var.TimedVariable;
import opendcs.opentsdb.OpenTsdbFlags;
import ilex.util.TextUtil;
import ilex.util.Logger;
import ilex.util.PropertiesUtil;

import decodes.cwms.CwmsFlags;
import decodes.decoder.DecodedMessage;
import decodes.decoder.TimeSeries;
import decodes.hdb.HdbFlags;
import decodes.sql.SqlDatabaseIO;
import decodes.datasource.RawMessage;
import decodes.db.Constants;
import decodes.db.DataType;
import decodes.db.Database;
import decodes.db.DatabaseIO;
import decodes.db.Platform;
import decodes.db.PresentationGroup;
import decodes.util.DecodesSettings;
import decodes.util.PropertySpec;

/**
  This class formats decoded data in an easy-to-read row/column format.
  Properties honored:
  <ul>
    <li>delimiter - Delimits each column, default=" | "</li>
    <li>dateformat - A template handled by java.util.SimpleDateFormat</li>
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
	boolean showFlags = false;
	boolean includeSensorNum = false;
	
	private PropertySpec propSpecs[] = 
	{		
		new PropertySpec("delimiter", PropertySpec.STRING,
			"(default=' | ') delimiter between colums of data"),
		new PropertySpec("dateformat", PropertySpec.STRING,
			"(default=MM/dd/yyyy HH:mm:ss) format for date/time in left column"),
		new PropertySpec("datatype", PropertySpec.STRING,
			"(default taken from decodes.properties) preferred data type standard to display in column header"),
		new PropertySpec("cwmsflags", PropertySpec.BOOLEAN,
			"(default=false) display CWMS flag values alongside values"),
		new PropertySpec("showflags", PropertySpec.BOOLEAN,
			"(default=false) display validation flags alongside values")
	};

	/** default constructor */
	public HumanReadableFormatter()
	{
		super();
		delimiter = " | ";
		displayEmpty = false;
	}

	/**
	  Initializes the Formatter. This method is called from the static
	  makeOutputFormatter method in this class. The RoutingSpec does not
	  need to call it explicitly.
	  @param type the type of this output formatter.
	  @param tz the time zone as specified in the routing spec.
	  @param presGrp The presentation group to handle rounding &amp; EU conversions.
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
		if (s == null)
			s = PropertiesUtil.getIgnoreCase(rsProps, "showflags");
		if (s != null)
			showFlags = TextUtil.str2boolean(s);

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
Logger.instance().debug3("HRF.formatMessage showFlags=" + showFlags);
		try 
		{
			Platform p = rawmsg.getPlatform();
			consumer.println("");
			String name = p.makeFileName();
			if ( ! name.equals("dummy") )
				consumer.println("Message for Platform " + p.makeFileName());
		}
		catch(Exception e) {}


		// Construct column array.
//		int numColumns = 0;
//		for(Iterator<TimeSeries> it = msg.getAllTimeSeries(); it.hasNext(); it.next())
//			numColumns++;
		columns = new ArrayList<Column>();

		int i=0;
		for(Iterator<TimeSeries> it = msg.getAllTimeSeries(); it.hasNext(); )
		{
			TimeSeries ts = it.next();
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
		consumer.println(sb.toString());

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
		consumer.println(sb.toString());

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
		consumer.println(sb.toString());

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
			consumer.println(sb.toString());

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
			consumer.println(sb.toString());
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
		int flagWidth;
		int dataWidth = 0;

		Column(TimeSeries ts)
		{
			timeSeries = ts;
			curSampleNum = 0;
			colWidth = 0;
			flagWidth = 0;
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
			
Logger.instance().debug3("Column ctor: ts size=" + timeSeries.size());
			for(int i=0; i<timeSeries.size(); i++)
			{
				TimedVariable tv = timeSeries.sampleAt(i);
				String v = timeSeries.formattedSampleAt(i);
				v = v.trim();
				if (v.length() > dataWidth)
					dataWidth = v.length();
				int dp = v.indexOf('.');
				if (dp == -1)
					dp = v.length();
				if (dp > dotPos)
					dotPos = dp;
				
				if (showFlags)
				{
					String f = flags2display(tv.getFlags());
					if (f.length() > flagWidth)
						flagWidth = f.length();
				}

				int cw = dataWidth + flagWidth + (flagWidth > 0 ? 1 : 0);
				if (cw > colWidth)
					colWidth = cw;
			}
Logger.instance().debug3("colWidth=" + colWidth + ", dotPos=" + dotPos + ", dataWidth=" + dataWidth + ", flagWidth=" + flagWidth);
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
			String val = timeSeries.formattedSampleAt(curSampleNum++);
			int dp = val.indexOf('.');
			if (dp == -1)
				dp = val.length();
			StringBuffer sb = new StringBuffer(val);
			if (dp < dotPos)
			{
				while(dp++ < dotPos)
				{
					sb.insert(0, ' ');
					if (sb.charAt(sb.length() - 1) == ' ')
						sb.deleteCharAt(sb.length() - 1);
				}
			}
			if (showFlags)
			{
				while(sb.length() < dataWidth)
					sb.append(' ');
				sb.append(' ');
				sb.append(flags2display(tv.getFlags()));
			}
			while(sb.length() < colWidth-2)
				sb.append(' ');
			
			val = sb.toString();
			return val;
		}
	}
	
	private String flags2display(int flags)
	{
		DatabaseIO dbio = Database.getDb().getDbIo();
		if (dbio == null || !(dbio instanceof SqlDatabaseIO))
			return "";
		SqlDatabaseIO sqldbio = (SqlDatabaseIO)dbio;
		String ret = null;
		if (sqldbio.isCwms())
			ret = CwmsFlags.flags2Display(flags);
		else if (sqldbio.isHdb())
			ret = HdbFlags.flag2HdbDerivation(flags);
		else
			ret = OpenTsdbFlags.flags2screeningString(flags);
		if (ret == null)
			ret = "";
		return ret;
	}
	
	@Override
	public PropertySpec[] getSupportedProps()
	{
		return propSpecs;
	}
}


