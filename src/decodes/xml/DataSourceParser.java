/*
*  $Id$
*/
package decodes.xml;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import java.util.Enumeration;
import decodes.db.*;
import ilex.util.TextUtil;
import ilex.util.IDateFormat;
import ilex.util.Logger;
import java.io.IOException;
import ilex.xml.*;

/**
 * This class maps the DECODES XML representation for DataSource elements.
 */
public class DataSourceParser implements XmlObjectParser, XmlObjectWriter, TaggedStringOwner
{
	private DataSource dataSource; // object that we will build.
	private int sequenceNum;

	private static final int argTag = 0;

	/**
	 * @param ob the object in which to store the data.
	 */
	public DataSourceParser( DataSource ob )
	{
		super();
		dataSource = ob;
		sequenceNum = -1;
	}

	/**
	 * @return name of element parsed by this parser
	 */
	public String myName( ) { return XmlDbTags.DataSource_el; }
		
	/**
	 * @param ch Characters from file
	 * @param start start of characters
	 * @param length length of characters
	 */
	public void characters( char[] ch, int start, int length ) throws SAXException
	{
		if (!TextUtil.isAllWhitespace(new String(ch, start, length)))
			throw new SAXException(
				"No character data expected within DataSource");
	}

	/**
	 * @param hier the stack of parsers
	 * @param namespaceURI namespaceURI
	 * @param localName name of element
	 * @param qname ignored
	 * @param atts attributes for this element
	 * @throws SAXException on parse error
	 */
	public void startElement( XmlHierarchyParser hier, String namespaceURI, String localName, String qname, Attributes atts ) throws SAXException
	{
		if (localName.equalsIgnoreCase(XmlDbTags.DataSourceArg_el))
		{
			hier.pushObjectParser(new TaggedStringSetter(this, argTag));
		}
		else if (localName.equalsIgnoreCase(XmlDbTags.DataSourceGroupMember_el))
		{
			// Get the sequence number argument & save for DataSource tag
			String seqs = atts.getValue(XmlDbTags.sequenceNum_at);
			if (seqs == null)
				throw new SAXException(XmlDbTags.DataSourceGroupMember_el + " without "
					+ XmlDbTags.sequenceNum_at +" attribute");
			try
			{
				sequenceNum = Integer.parseInt(seqs);
			}
			catch(NumberFormatException e)
			{
				throw new SAXException(
					"DataSourceGroupMember sequenceNum must be an integer ("+seqs+")");
			}
		}
		else if (localName.equalsIgnoreCase(XmlDbTags.DataSource_el))
		{
			if (sequenceNum == -1)
				throw new SAXException(XmlDbTags.DataSource_el + 
					" must be preceeded by " + XmlDbTags.DataSourceGroupMember_el);

			// Recursion: get attributes, make new DS, add to group & parse.
			String nm = atts.getValue(XmlDbTags.name_at);
			String tp = atts.getValue(XmlDbTags.type_at);
			if (nm == null || tp == null)
				throw new SAXException(XmlDbTags.DataSource_el + " without "
					+ XmlDbTags.name_at +" or " + XmlDbTags.type_at 
					+ " attribute");
			
			// In order to keep things normalized, if this new member
			// data source already exists in the db cache, use the cache version,
			// and parse into it.
			// That way, any other groups that already reference this member won't
			// be left dangling.
			DataSourceList dsl = dataSource.getDatabase().dataSourceList;
			DataSource ds = dsl.get(nm);
			if (ds == null)
			{
				ds = new DataSource(nm, tp);
				dsl.add(ds);
Logger.instance().debug3("Added member data source '" + ds.getName() + "' to cache list.");
			}
			else
			{
				ds.dataSourceType = tp;
Logger.instance().debug3("Referenced existing member data source '" + ds.getName() + "' from cache list.");
			}

			dataSource.addGroupMember(sequenceNum, ds);
			hier.pushObjectParser(new DataSourceParser(ds));
		}
		else
		{
			Logger.instance().log(Logger.E_WARNING,
				"Invalid element '" + localName + "' under " + myName()
				+ " -- skipped.");
			hier.pushObjectParser(new ElementIgnorer());
		}
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
		if (localName.equalsIgnoreCase(XmlDbTags.DataSourceGroupMember_el))
			sequenceNum = -1;	// End of Group Member element
		else if (localName.equalsIgnoreCase(myName()))
		{
			Database db = dataSource.getDatabase();
			if (db.dataSourceList.get(dataSource.getName()) == null)
				db.dataSourceList.add(dataSource);
			hier.popObjectParser();
		}
		else
			throw new SAXException(
				"Parse stack corrupted: got end tag for " + localName
				+ ", expected " + myName());
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
	 * From TaggedStringOwner, called from TaggedStringSetter when string
	 * elements are parsed.
	 * @param tag the tag defined above
	 * @param str the string content of the element
	 * @throws SAXException if context or parse error
	 */
	public void set( int tag, String str ) throws SAXException
	{
		switch(tag)
		{
		case argTag:
			dataSource.dataSourceArg = str;
			break;
		}
	}

	/**
	 * Writes this enumeration structure to an XML file.
	 * @param xos the output stream object
	 * @throws IOException on IO error
	 */
	public void writeXml( XmlOutputStream xos ) throws IOException
	{
		xos.startElement(myName(), 
			XmlDbTags.name_at, dataSource.getName(),
			XmlDbTags.type_at, dataSource.dataSourceType);

		if (dataSource.dataSourceArg != null)
			xos.writeElement(XmlDbTags.DataSourceArg_el, dataSource.dataSourceArg);

		int n = dataSource.groupMembers.size();
		for(int i=0; i<n; i++)
		{
			Object ob = dataSource.groupMembers.elementAt(i);
			if (ob == null)
				continue;
			DataSource ds = (DataSource)ob;
			xos.startElement(XmlDbTags.DataSourceGroupMember_el,
				XmlDbTags.sequenceNum_at, "" + i);
			DataSourceParser p = new DataSourceParser(ds);
			p.writeXml(xos);
			xos.endElement(XmlDbTags.DataSourceGroupMember_el);
		}

		xos.endElement(myName());
	}
}
