/*
*  $Id$
*/
package decodes.consumer;

import ilex.util.EnvExpander;
import ilex.util.Logger;
import ilex.util.PropertiesUtil;
import ilex.util.StringPair;
import ilex.util.TextUtil;
import ilex.var.NoConversionException;
import ilex.var.Variable;
import ilex.xml.XmlOutputStream;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.Properties;
import java.util.TimeZone;

import lrgs.common.DcpAddress;
import lrgs.common.DapsFailureCode;
import decodes.datasource.GoesPMParser;
import decodes.datasource.RawMessage;
import decodes.datasource.UnknownPlatformException;
import decodes.db.Constants;
import decodes.db.DataType;
import decodes.db.Platform;
import decodes.db.PresentationGroup;
import decodes.db.Site;
import decodes.db.SiteName;
import decodes.db.TransportMedium;
import decodes.dcpmon.DcpMonitor;
import decodes.dcpmon.DcpNameDescResolver;
import decodes.decoder.DecodedMessage;
import decodes.decoder.TimeSeries;
import decodes.drgsinfogui.DrgsReceiverIo;
import decodes.util.DecodesSettings;
import decodes.util.PropertySpec;
import decodes.xml.XmlDatabaseIO;
import decodes.util.Pdt;

/**
  This class formats raw and decoded data together in an HTML page.
  Properties honored:
  <ul>
    <li>dateformat - A template handled by java.util.SimpleDateFormat</li>
    <li>datatype - the preferred data type standard (e.g. SHEF-PE) </li>
    <li>sitenametype - the preferred data type standard (e.g. SHEF-PE) </li>
	<li>xmlplatformdir - Directory containing XML platform metadata files</li>
  </ul>
*/
public class HtmlFormatter extends OutputFormatter
{
	private SimpleDateFormat dateFormat;
	private SimpleDateFormat msgDateFormat;
	private SimpleDateFormat tzFormat;
	private String dateFormatString = Constants.defaultDateFormat_fmt;
	private String preferredDataType = Constants.datatype_SHEF;
	private String siteNameType = null;
	private String xmlPlatformDir = null;
	private Column columns[];
	private TimeZone myTZ;
	public static boolean comesFromDcpMon = false;
	
	/** Set to true to have meta-data link be a CGI call for DCP monitor. */
	public static boolean metaDataCgi = false;
	
	private PropertySpec propSpecs[] = 
	{		
		new PropertySpec("dateformat", PropertySpec.STRING,
			"SimpleDateFormat spec used to format date/times (default=" + Constants.defaultDateFormat_fmt
			+ ")"),
		new PropertySpec("datatype", PropertySpec.STRING,
			"Preferred data type standard (default set in your decodes.properties)"),
		new PropertySpec("sitenametype", PropertySpec.STRING,
			"Preferred site name type (default set in your decodes.properties)"),
		new PropertySpec("xmlplatformdir", PropertySpec.DIRECTORY,
			"Directory of downloadable xml platform files (default=null)")
	};


