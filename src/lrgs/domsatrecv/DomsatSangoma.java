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

import ilex.util.EnvExpander;
import ilex.util.Logger;
import ilex.jni.OsSuffix;

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
			String libname = "domsat." + OsSuffix.getOsSuffix();
			Logger.instance().info("Loading native library " + libname);
			System.loadLibrary(libname);
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
	public void run()
	{
		try
		{
			DomsatSangoma ds = new DomsatSangoma();
			ds.init();
			if (!ds.setEnabled(true))
			{
				System.out.println("Enable failed -- see log messages.");
				System.exit(1);
			}
			byte packet[] = new byte[1024];
			while(true)
			{
				int len = ds.getPacket(packet);
				if (len == 0)
					System.out.println("Timeout");
				else if (len == -1)
					System.out.println("Recoverable Error: " + ds.getErrorMsg());
				else if (len == -2)
				{
					System.out.println("Fatal Error: " + ds.getErrorMsg());
					break;
				}
				else
				{
					System.out.println("Frame received, len=" + len);
					System.out.println(" "
						+ Integer.toHexString((int)packet[0] & 0xff) + ", "
						+ Integer.toHexString((int)packet[1] & 0xff) + ", "
						+ Integer.toHexString((int)packet[2] & 0xff) + ", "
						+ Integer.toHexString((int)packet[3] & 0xff) + ", "
						+ Integer.toHexString((int)packet[4] & 0xff) + ", "
						+ Integer.toHexString((int)packet[5] & 0xff) + ", "
						+ Integer.toHexString((int)packet[6] & 0xff) + ", "
						+ Integer.toHexString((int)packet[7] & 0xff) + ", ... "
						+ Integer.toHexString((int)packet[len-2] & 0xff) + ", "
						+ Integer.toHexString((int)packet[len-1] & 0xff));
				}
			}
			ds.shutdown();
		}
		catch(Exception ex)
		{
			System.err.println("Exception in get-thread: " + ex);
		}
	}
}

