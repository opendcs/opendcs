/*
*  $Id$
*  
*  Open Source Software
*  
*  $Log$
*  Revision 1.1.1.1  2014/05/19 15:28:59  mmaloney
*  OPENDCS 6.0 Initial Checkin
*
*  Revision 1.5  2013/03/21 18:27:39  mmaloney
*  DbKey Implementation
*
*/
package decodes.xml;

import java.net.URL;
import org.xml.sax.*;

import java.util.Date;
import decodes.db.*;
import decodes.sql.DbKey;
import decodes.tsdb.CompAppInfo;
import decodes.tsdb.xml.CompXio;
import decodes.tsdb.xml.CompXioTags;
import ilex.util.TextUtil;
import ilex.util.Logger;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.IOException;
import java.io.File;
import java.io.InputStream;
import java.io.FileInputStream;
import java.io.OutputStream;
import java.io.FileOutputStream;
import ilex.xml.*;

/**
 * This class maps the DECODES XML files into the object model.
 */
public class TopLevelParser implements XmlObjectParser
{
	private DatabaseObject topLevelObject;  // Object currently being parsed
	private XMLReader parser;               // SAX parser object
	private XmlHierarchyParser xhp;         // Manages hierarchy of parsers
	private static TopLevelParser _instance;
	private String inputName;
	private File inputFile;
	private ElementFilter elementFilter = null;

	/**
	  Constructor.
	 */
	public TopLevelParser( ) throws ParserConfigurationException, SAXException
	{
		super();
		topLevelObject = null;

        SAXParserFactory spf = SAXParserFactory.newInstance();
		SAXParser sp = spf.newSAXParser();
		parser = sp.getXMLReader();
		parser.setFeature("http://xml.org/sax/features/namespaces", true);

		ErrorHandler eh = new PrintStreamErrorHandler(System.out);
		xhp = new XmlHierarchyParser(eh);
		setErrorHandler(eh);
		parser.setContentHandler(xhp);
		if (_instance == null)
			_instance = this;
		inputName = "";
		inputFile = null;
	}

	/**
	 * @return the global shared instance.
	 */
	public static TopLevelParser instance( ) { return _instance; }

	/**
	 * Explicitely sets the global shared instance. Use this when there is
	 * more than one top level parser defined.
	 * @param tlp the instance
	 */
	public static void setInstance( TopLevelParser tlp ) { _instance = tlp; }

	/**
	 * @param eh the ErrorHandler to use
	 */
	public void setErrorHandler( ErrorHandler eh )
	{
		parser.setErrorHandler(eh);
		xhp.setErrorHandler(eh);
	}

	/**
	 * Opens and parses the passed file, creating a new DatabaseObject.
	 * @param input the File to parse
	 * @return @throws IOException on File IO errors
	 * @throws SAXException on parse errors
	 */
	public DatabaseObject parse( File input ) throws IOException, SAXException
	{
		FileInputStream fis = null;
		inputName = input.getName();
		inputFile = input;
		xhp.setFileName(input.getName());
		try
		{
			fis = new FileInputStream(input);
			DatabaseObject ret = parse(fis);
			return ret;
		}
		finally
		{
			if (fis != null)
				fis.close();
		}
	}

	/**
	 * Opens and parses the passed URL, creating a new DatabaseObject.
	 * @param input the URL to read
	 * @return @throws IOException on IO errors
	 * @throws SAXException on parse errors
	 */
	public DatabaseObject parse( URL input ) throws IOException, SAXException
	{
		InputStream fis = null;
		inputName = input.toString();
		inputFile = null;
		xhp.setFileName(input.toString());
		try
		{
			fis = input.openStream();
			DatabaseObject ret = parse(fis);
			return ret;
		}
		finally
		{
			if (fis != null)
				fis.close();
		}
	}

	/**
	 * Parses data from input stream, creating a new DatabaseObject.
	 * @param is the InputStream to read
	 * @return @throws IOException on IO errors
	 * @throws SAXException on parse errors
	 */
	public synchronized DatabaseObject parse( InputStream is ) throws IOException, SAXException
	{
		topLevelObject = null;    // New object will be created.
		xhp.pushObjectParser(this);
		parser.parse(new InputSource(is));
		inputName = "";
		return topLevelObject;
	}