	/** default constructor */
	protected HtmlFormatter()
	{
		super();
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
		String s = PropertiesUtil.getIgnoreCase(rsProps, "dateformat");
		if (s != null)
			dateFormatString = s;
		dateFormat = new SimpleDateFormat(dateFormatString);
		tzFormat = new SimpleDateFormat("z");
		
		dateFormat.setTimeZone(tz);
		tzFormat.setTimeZone(tz);
		myTZ = tz;

		msgDateFormat = new SimpleDateFormat(dateFormatString);
		msgDateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));

		s = PropertiesUtil.getIgnoreCase(rsProps, "datatype");
		if (s != null)
			preferredDataType = s;

		siteNameType = DecodesSettings.instance().siteNameTypePreference;
		s = PropertiesUtil.getIgnoreCase(rsProps, "sitenametype");
		if (s != null)
			siteNameType = s;

		xmlPlatformDir = PropertiesUtil.getIgnoreCase(rsProps,"xmlplatformdir");
	}

	/** Does nothing.  */
	public void shutdown()
	{
	}


	/**
	 * HtmlFormatter will still create an abbreviated report for data that
	 * wasn't decoded. 
	 * @return false
	 */
	public boolean requiresDecodedMessage() { return false; }

	/**
	 * Formatter needs to accept DAPS status messages, but they will be
	 * ignored.
	 * @return false
	 */
	public boolean acceptRealDcpMessagesOnly() { return false; }

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

		/** Explicitely skip DAPS status messages. */
		try
		{
			Variable v = msg.getRawMessage().getPM(GoesPMParser.FAILURE_CODE);
			char failureCode = v.getCharValue();

			if (failureCode !='G' && failureCode != '?')
			{
				consumer.endMessage();
				return;
			}
		}
		catch(Exception ex)
		{
			Logger.instance().warning("Cannot determine failure code "
				+ " -- assuming good.");
		}

		OutputStream os = consumer.getOutputStream();
		if (os == null)
		{
			Logger.instance().debug1("Messgae skipped -- null output stream.");
			return;
		}

		XmlOutputStream xos = new XmlOutputStream(os, "html");
		xos.xmlDtdUri = "-//W3C//DTD HTML 4.01 Transitional//EN";
		xos.xmlDtdScope = "PUBLIC";

		try
		{
			xos.writeXmlHeader();
			writeHtmlHeader(xos);
			xos.startElement("body");
			writePageHeader(msg, xos);
			writeMsgParams(msg, xos);
			writeRaw(msg, xos);
			writeDecoded(msg, xos);
			writeMetaData(msg, xos);
			xos.endElement("body");
		}
		catch(IOException ex)
		{
			throw new DataConsumerException("Cannot write to output: " + ex);
		}
		consumer.endMessage();
	}

	/**
	 * Writes the HTML header to the output stream.
	 * @param xos the XmlOutputStream object
	 */
	private void writeHtmlHeader(XmlOutputStream xos)
		throws IOException
	{
		xos.startElement("head");
		xos.writeElement("meta", "http-equiv", "content-type",
			"content", "text/html; charset=ISO-8859-1", null);
		xos.writeElement("title", "DCP Message Display");
		xos.endElement("head");
	}

	private void writePageHeader(DecodedMessage msg, XmlOutputStream xos)
		throws IOException, OutputFormatterException
	{
		RawMessage rawmsg = msg.getRawMessage();
		String siteName = "";
		String siteDesc = null;

		try
		{
			Variable v = rawmsg.getPM(GoesPMParser.SITE_NAME);
			if (v != null)
			{
				siteName = v.toString();
				v = rawmsg.getPM(GoesPMParser.SITE_DESC);
				if (v != null)
					siteDesc = v.toString();
				else
					siteDesc = "";
			}
			else
			{
				Platform p = msg.getPlatform();
				Site s = p.getSite();
				SiteName sn = s.getName(siteNameType);
				if (sn == null)
					sn = s.getPreferredName();
				siteName = sn.getNameValue();
				siteDesc = p.description;
				if (siteDesc == null || siteDesc.equals(""))
					siteDesc = s.getDescription();
	
//TODO When running as part of the DCP Mon web service, we want to resolve the
// name according to DCP Monitor config.
// Options: Make a separate formatter for DCP Mon or figure out how this reference can work
// without DcpMonitor being a singleton.
//				if (comesFromDcpMon)
//				{
//					DcpAddress daddr = new DcpAddress(
//						rawmsg.getPM(GoesPMParser.DCP_ADDRESS).getStringValue());
//					DcpNameDescResolver dndr = 
//						DcpMonitor.instance().getDcpNameDescResolver();
//					StringPair sp = dndr.getBestNameDesc(daddr, p);
//					siteName = sp.first;
//					siteDesc = sp.second;
//				}
			}
		}
		catch(Exception ex)
		{
//TODO Likewise, DcpMonitor is no longer a singleton.
//			if (comesFromDcpMon)
//			{
//				DcpNameDescResolver dndr = 
//					DcpMonitor.instance().getDcpNameDescResolver();
//				DcpAddress daddr = new DcpAddress(
//					rawmsg.getPM(GoesPMParser.DCP_ADDRESS).getStringValue());
//				StringPair sp = dndr.getBestNameDesc(daddr, null);
//				siteName = sp.first;
//				siteDesc = sp.second;
//			}
			Logger.instance().debug1(
				"Cannot get platform metadata, will display raw only: " + ex);
			Variable siteNameV = rawmsg.getPM(GoesPMParser.DCP_ADDRESS);
			if (siteName.equals("") && siteNameV != null)
			{
				siteName = siteNameV.toString();
			}
			if (siteDesc != null && !siteDesc.equals(""))
			{
				siteDesc = siteDesc + 
				", No Matching TransportMedium for Channel "
					+ getPM(rawmsg, GoesPMParser.CHANNEL);	
			}
			else
			{
				siteDesc = "No Matching TransportMedium for Channel "
					+ getPM(rawmsg, GoesPMParser.CHANNEL);
			}
		}	

		Date t = rawmsg.getTimeStamp();
		xos.startElement("h2", "style", "text-align: center;");
		xos.writePCDATA(siteName + " - " + msgDateFormat.format(t) + " UTC");
		if (siteDesc != null)
		{
			xos.writeElement("br", null);
			xos.writePCDATA(siteDesc);
		}
		xos.endElement("h2");
	}
	
	private void writeMsgParams(DecodedMessage msg, XmlOutputStream xos)
		throws IOException, OutputFormatterException
	{
		xos.writeElement("h3", "Message Parameters:");
		StringPair sp4[] = new StringPair[4];
		sp4[0] = new StringPair("cellpadding", "2");
		sp4[1] = new StringPair("cellspacing", "2");
		sp4[2] = new StringPair("border", "1");
		sp4[3] = new StringPair("style",
	"width: 60%; text-align: left; margin-left: auto; margin-right: auto");
		xos.startElement("table", sp4);
		xos.startElement("tbody");

		RawMessage rawmsg = msg.getRawMessage();
		xos.startElement("tr");
		String addr = getPM(rawmsg, GoesPMParser.DCP_ADDRESS);
		xos.writeElement("td", "DCP Address: " + addr);
		String fc = getPM(rawmsg, GoesPMParser.FAILURE_CODE);
		String qual = fc.indexOf('G') >= 0 ? "Good"
		    : fc.indexOf('?') >= 0 ? "Parity Err"
		        : fc.indexOf('M') >= 0 ? "Missing" : fc;
		xos.writeElement("td", "Message Quality: " + qual);
		xos.endElement("tr");

		xos.startElement("tr");
		String s = getPM(rawmsg, GoesPMParser.SIGNAL_STRENGTH);
		xos.writeElement("td", "Signal Strength: " + s + " dBM");
		int fo = 0;
		try
		{
			fo = rawmsg.getPM(GoesPMParser.FREQ_OFFSET).getIntValue();
		}
		catch (Exception ex)
		{
			fo = 0;
		}
		xos.writeElement("td", "Frequency Offset: " + fo + " (" + (fo * 50)
		    + " Hz)");
		xos.endElement("tr");

		xos.startElement("tr");
			try
			{
				s = getPM(rawmsg, GoesPMParser.CHANNEL)
			  	  + getPM(rawmsg, GoesPMParser.SPACECRAFT);
			}
			catch(Exception ex)
			{
				s = "unknown";
			}
			xos.writeElement("td", "GOES Channel: " + s);
			try { s = rawmsg.getPM(GoesPMParser.MESSAGE_LENGTH).toString(); }
			catch(Exception ex)
			{
				s = "unknown";
			}
			xos.writeElement("td", "Message Length: " + s);
		xos.endElement("tr");
		
		if (comesFromDcpMon)
		{
			String code = "";
			String drgsDescription = "";
			//build the path to check if drgsident.html file exists or not
			String path =  				
				EnvExpander.expand(
					"$DECODES_INSTALL_DIR/drgsident/drgsident.html");
			File htmlFile = new File(path);
			if (rawmsg.getPM(GoesPMParser.UPLINK_CARRIER) != null)
			{
				code = 
					rawmsg.getPM(GoesPMParser.UPLINK_CARRIER).getStringValue();
				//Find out the drgs code description for this "upLinkCarrier" 
				//in the DRGS receiver xml file
				drgsDescription = DrgsReceiverIo.findDescforCode(code);	
			}
			xos.startElement("tr");
			//If code empty or file drgsident.html does not exist
			//- do not show link
			if (code.equals("") || (!htmlFile.canRead()))
			{
				xos.writeElement("td", "DRGS code: " + code);	
			}
			else
			{	//just path the file name - we do not want to expose the
				//System file path to the public
				path = "drgsident.html";
				String link = "msg-html.cgi?msgfilename=" + path;
				xos.startElement("td");
				xos.writePCDATA("DRGS code: ");
				xos.writeElement("a", "href", link, code);
				xos.endElement("td");
			}
			xos.writeElement("td", "DRGS Description: " + drgsDescription);
			xos.endElement("tr");
		}
		
		// Add start and end carrier times if they are present. 
		SimpleDateFormat timeFmt = new SimpleDateFormat("HH:mm:ss.S");
		TimeZone utc = TimeZone.getTimeZone("UTC");
		timeFmt.setTimeZone(utc);
		Date cstart = getVarDateValue(
			rawmsg.getPM(GoesPMParser.CARRIER_START), null);
		Date cstop = getVarDateValue(
			rawmsg.getPM(GoesPMParser.CARRIER_STOP), null);
		if (cstart != null || cstop != null)
		{
			xos.startElement("tr");
			xos.writeElement("td", 
				"Carrier Start (UTC): " + 
				(cstart == null ? "(unknown)" : timeFmt.format(cstart)));
			xos.writeElement("td", 
				"Carrier Stop (UTC): " + 
				(cstop == null ? "(unknown)" : timeFmt.format(cstop)));
			xos.endElement("tr");
		}

		if (comesFromDcpMon)
		{
			xos.startElement("tr");
			StringBuffer sb = new StringBuffer();
			for(int i=0; i<fc.length(); i++)
			{
				char c = fc.charAt(i);
				if (c == (char)0 || c == ' ' || c == 'G' || c == '?')
					continue;
				if (sb.length()>0)
					sb.append(", ");
				sb.append(c + " (" + 
					DapsFailureCode.failureCode2string(c)+")");
			}
			xos.writeElement("td", "colspan", "2", "Additional Flags: "
				+ (sb.length() > 0 ? sb.toString():"(none)"));
			xos.endElement("tr");
		}

		xos.endElement("tbody");
		xos.endElement("table");
	}

		
	private void writeRaw(DecodedMessage msg, XmlOutputStream xos)
		throws IOException, OutputFormatterException
	{
		xos.writeElement("h3", "Raw Data:");
		StringPair sp3[] = new StringPair[3];
		sp3[0] = new StringPair("cellpadding", "4");
		sp3[1] = new StringPair("border", "1");
		sp3[2] = new StringPair("style",
	"width: 100%; font-family: monospace; text-align: left; margin-left: auto; "
			+ "margin-right: auto; border-collapse: collapse");
		xos.startElement("table", sp3);
		xos.startElement("tbody");
		xos.startElement("tr");
			xos.startElement("td");
			xos.startElement("big");
			xos.startElement("pre");
			xos.writeLiteral(
				wrapString(new String(msg.getRawMessage().getData())));
			xos.endElement("pre");
			xos.endElement("big");
			xos.endElement("td");
		xos.endElement("tr");
		xos.endElement("tbody");
		xos.endElement("table");
	}

	private void writeDecoded(DecodedMessage msg, XmlOutputStream xos)
		throws IOException, OutputFormatterException
	{
		// Construct column array for any time series with data in it.
		int numColumns = 0;
		Iterator tsit = msg.getAllTimeSeries();
		if (tsit == null)
			return;
		while(tsit.hasNext())
		{
			TimeSeries ts = (TimeSeries)tsit.next();
			if (ts.size() > 0)
				numColumns++;
		}
		if (numColumns == 0)
			return;

		columns = new Column[numColumns];
		int i=0;
		for(Iterator it = msg.getAllTimeSeries(); it.hasNext(); )
		{
			TimeSeries ts = (TimeSeries)it.next();
			if (ts.size() > 0)
				columns[i++] = new Column(ts);
		}

		Date firstTime = findNextDate();
		if (firstTime == null)
			return;
		xos.writeElement("h3", "Decoded Data:");
		StringPair sp3[] = new StringPair[4];
		sp3[0] = new StringPair("cellpadding", "2");
		sp3[1] = new StringPair("cellspacing", "2");
		sp3[3] = new StringPair("style",
			"text-align: center; width: 100%; border-collapse: collapse");
		xos.startElement("table", sp3);
		xos.startElement("thead", "style", 
			"background-color: rgb(210, 210, 210);");

		xos.startElement("th", "style", "width: 20%;");
			xos.writeElement("br", null);
			xos.writeElement("br", null);
			xos.writePCDATA(myTZ.getID());
			if (myTZ.inDaylightTime(firstTime))
				xos.writePCDATA("(Daylight)");
		xos.endElement("th");

		int pct = columns.length > 0 ? 80/columns.length : 80;
		String style="width: " + pct + "%";
		for(i=0; i<columns.length; i++)
		{
			xos.startElement("th", "style", style);
			String sn = columns[i].sensorName;
			if (sn != null && sn.length() > 0)
				xos.writePCDATA(sn);
			xos.writeElement("br", null);
			String dt = columns[i].dataType;
			if (dt != null || dt.length() > 0)
				xos.writePCDATA(dt);
			xos.writeElement("br", null);
			String eu = columns[i].euAbbr;
			if (eu != null || eu.length() > 0)
				xos.writePCDATA(eu);
			xos.writeElement("br", null);
			xos.endElement("th");
		}
		xos.endElement("thead");

		// Forth line is actual-site-name. Only use if a sensor uses it.
//		sb.setLength(0);
//		for(i=0; i<dateFormatString.length(); i++)
//			sb.append(' ');
//		sb.append(delimiter);
//		boolean doSensorSite = false;
//		for(i=0; i<columns.length; i++)
//		{
//			if (columns[i].siteName != null)
//			{
//				doSensorSite = true;
//				sb.append(TextUtil.strcenter(columns[i].siteName,
//					columns[i].colWidth));
//			}
//			else
//				sb.append(TextUtil.strcenter(" ", columns[i].colWidth));
//			sb.append(delimiter);
//		}
//		if (doSensorSite)
//			consumer.println(sb.toString());


		xos.startElement("tbody");

		Date d;
		while((d = findNextDate()) != null)
		{
			xos.startElement("tr");
			xos.writeElement("td", dateFormat.format(d));
			for(i=0; i<columns.length; i++)
			{
				String s;
				if (d.equals(columns[i].nextSampleTime()))
					s = TextUtil.setLengthLeftJustify(columns[i].nextSample(),
						columns[i].colWidth);
				else
					s = columns[i].getBlankSample();
				xos.writeElement("td", s);
			}
			xos.endElement("tr");
		}
		xos.endElement("tbody");
		xos.endElement("table");
	}

	/**
	 * Adds a link to platform metadata at the bottom of the page. 
	 */
	private void writeMetaData(DecodedMessage msg, XmlOutputStream xos)
		throws IOException, OutputFormatterException
	{
		if (metaDataCgi)
		{
			try
			{
				RawMessage rm = msg.getRawMessage();
				TransportMedium tm = rm.getTransportMedium();
				String id = tm.getMediumId();
				xos.writeElement("hr", null);
				xos.writeElement("p", null);
				xos.writePCDATA("Click ");
				xos.writeElement("a", "href", 
					"meta-data.cgi?dcpAddr=" + id,
					"here");
				xos.writePCDATA(" for platform meta-data.");
			}
			catch(UnknownPlatformException upex)
			{
				Logger.instance().debug2("Cannot write meta-data link: "+upex);
				return;
			}
		}
		else
		{
			if (xmlPlatformDir == null)
				return;
			Platform p = msg.getPlatform();
			if (p == null)
				return;
			xos.writeElement("hr", null);
			xos.writeElement("p", null);
			xos.writePCDATA("Click ");
			xos.writeElement("a", "href", 
				xmlPlatformDir + "/" + XmlDatabaseIO.makeFileName(p),
				"here");
			xos.writePCDATA(" for platform meta-data.");
		}
	}

	/** @return next date in any time series */
	private Date findNextDate()
	{
		Date ret = null;

		for(int i=0; i<columns.length; i++)
		{
			Date d = columns[i].nextSampleTime();
			if (d != null
			 && (ret == null || d.compareTo(ret) < 0))
				ret = d;
		}
		return ret;
	}

	/**
	 * This method is a kludge to account for the fact that HTML will not
	 * wrap a string that contains no whitespace. The Raw Data field might
	 * be one very long line with no whitespace. We insert spaces after 80
	 * characters if needed.
	 */
	private String wrapString(String ins)
	{
		StringBuffer sb = new StringBuffer(ins);
		int length = ins.length();

		int wordlen = 0;
		for(int i = 0; i<length; i++)
		{
			if (Character.isWhitespace(sb.charAt(i)))
				wordlen = 0;
			else if (++wordlen >= 80)
			{
				sb.insert(i, "<br>");
				length++;
				wordlen = 0;
			}
		}
		return sb.toString();
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
						+ ts.getSensor().configSensor.sensorNumber 
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
			while(sb.length() < colWidth-2)
				sb.append(' ');
			
			s = sb.toString();
			return s;
		}
	}

	private String getPM(RawMessage rawmsg, String field)
	{
		Variable v = rawmsg.getPM(field);
		return v == null ? "" : v.toString();
	}
	
	public static Date getVarDateValue(Variable v, Date dflt)
	{
		if (v == null)
			return dflt;
		try { return v.getDateValue(); }
        catch (NoConversionException ex) { return dflt; }
	}

	@Override
	public PropertySpec[] getSupportedProps()
	{
		return propSpecs;
	}

}


