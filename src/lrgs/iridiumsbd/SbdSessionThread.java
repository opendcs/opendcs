/*
* Open Source Software by Cove Softare, LLC.
* Prepared under contract to the U.S. Government.
* Copyright 2014 United States Government, U.S. Geological Survey
*/
package lrgs.iridiumsbd;

import java.io.*;
import java.net.*;
import java.text.*;
import java.util.*;

import decodes.datasource.IridiumPMParser;
import ilex.util.Location;
import ilex.util.EnvExpander;
import ilex.util.ByteUtil;
import ilex.net.*;
import lrgs.common.*;
import lrgs.lrgsmain.*;

/**
 * Created by listening thread when a new client connects. This class handles
 * interaction with the SBD gateway to receive a new Iridium DCP message.
 * @author mmaloney
 */
public class SbdSessionThread extends BasicSvrThread
{
	private IridiumSbdInterface parent;
	private int sessionNum;
	private DcpMsg dcpMsg = new DcpMsg();
	private NumberFormat seqNumNF = NumberFormat.getIntegerInstance();
	private NumberFormat latLonFormat = NumberFormat.getNumberInstance();
	private String capFile = null;
	private BufferedOutputStream capOutput = null;
	private int byteCount = 0;
	private int numMsgBytes = 0;
	private boolean ignoreRest = false;
	private InputStream iStream;
	private IridiumPMParser iridiumPMP = new IridiumPMParser();
	private Location location = null;
	
	/**
	 * Construct thread to read a single message from the SBD Gateway
	 * @param svr the parent listening svr
	 * @param socket the client socket I am to use
	 * @param parent the LRGS interface for SBD
	 * @param sessionNum Unique session number provided by parent
	 * @throws IOException on failure to initialize
	 */
	SbdSessionThread(BasicServer svr, Socket socket, 
		IridiumSbdInterface parent, int sessionNum)
		throws IOException
	{
		super(svr, socket);
		this.sessionNum = sessionNum;
		LrgsConfig cfg = LrgsConfig.instance();
		seqNumNF.setGroupingUsed(false);
		seqNumNF.setMinimumIntegerDigits(5);
		latLonFormat.setGroupingUsed(false);
		latLonFormat.setMaximumFractionDigits(5); // we get resolution to 1/60000 deg.
		
		// Initialize DCP Message that this thread will build.
		dcpMsg.flagbits = DcpMsgFlag.MSG_PRESENT | DcpMsgFlag.SRC_IRIDIUM | DcpMsgFlag.MSG_TYPE_IRIDIUM;
		dcpMsg.setDataSourceId(parent.getDataSourceId());

		this.parent = parent;
		socket.setSoTimeout(90000); // timeout after 90 idle seconds.
		this.iStream = socket.getInputStream();
		
		capFile = cfg.iridiumCaptureFile;
		if (capFile != null && capFile.length() > 0)
		{
			// May contain env variables and $DATE(format) time stamp.
			// May contain $SESSION
			Properties props = new Properties(System.getProperties());
			props.setProperty("SESSION", "" + sessionNum);
			capFile = EnvExpander.expand(capFile, props);
			parent.debug1("New client, captururing data to '" + capFile + "', sessionProp='"
				+ props.getProperty("SESSION") + "'");
			try
			{
				capOutput = new BufferedOutputStream(
					new FileOutputStream(
						new File(capFile)));
			}
			catch(IOException ex)
			{
				parent.warning("Error opening capture file '" + capFile + "': " + ex
					+ " -- capture disabled.");
				capOutput = null;
			}
		}
	}
	
	/**
	 * Read the next byte from the socket. Handle -1 meaning Client Hangup
	 * @return the byte as an integer
	 * @throws EOFException when -1 is received
	 * @throws IOException on other io errors
	 */
	private int nextByte()
		throws EOFException, IOException
	{
		int byt;
		if ((byt = iStream.read()) == -1)
		{
			if (!ignoreRest)
				termination(IridiumEvent.ClientHangup, null);
			parent.setStatus("Disconnected session " + sessionNum, false);
			throw new EOFException();
		}
		else
			byteCount++;

		capture(byt);
		return byt;
	}