	/**
	 * Opens and parses the passed file, places data in passed DatabaseObject.
	 * @param input the file to read
	 * @param ob the object in which to place the data
	 * @throws IOException on IO errors
	 * @throws SAXException on parse errors
	 */
	public void parse( File input, DatabaseObject ob ) throws IOException, SAXException
	{
		FileInputStream fis = null;
		inputName = input.getName();
		inputFile = input;
		xhp.setFileName(input.getName());
		try
		{
			fis = new FileInputStream(input);
			parse(fis, ob);
		}
		finally
		{
			if (fis != null)
				fis.close();
		}
	}
	
	public Date getFileLMT()
	{
		if (inputFile == null)
			return new Date();
		else
			return new Date(inputFile.lastModified());
	}

	/**
	 * Opens and parses the passed URL, places data in passed DatabaseObject.
	 * @param input the URL to read
	 * @param ob the object in which to place the data
	 * @throws IOException on IO errors
	 * @throws SAXException on parse errors
	 */
	public void parse( URL input, DatabaseObject ob ) throws IOException, SAXException
	{
		InputStream fis = null;
		inputName = input.toString();
		inputFile = null;
		xhp.setFileName(input.toString());
		try
		{
			fis = input.openStream();
			parse(fis, ob);
		}
		finally
		{
			if (fis != null)
				fis.close();
		}
	}

	/**
	 * Parses data from input stream, places data in passed DatabaseObject.
	 * @param is the InputStream to read
	 * @param ob the object in which to place data.
	 * @throws IOException on IO errors
	 * @throws SAXException on parse errors
	 */
	public synchronized void parse( InputStream is, DatabaseObject ob ) throws IOException, SAXException
	{
		topLevelObject = ob;         // Use caller-provided object
		xhp.pushObjectParser(this);
		parser.parse(new InputSource(is));
		inputName = "";
	}


	/**
	 * Writes the passed object to the passed file.
	 * @param output The file to write to.
	 * @param topLevelObject the object to write in the file
	 * @throws IOException on IO error
	 */
	public static void write( File output, Object topLevelObject ) 
		throws IOException
	{
		write(new FileOutputStream(output), topLevelObject);
	}

	/**
	 * Writes the passed object to the passed stream.
	 * @param os The stream to write to.
	 * @param topLevelObject the object to write in the stream
	 * @throws IOException on IO error
	 */
	public static void write(OutputStream os, Object topLevelObject) throws IOException
	{
		write(os,topLevelObject,false);
	}

	/**
	 * Writes the passed object to the passed stream.
	 * @param os The stream to write to.
	 * @param topLevelObject the object to write in the stream
	 * @param insecure when true username and passwords are included in output
	 * @throws IOException on IO error
	 */
	public static void write( OutputStream os, Object topLevelObject,boolean insecure )
		throws IOException
	{
		XmlObjectWriter xow = null;

		if (topLevelObject instanceof EnumList)
			xow = new EnumListParser(
				((EnumList)topLevelObject).getDatabase());
		else if (topLevelObject instanceof Platform)
			xow = new PlatformParser((Platform)topLevelObject);
		else if (topLevelObject instanceof DataTypeSet)
			xow = new DataTypeEquivalenceListParser();
		else if (topLevelObject instanceof EngineeringUnitList)
			xow = new EngineeringUnitListParser();
		else if (topLevelObject instanceof RoutingSpec)
			xow = new RoutingSpecParser((RoutingSpec)topLevelObject);
		else if (topLevelObject instanceof NetworkList)
			xow = new NetworkListParser((NetworkList)topLevelObject);
		else if (topLevelObject instanceof PresentationGroup)
			xow = new PresentationGroupParser(
				(PresentationGroup)topLevelObject);
//		else if (topLevelObject instanceof PMConfigList)
//			xow = new PMConfigListParser();
//		else if (topLevelObject instanceof PMConfig)
//			xow = new PMConfigParser((PMConfig)topLevelObject);
		else if (topLevelObject instanceof PlatformConfig)
			xow = new PlatformConfigParser((PlatformConfig)topLevelObject);
		else if (topLevelObject instanceof EquipmentModel)
			xow = new EquipmentModelParser((EquipmentModel)topLevelObject);
		else if (topLevelObject instanceof DataSource)
			xow = new DataSourceParser((DataSource)topLevelObject);
		else if (topLevelObject instanceof Database)
		{
			DatabaseParser dp = new DatabaseParser((Database)topLevelObject);
			xow = dp;
		}
		else if (topLevelObject instanceof Site)
			xow = new SiteParser((Site)topLevelObject);
		else if (topLevelObject instanceof IntervalList)
			xow = new IntervalListParser((IntervalList)topLevelObject);
		else if (topLevelObject instanceof ScheduleEntry)
			xow = new ScheduleEntryParser((ScheduleEntry)topLevelObject, true);
		else if (topLevelObject instanceof PlatformStatus)
			xow = new PlatformStatusParser((PlatformStatus)topLevelObject);
		else
			throw new IOException(
				"Invalid top-level object type for XML file.");

		XmlOutputStream xos = new XmlOutputStream(os, xow.myName());
		xos.xmlDtdUri = null; // This will force standalone=true

		xos.writeXmlHeader();
		xow.writeXml(xos);
	}

