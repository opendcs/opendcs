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

import ilex.var.IFlags;
import ilex.var.TimedVariable;
import ilex.var.Variable;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.StringTokenizer;
import org.opendcs.spi.decodes.DecodesFunctionProvider;
import org.opendcs.utils.logging.OpenDcsLoggerFactory;
import org.slf4j.Logger;

import decodes.db.DecodesScript;


/**
 * Extends the DecodesFunction. Responsible for parsing and processing CSV
 * formatted data. It reads a CSV row, extracts sensor data from
 * columns, and adds the corresponding values to a DecodedMessage object.
 *
 * The function allows specifying a custom delimiter and assigns sensor
 * numbers to CSV columns, handling missing or malformed data.
 *
 * It uses the DataOperations object to process the telemetry data byte by byte
 * and the DecodedMessage to store the extracted samples for each sensor.
 */
public class HtmlFormEncodedFunction extends DecodesFunction
{
	private static final Logger log = OpenDcsLoggerFactory.getLogger();	
	public static final String MODULE = "htmlformdata";

	private ArrayList<Integer> sensorNumbers = new ArrayList<Integer>();
	private String argList = null;
	private String dataTypeStandard = null;
	private String dateTimeField = null;
	private DateTimeFormatter dateTimeFormatter = null;

	/**
	 * Default constructor for CsvFunction.
	 * Initializes the function with default values.
	 */
	public HtmlFormEncodedFunction()
	{
	}


	/**
	 * Creates a copy of the current CsvFunction object.
	 *
	 * @return a new instance of CsvFunction.
	 */
	@Override
	public DecodesFunction makeCopy()
	{
		return new HtmlFormEncodedFunction();
	}


	/**
	 * Returns the function name, which is "htmlformdata" in this case.
	 *
	 * @return the name of the function as a String.
	 */
	@Override
	public String getFuncName()
	{
		return MODULE;
	}


	/**
	 * Executes the HTML Form Data parsing function by processing the key/value pair to sensor name
	 * and value.
	 *
	 * NOTE: this intentionally doesn't use the netty QueryStringDecoder, if we don't individually process
	 * the pairs we can't assign the location in the message for highlighting.
	 *
	 * @param dd        DataOperations object used to read data.
	 * @param decmsg    DecodedMessage object to store the extracted sensor data.
	 * @throws DecoderException if an error occurs during decoding.
	 */
	@Override
	public void execute(DataOperations dd, DecodedMessage decmsg) throws DecoderException
	{
		var sb = new StringBuffer();
		log.trace("Executing with args '{}' #sensors={}", argList, sensorNumbers.size());
		int fieldStart = dd.getBytePos();
		HashMap<String,Value> pairs = new HashMap<>();
		try
		{
			while (dd.moreChars())
			{
				char current = (char) dd.curByte();
				if (current == '\n' || current == '\r')
				{
					break;
				}
				if (current == '&')
				{
					log.info("Processing {}", sb);
					processPair(pairs, sb.toString(), decmsg, dd.getCurrentLine(), fieldStart, dd);
					fieldStart=dd.getBytePos();
					sb.setLength(0);
				}
				else
				{
					sb.append(current);
				}
				dd.forwardspace();
			}

			if (!sb.isEmpty())
			{
				log.info("Processing {} (final)", sb);
				processPair(pairs, sb.toString(), decmsg, dd.getCurrentLine(), fieldStart, dd);
			}
		}
		catch(EndOfDataException ex)
		{
			if (!sb.isEmpty()) {
				log.info("Processing {} (exception)", sb);
				processPair(pairs, sb.toString(), decmsg, dd.getCurrentLine(), fieldStart, dd);
			}
			log.trace("End of message reached");
			throw ex;
		}
		finally
		{
			processPairs(pairs, decmsg, dd);
		}
		
	}

	private void processPair(HashMap<String,Value> pairs, String pair, DecodedMessage decmsg, int linenum,
			int fieldStart, DataOperations dd)
	{
		var parts = pair.split("=");
		var key = URLDecoder.decode(parts[0], StandardCharsets.UTF_8);
		var value = URLDecoder.decode(parts[1], StandardCharsets.UTF_8);
		pairs.put(key, new Value(value, dd.getCurrentLine(), linenum));
	}

