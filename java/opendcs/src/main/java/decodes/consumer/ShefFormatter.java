/*
* Where Applicable, Copyright 2025 OpenDCS Consortium and/or its contributors
* 
* Licensed under the Apache License, Version 2.0 (the "License"); you may not
* use this file except in compliance with the License. You may obtain a copy
* of the License at
* 
*   http://www.apache.org/licenses/LICENSE-2.0
* 
* Unless required by applicable law or agreed to in writing, software 
* distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
* WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
* License for the specific language governing permissions and limitations 
* under the License.
*/
package decodes.consumer;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.Properties;

import org.opendcs.utils.logging.OpenDcsLoggerFactory;
import org.slf4j.Logger;

import decodes.datasource.RawMessage;
import decodes.datasource.UnknownPlatformException;
import decodes.db.Constants;
import decodes.db.DataPresentation;
import decodes.db.DataType;
import decodes.db.EngineeringUnit;
import decodes.db.Platform;
import decodes.db.PresentationGroup;
import decodes.db.Site;
import decodes.db.SiteName;
import decodes.db.TransportMedium;
import decodes.decoder.DecodedMessage;
import decodes.decoder.Sensor;
import decodes.decoder.TimeSeries;
import decodes.util.DecodesSettings;
import decodes.util.PropertySpec;
import ilex.util.PropertiesUtil;
import ilex.util.TextUtil;
import ilex.var.IFlags;
import ilex.var.TimedVariable;

/**
  This class formats decoded data in SHEF .A or .E lines.
  Properties honored:
  <ul>
	<li>dotAOnly - Forces output of .A lines even for regular interval data</li>
	<li>seconds - true/false indicating whether time stamps should include 
	    seconds</li>
	<li>century - true/false indicating whether time stamps should include 
	    century</li>
	<li>useNesdisId - Normally site names are used, set this to true to cause
	    the 8 hex-char NESDIS ID to be used instead</li>
	<li>FullShefCode - Set to true to cause full 7-char SHEF codes to be 
	    output rather than the 2-char PE.</li>
	<li>DefaultShefCode - default="xxIRZZZ", this is used to fill-in the
		missing parts of the SHEF code when FullSheffCode is specified.</li>
  </ul>
*/
public class ShefFormatter extends OutputFormatter
{
	private static final Logger log = OpenDcsLoggerFactory.getLogger();
	private SimpleDateFormat dateFormat;
	private boolean dotAOnly;
	private java.util.TimeZone myTZ;
	private boolean useNesdisId;
	private boolean fullShefCode;
	private String defcode;
	private NumberFormat numberFormat;
	private PresentationGroup presGrp;
	private String siteNameType = null;
	
	private PropertySpec propSpecs[] = 
	{		
		new PropertySpec("dotAOnly", PropertySpec.BOOLEAN,
			"(default=false) Set to true to force .A SHEF output, even for"
			+ " regular time series."),
		new PropertySpec("seconds", PropertySpec.BOOLEAN,
			"(default=true) Set to false to omit seconds from the time stamp."),
		new PropertySpec("century", PropertySpec.BOOLEAN,
			"(default=false) Set to true to include the century in the time stamp."),
		new PropertySpec("useNesdisId", PropertySpec.BOOLEAN,
			"(default=false) Set to true to use the GOES DCP Address in the"
			+ " output in place of the site name."),
		new PropertySpec("fullShefCode", PropertySpec.BOOLEAN,
			"(default=false) Set to true to include the full 7-char SHEF code"
			+ "in the output. Normally only the physical element code will be included."),
		new PropertySpec("sitenametype", PropertySpec.STRING,
			"Preferred site name type (default set in your decodes.properties)"),
		new PropertySpec("DefaultShefCode", PropertySpec.STRING,
			"(default=xxIRZZZ) If you are including the full shef code, this property allows "
			+ "you to specify the default residual fields. Leave two char place holder"
			+ " (xx in the default) for the Physical element code."),
	};


