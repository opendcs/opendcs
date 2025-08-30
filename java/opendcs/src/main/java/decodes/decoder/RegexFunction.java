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

import decodes.db.DecodesScript;
import ilex.var.IFlags;
import ilex.var.TimedVariable;
import ilex.var.Variable;

import org.opendcs.utils.logging.OpenDcsLoggerFactory;
import org.slf4j.Logger;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RegexFunction extends DecodesFunction
{
	private final static Logger log = OpenDcsLoggerFactory.getLogger();
	public static final String module = "regex";
	private String argString = null;

	public RegexFunction()
	{
	}

	@Override
	public DecodesFunction makeCopy()
	{
		return new RegexFunction();
	}

	@Override
	public String getFuncName()
	{
		return module;
	}

	@Override
	public void execute(DataOperations dd, DecodedMessage decmsg) throws DecoderException
	{
		log.trace("Executing with args '{}'", argString);
		int sensorNumber = getSensorNumber();

		Pattern pattern = Pattern.compile(this.argString);
		String s= dd.getRawMessage().toString();
		s = s.substring(dd.getBytePos(),s.length()-1);
		Matcher matcher = pattern.matcher(s);
		Variable v = getValue(matcher,sensorNumber);

		if (matcher.groupCount()==1)
		{
			TimedVariable tv = decmsg.addSample(sensorNumber, v, 1);
			if (tv != null && DecodesScript.trackDecoding)
			{
				int start =matcher.start(1);
				int end =matcher.end(1);
				DecodedSample ds = new DecodedSample(this,
						start+dd.getBytePos(), end+dd.getBytePos(),
						tv, decmsg.getTimeSeries(sensorNumber));
				formatStatement.getDecodesScript().addDecodedSample(ds);
				for (int i = 0; i <end ; i++)
				{
					if (dd.moreChars())
					{
						dd.forwardspace();
					}
				}
			}
		}
		else
		{
			log.warn("value is flagged as missing");
		}
	}

	/**
	 * Gets sensor number from the regular expression
	 * expected to have named group that starts with 'sensor'
	 * and ends with a number.  For example 'sensor2'
	 * @return number portion sensor[number]
	 */
	private int getSensorNumber() throws DecoderException
	{ // ?<sensor2>[0-9\,\.]+)
		String grpPrefix = "(?<sensor";
		int idxStart = argString.indexOf(grpPrefix);
		if(idxStart == -1)
		{
			throw new DecoderException("Could not find named sensor group in the regex " + argString);
		}

		int idxEnd = argString.indexOf(">",idxStart);
		if( idxEnd == -1)
		{
			throw new DecoderException("Did not find expected closing '>' in named capture group " + argString);
		}
		String number = argString.substring(idxStart+grpPrefix.length(),idxEnd);
		return  Integer.parseInt(number);
	}


	public static Variable getValue(Matcher matcher, int sensorNumber)
	{
		if(matcher.find())
		{
			String s = matcher.group("sensor"+sensorNumber);
			s = s.replaceAll(",",""); // remove comma from number
			double d = Double.parseDouble(s);
			return new Variable(d);
		}
		Variable v = new Variable("m");
		v.setFlags(v.getFlags() | IFlags.IS_MISSING);
		return v;
	}

	@Override
	public void setArguments(String argString, DecodesScript script) throws ScriptFormatException
	{
		this.argString = argString;
	}
}
