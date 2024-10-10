package decodes.decoder;

import ilex.util.Logger;
import ilex.util.TextUtil;
import ilex.var.Variable;

import java.io.File;
import java.io.FileReader;
import java.io.LineNumberReader;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import decodes.datasource.RawMessage;
import decodes.db.Constants;
import decodes.db.DataType;
import decodes.db.DecodesScript;
import decodes.util.DecodesSettings;

/**
 * This function parses ASCII latitude and longitude that are frequently
 * included at the end of messages, in the following format:
 * <pre>N35o48'31.80"W83o55'36.30"</pre>
 * This function uses Java regular expressions to parse the data.
 * The only additional information it needs is how to map the labels that
 * occur in the message to sensor data types.
 * <p>
 * This is done in the first of the following ways that succeeds:
 * <ul>
 *   <li>Look for an argument of the form <label>=<sensornum> that directly assigns
 *       a label to a sensor number</li>
 *   <li>Look for a sensor with data type standard="label" and code that matches
 *       the label in the message</li>
 *   <li>Go through each sensor and try to find a data type equivalent to the
 *       label that matches a data time assigned to the sensor</li>
 * </ul>
 * After executing, the cursor is left after the last sensor block that was
 * successfully parsed.
 */
public class LatLonFunction
	extends DecodesFunction 
{
	public static final String module = "LatLon";
	
	/** Pattern for a degrees minutes seconds, all inside capture groups */
	public static final String degMinSec = 
		"(\\d{1,3})o(\\d{1,2})'(\\d{1,2}\\.?\\d*)\"";

	/**
	 * Pattern for a entire lat/lon. Capture groups are:
	 * <ul>
	 *   <li>0: entire block</li>
	 *   <li>1: N or S</li>
	 *   <li>2: latitude degrees</li>
	 *   <li>3: latitude minutes</li>
	 *   <li>4: latitude seconds</li>
	 *   <li>5: E or W</li>
	 *   <li>6: longitude degrees</li>
	 *   <li>7: longitude minutes</li>
	 *   <li>8: longitude seconds</li>
	 * </ul>
	 */
	public static final String latLon = "^\\s*([NnSs])" + degMinSec + "([EeWw])" + degMinSec;

	private Pattern latLonPat = Pattern.compile(latLon);
	
	private boolean testMode = false;
	private int lineNumber = 1; // line number where first block starts
	int latSensorNum = -1;
	int lonSensorNum = -1;
	boolean storeInSite = false;
	
	public LatLonFunction()
	{
	}
	
	
	/**
	 * Parse the passed message.
	 * @param msg the message in a single string
	 * @return the index after the final character that was parsed.
	 */
	public int parse(String msgData, DecodedMessage decmsg)
	{
		Matcher blockMatcher = latLonPat.matcher(msgData);

		boolean foundBlock = false;
		int endProcessingIdx = 0;
		if (blockMatcher.find()) 
		{
			// Get the label, minute offset, minute index and sensor data
			foundBlock = true;
			trace("Sensor Block at (" + blockMatcher.start() + "," + blockMatcher.end() + "): "
				+ "'" + blockMatcher.group(0) + "'");
			endProcessingIdx = blockMatcher.end();
			
			char ns = blockMatcher.group(1).charAt(0);
			int sign = (ns == 'S' || ns == 's') ? -1 : 1;
			double latitude = 0.0;
			int deg, min;
			double sec;
			try
			{
				deg = Integer.parseInt(blockMatcher.group(2));
				min = Integer.parseInt(blockMatcher.group(3));
				sec = Double.parseDouble(blockMatcher.group(4));
				latitude = sign * (deg + (min/60.) + (sec/3600.));
			}
			catch(NumberFormatException ex)
			{
				// shouldn't happen
				warning("Bad latitude '" + blockMatcher.group(0) + "': " + ex);
			}
			char ew = blockMatcher.group(5).charAt(0);
			sign = (ew == 'W' || ns == 'w') ? -1 : 1;
			double longitude = 0.0;
			try
			{
				deg = Integer.parseInt(blockMatcher.group(6));
				min = Integer.parseInt(blockMatcher.group(7));
				sec = Double.parseDouble(blockMatcher.group(8));
				longitude = sign * (deg + (min/60.) + (sec/3600.));
			}
			catch(NumberFormatException ex)
			{
				// shouldn't happen
				warning("Bad latitude '" + blockMatcher.group(0) + "': " + ex);
			}
			
			if (storeInSite)
			{
				
			}
			else if (decmsg != null && latSensorNum >= 0 && lonSensorNum >= 0)
			{
				decmsg.addSample(latSensorNum, new Variable(latitude), lineNumber);
				decmsg.addSample(lonSensorNum, new Variable(longitude), lineNumber);
			}
			else
				info("parsed '" + blockMatcher.group(0)
					+ "' latitude=" + latitude + ", longitude=" + longitude);
			
		}
		
		// Finally eat any whitespace at the end.
		while(endProcessingIdx < msgData.length()
			&& Character.isWhitespace(msgData.charAt(endProcessingIdx)))
			endProcessingIdx++;
		
		if(!foundBlock)
		{
			trace("No match found.");
		}
		return endProcessingIdx;
	}
	
	@Override
	public DecodesFunction makeCopy()
	{
		return new LatLonFunction();
	}

	@Override
	public String getFuncName()
	{
		return module;
	}

	@Override
	public void execute(DataOperations dd, DecodedMessage decmsg) 
		throws DecoderException
	{
		// Get from here to end of message into a String
		int startPos = dd.getBytePos();
		lineNumber = dd.getCurrentLine();
		StringBuilder sb = new StringBuilder();
		while(dd.moreChars())
		{
			sb.append((char)dd.curByte());
			dd.forwardspace();
		}
		String toParse = sb.toString();
		trace("Processing '" + toParse + "'");
		
		int finishIdx = parse(toParse, decmsg);
		dd.setBytePos(startPos + finishIdx);
	}

	private void trace(String s)
	{
		if (testMode)
			System.out.println(module + " " + s);
		else
			Logger.instance().debug3(module + " " + s);
	}

	private void info(String s)
	{
		if (testMode)
			System.out.println(module + " " + s);
		else
			Logger.instance().info(module + " " + s);
	}

	private void warning(String s)
	{
		if (testMode)
			System.out.println(module + " " + s);
		else
			Logger.instance().warning(module + " " + s);
	}

	
	/**
	 * The argument contains a map of message sensor labels to DECODES sensor
	 * numbers, separated by comma.
	 * <p>
	 *    label=number [, label=number]*
	 * <p>
	 * where label is the label as it appears in the message and number is an integer
	 * matching a DECODES sensor number.
	 * <p>
	 * The following additional arguments are processed:
	 * <ul>
	 *   <li>processMOFF=true|false   - use this to override the decodes.properties setting</li>
	 * </ul>
	 */
	@Override
	public void setArguments(String argString, DecodesScript script) 
		throws ScriptFormatException
	{
		argString = argString.trim();
		if (argString.equalsIgnoreCase("site"))
			storeInSite = true;
		else if (argString.length() > 0)
		{
			int comma = argString.indexOf(',');
			if (comma == -1)
				throw new ScriptFormatException(module
					+ " Bad argument '" + argString
					+ "'. Expected the word 'site' or lat,lon sensor numbers.");
			try
			{
				latSensorNum = Integer.parseInt(argString.substring(0,comma));
				lonSensorNum = Integer.parseInt(argString.substring(comma+1));
			}
			catch(Exception ex)
			{
				throw new ScriptFormatException(module
					+ " Bad argument '" + argString
					+ "'. Expected lat and lon integers, separated by comma.");
			}
		}
	}
	
	public void setTestMode(boolean testMode)
	{
		this.testMode = testMode;
	}
	
	/**
	 * Main method for test. Pass name of a file containing ascii self-describing
	 * messages, one per line.
	 * @param args single argument filename
	 * @throws Exception
	 */
	public static void main(String[] args)
		throws Exception
	{
		File file = new File(args[0]);
		LineNumberReader lnr = new LineNumberReader(
			new FileReader(file));
		String line;
		AsciiSelfDescFunction asdf = new AsciiSelfDescFunction();
		LatLonFunction llf = new LatLonFunction();
		asdf.setTestMode(true);
		llf.setTestMode(true);
		
		while((line = lnr.readLine()) != null)
		{
			System.out.println("=============");
			System.out.println(line);
			System.out.println();
			RawMessage rawMsg = new RawMessage(line.getBytes());
			rawMsg.setHeaderLength(37);
			DataOperations dataOps = new DataOperations(rawMsg);
			// If the high-data rate status byte is present, skip it.
			if ((char)dataOps.curByte() != ':')
				dataOps.forwardspace();
			asdf.execute(dataOps, null);
			llf.execute(dataOps, null);
			System.out.println("Processed " + 
				(dataOps.getBytePos()+rawMsg.getHeaderLength()) + " characters out of " + 
				line.length() + ".");
			
//			int len = asdp.parse(line.substring(37), null);
//			System.out.println("Processed " + (len+37) + " characters out of " + line.length() + ".");
		}
	}


}
