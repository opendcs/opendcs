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

public class NdbcLatLon
	extends DecodesFunction 
{
	private final static String module = "ndbcLatLon";
	private final static String usage = "usage: " + module + "(sensorNumber)";
	private int latSensNum = -1, lonSensNum = -1;

	public NdbcLatLon() {}

	@Override
	public String getFuncName() { return module; }

	@Override
	public DecodesFunction makeCopy() { return new NdbcLatLon(); }

	@Override
	public void setArguments(String argstr)
		throws ScriptFormatException
	{
		String args[] = argstr.split(",");
		if (args.length != 2)
			throw new ScriptFormatException(module + " missing required arg -- " + usage);
		try
		{
			latSensNum = Integer.parseInt(args[0].trim());
			lonSensNum = Integer.parseInt(args[1].trim());
		}
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
		// 1st char should be S = south = negative lat,
		// or N = north = positive lat
		// or / meaning missing data.
		int sign = 1;
		byte signByte = dd.curByte(); dd.forwardspace();
		char signChar = (char)signByte;
		byte[] data = dd.getField(6, null);
		
		String latData = "" + signChar + new String(data);
		
		if (signChar == 'S') sign = -1;
		else if (signChar == 'N') sign = 1;
		else if (signChar == '/')
			return;
		else
			throw new FieldParseException(module + " invalid latitude sign '" + latData + "'");

		double lat = 0;
		for(int i=0; i<data.length; i++)
		{
			if (!Character.isDigit((char)data[i]))
				throw new FieldParseException(module + " invalid data '" + latData
					+ " non-numeric latitude.");
			lat = lat*10.0 + ((int)data[i]-(int)'0');
		}
		lat = lat / 10000.0 * sign;

		sign = 1;
		signByte = dd.curByte(); dd.forwardspace();
		signChar = (char)signByte;
		data = dd.getField(7, null);
		
		String lonData = "" + signChar + new String(data);
		
		if (signByte == (byte)'W') sign = -1;
		else if (signByte == (byte)'E') sign = 1;
		else if (signByte == (byte)'/')
			return;
		else
			throw new FieldParseException(module + " invalid longitude sign '" + lonData + "'");

		double lon = 0;
		for(int i=0; i<data.length; i++)
		{
			if (!Character.isDigit((char)data[i]))
				throw new FieldParseException(
					"Non-numeric data in longitude field.");
			lon = lon*10.0 + ((int)data[i]-(int)'0');
		}
		lon = lon / 10000.0 * sign;
		
		if (latSensNum != -1)
			decmsg.addSample(latSensNum, new Variable(lat), curLine);
		if (lonSensNum != -1)
			decmsg.addSample(lonSensNum, new Variable(lon), curLine);
	}
}
