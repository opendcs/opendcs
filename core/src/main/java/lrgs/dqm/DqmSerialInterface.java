/*
* $Id$
*/
package lrgs.dqm;

import java.io.*;
import java.util.*;
import lrgs.lrgsmain.LrgsConfig;
import ilex.util.Logger;

/**
Handles serial port IO to DAPS.

MJM 20061011 - simplified:
- Don't use javax.comm because it's not supported on Windows.
- Open port specified in LRGS configuration file.
- Serial params must be set outside of the DQM program (e.g. with hyperterm).
*/
public class DqmSerialInterface 
{
	/** The port name to use, settable via setPortName() method. */
	private String portName;

	/** The output stream object */
	private FileOutputStream outputStream;
	
	public DqmSerialInterface()
	{
		portName = "COM1";
		outputStream = null;
	}

	public void open()
		throws DqmSerialException
	{
		portName = LrgsConfig.instance().dqmSerialPort;
		Logger.instance().debug1(
			"Attempting to open serial port '" + portName + "'");

		try
		{
			outputStream = new FileOutputStream(portName);
		}
		catch(Exception ex)
		{
			throw new DqmSerialException(
				"Cannot open DQM serial port '" + portName + "': " + ex);
		}
	}

	public void write(String msg)
		throws DqmSerialException
	{
		if (outputStream == null)
			throw new DqmSerialException("Open was unsuccessful, cannot write '"
				+ msg + "'");
		try
		{
			byte data[] = msg.getBytes();
			outputStream.write(data);
			outputStream.flush();
			Logger.instance().debug1("serial write(), datalen=" + data.length);
		}
		catch(Exception ex)
		{
			throw new DqmSerialException("Error writing '" + msg + "': " + ex);
		}
	}

	public void close() 
	{
		try { outputStream.close(); }
		catch(IOException ex) {}
		outputStream = null;
	}

    public static final void main(String args[])
        throws Exception
    {
        DqmSerialInterface sdi = new DqmSerialInterface();
        sdi.open();
        // args[0] is a filename -- open it.
        File f = new File(args[0]);
        FileInputStream fis = new FileInputStream(f);
        byte buf[] = new byte[(int)f.length()];
        fis.read(buf);
        fis.close();
        String s = new String(buf);
        sdi.write(s);
        sdi.close();
    }
}
