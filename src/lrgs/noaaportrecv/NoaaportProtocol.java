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
*  For more information contact: info@ilexeng.com
*/
package lrgs.noaaportrecv;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.TimeZone;

import ilex.util.ByteUtil;
import ilex.util.EnvExpander;
import ilex.util.Logger;
import lrgs.common.DcpMsg;
import lrgs.iridium.IridiumRecv;
import lrgs.lrgsmain.LrgsConfig;

/**
Handles the parsing of messages from the NOAAPORT socket.
*/
public class NoaaportProtocol
{
	protected NoaaportRecv noaaportRecv;
	protected InputStream input;

	enum States { HUNT, SEQNUM, PROPHEADER, DCPMSG };
	States currentState;
	private byte header_buf[];
	private int hb_len = 0;
	protected byte message_buf[];
	protected int mb_len = 0;
	private static int HEADER_MAX = 100;
	protected static int MESSAGE_MAX = 20000;
	protected NoaaportConnection parent;
	protected String clientName;
	private boolean seqNumPresent = false;
	private int seqNum;
	private BufferedOutputStream captureStream = null;
	private String captureFileName = null;

	public NoaaportProtocol(InputStream input, NoaaportRecv noaaportRecv,
		NoaaportConnection parent, String clientName)
		throws IOException
	{
		this.input = input;
		this.noaaportRecv = noaaportRecv;
		this.parent = parent;
		this.clientName = clientName;
		
		// Sequence #s are present for Unisys and PDI with passthrough. Not Marta.
		// Also for Unisys, we are the client. Thus use that to detect:
		seqNumPresent = parent instanceof NoaaportClient;
	
		header_buf = new byte[HEADER_MAX];
		message_buf = new byte[MESSAGE_MAX];
		hb_len = mb_len = 0;
		currentState = States.HUNT;
		
		captureFileName = EnvExpander.expand(
			LrgsConfig.instance().noaaportCaptureFile);
		if (captureFileName != null && captureFileName.trim().length() > 0)
		{
			SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd-HHmmss");
			sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
			captureFileName = captureFileName + "-" + sdf.format(new Date());
			info(" New client, captureFile = '" + captureFileName + "'");

			File f = new File(EnvExpander.expand(captureFileName));
			try
			{
				captureStream = new BufferedOutputStream(
					new FileOutputStream(f));
			}
			catch(IOException ex)
			{
				warning("" + IridiumRecv.EVT_BAD_CONFIG
					+ " Cannot open capture file '" + f.getPath() + "': " 
					+ ex);
				captureStream = null;
			}
		}

	}

	protected int readByte()
		throws IOException
	{
		int c = input.read();
		if (c == -1)
		{
			warning("NOAAPORT receiver hung up.");
			disconnect();
			return -1;
		}

		c = c & 0xff;
		if (captureStream != null)
			captureStream.write(c);

		return c;
	}
	
	/**
	 * Repeatedly called from base-class until connection is broken.
	 */
	protected void read()
	{
		noaaportRecv.setStatus("Receiving");
		try
		{
			// Attempt to read some data from the client.
			switch(currentState)
			{
			case HUNT: 
				huntState(); 
				break;
			case SEQNUM:
				seqNumState();
				break;
			case PROPHEADER: 
				productHeaderState(); 
				break;
			case DCPMSG: 
				dcpmsgState(); 
				break;
			default:
				warning("Unknown state " + currentState
					+ " -- disconnecting.");
				disconnect();
				break;
			}
		}
		catch(IOException ex)
		{
			info("" + NoaaportRecv.EVT_RECV_FAILED
				+ " Error on connection to " + clientName + ": " + ex);
			disconnect();
		}
	}

	/**
	 * Look for CTRL-A (SOH).
	 * @throws IOException
	 */
	private void huntState()
		throws IOException
	{
		int c = -1;
		while(parent.isConnected() && (c = readByte()) != -1)
		{
			if (c == 0x01)
			{
				currentState = 
					seqNumPresent? States.SEQNUM : States.PROPHEADER;
				hb_len = mb_len = 0;
				seqNum = -1;
				return;
			}
		}
	}
	
	/**
	 * Skip white space \r\r\n then parse digits into sequence number
	 */
	private void seqNumState()
		throws IOException
	{
		int i = readByte();
		if (i == -1)
			return;
		
		char c = (char)i;
		if (Character.isWhitespace(c))
		{
			// whitespace after seqnum -- switch to PROPHEADER
			if (seqNum != -1)
				currentState = States.PROPHEADER;
			return;
		}
		else if (Character.isDigit(c))
		{
			if (seqNum == -1)
				seqNum = i;
			else
				seqNum = (seqNum*10) + (i - 48);
		}
	}

