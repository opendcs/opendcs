/*
* $Id$
*
* Copyright 2015 Cove Software, LLC. All Rights Reserved.
* 
* $Log$
*/
package covesw.azul.decoder;

import decodes.decoder.DataOperations;
import decodes.decoder.DecodedMessage;
import decodes.decoder.DecoderException;
import decodes.decoder.DecodesFunction;
import decodes.decoder.FieldParseException;
import decodes.decoder.ScriptFormatException;
import ilex.var.Variable;

/**
 * usage ndbcwind(dirSensNum, speedSensNum)
 * Handle the NDBC Wind direction & speed encoding.
 * Encode: /dddsss,
 * ... where ddd is wind direction (degrees),
 *     and sss is wind speed (averaged over period) in either knots/10 
 *     or m/s/10.
 * Special case, ddd >=500 is used to indicate speeds over 100.
 * When this is seen, subtract 500 from ddd and add 100 to sss.
 */
public class NdbcWind
	extends DecodesFunction 
{
	private final static String module = "ndbcWind";
	private final static String usage = "usage: " + module + "(dirSens#, speedSens#)";
	private int dirSensNum = -1;
	private int speedSensNum = -1;

	public NdbcWind() {}

	@Override
	public String getFuncName() { return module; }

	@Override
	public DecodesFunction makeCopy() { return new NdbcWind(); }

	@Override
	public void setArguments(String argstr)
		throws ScriptFormatException
	{
		String args[] = argstr.split(",");
		if (args.length != 2)
			throw new ScriptFormatException(module + " missing required arg -- " + usage);
		try
		{
			dirSensNum = Integer.parseInt(args[0].trim());
			speedSensNum = Integer.parseInt(args[1].trim());
		}
		catch(NumberFormatException ex)
		{
			throw new ScriptFormatException(module + " non-numeric arg -- " + usage);
		}
	}

	@Override
	public void execute(DataOperations dd, DecodedMessage msg)
		throws DecoderException
	{
		if (dd.curByte() == (byte)'/')
			dd.forwardspace();

		byte[] dirfld = dd.getField(3, null);
		int dir = get3digField(dirfld);
		byte[] spdfld = dd.getField(3, null);
		double spd = (double)get3digField(spdfld);
		if (spd >= 0)
		{
			spd /= 10.;
			if (dir >= 500)
			{
				dir -= 500;
				spd += 100.;
			}
		}

		int curLine = dd.getCurrentLine();
		if (dirSensNum != -1 && dir >= 0)
			msg.addSample(dirSensNum, new Variable(dir), curLine);
		if (speedSensNum != -1 && spd >= 0)
			msg.addSample(speedSensNum, new Variable(spd), curLine);
	}
	
	private int get3digField(byte[] d)
		throws DecoderException
	{
		if (d[0] == (byte)'/' && d[1] == (byte)'/' && d[2] == (byte)'/')
			return -1;
		else
		{
			int n = 0;
			for(int i=0; i<d.length; i++)
			{
				if (!Character.isDigit((char)d[i]))
					throw new FieldParseException(
						"Non-numeric value in NdbcWind field.");
				n = n*10 + ((int)d[i]-(int)'0');
			}
			return n;
		}
	}
}