	/** default constructor */
	public ShefFormatter()
	{
		super();
		dateFormat = new SimpleDateFormat("yyMMdd  'DH'HHmmss");
		dotAOnly = false;
		useNesdisId = false;
		fullShefCode = false;
		defcode = "xxIRZZZ";
		numberFormat = NumberFormat.getNumberInstance();
		numberFormat.setGroupingUsed(false);
		presGrp = null;
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
		myTZ = tz;
		this.presGrp = presGrp;
		dateFormat.setTimeZone(tz);
		String s = PropertiesUtil.getIgnoreCase(rsProps, "dotAOnly");
		if (s != null)
			dotAOnly = TextUtil.str2boolean(s);
		
		boolean newDateFormat = false;
		boolean seconds = true;
		boolean century = false;
		s = PropertiesUtil.getIgnoreCase(rsProps, "seconds");
		if (s != null && 
			(s.equalsIgnoreCase("no") || s.equalsIgnoreCase("false")
			 || s.equalsIgnoreCase("off")))
		{
			newDateFormat = true;
			seconds = false;
		}
		s = PropertiesUtil.getIgnoreCase(rsProps, "century");
		if (s != null && 
			(s.equalsIgnoreCase("yes") || s.equalsIgnoreCase("true")
			 || s.equalsIgnoreCase("on")))
		{
			newDateFormat = true;
			century = true;
		}

		s = PropertiesUtil.getIgnoreCase(rsProps, "useNesdisId");
		if (s == null)
			s = PropertiesUtil.getIgnoreCase(rsProps, "useNesdisIds");
		log.debug("useNesdisId property is '{}'", s);
		if (s != null && 
			(s.equalsIgnoreCase("yes") || s.equalsIgnoreCase("true")
			 || s.equalsIgnoreCase("on")))
		{
			useNesdisId = true;
			log.debug("Using NESDIS IDs in SHEF");
		}
		else
			log.debug("Not using NESDIS IDs");

		s = PropertiesUtil.getIgnoreCase(rsProps, "FullShefCode");
		if (s != null)
			fullShefCode = TextUtil.str2boolean(s);

		s = PropertiesUtil.getIgnoreCase(rsProps, "DefaultShefCode");
		if (s != null)
			defcode = s;

		if (newDateFormat)
		{
			dateFormat = new SimpleDateFormat(
				(century ? "yy" : "") + "yyMMdd  'DH'HHmm"
				+ (seconds ? "ss" : ""));
		}
		
		siteNameType = DecodesSettings.instance().siteNameTypePreference;
		s = PropertiesUtil.getIgnoreCase(rsProps, "sitenametype");
		if (s != null)
		{
			siteNameType = s;
			log.info("ShefFormatter.init - will use siteNameType={}", siteNameType);
		}
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
		Platform platform = null;

		String platformSiteName = "unknown";
		char platformType = 'R';
		String dcpId = "unknown";
		try
		{
			tm = rawmsg.getTransportMedium();
			dcpId = tm.getMediumId();
			if (tm.getMediumType().equalsIgnoreCase(Constants.medium_GoesST))
				platformType = 'I';
			platform = rawmsg.getPlatform();

			// MJM 20151028 added siteNameType property
//			platformSiteName = platform.getSiteName(false);
			Site s = platform.getSite();
			SiteName sn = s.getName(siteNameType);
			if (sn == null)
			{
				sn = s.getPreferredName();
				log.info("Platform '{}' does not have site name with type '{}'. Will use {}",
						 platform.makeFileName(), siteNameType, sn.toString());

				if (log.isTraceEnabled())
				{
					log.trace("Available site names are:");
					for(SiteName tsn : s.getNameArray())
					{
						log.trace("    {}", tsn.toString());
					}
				}
			}
			platformSiteName = sn.getNameValue();
		}
		catch(UnknownPlatformException ex)
		{
			log.atError().setCause(ex).log("Unknown platform in format Message");
		}



		for(Iterator it = msg.getAllTimeSeries(); it.hasNext(); )
		{
			TimeSeries ts = (TimeSeries)it.next();
			if (ts.size() == 0)
				continue;
			

			Sensor sensor = ts.getSensor();
			String platformName;
			if (useNesdisId)
				platformName = dcpId;
			else
			{
				platformName = platformSiteName;
				if (sensor.getSite() != null
				 && platform != null
				 && sensor.getSite() != platform.getSite())
				{
					SiteName sn = sensor.getSite().getName(siteNameType);
					if (sn == null)
						sn = sensor.getSite().getPreferredName();
					platformName = sn.getNameValue();
				}
				if (platformName == null)
					platformName = platformSiteName;
			}

			EngineeringUnit eu = ts.getEU();
			DataType dt = sensor.getDataType(Constants.datatype_SHEF);
			String shefCode = dt != null ? dt.getCode() : "XX";
			if (fullShefCode)
			{
				StringBuffer scb = new StringBuffer(shefCode.trim());
				for(int i=scb.length(); i<7; i++)
					scb.append(defcode.charAt(i));
				shefCode = scb.toString();
			}

			ts.sort();

			
			//TODO
			// determine whether to do .A or .E
			// if sensor.recordingMode is F (fixed) and there are no gaps in the data, I can do .E
			// otherwise I have to do .A
			boolean doDotA = true;
			if (!dotAOnly
			 && sensor.getRecordingMode() == Constants.recordingModeFixed
			 && sensor.getRecordingInterval() > 0)
			{
				int sz = ts.size();
				long lastSecTime = 0L;
				doDotA = false;
				for(int i=0; i<sz; i++)
				{
					TimedVariable tv = ts.sampleAt(i);
					long secTime = tv.getTime().getTime() / 1000L;
					
					if (lastSecTime != 0L
					 && (secTime - lastSecTime != sensor.getRecordingInterval()))
					{
						doDotA = true;
						break;
					}
					lastSecTime = secTime;
				}
			}
			

			String unitsId = " /DUE ";
			if (eu != null && eu.family != null
			 && eu.family.equalsIgnoreCase(Constants.unitFamilyMetric))
				unitsId = " /DUS ";

			if (!doDotA) // Do .E
			{
				sb.setLength(0);
				sb.append(".E ");
				sb.append(platformName);
				sb.append(' ');
				sb.append(formatTime(ts.timeAt(0)));
				sb.append(unitsId);
				sb.append("/");
				sb.append(shefCode);
				sb.append("/ ");

				int recInt = sensor.getRecordingInterval();
				if (recInt < 60)
					sb.append("DIS+" + recInt);
				else if (recInt < 3600)
					sb.append("DIN+" + (recInt/60));
				else if (recInt < (3600*24))
					sb.append("DIH+" + (recInt/3600));
				else
					sb.append("DID+" + (recInt/(3600*24)));
				sb.append(" ");
				
				int sz = ts.size();
				for(int i=0; i<sz; i++)
				{
					TimedVariable tv = ts.sampleAt(i);
					if ((tv.getFlags() & (IFlags.IS_MISSING|IFlags.IS_ERROR))
						!= 0)
						sb.append("/+");
					else
						sb.append("/" + formatValue(tv, dt));
				}
				sb.append(" : ");
				sb.append(sensor.getName());
				if (eu != null && !eu.abbr.equals("unknown"))
						sb.append(" " + eu.abbr);

				consumer.println(sb.toString());
			}
			else // output .A
			{
				int sz = ts.size();
				for(int i=0; i<sz; i++)
				{
					TimedVariable tv = ts.sampleAt(i);

					if ((tv.getFlags() & (IFlags.IS_MISSING|IFlags.IS_ERROR))
						!= 0)
						continue;
	
					sb.setLength(0);
					sb.append(".A ");
					sb.append(platformName);
					sb.append(' ');
					sb.append(formatTime(tv.getTime()));
					sb.append(unitsId);

					sb.append("/");
					sb.append(shefCode);
					sb.append(' ');
					sb.append(formatValue(tv, dt));
					//sb.append(ts.formattedSampleAt(i));
					sb.append(" : ");
					sb.append(sensor.getName());
					if (eu != null && !eu.abbr.equals("unknown"))
						sb.append(" " + eu.abbr);
	
					consumer.println(sb.toString());
				}
			}
		}
		consumer.endMessage();
	}

