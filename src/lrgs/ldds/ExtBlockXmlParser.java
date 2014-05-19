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
*/
package lrgs.ldds;

import java.util.ArrayList;
import java.util.Date;
import java.util.TimeZone;
import java.text.SimpleDateFormat;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.DocumentBuilder;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import ilex.util.Base64;
import ilex.util.Logger;
import ilex.util.TextUtil;
import ilex.xml.DomHelper;
import ilex.xml.XmlOutputStream;

import lrgs.common.DcpAddress;
import lrgs.common.DcpMsg;
import lrgs.common.DcpMsgFlag;
import lrgs.common.DcpMsgIndex;

/**
Parses the XML returned in an Extended Block Request into an array of
DcpMsg objects.
*/
public class ExtBlockXmlParser
{
	static final String module = "LddsClient";
	private DocumentBuilderFactory factory = null;
	private DocumentBuilder builder = null;
	private SimpleDateFormat sdf;
	private int assumedSrc = 0;
	private int ddsVersion = DdsVersion.DdsVersionNum;
	
	public static final String MsgBlockElem = "MsgBlock";
	public static final String DcpMsgElem = "DcpMsg";
	public static final String AsciiMsgElem = "AsciiMsg";
	public static final String BinaryMsgElem = "BinaryMsg";
	public static final String CarrierStartElem = "CarrierStart";
	public static final String CarrierStopElem = "CarrierStop";
	public static final String DomsatTimeElem = "DomsatTime";
	public static final String DomsatSeqElem = "DomsatSeq";
	public static final String BaudElem = "Baud";
	public static final String flagsAttr = "flags";
	public static final String platformIdAttr = "platformId";
	public static final String xmitTimeElem = "XmitTime";
	public static final String localRecvTimeElem = "LocalRecvTime";
	public static final String momsmElem = "MOMSM";
	public static final String mtmsmElem = "MTMSM";
	public static final String cdrRefElem = "CDR_Reference";
	public static final String sessionStatusElem = "SessionStatus";
	
	private boolean writeLocalTime = true;
	
	/** Default constructor. */
	public ExtBlockXmlParser(int assumedSrc)
	{
		sdf = new SimpleDateFormat("yyyy/DDD HH:mm:ss.SSS");
		sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
		this.assumedSrc = assumedSrc;
	}
	
	private void initFactory()
		throws ProtocolError
	{
		try
		{
			factory = DocumentBuilderFactory.newInstance();
			builder = factory.newDocumentBuilder();
		}
		catch(Exception ex)
		{
			throw new ProtocolError(
				"Could not initialize ExtBlockXmlParser: " + ex);
		}
	}

	/**
	  Parses a block of XML data into an array of DcpMsg objects.
	  configuration. Sets internal variables according to that configuration.
	  @param data the data to parse
	  @throws ProtocolError if parsing fails.
	*/
	public DcpMsg[] parseMsgBlock(byte[] data)
		throws ProtocolError, IOException 
	{
		if (factory == null)
			initFactory();

		ArrayList<DcpMsg> msgList = new ArrayList<DcpMsg>();
		try
		{
			ByteArrayInputStream bais = new ByteArrayInputStream(data);
			Document doc = builder.parse(bais);
			Node topElem = doc.getDocumentElement();
			if (!topElem.getNodeName().equalsIgnoreCase(MsgBlockElem))
			{
				String s = module
					+ ": Wrong top-level element '" + topElem.getNodeName()
					+ "' expected '" + MsgBlockElem + "'";
				throw new ProtocolError(s);
			}
			NodeList msgElemList = topElem.getChildNodes();
			for(int i=0; msgElemList != null && i<msgElemList.getLength(); i++)
			{
				Node node = msgElemList.item(i);
				if (node.getNodeType() == Node.ELEMENT_NODE
				 && node.getNodeName().equalsIgnoreCase(DcpMsgElem))
				{
					DcpMsg msg = convert2Msg((Element)node);
					if (msg != null)
						msgList.add(msg);
				}
			}
		}
		catch(SAXException ex)
		{
			throw new ProtocolError("Could not parse XML Block Response: "
				+ ex);
		}
		DcpMsg ret[] = new DcpMsg[msgList.size()];
		int i = 0;
		for(DcpMsg msg : msgList)
			ret[i++] = msg;
		return ret;
	}
	/**
	  Parses a block of XML data into a single DcpMsg object.
	  configuration.
	  @param data the data to parse
	  @throws ProtocolError if parsing fails.
	*/
	public synchronized DcpMsg parseDcpMsg(byte[] data)
		throws ProtocolError, IOException 
	{
		if (factory == null)
			initFactory();

		try
		{
			ByteArrayInputStream bais = new ByteArrayInputStream(data);
			Document doc = builder.parse(bais);
			Element topElem = doc.getDocumentElement();
			if (!topElem.getNodeName().equalsIgnoreCase(DcpMsgElem))
			{
				String s = module
					+ ": Wrong top-level element '" + topElem.getNodeName()
					+ "' expected '" + DcpMsgElem + "'";
				throw new ProtocolError(s);
			}
			return convert2Msg(topElem);
		}
		catch(SAXException ex)
		{
			throw new ProtocolError("Could not parse XML Block Response: "
				+ ex);
		}
	}

