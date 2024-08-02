package decodes.decoder;

import ilex.util.Logger;
import ilex.var.IFlags;
import ilex.var.TimedVariable;
import ilex.var.Variable;

import java.util.ArrayList;
import java.util.StringTokenizer;

import decodes.db.DecodesScript;

public class CsvFunction
	extends DecodesFunction
{
	private ArrayList<Integer> sensorNumbers = new ArrayList<Integer>();
	private String module = "csv";
	private String argList = null;
	private String delimiter = ",";
	
	public CsvFunction()
	{
	}

	@Override
	public DecodesFunction makeCopy()
	{
		return new CsvFunction();
	}

	@Override
	public String getFuncName()
	{
		return module;
	}

	@Override
	public void execute(DataOperations dd, DecodedMessage decmsg) throws DecoderException
	{
		StringBuilder sb = new StringBuilder();
		
		trace("Executing with args '" + argList + "' #sensors=" + sensorNumbers.size());
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
					trace("End of line seen.");
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
			trace("End of message reached");
			throw ex;
		}
		
	}

	private void processColumn(int col, String sample, DecodedMessage decmsg, int linenum,
		int fieldStart, DataOperations dd)
	{
		int sensorNumber = sensorNumbers.get(col);
		if (sensorNumber < 0)
			return;
		trace("Processing column " + col + " value='" + sample + "'");
		if (sample.length() == 0 || sample.startsWith("M") || sample.startsWith("/"))
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
					formatStatement.getDecodesScript().addDecodedSample(ds);
				}
			}
			else
				trace("    value is flagged as missing");
			return;
		}
		try
		{
			double x = Double.parseDouble(sample); 
			Variable v = new Variable(x);
			TimedVariable tv = decmsg.addSample(sensorNumber, v, linenum);
			if (tv != null && DecodesScript.trackDecoding)
			{
				DecodedSample ds = new DecodedSample(this, 
					fieldStart, dd.getBytePos(),
					tv, decmsg.getTimeSeries(sensorNumber));
				formatStatement.getDecodesScript().addDecodedSample(ds);
			}
			trace("    Added value " + x);
		}
		catch (NumberFormatException ex)
		{
			warning(" line " + linenum + " Cannot parse sample data '" + sample + "' for sensor "
				+ sensorNumber + " -- ignored.");
			Variable v = new Variable("m");
			v.setFlags(v.getFlags() | IFlags.IS_ERROR);
			decmsg.addSample(sensorNumber, v, linenum);
			trace("    Value flagged as error");
			return;
		}
	}

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
					sensorNumbers.add(-1);
				}
			}
		}
		trace("Instantiated from argument '" + argString + "' -- # sensors=" 
			+ sensorNumbers.size());
	}
	
	private void trace(String s)
	{
		Logger.instance().debug3(module + " " + s);
	}

	private void warning(String s)
	{
		Logger.instance().warning(module + " " + s);
	}

}