	/**
	 * Header is after SOH (01) up until 1E (EOH?).
	 * Collect header data in header_buf.
	 * Verify that it has KWAL and type 'S'.
	 * @throws IOException
	 */
	private void productHeaderState()
		throws IOException
	{
		int c = readByte();
		if (c == -1)
			return;
		
		// Skip initial whitespace
		if (Character.isWhitespace((char)c) && hb_len == 0)
			return;
		else if (c == 0x1e) // 0x1E means end of header
		{
			if (hb_len == 0)
			{
				warning(":" + NoaaportRecv.EVT_HEADER_PARSE
					+ " No data in WMO header, len=" + hb_len);
				currentState = States.HUNT;
				return;
			}

			if ((char)header_buf[0] != 'S')
			{
				info(" Skipping non-DCP-message with WMO header '" + 
					(new String(header_buf, 0, hb_len < 6 ? hb_len : 6)) + "'");
				currentState = States.HUNT;
				return;
			}
			
			if (hb_len <= 6)
			{
				debug(":" + NoaaportRecv.EVT_HEADER_PARSE
					+ " No data after WMO header, len=" + hb_len);
				currentState = States.HUNT;
				return;
			}

			if (hb_len < 11)
			{
				debug(":" + NoaaportRecv.EVT_HEADER_PARSE
					+ " No office-ID header, len=" + hb_len);
				currentState = States.HUNT;
				return;
			}

			// Office ID must be 'KWAL'
			String officeId = new String(header_buf, 7 , 4);
			if (!officeId.equals("KWAL"))
			{
				info(" Skipping non-DCP-message with office '" + officeId + "'");
				currentState = States.HUNT;
				return;
			}

			mb_len = 0;
			currentState = States.DCPMSG;
		}
		else if (c == 0x01)
		{
			debug(":" + NoaaportRecv.EVT_HEADER_PARSE
				+ " Unexpected SOH. No 0x1e seen. Discarding " 
				+ hb_len + " bytes.");
			currentState = States.PROPHEADER;
			hb_len = mb_len = 0;
			return;
		}
		else
		{
			header_buf[hb_len++] = (byte)c;
			if (hb_len == HEADER_MAX)
			{
				debug(":" + NoaaportRecv.EVT_HEADER_PARSE
					+ " Header too long before 0x1E");
				currentState = States.HUNT;
			}
		}
	}

	private void dcpmsgState()
		throws IOException
	{
		int c = readByte();
		if (c == -1)
			return;

		if (c == 0x03)
		{
			if (mb_len < 29)
				debug(":" + NoaaportRecv.EVT_MESSAGE_PARSE
					+ " Message too short, len=" + mb_len
					+ ", hdr='" + (new String(header_buf, 0, hb_len))
					+ "', msg='" + (new String(message_buf, 0, mb_len)));
			else
				processMessage();
			currentState = States.HUNT;
		}
		else if (c == 0x01)
		{
			debug(":" + NoaaportRecv.EVT_MESSAGE_PARSE
				+ " Unexpected SOH. No ETX seen. Discarding " 
				+ mb_len + " bytes.");
			currentState = States.PROPHEADER;
			hb_len = mb_len = 0;
			return;
		}
		else
		{
			message_buf[mb_len++] = (byte)c;
			if (mb_len == MESSAGE_MAX)
			{
				debug(":" + NoaaportRecv.EVT_MESSAGE_PARSE
					+ " Message too long before 0x03");
				currentState = States.HUNT;
			}
		}
	}

