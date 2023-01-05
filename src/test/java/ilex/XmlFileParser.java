package ilex;

import ilex.xml.PrintStreamErrorHandler;
import ilex.xml.XmlHierarchyParser;
import ilex.xml.XmlObjectParser;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.IOException;
import java.net.URL;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import org.xml.sax.Attributes;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

/**
 * <p>
 *   This class can be used as the top-level parser when parsing
 *   an XML file.
 * </p>
 * <p>
 *   There is a wide variety of parse() methods.
 * </p>
 */

public class XmlFileParser
	extends XmlContentParser
{

  //===================================================================
  // Member data.

  /** SAX parser object.  */

	private XMLReader reader;

  /** Manages hierarchy of parsers.  */

	private XmlHierarchyParser xhp;

  /** This is the object that is being built.  */

    Object topLevelObject;

  //====================================================================
  // Constructors and Related Methods.

  /**
   * Default constructor.
   */

    public XmlFileParser()
	    throws ParserConfigurationException, SAXException
    {
        this(new PrintStreamErrorHandler(System.out));
    }

  /**
   * Construct with a list of XmlElementDescriptors.
   */

    public XmlFileParser(XmlElementDescriptor[] descs)
        throws ParserConfigurationException, SAXException
    {
        this();
        addElemDescs(descs);
    }

  /**
   * Construct with a user-supplied error handler.
   */

    public XmlFileParser(ErrorHandler eh)
        throws ParserConfigurationException, SAXException
    {
		super();

		topLevelObject = null;

        SAXParserFactory spf = SAXParserFactory.newInstance();
		SAXParser sp = spf.newSAXParser();
		reader = sp.getXMLReader();
		reader.setFeature("http://xml.org/sax/features/namespaces", true);

		xhp = new XmlHierarchyParser(eh);
		setErrorHandler(eh);

		reader.setContentHandler(xhp);
	}

  /**
   * Sets the error handler for this parser.
   */

	public void setErrorHandler(ErrorHandler eh)
	{
		reader.setErrorHandler(eh);
		xhp.setErrorHandler(eh);
	}


  //====================================================================
  // Parsing methods.

  /**
   * Opens and parses the passed file, creating a new XmlElement object..
   */

	public Object parse(File input)
		throws IOException, SAXException
	{
		FileInputStream fis = null;
		try
		{
			fis = new FileInputStream(input);
			Object ret = parse(fis);
			return ret;
		}
		finally
		{
			if (fis != null) fis.close();
		}
	}

  /**
   * Opens and parses the passed URL, creating a new Object.
   */

	public Object parse(URL input)
		throws IOException, SAXException
	{
		InputStream fis = null;
		try
		{
			fis = input.openStream();
			Object ret = parse(fis);
			return ret;
		}
		finally
		{
			if (fis != null) fis.close();
		}
	}

  /**
   * Parses data from input stream, creating a new DatabaseObject.
   */

	public Object parse(InputStream is)
		throws IOException, SAXException
	{
		topLevelObject = null;    // New object will be created.
		xhp.pushObjectParser(this);
		reader.parse(new InputSource(is));
		return topLevelObject;
	}

  /**
   * Opens and parses the passed file, places data in passed DatabaseObject.
   */

	public void parse(File input, Object obj)
		throws IOException, SAXException
	{
		FileInputStream fis = null;
		try
		{
			fis = new FileInputStream(input);
			parse(fis, obj);
		}
		finally
		{
			if (fis != null) fis.close();
		}
	}

  /**
   * Opens and parses the passed URL, places data in passed DatabaseObject.
   */

	public void parse(URL input, Object obj)
		throws IOException, SAXException
	{
		InputStream fis = null;
		try
		{
			fis = input.openStream();
			parse(fis, obj);
		}
		finally
		{
			if (fis != null) fis.close();
		}
	}

  /**
   * Parses data from input stream, places data in passed DatabaseObject.
   */

	public void parse(InputStream is, Object obj)
		throws IOException, SAXException
	{
		topLevelObject = obj;      // Use the caller-provided object
		xhp.pushObjectParser(this);
		reader.parse(new InputSource(is));
	}


  //====================================================================
  // XmlObjectParser methods.

  /**
   * This is a pass-through from the SAX Content Handler.
   * Your parser should save/convert these characters as appropriate.
   */

	public void characters(char[] ch, int start, int length)
		throws SAXException
	{
	    System.out.println("In XmlFileParser.characters()");
	}

  /**
   * Start a new element within the hierarchy.
   * The first argument is the hierarchy. You can use it to push lower-level
   * object parsers. Subsequent arguments are from the SAX Content Handler.
   */

	public void startElement(XmlHierarchyParser hier,
                             String namespaceURI,
                             String localName,
                             String qname,
                             Attributes atts)
		throws SAXException
	{
	    System.out.println("In XmlFileParser.startElement()");
	    XmlObjectFactory f =
	        (XmlObjectFactory) _factories.get(localName);
	    if (f == null)
			throw new SAXException("Invalid top-level element:  '" +
			                       localName + "'");

		hier.pushObjectParser(f);
	}

  /**
   * Signals the end of the current element.
   * Typically this should cause your parser to pop the stack in the
   * hierarchy. Then do whatever cleanup or finalizing is necessary.
   */

	public void endElement(XmlHierarchyParser hier,
                           String namespaceURI,
                           String localName,
                           String qname)
		throws SAXException
	{
	    System.out.println("In XmlFileParser.endElement()");
	}

  /**
   * Allows an object to keep track of whitespace, if needed.
   */

	public void ignorableWhitespace (char ch[], int start, int length)
		throws SAXException
	{
	    System.out.println("In XmlFileParser.ignorableWhitespace()");
	}
}