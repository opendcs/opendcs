/*
* $Id$
*
* Copyright 2015 Cove Software, LLC
* All Rights Reserved
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
 * Usage: ndbcbattery(sensorNum)
 * where 'sensorNum' is the number for the battery sensor in the DECODES config.
 * According to NDBC, values are encoded as a 2-digit integer in tenths of volts.
 * When encoding, if the voltage is > 9.9, they subtract 10 before encoding.
 * Thus the range is 9.9 (encoded as "99") ... 19.8 (encoded as "98").
 */
public class NdbcBattery extends DecodesFunction
{
	private final static String module = "ndbcBattary";
	private final static String usage = "usage: " + module + "(sensorNumber)";
	private int sensorNum = -1;

	public NdbcBattery()
	{
	}

	@Override
	public String getFuncName() { return module; }

	@Override
	public DecodesFunction makeCopy() { return new NdbcBattery(); }

	@Override
	public void setArguments(String argstr)
		throws ScriptFormatException
	{
		String args[] = argstr.split(",");
		if (args.length != 1)
			throw new ScriptFormatException(module 
				+ " missing required arg -- " + usage);
		try { sensorNum = Integer.parseInt(args[0].trim()); }
		catch(NumberFormatException ex)
		{
			throw new ScriptFormatException(module + " non-numeric arg -- " + usage);
		}
	}

	public void execute(DataOperations dd, DecodedMessage msg)
		throws DecoderException
	{
		byte[] data = dd.getField(2, null);
		int currentLine = dd.getCurrentLine();

		double voltage = 0;
		for(int i=0; i<data.length; i++)
		{
			if (!Character.isDigit((char)data[i]))
				throw new FieldParseException(module + " invalid battery field '"
					+ new String(data) + "' on line + " + currentLine + ".");
			voltage = voltage*10.0 + ((int)data[i]-(int)'0');
		}
		
		voltage /= 10.;
		if (voltage <= 9.9) voltage += 10.;

		if (sensorNum != -1)
			msg.addSample(sensorNum, new Variable(voltage),	currentLine);
	}
}
