/*
*  $Id$
*  
*  Open source software
*  
*  $Log$
*  Revision 1.5  2013/03/21 18:27:40  mmaloney
*  DbKey Implementation
*
*/
package decodes.xml;

import opendcs.dai.LoadingAppDAI;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import decodes.db.*;
import decodes.sql.DbKey;
import decodes.tsdb.CompAppInfo;
import decodes.tsdb.xml.CompXio;
import decodes.tsdb.xml.CompXioTags;
import ilex.util.TextUtil;
import java.io.IOException;
import ilex.xml.*;

/**
 * This class maps the DECODES XML representation for complete Databases.
 */
public class DatabaseParser implements XmlObjectParser, XmlObjectWriter
{
	/** The database we parse records into and out of. */
	private Database theDb;
	private Date fileLMT = new Date();

	/**
	 * Constructor.
	 * @param db The database to parse records into and out of.
	 */
	public DatabaseParser(Database db)
	{
		super();
		theDb = db;
	}

	/**
	 * Sets the database to parse records into and out of.
	 * @param db The database to parse records into and out of.
	 */
	public void setDb(Database db)
	{
		theDb = db;
	}

	/**
	 * @return name of element parsed by this parser
	 */
	public String myName( ) { return XmlDbTags.Database_el; }

	public void setFileLMT(Date lmt) { fileLMT = lmt; }
	
	/**
	 * @param ch Characters from file
	 * @param start start of characters
	 * @param length length of characters
	 */
	public void characters( char[] ch, int start, int length ) throws SAXException
	{
		if (!TextUtil.isAllWhitespace(new String(ch, start, length)))
			throw new SAXException(
				"No character data expected within " + myName());
	}