	/**
	 * Processes a specific column of the CSV data by extracting the sample value
	 * and adding it to the DecodedMessage object. If the data is flagged as missing
	 * or contains an error, it appropriately marks the variable with flags.
	 *
	 * @param key         embedded sensor name
	 * @param sample      sample data extracted from the column.
	 * @param decmsg      the DecodedMessage to store the sample.
	 * @param linenum     the line number being processed.
	 * @param fieldStart  the starting byte position of the field.
	 * @param dd          the DataOperations object for reading data.
	 */
	private void processPairs(HashMap<String, Value> pairs, DecodedMessage decmsg, DataOperations dd) throws DecoderException
	{
		final DecodesScript script = formatStatement.getDecodesScript();
		
		final var dateTimeStr = pairs.get(dateTimeField);
		Instant dateTime = null;
		if (dateTimeStr == null && dateTimeField != null)
		{
			throw new DecoderException("DateTimeField was set, but no date/time field was available in the message.");
		}
		else if (dateTimeStr != null)
		{
			dateTime = LocalDateTime.parse(dateTimeStr.value, dateTimeFormatter).atZone(ZoneId.of("UTC")).toInstant();
			
		}
		final Date dataDate = dateTime != null ? Date.from(dateTime) : null;
		pairs.forEach((key, sample) ->
		{
			if (key.equals(dateTimeStr))
			{
				return;
			}
			log.trace("Processing key '{}', value='{}'", key, sample);
			var sensorNumber = script.scriptSensors.stream()
											.filter(ss -> ss.getSensorName().equalsIgnoreCase(key))
											.map(ss -> ss.sensorNumber)
											.findFirst()
											.orElse(-1);
			if (sensorNumber == -1)
			{
				var configSensors = script.platformConfig.getSensorVec();
				sensorNumber = configSensors.stream()
											.filter(cs -> 
												cs.sensorName.equalsIgnoreCase(key) ||
												cs.getDataTypeVec().stream()
												.filter(dt -> dataTypeStandard == null
													        ? true
															: dataTypeStandard.equalsIgnoreCase(dt.getStandard()))
												.anyMatch(csDt -> csDt.getCode().equalsIgnoreCase(key))
											)
											.map(cs -> cs.sensorNumber)
											.findFirst()
											.orElse(-1);
			};

			if (sensorNumber < 0) {
				log.atWarn().log("could not find matching sensor for {}", key);
				return ;
			}
			try
			{
				String real = script.getReplacement(sample.value);
				log.trace( "Turning {} -> {}", sample, real);
				double x = Double.parseDouble(real); 
				Variable v = new Variable(x);
				TimedVariable tv = null;
				if (dataDate != null)
				{
					tv = decmsg.addSampleWithTime(sensorNumber, v, dataDate, sample.lineNumber);
				}
				else
				{
					tv = decmsg.addSample(sensorNumber, v, sample.lineNumber);
				}
				if (tv != null && DecodesScript.trackDecoding)
				{
					DecodedSample ds = new DecodedSample(this, 
						sample.fieldStart, dd.getBytePos(),
						tv, decmsg.getTimeSeries(sensorNumber));
					script.addDecodedSample(ds);
				}
				log.trace("Added value {}", x);
			}
			catch (NumberFormatException ex)
			{
				log.atWarn()
				.setCause(ex)
				.log("line {} Cannot parse sample data '{}' for sensor {} -- ignored.",
						sample.lineNumber, sample, sensorNumber);
				Variable v = new Variable("m");
				v.setFlags(v.getFlags() | IFlags.IS_ERROR);
				decmsg.addSample(sensorNumber, v, sample.lineNumber);
				log.trace("Value flagged as error");
				return;
			}
		});
	}

	/**
	 * Sets the arguments for the HtmlFormDataFunction based on the provided argument string.
	 * The arguments specify sensor numbers to save and optional datatype standard.
	 * If the standard not set then the first matching name (ignoring case) is used.
	 * If the standard is set the lookup will <b>only</b> use that standard.
	 *
	 * @param argString the argument string containing sensor numbers and delimiter.
	 * @param script    the DecodesScript object to be used with the function.
	 * @throws ScriptFormatException if the argument string format is invalid.
	 */
	@Override
	public void setArguments(String argString, DecodesScript script) throws ScriptFormatException
	{
		argList = argString;
		StringTokenizer st = new StringTokenizer(argString, ",");
		dateTimeField = null;
		sensorNumbers.clear();
		dateTimeFormatter = null;
		dataTypeStandard = null;
		while(st.hasMoreTokens())
		{
			String t = st.nextToken();
			t = t.trim();
			if (t.startsWith("datatype"))
			{
				String[] parts = t.split("=");
				if (parts.length > 1)
				{
					this.dataTypeStandard = parts[1];
				}
			}
			else if (t.toLowerCase().startsWith("timefield"))
			{
				String[] parts = t.split("=");
				if (parts.length > 1)
				{
					dateTimeField = parts[1];
				}
			}
			else if (t.toLowerCase().startsWith("timeformat"))
			{
				String[] parts = t.split("=");
				if (parts.length > 1)
				{
					dateTimeFormatter = DateTimeFormatter.ofPattern(parts[1]);
				}
			}
			else
			{
				try
				{
					int sensorNum = Integer.parseInt(t);
					sensorNumbers.add(sensorNum);
				}
				catch(Exception ex)
				{
					log.atWarn()
					   .setCause(ex)
					   .log("Unable to parse sensor '{}'", t);
				}
			}
		}
		log.trace("Instantiated from argument '{}' -- # sensors={}", argString, sensorNumbers.size());
	}

	public static class HtmlFormEncodedFunctionProvider implements DecodesFunctionProvider
	{

		@Override
		public String getName() 
		{
			return MODULE;
		}

		@Override
		public DecodesFunction createInstance()
		{
			return new HtmlFormEncodedFunction();
		}
	
		
	}

	public record Value(String value, int fieldStart, int lineNumber)
	{
	};
}
