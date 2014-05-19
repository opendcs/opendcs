/*
* $Id$
*
* $Log$
* Revision 1.2  2012/05/24 15:47:15  mmaloney
* Fixed date/time processing for DH, etc.
*
* Revision 1.1  2011/09/27 01:24:48  mmaloney
* Enhancements for SHEF and NOS Decoding.
*
* Revision 1.2  2011/09/21 18:27:08  mmaloney
* Updates to NOS decoding.
*
* Revision 1.1  2011/08/26 19:49:34  mmaloney
* Implemented the NOS decoders.
*
*/
package decodes.decoder;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Iterator;
import java.util.StringTokenizer;
import java.util.TimeZone;

import ilex.util.Logger;
import ilex.var.IFlags;
import ilex.var.TimedVariable;
import ilex.var.Variable;
import decodes.cwms.CwmsFlags;
import decodes.datasource.RawMessage;
import decodes.datasource.ShefPMParser;
import decodes.db.ConfigSensor;
import decodes.db.Constants;
import decodes.db.DataType;
import decodes.db.DecodesScript;
import decodes.db.Platform;
import decodes.db.PlatformConfig;
import decodes.decoder.DataOperations;
import decodes.decoder.DecodedMessage;
import decodes.decoder.DecodesFunctionOperation;
import decodes.tsdb.IntervalIncrement;
import decodes.tsdb.VarFlags;

