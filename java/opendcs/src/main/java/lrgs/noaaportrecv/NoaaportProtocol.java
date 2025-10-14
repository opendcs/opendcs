/*
* Where Applicable, Copyright 2025 OpenDCS Consortium and/or its contributors
*
* Licensed under the Apache License, Version 2.0 (the "License"); you may not
* use this file except in compliance with the License. You may obtain a copy
* of the License at
*
*   http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
* WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
* License for the specific language governing permissions and limitations
* under the License.
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

import org.opendcs.utils.logging.OpenDcsLoggerFactory;
import org.slf4j.Logger;

import ilex.util.ByteUtil;
import ilex.util.EnvExpander;
import lrgs.common.DcpMsg;
import lrgs.lrgsmain.LrgsConfig;

/**
Handles the parsing of messages from the NOAAPORT socket.
*/
public class NoaaportProtocol
{
	private static final Logger log = OpenDcsLoggerFactory.getLogger();
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
			log.info(" New client, captureFile = '{}'", captureFileName);

			File f = new File(EnvExpander.expand(captureFileName));
			try
			{
				captureStream = new BufferedOutputStream(
					new FileOutputStream(f));
			}
			catch(IOException ex)
			{
				log.atWarn().setCause(ex).log("Cannot open capture file '{}'", f.getPath());
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
			log.warn("NOAAPORT receiver hung up.");
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
				log.warn("Unknown state {} -- disconnecting.", currentState);
				disconnect();
				break;
			}
		}
		catch(IOException ex)
		{
			log.atError()
			   .setCause(ex)
			   .log("{} Error on connection to {}", NoaaportRecv.EVT_RECV_FAILED, clientName);
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
				log.warn("{} No data in WMO header, len={}", NoaaportRecv.EVT_HEADER_PARSE, hb_len);
				currentState = States.HUNT;
				return;
			}

			if ((char)header_buf[0] != 'S')
			{
				log.info("Skipping non-DCP-message with WMO header '{}'",
						(new String(header_buf, 0, hb_len < 6 ? hb_len : 6)));
				currentState = States.HUNT;
				return;
			}

			if (hb_len <= 6)
			{
				log.debug("{} No data after WMO header, len={}", NoaaportRecv.EVT_HEADER_PARSE, hb_len);
				currentState = States.HUNT;
				return;
			}

			if (hb_len < 11)
			{
				log.debug("No office-ID header, len={}", NoaaportRecv.EVT_HEADER_PARSE, hb_len);
				currentState = States.HUNT;
				return;
			}

			// Office ID must be 'KWAL'
			String officeId = new String(header_buf, 7 , 4);
			if (!officeId.equals("KWAL"))
			{
				log.info(" Skipping non-DCP-message with office '{}'", officeId);
				currentState = States.HUNT;
				return;
			}

			mb_len = 0;
			currentState = States.DCPMSG;
		}
		else if (c == 0x01)
		{
			log.debug("Unexpected SOH. No 0x1e seen. Discarding {} bytes.",
					  NoaaportRecv.EVT_HEADER_PARSE, hb_len);
			currentState = States.PROPHEADER;
			hb_len = mb_len = 0;
			return;
		}
		else
		{
			header_buf[hb_len++] = (byte)c;
			if (hb_len == HEADER_MAX)
			{
				log.debug("{} Header too long before 0x1E", NoaaportRecv.EVT_HEADER_PARSE);
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
			{
				log.debug("{} Message too short, len={}, hdr='{}', msg='{}'",
						  NoaaportRecv.EVT_MESSAGE_PARSE, mb_len,
						  (new String(header_buf, 0, hb_len)), (new String(message_buf, 0, mb_len)));
			}
			else
				processMessage();
			currentState = States.HUNT;
		}
		else if (c == 0x01)
		{
			log.debug("{} Unexpected SOH. No ETX seen. Discarding {} bytes",
					  NoaaportRecv.EVT_MESSAGE_PARSE, mb_len);
			currentState = States.PROPHEADER;
			hb_len = mb_len = 0;
			return;
		}
		else
		{
			message_buf[mb_len++] = (byte)c;
			if (mb_len == MESSAGE_MAX)
			{
				log.debug("{} Message too long before 0x03", NoaaportRecv.EVT_MESSAGE_PARSE);
				currentState = States.HUNT;
			}
		}
	}

	/** Called when we now have a message sitting in the buffer. */
	protected void processMessage()
	{
		log.debug("Processing buffered message of length {} '{}' byte[0]={}, byte[1]={}",
				  mb_len, new String(message_buf), message_buf[0], message_buf[1]);

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
				log.warn("{} non-hex-digit '{}' at position {} in DCP address field -- msg skipped.",
						 NoaaportRecv.EVT_MESSAGE_PARSE, (char)c, i);
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
				log.warn("{} non-digit in Date/Time field -- msg skipped.", NoaaportRecv.EVT_MESSAGE_PARSE);
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
				log.warn("{} non-digit in channel field '{}' -- msg skipped.",
						 NoaaportRecv.EVT_MESSAGE_PARSE, (new String(message_buf, mb_len-4, 3)));
				currentState = States.HUNT;
				return;
			}
			domsatBuf[DcpMsg.IDX_GOESCHANNEL+i] = (byte)c;
		}
		domsatBuf[DcpMsg.IDX_GOES_SC] = message_buf[mb_len-1];

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



	protected void disconnect( )
	{
		if (captureStream != null)
		{
			try { captureStream.close(); captureStream = null; }
			catch(Exception ex) {}
		}
		noaaportRecv.setStatus("Disconnected");
		log.info("Disconnecting from {}", clientName);
		parent.disconnect();
	}
}