	private DcpMsg convert2Msg(Element elem)
	{
		DcpMsg msg= new DcpMsg();
		String ns = DomHelper.findAttr(elem, flagsAttr);
		if (ns.startsWith("0x")) ns = ns.substring(2);
		msg.flagbits = DcpMsgFlag.MSG_PRESENT;
		int seqnum = -1;
		Date domsatTime = null;

		if (ns != null)
		{
			try { msg.setFlagbits(Integer.parseInt(ns.trim(), 16)); }
			catch(NumberFormatException ex)
			{
				Logger.instance().warning(module + 
				" Can't parse flags from '" + ns + "': " + ex);
			}
		}
		if (assumedSrc != 0)
			msg.flagbits = (msg.flagbits & (~DcpMsgFlag.SRC_MASK)) | assumedSrc;
		
		ns = DomHelper.findAttr(elem, platformIdAttr);
		if (ns != null)
			msg.setDcpAddress(new DcpAddress(ns));

		// Content elements will contain the optional settings.
		NodeList children = elem.getChildNodes();
		if (children == null)
		{
			Logger.instance().warning("DcpMsg element with no children!");
			return null;
		}
		for(int i=0; i<children.getLength(); i++)
		{
			Node child = children.item(i);
			if (child.getNodeType() != Node.ELEMENT_NODE)
				continue;

			String nn = child.getNodeName();
			if (nn.equalsIgnoreCase(BinaryMsgElem))
			{
				String b64data = 
					TextUtil.removeAllSpace(DomHelper.getTextContent(child));
				msg.setData(Base64.decodeBase64(b64data.getBytes()));
			}
			else if (nn.equalsIgnoreCase(CarrierStartElem))
			{
				String ds = DomHelper.getTextContent(child).trim();
				try
				{
					if (ds != null && ds.length() > 0)
					{
						Date d = sdf.parse(ds);
						msg.setCarrierStart(d);
//Logger.instance().info("Carrier start string = '" + ds + "' date=" + d);
						msg.flagbits |= DcpMsgFlag.HAS_CARRIER_TIMES;
					}
				}
				catch(Exception ex)
				{
					Logger.instance().warning(module + " Bad date format '"
						+ ds + "' in " + CarrierStartElem
						+ ": " + ex);
				}
			}
			else if (nn.equalsIgnoreCase(CarrierStopElem))
			{
				String ds = DomHelper.getTextContent(child).trim();
				try
				{
					if (ds != null && ds.length() > 0)
					{
						msg.setCarrierStop(sdf.parse(ds));
					}
				}
				catch(Exception ex)
				{
					Logger.instance().warning(module + " Bad date format '"
						+ ds + "' in " + CarrierStopElem
						+ ": " + ex);
				}
			}
			else if (nn.equalsIgnoreCase(DomsatTimeElem))
			{
				String ds = DomHelper.getTextContent(child).trim();
				try
				{
					if (ds != null && ds.length() > 0)
					{
						domsatTime = sdf.parse(ds);
						msg.setDomsatTime(domsatTime);
					}
				}
				catch(Exception ex)
				{
					Logger.instance().warning(module + " Bad date format '"
						+ ds + "' in " + DomsatTimeElem
						+ ": " + ex);
				}
			}
			else if (nn.equalsIgnoreCase(BaudElem))
				msg.setBaud(DomHelper.getIntegerContent(child, 0, module));
			else if (nn.equalsIgnoreCase(DomsatSeqElem))
			{
				seqnum = DomHelper.getIntegerContent(child, -1, module);
				msg.setSequenceNum(seqnum);
			}
			else if (nn.equalsIgnoreCase(AsciiMsgElem))
			{
				// MJM: Shouldn't use this because XML modifies white space!
				msg.setData(DomHelper.getTextContent(child).trim().getBytes());
			}
			else if (nn.equalsIgnoreCase(xmitTimeElem))
			{
				String ds = DomHelper.getTextContent(child).trim();
				try
				{
					if (ds != null && ds.length() > 0)
					{
						Date d = sdf.parse(ds);
						msg.setXmitTime(d);
						if (msg.getLocalReceiveTime() == null)
							msg.setLocalReceiveTime(d);
					}
				}
				catch(Exception ex)
				{
					Logger.instance().warning(module + " Bad date format '"
						+ ds + "' in " + xmitTimeElem + ": " + ex);
				}
			}
			else if (nn.equalsIgnoreCase(localRecvTimeElem))
			{
				String ds = DomHelper.getTextContent(child).trim();
				try
				{
					if (ds != null && ds.length() > 0)
					{
						msg.setLocalReceiveTime(sdf.parse(ds));
					}
				}
				catch(Exception ex)
				{
					Logger.instance().warning(module + " Bad date format '"
						+ ds + "' in " + localRecvTimeElem + ": " + ex);
				}
			}
			else if (nn.equalsIgnoreCase(momsmElem))
			{
				seqnum = DomHelper.getIntegerContent(child, -1, module);
				msg.setSequenceNum(seqnum);
			}
			else if (nn.equalsIgnoreCase(mtmsmElem))
			{
				int mtmsm = DomHelper.getIntegerContent(child, -1, module);
				msg.setMtmsm(mtmsm);
			}
			else if (nn.equalsIgnoreCase(cdrRefElem))
			{
				String s = DomHelper.getTextContent(child).trim();
				try { msg.setCdrReference(Long.parseLong(s)); }
				catch(NumberFormatException ex)
				{
					Logger.instance().warning(module + " Bad number format for "
						+ cdrRefElem + " '" + s + "'");
				}
			}
			else if (nn.equalsIgnoreCase(sessionStatusElem))
			{
				int ss = DomHelper.getIntegerContent(child, -1, module);
				msg.setSessionStatus(ss);
			}
			else
				Logger.instance().debug1(module + " Unexpected node '" 
					+ nn + "' in DcpMsg element with value '"
					+ DomHelper.getTextContent(child)+ "' -- ignored.");
		}
		if (seqnum < 0 || domsatTime == null)
			msg.flagbits |= DcpMsgFlag.MSG_NO_SEQNUM;

		byte msgdata[] = msg.getData();
//		if (msgdata == null || msgdata.length < DcpMsg.IDX_DATA)
		if (msgdata == null)
		{
			String errmsg = module 
			+ " Received empty message with seqnum=" + seqnum
			+ " in ext-xml block -- ignored.";
			try { throw new Exception(errmsg); }
			catch(Exception ex)
			{
				System.err.println(ex);
				ex.printStackTrace(System.err);
			}
			Logger.instance().warning(errmsg);
			return null;
		}

		return msg;
	}
	