	@Override
	protected void serviceClient()
	{
		try
		{
			byteCount = 0;
			getMessageHeader();
			infoElement1Header();
			getMessageProper();
			parent.doArchive(dcpMsg);
		}
		catch(SBDFormatException ex)
		{
			parent.failure(IridiumEvent.BadMessageFormat, 
				"session " + sessionNum + ": Format Error on connection to " + getClientName() + ": " + ex);
			ignoreRestOfMessage();
		}
		catch(Exception ex)
		{
			parent.failure(IridiumEvent.ClientHangup, 
				"session " + sessionNum + ": Error on connection to " + getClientName() + ": " + ex);
		}
		try { disconnect(); }
		catch(Exception ex) {}
	}

	/**
	 * Called on error or client hangup.
	 * @param evt event causing termination
	 * @param ex exception or null
	 */
	private void termination(IridiumEvent evt, Exception ex)
	{
		parent.failure(evt, "session " + sessionNum + ": Client "
			+ (ex == null ? "termination" : ("error: " + ex)));
	}

	@Override
	public void disconnect( )
	{
		parent.debug1("session " + sessionNum + ": Disconnecting");
		super.disconnect();
		parent.threadComplete(sessionNum);
		if (capOutput != null)
		{
			try { capOutput.close(); capOutput = null; }
			catch(Exception ex) {}
		}
	}

	
	/**
	 * State method to handle the initial header for the entire message.
	 * See Iridium spec for details
	 * @throws EOFException if client hangs up during header.
	 * @throws IOException if error during receive
	 */
	private void getMessageHeader()
		throws EOFException, IOException
	{
		int sbdProtocol = nextByte();
		numMsgBytes = (nextByte() << 8) + nextByte();
		parent.debug1("session " + sessionNum + " SBD header: protocol=" 
			+ sbdProtocol + ", numBytes = " + numMsgBytes);
	}

	/**
	 * Receive header for information element 1, containing the ID, sequence #s, etc.
	 * @throws EOFException
	 * @throws IOException
	 * @throws SBDFormatException
	 */
	private void infoElement1Header()
		throws EOFException, IOException, SBDFormatException
	{
		int elemNum = nextByte();
		if (elemNum != 1)
			throw new SBDFormatException("Initial IE must have number 1, got "
				+ elemNum);
		int elemLen = (nextByte() << 8) + nextByte();
		if (elemLen != 28)
			throw new SBDFormatException("Initial IE must have length 28, got "
				+ elemLen);
		dcpMsg.setCdrReference((nextByte() << 24) + (nextByte()<<16) + (nextByte()<<8)
			+ nextByte());
		byte buf[] = new byte[15];
		for(int i=0; i<15; i++) buf[i] = (byte)nextByte();
		dcpMsg.setDcpAddress(new DcpAddress(new String(buf)));
		dcpMsg.setSessionStatus(nextByte());
		dcpMsg.setSequenceNum((nextByte()<<8) + nextByte());
		dcpMsg.setMtmsm((nextByte()<<8) + nextByte());
		long xmitTime = (nextByte() << 24) + (nextByte()<<16) + (nextByte()<<8)
			+ nextByte();
		dcpMsg.setXmitTime(new Date(xmitTime * 1000L));
		parent.debug1("Read IE1 Header, cdrRef=" + dcpMsg.getCdrReference()
			+ ", emei='" + dcpMsg.getDcpAddress() + "', mtmsm=" + dcpMsg.getMtmsm() + ", momsm=" + 
			dcpMsg.getSequenceNum()
			+ ", sessionTime=" + new Date(xmitTime*1000L));
	}
	
