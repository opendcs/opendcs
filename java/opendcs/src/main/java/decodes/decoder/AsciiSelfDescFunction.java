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

import ilex.util.TextUtil;
import ilex.var.IFlags;
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

import org.opendcs.utils.logging.OpenDcsLoggerFactory;
import org.slf4j.Logger;

import decodes.datasource.RawMessage;
import decodes.db.Constants;
import decodes.db.DataType;
import decodes.db.DecodesScript;
import decodes.util.DecodesSettings;

/**
 * This function parses ASCII self-describing GOES messages.
 * There are many DCPs that report in this format:
 * <pre>  (:<label> <moff> #<mint> (<value>)*)* </pre>
 * This function uses Java regular expressions to parse the message.
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
public class AsciiSelfDescFunction extends DecodesFunction
{
	private static final Logger log = OpenDcsLoggerFactory.getLogger();
	public static final String module = "AsciiSelfDesc";

	/** Pattern for a floating point number optionally preceded by a sign */
	public static final String floatNumber = "[+-]?\\d*\\.?[\\dM/]+";

	/** Pattern for a float number inside a capture group */
	public static final String capFloatNumber = "(" + floatNumber + ")";

	/**
	 * Pattern string for sensor block in a self-describing ascii message.
	 * Skip whitespace up until the next ':' and then parse the block.
	 * <ul>
	 *   <li>0: entire block</li>
	 *   <li>1: sensor label</li>
	 *   <li>2: minute offset</li>
	 *   <li>3: minute interval</li>
	 *   <li>4: String containing one or more sensor values</li>
	 * </ul>
	 */
	public static final String sensorBlock =
		"^\\s*:(\\w+)\\s+(\\d+)\\s*#(\\d+)((?:\\s*" + floatNumber + ")+)";
	// Note -- this is too permissive:
	//		"[^:]*:(\\w+)\\s+(\\d+)\\s*#(\\d+)([^:]*)";

	/**
	 * Many have a BATTLOAD block at the end of the message with an optional
	 * MOFF (typically zero) and a single battery load sample.
	 */
	public static final String battloadBlock =
		"^\\s*:(\\w+)\\s+(?:\\d+\\s+)?" + capFloatNumber;

	private Pattern sensorBlockPat = Pattern.compile(sensorBlock);
	private Pattern capFloatNumberPat = Pattern.compile(capFloatNumber);
	private Pattern battloadBlockPat = Pattern.compile(battloadBlock);
	private HashMap<String, Integer> labelSensorNumMap = new HashMap<String, Integer>();

	private boolean testMode = false;
	private int lineNumber = 1; // line number where first block starts
	private boolean processMOFF = DecodesSettings.instance().asciiSelfDescProcessMOFF;

	public AsciiSelfDescFunction()
	{
	}


	/**
	 * Parse the passed message.
	 * @param msg the message in a single string
	 * @return the index after the final character that was parsed.
	 */
	public int parse(String msgData, DecodedMessage decmsg)
	{
		int numBlocks = 0;
		int endProcessingIdx = 0;

		// The sensor block pattern must be at the beginning of data so that
		// we don't eat characters not part of a sensor block.
		// So recreate the block matcher for each time through.
		String parseData = msgData;
		for(Matcher blockMatcher = sensorBlockPat.matcher(parseData);
			blockMatcher.find();
			parseData = msgData.substring(endProcessingIdx),
			blockMatcher = sensorBlockPat.matcher(parseData))
		{
			numBlocks++;

			// Get the label, minute offset, minute index and sensor data
			log.trace("Sensor Block at {}, block extent=({},{}): '{}'",
					  endProcessingIdx, blockMatcher.start(), blockMatcher.end(), blockMatcher.group(0));
			endProcessingIdx += blockMatcher.end();
			String label = blockMatcher.group(1);
			log.trace( "Sensor Label '{}'", label);
			int moff = -1;
			String smoff = blockMatcher.group(2);
			log.trace("Minute Offset '{}'", smoff);
			try { moff = Integer.parseInt(smoff); }
			catch(NumberFormatException ex)
			{
				moff = -1;
				log.atWarn()
				   .setCause(ex)
				   .log("Invalid minute offset '{}'", smoff);
			}
			int mint = -1;
			String smint = blockMatcher.group(3);
			log.trace("Minute Interval '{}'", smint);
			try { mint = Integer.parseInt(smint); }
			catch(NumberFormatException ex)
			{
				mint = -1;
				log.atWarn()
				   .setCause(ex)
				   .log("Invalid minute interval '{}'", smint);
			}

			String sensorData = blockMatcher.group(4);
			log.trace("Sensor Data '{}'", sensorData);
			TimeSeries ts = null;
			int sensorNumber = -1;
			if (decmsg != null)
			{
				ts = mapLabel2TimeSeries(label, decmsg);

				if (ts == null)
					log.warn("Cannot map sensor for label '{}' -- values will be discarded.", label);
				else
				{
					sensorNumber = ts.getSensorNumber();

					if (processMOFF && moff > 0)
					{
						// Reset 'current' time to 'message' time minus offset.
						Date msgTime = decmsg.getUntruncatedMessageTime();
						if (msgTime == null) msgTime = new Date();
						long msec = msgTime.getTime();
						// moff implies that we truncate to minute boundary
						msec = (msec / 60000L) * 60000L;
						msec -= (moff * 60000L);
						Date timeStamp = new Date(msec);
						decmsg.getTimer().setComplete(timeStamp);
						log.trace("After Minute OFFset {}, timer={}", moff, timeStamp);
					}
					if (mint != -1)
					{
						log.trace( "Setting interval for sensor {} to {} minutes.", sensorNumber, mint);
						decmsg.setTimeInterval(sensorNumber, mint*60);
					}
				}
			}

			Matcher sampleMatcher = capFloatNumberPat.matcher(sensorData);
			boolean sampleFound = false;
			int numSamples = 0;
			while(sampleMatcher.find())
			{
				sampleFound = true;
				String sample = sampleMatcher.group(1);
				log.trace("tsample[{}]: {}",  numSamples++, sample);
				if (ts != null)
				{
					if (sample.startsWith("M") || sample.startsWith("/"))
					{
						Variable v = new Variable("m");
						v.setFlags(v.getFlags() | IFlags.IS_MISSING);
						if (sensorNumber != -1)
							decmsg.addSample(sensorNumber, v, lineNumber);
						continue;
					}
					try
					{
						double x = Double.parseDouble(sample);
						Variable v = new Variable(x);
						if (sensorNumber != -1)
							decmsg.addSample(sensorNumber, v, lineNumber);
					}
					catch (NumberFormatException ex)
					{
						log.atWarn()
						   .setCause(ex)
						   .log("Cannot parse sample data '{}' for sensor label '{}'" +
						   		" sensorNumber={} -- ignored.",
								sample, label, sensorNumber);
					}
				}
			}
			if (!sampleFound)
			{
				log.trace("No samples found.");
			}
		}

		Matcher blockMatcher = battloadBlockPat.matcher(msgData.substring(endProcessingIdx));
		if (blockMatcher.find())
		{
			// Get the label, minute offset, minute index and sensor data
			numBlocks++;
			log.trace("Battload block at ({},{}): '{}'",
					  endProcessingIdx+blockMatcher.start(),
					  endProcessingIdx+blockMatcher.end(),
					  blockMatcher.group(0));
			endProcessingIdx += blockMatcher.end();
			String label = blockMatcher.group(1);
			String sample = blockMatcher.group(2);
			log.trace("Battload Sensor Label '{}' value={}", label, sample);
			if (decmsg != null)
				{
				TimeSeries ts = mapLabel2TimeSeries(label, decmsg);
				if (ts != null)
				{
					try
					{
						double x = Double.parseDouble(sample);
						Variable v = new Variable(x);
						decmsg.addSample(ts.getSensorNumber(), v, lineNumber);
					}
					catch (NumberFormatException ex)
					{
						log.atWarn()
						   .setCause(ex)
						   .log("Cannot parse battload sample data '{}' for sensor label '{}'" +
						   		" sensorNumber={} -- ignored.",
								sample, label, ts.getSensorNumber());
					}
				}
			}
		}
		// Finally eat any whitespace at the end.
		while(endProcessingIdx < msgData.length()
			&& Character.isWhitespace(msgData.charAt(endProcessingIdx)))
			endProcessingIdx++;

		log.trace("Processed {} blocks.", numBlocks);
		return endProcessingIdx;
	}

	private TimeSeries mapLabel2TimeSeries(String label, DecodedMessage decmsg)
	{
		// Find the time series for this label, using the mapping described above.
		// set sensorNumber
		Integer sensorNum = labelSensorNumMap.get(label);
		if (sensorNum != null)
			return decmsg.getTimeSeries(sensorNum);
		TimeSeries best = null;
		for(Iterator<TimeSeries> tsit = decmsg.getAllTimeSeries(); tsit.hasNext(); )
		{
			TimeSeries test = tsit.next();
			Sensor sensor = test.getSensor();
			for(Iterator<DataType> dtit = sensor.getAllDataTypes(); dtit.hasNext(); )
			{
				DataType dt = dtit.next();
				if (dt.getStandard().equalsIgnoreCase(Constants.datatype_LABEL)
				 && dt.getCode().equalsIgnoreCase(label))
				{
					return test;
				}
				// else see if this is an equivalent to the label
				DataType labelEquiv = dt.findEquivalent(Constants.datatype_LABEL);
				if (labelEquiv != null && labelEquiv.getCode().equalsIgnoreCase(label))
				{
					if (best == null)
						best = test;
				}
				// But don't break on an equiv match. There may be an actual
				// "label" data type in the sensor that I haven't checked yet.
			}
		}
		// best is now either null or the first equivalent found.
		// No "label" match was found.
		return best;
	}
	@Override
	public DecodesFunction makeCopy()
	{
		return new AsciiSelfDescFunction();
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
		StringTokenizer st = new StringTokenizer(argString, ",");
		while(st.hasMoreTokens())
		{
			String labnum = st.nextToken();
			int idx = labnum.indexOf('=');
			if (idx == -1)
				throw new ScriptFormatException(module
					+ " invalid label/sensor map '" + labnum + "'. Expect label=sensorNum");
			try
			{
				String label = labnum.substring(0, idx);
				String value = labnum.substring(idx+1);
				if (label.equalsIgnoreCase("processMOFF"))
					processMOFF = TextUtil.str2boolean(value);
				else
				{
					int num = Integer.parseInt(value);
					labelSensorNumMap.put(label, num);
				}
			}
			catch(Exception ex)
			{
				throw new ScriptFormatException(module
					+ " invalid label/sensor map '" + labnum + "'", ex);
			}
		}
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
		AsciiSelfDescFunction asdp = new AsciiSelfDescFunction();
		asdp.setTestMode(true);

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
			asdp.execute(dataOps, null);
			System.out.println("Processed " +
				(dataOps.getBytePos()+rawMsg.getHeaderLength()) + " characters out of " +
				line.length() + ".");

		}
	}


	public void setTestMode(boolean testMode)
	{
		this.testMode = testMode;
	}


}