	/**
	 * Adds a DCP Message to the XML Output Stream.
	 * @param xos XML Output Stream
	 * @param msg The DCP Message Object
	 * @param module Name of module for log messages
	 * @throws IOException
	 * @return true if message added, false if error encountered.
	 */
	public synchronized boolean addMsg(XmlOutputStream xos, DcpMsg msg, 
		String module)
		throws IOException
	{
		int len = -1;
		try 
		{
			if ((len = msg.getDcpDataLength()) <= 0)
				len = -1;
		}
		catch(Exception ex) { len = -1; }
		if (len == -1)
		{
			debug(module, "Bad length field in message '" 
				+ msg.getHeader() + "' -- skipped.");
			return false;
		}

		xos.startElement(DcpMsgElem, 
			flagsAttr, formatFlagsValue(msg.flagbits),
			platformIdAttr, msg.getDcpAddress().toString());


		if (DcpMsgFlag.isGOES(msg.flagbits))
		{
			// For GOES, always write Domsat seq & time, even if not 
			// supplied. This is for the V7 MessageFile, which might need
			// to add these after the fact, and will need a place-holder.
			xos.writeElement(DomsatSeqElem, formatDomsatSeq(msg.getSequenceNum()));

			Date t = msg.getDomsatTime();
			if (t == null)
				t = DcpMsgIndex.zeroDate;
			xos.writeElement(DomsatTimeElem, formatDate(t));

			t = msg.getCarrierStart();
			if (t != null && !t.equals(DcpMsgIndex.zeroDate))
				xos.writeElement(CarrierStartElem, formatDate(t));
			t = msg.getCarrierStop();
			if (t != null && !t.equals(DcpMsgIndex.zeroDate))
				xos.writeElement(CarrierStopElem, formatDate(t));
			if (msg.getBaud() != 0)
				xos.writeElement(BaudElem, "" + msg.getBaud());
		}
		else if (DcpMsgFlag.isIridium(msg.flagbits))
		{
			Date t = msg.getXmitTime();
			if (t != null && t != DcpMsgIndex.zeroDate)
				xos.writeElement(xmitTimeElem, formatDate(t));
			int sn = msg.getSequenceNum();
			if (sn > 0)
				xos.writeElement(momsmElem, "" + sn);
			sn = msg.getMtmsm();
			if (sn > 0)
				xos.writeElement(mtmsmElem, "" + sn);
			long cdrRef = msg.getCdrReference();
			if (cdrRef > 0)
				xos.writeElement(cdrRefElem, "" + cdrRef);
			xos.writeElement(sessionStatusElem, "" + msg.getSessionStatus());
		}
		else if (DcpMsgFlag.isNetDcp(msg.flagbits))
		{
			Date t = msg.getXmitTime();
			if (t != null && t != DcpMsgIndex.zeroDate)
				xos.writeElement(xmitTimeElem, formatDate(t));
		}
		
		if (ddsVersion >= DdsVersion.version_11)
		{
			Date t = msg.getLocalReceiveTime();
			if (t != null && t != DcpMsgIndex.zeroDate && writeLocalTime)
				xos.writeElement(localRecvTimeElem, formatDate(t));
		}
		
		// We always need to Base64 encode. Even ASCII message may have
		// non-printing chars like form-feed, CR/LF combinations, which must be
		// preserved EXACTLY.
		xos.startElement(BinaryMsgElem);
		xos.writeLiteral(new String(Base64.encodeBase64(msg.getData())));
		xos.endElement(BinaryMsgElem);

		xos.endElement(DcpMsgElem);
		return true;
	}
	