	private String formatTime(Date d)
	{
		StringBuffer sb = new StringBuffer(dateFormat.format(d));
		sb.insert(7, getShefTimeZone(myTZ, d));
		return sb.toString();
	}


	private String formatValue(TimedVariable tv, DataType dt)
	{
		int numDec = 2;
		if (presGrp != null)
		{
			DataPresentation dp = presGrp.findDataPresentation(dt);
			if (dp != null)
			{
				numDec = dp.getMaxDecimals();
				if (numDec < 0 || numDec == Integer.MAX_VALUE)
					numDec = 2;
			}
		}
		numberFormat.setMinimumFractionDigits(numDec);
		numberFormat.setMaximumFractionDigits(numDec);
		try
		{
			double d = tv.getDoubleValue();
			return numberFormat.format(d).trim();
		}
		catch(Exception ex)
		{
		log.atWarn()
		   .setCause(ex)
		   .log("Cannot format variable '{}'", tv.toString());
			return "+";
		}
	}

	/**
	  Converts real time zone into SHEF designator.
	  @param tz the time zone.
	  @param d the date being converted (needed to determine if daylight time
		is in effect.
	  @return String SHEF time zone designator or "??" if error.
	*/
	public static String getShefTimeZone(java.util.TimeZone tz, Date d)
	{
		int offset = tz.getRawOffset() / 1000;
		boolean daylight = tz.useDaylightTime() && tz.inDaylightTime(d);
		String ret = "???";

		if (offset == 0)
			ret = "Z";
		else if (offset == -12600) // -3 1/2 hours
			ret = daylight ? "N" : "NS";
		else if (offset == -4 * 60 * 60)
			ret = daylight ? "AD" : "AS";
		else if (offset == -5 * 60 * 60)
			ret = daylight ? "ED" : "ES";
		else if (offset == -6 * 60 * 60)
			ret = daylight ? "CD" : "CS";
		else if (offset == -7 * 60 * 60)
			ret = daylight ? "MD" : "MS";
		else if (offset == -8 * 60 * 60)
			ret = daylight ? "PD" : "PS";
		else if (offset == -9 * 60 * 60)
			ret = daylight ? "LD" : "LS";
		else if (offset == -10 * 60 * 60)
			ret = daylight ? "H" : "HS";
		else if (offset == -11 * 60 * 60)
			ret = daylight ? "BD" : "BS";
		else if (offset == 8 * 60 * 60)
			ret = "J";
		else
			ret = "??";

		return ret;
	}
	
	@Override
	public PropertySpec[] getSupportedProps()
	{
		return propSpecs;
	}

}

