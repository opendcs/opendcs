package decodes.syncgui;

import java.util.Vector;
import java.io.*;
import java.util.*;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import org.xml.sax.*;

import ilex.xml.*;
import ilex.util.*;
import decodes.xml.XmlDbTags;

/**
 * Represents a list of platforms in a particular archived district database.
 */
public class PlatList
{
	/** The owning database */
	DistrictDBSnap myDB;

	/** Vector of PlatListEntry objects */
	private Vector entries;

	/** The Main XML parser object. Note: Must be static so that XML
	    reads are synchronized between all db platlist objects. */
	static TopLevelXio topLevelXio = null;

	/**
	 * Constructor.
	 * @param myDB DistrictDBSnap
	 */
	public PlatList( DistrictDBSnap myDB )
	{
		this.myDB = myDB;
		entries = new Vector();
	}

	/** Clears the vector. */
	public void clear()
	{
		entries.clear();
	}

	/**
	 * @return "Platforms"
	 */
	public String toString( ) { return "platform"; }

	/**
	 * @return Vector of entries
	 */
	public Vector getEntries( ) { return entries; }


	/**
	 * Reads the PlatformList.xml file from a stream.
	 */
	public void read( InputStream strm )
		throws IOException
	{
		entries.clear();

		try
		{
			if (topLevelXio == null)
				topLevelXio = new TopLevelXio();
			topLevelXio.parse(this, strm, myDB.toString());
//System.out.println("Finished PlatList read: " + entries.size() + " entries.");
		}
		catch(ParserConfigurationException ex)
		{
			Logger.instance().failure("Cannot initialize XML parser: "
				+ ex);
		}
		catch(SAXException ex)
		{
			Logger.instance().failure("Cannot initialize XML parser: "
				+ ex);
		}
	}

	/** Dumps the platform list to stdout for testing. */
	public void dump()
	{
		System.out.println("\t\tPlatforms:");
		for(Iterator it = entries.iterator(); it.hasNext(); )
		{
			PlatListEntry ple = (PlatListEntry)it.next();
			System.out.println("\t\t\t" + ple);
			for(Iterator tmit = ple.getTMIterator(); tmit.hasNext(); )
				System.out.println("\t\t\tTM: " + tmit.next());
		}
	}

	/**
	 * Adds a PlatListEntry to this collection.
	 * @param ple the entry to add.
	*/
	public void add(PlatListEntry ple)
	{
		entries.add(ple);
	}
}


/**
Top-level SAX parser for reading the PlatformXref.xml file.
*/
class TopLevelXio
	implements XmlObjectParser
{
	/** SAX parser object */
	private XMLReader parser;

	/** Manages hierarchy of parsers */
	private XmlHierarchyParser xhp;

	/** Save original input file name for use in log messages. */
	String inputName;

	/** The object currently being parsed. */
	PlatList platList;

	/**
	  Constructor.
	*/
	public TopLevelXio()
	    throws ParserConfigurationException, SAXException
	{
		super();

        SAXParserFactory spf = SAXParserFactory.newInstance();
		SAXParser sp = spf.newSAXParser();
		parser = sp.getXMLReader();
		parser.setFeature("http://xml.org/sax/features/namespaces", true);

		ErrorHandler eh = new PrintStreamErrorHandler(System.out);
		xhp = new XmlHierarchyParser(eh);
		setErrorHandler(eh);
		parser.setContentHandler(xhp);
	}

	/** Sets the SAX error handler. */
	public void setErrorHandler(ErrorHandler eh)
	{
		parser.setErrorHandler(eh);
		xhp.setErrorHandler(eh);
	}

	/**
	  Parses the passed buffer into an LRGS Snapshot
	  @param data a byte array containing the complete XML data.
	  @param offset offset into data where XML starts
	  @param len length of XML data
	  @param inputName for log messages
	  @return LrgsStatusSnapshotExt parsed from data
	*/
	public synchronized void parse(PlatList platList, 
		InputStream is, String inputName)
		throws IOException, SAXException
	{
		this.platList = platList;
		this.inputName = inputName;
		xhp.setFileName(inputName);
		xhp.pushObjectParser(this);
		parser.parse(new InputSource(is));
		inputName = "";
	}

	/** No content characters expected -- only sub-elements. */
	public void characters(char[] ch, int start, int length)
		throws SAXException
	{
		if (!TextUtil.isAllWhitespace(new String(ch, start, length)))
			throw new SAXException(
				"No character data expected in TopLevelElement");
	}

	/**
	  Called when sub-element seen.
	  The only element expected at the top level is "LrgsStatusSnapshot".
	  @param hier the parser stack
	  @param namespaceURI ignored
	  @param localName name of the new element
	  @param qname ignored
	  @param atts attributes from the new element
	*/
	public void startElement(XmlHierarchyParser hier,
		String namespaceURI, String localName, String qname, Attributes atts)
		throws SAXException
	{
		if (localName.equalsIgnoreCase(XmlDbTags.PlatformList_el))
		{
			hier.pushObjectParser(new PlatformListParser(platList));
		}
		else
		{
			String msg = 
				"Unexpected tag '" + localName + "' at top level of file.";
			Logger.instance().warning(msg);
			throw new SAXException(msg);
		}
	}

	/**
	  Signals the end of the current element.
	  @param hier the parser stack
	  @param namespaceURI ignored
	  @param localName name of the element
	  @param qname ignored
	*/
	public void endElement(XmlHierarchyParser hier,
		String namespaceURI, String localName, String qname)
		throws SAXException
	{
		hier.popObjectParser();
	}

	/** Does nothing. */
    public void ignorableWhitespace (char ch[], int start, int length)
		throws SAXException
	{
	}
}


