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
import java.util.ArrayList;
import java.util.StringTokenizer;
import org.opendcs.spi.decodes.DecodesFunctionProvider;
import org.opendcs.utils.logging.OpenDcsLoggerFactory;
import org.slf4j.Logger;

import com.google.auto.service.AutoService;

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
	private String delimiter = ",";
	private String dataTypeStandard = null;

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
		try
		{		
			char current = (char)dd.curByte();	
			while(!(current == '\n' || current == '\r'))
			{
				if (current == '&' || !dd.moreChars())
				{
					log.info("Processing {}", sb);
					processPair(sb.toString(), decmsg, dd.getCurrentLine(), fieldStart, dd);
					fieldStart++;
					sb.setLength(0);
				}
				else
				{
					sb.append(current);
				}
				dd.forwardspace();
				current = (char) dd.curByte();
			}

			if (!sb.isEmpty())
			{
				processPair(sb.toString(), decmsg, dd.getCurrentLine(), fieldStart, dd);
			}

		}
		catch(EndOfDataException ex)
		{
			if (!sb.isEmpty()) {
				processPair(sb.toString(), decmsg, dd.getCurrentLine(), fieldStart, dd);
			}
			log.trace("End of message reached");
			throw ex;
		}
		
	}

	private void processPair(String pair, DecodedMessage decmsg, int linenum,
			int fieldStart, DataOperations dd)
	{
		var parts = pair.split("=");
		var key = URLDecoder.decode(parts[0], StandardCharsets.UTF_8);
		var value = URLDecoder.decode(parts[1], StandardCharsets.UTF_8);
		processPair(key, value, decmsg, dd.getCurrentLine(), fieldStart, dd);
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
	private void processPair(String key, String sample, DecodedMessage decmsg, int linenum,
		int fieldStart, DataOperations dd)
	{
		final DecodesScript script = formatStatement.getDecodesScript();
		int sensorNumber = -1;
		log.trace("Processing key '{}', value='{}'", key, sample);

		sensorNumber = script.scriptSensors.stream()
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
			String real = script.getReplacement(sample);
			log.trace( "Turning {} -> {}", sample, real);
			double x = Double.parseDouble(real); 
			Variable v = new Variable(x);
			
			TimedVariable tv = decmsg.addSample(sensorNumber, v, linenum);
			if (tv != null && DecodesScript.trackDecoding)
			{
				DecodedSample ds = new DecodedSample(this, 
					fieldStart, dd.getBytePos(),
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
			   		linenum, sample, sensorNumber);
			Variable v = new Variable("m");
			v.setFlags(v.getFlags() | IFlags.IS_ERROR);
			decmsg.addSample(sensorNumber, v, linenum);
			log.trace("Value flagged as error");
			return;
		}
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

	//@AutoService (DecodesFunctionProvider.class)
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
}
