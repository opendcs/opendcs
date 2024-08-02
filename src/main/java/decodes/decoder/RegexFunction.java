package decodes.decoder;

import decodes.db.DecodesScript;
import decodes.sql.PlatformListIO;
import ilex.var.IFlags;
import ilex.var.TimedVariable;
import ilex.var.Variable;
import org.slf4j.LoggerFactory;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RegexFunction
	extends DecodesFunction
{
	private final static org.slf4j.Logger log = LoggerFactory.getLogger(PlatformListIO.class);
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
		log.trace("Executing with args '" + argString + "'");
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
			log.warn("    value is flagged as missing");
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
			throw new DecoderException("Could not find named sensor group in the regex "+ argString);
		}

		int idxEnd = argString.indexOf(">",idxStart);
		if( idxEnd == -1)
		{
			throw new DecoderException("Did not find expected closing '>' in named capture group "+ argString);
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