	/**
	 * Retrieve the data for the message proper.
	 * @throws EOFException
	 * @throws IOException
	 * @throws SBDFormatException
	 */
	private void getMessageProper()
		throws EOFException, IOException, SBDFormatException
	{
		
		ByteArrayOutputStream msgPropStream = new ByteArrayOutputStream();
		while(byteCount < numMsgBytes + 3)
			nextInfoElem(msgPropStream);
		
		byte msgPropBytes[] = msgPropStream.toByteArray();

		StringBuffer hdr = new StringBuffer();
		hdr.append("ID=");
		hdr.append(dcpMsg.getDcpAddress().toString());
		hdr.append(",TIME=");
		hdr.append(iridiumPMP.getGoesDateFormat().format(dcpMsg.getXmitTime()));
		hdr.append(",STAT=");
		hdr.append(dcpMsg.getSessionStatus() < 10 ? "0" : "");
		hdr.append("" + dcpMsg.getSessionStatus());
		hdr.append(",MO=");
		hdr.append(seqNumNF.format(dcpMsg.getSequenceNum()));
		hdr.append(",MT=");
		hdr.append(seqNumNF.format(dcpMsg.getMtmsm()));
		hdr.append(",CDR=");
		hdr.append((new DcpAddress(dcpMsg.getCdrReference()).toString()));
		
		if (location != null)
			hdr.append(",LAT=" + latLonFormat.format(location.getLatitude())
				+ ",LON=" + latLonFormat.format(location.getLongitude())
				+ ",RAD=" + latLonFormat.format(location.getRadius()));
		hdr.append(" ");
		
		ByteArrayOutputStream msgStream = new ByteArrayOutputStream();
		msgStream.write(hdr.toString().getBytes());
		msgStream.write(msgPropBytes);

		byte[] messageData = msgStream.toByteArray();
		if (byteCount != numMsgBytes + 3)
			parent.warning("Iridium format problem: num bytes from hdr=" + numMsgBytes
				+ ", bytes read=" + byteCount
				+ ", should be " + (numMsgBytes+3));
		dcpMsg.setData(messageData);
	}

	/**
	 * Get next information element.
	 * @param baos
	 * @throws EOFException
	 * @throws IOException
	 * @throws SBDFormatException
	 */
	private void nextInfoElem(ByteArrayOutputStream baos)
		throws EOFException, IOException, SBDFormatException
	{
		int ieStart = byteCount;
		
		byte elemHdr[] = new byte[3];
		for(int i=0; i<3; i++)
			elemHdr[i] = (byte)nextByte();
		
		int elemLen = (((int)elemHdr[1] & 255)<<8) + ((int)elemHdr[2] & 255);
		
		parent.debug1("Start pos=" + ieStart 
			+ ", IE[" + (int)elemHdr[0] + "] len=" + elemLen
			+ ", bytes(hex): " + Integer.toHexString((int)(elemHdr[0]&0xff))
			+ " " + Integer.toHexString((int)(elemHdr[1]&0xff))
			+ " " + Integer.toHexString((int)(elemHdr[2]&0xff)));
		
		if (elemHdr[0] == 3 && elemLen == 11)
		{
			int firstByte = nextByte();
			
			// Latitude in minutes/1000:
			boolean latPositive = (firstByte & 0x2) == 0;
			double lat = (double)nextByte() + ((nextByte()<<8) + nextByte()) / 60000.;
			if (!latPositive) lat = -lat;

			// Longitued in minutes/1000:
			boolean lonPositive = (firstByte & 0x1) == 0;
			double lon = (double)nextByte() + ((nextByte()<<8) + nextByte()) / 60000.;
			if (!lonPositive) lon = -lon;
			
			double rad = (double)((nextByte()<<24) + (nextByte()<<16) + (nextByte()<<8) + nextByte());
			location = new Location();
			location.setLatitude(lat);
			location.setLongitude(lon);
			location.setRadius(rad);
			parent.debug1("Received location: " + location);
		}
		else
		{
			if (byteCount-3 + elemLen > numMsgBytes)
				throw new SBDFormatException(
					"IE[" + (int)elemHdr[0] 
					+ "] length too long. Starts at pos=" + byteCount
					+ " with length=" + elemLen + " but total msg len="
					+ numMsgBytes);
			baos.write("IE:".getBytes());
			baos.write(ByteUtil.toHexString(elemHdr).getBytes());
			baos.write((byte)' ');
			
			for(int i=0; i<elemLen; i++)
				baos.write(nextByte());
		}
	}
	
	private void capture(int b)
	{
		if (capOutput != null)
			try
			{
				capOutput.write(b);
			}
			catch (IOException ex)
			{
				parent.warning("Error writing to capture file '" + capFile
					+ "': " + ex + " -- capture disabled for remainder of data.");
				try { capOutput.close(); } catch(IOException ex2) {}
				capOutput = null;
			}
	}

	private void ignoreRestOfMessage()
	{
		parent.warning("Ignoring rest of message.");
		ignoreRest = true;
		int nbytes = 0;
		try 
		{
			while(nextByte() != -1)
				nbytes++;
		}
		catch(IOException ex) {}
		parent.warning("" + nbytes + " ignored after format error.");
	}
	

	
}
