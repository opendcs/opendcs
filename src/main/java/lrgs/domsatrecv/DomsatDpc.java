/*
*  $Id$
*
*  This is open-source software written by Sutron Corporation, under
*  contract to the federal government. You are free to copy and use this
*  source code for your own purposes, except that no part of the information
*  contained in this file may be claimed to be proprietary.
*
*  Except for specific contractual terms between Sutron and the federal 
*  government, this source code is provided completely without warranty.
*  For more information contact: tempest@sutron.com
*
*/
package lrgs.domsatrecv;

import java.io.IOException;
import java.io.InputStream;
import java.net.SocketTimeoutException;

import ilex.util.Logger;
import ilex.net.BasicClient;

import lrgs.lrgsmain.LrgsConfig;

/**
This defines the interface to code controlling the DOMSAT interface.
*/
public class DomsatDpc
	extends BasicClient
	implements DomsatHardware
{
	private boolean _enabled = false;
	private String errMsg = "";

	// DPC Frames are Start with 2-byte length, then Addr=0x01, Ctrl=0x00,
	// Grp=0x10, Chan=0x00, Seq/Mor, 16-bit Msg Seq, PktSeq/Dup
	// The strategy: 
	// HuntMode looks for (01 00 10 00) and then backs up 2 bytes ==> LenMSB
	// LenMSB gets the MSB of the 2-byte packet len
	// LenLSB gets the LSB of the 2-byte packet len & checks reasonableness
	// DATA gets the specified # of data bytes, & returns the packet ==>LenMSB
	enum STATE { HUNT, LEN_MSB, LEN_LSB, DATA };
	private STATE state = STATE.HUNT;
	private byte[] packetbuf = null;
	private int packetLen = 0;
	private int packetIdx = 0;
	private byte[] huntbuf = new byte[6];
	private int huntlen = 0;
	private int lenmsb;
	private int huntDiscarded = 0;
	
	/**
	 * Constructor.
	 */
	public DomsatDpc()
	{
		super("localhost", 9000);
	}

	/** Initializes the interface. */
	public int init()
	{
		// Host & Port stored in BasicClient base-class.
		setHost(LrgsConfig.instance().dpcHost);
		setPort(LrgsConfig.instance().dpcPort);

		Logger.instance().info(DomsatRecv.module + ":" 
			+ (-DomsatRecv.EVT_HW_INIT_FAILED)
			+ " DOMSAT DPC Interface Configured to '"
			+ getName() + "'");
		
		return 0;
	}

	/** Enable/Disable the interface. */
	public boolean setEnabled(boolean enable)
	{
		// If I'm RE-enabling, disable first, then enable.
		if (enable && _enabled)
			setEnabled(false);
		_enabled = enable;

		if (enable)
		{
			init();
			Logger.instance().info(DomsatRecv.module + " Connecting to DOMSAT DPC at '"
				+ getName() + "'");
			try
			{
				connect();
				startHunt();
			}
			catch(Exception ex)
			{
				errMsg = 
					DomsatRecv.module + ":" + DomsatRecv.EVT_HW_CANNOT_ENABLE
					+ " Cannot connect to DOMSAT Protocol Converter (DPC): " + ex;
				Logger.instance().warning(errMsg);
				_enabled = false;
				return false;
			}
	
			Logger.instance().info(
				DomsatRecv.module + ":" + (-DomsatRecv.EVT_HW_CANNOT_ENABLE)
					+ " DOMSAT Protocol Converter (DPC) Connected.");
		}
		else
		{
			disconnect();
			Logger.instance().info(DomsatRecv.module
				+ " Disonnected from DOMSAT Proto Converter.");
		}
		return true;
	}

	/**
	 * Retrieve a packet from the interface.
	 * @return length of the packet if one is available, -2=HW Failure, -1=Bad Frame,
	 * 0=short frame, -3=pause & try again
	 */
	public int getPacket(byte[] packetbuf)
	{
		Logger.instance().debug3(DomsatRecv.module + " DPC getPacket");

		InputStream tInput = input;

		// If this method is called, then obviously I'm supposed to
		// be enabled. This means the connection to the DPC failed.
		// Periodically (every 30 seconds) retry.
		if (!_enabled || tInput == null)
		{
			if (System.currentTimeMillis() - lastConnectAttempt >= 15000L)
			{
				if (!setEnabled(true))
					return -3;
			}
			return -3;
		}

		this.packetbuf = packetbuf;
		int n = 0;
		try
		{
			while(tInput.available() > 0)
			{
				int byt = tInput.read();
				n++;
				switch(state)
				{
				case HUNT:
					if (huntlen < 6)
						huntbuf[huntlen++] = (byte)byt;
					else
					{
						huntbuf[0] = huntbuf[1];
						huntbuf[1] = huntbuf[2];
						huntbuf[2] = huntbuf[3];
						huntbuf[3] = huntbuf[4];
						huntbuf[4] = huntbuf[5];
						huntbuf[5] = (byte)byt;
						huntDiscarded++;
					}
					if (huntlen == 6
					 && huntbuf[2] == (byte)0x01 && huntbuf[3] == (byte)0x00
					 && huntbuf[4] == (byte)0x10 && huntbuf[5] == (byte)0x00)
					{
						checkLen(huntbuf[0], huntbuf[1]);
						if (state == STATE.DATA);
						{
							Logger.instance().info(DomsatRecv.module + 
								" HUNT state found HDML Header, starting read packet"
								+ ", len=" + packetLen
								+ ", discarded " + huntDiscarded 
								+ " bytes hunting.");
							// We already read 1st 4 packet bytes.
							packetbuf[0] = huntbuf[2];
							packetbuf[1] = huntbuf[3];
							packetbuf[2] = huntbuf[4];
							packetbuf[3] = huntbuf[5];
							packetIdx = 4;
						}
					}
					break;
				case LEN_MSB:
					lenmsb = byt;
					state = STATE.LEN_LSB;
					break;
				case LEN_LSB:
					checkLen(lenmsb, byt);
					break;
				case DATA:
					packetbuf[packetIdx++] = (byte)byt;
					if (packetIdx == packetLen)
					{
						if (packetbuf[0] != 0x01 || packetbuf[1] != 0x00
						 || packetbuf[2] != 0x10 || packetbuf[3] != 0x00)
						{
							startHunt();
							errMsg = "Bad packet - invalid HDLC Header"
								+ " 0x" + packetbuf[0]
								+ " 0x" + packetbuf[1]
								+ " 0x" + packetbuf[2]
								+ " 0x" + packetbuf[3];
							return -1;
						}
						state = STATE.LEN_MSB;
						return packetLen;
					}
					break;
				}
			}
		}
		catch(SocketTimeoutException ex)
		{
			Logger.instance().debug1(DomsatRecv.module
				+ " Timeout on DPC input socket.");
			return -3;
		}
		catch(IOException ex)
		{
			Logger.instance().warning(DomsatRecv.module
				+ " DPC Connection error: " + ex);
			Logger.instance().warning(DomsatRecv.module
				+ " Will attempt disconnect/reconnect.");
			errMsg = "DPC Not Enabled.";
			setEnabled(false);
			try { Thread.sleep(5000L); } catch(Exception ex2) {}
			setEnabled(true);
		}

		return -3; // -3 means pause & try again later.
	}
	
	private void checkLen(int msb, int lsb)
	{
		packetLen = ((msb&0xff) << 8) | (lsb&0xff);
		if (packetLen >= 8 && packetLen <= packetbuf.length)
		{
			state = STATE.DATA;
			packetIdx = 0;
		}
		else if (state != STATE.HUNT)
			startHunt();
	}
	
	private void startHunt()
	{
		Logger.instance().info(DomsatRecv.module + " Entering HUNT state");
		state = STATE.HUNT;
		huntlen = 0;
		huntDiscarded = 0;
	}

	/** Shuts down the interface. */
	public void shutdown()
	{
		setEnabled(false);
		Logger.instance().info(DomsatRecv.module + " Closing DPC Interface");
	}

	public String getErrorMsg()
	{
		return errMsg;
	}

//	public void reset()
//	{
//		Logger.instance().info(DomsatRecv.module
//			+ " Resetting DOMSAT DPC.");
//		setEnabled(false);
//		setEnabled(true);
//	}
	
	public void timeout()
	{
		Logger.instance().info(DomsatRecv.module
			+ " Reconnecting to DPC after timeout.");
		setEnabled(false);
		setEnabled(true);
	}

	/**
	 * Test main method continually receives frames and sends them to stdout.
	 * Usage: lrgsj lrgs.domsatrecv.DomsatDpc [host [port]]
	 */
	public static void main(String args[])
		throws Exception
	{
		DpcDomsatTest dt = new DpcDomsatTest();
		if (args.length > 0)
			dt.host = args[0];
		if (args.length > 1)
			dt.port = Integer.parseInt(args[1]);
		dt.start();
	}
}

class DpcDomsatTest
	extends Thread
{
	String host = "localhost";
	int port = 9000;
	public void run()
	{
		try
		{
			DomsatDpc ds = new DomsatDpc();
			LrgsConfig.instance().dpcHost = host;
			LrgsConfig.instance().dpcPort = port;
			
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
				else if (len == -3)
				{
					try { Thread.sleep(100L); } catch(Exception ex) {}
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