/** Handles SHEF. */
public class ShefProcess
	extends DecodesFunctionOperation 
{
	public static final String module = "ShefProcess";
	private boolean matchShefPE = false;
	private SimpleDateFormat debugSdf = new SimpleDateFormat("yyyy/MM/dd-HH:mm:ss");
	private Calendar cal = null;
	private char unitsFamily = 'E';
	private RawMessage rawmsg = null;
	private String tzID = "UTC";
	private int qualityFlags = 0;
	private String duration = null;
	private int sensorNumber = -1;
	private Platform platform = null;
	private IntervalIncrement intinc = null;
	private DecodedMessage decmsg = null;


	// Where we are in the line. Used to ignore comments and bad lines
	enum LineContext
	{
		StartOfLine, FormatType, FormatExt, IgnoreLine, InComment, Process
	};
	// Where we are in the SHEF message, which may span several lines
//	enum ParseState 
//	{
//		ObsTime, CreateDate,
//		Units, DataQual, Duration, 
//		EParmCode, ETimeInterval, EValue, // Used only for .E format lines
//		AParmCode, AValue,                // Used only for .A format lines
//	};
	
	public ShefProcess()
	{
		super();
	}
	
	public DecodesFunctionOperation copy()
	{
		return new ShefProcess();
	}

	public void enableMatchShefPE() { matchShefPE = true; }
	
	public String getFunctionName() { return module; }

	/**
	 * No arguments expected
	 */
	public void setArguments(String argString)
	{
	}
	
	public void execute(DataOperations dd, DecodedMessage msg)
		throws DecoderException
	{
		this.decmsg = msg;
		LineContext lineContext = LineContext.Process;
//		ParseState parseState = ParseState.LocId;

		platform = msg.getPlatform();
		if (platform == null)
			throw new DecoderException(module + " function cannot be called with null platform.");
		PlatformConfig config = platform.getConfig();
		if (config == null)
			throw new DecoderException(module + " function cannot be called with null config.");

		rawmsg = msg.getRawMessage();
		
		Variable v = rawmsg.getPM(ShefPMParser.PM_MESSAGE_TYPE);
		if (v == null || v.getStringValue().length() == 0)
			throw new DecoderException("Cannot process SHEF without " + ShefPMParser.PM_MESSAGE_TYPE
				+ " defined. Is the header type set to SHEF?");
		char formatType = v.getStringValue().charAt(0);
		boolean isRevised = false;

		// Set up calendar to process times as we step through message.
		v = rawmsg.getPM(ShefPMParser.PM_TIMEZONE); // ShefPMParser sets this.
		if (v != null)
			tzID = v.toString();
		
		// Default is english units
		unitsFamily = 'E';
		duration = null;
		sensorNumber = -1;

		// All times will be completely assigned, so set timer to complete.
		// Otherwise, after calling this method, the code will attempt to 'fixup'
		// times.
		debugSdf.setTimeZone(TimeZone.getTimeZone(tzID));
		msg.getTimer().setTimeZoneName(tzID);
		msg.getTimer().setComplete(msg.getMessageTime());
		cal = msg.getTimer().getCalendar();

		qualityFlags = shefQuality2Flags('Z'); // default is no qual-checks performed
		
		String msgData = new String(rawmsg.getData());
		String shefdata = msgData.substring(rawmsg.getHeaderLength());
		Logger.instance().debug3("SHEF Message = '" + shefdata + "'");

		boolean error = false;
		StringBuilder field = new StringBuilder();
		int fieldStart = -1;
		for(int idx = 0; idx < shefdata.length() && !error; idx++)
		{
			char c = shefdata.charAt(idx);
			
			switch(lineContext)
			{
			case StartOfLine:
				lineContext = (c == '.' ? LineContext.FormatType : LineContext.IgnoreLine);
				if (lineContext == LineContext.IgnoreLine)
					debug(idx, "Line start without period, will ignore line.");
				continue;
			case IgnoreLine:
				if (c == '\n')
				{
					lineContext = LineContext.StartOfLine;
					debug(idx, "End of line reached.");
				}
				continue;
			case FormatType:
				if (c == 'A' || c == 'E')
				{
					if (formatType != 'x' && c != formatType)
					{
						warning(idx, "Continuation of format '" + formatType + "' with type '"
							+ "' -- invalid.");
						error = true;
					}
					formatType = c;
					lineContext = LineContext.FormatExt;
					debug(idx, "Got type = '" + c + "' expecting extension");
				}
				else
				{
					warning(idx, "Unexpected format type '" + c + "' -- ignoring line.");
					lineContext = LineContext.IgnoreLine;
				}
				continue;
			case FormatExt:
				if (c == 'R')
					isRevised = true;
				else if (c == ' ')
				{
					lineContext = LineContext.Process;
					field.setLength(0);
				}
				else if (!Character.isDigit(c))
					warning(idx, "Unexpexted continuation character '" + c + "' -- ignored.");
				continue;
			case InComment:
				if (c == ':')
				{
					lineContext = LineContext.Process;
					field.setLength(0);
					debug(idx, "end of comment");
				}
				else if (c == '\n')
				{
					lineContext = LineContext.StartOfLine;
					debug(idx, "end of line -- comment closed");
				}
				continue;
			case Process:
				if (c == ':')
				{
					lineContext = LineContext.InComment;
					debug(idx, "start of commnet");
					continue;
				}
				else if (Character.isWhitespace(c) || c == '/')
				{
					if (field.length() == 0)
						continue; // ignore contiguous whitespace before field starts
					else
					{
						processField(fieldStart, idx, field.toString(), c);
						field.setLength(0);
					}
					if (c == '\n')
					{
						lineContext = LineContext.StartOfLine;
						debug(idx, "end of line");
					}
					
				}
				else
				{
					if (field.length() == 0)
						fieldStart = idx;
					field.append(c);
				}
			}
		}
	}
	
	/**
	 * Process a field from the message
	 * @param field the blank or slash-delimited field
	 * @param delim the delimiter that ended the field
	 */
	private void processField(int fieldStart, int fieldEnd, 
		String field, char delim)
	{
		debug(fieldStart, "processField '" + field + "' delim='" + delim + "'");
		field = field.toUpperCase();
		if (field.startsWith("DS"))
		{
			setTime(cal, field.substring(2));
		}
		else if (field.startsWith("DN"))
		{
			field = field.substring(2);
			while(field.length() < 4)
				field = field + "0";
			setTime(cal, field);
		}
		else if (field.startsWith("DH"))
		{
			field = field.substring(2);
			while(field.length() < 6)
				field = field + "0";
			setTime(cal, field);
		}
		else if (field.startsWith("DD"))
		{
			field = field.substring(2);
			while(field.length() < 8)
				field = field + "0";
			setTime(cal, field);
		}
		else if (field.startsWith("DM"))
		{
			field = field.substring(2);
			if (field.length() == 2)
				field = field + "01"; // Add day 1 of month
			while(field.length() < 10)
				field = field + "0";
			setTime(cal, field);
		}
		else if (field.startsWith("DY"))
		{
			field = field.substring(2);
			if (field.length() == 2)
				field = field + "0101"; // Add Jan 1
			else if (field.length() == 4)
				field = field + "01";
			while(field.length() < 12)
				field = field + "0";
			setTime(cal, field);
		}
		else if (field.startsWith("DT"))
		{
			field = field.substring(2);
			if (field.length() == 2)
				field = field + "000101"; // Add Jan 1, 1st yr of century
			else if (field.length() == 4)
				field = field + "0101";   // Add jan 1
			else if (field.length() == 6)
				field = field + "01";     // Add 1st day of month
			while(field.length() < 14)
				field = field + "0";
			setTime(cal, field);
		}
		else if (field.startsWith("DJ"))
		{
			setTime(cal, field.substring(2));
		}
		else if (field.startsWith("DR"))
		{
			IntervalIncrement drinc = shefInc2IntInc(field.substring(2));
			if (drinc != null)
				cal.add(drinc.getCalConstant(), drinc.getCount());
			else
				Logger.instance().warning("Invalid Date Relative '" + field + "'");
		}
		else if (field.startsWith("DC"))
		{
			GregorianCalendar creationCal = new GregorianCalendar(TimeZone.getTimeZone(tzID));
			creationCal.setTime(cal.getTime());
			setTime(creationCal, field.substring(2));
			rawmsg.setPM("CREATION_TIME", new Variable(creationCal.getTime()));
		}
		else if (field.startsWith("DU") && field.length() > 2)
			unitsFamily = field.charAt(2);
		else if (field.startsWith("DQ") && field.length() > 2)
			qualityFlags = shefQuality2Flags(field.charAt(2));
		else if (field.startsWith("DV"))
		{
			duration = shefDur2Cwms(field.substring(2));
			if (sensorNumber != -1)
				platform.getPlatformSensor(sensorNumber).getProperties().setProperty(
					"CwmsDuration", duration);
		}
		else if (field.startsWith("DI"))
		{
			intinc = shefInc2IntInc(field.substring(2));
			if (intinc == null)
				Logger.instance().warning("Invalid Interval '" + field + "'");
		}
		else if (Character.isLetter(field.charAt(0)))
		{
			// Could be shef code by itself for .E
			// or could be "code<sp>value" for .A
			String shefcode = field;
			sensorNumber = getSensor(shefcode, platform.getConfig());
			if (duration != null)
				platform.getPlatformSensor(sensorNumber).getProperties().setProperty(
					"CwmsDuration", duration);
		}
		else if (Character.isDigit(field.charAt(0)) || ".-+".indexOf(field.charAt(0)) >= 0)
		{
			// get the sensor value optionally followed by quality
			char c = field.charAt(field.length()-1);
			int qf = 0;
			if (Character.isLetter(c))
			{
				qf = shefQuality2Flags(c);
				field = field.substring(0, field.length()-1);
			}
			else
				qf = qualityFlags;
				
			Variable v = null;
			try
			{
				v = new Variable(Double.parseDouble(field));
				v.setFlags(qf);
			}
			catch(NumberFormatException ex)
			{
				if (!field.equals("M"))
					Logger.instance().warning("Invalid data value '" + field + "'");
				v = new Variable(0.0);
				v.setFlags(IFlags.IS_MISSING);
			}
				
			if (sensorNumber == -1)
			{
				Logger.instance().warning("Value '" + field + "' discarded - no sensor found.");
			}
			else // Assign it to the sensor's time-series
			{
				TimedVariable tv = decmsg.addSample(sensorNumber, v, 0);
				if (tv != null && DecodesScript.trackDecoding)
				{
					DecodedSample ds = new DecodedSample(this, 
						fieldStart, fieldEnd,
						tv, decmsg.getTimeSeries(sensorNumber));
					formatStatement.getDecodesScript().addDecodedSample(ds);
				}

			}
				
			// Adjust the clock according to the interval
			if (intinc != null)
				cal.add(intinc.getCalConstant(), intinc.getCount());
		}
	}


	private void warning(int pos, String msg)
	{
		Logger.instance().warning(module + " postion=" + pos + " " + msg);
	}
	
	private void debug(int pos, String msg)
	{
		Logger.instance().debug3(module + " position=" + pos + " " + msg);
	}

	private void setTime(Calendar cal, String obsTime)
	{
		try
		{
			int x;
			switch(obsTime.length())
			{
			case 14: // ccyymmddhhnnss
				x = Integer.parseInt(obsTime.substring(0,4));
				cal.set(Calendar.YEAR, x);
				setTime(cal, obsTime.substring(4));
				break;
			case 12: // yymmddhhnnss
				x = Integer.parseInt(obsTime.substring(0,2));
				if (x < 31) x += 2000;
				else x += 1900;
				cal.set(Calendar.YEAR, x);
				obsTime = obsTime.substring(2);
				// no break -- fall through
			case 10: // mmddhhnnss
				x = Integer.parseInt(obsTime.substring(0,2));
				cal.set(Calendar.MONTH, x);
				obsTime = obsTime.substring(2);
				// no break -- fall through
			case 8: // ddhhnnss
				x = Integer.parseInt(obsTime.substring(0,2));
				cal.set(Calendar.DAY_OF_MONTH, x);
				obsTime = obsTime.substring(2);
				// no break -- fall through
			case 6: // hhnnss
				x = Integer.parseInt(obsTime.substring(0,2));
				cal.set(Calendar.HOUR_OF_DAY, x);
				obsTime = obsTime.substring(2);
				// no break -- fall through
			case 4: // nnss
				x = Integer.parseInt(obsTime.substring(0,2));
				cal.set(Calendar.MINUTE, x);
				obsTime = obsTime.substring(2);
				// no break -- fall through
			case 2: // ss
				x = Integer.parseInt(obsTime.substring(0,2));
				cal.set(Calendar.SECOND, x);
				break;
			case 7: // ccyyddd
				x = Integer.parseInt(obsTime.substring(0,4));
				cal.set(Calendar.YEAR, x);
				setTime(cal, obsTime.substring(4));
				break;
			case 5: // yyddd
				x = Integer.parseInt(obsTime.substring(0,2));
				if (x < 31) x += 2000;
				else x += 1900;
				cal.set(Calendar.YEAR, x);
				obsTime = obsTime.substring(2);
				// no break -- fall through
			case 3: // ddd
				x = Integer.parseInt(obsTime.substring(0,3));
				cal.set(Calendar.DAY_OF_YEAR, x);
				break;
			}
		}
		catch(NumberFormatException ex)
		{
			Logger.instance().warning("Invalid observation time '" + obsTime
				+ "' -- ignored.");
		}
		Logger.instance().debug3("observation time set to "
			+ debugSdf.format(cal.getTime()));
	}
	
	private int shefQuality2Flags(char q)
	{
		// This is based on table 10 in the SHEF spec
		// We are using the CWMS quality bits to store info.
		switch(q)
		{
		case 'G': // Good
		case 'Z': // no QC performed (this is the default)
			return VarFlags.TO_WRITE;
		case 'S': // Screened
			return VarFlags.TO_WRITE | CwmsFlags.SCREENED;
		case 'V': // Verified (i.e. screening passed)
		case 'P': // Passed
			return VarFlags.TO_WRITE | CwmsFlags.SCREENED | CwmsFlags.VALIDITY_OKAY;
		case 'M': // Manual edit
		case 'W': // Withheld == Manual edit
			return VarFlags.TO_WRITE | CwmsFlags.SCREENED | CwmsFlags.REPLACEMENT_MANUAL;
		case 'F': // Flagged
		case 'Q': // Questionable
		case 'B': // Bad
			return VarFlags.TO_WRITE | CwmsFlags.SCREENED | CwmsFlags.VALIDITY_QUESTIONABLE;
		case 'R': // Rejected (should be treated as missing)
			return VarFlags.TO_WRITE | CwmsFlags.SCREENED | CwmsFlags.VALIDITY_REJECTED
				| IFlags.IS_MISSING;
		case 'E': // Estimated
		case 'D': // Partial
		case 'L': // Lumped = a special case of estimated
			return VarFlags.TO_WRITE | CwmsFlags.SCREENED | CwmsFlags.REPLACEMENT_AUTOMATIC;
		default:
			return VarFlags.TO_WRITE;
		}
	}
	
	/**
	 * Remove comments (everything from colon to end of line, or colon to 
	 * another colon)
	 * Concatenates continuation lines into one big line, removing the .E[n] 
	 * or .A[n] specifiers. The result is one big line with no comments.
	 * Collapse multiple adjacent whitespace (\t\n\r or space) to a single space.
	 * Remove all whitespace on either side of a slash: This should just leave 
	 * a single space in .A messages between /pc vvv/.  .E messages require no spaces.
	 * @param rawmsg
	 * @return
	 * @throws DecoderException
	 */
	private String preprocess(RawMessage rawmsg)
		throws DecoderException
	{
		StringBuilder sb = new StringBuilder();

		byte data[] = rawmsg.getData();
		boolean inComment = false;
		boolean inContinuationFlag = false; // True when we're in .En or .An
		char prevChar = '\n'; // start as if previous line was newline
		char c = '\0';
		boolean lastTokenSlash = false;
		boolean ignoreLine = false; // Set to true when first column is not a period.
		boolean blockStarted = false;
		for(int idx = rawmsg.getHeaderLength(); idx < data.length; idx++, prevChar = c)
		{
			c = (char)data[idx];
Logger.instance().debug3("   shefProcess  prevChar=" + (int)prevChar + ", c=" + (int)c
+ ", ignoreLine=" + ignoreLine + ", c=" + c);
			if (prevChar == '\n' && c != '.')
				ignoreLine = true;
			if (ignoreLine)
			{
				if (c == '\n')
					ignoreLine = false;
				continue;
			}

			if (inComment)
			{
				if (c == ':' || c == '\n')
					inComment = false;
				continue;
			}
			else if (c == ':')
			{
				inComment = true;
				continue;
			}
					
			// Collapse multiple WS to 1 space.
			// don't allow space after a '/'
			if (Character.isWhitespace(c))
			{
				if (!Character.isWhitespace(prevChar) && prevChar != '/')
					sb.append(' ');
				if (inContinuationFlag && !Character.isDigit(prevChar))
				{
					// uh-oh. This is not a continuation. It's a start of
					// a new shef message. Bail out.
					break;
				}
				inContinuationFlag = false;
				if (c == '\n')
				{
					ignoreLine = false;
				}
				continue;
			}
			// Start of continuation-line sequence .E1 or .A1, etc.
			if (c == '.' && prevChar == '\n') // dot at start of line
			{
				if (!blockStarted)
					blockStarted = true;
				else
					inContinuationFlag = true;
			}
			if (inContinuationFlag)
				continue;
			
			// Don't allow WS on either side of slash
			if (c == '/' && Character.isWhitespace(prevChar))
				sb.setLength(sb.length()-1);

			// Getting here means this is a non-whitespace char!
			
			// Weird case for continuation lines. Sometimes the slash is present
			// sometimes it is not. Add it.
			if (prevChar == '\n' && !lastTokenSlash && c != '/')
				sb.append('/');
				
			lastTokenSlash = (c == '/');
			sb.append(c);
		}
		
		if (sb.charAt(0) == '/')
			sb.deleteCharAt(0);
		return sb.toString();
	}
	private String shefDur2Cwms(String dur)
	{
		if (dur.length() < 2)
			return null;
		int inc = 0;
		String durs;
		try
		{
			String s = dur.substring(1);
			if (s.startsWith("+"))
				s = s.substring(1);
			inc = Integer.parseInt(s);
			durs = "" + inc;
		}
		catch(NumberFormatException ex)
		{
			Logger.instance().warning("Invalid duration '" + dur + "' -- ignored.");
			return null;
		}
		
		switch(dur.charAt(0))
		{
		case 'S':
			durs += "Second";
			break;
		case 'N':
			durs += "Minute";
			break;
		case 'H':
			durs += "Hour";
			break;
		case 'D':
			durs += "Day";
			break;
		case 'M':
			durs += "Month";
			break;
		case 'Y':
			durs += "Year";
			break;
		default:
			return null;
		}
		if (inc > 1)
			durs = durs + "s";
		return durs;
	}

	private IntervalIncrement shefInc2IntInc(String incr)
	{
		if (incr.length() < 2)
			return null;
		int count = 1;
		try
		{
			String s = incr.substring(1);
			if (s.startsWith("+"))
				s = s.substring(1);
			count = Integer.parseInt(s);
		}
		catch(NumberFormatException ex)
		{
			Logger.instance().warning("Invalid interval '" + incr + "' -- ignored.");
			return null;
		}
		
		switch(incr.charAt(0))
		{
		case 'S': return new IntervalIncrement(Calendar.SECOND, count);
		case 'N': return new IntervalIncrement(Calendar.MINUTE, count);
		case 'H': return new IntervalIncrement(Calendar.HOUR_OF_DAY, count);
		case 'D': return new IntervalIncrement(Calendar.DAY_OF_MONTH, count);
		case 'E': // end of month - not sure how to handle this.
		case 'M': return new IntervalIncrement(Calendar.MONTH, count);
		case 'Y': return new IntervalIncrement(Calendar.YEAR, count);
		default:
			return null;
		}
	}

	/**
	 * Find a match for the full shef code, failing that, find a sensor with
	 * a SHEF-PE that matches. Return the sensor number.
	 * @param shefcode the shefcode in the shef file
	 * @param config the platform configuration
	 * @return the sensor number, or -1 if not found.
	 */
	private int getSensor(String shefcode, PlatformConfig config)
	{
		// First look for an exact match in SHEFCODE.
		for(Iterator<ConfigSensor> csit = config.getSensors(); csit.hasNext();)
		{
			ConfigSensor cs = csit.next();
			DataType dt = cs.getDataType(Constants.datatype_SHEFCODE);
			if (dt != null && dt.getCode().equalsIgnoreCase(shefcode))
				return cs.sensorNumber;
		}
		
		// Now look for an exact match in SHEF-PE code
		for(Iterator<ConfigSensor> csit = config.getSensors(); csit.hasNext();)
		{
			ConfigSensor cs = csit.next();
			DataType dt = cs.getDataType(Constants.datatype_SHEF);
			if (dt != null && dt.getCode().equalsIgnoreCase(shefcode))
				return cs.sensorNumber;
		}

		// Finally, look for a SHEF-PE code that matches the start of the
		// provided SHEF-code, but only if this feature is enabled.
		if (!matchShefPE)
			return -1;
		for(Iterator<ConfigSensor> csit = config.getSensors(); csit.hasNext();)
		{
			ConfigSensor cs = csit.next();
			DataType dt = cs.getDataType(Constants.datatype_SHEF);
			if (dt != null && shefcode.startsWith(dt.getCode().toUpperCase()))
				return cs.sensorNumber;
		}
		
		return -1;
	}
}
