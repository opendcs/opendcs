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

import ilex.util.ArrayUtil;

import java.io.IOException;
import java.io.InputStream;

import org.opendcs.utils.logging.OpenDcsLoggerFactory;
import org.slf4j.Logger;

import lrgs.noaaportrecv.NoaaportProtocol.States;

/**
 * This class extends the normal NoaaportProtocol for the PDI
 * (Planetary Data Inc) NOAAPORT socket protocol.
 * The protocol divides the interaction into packets. Packets
 * must be received and the entire message reconstructed.
 */
public class PdiNoaaportProtocol extends NoaaportProtocol
{
	private static final Logger log = OpenDcsLoggerFactory.getLogger();
	private enum ByteState { StartFlag, FrameSize, DataFrame, MethodFlag, HeaderString };
	private ByteState byteState = ByteState.StartFlag;
	private int frameSizeByte = 0, dataFrameByte = 0, headerStringByte = 0;
	private int dataFrameLength = 0;
	private byte dataFrame[] = null;
	private int numMessagesReceived = 0;

	PdiNoaaportProtocol(InputStream input, NoaaportRecv noaaportRecv,
		NoaaportConnection parent, String clientName)
		throws IOException
	{
		super(input, noaaportRecv, parent, clientName);
		mb_len = 0;
	}

	/**
	 * This method is called repeatedly by the thread servicing this client.
	 */
	protected void read()
	{
		noaaportRecv.setStatus("Receiving");

		try { while(byteProtocol() != -1); }
		catch(IOException ex)
		{
			log.atWarn()
			   .setCause(ex)
			   .log("{} Error on connection to {}, numMessagesReceived={}",
			   		NoaaportRecv.EVT_RECV_FAILED, clientName, numMessagesReceived);
			disconnect();
		}
	}

	/**
	 * ByteState steps through packet bytes to construct packets.
	 * It buffers packet data and calls packetProtocol when it has a complete one.
	 */
	private int byteProtocol()
		throws IOException
	{
		int c = readByte();
		if (c != -1)
			switch(byteState)
			{
			case StartFlag:
				startFlag(c);
				break;
			case FrameSize:
				frameSize(c);
				break;
			case DataFrame:
				dataFrame(c);
				break;
			case MethodFlag:
				methodFlag(c);
				break;
			case HeaderString:
				headerString(c);
				break;
			}
		return c;
	}

	private void startFlag(int c)
		throws IOException
	{
		log.debug("startFlag={}", c);
		switch(c)
		{
		case 0: // data frame
			frameSizeByte = 0;
			dataFrameLength = 0;
			byteState = ByteState.FrameSize;
			break;
		case 1: // header frame
			headerStringByte = 0;
			byteState = ByteState.MethodFlag;
			break;
		case 2: // tail frame
			processMessage(); // in parent NoaaportProtocol class.
			byteState = ByteState.StartFlag;
			break;
		case 3: // uncompressed data frame
			throw new IOException("Uncompressed Data Frame Error received.");
		case 4: // abort message
			log.warn("Abort Frame Received");
			frameSizeByte = dataFrameLength = 0;
			dataFrame = null;
			byteState = ByteState.StartFlag;
			break;
		default:
			throw new IOException("Invalid PDI packet start flag " + c);
		}
	}

	private void frameSize(int c)
		throws IOException
	{
		// Sometimes frames are 0 filled, sometimes, space filled. Allow either.
		if (c == (int)' ' && dataFrameLength == 0)
			c = (int)'0';

		if (!Character.isDigit(c))
			throw new IOException(
				"Non-digit in PDI frame size field '" + c
				+ "' (" + c + "): Frame aborted.");
		dataFrameLength = dataFrameLength * 10 + (c - (int)'0');
		if (++frameSizeByte >= 4)
		{
			byteState = ByteState.DataFrame;
			dataFrameByte = 0;
			dataFrame = new byte[dataFrameLength];
			log.debug("frameLength={}", dataFrameLength);
		}
	}

	private void dataFrame(int c)
		throws IOException
	{
		dataFrame[dataFrameByte++] = (byte)c;
		if (dataFrameByte == dataFrameLength)
		{
			processDataFrame();
			byteState = ByteState.StartFlag;
			dataFrameLength = 0;
			dataFrame = null;
		}
	}

	/** First byte after a startFlag = Header */
	private void methodFlag(int c)
	{
		log.debug("PdiNoaaportProtocol: Header method={}", c);
		headerStringByte = 0;
		byteState = ByteState.HeaderString;
	}

	/** 256 bytes of 'header' that we ignore. */
	private void headerString(int c)
	{
		// Ignore header strings.
		if (++headerStringByte >= 256)
			byteState = ByteState.StartFlag;
	}

	private void processDataFrame()
		throws IOException
	{
		if (dataFrameLength + mb_len > MESSAGE_MAX)
			throw new IOException("Message too long. Max is " + MESSAGE_MAX);
		for(int i=0; i<dataFrameLength; i++)
			message_buf[mb_len++] = dataFrame[i];
		log.debug(" buffered data frame with length={}, total buffer length now={}", dataFrameLength, mb_len);
	}

	protected void processMessage()
	{
		int escapePosition = 0;
		for(; escapePosition <= mb_len
			&& message_buf[escapePosition] != (byte)0x1E; escapePosition++)
			;
		if (escapePosition >= mb_len)
		{
			log.warn("{} No product header in NOAAPORT Message -- skipped.", NoaaportRecv.EVT_HEADER_PARSE);
			return;
		}

		String hdr = new String(message_buf, 0, escapePosition);
		hdr = hdr.trim();
		log.debug("WMO header '{}'", hdr);
		if (hdr.length() < 18)
		{
			log.warn("{} Empty or short WMO header '{}' -- message skipped.", NoaaportRecv.EVT_HEADER_PARSE, hdr);
			return;
		}

		if (hdr.charAt(0) != 'S')
		{
			log.warn("Skipping non-DCP-message with WMO header '{}'", hdr);
			return;
		}

		// Office ID must be 'KWAL'
		String officeId = hdr.substring(7, 11);
		if (!officeId.equals("KWAL"))
		{
			log.warn(" Skipping non-DCP-message with office '{}'", officeId);
			return;
		}

		byte tmb[] = message_buf;
		message_buf = ArrayUtil.getField(message_buf, escapePosition+1,
			mb_len - (escapePosition+1));
		mb_len -= (escapePosition+1);
		log.debug("Processing message '{}'", new String(message_buf));
		// Parent class NoaaportProtocol to process message buffer proper.
		super.processMessage();
		message_buf = tmb;
		mb_len = 0;
		numMessagesReceived++;
	}

}