	/**
	 * Called after start of new element for this parser is detected.
	 * @param hier the stack of parsers
	 * @param namespaceURI namespaceURI
	 * @param localName name of element
	 * @param qname ignored
	 * @param atts attributes for this element
	 * @throws SAXException on parse error
	 */
	public void startElement( XmlHierarchyParser hier, String namespaceURI, 
		String localName, String qname, Attributes atts ) 
			throws SAXException
	{
		if (localName.equalsIgnoreCase(XmlDbTags.Platform_el))
		{
			String id = atts.getValue(XmlDbTags.PlatformId_at);
			DbKey pid = Constants.undefinedId;
			if (id != null)
			{
				try { pid = DbKey.createDbKey(Long.parseLong(id)); }
				catch(NumberFormatException e)
				{ pid = Constants.undefinedId;}
			}

            Platform p = null;
            p = new Platform(pid);
            p.lastModifyTime = fileLMT;

			theDb.platformList.add(p);
			hier.pushObjectParser(new PlatformParser(p));
		}
		else if (localName.equalsIgnoreCase(XmlDbTags.RoutingSpec_el))
		{
			String nm = atts.getValue(XmlDbTags.name_at);
			if (nm == null)
				throw new SAXException(XmlDbTags.RoutingSpec_el + " without "
					+ XmlDbTags.name_at +" attribute");
			RoutingSpec spec = new RoutingSpec(nm);
			theDb.routingSpecList.add(spec);
			hier.pushObjectParser(new RoutingSpecParser(spec));
		}
		else if (localName.equalsIgnoreCase(XmlDbTags.NetworkList_el))
		{
			String nm = atts.getValue(XmlDbTags.name_at);
			if (nm == null)
				throw new SAXException(XmlDbTags.NetworkList_el + " without "
					+ XmlDbTags.name_at +" attribute");
			NetworkList spec = new NetworkList(nm);
			theDb.networkListList.add(spec);
			hier.pushObjectParser(new NetworkListParser(spec));
		}
		else if (localName.equalsIgnoreCase(XmlDbTags.PresentationGroup_el))
		{
			String nm = atts.getValue(XmlDbTags.name_at);
			if (nm == null)
				throw new SAXException(XmlDbTags.PresentationGroup_el
					+ " without " + XmlDbTags.name_at +" attribute");
			PresentationGroup spec = new PresentationGroup(nm);
			theDb.presentationGroupList.add(spec);
			hier.pushObjectParser(new PresentationGroupParser(spec));
		}
		else if (localName.equalsIgnoreCase(XmlDbTags.EngineeringUnitList_el))
		{
			hier.pushObjectParser(new EngineeringUnitListParser());
		}
		else if (localName.equalsIgnoreCase(
			XmlDbTags.DataTypeEquivalenceList_el))
		{
			hier.pushObjectParser(new DataTypeEquivalenceListParser());
		}
		else if (localName.equalsIgnoreCase(XmlDbTags.EnumList_el))
		{
			hier.pushObjectParser(new EnumListParser(theDb));
		}
		else if (localName.equalsIgnoreCase(XmlDbTags.DataSource_el))
		{
			String nm = atts.getValue(XmlDbTags.name_at);
			String tp = atts.getValue(XmlDbTags.type_at);
			if (nm == null || tp == null)
				throw new SAXException(XmlDbTags.DataSource_el + " without "
					+ XmlDbTags.name_at +" or " + XmlDbTags.type_at
					+ " attribute");
			DataSourceList dsl = theDb.dataSourceList;
			DataSource ds = dsl.get(nm);
			if (ds == null)
			{
				ds = new DataSource(nm, tp);
				dsl.add(ds);
			}
			hier.pushObjectParser(new DataSourceParser(ds));
		}
		else if (localName.equalsIgnoreCase(XmlDbTags.IntervalList_el))
		{
			IntervalList ilist = IntervalList.editInstance();
			hier.pushObjectParser(new IntervalListParser(ilist));
		}

		else if (localName.equalsIgnoreCase(CompXioTags.loadingApplication))
		{
			CompAppInfo cai = new CompAppInfo();
			cai.setAppName(XmlUtils.getAttrIgnoreCase(atts, CompXioTags.name));
			CompXio compXio = new CompXio("DecodesDbParser", null);
			compXio.setWorkingObject(cai);
			theDb.loadingAppList.add(cai);
			hier.pushObjectParser(compXio);
		}
		else if (localName.equalsIgnoreCase(XmlDbTags.ScheduleEntry_el))
		{
			ScheduleEntry se = 
				new ScheduleEntry(
					XmlUtils.getAttrIgnoreCase(atts, XmlDbTags.name_at));
			theDb.schedEntryList.add(se);
			hier.pushObjectParser(new ScheduleEntryParser(se, false));
		}
		else if (localName.equalsIgnoreCase(XmlDbTags.PlatformStatus_el))
		{
			try
			{
				long platformId = Long.parseLong(XmlUtils.getAttrIgnoreCase(atts, XmlDbTags.id_at));
				PlatformStatus ps = new PlatformStatus(DbKey.createDbKey(platformId));
				theDb.platformStatusList.add(ps);
				hier.pushObjectParser(new PlatformStatusParser(ps));
			}
			catch(NumberFormatException ex)
			{
				throw new SAXException("Invalid non-numeric platform id in " + localName);
			}
		}
		
//		else if (localName.equalsIgnoreCase(XmlDbTags.PMConfigList_el))
//		{
//			hier.pushObjectParser(new PMConfigListParser());
//		}
//		else if (localName.equalsIgnoreCase(XmlDbTags.PMConfig_el))
//		{
//			String type = atts.getValue(
//				XmlDbTags.TransportMedium_mediumType_at);
//			if (type == null)
//				throw new SAXException("PMConfig without " +
//					XmlDbTags.TransportMedium_mediumType_at + " attribute");
//			PMConfig cfg = new PMConfig(type);
//			hier.pushObjectParser(new PMConfigParser(cfg));
//		}
		else
			throw new SAXException("Invalid element '" + localName
				+ "' under " + myName());
	}

