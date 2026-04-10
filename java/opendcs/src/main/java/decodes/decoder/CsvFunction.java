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

import java.util.ArrayList;
import java.util.StringTokenizer;

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
public class CsvFunction extends DecodesFunction
{
	private static final Logger log = OpenDcsLoggerFactory.getLogger();
	private ArrayList<Integer> sensorNumbers = new ArrayList<Integer>();
	public static final String module = "csv";
	private String argList = null;
	private String delimiter = ",";

	/**
	 * Default constructor for CsvFunction.
	 * Initializes the function with default values.
	 */
	public CsvFunction()
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
		return new CsvFunction();
	}


	/**
	 * Returns the function name, which is "csv" in this case.
	 *
	 * @return the name of the function as a String.
	 */
	@Override
	public String getFuncName()
	{
		return module;
	}


	/**
	 * Executes the CSV parsing function by processing a CSV row and extracting
	 * the corresponding sensor data from the columns.
	 *
	 * @param dd        DataOperations object used to read data.
	 * @param decmsg    DecodedMessage object to store the extracted sensor data.
	 * @throws DecoderException if an error occurs during decoding.
	 */
	@Override
	public void execute(DataOperations dd, DecodedMessage decmsg) throws DecoderException
	{
		StringBuilder sb = new StringBuilder();
		
		log.trace("Executing with args '{}' #sensors={}", argList, sensorNumbers.size());
		int col = 0;
		int fieldStart = dd.getBytePos();
		try
		{
			while(col < sensorNumbers.size())
			{
				char c = (char)dd.curByte();
				if (c == '\n' || c=='\r')
				{
					processColumn(col++, sb.toString().trim(), decmsg, dd.getCurrentLine(), 
						fieldStart, dd);
					log.trace("End of line seen.");
					break;
				}
				if (dd.checkString(delimiter))
				{
					processColumn(col++, sb.toString().trim(), decmsg, dd.getCurrentLine(),
						fieldStart, dd);
					sb.setLength(0);
					fieldStart = dd.getBytePos()+delimiter.length();
				}
				else
					sb.append(c);
				dd.forwardspace();
			}
		}
		catch(EndOfDataException ex)
		{
			String s = sb.toString().trim();
			if (s.length() > 0)
				processColumn(col++, s, decmsg, dd.getCurrentLine(), fieldStart, dd);
			log.trace("End of message reached");
			throw ex;
		}
		
	}

	/**
	 * Processes a specific column of the CSV data by extracting the sample value
	 * and adding it to the DecodedMessage object. If the data is flagged as missing
	 * or contains an error, it appropriately marks the variable with flags.
	 *
	 * @param col         column index being processed.
	 * @param sample      sample data extracted from the column.
	 * @param decmsg      the DecodedMessage to store the sample.
	 * @param linenum     the line number being processed.
	 * @param fieldStart  the starting byte position of the field.
	 * @param dd          the DataOperations object for reading data.
	 */
	private void processColumn(int col, String sample, DecodedMessage decmsg, int linenum,
		int fieldStart, DataOperations dd)
	{
		final DecodesScript script = formatStatement.getDecodesScript();
		int sensorNumber = sensorNumbers.get(col);
		if (sensorNumber < 0)
			return;
		log.trace("Processing column {}, value='{}'", col, sample);
		boolean isMissing =script.isMissingSymbol(sample) || sample.startsWith("M");
		if (sample.length() == 0 || isMissing || sample.startsWith("/"))
		{
			Variable v = new Variable("m");
			v.setFlags(v.getFlags() | IFlags.IS_MISSING);
			if (sensorNumber != -1)
			{
				TimedVariable tv = decmsg.addSample(sensorNumber, v, linenum);
				if (tv != null && DecodesScript.trackDecoding)
				{
					DecodedSample ds = new DecodedSample(this, 
						fieldStart, dd.getBytePos(),
						tv, decmsg.getTimeSeries(sensorNumber));
					script.addDecodedSample(ds);
				}
			}
			else
				log.trace("value is flagged as missing");
			return;
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
	 * Sets the arguments for the CsvFunction based on the provided argument string.
	 * The arguments specify sensor numbers and optional custom delimiters for
	 * parsing the CSV data.
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
			if (t.startsWith("delimiter"))
			{
				String[] parts = t.split("=");
				if( parts.length ==2 )
				{
					this.delimiter = parts[1];
				}
				if (delimiter.equals("\\s"))
				{
					delimiter = " ";
				}
				else if (delimiter.equals("\\t"))
				{
					delimiter = "\t";
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
					sensorNumbers.add(-1);
				}
			}
		}
		log.trace("Instantiated from argument '{}' -- # sensors={}", argString, sensorNumbers.size());
	}

}
