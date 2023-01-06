package ilex;



/**
 * This class is designed to be the base class of
 * application-specific classes that perform two functions:
 * <ol>
 *   <li>
 *     Create new objects when certain XML tags are seen, and
 *   </li>
 *   <li>
 *     Parse the contents of the XML elements.
 *   </li>
 * </ol>
 * <p>
 *   As with the XmlContentParser (from which this derives)
 *   the default implementation of each of the XML event methods
 *   produces a SAXException.  Derived classes should override
 *   these as appropriate.
 * </p>
 */

public class XmlObjectFactory extends XmlContentParser
{
  /**
   * Create a new object of the correct type.
   * This will create a new, "blank", object of some class that is a
   * subclass of XmlObject.  Derived classes should override this
   * method such that the actual class of the newly created object
   * is the correct one for a particular tag.
   * By default, this creates a new object which
   * is an instance of XmlObject.
   */

    public XmlObject create()
    {
        return new XmlObject();
    }



}