	/** Called when we now have a message sitting in the buffer. */
	protected void processMessage()
	{
debug(" Processing buffered message of length " + mb_len
+ " '" + new String(message_buf) + "' byte[0]=" + message_buf[0]
+ ", byte[1]=" + message_buf[1]);

		// Remove white space from the end of the message buffer
		while(mb_len > 0 && Character.isWhitespace((char)message_buf[mb_len-1]))
			mb_len--;
		// CCCS should now be end of buffer, where CCC is 3-digit channel
		// and S is 'E' or 'W'
		
		// Convert the NOAAPORT format to DOMSAT
		// (18=header bytes before msg, 12 = trailer bytes after msg)
		int domsatLen = mb_len - 18 - 12;
		byte domsatBuf[] = new byte[domsatLen + DcpMsg.IDX_DATA];

		for(int i=0; i<8; i++)
		{
			byte c = message_buf[i];
			if (!ByteUtil.isHexChar(c))
			{
				warning("" + NoaaportRecv.EVT_MESSAGE_PARSE
					+ " non-hex-digit '" + (char)c 
					+ "' at position " + i 
					+ " in DCP address field -- msg skipped.");
				currentState = States.HUNT;
				return;
			}
			domsatBuf[DcpMsg.IDX_DCP_ADDR + i] = c;
		}

		// Assume my clock is up-to-date. So day-of-year in the msg
		// should always be <= today. If it's > today, assume that it's
		// from the previous year.
		int day = ((int)message_buf[9] - (int)'0') * 100
			+ ((int)message_buf[10] - (int)'0') * 10
			+ ((int)message_buf[11] - (int)'0');
		GregorianCalendar cal = new GregorianCalendar();
		cal.setTimeZone(TimeZone.getTimeZone("UTC"));
		int year = cal.get(Calendar.YEAR);
		if (day > cal.get(Calendar.DAY_OF_YEAR))
			--year;
		domsatBuf[DcpMsg.IDX_YEAR] = (byte)((byte)'0' + ((year % 100) / 10));
		domsatBuf[DcpMsg.IDX_YEAR+1] = (byte)((int)'0' + (year % 10));

		for(int i=0; i<9; i++)
		{
			char c = (char)message_buf[i+9];
			if (!Character.isDigit(c))
			{
				warning("" + NoaaportRecv.EVT_MESSAGE_PARSE
					+ " non-digit in Date/Time field -- msg skipped.");
				currentState = States.HUNT;
				return;
			}
			domsatBuf[DcpMsg.IDX_DAY + i] = message_buf[i+9];
		}

		domsatBuf[DcpMsg.IDX_FAILCODE] = 
			message_buf[8] == (byte)'?' ? (byte)'?' : (byte)'G';

		domsatBuf[DcpMsg.IDX_SIGSTRENGTH] = message_buf[mb_len-11];
		domsatBuf[DcpMsg.IDX_SIGSTRENGTH+1] = message_buf[mb_len-10];
		domsatBuf[DcpMsg.IDX_FREQOFFSET] = message_buf[mb_len-9];
		domsatBuf[DcpMsg.IDX_FREQOFFSET+1] = message_buf[mb_len-8];
		domsatBuf[DcpMsg.IDX_MODINDEX] = message_buf[mb_len-7];
		domsatBuf[DcpMsg.IDX_DATAQUALITY] = message_buf[mb_len-6];

		for(int i=0; i<3; i++)
		{
			char c = (char)message_buf[mb_len-4+i];
			if (c == ' ')
				c = '0';
			else if (!Character.isDigit(c))
			{
				warning("" + NoaaportRecv.EVT_MESSAGE_PARSE
					+ " non-digit in channel field '"
					+ (new String(message_buf, mb_len-4, 3))
					+ "' -- msg skipped.");
				currentState = States.HUNT;
				return;
			}
			domsatBuf[DcpMsg.IDX_GOESCHANNEL+i] = (byte)c;
		}
		domsatBuf[DcpMsg.IDX_GOES_SC] = message_buf[mb_len-1];
//Logger.instance().debug1("channel/sc field '" + new String(domsatBuf, DcpMsg.IDX_GOESCHANNEL, 4) + "'");

		domsatBuf[DcpMsg.DRGS_CODE] = (byte)'N';
		domsatBuf[DcpMsg.DRGS_CODE+1] = (byte)'P';

		domsatBuf[DcpMsg.IDX_DATALENGTH] = 
			(byte)(domsatLen / 10000 + (int)'0');
		domsatBuf[DcpMsg.IDX_DATALENGTH+1] = 
			(byte)((domsatLen%10000) / 1000 + (int)'0');
		domsatBuf[DcpMsg.IDX_DATALENGTH+2] = 
			(byte)((domsatLen%1000) / 100 + (int)'0');
		domsatBuf[DcpMsg.IDX_DATALENGTH+3] = 
			(byte)((domsatLen%100) / 10 + (int)'0');
		domsatBuf[DcpMsg.IDX_DATALENGTH+4] = 
			(byte)((domsatLen%10) + (int)'0');

		for(int i=0; i<domsatLen; i++)
			domsatBuf[DcpMsg.IDX_DATA+i] = message_buf[18+i];

		DcpMsg msg = new DcpMsg(domsatBuf, domsatLen + 37, 0);
		if (seqNumPresent && seqNum >= 0)
			msg.setSequenceNum(seqNum);
		noaaportRecv.archive(msg);
	}
	
	protected void warning(String msg)
	{
		Logger.instance().warning(noaaportRecv.module + ":" + msg);
	}
	
	protected void debug(String msg)
	{
		Logger.instance().debug1(noaaportRecv.module + ":" + msg);
	}
	protected void info(String msg)
	{
		Logger.instance().info(noaaportRecv.module + ":" + msg);
	}
	

	protected void disconnect( )
	{
		if (captureStream != null)
		{
			try { captureStream.close(); captureStream = null; }
			catch(Exception ex) {}
		}
		noaaportRecv.setStatus("Disconnected");
		info("Disconnecting from " + clientName);
		parent.disconnect();
	}
}
