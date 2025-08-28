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
package decodes.decoder;

import ilex.var.Variable;

import java.io.File;
import java.io.FileReader;
import java.io.LineNumberReader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.opendcs.utils.logging.OpenDcsLoggerFactory;
import org.slf4j.Logger;

import decodes.datasource.RawMessage;
import decodes.db.DecodesScript;

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
public class LatLonFunction	extends DecodesFunction 
{
	private static final Logger log = OpenDcsLoggerFactory.getLogger();
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
	 * @param msgData the message in a single string
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
			log.trace("Sensor Block at ({},{}): '{}'",
					  blockMatcher.start(),
					  blockMatcher.end(),
					  blockMatcher.group(0));
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
				log.atWarn().setCause(ex).log("Bad latitude '{}'", blockMatcher.group(0));
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
				log.atWarn().setCause(ex).log("Bad longitude '{}'", blockMatcher.group(0));
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
				log.info("parsed '{}' latitude={}, longitude={}",
						 blockMatcher.group(0), latitude, longitude);
			
		}
		
		// Finally eat any whitespace at the end.
		while(endProcessingIdx < msgData.length()
			&& Character.isWhitespace(msgData.charAt(endProcessingIdx)))
			endProcessingIdx++;
		
		if(!foundBlock)
		{
			log.trace("No match found.");
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
		log.trace("Processing '{}'", toParse);
		
		int finishIdx = parse(toParse, decmsg);
		dd.setBytePos(startPos + finishIdx);
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
					+ "'. Expected lat and lon integers, separated by comma.",ex);
			}
		}
	}
	
}