	/**
	 * @param ch Characters from file
	 * @param start start of characters
	 * @param length length of characters
	 */
	public void characters( char[] ch, int start, int length ) throws SAXException
	{
		if (!TextUtil.isAllWhitespace(new String(ch, start, length)))
			throw new SAXException("No character data expected in TopLevelElement");
	}

	/**
	 * Called after start of first element in file is found.
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
		if (localName.equalsIgnoreCase(XmlDbTags.Database_el))
		{
			if (topLevelObject != null)
			{
				if (!(topLevelObject instanceof Database))
					throw new SAXException("Wrong top-object in file, expected "
						+ topLevelObject.getObjectType());
			}
			else
				topLevelObject = Database.getDb();
			DatabaseParser dp = new DatabaseParser((Database)topLevelObject);
			dp.setFileLMT(getFileLMT());
			hier.pushObjectParser(dp);
		}
		else if (elementFilter != null && !elementFilter.acceptElement(localName))
		{
			Logger.instance().info("In file '" + inputFile.getPath() 
				+ "', Ignoring element '" + localName + "' because filter returned false.");
			hier.pushObjectParser(new ElementIgnorer());
		}
		else if (localName.equalsIgnoreCase(XmlDbTags.EnumList_el))
		{
			if (topLevelObject != null)
			{
				if (!(topLevelObject instanceof EnumList))
					throw new SAXException("Wrong top-object in file, expected "
						+ topLevelObject.getObjectType());
			}
			else
				topLevelObject = Database.getDb().enumList;

			hier.pushObjectParser(new EnumListParser(topLevelObject.getDatabase()));
		}
		else if (localName.equalsIgnoreCase(XmlDbTags.Platform_el))
		{
			String id = atts.getValue(XmlDbTags.PlatformId_at);
			DbKey pid = Constants.undefinedId;
			if (id != null)
			{
				try { pid = DbKey.createDbKey(Long.parseLong(id)); }
				catch(NumberFormatException e)
				{ pid = Constants.undefinedId;}
			}

			Platform top;
			if (topLevelObject != null)
			{
				if (!(topLevelObject instanceof Platform))
					throw new SAXException("Wrong top-object in file, expected "
						+ topLevelObject.getObjectType());
				top = (Platform)topLevelObject;
			}
			else
			{
				top = new Platform();
				// Default LMT to the file time. If file contains
				// XML element for lmt, it will overwrite this.
				top.lastModifyTime = this.getFileLMT();
				topLevelObject = top;
			}
			//top.platformId = pid;
            try {
                top.setId(pid);
            }
            catch (DatabaseException e) {
                // this shouldn't happen.
            }

			hier.pushObjectParser(new PlatformParser(top));
		}
		else if (localName.equalsIgnoreCase(
			XmlDbTags.DataTypeEquivalenceList_el))
		{
			if (topLevelObject != null)
			{
				if (!(topLevelObject instanceof DataTypeSet))
					throw new SAXException("Wrong top-object in file, expected "
						+ topLevelObject.getObjectType());
			}
			else
				topLevelObject = Database.getDb().dataTypeSet;
			hier.pushObjectParser(new DataTypeEquivalenceListParser());
		}
		else if (localName.equalsIgnoreCase(XmlDbTags.EngineeringUnitList_el))
		{
			if (topLevelObject != null)
			{
				if (!(topLevelObject instanceof EngineeringUnitList))
					throw new SAXException("Wrong top-object in file, expected "
						+ topLevelObject.getObjectType());
			}
			else
				topLevelObject = Database.getDb().engineeringUnitList;
			hier.pushObjectParser(new EngineeringUnitListParser());
		}
		else if (localName.equalsIgnoreCase(XmlDbTags.RoutingSpec_el))
		{
			String name = atts.getValue(XmlDbTags.name_at);
			if (name == null)
				throw new SAXException(XmlDbTags.RoutingSpec_el
					+ " without " + XmlDbTags.name_at +" attribute");

			RoutingSpec top;
			if (topLevelObject != null)
			{
				if (!(topLevelObject instanceof RoutingSpec))
					throw new SAXException("Wrong top-object in file, expected "
						+ topLevelObject.getObjectType());
				top = (RoutingSpec)topLevelObject;
			}
			else
			{
				top = new RoutingSpec(name);
				topLevelObject = top;
			}

			if (inputFile != null)
				top.lastModifyTime = new Date(inputFile.lastModified());
			else
				top.lastModifyTime = new Date();

			hier.pushObjectParser(new RoutingSpecParser(top));
		}
		else if (localName.equalsIgnoreCase(XmlDbTags.NetworkList_el))
		{
			String name = atts.getValue(XmlDbTags.name_at);
			if (name == null)
				throw new SAXException(XmlDbTags.NetworkList_el
					+ " without " + XmlDbTags.name_at +" attribute");

			NetworkList top;
			if (topLevelObject != null)
			{
				if (!(topLevelObject instanceof NetworkList))
					throw new SAXException("Wrong top-object in file, expected "
						+ topLevelObject.getObjectType());
				top = (NetworkList)topLevelObject;
			}
			else
			{
				top = new NetworkList(name);
				topLevelObject = top;
			}

			if (inputFile != null)
				top.lastModifyTime = new Date(inputFile.lastModified());
			else
				top.lastModifyTime = new Date();

			hier.pushObjectParser(new NetworkListParser(top));
		}
		else if (localName.equalsIgnoreCase(XmlDbTags.PresentationGroup_el))
		{
			String name = atts.getValue(XmlDbTags.name_at);
			if (name == null)
				throw new SAXException(XmlDbTags.PresentationGroup_el
					+ " without " + XmlDbTags.name_at +" attribute");

			PresentationGroup top;
			if (topLevelObject != null)
			{
				if (!(topLevelObject instanceof PresentationGroup))
					throw new SAXException("Wrong top-object in file, expected "
						+ topLevelObject.getObjectType());
				top = (PresentationGroup)topLevelObject;
			}
			else
			{
				top = new PresentationGroup(name);
				topLevelObject = top;
			}

			if (inputFile != null)
				top.lastModifyTime = new Date(inputFile.lastModified());
			else
				top.lastModifyTime = new Date();
			hier.pushObjectParser(new PresentationGroupParser(top));
		}
		else if (localName.equalsIgnoreCase(XmlDbTags.PlatformConfig_el))
		{
			String nm = atts.getValue(XmlDbTags.PlatformConfig_configName_at);
			if (nm == null)
				throw new SAXException(XmlDbTags.PlatformConfig_el + " without "
					+ XmlDbTags.PlatformConfig_configName_at +" attribute");

			PlatformConfig top;
			if (topLevelObject != null)
			{
				if (!(topLevelObject instanceof PlatformConfig))
					throw new SAXException("Wrong top-object in file, expected "
						+ topLevelObject.getObjectType());
				top = (PlatformConfig)topLevelObject;
			}
			else
			{
				top = new PlatformConfig(nm);
				topLevelObject = top;
			}
			top.configName = nm;

			Database.getDb().platformConfigList.add(top);
			hier.pushObjectParser(new PlatformConfigParser(top));
		}
		else if (localName.equalsIgnoreCase(XmlDbTags.EquipmentModel_el))
		{
			String nm = atts.getValue(XmlDbTags.name_at);
			if (nm == null)
				throw new SAXException(XmlDbTags.EquipmentModel_el + " without "
					+ XmlDbTags.name_at +" attribute");

			EquipmentModel top;
			if (topLevelObject != null)
			{
				if (!(topLevelObject instanceof EquipmentModel))
					throw new SAXException("Wrong top-object in file, expected "
						+ topLevelObject.getObjectType());
				top = (EquipmentModel)topLevelObject;
			}
			else
			{
				top = new EquipmentModel(nm);
				topLevelObject = top;
			}

			Database.getDb().equipmentModelList.add(top);
			hier.pushObjectParser(new EquipmentModelParser(top));
		}
		else if (localName.equalsIgnoreCase(XmlDbTags.DataSource_el))
		{
			String nm = atts.getValue(XmlDbTags.name_at);
			if (nm == null)
				throw new SAXException(XmlDbTags.DataSource_el + " without "
					+ XmlDbTags.name_at +" attribute");
			String tp = atts.getValue(XmlDbTags.type_at);
			if (tp == null)
				throw new SAXException(XmlDbTags.DataSource_el + " without "
					+ XmlDbTags.type_at +" attribute");

			DataSource top;
			if (topLevelObject != null)
			{
				if (!(topLevelObject instanceof DataSource))
					throw new SAXException("Wrong top-object in file, expected "
						+ topLevelObject.getObjectType());
				top = (DataSource)topLevelObject;
				top.dataSourceType = tp;
			}
			else
			{
				top = new DataSource(nm, tp);
				topLevelObject = top;
			}

			// Database.getDb().dataSourceList.add(top);
			hier.pushObjectParser(new DataSourceParser(top));
		}
		else if (localName.equalsIgnoreCase(XmlDbTags.Site_el))
		{
			if (topLevelObject != null)
			{
				if (!(topLevelObject instanceof Site))
					throw new SAXException("Wrong top-object in file, expected "
						+ topLevelObject.getObjectType());
			}
			else
				topLevelObject = new Site();

			hier.pushObjectParser(new SiteParser((Site)topLevelObject));
		}
		else if (localName.equalsIgnoreCase(XmlDbTags.PlatformList_el))
		{
			if (topLevelObject != null)
			{
				if (!(topLevelObject instanceof PlatformList))
					throw new SAXException("Wrong top-object in file, expected "
						+ topLevelObject.getObjectType());
			}
			else
				topLevelObject = Database.getDb().platformList;

			PlatformListParser plp = new PlatformListParser(
				(PlatformList)topLevelObject);
			plp.setFileLMT(getFileLMT());
			hier.pushObjectParser(plp);
		}
		else if (localName.equalsIgnoreCase(XmlDbTags.IntervalList_el))
		{
			IntervalList ilist = IntervalList.editInstance();
			topLevelObject = ilist;
			hier.pushObjectParser(new IntervalListParser(ilist));
		}
		else if (localName.equalsIgnoreCase(CompXioTags.loadingApplication))
		{
			CompAppInfo cai = new CompAppInfo();
			topLevelObject = cai;
			cai.setAppName(atts.getValue(CompXioTags.name));
			CompXio compXio = new CompXio("TopLevelParser", null);
			compXio.setWorkingObject(cai);
			hier.pushObjectParser(compXio);
		}
		else if (localName.equalsIgnoreCase(XmlDbTags.ScheduleEntry_el))
		{
			ScheduleEntry se = 
				new ScheduleEntry(
					XmlUtils.getAttrIgnoreCase(atts, XmlDbTags.name_at));
			topLevelObject = se;
			hier.pushObjectParser(new ScheduleEntryParser(se, true));
		}
		else if (localName.equalsIgnoreCase(XmlDbTags.PlatformStatus_el))
		{
			try
			{
				long platformId = Long.parseLong(XmlUtils.getAttrIgnoreCase(atts, XmlDbTags.id_at));
				PlatformStatus ps = new PlatformStatus(DbKey.createDbKey(platformId));
				topLevelObject = ps;
				hier.pushObjectParser(new PlatformStatusParser(ps));
			}
			catch(NumberFormatException ex)
			{
				throw new SAXException("Invalid non-numeric platform id in " + localName);
			}
		}
		else
			throw new SAXException("Invalid top-level element '" + localName
				+ "'");
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
	 * @return the top level object being parsed.
	 */
	public Object getTopLevelObject( ) { return topLevelObject; }

	/**
	 * Convenience method for parsers to log warnings.
	 * @param msg the message
	 */
	public void parseWarning( String msg )
	{
		Logger.instance().log(Logger.E_WARNING, inputName + ": " + msg);
	}

	/**
	 * Convenience method for parsers to log errors.
	 * @param msg the message
	 */
	public void parseFailure( String msg )
	{
		Logger.instance().log(Logger.E_FAILURE, inputName + ": " + msg);
	}

  	/**
	 * Test main: Parses all files given on command line & prints them
	 * to System.out.
	 * By comparing the input and output, you can verify that both the parse
	 * and print functioned properly.
	 * @param args command line args
	 */
	public static void main( String[] args ) throws IOException, SAXException, ParserConfigurationException
	{
		TopLevelParser tlp = new TopLevelParser();
		Database.setDb(new decodes.db.Database());

		for(int i=0; i<args.length; i++)
		{
			tlp.parse(new File(args[i]));
			TopLevelParser.write(System.out, tlp.topLevelObject);
		}
	}
	
	public ElementFilter getElementFilter()
	{
		return elementFilter;
	}

	public void setElementFilter(ElementFilter elementFilter)
	{
		this.elementFilter = elementFilter;
	}
}
