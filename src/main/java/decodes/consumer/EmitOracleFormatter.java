/*
*  $Id$
*/
package decodes.consumer;

import java.util.Iterator;
import java.util.Properties;
import java.text.SimpleDateFormat;

import ilex.util.PropertiesUtil;
import ilex.util.TextUtil;
import ilex.var.TimedVariable;
import ilex.var.IFlags;
import decodes.decoder.DecodedMessage;
import decodes.decoder.TimeSeries;
import decodes.decoder.Sensor;
import decodes.datasource.RawMessage;
import decodes.datasource.UnknownPlatformException;
import decodes.db.Constants;
import decodes.db.DataType;
import decodes.db.EngineeringUnit;
import decodes.db.Platform;
import decodes.db.PresentationGroup;
import decodes.db.SiteName;
import decodes.db.TransportMedium;
import decodes.util.PropertySpec;

/**
  This class formats decoded data in the same way that EMIT did when
  ORACLE output was selected.
*/
public class EmitOracleFormatter extends OutputFormatter
{
	/** Column-delimiter specified by 'delimiter' property */
	private String delimiter = " ";
	
	private boolean justify = true;
	private boolean addMsgDelim = true;
	private String dateFormat = "yyDDD/HH:mm:ss";
	private String siteNameType = null;
	private String sitePrefix = null;
	private String dataType = Constants.datatype_SHEF;

	/** Used to format time stamps. */
	private SimpleDateFormat sdf = null;
	
	private PropertySpec propSpecs[] = 
	{		
		new PropertySpec("delimiter", PropertySpec.STRING,
			"Used between columns (default=space)"),
		new PropertySpec("justify", PropertySpec.BOOLEAN,
			"(default=true) Pad with blanks to line up columns."),
		new PropertySpec("addMsgDelim", PropertySpec.BOOLEAN,
			"(default=true) Add a line with ZZZZ at the end of each message."),
		new PropertySpec("dateFormat", PropertySpec.STRING,
			"(default=yyDDD/HH:mm:ss) Java SimpleDateFormat spec."),
		new PropertySpec("siteNameType", PropertySpec.STRING,
			"(default=null meaning print DCP Address)"),
		new PropertySpec("sitePrefix", PropertySpec.STRING,
			"(default=none) If supplied, this is added as a prefix to site names."),
		new PropertySpec("dataType", PropertySpec.DECODES_ENUM + "DataTypeStandard",
			"(default=" + Constants.datatype_SHEF + ") Determines which data type to print.")
	
	};

	/** default constructor */
	public EmitOracleFormatter()
	{
		super();
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
		
		s = PropertiesUtil.getIgnoreCase(rsProps, "justify");
		if (s != null)
			justify = TextUtil.str2boolean(s);
		
		s = PropertiesUtil.getIgnoreCase(rsProps, "addMsgDelim");
		if (s != null)
			addMsgDelim = TextUtil.str2boolean(s);
		
		s = PropertiesUtil.getIgnoreCase(rsProps, "dateFormat");
		if (s != null)
			dateFormat = s;
		sdf = new SimpleDateFormat(dateFormat);
		sdf.setTimeZone(tz);

		s = PropertiesUtil.getIgnoreCase(rsProps, "siteNameType");
		if (s != null)
			siteNameType = s;
		s = PropertiesUtil.getIgnoreCase(rsProps, "sitePrefix");
		if (s != null)
			sitePrefix = s;
		
		s = PropertiesUtil.getIgnoreCase(rsProps, "dataType");
		if (s != null)
			dataType = s;

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

		try
		{
			tm = rawmsg.getTransportMedium();
			platform = rawmsg.getPlatform();
		}
		catch(UnknownPlatformException e)
		{
			throw new OutputFormatterException(e.toString());
		}

		String dcpId = tm.getMediumId();
		if (siteNameType != null)
		{
			String snt[] = siteNameType.split(",");
			for(String s : snt)
			{
				SiteName sn = platform.getSite().getName(s);
				if (sn != null)
				{
					dcpId = sn.getNameValue();
					break;
				}
			}
			// Fell through, no match found.
			dcpId = platform.getSiteName(false);
		}
		if (sitePrefix != null)
			dcpId = sitePrefix + dcpId;

		char platformType = 'I';
		if (tm.getMediumType().equalsIgnoreCase(Constants.medium_GoesRD))
			platformType = 'R';

		for(Iterator<TimeSeries> it = msg.getAllTimeSeries(); it.hasNext(); )
		{
			TimeSeries ts = it.next();
			Sensor sensor = ts.getSensor();

			String siteName = dcpId;
			String sensorSiteName = sensor.getSensorSiteName();
			if (sensorSiteName != null)
				siteName = (sitePrefix == null ? "" : sitePrefix) + sensorSiteName;

			EngineeringUnit eu = ts.getEU();

//			DataType dt = sensor.getDataType(Constants.datatype_EPA);
//			String usgsCode = dt != null ? dt.getCode() : "0";

			DataType dt = sensor.getDataType(dataType);
			String dtCode = dt != null ? dt.getCode() : "XX";

			String sensorNum = "" + ts.getSensorNumber();
//			String sensorName = sensor.getName();

//			String recordingInt = "" + sensor.getRecordingInterval();

			int sz = ts.size();
			for(int i=0; i<sz; i++)
			{
				TimedVariable tv = ts.sampleAt(i);
				if ((tv.getFlags() & (IFlags.IS_MISSING | IFlags.IS_ERROR)) != 0)
					continue;

				sb.setLength(0);
				sb.append(siteName);
				if (justify)
					for(int j=sb.length(); j<8; j++)
						sb.append(' ');

				sb.append(delimiter);
				sb.append(dtCode);

				sb.append(delimiter);
				sb.append(sensorNum);
				if (justify)
					for(int j=sensorNum.length(); j<4; j++)
						sb.append(' ');
			
				sb.append(delimiter);
				sb.append(sdf.format(tv.getTime()));

				sb.append(delimiter);
				String s = ts.formattedSampleAt(i);
				sb.append(s);
				if (justify)
					for(int j=s.length(); j<10; j++)
						sb.append(' ');
			
				sb.append(delimiter);
				sb.append(platformType);

				sb.append(delimiter);
				sb.append(eu.abbr);

				consumer.println(sb.toString());
			}
		}
		if (addMsgDelim)
			consumer.println("ZZZZ");
		consumer.endMessage();
	}
	
	@Override
	public PropertySpec[] getSupportedProps()
	{
		return propSpecs;
	}

}