/**
 * Parses the platform list cross reference file. Populates PlatformList
 * with partial PlatformObjects.
 */
class PlatformListParser 
	implements XmlObjectParser, TaggedStringOwner
{
  	/** This is the object being parsed or written */
	private PlatList platList;

	/** partial Platform object being populated */
	PlatListEntry ple;

	private static final int siteNameTag = 1;
	private static final int expirationTag = 2;
	private static final int agencyTag = 3;
	private static final int descriptionTag = 4;
	private static final int configNameTag = 5;

	/**
	 * Constructor.
	 * @param pl the object in which to store the data.
	 */
	public PlatformListParser( PlatList pl )
	{
		super();
		ple = null;
		platList = pl;
	}

	/**
	 * @return name of element parsed by this parser
	 */
	public String myName( ) { return XmlDbTags.PlatformList_el; }

	/**
	 * @param ch Characters from file
	 * @param start start of characters
	 * @param length length of characters
	 */
	public void characters( char[] ch, int start, int length ) 
		throws SAXException
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
		String localName, String qname, Attributes atts ) throws SAXException
	{
		if (localName.equalsIgnoreCase(XmlDbTags.PlatformXref_el))
		{
			if (ple != null)
				throw new SAXException(XmlDbTags.PlatformXref_el
					+ " not allowed inside " + XmlDbTags.PlatformXref_el);

			String str = atts.getValue(XmlDbTags.PlatformId_at);
			if (str == null)
				throw new SAXException("PlatformXref without " +
					XmlDbTags.PlatformId_at + " attribute");
			int platformId;
			try { platformId = Integer.parseInt(str); }
			catch (NumberFormatException e)
			{
				throw new SAXException("PlatformXref " +
					XmlDbTags.PlatformId_at + " must be a number");
			}

			ple = new PlatListEntry(platformId);
			platList.add(ple);
		}
		else if (localName.equalsIgnoreCase(XmlDbTags.TransportXref_el))
		{
			if (ple == null)
				throw new SAXException(XmlDbTags.TransportXref_el
					+ " must be inside " + XmlDbTags.PlatformXref_el);

			String ts = atts.getValue(XmlDbTags.TransportMedium_mediumType_at);
			if (ts  == null)
				throw new SAXException(XmlDbTags.TransportMedium_mediumType_at
					+ " is a required attribute for "
					+ XmlDbTags.TransportXref_el);

			String id = atts.getValue(XmlDbTags.TransportMedium_mediumId_at);
			if (id  == null)
				throw new SAXException(XmlDbTags.TransportMedium_mediumId_at
					+ " is a required attribute for "
					+ XmlDbTags.TransportXref_el);

			String tm = ts + ":" + id;
			ple.addMedium(tm);
		}
		else if (localName.equalsIgnoreCase(XmlDbTags.SiteName_el))
		{
			ple.siteNameType = atts.getValue(XmlDbTags.SiteName_nameType_at);
			if (ple.siteNameType == null)
				throw new SAXException(XmlDbTags.SiteName_el + " without "
					+ XmlDbTags.SiteName_nameType_at +" attribute");
			hier.pushObjectParser(new TaggedStringSetter(this, siteNameTag));
		}
		else if (localName.equalsIgnoreCase(XmlDbTags.expiration_el))
		{
			hier.pushObjectParser(new TaggedStringSetter(this, expirationTag));
		}
		else if (localName.equalsIgnoreCase(XmlDbTags.agency_el))
		{
			hier.pushObjectParser(new TaggedStringSetter(this, agencyTag));
		}
		else if (localName.equalsIgnoreCase(XmlDbTags.description_el))
		{
			hier.pushObjectParser(new TaggedStringSetter(this, descriptionTag));
		}
		else if (localName.equalsIgnoreCase(
			XmlDbTags.PlatformConfig_configName_at))
		{
			hier.pushObjectParser(new TaggedStringSetter(this, configNameTag));
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
	public void endElement( XmlHierarchyParser hier, String namespaceURI, 
		String localName, String qname ) throws SAXException
	{
		if (localName.equalsIgnoreCase(XmlDbTags.PlatformXref_el))
		{
			ple = null;
		}
		else if (localName.equalsIgnoreCase(XmlDbTags.TransportXref_el))
			; // End of empty TransportXref element
		else if (!localName.equalsIgnoreCase(myName()))
			throw new SAXException(
				"Parse stack corrupted: got end tag for " + localName
				+ ", expected " + myName());
		else
			hier.popObjectParser();
	}

	/**
	 * Allows an object to keep track of whitespace, if needed.
	 * @param ch the whitespace
	 * @param start the start of the whitespace
	 * @param length the length of the whitespace
	 */
	public void ignorableWhitespace( char[] ch, int start, int length ) 
		throws SAXException
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
		case siteNameTag:
			ple.siteNameValue = str;
			break;
		case expirationTag:
			ple.expiration = str;
			break;
		case agencyTag:
			ple.agency = str;
			break;
		case descriptionTag:
			ple.desc = TextUtil.collapseWhitespace(str);
			break;
		case configNameTag:
			ple.configName = str;
			break;
		}
	}
}
