package ilex;

import ilex.util.TextUtil;
import ilex.xml.XmlHierarchyParser;
import ilex.xml.XmlObjectParser;
import java.util.HashMap;
import java.util.Vector;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

/**
 * This class provides a concrete implementation of the
 * XmlObjectParser interface, but this class is still pretty
 * generic.  This class should be the base class of other
 * classes that can handle any well-formed portion of XML
 * content -- i.e. anything that could conceivably form the
 * content inside an XML element.
 * <p>
 *   By default, no valid content is allowed by this class.
 *   The default implementation of each of the XML event methods
 *   produces a SAXException.  Derived classes should override
 *   these as appropriate.
 * </p>
 */


public class XmlContentParser implements XmlObjectParser
{

  /** This stores a list of XmlElementDescriptors.  */

    Vector _elemDescs;

  /**
   * This stores the same information, but as a HashMap.
   * The key to the HashMap is the element tag name, and the
   * value is the XmlObjectFactory.
   */

    HashMap _factories;


  //====================================================================
  // Constructors and Related Methods

  /**
   * Default constructor.
   */

    public XmlContentParser()
    {
		_elemDescs = new Vector();
		_factories = new HashMap();
	}

  /**
   * Add XmlElementDescriptors to our list.
   */

    public void addElemDescs(XmlElementDescriptor[] descs)
    {
        for (int i = 0; i < descs.length; ++i) {
            _elemDescs.add(descs[i]);
            _factories.put(descs[i].tag, descs[i].factory);
        }
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
	    System.out.println("In XmlContentParser.characters()");
	    throw new SAXException("Unexpected character data");
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
	    System.out.println("In XmlContentParser.startElement()");
	    throw new SAXException("Unexpected element:  '" + qname + "'");
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
	    System.out.println("In XmlContentParser.endElement()");
	    throw new SAXException("Unexpected end tag");
	}

  /**
   * Allows an object to keep track of whitespace, if needed.
   */

	public void ignorableWhitespace (char ch[], int start, int length)
		throws SAXException
	{
	    System.out.println("In XmlContentParser.ignorableWhitespace()");
	    throw new SAXException("Unexpected whitespace characters");
	}


  //====================================================================
  // Other methods.

  /**
   * This method is for use by derived classes to verify that character
   * data received by the characters() method is all whitespace.  This
   * throws a SAXException if not.
   */

    protected void checkWhite(char[] ch, int start, int length)
        throws SAXException
    {
		if (!TextUtil.isAllWhitespace(new String(ch, start, length)))
			throw new SAXException(
			    "No non-white character data expected");
	}
}
