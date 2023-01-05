package ilex;


/**
 * This class is used to associate XML element tag names with
 * factory objects that are used to create new objects.
 */

public class XmlElementDescriptor
{
  /** Store the tag */

    public final String tag;

  /** Store a factory object.  */

    public final XmlObjectFactory factory;

  /** Constructor  */

    public XmlElementDescriptor(String t, XmlObjectFactory f)
    {
        tag = t;
        factory = f;
    }
}