	/**
	 * Signals the end of the current element.
	 * Causes parser to pop the stack in the hierarchy. 
	 * @param hier the stack of parsers
	 * @param namespaceURI ignored
	 * @param localName element that is ending
	 * @param qname ignored
	 */
	public void endElement( XmlHierarchyParser hier, String namespaceURI, String localName, String qname ) throws SAXException
	{
		if (!localName.equalsIgnoreCase(myName()))
			throw new SAXException(
				"Parse stack corrupted: got end tag for " + localName
				+ ", expected " + myName());
		hier.popObjectParser();
	}

	/**
	 * Allows an object to keep track of whitespace, if needed.
	 * @param ch the whitespace
	 * @param start the start of the whitespace
	 * @param length the length of the whitespace
	 */
	public void ignorableWhitespace( char[] ch, int start, int length ) throws SAXException
	{
	}

	/**
	 * Writes this object's data, along with subordinates, to an XML file.
	 * @param xos the output stream object
	 * @throws IOException on IO error
	 */
	public void writeXml( XmlOutputStream xos ) throws IOException
	{
		xos.startElement(myName());

		if (theDb.platformList.size() > 0)
		{
			Iterator<Platform> it = theDb.platformList.iterator();
			while(it.hasNext())
			{
				Platform ob = it.next();
				PlatformParser p = new PlatformParser(ob);
				p.writeXml(xos);
			}
		}

		if (theDb.routingSpecList.size() > 0)
		{
			for(Iterator<RoutingSpec> it = theDb.routingSpecList.iterator();
				it.hasNext(); )
			{
				RoutingSpec ob = it.next();
				RoutingSpecParser p = new RoutingSpecParser(ob);
				p.writeXml(xos);
			}
		}
		
		if (theDb.dataSourceList.size() > 0)
		{
			for(Iterator<DataSource> it = theDb.dataSourceList.iterator();
				it.hasNext(); )
			{
				DataSource ds = it.next();
				DataSourceParser dsp = new DataSourceParser(ds);
				dsp.writeXml(xos);
			}
		}

		if (theDb.networkListList.size() > 0)
		{
			for(Iterator<NetworkList> it = theDb.networkListList.iterator();
				it.hasNext(); )
			{
				NetworkList ob = it.next();
				NetworkListParser p = new NetworkListParser(ob);
				p.writeXml(xos);
			}
		}

		if (theDb.presentationGroupList.size() > 0)
		{
			for(Iterator<PresentationGroup> it = theDb.presentationGroupList.iterator();
				it.hasNext(); )
			{
				PresentationGroup ob = it.next();
				PresentationGroupParser p = new PresentationGroupParser(ob);
				p.writeXml(xos);
			}
		}

		if (theDb.engineeringUnitList.size() > 0)
		{
			EngineeringUnitListParser p = new EngineeringUnitListParser();
			p.writeXml(xos);
		}

		DataTypeEquivalenceListParser dp = new DataTypeEquivalenceListParser();
		dp.writeXml(xos);

		if (theDb.enumList.size() > 0)
		{
			EnumListParser p = new EnumListParser(theDb);
			p.writeXml(xos);
		}
		
		if (IntervalList.instance().getList().size() > 0)
		{
			IntervalListParser p = new IntervalListParser(IntervalList.instance());
			p.writeXml(xos);
		}
		
		if (theDb.loadingAppList.size() > 0)
		{
			CompXio compXio = new CompXio("DecodesDbParser", null);
			for(CompAppInfo cai : theDb.loadingAppList)
				compXio.writeApp(xos, cai);
		}

		for(ScheduleEntry se : theDb.schedEntryList)
		{
			// This is used by export, so don't set LMT when writing 
			ScheduleEntryParser seParser = new ScheduleEntryParser(se, false);
			seParser.writeXml(xos);
		}

//		for(PlatformStatus ps : theDb.platformStatusList)
//		{
//			// This is used by export, so don't set LMT when writing 
//			PlatformStatusParser psParser = new PlatformStatusParser(os);
//			psParser.writeXml(xos);
//		}

		
//		if (theDb.pMConfigList.size() > 0)
//		{
//			PMConfigListParser p = new PMConfigListParser();
//			p.writeXml(xos);
//		}

		xos.endElement(myName());
	}
}
