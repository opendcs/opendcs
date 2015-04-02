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
 * Usage: ndbc921wind(sensorNumber)
 * NDBC encodes maximum (921) wind speed in section 3 in one of two wasy:
 * 921<fff>         - <fff> = wind speed in knots/10, or if metric, meters-per-sec/10
 * 921999 921<fff>  - <fff> = same as above, but add 100. This is used for values > 99.9
 */
public class Ndbc921Wind
	extends DecodesFunction 
{
	private final static String module = "ndbc921wind";
	private final static String usage = "usage: " + module + "(sensorNumber)";
	private int sensorNumber = -1;

	public Ndbc921Wind() {}
	
	@Override
	public String getFuncName() { return module; }
	
	@Override
	public DecodesFunction makeCopy() { return new Ndbc921Wind(); }

	@Override
	public void setArguments(String argstr) throws ScriptFormatException
	{
		String args[] = argstr.split(",");
		if (args.length != 1)
			throw new ScriptFormatException(module + " missing required arg -- " + usage);
		try { sensorNumber = Integer.parseInt(args[0].trim()); }
		catch(NumberFormatException ex)
		{
			throw new ScriptFormatException(module + " non-numeric arg -- " + usage);
		}
	}

	@Override
	public void execute(DataOperations dd, DecodedMessage decmsg)
		throws DecoderException
	{
		int velocity = parseVelocity(dd);
		if (velocity == -1) return;
		if (velocity == 999)
		{
			dd.forwardspace();
			velocity = 1000 + parseVelocity(dd);
		}
		velocity /= 10.;
		
		// If sensorNum == -1 it means to skip without assignment
		if (sensorNumber == -1)
			return;
		double dvel = (double)velocity / 10.0;
		decmsg.addSample(sensorNumber, new Variable(dvel), dd.getCurrentLine());
	}
	
	private int parseVelocity(DataOperations dd)
		throws DecoderException
	{
		byte[] bytes = dd.getField(3, null);
		String sdata = new String(bytes);
		if (bytes.length != 3 || !sdata.equals("921"))
			throw new FieldParseException(module + " expected 921, got '" + sdata + "'");

		bytes = dd.getField(3, null);
		int ret = 0;
		for(int i=0; i<bytes.length; i++)
		{
			if (bytes[i] == (byte)'/')
				return -1;
			if (!Character.isDigit((char)bytes[i]))
				throw new FieldParseException(module + " unexpected non-numeric data");
			ret = ret*10 + ((int)bytes[i]-(int)'0');
		}
		return ret;
	}
}
