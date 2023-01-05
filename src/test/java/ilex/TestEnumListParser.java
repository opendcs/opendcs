package ilex;

import ilex.xml.XmlHierarchyParser;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

/**
 * This class is used solely in the XmlParserTester test program.
 * This handles the parse events for an EnumList element in the test
 * XML file.
 */

public class TestEnumListParser extends XmlObjectFactory
{
  /** Default constructor.  */

    public TestEnumListParser()
    {
    }

  /** Create a new TestEnumList object.   */

    public XmlObject create()
    {
        return new TestEnumList();
    }


  //====================================================================
  // XmlObjectParser methods.

  /**
   * This throws a SAXException if any non-whitespace characters are
   * seen.
   */

	public void characters(char[] ch, int start, int length)
		throws SAXException
	{
	    System.out.println("In TestEnumListParser.characters()");
        checkWhite(ch, start, length);
	}

  /**
   * This handles the start of a new XML element.
   */

	public void startElement(XmlHierarchyParser hier,
	                         String namespaceURI,
	                         String localName,
	                         String qname,
	                         Attributes atts)
		throws SAXException
	{
	    System.out.println("In TestEnumListParser.startElement()");

	    if (!localName.equals("Enum"))
	        throw new SAXException(
	            "Invalid element inside of EnumList:  '" +
	            localName + "'");

        hier.pushObjectParser(new TestEnumParser());
	}

}