	/**
	 * Formats the flags as an 8-hex-digit string preceeded by "0x".
	 * @param flags the flag values as a 32-bit int.
	 * @return the formatted flags
	 */
	public static String formatFlagsValue(int flags)
	{
		StringBuilder sb = new StringBuilder("0x");
		sb.append(Integer.toHexString(flags));
		while(sb.length() < 10)
			sb.insert(2, "0");
		return sb.toString();
	}

	/**
	 * Formats the DOMSAT sequence number by blank-padding it to 5 chars.
	 * @param sn the sequence number
	 * @return the formatted 5-character string to save in the XML.
	 */
	public static String formatDomsatSeq(int sn)
	{
		StringBuilder sb = new StringBuilder("" + sn);
		while(sb.length() < 5)
			sb.insert(0, " ");
		return sb.toString();
	}

	public String formatDate(Date t)
	{
		if (t == null)
			t = DcpMsgIndex.zeroDate;
		synchronized(sdf)
		{
			return sdf.format(t);
		}
	}
	
	private void debug(String module, String msg)
	{
		Logger.instance().debug3(module + ": " + msg);
	}

	public boolean isWriteLocalTime()
	{
		return writeLocalTime;
	}

	public void setWriteLocalTime(boolean writeLocalTime)
	{
		this.writeLocalTime = writeLocalTime;
	}

	public void setDdsVersion(int ddsVersion)
	{
		this.ddsVersion = ddsVersion;
	}
}
