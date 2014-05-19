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
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.text.ParseException;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.DocumentBuilder;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import ilex.util.Logger;
import ilex.xml.DomHelper;
import ilex.xml.XmlOutputStream;

import lrgs.db.Outage;
import lrgs.common.DcpAddress;

/**
Parses the XML returned in an Extended Block Request into an array of
DcpMsg objects.
*/
public class OutageXmlParser
{
	public static final String outageListTag = "outageList";
	public static final String outageTag = "outage";
	public static final String outageIdTag = "outageId";
	public static final String beginTimeTag = "beginTime";
	public static final String endTimeTag = "endTime";
	public static final String outageTypeTag = "outageType";
	public static final String statusCodeTag = "statusCode";
	public static final String sourceIdTag = "sourceId";
	public static final String sourceNameTag = "sourceName";
	public static final String dcpAddressTag = "dcpAddress";
	public static final String beginSeqTag = "beginSeq";
	public static final String endSeqTag = "endSeq";

	private DocumentBuilderFactory factory = null;
	private DocumentBuilder builder = null;
	static final String module = "OutageXmlParser";

	private SimpleDateFormat sdf;

	/** Default constructor. */
	public OutageXmlParser()
	{
		sdf = new SimpleDateFormat("yyyy/DDD HH:mm:ss");
		sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
		try
		{
			factory = DocumentBuilderFactory.newInstance();
			builder = factory.newDocumentBuilder();
		}
		catch(Exception ex)
		{
			Logger.instance().failure(module +
				" Could not initialize OutageXmlParser: " + ex);
		}
	}

	/**
	  Parses a block of XML data into an array of Outage objects.
	  @param data the data to parse
	  @throws ProtocolError if parsing fails.
	*/
	public ArrayList<Outage> parse(byte[] data)
		throws ProtocolError, IOException 
	{
		ArrayList<Outage> outageList = new ArrayList<Outage>();
		try
		{
			ByteArrayInputStream bais = new ByteArrayInputStream(data);
			Document doc = builder.parse(bais);
			Node topElem = doc.getDocumentElement();
			if (!topElem.getNodeName().equalsIgnoreCase(outageListTag))
			{
				String s = module
					+ ": Wrong top-level element '" + topElem.getNodeName()
					+ "' expected '" + outageListTag + "'";
				throw new ProtocolError(s);
			}

			NodeList elemList = topElem.getChildNodes();
			for(int i=0; elemList != null && i<elemList.getLength(); i++)
			{
				Node node = elemList.item(i);
				if (node.getNodeType() == Node.ELEMENT_NODE
				 && node.getNodeName().equalsIgnoreCase(outageTag))
				{
					Outage otg = xml2Outage((Element)node);
					if (otg != null)
						outageList.add(otg);
				}
			}
		}
		catch(SAXException ex)
		{
			throw new ProtocolError("Could not parse XML Outages: "
				+ ex);
		}
		return outageList;
	}

	/**
	 * Passed an XML element pointing to an <Outage> element, returns the
	 * corresponding Outage object.
	 */
	public Outage xml2Outage(Element elem)
	{
		Outage otg = new Outage();

		String ns = DomHelper.findAttr(elem, outageIdTag);
		if (ns != null && ns.length() > 0)
		{
			try { otg.setOutageId(Integer.parseInt(ns.trim())); }
			catch(NumberFormatException ex)
			{
				Logger.instance().warning(module + " Bad outageId '"
					+ ns + "' -- expected integer.");
			}
		}

		ns = DomHelper.findAttr(elem, outageTypeTag);
		if (ns != null && ns.length() > 0)
			otg.setOutageType(ns.charAt(0));

		// Content elements will contain the optional settings.
		NodeList children = elem.getChildNodes();
		if (children == null)
		{
			Logger.instance().warning("Outage element with no children!");
			return null;
		}
		for(int i=0; i<children.getLength(); i++)
		{
			Node child = children.item(i);
			if (child.getNodeType() != Node.ELEMENT_NODE)
				continue;

			String nn = child.getNodeName();
			if (nn.equalsIgnoreCase(beginTimeTag))
			{
				Date d = DomHelper.getDateContent(child, module, sdf);
				if (d != null)
					otg.setBeginTime(d);
			}
			else if (nn.equalsIgnoreCase(endTimeTag))
			{
				Date d = DomHelper.getDateContent(child, module, sdf);
				if (d != null)
					otg.setEndTime(d);
			}
			else if (nn.equalsIgnoreCase(statusCodeTag))
			{
				String ds = DomHelper.getTextContent(child).trim();
				if (ds != null && ds.length() > 0)
					otg.setStatusCode(ds.charAt(0));
			}
			else if (nn.equalsIgnoreCase(sourceIdTag))
				otg.setSourceId(DomHelper.getIntegerContent(child, -1, module));
			else if (nn.equalsIgnoreCase(sourceNameTag))
				otg.setDataSourceName(DomHelper.getTextContent(child));
			else if (nn.equalsIgnoreCase(beginSeqTag))
				otg.setBeginSeq(DomHelper.getIntegerContent(child, -1, module));
			else if (nn.equalsIgnoreCase(endSeqTag))
				otg.setEndSeq(DomHelper.getIntegerContent(child, -1, module));
			else if (nn.equalsIgnoreCase(dcpAddressTag))
			{
				ns = DomHelper.getTextContent(child).trim();
				DcpAddress da = new DcpAddress(ns);
				otg.setDcpAddress((int)da.getAddr());
			}
			else
				Logger.instance().warning(module + " Unexpected node '" 
					+ nn + "' in DcpMsg element with value '"
					+ DomHelper.getTextContent(child)+ " -- ignored.");
		}
		return otg;
	}

	public byte[] outages2xml(ArrayList<Outage> outages)
	{
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		XmlOutputStream xos = new XmlOutputStream(baos, outageListTag);
		try
		{
			xos.startElement(outageListTag);
			for(Outage outage : outages)
				outage2xml(xos, outage);
			xos.endElement(outageListTag);
		}
		catch(IOException ex)
		{
			Logger.instance().warning(module + " Unexpected IOException: "
				+ ex);
		}
		return baos.toByteArray();
	}

	private void outage2xml(XmlOutputStream xos, Outage outage)
		throws IOException
	{
		xos.startElement(outageTag, 
			outageIdTag, "" + outage.getOutageId(),
			outageTypeTag, "" + outage.getOutageType());
		xos.writeElement(statusCodeTag, "" + outage.getStatusCode());

		Date d = outage.getBeginTime();
		xos.writeElement(beginTimeTag, sdf.format(d));

		d = outage.getEndTime();
		if (d != null)
			xos.writeElement(endTimeTag, sdf.format(d));

		int x = outage.getSourceId();
		if (x >= 0)
			xos.writeElement(sourceIdTag, "" + x);

		String s = outage.getDataSourceName();
		if (s != null)
			xos.writeElement(sourceNameTag, s);

		x = outage.getBeginSeq();
		if (x >= 0)
			xos.writeElement(beginSeqTag, "" + x);

		x = outage.getEndSeq();
		if (x >= 0)
			xos.writeElement(endSeqTag, "" + x);

		x = outage.getDcpAddress();
		if (x != 0)
		{
			DcpAddress da = new DcpAddress(x);
			xos.writeElement(dcpAddressTag, da.toString());
		}

		xos.endElement(outageTag);
	}
}
