package ilex;

/**
 * This class is used solely in the XmlParserTester test program.
 * This handles the parse events for an Enum element in the test
 * XML file.
 */


public class TestEnumParser extends XmlObjectFactory
{
  /** Default constructor.  */

    public TestEnumParser()
    {
    }

  /** Create a new TestEnum object.   */

    public XmlObject create()
    {
        return new TestEnum();
    }


  //====================================================================
  // XmlObjectParser methods.

}