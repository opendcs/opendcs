/*
*  $Id$
*
*  This is open-source software written by ILEX Engineering, Inc., under
*  contract to the federal government. You are free to copy and use this
*  source code for your own purposes, except that no part of the information
*  contained in this file may be claimed to be proprietary.
*
*  Except for specific contractual terms between ILEX and the federal 
*  government, this source code is provided completely without warranty.
*  For more information contact: info@ilexeng.com
*
*/
package lrgs.domsatrecv;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import ilex.util.EnvExpander;
import ilex.util.Logger;
import lrgs.lrgsmain.LrgsConfig;

/**
This defines the interface to code controlling the DOMSAT interface.
*/
public class DomsatSangoma
	implements DomsatHardware
{
	/**
	 * Constructor.
	 */
	public void DomsatSangoma()
	{
	}

	/** Initializes the interface. */
	public int init()
	{
		String ifName = LrgsConfig.instance().getSangomaIfName();
		if (ifName != null)
		{
			Logger.instance().info(
				"Setting sangoma interface name to '" + ifName + "'");
			setInterfaceName(ifName);
		}

		if (initSangoma() != 0)
		{
			Logger.instance().failure(
				DomsatRecv.module + ":" + DomsatRecv.EVT_HW_INIT_FAILED
				+ " " + getErrorMsg());
			return -1;
		}
		Logger.instance().info(DomsatRecv.module + ":" 
			+ (-DomsatRecv.EVT_HW_INIT_FAILED)
			+ " DOMSAT Hardware Initialization Success.");
		return 0;
	}

	/** Enable/Disable the interface. */
	public boolean setEnabled(boolean enabled)
	{
		if (enabled)
		{
			Logger.instance().info(DomsatRecv.module + " Enabling DOMSAT HW");
			if (enable() != 0)
			{
				Logger.instance().failure(
					DomsatRecv.module + ":" + DomsatRecv.EVT_HW_CANNOT_ENABLE
					+ " " + getErrorMsg());
				disable();
				return false;
			}
			else
				Logger.instance().info(
					DomsatRecv.module + ":" + (-DomsatRecv.EVT_HW_CANNOT_ENABLE)
					+ " DOMSAT Enabled.");
		}
		else
		{
			Logger.instance().info(DomsatRecv.module
				+ " Disabling DOMSAT interface.");
			disable();
		}
		return true;
	}

	/**
	 * Retrieve a packet from the interface.
	 * @return the byte[] packet if one is available, or null if none.
	 */
	public int getPacket(byte[] packetbuf)
	{
		Logger.instance().debug3(DomsatRecv.module + " Calling readPacket()");
		try
		{
			int r = readPacket(packetbuf);
			Logger.instance().debug3(DomsatRecv.module + " readPacket() returned " + r);
			return r;
		}
		catch(ArrayIndexOutOfBoundsException ex)
		{
			Logger.instance().warning(DomsatRecv.module + " Error in native code: "
				+ ex);
			return -1; // means bad frame
		}
	}

	/** Shuts down the interface. */
	public void shutdown()
	{
		setEnabled(false);
		Logger.instance().info("Closing Sangoma Driver");
		closeSangoma();
	}

	public String getErrorMsg()
	{
		byte buf[] = new byte[512];
		getErrorMsg(buf);
		int n=0;
		for(; n<512 && buf[n] != (byte)0; n++);

		return new String(buf, 0, n);
	}

	/** Sets the interface name. */
	private native static void setInterfaceName(String ifName);

	/**
	 * Opens and binds the socket to the sangoma board.
	 * @return 0 if success, or negative error code.
	 */
	private native static int initSangoma();

	/**
	 * Reads a packet of data from the sangoma board.
	 * @return number of bytes read, 0 if no data available, or 
	 *         negative error code.
	 */
	private native static int readPacket(byte[] buf);

	/**
	 * Closes the sangoma board interface.
	 */
	private native static void closeSangoma();

	/**
	 * Enable data reception.
	 */
	private native static int enable();

	/**
	 * Disable data reception.
	 */
	private native static int disable();

	/**
	 * Return a string explanation of the last error.
	 */
	private native static void getErrorMsg(byte[] buf);

	static // Static initializer to load native library
	{
		if (System.getProperty("os.name").toLowerCase().startsWith("win"))
		{
			System.load(EnvExpander.expand(
				"$DCSTOOL_HOME/lib/WinDomsatSangoma.dll"));
		}
		else
		{
			String libname = "domsat." + getOsSuffix();
			Logger.instance().info("Loading native library " + libname);
			System.loadLibrary(libname);
		}
	}
	
	public static String getOsSuffix()
	{
		String osn = System.getProperty("os.name");
		if (osn == null)
			return "unknown";
		osn = osn.toLowerCase();
		if (osn.startsWith("win"))
			return "win";
		else if (osn.startsWith("sunos"))
			return "sol10";
		try
		{
			Process uname = Runtime.getRuntime().exec("uname -rp");
			InputStreamReader isr = new InputStreamReader(
				uname.getInputStream());
			BufferedReader bis = new BufferedReader(isr);
			String line = bis.readLine();

			// RHEL3 is Kernel version 2.4.xxxxx
			if (line.startsWith("2.4")) 
				return "el3.32";
			int bits = 32;
			String n = System.getProperty("sun.arch.data.model");
			if (n != null && n.contains("64"))
				bits = 64;
			int rhelVersion = line.contains("el5") ? 5 : 4;
			return "el" + rhelVersion + "." + bits;
		}
		catch(IOException ex)
		{
			return "unknown";
		}
	}


	/**
	 * Test main method continually receives frames and sends them to stdout.
	 */
	public static void main(String args[])
		throws Exception
	{
		DomsatTest dt = new DomsatTest();
		dt.start();
	}

//	/* (non-Javadoc)
//     * @see lrgs.domsatrecv.DomsatHardware#reset()
//     */
//    @Override
//    public void reset()
//    {
//    }
    
	public void timeout()
	{
	}
}

class DomsatTest
	extends Thread
{

}

