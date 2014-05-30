/*
*  $Id$
*
*  $Log$
*  Revision 1.2  2014/05/28 13:09:29  mmaloney
*  dev
*
*  Revision 1.1.1.1  2014/05/19 15:28:59  mmaloney
*  OPENDCS 6.0 Initial Checkin
*
*  Revision 1.4  2012/09/10 23:55:39  mmaloney
*  Allow operation without platform record for OutputTs.
*
*  Revision 1.3  2011/05/19 17:11:46  mmaloney
*  use TextUtil.str2Boolean
*
*  Revision 1.2  2008/05/29 22:44:18  cvs
*  dev
*
*  Revision 1.3  2006/05/11 19:45:17  mjmaloney
*  dev
*
*  Revision 1.2  2005/03/21 14:26:21  mjmaloney
*  dev
*
*  Revision 1.1  2005/02/18 14:27:32  mjmaloney
*  Created.
*
*  Revision 1.19  2005/02/11 13:35:54  mjmaloney
*  dev
*
*  Revision 1.18  2004/09/06 13:42:00  mjmaloney
*  bug-fixes
*
*  Revision 1.17  2004/08/24 21:01:37  mjmaloney
*  added javadocs
*
*  Revision 1.16  2004/07/13 14:32:07  mjmaloney
*  Fixed bug in getting shef codes for several formatters.
*
*  Revision 1.15  2004/01/13 20:34:38  mjmaloney
*  Fix for full-shef-code option.
*
*  Revision 1.14  2004/01/13 17:19:51  mjmaloney
*  Bug-fixes on DECODES 6.0 beta
*
*  Revision 1.13  2003/12/23 20:10:17  mjmaloney
*  Mods to support -a (autoinstall) feature on dbimport.
*
*  Revision 1.12  2003/11/15 20:12:02  mjmaloney
*  Use accessor methods for transport medium type.
*
*  Revision 1.11  2002/12/03 21:43:07  mjmaloney
*  *** empty log message ***
*
*  Revision 1.10  2002/12/03 21:28:47  mjmaloney
*  Added UseNesdisId property.
*
*  Revision 1.9  2002/11/01 21:38:32  mjmaloney
*  Fixed null pointer bug if there was no sensor data type.
*
*  Revision 1.8  2002/11/01 21:35:09  mjmaloney
*  release prep
*
*  Revision 1.7  2002/05/19 13:02:44  mjmaloney
*  Final TimeZone mods
*
*  Revision 1.6  2002/05/19 00:22:18  mjmaloney
*  Deprecated decodes.db.TimeZone and decodes.db.TimeZoneList.
*  These are now replaced by the java.util.TimeZone class.
*
*  Revision 1.5  2002/04/18 12:19:40  mike
*  Fixed negative number parsing problem.
*  Fixed MISSING and ERROR value problem.
*
*  Revision 1.4  2002/04/05 21:25:14  mike
*  Implement sensor-site names in all formatters.
*  Fix time-ordering.
*  Fix DirectoryConsumer move functions.
*
*  Revision 1.3  2002/03/31 21:09:35  mike
*  bug fixes
*
*  Revision 1.2  2002/02/13 20:56:33  mike
*  Modify ShefFormatter to generate both .E and .A
*
*  Revision 1.1  2001/10/05 19:21:00  mike
*  Implemented ShefFormatter
*
*/
package decodes.consumer;

import java.util.Iterator;
import java.util.Properties;
import java.util.Date;
import java.text.SimpleDateFormat;
import java.text.NumberFormat;

import ilex.var.TimedVariable;
import ilex.var.IFlags;
import ilex.util.PropertiesUtil;
import ilex.util.Logger;
import ilex.util.TextUtil;

import decodes.db.*;
import decodes.decoder.DecodedMessage;
import decodes.decoder.TimeSeries;
import decodes.decoder.Sensor;
import decodes.datasource.RawMessage;
import decodes.datasource.UnknownPlatformException;

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
	private SimpleDateFormat dateFormat;
	private boolean dotAOnly;
	private java.util.TimeZone myTZ;
	private boolean useNesdisId;
	private boolean fullShefCode;
	private String defcode;
	private NumberFormat numberFormat;
	private PresentationGroup presGrp;

	/** default constructor */
	protected ShefFormatter()
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
		Logger.instance().log(Logger.E_DEBUG1,"useNesdisId property is '" + s + "'");
		if (s != null && 
			(s.equalsIgnoreCase("yes") || s.equalsIgnoreCase("true")
			 || s.equalsIgnoreCase("on")))
		{
			useNesdisId = true;
			Logger.instance().log(Logger.E_DEBUG1,"Using NESDIS IDs in SHEF");
		}
		else
			Logger.instance().log(Logger.E_DEBUG1,"Not using NESDIS IDs");

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
			platformSiteName = platform.getSiteName(false);
		}
		catch(UnknownPlatformException e)
		{
//			throw new OutputFormatterException(e.toString());
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
				platformName = sensor.getSensorSiteName();
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

			String recordingInt = "" + sensor.getRecordingInterval();

			String unitsId = " /DUE ";
			if (eu != null && eu.family != null
			 && eu.family.equalsIgnoreCase(Constants.unitFamilyMetric))
				unitsId = " /DUS ";

			if (!dotAOnly && platformType == 'I') // output .E ?
			{
				ts.sort();

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
						//sb.append("/" + ts.formattedSampleAt(i).trim());
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
			Logger.instance().warning("Cannot format variable '"
				+ tv.toString() + "': " + ex);
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
}

