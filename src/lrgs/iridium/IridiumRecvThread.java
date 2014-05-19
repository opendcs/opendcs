/*
* Open Source Software by Cove Softare, LLC
*/
package lrgs.iridium;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

import decodes.util.DecodesSettings;

import ilex.util.ByteUtil;
import ilex.util.EnvExpander;
import ilex.util.Logger;
import ilex.net.BasicSvrThread;
import ilex.net.BasicServer;
import ilex.net.FileSendClient;
import lrgs.common.DcpAddress;
import lrgs.common.DcpMsg;
import lrgs.common.DcpMsgFlag;
import lrgs.lrgsmain.LrgsConfig;

/**
Handles the parsing of messages from the Iridium DirectIP socket.
*/
public class IridiumRecvThread
	extends BasicSvrThread
{
	private IridiumRecv iridiumRecv;
	private InputStream input;
	private int msgLength = 0;
	private long cdrRef = 0;
	private BufferedOutputStream captureStream = null;
	private String emei = null;
	private int sessionStatus = 0;
	private int momsm = 0;
	private int mtmsm = 0;
	private long sessionTimeT = 0L;
	private byte[] messageData = null;
	private SimpleDateFormat dateFmt = 
		new SimpleDateFormat("yyDDDHHmmss");
	private NumberFormat msmFmt = NumberFormat.getIntegerInstance();
	private Date msgTime = null;
	private int numBytesRead = 0;
	private int protoVersion;
	private boolean ignoring = false;
	private String captureFileName = null;
	private double lat = 0.0;
	private double lon = 0.0;
	private double cepRadius = 0.0;
	private NumberFormat locFmt = NumberFormat.getNumberInstance();
	private boolean ie3seen = false;
	private InetAddress sourceAddr = null;
	

	IridiumRecvThread(BasicServer parent, Socket socket, 
		IridiumRecv iridiumRecv, int connectNum)
		throws IOException
	{
		super(parent, socket);
		dateFmt.setTimeZone(TimeZone.getTimeZone("UTC"));
		msmFmt.setMinimumIntegerDigits(5);
		msmFmt.setGroupingUsed(false);
		this.iridiumRecv = iridiumRecv;
		socket.setSoTimeout(60000);
		this.input = socket.getInputStream();
		captureFileName = EnvExpander.expand(
			LrgsConfig.instance().iridiumCaptureFile);
		if (captureFileName != null && captureFileName.trim().length() > 0)
		{
			SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd-HHmmss");
			sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
			captureFileName = captureFileName + "-" + sdf.format(new Date()) + "." + connectNum;
			Logger.instance().debug1(IridiumRecv.module + 
				" New client, captureFile = '" + captureFileName + "'");

			File f = new File(EnvExpander.expand(captureFileName));
			try
			{
				captureStream = new BufferedOutputStream(
					new FileOutputStream(f));
			}
			catch(IOException ex)
			{
				Logger.instance().warning(IridiumRecv.module + ":" 
					+ IridiumRecv.EVT_BAD_CONFIG
					+ " Cannot open capture file '" + f.getPath() + "': " 
					+ ex);
				captureStream = null;
			}
		}
		locFmt.setGroupingUsed(false);
		locFmt.setMaximumFractionDigits(5); // we get resolution to 1/60000 deg.
		sourceAddr = socket.getInetAddress();
	}

	public void disconnect( )
	{
		Logger.instance().debug3(IridiumRecv.module + " Disconnect: Capture file '"
			+ captureFileName + "' captureStream is "
			+ (captureStream == null ? "" : "NOT") + " null");
		super.disconnect();
		iridiumRecv.listener.clientDisconnect();
		if (captureStream != null)
		{
			try { captureStream.close(); captureStream = null; }
			catch(Exception ex) {}
			
			// If the special iridiumForward is set, do the forwarding
			String host = LrgsConfig.instance().iridiumForwardHost;
			int port = LrgsConfig.instance().iridiumForwardPort;
//Logger.instance().debug3(IridiumRecv.module + " forwarding host='" + host + "', port=" + port);
			if (host != null && host.trim().length() > 0)
			{
				if (host.equalsIgnoreCase(sourceAddr.getHostAddress()))
				{
					Logger.instance().debug3(IridiumRecv.module + 
						" NOT Forwarding back to source IP Address");
				}
				else
				{
					Logger.instance().debug3(IridiumRecv.module + 
						" Forwarding '" + captureFileName + "' to "
						+ host + ":" + port);
					host = host.trim();
					try
					{
						FileSendClient.sendFile(captureFileName, host, port);
					}
					catch (IOException ex)
					{
						Logger.instance().warning(IridiumRecv.module
							+ " Could not forward '" + captureFileName + "' to "
							+ host + ":" + port + ": " + ex);
					}
				}
			}
		}
	}

	/**
	 * Repeatedly called from base-class until connection is broken.
	 */
	protected void serviceClient()
	{
		try
		{
			numBytesRead = 0;
			getOverallHeader();
			getIE1_Header();
			getMessageData();
			buildMessage();
		}
		catch(SBDFormatException ex)
		{
			Logger.instance().warning(
				IridiumRecv.module + ":" + IridiumRecv.EVT_RECV_FAILED
				+ " Format Error on connection to " + getClientName() + ": " + ex);
			ignoreRest();
		}
		catch(Exception ex)
		{
			Logger.instance().warning(
				IridiumRecv.module + ":" + IridiumRecv.EVT_RECV_FAILED
				+ " Error on connection to " + getClientName() + ": " + ex);
		}
		try { disconnect(); }
		catch(Exception ex) {}
	}
	
	private int readByte()
		throws IOException
	{
		int c = input.read();
		if (c == -1)
		{
			if (!ignoring)
				Logger.instance().warning(
						IridiumRecv.module + " IRIDIUM client hung up.");
			iridiumRecv.setStatus("Disconnected");
			throw new EOFException();
		}
		numBytesRead++;
		c = c & 0xff;
		if (captureStream != null)
			captureStream.write(c);
		return c;
	}
	
	private void getOverallHeader()
		throws IOException
	{
		protoVersion = readByte();
		msgLength = (readByte() << 8) + readByte();
		Logger.instance().debug1(IridiumRecv.module 
			+ " overall header: protoVersion=" + protoVersion
			+ ", msgLength = " + msgLength);
	}

	private void getIE1_Header()
		throws IOException, SBDFormatException
	{
		int ienum = readByte();
		if (ienum != 1)
			throw new SBDFormatException("First IE must have number 1, got "
				+ ienum);
		int ielen = (readByte() << 8) + readByte();
		if (ielen != 28)
			throw new SBDFormatException("First IE must have length 28, got "
				+ ielen);
		cdrRef = (readByte() << 24) + (readByte()<<16) + (readByte()<<8)
			+ readByte();
		StringBuilder sb = new StringBuilder();
		for(int i=0; i<15; i++)
			sb.append((char)readByte());
		emei = sb.toString();
		sessionStatus = readByte();
		momsm = (readByte()<<8) + readByte();
		mtmsm = (readByte()<<8) + readByte();
		sessionTimeT = (readByte() << 24) + (readByte()<<16) + (readByte()<<8)
			+ readByte();
		Logger.instance().debug3(IridiumRecv.module +
			" Read IE1 Header, cdrRef=" + cdrRef
			+ ", emei='" + emei + "', mtmsm=" + mtmsm + ", momsm=" + momsm
			+ ", sessionTime=" + new Date(sessionTimeT*1000L));
		ie3seen = false;
	}
	
	private void getMessageData()
		throws IOException, SBDFormatException
	{
		msgTime = new Date(sessionTimeT * 1000L);
		
		ByteArrayOutputStream payloadBaos = new ByteArrayOutputStream();
		while(numBytesRead < msgLength + 3)
			getInfoElement(payloadBaos);
		byte payload[] = payloadBaos.toByteArray();

		String hdr = "ID=" + emei 
		+ ",TIME=" + dateFmt.format(msgTime)
		+ ",STAT=" + (sessionStatus < 10 ? "0" : "") + sessionStatus
		+ ",MO=" + msmFmt.format(momsm)
		+ ",MT=" + msmFmt.format(mtmsm)
		+ ",CDR=" + DcpAddress.toGoesDcpAddr(cdrRef);
		if (ie3seen)
			hdr = hdr + ",LAT=" + locFmt.format(lat)
				+ ",LON=" + locFmt.format(lon)
				+ ",RAD=" + locFmt.format(cepRadius);
		hdr = hdr + " ";
		
		ByteArrayOutputStream msg_baos = new ByteArrayOutputStream();
		msg_baos.write(hdr.getBytes());
		msg_baos.write(payload);

		messageData = msg_baos.toByteArray();
		if (numBytesRead != msgLength + 3)
			Logger.instance().warning(IridiumRecv.module
				+ " Iridium format issue: msgLength=" + msgLength
				+ ", numBytesRead (total)=" + numBytesRead
				+ ", should be " + (msgLength+3));
	}

	private void getInfoElement(ByteArrayOutputStream baos)
		throws IOException, SBDFormatException
	{
		byte iehdr[] = new byte[3];
		for(int i=0; i<3; i++)
			iehdr[i] = (byte)readByte();
		int hb = (int)iehdr[1] & 0xff;   // don't want sign-extension
		int lb = (int)iehdr[2] & 0xff;   // don't want sign-extension
		int ielen = (hb << 8) + lb;
		
		if (iehdr[0] == (byte)3 && ielen == 11)
		{
			Logger.instance().debug3(IridiumRecv.module
				+ " Processing IE3 (lat/lon)");
			int b1 = readByte();
			int signLat = (b1 & 2) == 0 ? 1 : -1; // 0 means north +lat
			int signLon = (b1 & 1) == 0 ? 1 : -1; // 0 means east  +lon
			lat = (double)readByte();      // whole number degrees
			lat += ((readByte()<<8) + readByte()) / 60000.;   // thousandths of minutes
			lat = lat * signLat;
			lon = (double)readByte();      // whole number deg
			lon += ((readByte()<<8) + readByte()) / 60000.;   // thousandths of minutes
			lon = lon * signLon;
			cepRadius = (double)((readByte()<<24) + (readByte()<<16)
					+ (readByte()<<8) + readByte());
			ie3seen = true;
		}
		else
		{
			Logger.instance().debug3(IridiumRecv.module
				+ " Reading Payload IE[" + (int)iehdr[0] + "] len=" + ielen);
			if (numBytesRead-3 + ielen > msgLength)
				throw new SBDFormatException(
					"IE[" + (int)iehdr[0] 
					+ "] length too long. Starts at pos=" + numBytesRead
					+ " with length=" + ielen + " but total msg len="
					+ msgLength);
			baos.write("IE:".getBytes());
			baos.write(ByteUtil.toHexString(iehdr).getBytes());
			baos.write((byte)' ');
			
			for(int i=0; i<ielen; i++)
				baos.write(readByte());
		}
	}
	
	private void buildMessage()
	{
		DcpMsg msg = new DcpMsg();
		
		msg.flagbits =
			DcpMsgFlag.MSG_PRESENT
			| DcpMsgFlag.SRC_IRIDIUM
			| DcpMsgFlag.MSG_TYPE_IRIDIUM;
		msg.setCdrReference(cdrRef);
		msg.setMtmsm(mtmsm);
		msg.setSequenceNum(momsm);
		msg.setXmitTime(msgTime);
		msg.setDcpAddress(new DcpAddress(emei));
		msg.setSessionStatus(sessionStatus);
		msg.setData(messageData);
		msg.setDataSourceId(iridiumRecv.getDataSourceId());

		iridiumRecv.archive(msg);
		Logger.instance().debug2(IridiumRecv.module + " Archived message");
	}

	/**
	 * Called when a formatting error occurs. This method reads the rest 
	 * of the bytes on the socket so that they may be captured in the 
	 * capture-file (if one is being used).
	 */
	private void ignoreRest()
	{
		Logger.instance().warning(IridiumRecv.module 
			+ " Ignoring rest of message.");
		ignoring = true;
		int nbytes = 0;
		// readByte will throw IOException when done.
		try 
		{
			while(readByte() != -1)
				nbytes++;
		}
		catch(IOException ex) {}
		Logger.instance().warning(IridiumRecv.module
			+ " " + nbytes + " ignored after format error.");
	}
	
	
}
