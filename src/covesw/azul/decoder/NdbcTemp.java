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
 * 
 * Handle the NDBC temperature encoding.
 * Temperature should be 4 digits. The first digit should be 0 (positive)
 * or 1 (negative). The next 3 digits is the temperature in degC/10.
 */
public class NdbcTemp
	extends DecodesFunction 
{
	private final static String module = "ndbcTemp";
	private final static String usage = "usage: " + module + "(sensorNumber)";
	private int sensorNumber = -1;

	public NdbcTemp() {}

	@Override
	public String getFuncName() { return module; }

	@Override
	public DecodesFunction makeCopy() { return new NdbcTemp(); }

	@Override
	public void setArguments(String argstr)
		throws ScriptFormatException
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
		int curLine = dd.getCurrentLine();
		int sign = 1;
		byte signByte = dd.curByte();
		dd.forwardspace();
		byte[] fld = dd.getField(3, null);
		
		if (sensorNumber == -1)
			return; // Just skip the field.
		
		// first byte determines sign: 1 means negative, 0 means positive
		char signC = (char)signByte;
		if (signC == '/')  /* missing */
			return;
		else if (signC == '1') sign = -1;
		else if (signC == '0');
		else
			throw new FieldParseException("Invalid " + module + " data '"
				+ signC + (new String(fld))
				+ "' -- 1st char should be 0 or 1 for sign.");

		double tmp = 0.0;
		for(int i=0; i<fld.length; i++)
		{
			if (!Character.isDigit((char)fld[i]))
				throw new FieldParseException(
					"Non-numeric data in " + module);
			tmp = tmp*10.0 + ((int)fld[i]-(int)'0');
		}

		decmsg.addSample(sensorNumber, new Variable(tmp/10.0 * sign), curLine);
	}
}
